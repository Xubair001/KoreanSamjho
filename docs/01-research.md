# Phase 1 — Research

Status: complete for MVP scope · Last verified 2026-09-01

Every claim below is tagged with a confidence level:

- **[VERIFIED]** — confirmed against an official or authoritative source, cited.
- **[CORROBORATED]** — consistent across multiple independent secondary sources, no official confirmation obtained yet.
- **[ASSUMPTION]** — our judgement. Must not be presented to users as fact.

---

## 1.1 The finding that reshapes the product

The brief assumes Pakistani and Indian learners share one goal — Korean for
employment/visa. **They do not.** This is the single most important research
result and it drives the entire information architecture.

**Pakistan is one of the 17 EPS MOU countries. India is not.** [VERIFIED]

The Employment Permit System (EPS) places workers on E-9 visas from exactly 17
countries: Bangladesh, Cambodia, China, Indonesia, Kyrgyzstan, Laos, Mongolia,
Myanmar, Nepal, **Pakistan**, Philippines, Sri Lanka, Tajikistan, Thailand,
Timor-Leste, Uzbekistan, Vietnam. India is absent from this list.

Consequences:

| | Pakistan | India |
|---|---|---|
| Primary exam | **EPS-TOPIK** (employment, E-9) | **TOPIK I/II** (study, D-2/D-4, academic, professional) |
| Dominant motivation | Overseas employment income | University admission, Korean-firm employment in India, K-culture |
| Korean employers | Manufacturing, agriculture, fisheries, construction | Hyundai, Kia, Samsung, LG in-India operations; translation/BPO |
| Register needed | Workplace/safety Korean, blue-collar honorifics | Academic + business Korean, writing |

**If we shipped one undifferentiated "exam prep" module we would mis-serve
whichever audience it was not built for.** An Indian learner pushed toward
EPS-TOPIK is being prepared for a visa route that is not open to them — an
accuracy failure with real personal cost.

**Product decision:** the app asks for the learner's country and goal during
onboarding and routes them to a **goal track**. Shared foundation (Hangul,
core vocabulary, grammar, sentences) is common; the exam layer is not.

### TOPIK in India [VERIFIED]

TOPIK is administered in India by the Korean Cultural Centre India (New Delhi)
with its consulates and partner institutions. 2026 sittings: 5 per year.
Centres reported for 2026 include Delhi (Jawaharlal Nehru University),
Bengaluru (CMR University), Manipur (Manipur University), Chennai (Shri
Shankarlal Sundarbai Shasun Jain College for Women), and Pune. No eligibility
restriction and no cap on attempts. Fees approx. ₹1,200 (TOPIK I) / ₹1,500
(TOPIK II). [CORROBORATED — fees and centre lists change per sitting; the app
must not hard-code them.]

---

## 1.2 Exam formats

### TOPIK I — Levels 1–2 [VERIFIED]

| Section | Questions | Time | Points |
|---|---|---|---|
| Listening (듣기) | 30 | 40 min | 100 |
| Reading (읽기) | 40 | 60 min | 100 |
| **Total** | **70** | **100 min** | **200** |

Cut scores (total, not per section): **Level 1 ≥ 80**, **Level 2 ≥ 140**.
No writing section. All multiple choice, 4 options.

### TOPIK II — Levels 3–6 [VERIFIED]

| Section | Questions | Points |
|---|---|---|
| Listening (듣기) | 50 | 100 |
| Writing (쓰기) | 4 | 100 |
| Reading (읽기) | 50 | 100 |
| **Total** | **104** | **300** |

Cut scores: **L3 120–149 · L4 150–189 · L5 190–229 · L6 230–300**.
Total test time 180 minutes across two sessions. [CORROBORATED — the
per-session split (Listening+Writing, then Reading) is consistently reported
but we did not obtain it from topik.go.kr directly; see Open Questions.]

**There are no per-section minimums.** A weak section can be offset by a strong
one. This is unusual and worth teaching explicitly — it changes revision
strategy, and most learners assume the opposite.

### EPS-TOPIK [CORROBORATED — needs official confirmation]

| Section | Questions | Points |
|---|---|---|
| Reading (읽기) | 20 | 100 |
| Listening (듣기) | 20 | 100 |
| **Total** | **40** | **200** |

Delivered as **CBT** (computer-based) or **UBT** (tablet-based); paper
administration has been retired. Four-option multiple choice throughout.

**Critical correction to a widespread myth:** EPS-TOPIK has **no fixed pass
mark**. Selection is **relative** — candidates are ranked, and a sector
minimum floor (reported as 60 points manufacturing / 45 other sectors / 30 for
the fishing special category) only makes a candidate *eligible to be ranked*.
Passing the floor is not the same as being selected. Very many low-quality
apps and blogs state "pass mark = 110/200"; this is wrong and materially
misleads candidates about how hard they need to work.

**Product rule:** the app will state that EPS-TOPIK selection is competitive
and rank-based, will not display a fabricated pass mark, and will label our
scoring as a practice benchmark only.

**Open question (blocking for the EPS exam module, not for MVP):** the exact
current per-section time limits and total duration. Reported figures conflict
(25+25 min vs. longer). Resolution requires HRD Korea's official notice for
the specific round. Until confirmed, the app's EPS mock test uses a documented
practice timing and labels it as such.

---

## 1.3 Content sources and licensing

### National Institute of Korean Language (국립국어원) — the anchor source

**한국어기초사전 (Basic Korean Dictionary / krdict)** — a learner-oriented
dictionary of roughly 50,000 entries, published by NIKL.

**Licence: CC BY-SA 2.0 KR, effective 11 March 2019** [VERIFIED], covering
한국어기초사전, 표준국어대사전 and 우리말샘.

Two critical caveats [VERIFIED]:

1. **Example sentences drawn from publications or newspapers are *not* openly
   licensed** and are non-redistributable. Media files likewise.
2. **The 11 bilingual editions are Korean, English, Arabic, French, Indonesian,
   Japanese, Mongolian, Russian, Spanish, Thai, Vietnamese — Urdu and Hindi
   are not among them.**

### The consequence for this project

> **There is no authoritative, openly-licensed Korean→Urdu or Korean→Hindi
> lexical resource.** The Urdu and Hindi layer is original work. It is
> simultaneously this product's main contribution and its largest accuracy
> risk.

This directly determines the content architecture:

- Korean lexical data (headword, POS, sense) is **verified against krdict**,
  attributed, and our derived dataset is licensed **CC BY-SA 4.0** to satisfy
  share-alike. Application source code is separately licensed (Apache-2.0).
- **All example sentences are original**, authored by us. We never copy krdict
  examples, because a large share of them are not redistributable.
- **Every Urdu and Hindi string carries a `review_status`** field. Content that
  has not been reviewed by a native speaker is marked as such in the database
  and is gated out of exam-critical surfaces.

### Other sources evaluated

| Source | Use | Licence position |
|---|---|---|
| King Sejong Institute / 누리세종학당 | Curriculum sequencing reference, register norms | Materials are copyrighted. **Reference for structure only — no text reuse.** |
| EPS-TOPIK 표준교재 (standard textbook) | Vocabulary/topic scope reference | Copyrighted by HRD Korea. **We use it to scope topics, never to copy items.** |
| topik.go.kr past papers | Format and question-type reference | Publicly downloadable but copyrighted. **Format modelling only; every practice item is original.** |
| Revised Romanization of Korean (MCST, 2000/rev. 2014) | Transliteration standard | A government standard/algorithm — implemented, not copied. |
| Wiktionary / Wikipedia | Cross-check only | CC BY-SA. Never a primary authority. |

**Blanket rule:** authoritative sources are used for *verification and
structure*. Every user-facing sentence, question and explanation in Korean Samjho
is written for Korean Samjho.

---

## 1.4 Competitive landscape

Searching the EPS-TOPIK app category returns an unambiguous picture:

- The category is **dominated by Nepali-language apps** (e.g. "Eps-Topik Nepali
  Book", "Korean Eps-Topik Book"), reflecting Nepal's much larger and longer
  established EPS pipeline.
- The dominant format is a **scanned/─reflowed textbook** — effectively a PDF
  reader — rather than an interactive learning system.
- The consistent user complaints are **ad frequency**, **stale content**, and
  **missing pages** interrupting study flow.
- The consistent praise is for **offline access** and **learning in one's own
  language, free of charge**. Both are table stakes, not differentiators.

General-purpose apps (Duolingo, LingoDeer, Memrise, Drops, Eggbun) teach Korean
well but are **English-medium, subscription-driven, and have no EPS/TOPIK
alignment and no Urdu or Hindi support**.

**The gap:** there is no polished, ad-free, Urdu/Hindi-first, offline Korean
app that is honest about the exams. That gap is the product.

**Where we do not compete:** we will not out-gamify Duolingo, and we will not
attempt speech recognition scoring. See §1.5.

---

## 1.5 Technology research

### Audio — the biggest cost/licensing trap

| Option | Cost | Offline | Verdict |
|---|---|---|---|
| Bundled recorded audio | Free but needs paid/volunteer voice talent | Yes | Adds 100s of MB to APK. Rejected for MVP. |
| Cloud TTS (Google/Azure/Naver Clova) | **Per-character billing** | No | Violates the "no mandatory paid API" rule. Rejected. |
| **Android `TextToSpeech` (on-device)** | **Free** | **Yes**, once the Korean voice is installed | **Selected.** |

Android's platform TTS costs nothing, adds nothing to APK size, supports
speech-rate control (giving us slow/normal playback for free), and runs
offline once the Korean voice data is present. The failure mode —
`LANG_MISSING_DATA` / `LANG_NOT_SUPPORTED` on a device with no Korean voice —
is handled explicitly with a guided install prompt rather than silent failure.

This is a deliberate trade: synthesised Korean is good enough for word- and
sentence-level modelling, but it is **not** authentic exam listening audio. The
app will say so rather than implying its listening practice equals the real
exam recording. Recorded native audio for exam listening is a post-MVP content
project, and the schema already carries an `audio_asset` reference so recorded
audio can replace TTS per-item without a migration.

**Rejected: pronunciation scoring.** Doing it credibly needs forced alignment
against native recordings; doing it badly teaches learners wrong things and
would breach the accuracy rule. Out of scope — we do not ship a fake score.

### Typography

| Script | Font | Licence | Decision |
|---|---|---|---|
| Hangul | Noto Sans KR | SIL OFL 1.1 | Bundle a subset — Korean must never fall back |
| Urdu | **Noto Nastaliq Urdu** | SIL OFL 1.1 | **Bundle.** Android ships Naskh; Urdu readers strongly prefer Nastaliq |
| Devanagari | Noto Sans Devanagari | SIL OFL 1.1 | System font is reliable; bundle only if QA shows gaps |
| Latin | System / Noto Sans | — | System |

Bundling Nastaliq is non-negotiable for the Pakistani audience: Naskh-rendered
Urdu reads as foreign/低-quality to the target user even though it is legible.

### Stack

Confirmed available and cached locally, so the build is reproducible offline:
Gradle 8.9 · AGP 8.7.3 · Kotlin 2.0.21 · KSP 2.0.21-1.0.28 · Compose BOM
2024.10.01 · Room 2.6.1 · Navigation Compose · DataStore · kotlinx.serialization.
All Apache-2.0. Rationale and rejected alternatives: [`04-technical-architecture.md`](04-technical-architecture.md).

---

## 1.6 Target device profile

The app is built for the **low end of the Pakistani and Indian market**, not
for flagships: 2–3 GB RAM, eMMC storage, Android 7–13, frequently offline or
on metered data, often a shared device.

Design consequences, each of which shows up as a concrete architectural
decision later:

- Content ships as a **pre-built SQLite database**, not JSON parsed at first
  run — no multi-second first-launch stall, no large heap spike.
- **Offline-first is the default path**, not a degraded mode.
- **No account required.** Account creation is a real drop-off cliff and a
  privacy liability for this audience.
- **APK size is a feature.** Every megabyte is a download cost on metered data.

---

## 1.7 Open questions

| # | Question | Blocks | Handling until resolved |
|---|---|---|---|
| 1 | Exact EPS-TOPIK section timings for the current round | EPS mock timer fidelity | Documented practice timing, labelled as such |
| 2 | Official TOPIK II per-session time split | TOPIK II mock fidelity | Corroborated figures, flagged in-app |
| 3 | Native-speaker review capacity for Urdu/Hindi | Content review pipeline throughput | `review_status` gating; unreviewed content is visibly marked |
| 4 | Current TOPIK India centre list and fees | Exam info screen | Not hard-coded; links out to official site |

## Sources

- [Employment Permit System (EPS) — Global Skill Partnerships, CGD](https://gsp.cgdev.org/legalpathway/employment-permit-system-eps/)
- [Employment Permit System (EPS) — Embassy of the Republic of Korea to Pakistan](https://overseas.mofa.go.kr/pk-en/wpge/m_3167/contents.do)
- [EPS official portal](https://www.eps.go.kr/eo/langMain.eo)
- [TOPIK official site](https://www.topik.go.kr/)
- [TOPIK Test in India — dates, centres, fees](https://learnkorean.in/everything-about-topik-exams-in-india/)
- [How to Apply & Register for TOPIK in India](https://joyofkorean.com/register-topik-in-india/)
- [TOPIK Overview — TOPIK GUIDE](https://www.topikguide.com/topik-overview/)
- [TOPIK Levels & Scoring](https://info.topiklab.com/en/topik-scoring/)
- [TOPIK II Exam Guide](https://info.topiklab.com/en/topik-2/)
- [EPS-TOPIK exam day and scoring — Seoulstart](https://seoulstart.com/guides/eps-topik-exam-day-guide)
- [A Complete 2026 EPS TOPIK Test Guide](https://joyofkorean.com/eps-topik/)
- [NIKL dictionary data licence (CC BY-SA 2.0 KR) — spellcheck-ko/korean-dict-nikl](https://github.com/spellcheck-ko/korean-dict-nikl)
- [한국어기초사전 (krdict)](https://krdict.korean.go.kr/)
- [Basic Korean Dictionary — Wikipedia](https://en.wikipedia.org/wiki/Basic_Korean_Dictionary)
- [Eps-Topik Nepali Book — Google Play](https://play.google.com/store/apps/details?id=com.Topik.kiranchhetri.eps_topiknepalibook)
