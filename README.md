# Korean Samjho — 한사티

[![Android CI](https://github.com/Xubair001/KoreanSamjho/actions/workflows/android.yml/badge.svg)](https://github.com/Xubair001/KoreanSamjho/actions/workflows/android.yml)

**Learn Korean in Urdu, Hindi or English. Free, offline, honest.**

A Korean-language learning app for Pakistani and Indian learners, built for the
low end of the Android market and for people preparing for real exams and real jobs.

```
Release APK   3.99 MB      No INTERNET permission
Content       1.0 MB SQLite, 16 tables, FTS4 across 4 languages
Tests         20/20 passing
minSdk 24 (Android 7.0)    Kotlin · Jetpack Compose · Material 3 · Room
```

## Why this exists

Research (see [`docs/01-research.md`](docs/01-research.md)) turned up three things
that shaped the whole product:

1. **Pakistan is one of the 17 EPS partner countries. India is not.** So the app
   asks where you are from and routes you to the exam that actually applies —
   EPS-TOPIK for Pakistan, TOPIK for India. Sending an Indian learner to EPS prep
   would be preparing them for a visa route that is closed to them.
2. **EPS-TOPIK has no fixed pass mark.** Selection is competitive and rank-based.
   Many apps confidently state "110/200 to pass". The app corrects this explicitly.
3. **There is no authoritative open Korean→Urdu or Korean→Hindi dictionary.** The
   Urdu and Hindi layer is original work, which makes it both the product's main
   contribution and its biggest accuracy risk — so every item carries a review
   status and the app is honest about it.

## Features

Hangul from zero · vocabulary in 4 languages with audio · grammar with common
mistakes · scenario sentences · spaced-repetition revision · listening practice ·
timed mock exams with weak-area analysis · interview preparation · progress,
streaks and achievements · offline search in Korean, English, Urdu and Hindi ·
light/dark themes · adjustable text size · reduced motion · full Urdu RTL.

## Build

Requires JDK 17, the Android SDK, and Python 3 with `fonttools`.

```bash
python3 content/tools/build_db.py       # validates content, then builds content.db
python3 content/tools/build_fonts.py    # subsets fonts to the shipped glyph set
./gradlew :app:assembleRelease
```

`preBuild` depends on the content validator, so **a release cannot be built from
invalid content**.

## Layout

```
app/          Android application (single module, strictly layered packages)
content/      src/ authored JSON · tools/ validate · build_db · build_fonts · curriculum · questions
docs/         Phases 1-8: research, product spec, design system, architecture, content, QA
```

## Documentation

| Doc | Contents |
|---|---|
| [01-research.md](docs/01-research.md) | Exam formats, licensing, competitors, sources, open questions |
| [02-product-spec.md](docs/02-product-spec.md) | Personas, goal tracks, MVP scope, IA, roadmap |
| [03-design-system.md](docs/03-design-system.md) | Naming, logo, colour, typography, components, motion |
| [04-technical-architecture.md](docs/04-technical-architecture.md) | Two-database design, layers, DI, RTL, audio, budgets |
| [05-content-architecture.md](docs/05-content-architecture.md) | Schemas, pipeline, validation rules, licensing |
| [06-qa-and-release.md](docs/06-qa-and-release.md) | What was built, QA results, **what is not yet done** |

## Licensing

Application code Apache-2.0 · content dataset CC BY-SA 4.0 · fonts SIL OFL 1.1 ·
icons Apache-2.0. Korean lexical data verified against 한국어기초사전 (National
Institute of Korean Language, CC BY-SA 2.0 KR) and attributed in-app. All example
sentences, explanations, questions and all Urdu and Hindi text are original.

## Status

Working, installable, signed. The known gaps — native-speaker review of Urdu and
Hindi, instrumented UI tests, physical low-end device measurement — are listed in
[docs/06-qa-and-release.md](docs/06-qa-and-release.md) §6.4 rather than glossed over.
