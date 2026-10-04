# -*- coding: utf-8 -*-
"""调试个别词的切分"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_data as B

words = B.parse_words()
prefixes, suffixes, roots = B.parse_morphs()
for k, extra in B.PATCH_PREFIX_VAR.items():
    for p in prefixes:
        if p["key"] == k:
            for s in extra:
                if s not in p["surfaces"]: p["surfaces"].append(s)
P, S, S_MEAN, R, all_rs = B.build_lexicons(prefixes, suffixes, roots)

CASES = [
    ("composer", "pon", "P+R+S"),
    ("suspicion", "spect", "P+R+S"),
    ("state", "st", "R+S"),
    ("execute", "us", "P+P+R"),
    ("retreat", "tract", "P+R"),
    ("committee", "mit", "P+R+S"),
]

def all_segs(word, fam, pat, maxsegs=6):
    out = []
    def dfs(pos, segs):
        if pos == len(word):
            if len(segs) >= 2 and any(
                k == fam or (t in ("P","R") and (s == fam or s.startswith(fam) or fam.startswith(s) or k.startswith(fam)))
                for t, s, k in segs):
                out.append(list(segs))
            return
        if len(segs) >= maxsegs: return
        cands = []
        for s, k in P.items():
            if s and word.startswith(s, pos): cands.append(("P", s, k))
        for s, k in S.items():
            if s and word.startswith(s, pos): cands.append(("S", s, k))
        for s, keys in all_rs.items():
            if word.startswith(s, pos):
                for k in keys: cands.append(("R", s, k))
                if len(s) >= 2 and pos + len(s) < len(word) and word[pos+len(s)] in B.LINK_VOWELS:
                    ext = s + word[pos+len(s)]
                    for k in keys: cands.append(("R", ext, k))
        for t, s, k in cands:
            segs.append((t, s, k)); dfs(pos+len(s), segs); segs.pop()
    dfs(0, [])
    return out

for w, fam, pat in CASES:
    res = all_segs(w, fam, pat)
    print(f"--- {w} [{fam}] {pat}: {len(res)} 个切分")
    for segs in sorted(res, key=lambda x: (len(x), -sum(len(s) for t,s,_ in x if t in 'PR')))[:6]:
        print("   ", " | ".join(f"{t}:{s}" for t, s, _ in segs))
