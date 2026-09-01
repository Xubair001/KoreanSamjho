# Phase 6–8 — Implementation, QA and Release

## 6.1 What was built

A complete, installable Android application, not a prototype.

| Area | State |
|---|---|
| Onboarding (language → country → goal track) | Built |
| Hangul course (40 letters, syllable builder, batchim) | Built |
| Vocabulary browse, categories, detail, favourites | Built |
| Grammar list + detail (structure, usage, examples, common mistakes) | Built |
| Sentences by scenario (10 scenarios) | Built |
| Practice hub + SRS-driven quiz runner | Built |
| Tests: quick, vocab, grammar, listening, reading, timed mock | Built |
| Question navigator, mark-for-review, submit confirmation | Built |
| Result screen with weak-area analysis and per-answer review | Built |
| Interview preparation with model answers | Built |
| Progress dashboard, streaks, XP, levels, achievements | Built |
| Global offline search across 4 languages | Built |
| Settings: language, track, theme, text size, romanization, reduced motion | Built |
| About: content sources, licences, privacy | Built |
| Exam info with confidence labelling and myth correction | Built |
| Korean TTS with slow playback and explicit failure handling | Built |

## 6.2 Technical QA — performed

| Check | Result |
|---|---|
| Kotlin compilation | Clean |
| Unit tests | **20/20 passing** (SRS scheduler 9, progress calculator 11) |
| Content validation gate | Passing; wired to `preBuild` so it blocks releases |
| Debug APK | Builds (22 MB unoptimised) |
| Release APK with R8 + resource shrinking | Builds, **3.99 MB** |
| Release signing | Verified, SHA-256 `cd607d35…` |
| `lintVitalRelease` | Passing (fixed a real backup-rules defect it found) |
| Permissions in release APK | **No `INTERNET` permission** — offline claim verifiable by inspection |
| Bundled assets | `content.db` 1.0 MB uncompressed; 3 fonts 1.86 MB |
| Multilingual FTS search | Verified: `water` / `پانی` / `पानी` / `mul` / `물` all resolve to 물 |
| Font subsetting | Noto Sans KR 10.4 MB → 424 KB per weight |
| Locale resources | en/ur/hi all 176 strings, **no missing and no extra keys** |

### On-device verification (Android 15 emulator, API 35)

Built, installed and driven end to end. Screenshots in [`screenshots/`](screenshots/).

| Flow | Result |
|---|---|
| Install release APK + cold launch | No crash, no exception in logcat |
| Onboarding EN → UR language switch | **Found and fixed a real bug**: the locale change recreates the activity, which reset onboarding to step 1. State is now `rememberSaveable`. |
| Urdu RTL layout | Correct — text right-aligned, progress bar fills from the right, Back/Continue mirrored, bottom nav mirrored |
| Nastaliq rendering | Correct, with the extra line-height; no clipping of descenders |
| Mixed-script bidi | Correct — `17`, `0 / 38`, `'son'`, `E-9`, `읽기` and romanization all stay LTR inside Urdu paragraphs |
| Track routing | Pakistan → EPS work goal offered; lesson count shows 38 (academic course correctly excluded) |
| Hangul lesson | ㅏ/ㅑ/ㅓ render with romanization, Korean letter name and Urdu sound description |
| Quiz | Question renders, **distractors drawn from the same category** (bank / bank account / remittance for 원) as designed |
| Answer feedback | Green + check icon + the word "درست" — colour is never the only signal |
| Tests screen | Practice-not-official notice shown prominently at the top, in Urdu |
| Exam info | EPS card shows the unverified-details warning, states Pakistan is included and India is not, and corrects the fixed-pass-mark myth |
| Missing-voice prompting (emulator, TTS engine disabled) | Verified end to end. Every audio control renders muted and stays tappable; tapping opens an explain-then-install dialog; a tappable banner appears on Home, in lessons, word detail, the quiz and the listening/mock runners; the Listening test tile is gated and states the problem in its subtitle. The dialog is status-aware — with the engine disabled it correctly said "Text-to-speech not available" with a single OK, instead of offering a voice download that would lead nowhere. Re-enabling the engine restored audio without a restart (`usableOffline=ko-kr-x-ism-local`). |
| Korean audio on a real device | **Found and fixed three real bugs.** (a) `setLanguage()` reported Korean available while the only voice was network-backed and not installed, so the button silently did nothing — availability now requires an installed, non-network voice. (b) The engine returned an empty voice list on first query and populated ~150 ms later, so the status is re-evaluated rather than latched. (c) Voice data installed while the app ran was invisible until a cold restart — the engine is now rebuilt on `onResume`. Verified after installing the Korean voice: `usableOffline=ko-kr-x-ism-local`, synthesis dispatched. |
| Korean synthesis (emulator) | `GoogleTTSServiceImpl: Synthesis request for locale kor-KOR` → `TTS dispatch: ko-kr-x-ism-seanet-embedded` |
| Language picker on a real device | **Found and fixed a real bug**: "اردو" rendered in fallback Naskh in the picker because the row used default typography. The picker is exactly where an Urdu reader who cannot read English must recognise their language, so language names now render in their own script's face. |

### Physical device — Redmi 13 (2404ARN45A, Android 16, 8 GB RAM, 8 cores, arm64-v8a)

Installed and measured on real hardware.

| Measurement | Result |
|---|---|
| **First cold start** (after `pm clear`, includes copying the 1 MB content DB out of assets) | **1 126 ms** |
| **Steady-state cold start** (process force-stopped each run, 5 runs) | 711 · 757 · 799 · 836 · 864 ms — **median 799 ms** |
| Total PSS | 89.9 MB |
| Java heap | 5.5 MB |
| Native heap | 11.4 MB |
| Crashes / exceptions | None |
| Bundled font rendering under MIUI's own font stack | Correct |

The first-launch figure is the important one: it includes the asset-database copy
and still comes in at 1.1 s, which validates the decision to ship a pre-built
SQLite database rather than importing JSON on first run.

**Caveat:** this handset has 8 GB of RAM, so it is mid-range, not the 2 GB low end
the app targets. These numbers are a strong signal, not a substitute for testing on
an entry-level device.

Installing needed the `pm install` route — MIUI rejects `adb install` with
`INSTALL_FAILED_USER_RESTRICTED` unless "Install via USB" is enabled, which
requires a Mi Account.

### Against the performance budget

| Budget | Target | Actual |
|---|---|---|
| Release APK | < 30 MB | **3.99 MB** |
| Content DB | small enough to ship | 1.0 MB |
| Cold start | < 2 s | **1.13 s first launch, 0.80 s steady state** on a Redmi 13 |
| Memory | modest heap | 5.5 MB Java heap, 89.9 MB PSS |

## 6.3 Language QA — status, stated honestly

**Korean:** authored and self-reviewed. Vocabulary verified against
한국어기초사전 (NIKL). Grammar attachment rules (은/는, 이/가, 을/를, ㅂ니다/습니다,
아요/어요, ㄹ-irregular) checked individually. Register is explicit on every
grammar pattern.

**Urdu and Hindi:** original work, written for natural register rather than
literal transfer. **Not yet reviewed by external native speakers.** All 255
affected items carry `review_status: reviewed_ko`, the validator reports the
count on every build, and the app itself tells the user that review is pending.

> This is the single largest known gap in the product. It is tracked, surfaced
> in-app, and must not be described as complete.

## 6.4 Not yet done

Stated plainly rather than implied:

- **Native-speaker review of Urdu and Hindi** (see above).
- **Instrumented UI tests.** Unit tests cover the domain engines; Compose UI and
  navigation tests are written into the strategy but not yet implemented.
- **Entry-level device testing.** Now measured on a Redmi 13 (8 GB) with good
  results, but not on a 2 GB Android Go class handset, which is the bottom of the
  target range. Frame timing under scroll has also not been profiled.
- **The MISSING_KOREAN_VOICE branch was verified by log, not by eye.** The device
  reached that state and the detection was confirmed from logcat, but the emulator
  could not be forced into it for a screenshot (only the whole engine could be
  disabled, which exercises the NO_ENGINE branch). The two branches differ only in
  which strings and buttons render.
- **Visual RTL audit by a native reader.** RTL and Nastaliq were verified
  working on-device across onboarding, home, lessons, quiz, tests and exam info
  (see above), but no native Urdu reader has reviewed the wording and layout
  screen by screen.
- **EPS-TOPIK section timings** remain unconfirmed (docs/01-research.md, Open
  Questions #1). The mock timer uses a documented practice timing, labelled.
- Recorded native audio, cloud sync, writing practice — deferred by design.

## 6.5 Release configuration

- `applicationId` `com.koreansamjho.app`, versionCode 1, versionName 1.0.0
- minSdk 24 (Android 7.0) · targetSdk 35 · compileSdk 35
- R8 full mode with resource shrinking; ProGuard rules for kotlinx.serialization and Room
- Adaptive launcher icon with monochrome layer (Android 13 themed icons)
- Splash screen via `core-splashscreen`
- `generateLocaleConfig` produces the Android 13+ per-app language entry
- Signed with a 4096-bit RSA key, 10,000-day validity
- `keystore.properties` and the `.jks` are git-ignored; the build degrades to
  unsigned if they are absent, so a fresh clone still builds

### Continuous integration

`.github/workflows/android.yml` runs on every push and pull request to `main`,
and can be triggered manually. Four jobs, gated in order:

| Job | Does | Artefact |
|---|---|---|
| `content` | Runs the content validator on its own, so bad educational data fails in seconds before any Gradle work | — |
| `lint` | `:app:lintDebug` | lint HTML + XML report |
| `test` | `:app:testDebugUnitTest` | JUnit HTML + XML results |
| `build` | Needs lint **and** test green, then builds debug and release APKs, asserts both exist, and dumps the release package name, label and permissions | both APKs, 30-day retention |

Two things worth noting:

- **The content gate cannot be bypassed.** `preBuild` depends on `buildContentDb`,
  which regenerates `content.db` from `content/src/` and aborts on any validation
  failure. `content.db` is therefore a build artefact and is **not** committed —
  CI builds the database from source on every run.
- **Signing is optional.** Signing material is never committed. If the repository
  secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`
  are set, the release APK is signed; otherwise the build still succeeds and
  produces `app-release-unsigned.apk`, so forks and pull requests are not blocked.

To enable signed release builds:

```bash
base64 -w0 koreansamjho-release.jks   # paste as the KEYSTORE_BASE64 secret
```

then add `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` alongside it.

**Verified by a clean-clone dry run** of every workflow step — fresh `git clone`,
no keystore, no `local.properties`, no build cache: content validation passed, lint
passed, 20/20 tests passed, and both APKs were produced
(`app-debug.apk` 21 MB, `app-release-unsigned.apk` 3.9 MB).

### Building

```bash
cd Korean Samjho
python3 content/tools/build_db.py      # validate + build content.db
python3 content/tools/build_fonts.py   # subset fonts (needs fonttools)
./gradlew :app:assembleRelease
```

## 6.6 Privacy

No account, no sign-in, no analytics, no tracking, and **no `INTERNET`
permission at all**. Progress never leaves the device. This is not a policy
promise — it is enforced by the manifest and verifiable with `aapt dump
permissions`.
