# -*- coding: utf-8 -*-
"""Builds the read-only content.db shipped in app assets.

Runs validate.py first; a validation failure aborts the build."""
import json, os, sys, glob, sqlite3, subprocess, hashlib, datetime
HERE = os.path.dirname(os.path.abspath(__file__))
SRC  = os.path.join(HERE, "..", "src")
OUT  = os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "content.db")
CONTENT_VERSION = 1

rc = subprocess.call([sys.executable, os.path.join(HERE, "validate.py")])
if rc != 0:
    print("ABORT: content validation failed; database not built."); sys.exit(rc)

def load(name, key):
    return json.load(open(os.path.join(SRC, name), encoding="utf-8")).get(key, [])
vocab = []
for f in sorted(glob.glob(os.path.join(SRC, "vocabulary_*.json"))):
    vocab += json.load(open(f, encoding="utf-8"))["vocabulary"]
letters   = load("hangul_letters.json","letters")
grammar   = load("grammar.json","grammar")
sentences = load("sentences.json","sentences")
interview = load("interview.json","interview")
exams     = load("exams.json","exams")
sources   = load("sources.json","sources")
courses   = load("curriculum.json","courses")
lessons   = load("curriculum.json","lessons")
questions = load("questions.json","questions")
passages  = load("questions.json","passages")

if os.path.exists(OUT): os.remove(OUT)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
db = sqlite3.connect(OUT)
db.executescript("""
PRAGMA page_size=4096;
CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE source(id TEXT PRIMARY KEY, title TEXT, publisher TEXT, url TEXT, licence TEXT, note TEXT);
CREATE TABLE letter(id TEXT PRIMARY KEY, ch TEXT, kind TEXT, ord_ INTEGER, romanization TEXT,
  name_ko TEXT, name_rr TEXT, initial_sound TEXT, final_sound TEXT,
  sound_en TEXT, sound_ur TEXT, sound_hi TEXT, source_ref TEXT, review_status TEXT);
CREATE TABLE vocab(id TEXT PRIMARY KEY, korean TEXT, romanization TEXT, pos TEXT, level INTEGER,
  category TEXT, m_en TEXT, m_ur TEXT, m_hi TEXT,
  ex_ko TEXT, ex_rr TEXT, ex_en TEXT, ex_ur TEXT, ex_hi TEXT, source_ref TEXT, review_status TEXT);
CREATE TABLE grammar(id TEXT PRIMARY KEY, ord_ INTEGER, pattern TEXT, title_en TEXT, level INTEGER,
  formality TEXT, structure TEXT, e_en TEXT, e_ur TEXT, e_hi TEXT,
  mis_en TEXT, mis_ur TEXT, mis_hi TEXT, source_ref TEXT, review_status TEXT);
CREATE TABLE grammar_example(id INTEGER PRIMARY KEY AUTOINCREMENT, grammar_id TEXT, ord_ INTEGER,
  korean TEXT, romanization TEXT, en TEXT, ur TEXT, hi TEXT);
CREATE TABLE scenario(id TEXT PRIMARY KEY, title_en TEXT, title_ur TEXT, title_hi TEXT, level INTEGER);
CREATE TABLE sentence(id TEXT PRIMARY KEY, scenario TEXT, ord_ INTEGER, level INTEGER,
  korean TEXT, romanization TEXT, t_en TEXT, t_ur TEXT, t_hi TEXT, source_ref TEXT, review_status TEXT);
CREATE TABLE interview(id TEXT PRIMARY KEY, ord_ INTEGER, category TEXT,
  q_ko TEXT, q_rr TEXT, q_en TEXT, q_ur TEXT, q_hi TEXT,
  a_ko TEXT, a_rr TEXT, a_en TEXT, a_ur TEXT, a_hi TEXT, tip_en TEXT, source_ref TEXT, review_status TEXT);
CREATE TABLE exam(id TEXT PRIMARY KEY, code TEXT, track TEXT, n_en TEXT, n_ur TEXT, n_hi TEXT,
  w_en TEXT, w_ur TEXT, w_hi TEXT, total_questions INTEGER, total_points INTEGER, delivery TEXT,
  s_en TEXT, s_ur TEXT, s_hi TEXT, confidence TEXT, official_url TEXT,
  c_en TEXT, c_ur TEXT, c_hi TEXT);
CREATE TABLE exam_section(id INTEGER PRIMARY KEY AUTOINCREMENT, exam_id TEXT, ord_ INTEGER,
  name TEXT, questions INTEGER, points INTEGER, minutes INTEGER);
CREATE TABLE course(id TEXT PRIMARY KEY, track TEXT, level INTEGER, ord_ INTEGER,
  t_en TEXT, t_ur TEXT, t_hi TEXT, s_en TEXT, s_ur TEXT, s_hi TEXT);
CREATE TABLE lesson(id TEXT PRIMARY KEY, course_id TEXT, ord_ INTEGER,
  t_en TEXT, t_ur TEXT, t_hi TEXT, kind TEXT, selector TEXT, level INTEGER);
CREATE TABLE passage(id TEXT PRIMARY KEY, level INTEGER, track TEXT, korean TEXT, romanization TEXT,
  source_ref TEXT, review_status TEXT);
CREATE TABLE question(id TEXT PRIMARY KEY, kind TEXT, level INTEGER, track TEXT, category TEXT,
  p_ko TEXT, p_rr TEXT, p_en TEXT, p_ur TEXT, p_hi TEXT, passage_id TEXT,
  correct_index INTEGER, e_en TEXT, e_ur TEXT, e_hi TEXT, audio_text TEXT,
  source_ref TEXT, review_status TEXT);
CREATE TABLE question_option(id INTEGER PRIMARY KEY AUTOINCREMENT, question_id TEXT, ord_ INTEGER,
  korean TEXT, en TEXT, ur TEXT, hi TEXT);
CREATE INDEX idx_vocab_cat ON vocab(category);
CREATE INDEX idx_vocab_level ON vocab(level);
CREATE INDEX idx_sentence_scenario ON sentence(scenario);
CREATE INDEX idx_sentence_level ON sentence(level);
CREATE INDEX idx_lesson_course ON lesson(course_id, ord_);
CREATE INDEX idx_question_kind ON question(kind, level);
CREATE INDEX idx_question_track ON question(track, kind);
CREATE INDEX idx_question_cat ON question(category);
CREATE INDEX idx_qopt_q ON question_option(question_id, ord_);
CREATE INDEX idx_gex_g ON grammar_example(grammar_id, ord_);
CREATE INDEX idx_exsec ON exam_section(exam_id, ord_);
CREATE VIRTUAL TABLE search_fts USING fts4(entity_type, entity_id, korean, romanization, en, ur, hi, tokenize=simple);
""")

db.executemany("INSERT INTO source VALUES(?,?,?,?,?,?)",
    [(s["id"],s["title"],s["publisher"],s["url"],s["licence"],s["note"]) for s in sources])
db.executemany("INSERT INTO letter VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
    [(l["id"],l["char"],l["kind"],l["order"],l["romanization"],l["name_ko"],l["name_rr"],
      l["initial_sound"],l["final_sound"],l["sound"]["en"],l["sound"]["ur"],l["sound"]["hi"],
      l["source_ref"],l["review_status"]) for l in letters])
db.executemany("INSERT INTO vocab VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
    [(v["id"],v["korean"],v["romanization"],v["pos"],v["level"],v["category"],
      v["meaning"]["en"],v["meaning"]["ur"],v["meaning"]["hi"],
      v["example"]["korean"],v["example"]["romanization"],v["example"]["en"],
      v["example"]["ur"],v["example"]["hi"],v["source_ref"],v["review_status"]) for v in vocab])
db.executemany("INSERT INTO grammar VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
    [(g["id"],g["order"],g["pattern"],g["title_en"],g["level"],g["formality"],g["structure"],
      g["explanation"]["en"],g["explanation"]["ur"],g["explanation"]["hi"],
      g["common_mistake"]["en"],g["common_mistake"]["ur"],g["common_mistake"]["hi"],
      g["source_ref"],g["review_status"]) for g in grammar])
for g in grammar:
    db.executemany("INSERT INTO grammar_example(grammar_id,ord_,korean,romanization,en,ur,hi) VALUES(?,?,?,?,?,?,?)",
        [(g["id"],n+1,e["korean"],e["romanization"],e["en"],e["ur"],e["hi"]) for n,e in enumerate(g["examples"])])
scen = {}
for s in sentences:
    scen.setdefault(s["scenario"], (s["scenario_title"], s["level"]))
db.executemany("INSERT INTO scenario VALUES(?,?,?,?,?)",
    [(k,t["en"],t["ur"],t["hi"],lv) for k,(t,lv) in scen.items()])
db.executemany("INSERT INTO sentence VALUES(?,?,?,?,?,?,?,?,?,?,?)",
    [(s["id"],s["scenario"],s["order"],s["level"],s["korean"],s["romanization"],
      s["translation"]["en"],s["translation"]["ur"],s["translation"]["hi"],
      s["source_ref"],s["review_status"]) for s in sentences])
db.executemany("INSERT INTO interview VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
    [(q["id"],q["order"],q["category"],
      q["question"]["korean"],q["question"]["romanization"],q["question"]["en"],q["question"]["ur"],q["question"]["hi"],
      q["model_answer"]["korean"],q["model_answer"]["romanization"],q["model_answer"]["en"],
      q["model_answer"]["ur"],q["model_answer"]["hi"],q["tip_en"],q["source_ref"],q["review_status"]) for q in interview])
for e in exams:
    c = e.get("caution") or {}
    db.execute("INSERT INTO exam VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (e["id"],e["code"],e["track"],e["name"]["en"],e["name"]["ur"],e["name"]["hi"],
         e["who"]["en"],e["who"]["ur"],e["who"]["hi"],e["total_questions"],e["total_points"],e["delivery"],
         e["scoring"]["en"],e["scoring"]["ur"],e["scoring"]["hi"],e["confidence"],e["official_url"],
         c.get("en"),c.get("ur"),c.get("hi")))
    db.executemany("INSERT INTO exam_section(exam_id,ord_,name,questions,points,minutes) VALUES(?,?,?,?,?,?)",
        [(e["id"],n+1,s["name"],s["questions"],s["points"],s.get("minutes")) for n,s in enumerate(e["sections"])])
db.executemany("INSERT INTO course VALUES(?,?,?,?,?,?,?,?,?,?)",
    [(c["id"],c["track"],c["level"],c["order"],c["title"]["en"],c["title"]["ur"],c["title"]["hi"],
      c["subtitle"]["en"],c["subtitle"]["ur"],c["subtitle"]["hi"]) for c in courses])
db.executemany("INSERT INTO lesson VALUES(?,?,?,?,?,?,?,?,?)",
    [(l["id"],l["course_id"],l["order"],l["title"]["en"],l["title"]["ur"],l["title"]["hi"],
      l["kind"],json.dumps(l["selector"],ensure_ascii=False),l["level"]) for l in lessons])
db.executemany("INSERT INTO passage VALUES(?,?,?,?,?,?,?)",
    [(p["id"],p["level"],p["track"],p["korean"],p["romanization"],p["source_ref"],p["review_status"]) for p in passages])
for q in questions:
    pr = q["prompt"]
    db.execute("INSERT INTO question VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (q["id"],q["kind"],q["level"],q["track"],q["category"],
         pr.get("korean"),pr.get("romanization"),pr.get("en"),pr.get("ur"),pr.get("hi"),pr.get("passage_id"),
         q["correct_index"],q["explanation"]["en"],q["explanation"]["ur"],q["explanation"]["hi"],
         q.get("audio_text"),q["source_ref"],q["review_status"]))
    db.executemany("INSERT INTO question_option(question_id,ord_,korean,en,ur,hi) VALUES(?,?,?,?,?,?)",
        [(q["id"],n,o.get("korean"),o.get("en"),o.get("ur"),o.get("hi")) for n,o in enumerate(q["options"])])

# Unified offline search index: one query serves Korean, romanization, English, Urdu and Hindi.
rows = []
for v in vocab:
    rows.append(("vocab",v["id"],v["korean"],v["romanization"],v["meaning"]["en"],v["meaning"]["ur"],v["meaning"]["hi"]))
for s in sentences:
    rows.append(("sentence",s["id"],s["korean"],s["romanization"],
                 s["translation"]["en"],s["translation"]["ur"],s["translation"]["hi"]))
for g in grammar:
    rows.append(("grammar",g["id"],g["pattern"],"",g["title_en"]+" "+g["explanation"]["en"],
                 g["explanation"]["ur"],g["explanation"]["hi"]))
for l in letters:
    rows.append(("letter",l["id"],l["char"],l["romanization"],l["sound"]["en"],l["sound"]["ur"],l["sound"]["hi"]))
for q in interview:
    rows.append(("interview",q["id"],q["question"]["korean"],q["question"]["romanization"],
                 q["question"]["en"],q["question"]["ur"],q["question"]["hi"]))
db.executemany("INSERT INTO search_fts VALUES(?,?,?,?,?,?,?)", rows)

db.executemany("INSERT INTO meta VALUES(?,?)", [
    ("content_version", str(CONTENT_VERSION)),
    ("built_at", datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds")),
    ("vocab_count", str(len(vocab))), ("question_count", str(len(questions))),
    ("licence", "Content CC BY-SA 4.0. Korean lexical data verified against 한국어기초사전 (National Institute of Korean Language), CC BY-SA 2.0 KR."),
])
db.commit()
db.execute("VACUUM"); db.execute("ANALYZE"); db.commit()
n = lambda t: db.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0]
print("\ncontent.db built:")
for t in ("vocab","letter","grammar","grammar_example","sentence","scenario","interview","exam",
          "exam_section","course","lesson","question","question_option","passage","search_fts","source"):
    print(f"  {t:18s} {n(t):6d}")
db.close()
size = os.path.getsize(OUT)
print(f"\nsize: {size/1024:.0f} KB  sha256: {hashlib.sha256(open(OUT,'rb').read()).hexdigest()[:16]}")
print("path:", os.path.relpath(OUT, os.path.join(HERE,'..','..')))
