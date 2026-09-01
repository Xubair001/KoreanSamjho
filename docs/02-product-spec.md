# Phase 2 — Product Specification

## 2.1 Positioning

> **Korean Samjho** — your Korean companion. Learn Korean in Urdu, Hindi or English.
> Free, offline, honest.

Three promises, each a direct response to a Phase 1 finding:

1. **Your language, not just English.** Urdu and Hindi are first-class, not
   afterthought translations. (Gap: every serious app is English-medium.)
2. **Works without internet.** Everything core is on-device after install.
   (Gap: the audience is frequently offline / on metered data.)
3. **Honest about the exams.** Practice tests are labelled practice. We never
   invent a pass mark or claim official status. (Gap: the category is full of
   confidently wrong exam claims — see §1.2.)

## 2.2 Personas

**Bilal, 24, Gujranwala — EPS candidate.** Intermediate school Urdu, limited
English. Has heard EPS-TOPIK is "the Korea test". Android Go phone, 2 GB RAM,
prepaid data. Needs: Hangul from zero in Urdu, workplace/safety vocabulary,
realistic CBT-style practice, and a truthful account of how selection works.
Fails with: English-only instructions, ads, anything requiring signup.

**Sana, 20, Pune — university applicant.** Fluent English, Hindi at home,
studying for TOPIK II for a Korean university scholarship. Mid-range phone.
Needs: grammar depth, reading passages, writing awareness, TOPIK-format mocks,
progress that shows level readiness. Is *not* served by EPS content and should
never be routed to it.

**Ayesha, 31, Karachi — returning learner.** Studied a little Korean from
YouTube. Wants structure and to stop forgetting. Needs: placement, spaced
revision, "what should I do today".

## 2.3 Goal tracks

Selected at onboarding (changeable in Settings), from country + goal:

| Track | Default for | Exam layer | Vocabulary emphasis |
|---|---|---|---|
| `EPS_EMPLOYMENT` | Pakistan + "work in Korea" | EPS-TOPIK practice | Factory, construction, agriculture, safety, workplace |
| `TOPIK_ACADEMIC` | India + "study/exam" | TOPIK I/II practice | Academic, formal, daily life, writing |
| `GENERAL` | Either + "everyday/culture" | None (self-assessment only) | Daily life, travel, culture |

Foundation content (Hangul, core grammar, core sentences) is shared by all
three. **The track changes emphasis and the exam module, never the accuracy or
the quality of the foundation.**

## 2.4 MVP scope

Committed for v1.0 — each item is either fully built or explicitly deferred,
never half-built:

- **Onboarding**: language choice (EN/UR/HI), country, goal → track.
- **Hangul course**: complete and authoritative — vowels, consonants, double
  consonants, compound vowels, batchim, syllable assembly, the major
  pronunciation-change rules. With TTS and interactive drills.
- **Vocabulary**: browsable by category and level, 4-language entries, TTS,
  favourites, detail view with example sentence.
- **Grammar**: pattern-based lessons with structure, usage, register, examples,
  and common mistakes, explained in the learner's language.
- **Sentences**: practical scenario-grouped sentences with full 4-language
  rendering and audio.
- **Practice**: flashcards + four exercise types driven by the SRS scheduler.
- **Smart Revision (SRS)**: due-today queue, difficult items, mistakes.
- **Tests**: quick quizzes and full timed mock exams matching the learner's
  track, with review and weak-area analysis.
- **Interview prep**: question bank with model answers and required vocabulary.
- **Progress**: dashboard, streak, per-skill breakdown, history.
- **Search**: offline full-text across all four languages.
- **Settings**: language, theme, font size, reduced motion, TTS setup, about,
  content sources, privacy.

**Explicitly deferred (and why):** cloud sync (needs a server; violates
zero-cost + privacy-minimal), speech recognition scoring (cannot be done
accurately — §1.5), community features (moderation cost), recorded native audio
(content project, schema-ready), handwriting practice.

## 2.5 Information architecture

```
Bottom navigation
├── Home        Dashboard: continue, due today, daily word/sentence, streak
├── Learn       Courses → Levels → Lessons  ·  Vocabulary · Grammar · Sentences
├── Practice    SRS queue · Flashcards · Listening · Exercises · Interview
├── Tests       Quick quiz · Mock exams (track-specific) · History
└── Progress    Skills · Streak · Achievements · Weak areas

Reachable from the above (not in bottom nav)
├── Search (global, from Home app bar)
├── Favourites / My List
├── Item detail (word · grammar · sentence)
├── Test runner → Test result → Answer review
├── Exam info (EPS-TOPIK / TOPIK — facts + official links)
├── Settings → Language · Appearance · Audio · Accessibility
└── About → Content sources · Licences · Privacy
```

Five tabs is the maximum for first-time smartphone users; anything more becomes
a scroll. Search lives in the Home app bar rather than a tab because it is a
verb, not a place.

## 2.6 Roadmap

**v1.0 (MVP)** — scope above, both tracks, three UI languages.
**v1.1** — recorded native audio for exam listening; expanded question bank;
Urdu/Hindi native review pass completed across all MVP content.
**v1.2** — content update packs downloadable without an app update (the schema
and versioning already support this); writing practice for TOPIK II.
**v2.0** — optional cloud sync (opt-in, still no mandatory account); additional
support languages (Bengali, Nepali, Pashto) — the schema is already
multilingual, so this is a content project, not a rewrite.

## 2.7 Success criteria

Not vanity metrics — these test whether the three promises hold:

- A learner who has never seen Hangul can read simple Korean words after the
  Level 0 course, measured by in-app assessment.
- Cold start to interactive Home on a 2 GB device: **< 2 s**.
- Full functionality with aeroplane mode on, from first launch after install.
- Zero content items reach users with `review_status = draft` on exam surfaces.
- Release APK **< 30 MB**.
