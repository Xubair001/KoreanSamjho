# -*- coding: utf-8 -*-
"""Content validation gate. Exits non-zero on any error, which fails the build.
No unverified or malformed content can reach production, because the database
cannot be built from it."""
import json, os, sys, glob, re, unicodedata
SRC = os.path.join(os.path.dirname(__file__), "..", "src")
errors, warnings = [], []
def err(c, m): errors.append(f"[{c}] {m}")
def warn(c, m): warnings.append(f"[{c}] {m}")

def load(name, key):
    p = os.path.join(SRC, name)
    if not os.path.exists(p): err("MISSING_FILE", name); return []
    return json.load(open(p, encoding="utf-8")).get(key, [])

vocab = []
for f in sorted(glob.glob(os.path.join(SRC, "vocabulary_*.json"))):
    vocab += json.load(open(f, encoding="utf-8"))["vocabulary"]
letters   = load("hangul_letters.json", "letters")
grammar   = load("grammar.json", "grammar")
sentences = load("sentences.json", "sentences")
interview = load("interview.json", "interview")
exams     = load("exams.json", "exams")
sources   = load("sources.json", "sources")
courses   = load("curriculum.json", "courses")
lessons   = load("curriculum.json", "lessons")
questions = load("questions.json", "questions")
passages  = load("questions.json", "passages")

LANGS = ("en", "ur", "hi")
VALID_LEVELS = {0,1,2,3,4}
VALID_POS = {"noun","verb","adjective","adverb","particle","expression","numeral","counter"}
VALID_REVIEW = {"draft","reviewed_ko","reviewed_full"}
EXAM_CRITICAL = {"reading","listening","grammar","vocab_ko_to_meaning","vocab_meaning_to_ko"}
HANGUL = re.compile(r"[가-힣ᄀ-ᇿ㄰-㆏]")
LATIN_OK = re.compile(r"^[a-zA-Z0-9 ,.\-'()?!:/]+$")

# 1. Globally unique IDs
seen = {}
for group, items in [("vocab",vocab),("letter",letters),("grammar",grammar),("sentence",sentences),
                     ("interview",interview),("exam",exams),("source",sources),("course",courses),
                     ("lesson",lessons),("question",questions),("passage",passages)]:
    for it in items:
        i = it.get("id")
        if not i: err("EMPTY_ID", f"{group} entry with no id"); continue
        if i in seen: err("DUPLICATE_ID", f"{i} in {group} and {seen[i]}")
        seen[i] = group

src_ids = {s["id"] for s in sources}

def check_langs(where, d, fields=LANGS):
    for l in fields:
        v = (d or {}).get(l)
        if v is None or (isinstance(v, str) and not v.strip()):
            err("MISSING_TRANSLATION", f"{where}: '{l}' missing or empty")

def check_review(where, it):
    rs = it.get("review_status")
    if rs not in VALID_REVIEW: err("BAD_REVIEW_STATUS", f"{where}: {rs!r}")
    ref = it.get("source_ref")
    if ref and ref not in src_ids: err("BROKEN_SOURCE_REF", f"{where}: {ref}")

# 2. Vocabulary
for v in vocab:
    w = f"vocab {v['id']}"
    if not HANGUL.search(v.get("korean","")): err("NOT_HANGUL", f"{w}: korean field has no Hangul")
    if v.get("level") not in VALID_LEVELS: err("BAD_LEVEL", f"{w}: {v.get('level')}")
    if v.get("pos") not in VALID_POS: err("BAD_POS", f"{w}: {v.get('pos')}")
    if not v.get("category"): err("EMPTY_FIELD", f"{w}: category")
    if not LATIN_OK.match(v.get("romanization","")): err("BAD_ROMANIZATION", f"{w}: {v.get('romanization')!r}")
    check_langs(w+" meaning", v.get("meaning"))
    ex = v.get("example") or {}
    if not HANGUL.search(ex.get("korean","")): err("NOT_HANGUL", f"{w}: example.korean has no Hangul")
    check_langs(w+" example", ex)
    check_review(w, v)

# 3. Hangul letters
kinds = {}
for l in letters:
    w = f"letter {l['id']}"
    if not HANGUL.search(l.get("char","")): err("NOT_HANGUL", f"{w}: char")
    check_langs(w+" sound", l.get("sound"))
    check_review(w, l)
    kinds[l["kind"]] = kinds.get(l["kind"],0)+1
expected = {"consonant":14,"double_consonant":5,"vowel":10,"compound_vowel":11}
for k,n in expected.items():
    if kinds.get(k) != n:
        err("HANGUL_SET_INCOMPLETE", f"expected {n} of {k}, found {kinds.get(k,0)}")

# 4. Grammar
for g in grammar:
    w = f"grammar {g['id']}"
    for f_ in ("pattern","structure","formality","title_en"):
        if not g.get(f_): err("EMPTY_FIELD", f"{w}: {f_}")
    if g.get("level") not in VALID_LEVELS: err("BAD_LEVEL", f"{w}: {g.get('level')}")
    check_langs(w+" explanation", g.get("explanation"))
    check_langs(w+" common_mistake", g.get("common_mistake"))
    exs = g.get("examples") or []
    if len(exs) < 1: err("NO_EXAMPLES", w)
    for n,e in enumerate(exs):
        if not HANGUL.search(e.get("korean","")): err("NOT_HANGUL", f"{w} example {n}")
        check_langs(f"{w} example {n}", e)
    check_review(w, g)

# 5. Sentences
for s in sentences:
    w = f"sentence {s['id']}"
    if not HANGUL.search(s.get("korean","")): err("NOT_HANGUL", w)
    check_langs(w, s.get("translation"))
    check_langs(w+" scenario_title", s.get("scenario_title"))
    if s.get("level") not in VALID_LEVELS: err("BAD_LEVEL", f"{w}: {s.get('level')}")
    check_review(w, s)

# 6. Interview
for q in interview:
    w = f"interview {q['id']}"
    check_langs(w+" question", q.get("question"))
    check_langs(w+" model_answer", q.get("model_answer"))
    if not HANGUL.search((q.get("question") or {}).get("korean","")): err("NOT_HANGUL", f"{w} question")
    if not HANGUL.search((q.get("model_answer") or {}).get("korean","")): err("NOT_HANGUL", f"{w} answer")
    check_review(w, q)

# 7. Exams — honesty rules
for e in exams:
    w = f"exam {e['id']}"
    check_langs(w+" name", e.get("name")); check_langs(w+" who", e.get("who"))
    check_langs(w+" scoring", e.get("scoring"))
    if e.get("confidence") not in {"verified","corroborated","assumption"}:
        err("BAD_CONFIDENCE", f"{w}: {e.get('confidence')}")
    if e.get("confidence") == "corroborated" and not e.get("caution"):
        err("UNCAUTIONED_CLAIM", f"{w}: corroborated exam facts must carry a caution note")
    secs = e.get("sections") or []
    if sum(s.get("questions",0) for s in secs) != e.get("total_questions"):
        err("SECTION_SUM_MISMATCH", f"{w}: section questions do not sum to total_questions")
    if sum(s.get("points",0) for s in secs) != e.get("total_points"):
        err("SECTION_SUM_MISMATCH", f"{w}: section points do not sum to total_points")

# 8. Curriculum referential integrity
course_ids = {c["id"] for c in courses}
vocab_cats = {v["category"] for v in vocab}
scenarios  = {s["scenario"] for s in sentences}
gram_ids   = {g["id"] for g in grammar}
for c in courses:
    check_langs(f"course {c['id']} title", c.get("title"))
    check_langs(f"course {c['id']} subtitle", c.get("subtitle"))
for l in lessons:
    w = f"lesson {l['id']}"
    if l.get("course_id") not in course_ids: err("BROKEN_REF", f"{w}: course {l.get('course_id')}")
    check_langs(w+" title", l.get("title"))
    sel = l.get("selector") or {}
    if l["kind"] == "vocab" and "category" in sel and sel["category"] not in vocab_cats:
        err("BROKEN_REF", f"{w}: no vocabulary in category {sel['category']}")
    if l["kind"] == "sentence" and sel.get("scenario") not in scenarios:
        err("BROKEN_REF", f"{w}: no sentences for scenario {sel.get('scenario')}")
    if l["kind"] == "grammar":
        for gid in sel.get("ids", []):
            if gid not in gram_ids: err("BROKEN_REF", f"{w}: grammar {gid}")
    # a lesson that would render empty is a broken lesson
    if l["kind"] == "vocab" and "level" in sel:
        if not [v for v in vocab if v["level"] == sel["level"]]:
            err("EMPTY_LESSON", f"{w}: no vocabulary at level {sel['level']}")

# 9. Questions
passage_ids = {p["id"] for p in passages}
for q in questions:
    w = f"question {q['id']}"
    opts = q.get("options") or []
    if len(opts) < 2: err("TOO_FEW_OPTIONS", w)
    ci = q.get("correct_index")
    if not isinstance(ci, int) or not (0 <= ci < len(opts)):
        err("MISSING_CORRECT_OPTION", f"{w}: correct_index {ci} out of range")
    for n,o in enumerate(opts):
        if not any((o.get(k) or "").strip() for k in ("korean","en","ur","hi")):
            err("EMPTY_OPTION", f"{w} option {n}")
    # options must be distinguishable
    keys = [tuple((o.get(k) or "") for k in ("korean","en","ur","hi")) for o in opts]
    if len(set(keys)) != len(keys): err("DUPLICATE_OPTIONS", w)
    check_langs(w+" explanation", q.get("explanation"))
    pid = (q.get("prompt") or {}).get("passage_id")
    if pid and pid not in passage_ids: err("BROKEN_REF", f"{w}: passage {pid}")
    if q.get("level") not in VALID_LEVELS: err("BAD_LEVEL", f"{w}: {q.get('level')}")
    if q.get("kind") in EXAM_CRITICAL and q.get("review_status") == "draft":
        err("DRAFT_IN_EXAM", f"{w}: draft content cannot be used in exam-critical questions")
    check_review(w, q)
for p in passages:
    if not HANGUL.search(p.get("korean","")): err("NOT_HANGUL", f"passage {p['id']}")
    check_review(f"passage {p['id']}", p)

# 10. Unicode hygiene across every user-facing string
def walk(o, path=""):
    if isinstance(o, dict):
        for k,v in o.items(): walk(v, f"{path}.{k}")
    elif isinstance(o, list):
        for i,v in enumerate(o): walk(v, f"{path}[{i}]")
    elif isinstance(o, str):
        for ch in o:
            if ch == "�": err("ENCODING_DAMAGE", f"{path}: U+FFFD replacement character")
            elif unicodedata.category(ch) == "Cc" and ch not in "\n\t":
                err("CONTROL_CHAR", f"{path}: {hex(ord(ch))}")
for name, items in [("vocab",vocab),("letters",letters),("grammar",grammar),("sentences",sentences),
                    ("interview",interview),("exams",exams),("questions",questions)]:
    walk(items, name)

# 11. Review-status reporting (warning, not error — this is the known Urdu/Hindi risk)
unrev = [i for i in (vocab+sentences+grammar) if i.get("review_status") != "reviewed_full"]
if unrev:
    warn("PENDING_NATIVE_REVIEW",
         f"{len(unrev)} items are not yet marked reviewed_full "
         f"(Urdu/Hindi native-speaker review pending). See docs/01-research.md 1.3.")

print(f"validated: {len(vocab)} vocab, {len(letters)} letters, {len(grammar)} grammar, "
      f"{len(sentences)} sentences, {len(interview)} interview, {len(questions)} questions, "
      f"{len(passages)} passages, {len(courses)} courses, {len(lessons)} lessons")
for wmsg in warnings: print("WARN ", wmsg)
if errors:
    print(f"\nFAILED with {len(errors)} error(s):")
    for e in errors[:60]: print("  ERROR", e)
    if len(errors) > 60: print(f"  ... and {len(errors)-60} more")
    sys.exit(1)
print("OK: all content validation rules passed")
