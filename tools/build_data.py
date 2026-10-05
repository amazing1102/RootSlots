# -*- coding: utf-8 -*-
"""M1 数据管线:解析四份 md -> assets/*.json (words/morphs/families/combos)
切分器:受图谱约束(词族+词根已知),DFS 找最优词素切分。
  - 精确模式匹配优先;失败则宽松回退(段数<=模式段数+1,须含家族根)。
  - 尾 e 吸收到最后一段;根面后可带连接元音。
"""
import json, os, re, sys
from collections import OrderedDict, defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "assets")
os.makedirs(ASSETS, exist_ok=True)

LINK_VOWELS = "aeiou"

# ---------- 词表清洗 ----------
WORD_FIX = {
    "accordingto": ["according"],
    "owingto": ["owing"],
    "fiarly": ["fairly"],
    "dependencedependable": ["dependence", "dependable"],
    "reformationreformist": ["reformation", "reformist"],
    "instalation": [],          # installation 已由斜杠行拆出
    "jewelery": [],             # jewellery 已由斜杠行拆出
}
MAP_FIX = {"instalation": "installation"}

def parse_words():
    words, seen = [], set()
    with open(os.path.join(ROOT, "cet_4_words.md"), encoding="utf-8") as f:
        for ln in f:
            ln = ln.strip()
            if not ln: continue
            parts = [p.strip() for p in ln.split("/")] if "/" in ln else [ln]
            for p in parts:
                if p in WORD_FIX:
                    for t in WORD_FIX[p]:
                        if t and t not in seen: seen.add(t); words.append(t)
                elif p and p not in seen:
                    seen.add(p); words.append(p)
    return words

# ---------- 词根词缀表 ----------
def parse_morphs():
    txt = open(os.path.join(ROOT, "cet4_roots_affixes.md"), encoding="utf-8").read()
    prefixes, suffixes, roots = [], [], []
    sec = None
    cur_group = ""
    for line in txt.splitlines():
        if line.startswith("## "):
            t = line[3:]
            if t.startswith("一"): sec = "P"
            elif t.startswith("二"): sec = "S"; cur_group = ""
            elif t.startswith("三") or t.startswith("四"): sec = "R"
            continue
        if sec == "S" and line.startswith("**") and line.rstrip().endswith("**") and ":" not in line:
            cur_group = line.strip().strip("*").replace('"', "").replace("表示", "").strip()
            continue
        if sec == "P" and line.startswith("|") and not line.startswith("|---") and not line.startswith("| 前缀"):
            cells = [c.strip() for c in line.strip("|").split("|")]
            if len(cells) < 3: continue
            surfaces = [s[:-1] for s in re.findall(r"[a-z]+-", cells[0])]
            if not surfaces: continue
            prefixes.append({"key": surfaces[0], "surfaces": surfaces,
                             "meaning": cells[1], "examples": cells[2]})
        elif sec == "S" and line.startswith("- **"):
            m = re.match(r"^-\s*\*\*(.+?)\*\*(?:\((.+?)\))?[:：](.*)$", line)
            if not m: continue
            variants, meaning, examples = m.group(1), m.group(2) or "", m.group(3)
            surfaces = [s.strip().lstrip("-") for s in variants.split("/")]
            surfaces = [s for s in surfaces if re.fullmatch(r"[a-z]+", s)]
            if not surfaces: continue
            if not meaning.strip(): meaning = cur_group
            suffixes.append({"key": surfaces[0], "surfaces": surfaces,
                             "meaning": meaning, "examples": examples.strip()})
        elif sec == "R" and line.startswith("|") and not line.startswith("|---") and not line.startswith("| 词根"):
            cells = [c.strip() for c in line.strip("|").split("|")]
            if len(cells) < 3: continue
            surfs = []
            for tok in cells[0].split("/"):
                tok = tok.strip()
                m = re.fullmatch(r"([a-z]+)\(([a-z]*)\)", tok)
                if m:
                    surfs.append(m.group(1))
                    if m.group(2): surfs.append(m.group(1) + m.group(2))
                elif re.fullmatch(r"[a-z]+", tok):
                    surfs.append(tok)
            if not surfs: continue
            roots.append({"key": surfs[0], "surfaces": surfs,
                          "meaning": cells[1], "examples": cells[2]})

    def merge(lst):
        out = OrderedDict()
        for e in lst:
            if e["key"] in out:
                o = out[e["key"]]
                for s in e["surfaces"]:
                    if s not in o["surfaces"]: o["surfaces"].append(s)
                if e["meaning"] and e["meaning"] not in o["meaning"]:
                    o["meaning"] = (o["meaning"] + ";" + e["meaning"]) if o["meaning"] else e["meaning"]
                for x in (e["examples"] or "").split(","):
                    x = x.strip()
                    if x and x not in o["examples"]:
                        o["examples"] = (o["examples"] + "," + x) if o["examples"] else x
            else:
                out[e["key"]] = dict(e)
        return list(out.values())
    return merge(prefixes), merge(suffixes), merge(roots)

# ---------- 手工补充(切分校正迭代) ----------
EXTRA_ROOT_VAR = {
    "st": ["st", "sta", "stat", "stant", "stan", "stit", "stitut", "stanc", "stin", "stall", "stand", "state", "store", "staur", "stud", "ist", "stabil"],
    "fac": ["fac", "fact", "fect", "fic", "fice", "fit", "fair", "fease", "feas", "feat", "fash", "facil", "fea"],
    "par": ["par", "part", "pear", "pair", "pars", "parti", "separat", "preparat"],
    "pon": ["pon", "pound", "pose", "posit", "pone", "pos"],
    "spect": ["spect", "spec", "spic", "pic"],
    "cap": ["cap", "capt", "cept", "ceit", "ceive", "ceiv", "cip", "cipi", "cup", "cupy"],
    "plic": ["plic", "plici", "ply", "plicit", "pli", "ploy", "plex", "pex"],
    "tain": ["tain", "tin", "tent", "tinu"],
    "press": ["press", "prise", "pres", "print"],
    "mot": ["mot", "mob", "mov"],
    "sum": ["sum", "sumpt"],
    "popul": ["popul", "publ", "pub", "pop"],
    "aud": ["aud", "audi", "audit"],
    "her": ["her", "hes"],
    "dict": ["dict", "dic"],
    "lect": ["lect", "leg", "lig", "leag", "llect"],
    "duc": ["duc", "duct", "duce"],
    "ven": ["ven", "vent", "veni"],
    "ced": ["ced", "ceed", "cess", "ancest"],
    "grad": ["grad", "gradu", "gress", "gree"],
    "pend": ["pend", "pens", "pond"],
    "quer": ["quer", "quisit", "quest", "quir"],
    "val": ["val", "vail"],
    "cord": ["cord", "cour", "courage"],
    "gen": ["gen", "gene", "gener", "genet", "generat", "genu"],
    "man": ["man", "manu", "main"],
    "mon": ["mon", "monit", "monstr"],
    "not": ["not", "nounc", "noun", "nounce", "nunci"],
    "spond": ["spons", "spond", "spont"],
    "hab": ["hab", "hibit", "hav"],
    "ver": ["ver", "vert", "vere", "cover"],
    "vers": ["vers", "vert", "vere", "vor", "vors", "verse"],
    "vit": ["vit", "viv", "vig", "vy"],
    "it": ["it", "unit"],
    "mod": ["mod", "modest"],
    "pet": ["pet", "pete", "petit"],
    "ple": ["ple", "plete", "pli", "plish"],
    "put": ["put", "putat", "pute"],
    "vi": ["vi", "vey", "voy"],
    "volv": ["volv", "volut"],
    "ag": ["ag", "act", "ig"],
    "cre": ["cre", "crease", "cresc", "crete"],
    "fend": ["fend", "fens", "fenc"],
    "mat": ["mat", "mature"],
    "sist": ["sist", "sista", "stant", "stit", "stitut"],
    "sid": ["sid", "sess", "sed", "side"],
    "fin": ["fin", "finit"],
    "struct": ["struct", "strue", "stru"],
    "reg": ["reg", "rig", "regul"],
    "min": ["min", "mini", "minis", "ministr"],
    "us": ["us", "ut", "use", "util", "ecut", "ecute", "beaut", "beauti"],
    "vis": ["vis", "vid", "view", "vey", "vise", "divi", "divid", "divis"],
    "sent": ["sent", "sens", "sensit"],
    "pot": ["pot", "poss", "potent"],
    "tract": ["tract", "treat", "train", "trail"],
    "mit": ["mit", "mitt", "miss", "mis"],
    "soci": ["soci", "soc"],
    "clud": ["clud", "clus", "clos"],
    "lig": ["lig", "li", "leag", "lyt"],
}

# 图谱小词族批量补(键与图谱一致;含 cid2/bank2 这类去重后缀键)
EXTRA_ROOT_VAR2 = {
    "ag": ["ag", "act", "ig", "age", "ager"],
    "agri": ["agri", "agr"],
    "alt": ["alt", "alter", "alti"],
    "astr": ["astr", "astro"],
    "audi": ["audi", "aud", "audit"],
    "aug": ["aug", "auth", "auct"],
    "bank2": ["bank"],
    "bar": ["bar", "barr"],
    "bat": ["bat", "batt"],
    "cand": ["cand", "candid"],
    "caus": ["caus", "cuse", "cuss", "cause", "cus"],
    "cel": ["cel", "ceal"],
    "celebr": ["celebr", "celebra"],
    "cent": ["cent", "centi"],
    "cern": ["cern"],
    "cert": ["cert", "certain"],
    "chiev": ["chiev", "chieve"],
    "cid2": ["cid", "cise"],
    "cis": ["cis", "cise"],
    "cit": ["cit", "cite"],
    "clar": ["clar", "clear"],
    "class": ["class", "classi"],
    "clin": ["clin", "cline"],
    "colon": ["colon", "coloni"],
    "count": ["count"],
    "cret": ["cret", "cre"],
    "cre": ["cre", "creas", "cruit"],
    "camp": ["camp", "champ", "champi"],
    "cad": ["cad", "cas", "cid"],
    "sorb": ["sorb", "sorp", "sorpt"],
    "sper": ["sper", "spair", "spers"],
    "crit": ["crit", "critic"],
    "cult": ["cult", "cultivat", "cumul", "cumulat"],
    "curr": ["curr", "cur"],
    "demn": ["demn"],
    "doc": ["doc", "doct"],
    "don": ["don", "band"],
    "dorm": ["dorm"],
    "dynam": ["dynam"],
    "eco": ["eco", "econ"],
    "electr": ["electr", "electri"],
    "fam": ["fam", "famil"],
    "fat": ["fat"],
    "fid": ["fid"],
    "fig": ["fig", "figur"],
    "flect": ["flect", "flex"],
    "flor": ["flor", "flour"],
    "flu": ["flu", "fluen"],
    "fort": ["fort", "forc", "force"],
    "frequ": ["frequ", "frequent"],
    "frig": ["frig", "friger", "frigerat"],
    "funct": ["funct"],
    "fund": ["fund", "found"],
    "fut": ["fut"],
    "geo": ["geo"],
    "gest": ["gest", "sug"],
    "gram": ["gram", "gramm"],
    "herit": ["herit", "heir"],
    "hon": ["hon", "onor"],
    "horr": ["horr"],
    "hospit": ["hospit", "host"],
    "hum": ["hum", "human"],
    "ide": ["ide"],
    "ident": ["ident", "identif"],
    "ign": ["ign", "nor", "cogn", "gn"],
    "junct": ["junct"],
    "jus": ["jus", "jur", "just", "jud"],
    "langu": ["langu"],
    "leis": ["leis"],
    "liber": ["liber", "libr"],
    "lic": ["lic", "licen"],
    "log": ["log", "apolog", "apologi"],
    "long": ["long", "leng"],
    "lustr": ["lustr"],
    "magn": ["magn", "magni", "magnet"],
    "mand": ["mand", "mend", "commend"],
    "mar": ["mar", "marr", "marit", "marv"],
    "meas": ["meas"],
    "medi": ["medi", "med"],
    "mer": ["mer", "mere", "merr"],
    "merc": ["merc", "merch"],
    "metr": ["metr", "met", "meter", "metre"],
    "milit": ["milit"],
    "mix": ["mix", "seem"],
    "mod": ["mod", "moder", "moderat"],
    "mor": ["mor"],
    "mut": ["mut", "mute", "mutu"],
    "nect": ["nect", "nex"],
    "opt": ["opt", "optim"],
    "or": ["or", "ora", "color", "orph", "vend"],
    "ord": ["ord", "ordin"],
    "ori": ["ori", "orig", "origin"],
    "oxy": ["ox", "oxy", "oxide"],
    "pac": ["pac", "pay", "peac"],
    "pact": ["pact"],
    "pan": ["pan", "pand", "pans"],
    "path": ["path", "patho"],
    "pect": ["pect"],
    "pel": ["pel", "pell", "puls"],
    "pen": ["pen", "penetr"],
    "phan": ["phan", "phas"],
    "phras": ["phras"],
    "pict": ["pict"],
    "pla": ["pla", "play", "empl", "plat"],
    "plan": ["plan"],
    "plas": ["plas", "plast"],
    "plaud": ["plaud", "plaus"],
    "ple": ["ple", "plete", "pleas", "plent"],
    "pleas": ["pleas"],
    "plor": ["plor"],
    "plur": ["plur"],
    "pol": ["pol", "poll", "lut", "polic", "polit", "polite"],
    "pot": ["pot", "poss", "pow"],
    "pract": ["pract", "practi"],
    "prehens": ["prehens", "preh", "hens", "pris", "prise", "prehend"],
    "pri": ["pri", "price", "prim", "pris"],
    "prim": ["prim", "princip"],
    "prov": ["prov", "prove", "prob", "probab"],
    "prox": ["prox", "proach"],
    "pun": ["pun"],
    "punct": ["punct", "point", "punc"],
    "quaint": ["quaint"],
    "qual": ["qual", "equ", "equa", "quali"],
    "quant": ["quant", "quanti"],
    "quart": ["quart", "equ", "equa"],
    "radic": ["radic"],
    "rap": ["rap", "rapid"],
    "ras": ["ras"],
    "rat": ["rat"],
    "rem": ["rem", "supre", "suprem"],
    "rend": ["rend"],
    "right": ["right"],
    "roll": ["roll"],
    "scend": ["scend", "scent"],
    "sect": ["sect", "seg"],
    "sen": ["sen"],
    "sil": ["sil"],
    "simil": ["simil", "sembl", "simul", "sim"],
    "sol": ["sol"],
    "strain": ["strain", "stress", "straint"],
    "sult": ["sult"],
    "sum": ["sum", "summ", "sumpt"],
    "sure": ["sure", "treas"],
    "tact": ["tact", "tach", "tack", "tamin"],
    "tal": ["tal"],
    "techn": ["techn", "techni", "techno"],
    "terr": ["terr", "terri", "territ"],
    "the": ["the", "theat", "therap"],
    "therm": ["therm", "thermo"],
    "tir": ["tir", "tire"],
    "toler": ["toler"],
    "tort": ["tort", "torn", "tour"],
    "tot": ["tot"],
    "tra": ["tra", "tray", "trad", "trast"],
    "trem": ["trem"],
    "turn": ["turn"],
    "typ": ["typ"],
    "ultim": ["ultim"],
    "und": ["und", "round"],
    "urg": ["urg"],
    "vac": ["vac", "vacu"],
    "van": ["van", "vanc", "vance", "vant"],
    "var": ["var", "vari"],
    "veget": ["veget"],
    "vel": ["vel", "velop", "veal"],
    "vinc": ["vinc", "vict", "victor"],
    "viol": ["viol"],
    "virt": ["virt", "virtu"],
    "vol": ["vol", "volum", "volunt"],
    "volv": ["volv", "volt", "volumin"],
    "vot": ["vot"],
}
# 独立补充词根/词缀(表里没有但切分需要)
EXTRA_ROOTS = [
    {"key": "point", "surfaces": ["point", "punct"], "meaning": "点、刺", "examples": "point, standpoint, viewpoint, punctual"},
    {"key": "per", "surfaces": ["per", "peri", "pir", "pert"], "meaning": "贯穿、完全、每、试验(同前缀 per-)", "examples": "experiment, experience, emperor"},
    {"key": "abil", "surfaces": ["abil", "abl"], "meaning": "能、适合(able)", "examples": "ability, able, enable"},
    {"key": "son", "surfaces": ["son"], "meaning": "声音", "examples": "person, personality"},
    {"key": "place", "surfaces": ["place"], "meaning": "场所、放置", "examples": "place, commonplace, replace"},
    {"key": "teen", "surfaces": ["teen"], "meaning": "十(十三~十九)", "examples": "teenager, thirteen"},
    {"key": "note", "surfaces": ["note"], "meaning": "注意、笔记", "examples": "note, banknote, notice"},
    {"key": "rupt", "surfaces": ["rupt"], "meaning": "破裂", "examples": "bankrupt, interrupt, erupt"},
    {"key": "ent", "surfaces": ["ent", "ess", "est"], "meaning": "存在(esse)", "examples": "absent, essence, interest"},
    {"key": "kilo", "surfaces": ["kilo"], "meaning": "千", "examples": "kilometer, kilogram"},
    {"key": "milli", "surfaces": ["milli"], "meaning": "千分之一、毫", "examples": "millimeter, millimetre"},
    {"key": "rid", "surfaces": ["rid"], "meaning": "跑(curr 变体)", "examples": "corridor"},
    {"key": "maj", "surfaces": ["maj", "major"], "meaning": "大", "examples": "major, majority, mayor"},
    {"key": "trem", "surfaces": ["trem"], "meaning": "颤抖", "examples": "tremble, tremendous, extreme"},
    {"key": "end", "surfaces": ["end"], "meaning": "末端、目的", "examples": "end, tremendous, intend"},
    {"key": "right", "surfaces": ["right"], "meaning": "直、正、右", "examples": "right, upright, copyright"},
    {"key": "crim", "surfaces": ["crim", "crimin"], "meaning": "鉴别、裁决(crimen)", "examples": "discriminate, crime"},
]
EXTRA_PREFIX_ENTRIES = [
    {"key": "co", "surfaces": ["co"], "meaning": "共同(与 com- 同源)", "examples": "cost, coincide"},
    {"key": "se", "surfaces": ["se"], "meaning": "分开、离开", "examples": "separate, select, persevere"},
    {"key": "intel", "surfaces": ["intel"], "meaning": "(inter- 变体)在…之间", "examples": "intelligence, intellectual"},
    {"key": "ana", "surfaces": ["ana"], "meaning": "向上、再、分析", "examples": "analyst, analytic, analysis"},
    {"key": "intro", "surfaces": ["intro"], "meaning": "向内、引入", "examples": "introduce, introduction"},
    {"key": "infra", "surfaces": ["infra"], "meaning": "在下、下部", "examples": "infrastructure"},
    {"key": "amb", "surfaces": ["amb", "ambi"], "meaning": "两边、周围", "examples": "ambition, ambiguous"},
    {"key": "apo", "surfaces": ["apo"], "meaning": "离开", "examples": "apology, apologise"},
    {"key": "extra", "surfaces": ["extra"], "meaning": "以外、超出", "examples": "extraordinary, extracurricular"},
    {"key": "contra", "surfaces": ["contra"], "meaning": "相反、相对", "examples": "contrary, contrast"},
    {"key": "i", "surfaces": ["i"], "meaning": "(in- 变体)进入、使", "examples": "isolate, ignite"},
]
# 给已有前缀行补同化变体
PATCH_PREFIX_VAR = {"dis": ["dif", "di"], "sub": ["sus"], "ad": ["al", "an"]}
EXTRA_SUFFIX = {
    "ing": "现在分词/动名词(…中、…的)",
    "ization": "…化(名词)",
    "isation": "…化(名词,英式)",
    "ism": "…主义、…学说",
    "ibility": "可…的性质",
    "ility": "…的性质",
    "ivity": "…的性质",
    "ular": "…的",
    "aneous": "…的",
    "id": "多…的、…的",
    "an": "…的、…之人",
    "ult": "词尾(结果)",
    "osity": "…的性质",
    "ies": "复数词尾/性质",
    "ce": "词尾(行为/结果)",
    "cel": "小(爱称/缩小)",
    "atus": "词尾(拉丁完成分词)",
    "ine": "…的、属于…的",
    "nomy": "法则、学科",
    "ium": "场所、集合(名词尾)",
    "ier": "…的人/物",
    "on": "词尾(名词)",
    "ey": "词尾",
    "el": "小(名词尾)",
    "ice": "行为/状态(名词尾)",
    "etic": "…的",
    "ily": "…地(副词尾)",
    "ety": "性质(名词尾)",
    "enza": "词尾",
    "re": "词尾(英式拼法)",
    "atory": "…的",
    "ivate": "动词尾(使…)",
    "um": "词尾(名词)",
    "ume": "词尾(名词)",
    "us": "词尾(拉丁名词)",
    "ior": "…的(比较级词尾)",
}
EXTRA_PREFIX = {}
KILL_ROOT_SURFACE = {"tel", "later", "nom", "perm", "prev", "radio", "rare", "reli", "tim", "str"}

# 从组合池排除(引擎硬凑、教学价值低,归入测验词池)
EXCLUDE_COMBOS = {"deadly", "early", "openly"}
# 词法上无法自动切分的词,手工指定段(兜底)
HAND_SEGS = {
    "analyst": [("P", "ana", "ana"), ("R", "ly", "lig"), ("R", "st", "st")],
    "virus": [("R", "vir", "vir"), ("S", "us", "us")],
    "acid": [("P", "ac", "ac"), ("R", "id", "cid2")],
    "acidity": [("P", "ac", "ac"), ("R", "id", "cid2"), ("S", "ity", "ity")],
    "priest": [("R", "pri", "pri"), ("S", "est", "est")],
    "principal": [("R", "princip", "prim"), ("S", "al", "al")],
    "missile": [("R", "miss", "mit"), ("S", "ile", "ile")],
    "existing": [("P", "ex", "ex"), ("R", "ist", "st"), ("S", "ing", "ing")],
    "champagne": [("R", "champ", "camp"), ("S", "agne", "agne")],
    "campaign": [("R", "camp", "camp"), ("S", "aign", "aign")],
    "tremendous": [("R", "trem", "trem"), ("R", "end", "end"), ("S", "ous", "ous")],
    "orphan": [("R", "orph", "or"), ("S", "an", "an")],
    "orphanage": [("R", "orph", "or"), ("S", "an", "an"), ("S", "age", "age")],
    "discriminate": [("P", "dis", "dis"), ("R", "crimin", "crim"), ("S", "ate", "ate")],
    "discrimination": [("P", "dis", "dis"), ("R", "crimin", "crim"), ("S", "ation", "ation")],
    "discriminatory": [("P", "dis", "dis"), ("R", "crimin", "crim"), ("S", "ator", "ator"), ("S", "y", "y")],
}

def build_lexicons(prefixes, suffixes, roots):
    P = {}
    for p in list(prefixes) + EXTRA_PREFIX_ENTRIES:
        for s in p["surfaces"]:
            P.setdefault(s, p["key"])
    S, S_MEAN = {}, {}
    for s_ in suffixes:
        for sf in s_["surfaces"]:
            S.setdefault(sf, s_["key"]); S_MEAN.setdefault(sf, s_["meaning"])
    for sf, mn in EXTRA_SUFFIX.items():
        S.setdefault(sf, sf); S_MEAN.setdefault(sf, mn)
    R = defaultdict(dict)
    all_roots = list(roots) + EXTRA_ROOTS
    for r in all_roots:
        for s in r["surfaces"]:
            R[r["key"]][s] = True
    for k, vs in EXTRA_ROOT_VAR.items():
        k = k.strip()
        if not k: continue
        for s in vs:
            R[k][s] = True
    for k, vs in EXTRA_ROOT_VAR2.items():
        for s in vs:
            R[k][s] = True
    all_root_surfaces = {}
    for k, d in R.items():
        for s in d:
            if s and s not in KILL_ROOT_SURFACE:
                all_root_surfaces.setdefault(s, set()).add(k)
    return P, S, S_MEAN, R, all_root_surfaces

def split_word(word, fam_key, max_segs, P, S, R, all_root_surfaces):
    """DFS 找最优切分。评分:段数少 > 根/前缀面总长大 > 后缀面总长大。"""
    best = None

    def cost(x):
        return len(x) + 2 * sum(1 for *_r, l in x if l)

    def better(a, b):
        if b is None: return True
        def key(x):
            lk = sum(1 for *_r, l in x if l)
            return (cost(x), -sum(len(s) for t, s, _k, l in x if t in "PR" and not l),
                    -sum(len(s) for t, s, _k, l in x if t == "S" and not l), lk)
        return key(a) < key(b)

    def dfs(pos, segs):
        nonlocal best
        if best is not None and cost(segs) > cost(best): return
        if pos == len(word):
            if len(segs) < 2: return
            # 族链验收收紧:必须存在词根/前缀段的 key 或词面直接等于族键。
            # 不再放行子串包含(really[fam=real] 曾被切成 re+al+ly 蒙混过关)。
            if any(t in ("P", "R") and (k == fam_key or s == fam_key) for t, s, k, _l in segs):
                if best is None or better(list(segs), best): best = list(segs)
            return
        if len(segs) >= max_segs: return
        has_r = any(t == "R" for t, _, _, _l in segs)
        has_s = any(t == "S" for t, _, _, _l in segs)
        cands = []
        if not has_r and not has_s:
            for s, k in P.items():
                if s and word.startswith(s, pos):
                    if pos + len(s) == len(word) and s in S:
                        continue    # 词尾同面优先后缀(如 -al/-en),不再给前缀解释
                    cands.append(("P", s, k, False))
        if not has_s:
            for s, keys in all_root_surfaces.items():
                if word.startswith(s, pos):
                    for k in keys: cands.append(("R", s, k, False))
                    if len(s) >= 2 and pos + len(s) < len(word) and word[pos + len(s)] in LINK_VOWELS:
                        ext = s + word[pos + len(s)]
                        for k in keys: cands.append(("R", ext, k, True))
            # 族键整段回退:族键能从当前位置整段匹配时,视为词根候选。
            # 修 real→re+al 类伪拆分:族键本身就是词面,整段上轴。
            if word.startswith(fam_key, pos):
                cands.append(("R", fam_key, fam_key, False))
        for s, k in S.items():
            if s and word.startswith(s, pos): cands.append(("S", s, k, False))
        cands.sort(key=lambda c: (0 if (c[0] == "R" and c[2] == fam_key) else 1, -len(c[1])))
        for t, s, k, _l in cands:
            segs.append((t, s, k, _l))
            dfs(pos + len(s), segs)
            segs.pop()
    dfs(0, [])
    return best

def pattern_counts(pat):
    p = pat.replace("(e)", "")
    counts = {"P": 0, "R": 0, "S": 0}
    for t in p.split("+"):
        counts[t.strip()] = counts.get(t.strip(), 0) + 1
    return counts

def main():
    words = parse_words()
    prefixes, suffixes, roots = parse_morphs()
    for k, extra in PATCH_PREFIX_VAR.items():
        for p in prefixes:
            if p["key"] == k:
                for s in extra:
                    if s not in p["surfaces"]: p["surfaces"].append(s)
    P, S, S_MEAN, R, all_root_surfaces = build_lexicons(prefixes, suffixes, roots)

    # --- 解析图谱(在 build_lexicons 之后,便于同样应用清洗) ---
    wordset = set(words)
    families, missing_words = [], []
    txt = open(os.path.join(ROOT, "cet4_word_formation_map.md"), encoding="utf-8").read()
    in2 = False
    for line in txt.splitlines():
        if line.startswith("## "):
            in2 = line.startswith("## 二"); continue
        if not in2: continue
        m = re.match(r"^-\s*\*\*(\S+)\s*(.*?)\*\*:(.*)$", line)
        if not m: continue
        key, meaning, body = m.group(1), m.group(2), m.group(3)
        wl = []
        for chunk in body.split("｜"):
            cm = re.match(r"\s*`([^`]+)`\s*(.*)", chunk)
            if not cm: continue
            pat, ws = cm.group(1), cm.group(2)
            for w in ws.split(","):
                w = w.strip()
                if not w: continue
                w = MAP_FIX.get(w, w)
                if w in EXCLUDE_COMBOS:
                    continue
                if w not in wordset:
                    missing_words.append(w); continue
                wl.append({"w": w, "pattern": pat})
        if wl: families.append({"key": key, "meaning": meaning, "words": wl})

    wordset = set(words)
    print(f"词表: {len(words)} 词 | 前缀 {len(prefixes)} | 后缀 {len(suffixes)} | 词根 {len(roots)+len(EXTRA_ROOTS)} | 词族 {len(families)}")
    combos, fail_exact, fail_all = [], [], []
    for fam in families:
        for item in fam["words"]:
            w, pat = item["w"], item["pattern"]
            cnt = pattern_counts(pat)
            total = cnt["P"] + cnt["R"] + cnt["S"]
            base = w[:-1] if w.endswith("e") else w
            # 两次尝试:原词 / 去尾 e(尾 e 归最后一段)
            results = []
            for target, add_e in ((w, ""), (base, "e") if w.endswith("e") else (w, "")):
                if add_e and not w.endswith("e"): continue
                segs = split_word(target, fam["key"], total + 1, P, S, R, all_root_surfaces)
                if segs:
                    if add_e:
                        t, s, k, l = segs[-1]
                        segs[-1] = (t, s + "e", k, l)
                    results.append((target == w, segs))
            if not results:
                if w in HAND_SEGS:
                    segs = [(t, s, k, False) for t, s, k in HAND_SEGS[w]]
                    got = pattern_counts("+".join(t for t, _, _, _l in segs))
                    combos.append({"w": w, "family": fam["key"], "map_pattern": pat,
                                   "pattern": "+".join(["P"] * got["P"] + ["R"] * got["R"] + ["S"] * got["S"]),
                                   "match": "hand",
                                   "segs": [{"t": t, "s": s, "k": k} for t, s, k, _l in segs]})
                    continue
                fail_all.append((w, fam["key"], pat)); continue
            # 优先精确模式匹配(段类型计数 == 模式计数),否则宽松
            exact = [segs for is_orig, segs in results
                     if [t for t, _, _, _l in segs].count("P") == cnt["P"]
                     and [t for t, _, _, _l in segs].count("R") == cnt["R"]
                     and [t for t, _, _, _l in segs].count("S") == cnt["S"]]
            relaxed = [segs for is_orig, segs in results]
            pick = exact[0] if exact else min(relaxed, key=len)
            tag = "exact" if exact else "loose"
            got = pattern_counts("+".join(t for t, _, _, _l in pick))
            got_pat = "+".join(f"{t}{'+'*0}" for t in ["P"] * got["P"] + ["R"] * got["R"] + ["S"] * got["S"])
            combos.append({"w": w, "family": fam["key"], "map_pattern": pat,
                           "pattern": got_pat, "match": tag,
                           "segs": [{"t": t, "s": s, "k": k} for t, s, k, _l in pick]})
            if not exact: fail_exact.append((w, fam["key"], pat, got_pat))

    fam_words = defaultdict(list)
    for c in combos: fam_words[c["family"]].append(c["w"])
    families_out = [{"key": f["key"], "meaning": f["meaning"],
                     "count": len(set(fam_words[f["key"]])),
                     "words": sorted(set(fam_words[f["key"]]))}
                    for f in families if f["key"] in fam_words]

    seen = set(); combos_d = []
    for c in combos:
        if c["w"] in seen: continue
        seen.add(c["w"]); combos_d.append(c)

    # 补充后缀并入输出;组合用到的词根 key 缺失时用词族释义合成
    fam_mean = {f["key"]: f["meaning"] for f in families}
    skeys = {s_["key"] for s_ in suffixes}
    for sf in sorted(set(S_MEAN) - skeys):
        suffixes.append({"key": sf, "surfaces": [sf], "meaning": S_MEAN[sf], "examples": ""})
    all_roots = roots + EXTRA_ROOTS
    rkeys = {r["key"] for r in all_roots}
    used_r = {sg["k"] for c in combos_d for sg in c["segs"] if sg["t"] == "R"}
    for k in sorted(used_r - rkeys):
        all_roots.append({"key": k, "surfaces": [k], "meaning": fam_mean.get(k, "词根"), "examples": ""})

    # 合并释义批文件 assets/glosses/*.json + 音标 assets/ipa.json(build_ipa.py 产物)
    import glob as _glob
    gloss_dir = os.path.join(ASSETS, "glosses")
    gmap = {}
    for f in sorted(_glob.glob(os.path.join(gloss_dir, "*.json"))):
        gmap.update(json.load(open(f, encoding="utf-8")))
    ipa_map = {}
    ipa_path = os.path.join(ASSETS, "ipa.json")
    if os.path.exists(ipa_path):
        ipa_map = json.load(open(ipa_path, encoding="utf-8"))
    else:
        print("(提示) 无 assets/ipa.json —— 先跑 tools/build_ipa.py 生成音标")
    words_out = [{"w": w, "g": gmap.get(w), "i": ipa_map.get(w) or ipa_map.get(w.lower())} for w in words]

    # 合并多义项释义 gd.json(ECDICT,build_gd.py 产物)+ 考试标签 exams.json
    gd_path = os.path.join(ASSETS, "gd.json")
    gd_map = json.load(open(gd_path, encoding="utf-8")) if os.path.exists(gd_path) else {}
    ex_path = os.path.join(ASSETS, "exams.json")
    ex_map = json.load(open(ex_path, encoding="utf-8")) if os.path.exists(ex_path) else {}
    # 合并例句批次 assets/sentences/*.json:{word: [英文句, 中文翻译]}
    sen_dir = os.path.join(ASSETS, "sentences")
    sen_map = {}
    for f in sorted(_glob.glob(os.path.join(sen_dir, "*.json"))):
        sen_map.update(json.load(open(f, encoding="utf-8")))
    # 合并扩展词库(build_dict.py 产物):新词条目 + 屈折吸收并入原形的考试标签
    ew_path = os.path.join(ASSETS, "exam_words.json")
    lt_path = os.path.join(ASSETS, "exam_lemma_tags.json")
    exam_words = json.load(open(ew_path, encoding="utf-8")) if os.path.exists(ew_path) else []
    lemma_tags = json.load(open(lt_path, encoding="utf-8")) if os.path.exists(lt_path) else {}

    def union_tags(cur, extra):
        if not extra:
            return cur
        return ",".join(sorted(set(cur.split(",")) | set(extra.split(",")) - {""}))

    for x in words_out:
        w = x["w"]
        x["gd"] = gd_map.get(w)
        x["x"] = union_tags(ex_map.get(w, "cet4"), lemma_tags.get(w.lower()))
        s = sen_map.get(w)
        x["se"] = s[0] if s else None
        x["sz"] = s[1] if s else None
    for e in exam_words:
        words_out.append({
            "w": e["w"], "g": e["g"], "i": e["i"],
            "gd": e["gd"], "x": union_tags(e["x"], lemma_tags.get(e["w"].lower())),
            "se": None, "sz": None,
        })
    n_gd = sum(1 for x in words_out if x["gd"])
    n_sen = sum(1 for x in words_out if x["se"])
    print(f"多义项 gd: {n_gd}/{len(words_out)} | 例句: {n_sen}/{len(words_out)}")
    n_g = sum(1 for x in words_out if x["g"])
    n_i = sum(1 for x in words_out if x["i"])
    n_gc = sum(1 for c in combos_d if gmap.get(c["w"]))
    print(f"释义: 词表 {n_g}/{len(words_out)} | 组合词 {n_gc}/{len(combos_d)} | 音标 {n_i}/{len(words_out)}")
    json.dump(words_out, open(os.path.join(ASSETS, "words.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    json.dump({"prefixes": prefixes + EXTRA_PREFIX_ENTRIES, "suffixes": suffixes,
               "roots": all_roots},
              open(os.path.join(ASSETS, "morphs.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    json.dump(families_out, open(os.path.join(ASSETS, "families.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    json.dump(combos_d, open(os.path.join(ASSETS, "combos.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    n_exact = sum(1 for c in combos_d if c["match"] == "exact")
    print(f"组合词: {len(combos_d)} | 精确模式 {n_exact} | 宽松 {len(combos_d)-n_exact}")
    print(f"词族输出: {len(families_out)} / {len(families)}")
    if missing_words: print("图谱引用但词表缺失(跳过):", sorted(set(missing_words)))
    print(f"完全失败 {len(fail_all)}:", [(w, k) for w, k, _ in fail_all][:30])
    print(f"宽松匹配 {len(fail_exact)} (抽查用):")
    for w, k, p, g in fail_exact[:50]:
        print(f"  {w} [{k}] 图谱{p} -> 实际{g}")

if __name__ == "__main__":
    main()
