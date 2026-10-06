# -*- coding: utf-8 -*-
"""例句批文件校验器:python tools/validate_batch.py sNN
检查:键集=待覆盖词集、结构、目标词在句中(大小写不敏感+词干容许屈折)、
批内重复、与已有批次重复、中文含汉字。"""
import json, re, sys, glob, os

name = sys.argv[1]
path = os.path.join("assets", "sentences", name + ".json")
batch = json.load(open(path, encoding="utf-8"))

words = json.load(open("assets/words.json", encoding="utf-8"))
combos = {c["w"] for c in json.load(open("assets/combos.json", encoding="utf-8"))}
done = set()
for f in sorted(glob.glob("assets/sentences/s*.json")):
    if os.path.basename(f) != name + ".json":
        done |= set(json.load(open(f, encoding="utf-8")))
todo = sorted(w for w in {x["w"] for x in words} if w not in done)
expected = set(todo[:250])

print("entries:", len(batch), "| keys==expected:", set(batch) == expected)
if set(batch) != expected:
    print("missing:", sorted(expected - set(batch))[:10])
    print("extra:", sorted(set(batch) - expected)[:10])

bad_struct = [w for w, v in batch.items() if not (isinstance(v, list) and len(v) == 2)]
print("bad structure:", bad_struct[:5])
bad_zh = [w for w, v in batch.items() if not re.search(r"[\u4e00-\u9fff]", v[1])]
print("zh-no-hanzi:", bad_zh[:5])

def stem_match(word, sent):
    s = sent.lower()
    wl = word.lower()
    if wl in s:
        return True
    toks = re.findall(r"[a-z]+", s)
    stem = wl[:-1] if wl.endswith("e") else wl
    return any(t == wl or (len(t) > len(stem) and t.startswith(stem) and len(t) - len(stem) <= 3) for t in toks)

bad = [w for w in batch if not stem_match(w, batch[w][0])]
print("word-not-in-sentence:", len(bad), bad[:8])
print("dups within:", len(batch) - len({v[0] for v in batch.values()}))
prev = set()
for f in sorted(glob.glob("assets/sentences/s*.json")):
    if os.path.basename(f) != name + ".json":
        prev |= {v[0] for v in json.load(open(f, encoding="utf-8")).values()}
print("overlap with other batches:", len([w for w in batch if batch[w][0] in prev]))
