"""从 ECDICT(stardict.db)抽取完整多义项释义与考试标签。

产出:
  assets/gd.json     {word: 多义项释义(按 \\n 分行,词性前缀已归一)}
  assets/exams.json  {word: "cet4,gaokao,..."}  (存量 CET-4 词默认带 cet4)

用法: python tools/build_gd.py
前置: tools/stardict.db —— ECDICT sqlite 版,下载方式见 词库扩展-数据规格.md §1
"""
import json
import os
import sqlite3

TOOLS = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(TOOLS)
ASSETS = os.path.join(ROOT, "assets")
DB = os.path.join(TOOLS, "stardict.db")

# ECDICT 词性前缀归一 → 项目风格(n./v./adj./adv.)
POS_MAP = [("a. ", "adj. "), ("ad. ", "adv. "), ("vi. ", "v. "), ("vt. ", "v. ")]
# 领域标注行([化] [医] [电] 等)对学习者是噪音,整行丢弃
DROP_LINE_PREFIX = "["

TAG_MAP = {"gk": "gaokao", "cet4": "cet4", "cet6": "cet6",
           "ky": "kaoyan", "ielts": "ielts", "toefl": "toefl", "gre": "gre"}


def clean_translation(t: str) -> str | None:
    lines = []
    for raw in (t or "").splitlines():
        line = raw.strip()
        if not line or line.startswith(DROP_LINE_PREFIX):
            continue
        for ab, full in POS_MAP:
            if line.startswith(ab):
                line = full + line[len(ab):]
                break
        lines.append(line)
    # 去重相邻行,最多保留 4 个义项
    out = []
    for line in lines:
        if line not in out:
            out.append(line)
        if len(out) >= 4:
            break
    return "\n".join(out) if out else None


def main():
    words = [x["w"] if isinstance(x, dict) else x
             for x in json.load(open(os.path.join(ASSETS, "words.json"), encoding="utf-8"))]
    con = sqlite3.connect(DB)
    cur = con.cursor()
    gd, exams = {}, {}
    n_tr = 0
    for i, w in enumerate(words):
        cur.execute("SELECT translation, tag FROM stardict WHERE word = ?", (w,))
        row = cur.fetchone()
        if row is None:
            cur.execute("SELECT translation, tag FROM stardict WHERE word = ?", (w.lower(),))
            row = cur.fetchone()
        if row is not None:
            tr, tag = row
            g = clean_translation(tr)
            if g:
                gd[w] = g
                n_tr += 1
            tags = {TAG_MAP[t] for t in (tag or "").split() if TAG_MAP.get(t)}
        else:
            tags = set()
        tags.add("cet4")  # 存量词表整体来自 CET-4
        exams[w] = ",".join(sorted(tags))
        if (i + 1) % 1000 == 0:
            print(f"... {i + 1}/{len(words)}")
    con.close()
    json.dump(gd, open(os.path.join(ASSETS, "gd.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    json.dump(exams, open(os.path.join(ASSETS, "exams.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    print(f"多义项释义 gd: {n_tr}/{len(words)} | 考试标签: {len(exams)}/{len(words)}")


if __name__ == "__main__":
    main()
