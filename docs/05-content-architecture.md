# Phase 5 — Content Architecture

## 5.1 The pipeline

```
content/src/*.json          authored, git-diffable, reviewable
        ↓
tools/validate.py           25+ rules · FAILS THE BUILD on violation
        ↓
tools/build_db.py           → app/src/main/assets/content.db  (SQLite + FTS4)
tools/build_fonts.py        → app/src/main/res/font/*.ttf     (subset to shipped glyphs)
        ↓
Gradle preBuild → validateContent → assembleRelease
```

Content is **never** hard-coded in UI code. Adding a lesson means editing JSON and
rebuilding the database; the app does not change. This is what makes content
updates cheap and lets Urdu/Hindi corrections ship without an engineering cycle.

The validator is wired into Gradle as `validateContent`, on which `preBuild`
depends — so **a release literally cannot be built from invalid content**.

## 5.2 Schemas

Every content record carries three fields that exist purely for accountability:

| Field | Purpose |
|---|---|
| `id` | Stable string key. Progress rows reference it, so content can be regenerated without orphaning user data. |
| `source_ref` | Points into `sources.json`. Every fact is traceable to where it was verified. |
| `review_status` | `draft` · `reviewed_ko` · `reviewed_full`. Gates exam-critical surfaces. |

`review_status` deserves emphasis. Phase 1 established that **no authoritative
open Korean→Urdu or Korean→Hindi resource exists**, so those translations are our
original work and our biggest accuracy risk. `reviewed_ko` means the Korean has
been verified but native Urdu/Hindi review is still pending; `reviewed_full` means
a native speaker has signed off. The app surfaces this honestly rather than
implying a confidence it has not earned.

### Vocabulary

```json
{
  "id": "vocab.safety.anjeonmo",
  "korean": "안전모", "romanization": "anjeonmo",
  "pos": "noun", "level": 3, "category": "safety",
  "meaning": { "en": "safety helmet", "ur": "حفاظتی ہیلمٹ", "hi": "सुरक्षा हेलमेट" },
  "example": {
    "korean": "안전모를 꼭 쓰세요.", "romanization": "anjeonmoreul kkok sseuseyo.",
    "en": "Be sure to wear a safety helmet.",
    "ur": "حفاظتی ہیلمٹ ضرور پہنیں۔", "hi": "सुरक्षा हेलमेट ज़रूर पहनें।"
  },
  "source_ref": "src.krdict", "review_status": "reviewed_ko"
}
```

### Grammar

Adds `pattern`, `structure`, `formality`, `explanation{en,ur,hi}`,
`examples[]`, and — unusually — **`common_mistake{en,ur,hi}`**. Teaching the
error is often more valuable than teaching the rule, and these are the errors
Urdu/Hindi speakers actually make.

### Sentence · Interview · Exam · Question

Sentences are grouped by `scenario`. Interview items pair a question with a model
answer plus a coaching `tip`. Exams carry `confidence` and `caution` so an
unverified claim can never be displayed as flat fact. Questions carry `options[]`,
`correct_index`, `explanation{en,ur,hi}` and an optional `audio_text` for
listening items.

## 5.3 Question generation

Vocabulary and listening questions are **generated** from the verified corpus
rather than authored separately. Two consequences that matter:

- A question can never contain a word that was not itself reviewed — it inherits
  the source item's `review_status`.
- **Distractors are drawn from the same category and part of speech**, so they
  are plausible. A "safety helmet" question offers "safety shoes" and "gloves",
  not "Tuesday". Trivially-wrong distractors teach nothing and inflate scores.

Grammar and reading items are hand-authored, because plausible wrong grammar
cannot be generated mechanically without teaching errors.

166 vocabulary items currently yield 498 generated questions; the ratio holds as
the corpus grows.

## 5.4 Database shape

`content.db` — 16 tables, read-only, 1.0 MB:

`meta · source · letter · vocab · grammar · grammar_example · scenario ·
sentence · interview · exam · exam_section · course · lesson · passage ·
question · question_option`, plus `search_fts`.

**One unified FTS4 index** backs global search across all four languages
simultaneously, so a single query answers "water", "پانی", "पानी", "mul" and
"물" — all of which resolve to 물. Verified working.

Indices on `vocab(category)`, `vocab(level)`, `sentence(scenario)`,
`lesson(course_id, ord_)`, `question(kind, level)` and `question(track, kind)`
keep every screen's query indexed rather than scanning.

## 5.5 Validation rules

The gate enforces, among others:

- Globally unique IDs across every content type
- `en`, `ur` and `hi` present and non-empty on every translatable field
- Korean fields actually contain Hangul (catches copy-paste and encoding errors)
- The Hangul set is complete — exactly 14/5/10/11 letters, or the build fails
- Romanization is Latin-only
- Every `source_ref` resolves to a real source
- Levels, parts of speech and review statuses are valid enum members
- Curriculum references resolve — no lesson can point at a category, scenario or
  grammar id that does not exist, and **no lesson may render empty**
- Every question has a correct option in range, ≥2 options, and no duplicates
- Exam section questions and points **sum to the stated totals**
- Any exam fact marked `corroborated` **must** carry a `caution` note
- No `draft` content in exam-critical question kinds
- Unicode hygiene: no U+FFFD replacement characters, no stray control characters

That last rule is not theoretical — it caught real encoding damage in an Urdu
string during authoring.

## 5.6 Current corpus

| Type | Count |
|---|---|
| Hangul letters | 40 (complete) |
| Vocabulary | 166 across 19 categories |
| Grammar patterns | 24 (+48 examples) |
| Sentences | 65 across 10 scenarios |
| Interview Q&A | 12 |
| Questions | 528 |
| Reading passages | 3 |
| Courses / lessons | 6 / 44 |
| Exams | 3 |

**This is a seed corpus, not a finished curriculum.** It is deliberately deep
where the product is differentiated (Hangul is complete; workplace, safety,
construction and health vocabulary are the strongest categories) and thin where
it must grow. The pipeline, not the corpus size, is the deliverable — adding
1,000 more words is authoring work that requires no code change.

## 5.7 Licensing

| Layer | Licence |
|---|---|
| Application source | Apache-2.0 |
| Content dataset | **CC BY-SA 4.0** (share-alike, inherited from krdict) |
| Noto Sans KR, Noto Nastaliq Urdu | SIL OFL 1.1 |
| Material Symbols | Apache-2.0 |

Korean lexical data is verified against 한국어기초사전 (NIKL, CC BY-SA 2.0 KR) and
attributed in-app under Settings → About → Content sources. **Every example
sentence, explanation, practice question and all Urdu and Hindi text is original
work** — krdict's own examples are largely drawn from copyrighted publications
and are not redistributable, so none are reused.
