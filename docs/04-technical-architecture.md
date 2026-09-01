# Phase 4 — Technical Architecture

## 4.1 The decision that shapes everything: two databases

Korean Samjho's data splits cleanly along a line most apps blur:

| | Content | User progress |
|---|---|---|
| Written by | Us, at build time | The user, at runtime |
| Read/write | **Read-only on device** | Read-write |
| Size | Megabytes, grows | Kilobytes |
| Update | Replaced wholesale on app/content update | Must survive forever |
| Loss | Re-downloadable | **Catastrophic** |

So there are **two Room databases**:

- **`content.db`** — shipped pre-built in `assets/`, opened via Room's
  `createFromAsset()`, `JournalMode.TRUNCATE`, read-only.
- **`progress.db`** — created on device, holds everything the user generates.

**Why this matters.** It is what makes "content updates without destructive
migrations" (§16 of the brief) actually true rather than aspirational. Shipping
new content means swapping one asset file; user progress is in a different file
and is never touched. Progress rows reference content by **stable string IDs**
(`vocab.work.salary`), not autoincrement integers, so content can be
re-generated, reordered or renumbered without orphaning a single user record.

**Rejected alternative — JSON in assets, parsed into Room at first launch.**
This is the common pattern and it is wrong here: it costs a multi-second
first-run stall and a large heap spike on exactly the 2 GB devices we target,
duplicates the data on disk, and gains nothing.

## 4.2 Layers

```
presentation/   Compose UI · ViewModels · UI state · navigation
     ↓ (depends on)
domain/         Models · repository interfaces · engines (SRS, test, progress)
     ↑ (implemented by)
data/           Room (content + progress) · DataStore · TTS · content pipeline
```

Dependencies point inward. `domain` has no Android imports and is pure Kotlin,
so the SRS scheduler, scoring and progress logic are all unit-testable on the
JVM with no emulator. UI components hold no business logic; ViewModels expose a
single immutable `UiState` per screen via `StateFlow`.

**MVI-leaning MVVM**: one state object, explicit event functions. Full MVI with
a reducer/effect framework was rejected as ceremony this app does not need.

## 4.3 Dependency injection — manual, deliberately

`AppContainer` constructs and holds singletons; ViewModels get dependencies
through constructors via `viewModelFactory`.

**Hilt was rejected.** Room already requires KSP; adding Hilt adds a second
annotation-processing round to every build, ~100 KB and startup reflection
cost, for a single-module app with ~15 ViewModels. Constructor injection *is*
dependency injection — the framework is optional, and here it does not pay for
itself. If the app grows to multi-module, Hilt becomes worth revisiting; the
constructor-injected design migrates to it without redesign.

## 4.4 Module structure

v1.0 ships as a **single `:app` module with strictly layered packages**:

```
com.koreansamjho.app
├── Korean SamjhoApp.kt · MainActivity.kt
├── di/                 AppContainer, ViewModel factories
├── data/
│   ├── content/        ContentDatabase, DAOs, entities  (read-only)
│   ├── progress/       ProgressDatabase, DAOs, entities (read-write)
│   ├── prefs/          DataStore settings
│   ├── audio/          TtsController
│   └── repository/     Repository implementations
├── domain/
│   ├── model/          Pure Kotlin models
│   ├── repository/     Interfaces
│   └── engine/         SrsScheduler · TestEngine · ProgressCalculator
└── ui/
    ├── theme/          Colour · type · shape · custom tokens
    ├── components/     Shared composables
    ├── navigation/     Routes, NavHost
    └── screen/         One package per screen
```

**Why not multi-module now.** Gradle configuration and build overhead per
module is real, and module boundaries drawn before feature boundaries have
stabilised are usually wrong. The package layering above is the *same* boundary
a module split would use, so extraction later is mechanical (`:core:data`,
`:core:domain`, `:feature:*`). The trade is deliberate and reversible; what
would not be reversible is business logic leaking into composables, which the
layering forbids from day one.

## 4.5 Localization and RTL

The UI language is a **learning choice**, not a system setting — a user with an
English phone may want an Urdu interface. So:

- Strings live in standard `values/`, `values-ur/`, `values-hi/`, `values-ko/`
  and are switched with `AppCompatDelegate.setApplicationLocales()`.
- `MainActivity` extends `AppCompatActivity` — **required**: on API < 33
  androidx applies the per-app locale through the AppCompat delegate's
  `attachBaseContext`. With a plain `ComponentActivity` the selection silently
  fails to apply below Android 13, which would break the feature for most of
  our target devices.
- `generateLocaleConfig = true` produces the Android 13+ per-app language
  entry automatically from the resource folders.
- `android:supportsRtl="true"`; all layouts use `start`/`end`, never
  `left`/`right`, so Urdu mirrors correctly.

**Direction is per-string, not per-screen.** A Korean word inside an Urdu
sentence must stay LTR inside an RTL paragraph. Korean, romanization and
numerals are wrapped in an explicit LTR composition-local override rather than
inheriting the layout direction. This is where naive RTL implementations break.

## 4.6 Audio

`TtsController` wraps `android.speech.tts.TextToSpeech` for `Locale.KOREAN`,
exposing normal (1.0×) and slow (0.6×) rates and a `StateFlow<TtsStatus>`. It is a
process singleton (init is expensive) and is shut down with the process.

**Detecting whether Korean will actually speak is harder than it looks**, and three
findings from testing on a real Redmi 13 shaped this code:

1. **`setLanguage()` cannot be trusted.** The device returned
   `setLanguage = 0` (available) while the only Korean voice was
   `ko-kr-x-koc-network` — a network voice flagged `notInstalled`. Trusting that
   result produced an audio button that silently did nothing forever. Availability
   is therefore decided by inspecting `engine.voices`: there must be a Korean voice
   that is **installed** and **not network-required**. This app requests no
   `INTERNET` permission, so a network-backed voice can never synthesise here —
   only an embedded voice counts, and the controller explicitly selects it.
2. **The voice list is empty on the first query.** The same device reported
   `voices=[]` immediately after connecting and the full list of nine Korean voices
   ~150 ms later. So availability is re-evaluated rather than latched once.
3. **Voice data installed while the app is running is invisible to a live engine.**
   `refresh()` rebuilds the engine and is called on every `onResume`, so a learner
   who installs the Korean voice and returns gets working audio without a restart.

Failure is never a dead button. When no usable voice exists:

- every audio control stays tappable, shows a muted icon, and opens an
  **explain-then-install dialog** rather than dumping the user straight into an
  unfamiliar Android settings page — that matters for a first-time smartphone user,
  who is a core persona;
- a tappable **banner** appears on every screen where audio is part of the point:
  lessons, word detail, quiz, and the listening and mock test runners;
- the **listening test is gated** — it cannot function without audio, so tapping it
  explains why and offers the install instead of opening a silent paper;
- the dialog is **status-aware**: a device with no TTS engine at all is told exactly
  that, rather than being offered a voice download that would lead nowhere;
- a runtime synthesis error downgrades the status so the install path reappears.

`AudioRef` in the schema lets a recorded native asset override TTS per item
later, with no migration.

## 4.7 Offline strategy

There is no network code in v1.0. No `INTERNET` permission is requested at all
— which makes "works offline" verifiable by inspection rather than by promise,
and is itself a privacy statement (§31).

## 4.8 Performance budget

| Metric | Budget | How it is met |
|---|---|---|
| Cold start → interactive | < 2 s on 2 GB device | Pre-built DB (no first-run import); no DI reflection; lazy repositories |
| Release APK | < 30 MB | R8 + resource shrinking; vectors only; subset fonts; no raster art |
| Frame time | 60 fps on mid-range | `LazyColumn` with stable keys; immutable state; no work in composition |
| Memory | Modest heap | Paged/limited queries; no full-table loads; DB `mmap` |
| Battery | Negligible | No background work, no polling, no wakelocks |

FTS4 backs search so queries stay indexed rather than scanning `LIKE %x%`
across four language columns.

## 4.9 Testing

| Layer | Tool | Covers |
|---|---|---|
| Domain engines | JUnit, pure JVM | SRS intervals, scoring, level thresholds, streaks |
| ViewModels | JUnit + Turbine + coroutines-test | State transitions |
| DAOs / migrations | Robolectric + `room-testing` | Queries, schema, progress migrations |
| Content | JUnit + Python validator | Every rule in §4.10 |
| UI | Compose UI test | Navigation, RTL, answer flows |

## 4.10 Content validation (build gate)

`content/tools/validate.py` runs before `build_db.py` and **fails the build**
on: duplicate IDs · broken cross-references · missing `en`/`ur`/`hi`
translations · empty required fields · invalid level/category/POS enums ·
malformed Hangul (non-Hangul in a Korean field) · questions whose correct
option is absent · items marked exam-critical carrying `review_status: draft`.

No unverified content can reach production, because the database cannot be
built from it.
