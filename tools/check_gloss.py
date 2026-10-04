# -*- coding: utf-8 -*-
"""校验 gloss 批文件:词覆盖 + 词性白名单(支持 n./vt./adv./prep. 斜杠组合)+ 内容含中文"""
import json, re, sys

WL = {"n","v","adj","adv","vt","vi","prep","pron","conj","aux","num","pl","abbr","art","int"}

def check(batch: str):
    want = open(f"tools/gloss_batches/{batch}.txt", encoding="utf-8").read().split()
    g = json.load(open(f"assets/glosses/{batch}.json", encoding="utf-8"))
    missing = [w for w in want if w not in g]
    extra = [k for k in g if k not in want]
    bad = []
    for k, v in g.items():
        if not isinstance(v, str) or " " not in v:
            bad.append((k, v)); continue
        pos = v.split(" ")[0]
        toks = [t.rstrip(".") for t in pos.split("/")]
        if not all(t in WL for t in toks) or not pos.endswith(".") \
           or not re.search(r"[\u4e00-\u9fff]", v) or len(v) > 48:
            bad.append((k, v))
    print(f"{batch}: 词单 {len(want)} | 词典 {len(g)} | 缺 {missing} | 多 {extra} | 格式错 {bad}")
    return not missing and not extra and not bad

ok = all(check(b) for b in sys.argv[1:])
sys.exit(0 if ok else 1)
