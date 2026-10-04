# -*- coding: utf-8 -*-
"""build_ipa.py — 从 CMUdict 生成美式 IPA 音标表
用法: 先跑一次(生成 assets/ipa.json),再跑 build_data.py 合并进 words.json 的 "i" 字段。
词表变更后重跑本脚本即可。缺词清单写入 tools/ipa_missing.txt(人工补录)。
数据源: cmusphinx/cmudict (BSD),ARPAbet → IPA(美式),含重音符。
"""
import json, os, re, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "assets")
CACHE = os.path.join(ROOT, "tools", "cmudict.dict.cache")

SOURCES = [
    # 完整版 0.7b(含专有名词/英式拼写变体);cmusphinx 版删了专有名词,只作兜底
    "https://raw.githubusercontent.com/Alexir/CMUdict/master/cmudict-0.7b",
    "https://cdn.jsdelivr.net/gh/Alexir/CMUdict@master/cmudict-0.7b",
    "https://ghproxy.net/https://raw.githubusercontent.com/Alexir/CMUdict/master/cmudict-0.7b",
    "https://raw.githubusercontent.com/cmusphinx/cmudict/master/cmudict.dict",
    "https://cdn.jsdelivr.net/gh/cmusphinx/cmudict@master/cmudict.dict",
]

# ARPAbet → IPA(美式)。AH0 弱读为 ə,ER0 弱读为 ər。
MAP = {
    "AA": "ɑː", "AE": "æ", "AH": "ʌ", "AO": "ɔː", "AW": "aʊ", "AY": "aɪ",
    "EH": "ɛ", "ER": "ɜːr", "EY": "eɪ", "IH": "ɪ", "IY": "iː", "OW": "oʊ",
    "OY": "ɔɪ", "UH": "ʊ", "UW": "uː",
    "B": "b", "CH": "tʃ", "D": "d", "DH": "ð", "F": "f", "G": "ɡ", "HH": "h",
    "JH": "dʒ", "K": "k", "L": "l", "M": "m", "N": "n", "NG": "ŋ", "P": "p",
    "R": "r", "S": "s", "SH": "ʃ", "T": "t", "TH": "θ", "V": "v", "W": "w",
    "Y": "j", "Z": "z", "ZH": "ʒ",
}
VOWELS = set(MAP) - {"B","CH","D","DH","F","G","HH","JH","K","L","M","N","NG","P","R","S","SH","T","TH","V","W","Y","Z","ZH"}


def ensure_cmudict() -> str:
    if os.path.exists(CACHE) and os.path.getsize(CACHE) > 1_000_000:
        return open(CACHE, encoding="latin-1").read()   # cmudict-0.7b 为 Latin-1
    last_err = None
    for url in SOURCES:
        try:
            print("下载 CMUdict:", url)
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            data = urllib.request.urlopen(req, timeout=60).read()
            if len(data) > 1_000_000:
                open(CACHE, "wb").write(data)
                return data.decode("latin-1")
        except Exception as e:  # noqa
            last_err = e
    raise SystemExit(f"CMUdict 下载失败(所有镜像): {last_err}")


def to_ipa(phonemes):
    """音标序列 → IPA 字符串;重音符按音节 onset 回退插入。未知音素返回 None。"""
    out = []  # (is_vowel, text, stress)
    for ph in phonemes:
        m = re.match(r"^([A-Z]+)([012]?)$", ph)
        if not m or m.group(1) not in MAP:
            return None
        base, st = m.group(1), int(m.group(2) or 0)
        txt = MAP[base]
        if base == "AH" and st == 0: txt = "ə"
        if base == "ER" and st == 0: txt = "ər"
        out.append((base in VOWELS, txt, st))
    pieces = [t for _v, t, _s in out]
    marks = [""] * len(pieces)
    for i, (is_v, _t, st) in enumerate(out):
        if is_v and st in (1, 2):
            j = i
            while j > 0 and not out[j - 1][0]:
                j -= 1
            mark = "ˈ" if st == 1 else "ˌ"
            if st == 1 or not marks[j]:
                marks[j] = mark
    return "".join(a + b for a, b in zip(marks, pieces))


def main():
    words = [w["w"] for w in json.load(open(os.path.join(ASSETS, "words.json"), encoding="utf-8"))]
    text = ensure_cmudict()
    dict_ipa = {}
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith(";;;"):
            continue
        parts = line.split()
        token = re.sub(r"\(\d+\)$", "", parts[0]).lower()
        if not re.fullmatch(r"[a-z]+", token):        # 只收纯字母词(本词表全是)
            continue
        if token in dict_ipa:                          # 首个变体即默认美式读音
            continue
        ipa = to_ipa(parts[1:])
        if ipa:
            dict_ipa.setdefault(token, ipa)

    result, missing = {}, []
    for w in words:
        lw = w.lower()
        if lw in dict_ipa:
            result[w] = dict_ipa[lw]
        elif "-" in w:
            # 连字符词:各段分别查,直接拼接(重音符可能有冗余,展示层可接受)
            parts = [dict_ipa.get(p.lower()) for p in w.split("-")]
            if all(parts):
                result[w] = "".join(parts)
            else:
                missing.append(w)
        else:
            missing.append(w)
    json.dump(result, open(os.path.join(ASSETS, "ipa.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=0)
    open(os.path.join(ROOT, "tools", "ipa_missing.txt"), "w", encoding="utf-8").write(
        "\n".join(missing))
    n = len(words)
    print(f"IPA 覆盖: {len(result)}/{n} = {len(result)/n*100:.1f}% | 缺词 {len(missing)}(已列 tools/ipa_missing.txt)")
    for w in missing[:40]:
        print("  缺:", w)


if __name__ == "__main__":
    main()
