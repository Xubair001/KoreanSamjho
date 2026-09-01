# Phase 3 — Design System

## 3.1 Naming

**Korean Samjho** — *samjho* is the imperative "understand" in both Urdu (سمجھو)
and Hindi (समझो), spelled and pronounced identically in each. The name reads as an
instruction in the learner's own language rather than in English, and it promises
comprehension rather than mere exposure, which matches a product whose whole point
is explaining Korean *through* Urdu and Hindi.

Renders as **کورین سمجھو** (UR) and **कोरियन समझो** (HI).

Alternatives considered: *Korean Seekho* ("learn Korean") — equally natural, but
collides with an existing Indian ed-tech app called **Seekho**; *Hansaathi*
(한 + ساتھی/साथी, "Korean companion") — more distinctive as a mark but needed
explaining; *Sikho Korean* — Hindi-leaning word order; *Annyeong* — several existing
apps.

**Trademark note:** "Samjho" is materially cleaner than "Seekho" in this category,
which is part of why it was chosen. A formal trademark search is still required
before Play Store submission. Flagged, not resolved.

## 3.2 Logo

**한 over three dots**, white on an indigo→violet gradient.

The reasoning matters more than the shape. The earlier mark was the letter **ㅎ**,
and that was a genuine design error: **ㅎ only reads as "Korean" to somebody who
already knows Hangul** — precisely the person this app does not exist for. A brand
mark for absolute beginners has to communicate before literacy.

So the mark carries two signals a non-reader can decode:

- **한** — the character for "Korea". Even a viewer who cannot read it recognises
  it as *Korean script*, which K-drama and K-pop exposure has made widely legible
  as a visual category across Pakistan and India.
- **Three dots** — a universally understood chat / speech / typing indicator.

Together they read as **"speak Korean"** without requiring any knowledge of Hangul.

The glyph outline is extracted from the bundled Noto Sans KR Bold (SIL OFL 1.1) by
[`content/tools/build_icon.py`](../content/tools/build_icon.py), so the letterform is
typographically correct rather than hand-approximated, and the whole icon set is
reproducible from one script. The script asserts the mark stays inside the
adaptive-icon safe zone (it spans y 24.0–83.6 of the 21–87 window) so it cannot be
clipped by an aggressive launcher mask.

Outputs: adaptive background, foreground, monochrome layer (Android 13 themed
icons), splash mark, and a 512×512 Play Store PNG.

**Icon palette.** Deliberately not green (Duolingo), orange (Memrise, Babbel) or
plain blue (Busuu, LingoDeer) so the icon is separable in a store grid of language
apps. `#1E3A8A → #7C3AED` diagonal, white mark, `#FF7A5C` coral dots carrying the
brand's warm accent. Verified legible at launcher size on a real device.

## 3.3 Colour

Not flag colours. A deep indigo drawn from Korean *dancheong* palettes, with a
restrained vermilion accent.

| Token | Light | Dark | Use |
|---|---|---|---|
| `primary` | `#2B4C8C` | `#A9C0F0` | Actions, active nav |
| `onPrimary` | `#FFFFFF` | `#0E2350` | |
| `primaryContainer` | `#D9E2FF` | `#123468` | Selected cards |
| `secondary` | `#4A5B7C` | `#B7C6E6` | Supporting |
| `tertiary` | `#8C4A3F` | `#F0B4A9` | Vermilion accent, streaks |
| `background` | `#FBFCFF` | `#111318` | |
| `surface` | `#FBFCFF` | `#111318` | |
| `surfaceVariant` | `#E1E2EC` | `#44464F` | Card fills, dividers |
| `error` | `#BA1A1A` | `#FFB4AB` | |
| `success` (custom) | `#2E6B41` | `#9BD5AC` | Correct answers |
| `warning` (custom) | `#8A5A00` | `#F2C25B` | Review due |

Success/warning are not in Material 3's scheme, so they are carried in a custom
`Korean SamjhoColors` object exposed via a `CompositionLocal` alongside the M3
scheme.

**Contrast:** every text/background pair meets WCAG AA (4.5:1 body, 3:1 large).
**Colour is never the only signal** — correct/incorrect always carry an icon
and a text label as well, which also covers colour-blind users (§26).

**Dynamic colour is deliberately off.** Material You would let a device
wallpaper recolour the app, which breaks the contrast guarantees we verify and
makes the brand unrecognisable. A learning app trades novelty for legibility.

## 3.4 Typography

| Role | Family | Size / weight |
|---|---|---|
| Display (Korean hero) | Noto Sans KR | 40sp / 700 |
| Korean word (list) | Noto Sans KR | 24sp / 600 |
| Korean sentence | Noto Sans KR | 20sp / 500 |
| Romanization | System | 14sp / 400, italic, muted |
| Screen title | System | 22sp / 600 |
| Body | System | 16sp / 400 |
| Urdu body | Noto Nastaliq Urdu | 18sp / 400, line-height ×1.9 |
| Hindi body | Noto Sans Devanagari | 16sp / 400, line-height ×1.5 |
| Caption | System | 13sp / 400 |
| Button | System | 15sp / 600 |

Two rules that matter more than the table:

- **Korean is always the visually dominant element** in any item that teaches
  Korean. Translation is support, set smaller and lower-contrast.
- **Nastaliq needs vertical room.** Its steep descending baseline clips at
  normal line heights. Urdu gets ×1.9 line-height and extra vertical padding —
  this is the single most common way apps get Urdu visibly wrong.

Font sizes respond to the user's system font scale and to an in-app size
control (§26), capped so layouts cannot break.

## 3.5 Spacing, shape, elevation

4 dp base grid: `xs 4 · sm 8 · md 16 · lg 24 · xl 32 · xxl 48`.
Screen gutter 16 dp; between cards 12 dp; inside cards 16 dp.

Corner radii: `sm 8 · md 16 · lg 24 · full 999`. Cards use `md`, sheets `lg`,
chips `full`.

Elevation is used sparingly — tonal surface colour is preferred over shadow,
which is cheaper to render on low-end GPUs and reads cleaner in dark mode.

**Touch targets are minimum 48 × 48 dp**, without exception.

## 3.6 Components

`HanCard` · `KoreanWordRow` (Korean + romanization + translation + audio button)
· `LessonCard` (progress ring + state) · `ProgressRing` · `StreakBadge` ·
`AnswerOption` (idle/selected/correct/incorrect, each with icon + label) ·
`AudioButton` (normal + slow) · `LevelChip` · `EmptyState` · `SectionHeader` ·
`StatTile` · `TestTimerBar` · `QuestionNavigatorGrid`.

## 3.7 Motion

Durations: 150 ms micro, 250 ms standard, 350 ms screen transition.
Easing: standard M3 emphasised curves.

Animated: screen transitions, progress fills, correct/incorrect feedback,
streak increments, achievement unlocks, card press.
Never animated: list scrolling content, text reflow.

**Reduced motion** (Settings, and honouring the system setting) replaces every
transition with a cross-fade ≤ 100 ms and disables all decorative motion. This
is both an accessibility requirement and a low-end-device performance win.

## 3.8 Iconography and illustration

Material Symbols (Apache-2.0) via `compose-material-icons-extended` only, plus
original vector illustrations authored as `VectorDrawable`. No raster
illustration, no scraped imagery, no third-party icon packs. Every asset is
either Apache-2.0 (Material) or original work, so the licence position is
trivially clean (§27).
