# -*- coding: utf-8 -*-
"""Subsets the bundled Korean font to exactly the glyphs the app can display.

Every Korean character the app ever renders comes from content.db or from the
app's own string resources, so subsetting to that exact set is safe and cuts
Noto Sans KR from ~10 MB to a few hundred KB — a real download saving for users
on metered data. This runs as part of the content pipeline, so the font can
never drift out of sync with the content.

Noto Nastaliq Urdu is bundled unmodified: Nastaliq's correct shaping depends on
large ligature and mark-positioning tables, and the ~690 KB is not worth the
risk of subtly breaking Urdu rendering.
"""
import os, sqlite3, subprocess, sys, glob, re, shutil
HERE = os.path.dirname(os.path.abspath(__file__))
APP  = os.path.join(HERE, "..", "..", "app")
DB   = os.path.join(APP, "src", "main", "assets", "content.db")
FONT_DIR = os.path.join(APP, "src", "main", "res", "font")
SRC_KR = "/tmp/fonts/notokr.ttf"
SRC_UR = "/tmp/fonts/nastaliq.ttf"
os.makedirs(FONT_DIR, exist_ok=True)
os.makedirs("/tmp/fonts", exist_ok=True)

# Upstream sources, so a fresh clone can regenerate the bundled fonts.
SOURCES = {
    SRC_KR: "https://raw.githubusercontent.com/google/fonts/main/ofl/notosanskr/NotoSansKR%5Bwght%5D.ttf",
    SRC_UR: "https://raw.githubusercontent.com/google/fonts/main/ofl/notonastaliqurdu/NotoNastaliqUrdu%5Bwght%5D.ttf",
}
for path, url in SOURCES.items():
    if not os.path.exists(path):
        print(f"downloading {os.path.basename(path)} ...")
        import urllib.request
        urllib.request.urlretrieve(url, path)

# 1. Collect every character the app can render in Korean
chars = set()
db = sqlite3.connect(DB)
for (table, cols) in [
    ("vocab", ["korean", "ex_ko"]), ("letter", ["ch", "name_ko"]), ("grammar", ["pattern", "structure"]),
    ("grammar_example", ["korean"]), ("sentence", ["korean"]),
    ("interview", ["q_ko", "a_ko"]), ("passage", ["korean"]),
    ("question", ["p_ko", "audio_text"]), ("question_option", ["korean"]),
    ("exam", ["n_en", "n_ur", "n_hi", "s_en"]), ("meta", ["value"]),
]:
    for col in cols:
        for (v,) in db.execute(f"SELECT {col} FROM {table} WHERE {col} IS NOT NULL"):
            chars.update(v)
db.close()

# App's own resource strings may contain Korean too
for f in glob.glob(os.path.join(APP, "src", "main", "res", "values*", "strings.xml")):
    chars.update(open(f, encoding="utf-8").read())

# Keep only what a Korean font must carry, plus Latin/digits/punctuation for mixed runs
def keep(ch):
    c = ord(ch)
    return (0xAC00 <= c <= 0xD7A3      # Hangul syllables
            or 0x1100 <= c <= 0x11FF   # Hangul jamo
            or 0x3130 <= c <= 0x318F   # Hangul compatibility jamo
            or 0x0020 <= c <= 0x007E   # basic Latin
            or c in (0x2018,0x2019,0x201C,0x201D,0x2013,0x2014,0x00B7,0x2026,0x3001,0x3002))
subset = sorted({ch for ch in chars if keep(ch)})
# Always include the full jamo blocks so the Hangul lessons can render any letter
subset = sorted(set(subset) | {chr(c) for c in range(0x3130, 0x3190)} | {chr(c) for c in range(0x1100, 0x1200)})
unicodes = ",".join(f"U+{ord(c):04X}" for c in subset)
print(f"korean glyph set: {len(subset)} characters")

# 2. Instance the variable font to static weights (minSdk 24 predates variable font support)
def build(weight, out_name):
    tmp = f"/tmp/fonts/_inst_{weight}.ttf"
    subprocess.check_call([sys.executable, "-m", "fontTools.varLib.instancer",
                           SRC_KR, f"wght={weight}", "-o", tmp],
                          stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    out = os.path.join(FONT_DIR, out_name)
    subprocess.check_call([sys.executable, "-m", "fontTools.subset", tmp,
                           f"--unicodes={unicodes}", f"--output-file={out}",
                           "--layout-features=*", "--no-hinting", "--desubroutinize",
                           "--name-IDs=*", "--drop-tables+=DSIG"],
                          stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print(f"  {out_name}: {os.path.getsize(out)/1024:.0f} KB")
    os.remove(tmp)

print("building Korean font subsets:")
build(400, "noto_sans_kr_regular.ttf")
build(700, "noto_sans_kr_bold.ttf")

# 3. Urdu: instance to a single weight, keep all glyphs and layout tables intact
ur_out = os.path.join(FONT_DIR, "noto_nastaliq_urdu.ttf")
tmp = "/tmp/fonts/_ur.ttf"
subprocess.check_call([sys.executable, "-m", "fontTools.varLib.instancer",
                       SRC_UR, "wght=400", "-o", tmp],
                      stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
shutil.copy(tmp, ur_out)
print(f"  noto_nastaliq_urdu.ttf: {os.path.getsize(ur_out)/1024:.0f} KB (unsubset, shaping preserved)")

total = sum(os.path.getsize(os.path.join(FONT_DIR, f)) for f in os.listdir(FONT_DIR))
print(f"\ntotal bundled fonts: {total/1024:.0f} KB")
