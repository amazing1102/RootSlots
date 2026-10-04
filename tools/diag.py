# -*- coding: utf-8 -*-
"""诊断切分失败词:找出缺失的后缀面/词根面"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from collections import Counter
import build_data as B

words = B.parse_words()
prefixes, suffixes, roots = B.parse_morphs()
P, S, S_MEAN, R, all_rs = B.build_lexicons(prefixes, suffixes, roots)
wordset = set(words)

families, missing = [], []
txt = open(os.path.join(B.ROOT, "cet4_word_formation_map.md"), encoding="utf-8").read()
in2 = False
for line in txt.splitlines():
    if line.startswith("## "):
        in2 = line.startswith("## 二"); continue
    if not in2: continue
    m = B.re.match(r"^-\s*\*\*(\S+)\s*(.*?)\*\*:(.*)$", line)
    if not m: continue
    for chunk in m.group(3).split("｜"):
        cm = B.re.match(r"\s*`([^`]+)`\s*(.*)", chunk)
        if not cm: continue
        for w in cm.group(2).split(","):
            w = B.MAP_FIX.get(w.strip(), w.strip())
            if w and w in wordset:
                families.append((w, m.group(1), cm.group(1)))

# 现有 combos(上次成功切分的词不再诊断)
import json
done = set()
if os.path.exists(os.path.join(B.ASSETS, "combos.json")):
    done = {c["w"] for c in json.load(open(os.path.join(B.ASSETS, "combos.json"), encoding="utf-8"))}

tail_counter = Counter()
unmatched = []
for w, fam, pat in families:
    if w in done: continue
    found = False
    for i in range(0, len(w)):          # 前缀长度
        head = w[:i]
        if head and head not in P: continue
        for j in range(i + 1, len(w) + 1):   # 根长度(家族根)
            if w[i:j] in R.get(fam, {}) and (w[i:j] not in B.KILL_ROOT_SURFACE):
                tail = w[j:]
                # 尾巴应完全由后缀组成;找出第一个不在 S 里的位置
                k = len(tail)
                while k > 0 and tail[:k] not in S:
                    k -= 1
                rest = tail[k:] if k > 0 else tail
                if rest:
                    tail_counter[rest] += 1
                found = True
    if not found:
        unmatched.append((w, fam, pat))

print("=== 失败词中,家族根匹配后残留的非后缀尾巴(按频次) ===")
for t, n in tail_counter.most_common(60):
    print(f"  {n:3d}  {t}")
print()
print(f"=== 家族根都配不上的词 ({len(unmatched)}) ===")
for w, fam, pat in unmatched[:60]:
    print(f"  {w} [{fam}] {pat}")
