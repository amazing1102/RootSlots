# -*- coding: utf-8 -*-
"""词库扩展·合并管线(词库扩展-数据规格.md §4/§5)。

从 ECDICT(stardict.db)抽取目标考试的新词,按规格规则清洗后与现有 words.json 合并:
  - 只拉带 gk(高考)标签的词;阶段三再放开 cet6/ky/ielts/toefl/gre
  - 屈折吸收:exchange 0:lemma 命中池内词 → 该屈折词不入库,标签并入原形(§4.2)
  - 丢弃:含空格词组 / 单字符 / 纯数字(§4.5)
  - 释义:自策(c01–c19)永不被覆盖;ECDICT 取首义项清洗 ≤24 汉字(§5.2)
  - IPA:ECDICT phonetic 兜底(解析失败则无音标,App 现有隐藏逻辑兜住)(§10)

产出:
  assets/exam_words.json      新词条目 [{w,g,gd,i,x}]  (x 为逗号分隔考试短码)
  assets/exam_lemma_tags.json {原形: "gaokao,..." }    被吸收屈折词的标签并入原形

用法: python tools/build_dict.py
前置: tools/stardict.db + assets/words.json(先跑 build_gd.py 保证 exams.json 为最新标签)
"""
import json
import os
import re
import sqlite3
import sys

TOOLS = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(TOOLS)
ASSETS = os.path.join(ROOT, "assets")
DB = os.path.join(TOOLS, "stardict.db")

# 基准词表 = 源词表 md(而非 words.json 产物):words.json 是本管线输出,
# 拿它当基准会让上一轮扩库词被当作「已有」而丢失(踩过:424 词凭空消失)。
sys.path.insert(0, TOOLS)
import build_data

# 目标考试:全部 7 类(阶段三全量)。gk/cet4 已随阶段一入库,此处重跑自动去重。
NEW_WORD_TAGS = {"gk", "cet4", "cet6", "ky", "ielts", "toefl", "gre"}
TAG_MAP = {"gk": "gaokao", "cet4": "cet4", "cet6": "cet6",
           "ky": "kaoyan", "ielts": "ielts", "toefl": "toefl", "gre": "gre"}

POS_MAP = [("a. ", "adj. "), ("ad. ", "adv. "), ("vi. ", "v. "), ("vt. ", "v. ")]
MAX_GLOSS_HANZI = 24   # 与结果卡/列表单行展示对齐(§5.2)


def clean_first_sense(t: str) -> str | None:
    """ECDICT translation → 首义项短释义(词性前缀归一,≤24 汉字)。"""
    for raw in (t or "").splitlines():
        line = raw.strip()
        if not line or line.startswith("["):
            continue
        for ab, full in POS_MAP:
            if line.startswith(ab):
                line = full + line[len(ab):]
                break
        hanzi = sum(1 for ch in line if "\u4e00" <= ch <= "\u9fff")
        if hanzi > MAX_GLOSS_HANZI:
            continue    # 首义项过长时取下一条更短的,而不是截断出残句
        return line
    return None


def clean_full(t: str) -> str | None:
    """ECDICT translation → 多义项 gd(同 build_gd.py 风格,≤4 义项)。"""
    out = []
    for raw in (t or "").splitlines():
        line = raw.strip()
        if not line or line.startswith("["):
            continue
        for ab, full in POS_MAP:
            if line.startswith(ab):
                line = full + line[len(ab):]
                break
        if line not in out:
            out.append(line)
        if len(out) >= 4:
            break
    return "\n".join(out) if out else None


def clean_ipa(phonetic: str) -> str | None:
    """ECDICT phonetic 兜底。ECDICT 特点:辅音是普通 ASCII 字母(hә:rikәn),
    次重音用「.」;纯 ASCII 串(hei/ei)是重拼不是音标。失败返回 None。"""
    p = (phonetic or "").strip().strip("/")
    if not p:
        return None
    p = re.sub(r"\s*[a-z]+\.\s*$", "", p).strip().strip("/")   # 去尾部词性残留 " n."
    p = p.replace(".", "ˌ")
    if not any(ord(ch) > 127 for ch in p):
        return None                                            # 纯 ASCII 重拼,风格不符,弃用
    if re.search(r"[\d\[\]()]", p):
        return None
    return p


def main():
    words = [{"w": w} for w in build_data.parse_words()]
    # ECDICT 错拼词条直接丢弃(源:目标考试标签拉取时带入)
    typo_drop = {"reservior"}
    gloss_map = {}
    import glob
    for f in sorted(glob.glob(os.path.join(ASSETS, "glosses", "*.json"))):
        gloss_map.update(json.load(open(f, encoding="utf-8")))
    base = {}
    for x in words:
        base[x["w"].lower()] = x

    con = sqlite3.connect(DB)
    cur = con.cursor()
    like = " OR ".join(f"tag LIKE '%{t}%'" for t in sorted(NEW_WORD_TAGS))
    cur.execute(f"SELECT word, tag, exchange, phonetic, translation FROM stardict WHERE {like}")
    rows = cur.fetchall()
    con.close()

    # 第一遍:候选新词(先收集齐,屈折判定才能看到彼此)
    cands = []
    for w, tag, ex, ph, tr in rows:
        if w.lower() in base or w.lower() in typo_drop:
            continue
        if " " in w or len(w) <= 1 or any(ch.isdigit() for ch in w):
            continue
        tags = {TAG_MAP[t] for t in tag.split() if TAG_MAP.get(t)}
        m = re.search(r"(?:^|/)0:([^/]+)", ex or "")
        cands.append({"w": w, "tags": tags, "lemma": m.group(1) if m else None,
                      "ph": ph, "tr": tr})

    pool = set(base) | {c["w"].lower() for c in cands}
    new_entries, lemma_tags, absorbed = [], {}, []
    seen = set()
    for c in cands:
        # 屈折吸收(§4.2):原形在池内 → 不单独入库,标签并入原形
        if c["lemma"] and c["lemma"] != c["w"] and c["lemma"].lower() in pool:
            tgt = c["lemma"].lower()
            key = tgt if tgt in base else tgt          # 原形可能是存量词或新词
            merged = lemma_tags.setdefault(key, set())
            merged |= c["tags"]
            absorbed.append((c["w"], c["lemma"]))
            continue
        k = c["w"].lower()
        if k in seen:                                   # 新词之间大小写去重,保留首个
            continue
        seen.add(k)
        g = gloss_map.get(c["w"]) or clean_first_sense(c["tr"])
        if not g:
            continue                                    # 无释义不入库(§5)
        new_entries.append({
            "w": c["w"],
            "g": g,
            "gd": clean_full(c["tr"]),
            "i": clean_ipa(c["ph"]),
            "x": ",".join(sorted(c["tags"])),
        })

    new_entries.sort(key=lambda e: e["w"].lower())
    lemma_tags = {k: ",".join(sorted(v)) for k, v in sorted(lemma_tags.items())}

    json.dump(new_entries, open(os.path.join(ASSETS, "exam_words.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=0)
    json.dump(lemma_tags, open(os.path.join(ASSETS, "exam_lemma_tags.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=0)

    n_ipa = sum(1 for e in new_entries if e["i"])
    print(f"ECDICT 目标考试行 {len(rows)} | 候选 {len(cands)} | "
          f"屈折吸收 {len(absorbed)} | 入库新词 {len(new_entries)}(IPA {n_ipa})")
    print(f"超集总量: {len(words)} + {len(new_entries)} = {len(words) + len(new_entries)}")
    print("吸收样例:", absorbed[:10])


if __name__ == "__main__":
    main()
