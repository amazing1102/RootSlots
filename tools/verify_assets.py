# -*- coding: utf-8 -*-
"""组合数据质量校验"""
import json, os, sys
ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "assets")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_data as B

words = json.load(open(os.path.join(ASSETS, "words.json"), encoding="utf-8"))
morphs = json.load(open(os.path.join(ASSETS, "morphs.json"), encoding="utf-8"))
combos = json.load(open(os.path.join(ASSETS, "combos.json"), encoding="utf-8"))
families = json.load(open(os.path.join(ASSETS, "families.json"), encoding="utf-8"))
wordset = {w["w"] if isinstance(w, dict) else w for w in words}

pm = {p["key"]: p for p in morphs["prefixes"]}
sm = {s["key"]: s for s in morphs["suffixes"]}
rm = {r["key"]: r for r in morphs["roots"]}
famk = {f["key"] for f in families}

errors = []
for c in combos:
    w = c["w"]
    if w not in wordset: errors.append(f"{w}: 不在词表")
    joined = "".join(s["s"] for s in c["segs"])
    if joined != w: errors.append(f"{w}: 拼接 {joined} != 单词")
    for s in c["segs"]:
        t, k = s["t"], s["k"]
        m = pm.get(k) if t == "P" else sm.get(k) if t == "S" else rm.get(k)
        if m is None: errors.append(f"{w}: 段 {t}:{s['s']} key={k} 无法解析")
        elif not (m.get("meaning") or "").strip(): errors.append(f"{w}: {t}:{k} 无释义")
    if c["family"] not in famk: errors.append(f"{w}: 家族 {c['family']} 不存在")

print(f"组合词 {len(combos)} | 错误 {len(errors)}")
for e in errors[:30]: print("  ", e)

# 覆盖率
used_r = {s["k"] for c in combos for s in c["segs"] if s["t"] == "R"}
used_p = {s["k"] for c in combos for s in c["segs"] if s["t"] == "P"}
used_s = {s["k"] for c in combos for s in c["segs"] if s["t"] == "S"}
print(f"使用词根 {len(used_r)}/{len(rm)+0} | 前缀 {len(used_p)} | 后缀 {len(used_s)}")
unused_r = [k for k in rm if k not in used_r]
print(f"未用词根({len(unused_r)}):", ", ".join(sorted(unused_r)[:25]))
# 模式分布
from collections import Counter
pc = Counter(c["pattern"] for c in combos)
print("模式分布:", dict(pc.most_common(12)))
