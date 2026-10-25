# Habit Radar — Full Build Specification (v5)

> Working title: **Habit Radar**. This v5 **replaces** all earlier versions (`habit-tracker-spec.md`, `-v2`, `-v3`, `-v4`). It is written for another AI/developer to build the whole app screen by screen as a **native Android app in Kotlin (Android SDK + Jetpack Compose)**.

A dark, calm, offline-first habit tracker for Android (iOS later). Soft pastel habit colors, serif headlines with one *italic* word, uppercase letter-spaced labels, floating pill tab bar.

---

## 0. Scope

### IN
1. **Today / Weekly / Monthly / Overall** tabs (floating bottom bar)
2. **New / Edit Habit** screen, **including per-habit notification settings**
3. **Habit Detail** screen with **deep analytics**
4. **Menu** screen (seven rows, see 5.7)
5. **Notifications** screen
6. **Settings** screen + **Home Studio** (theme, colors, home layout)
7. **Monthly Recap**
8. **Year in Review** (yearly recap)
9. Reorder habits / Reorder tabs screens
10. **Weekly Recap**
11. **Milestones** — big goals split into small steps (Goals list, Goal Detail, New/Edit Goal, Step editor)
12. **Home-screen widgets** (Android App Widgets built with Jetpack Glance) + in-app **Widgets** screen to preview and add them
13. **Backup & restore** (local export/import, automatic local backups)
14. **Archive & pause habits** (+ Archived habits screen)
15. **Onboarding + starter templates** (first-run flow and an ideas gallery)

### OUT (do NOT build)
Social sharing / share cards, What's new, Account & Sync / cloud accounts (local backup files ARE in scope), App lock (PIN/biometric), WhatsApp/community links, Rate the app, Share with friends, "PLUS" paywall and locks, "Receive your gift" promo banner, purple promo card at top of Menu.
**Everything is free. No accounts. No network access.** Data never leaves the phone.

---

## 1. Tech Stack (Native Android — Kotlin + Android SDK)

- **Language:** Kotlin 2.x, coroutines + Flow
- **UI:** Jetpack Compose + Material 3 (heavily customized to match the design), single-activity app, Navigation Compose (type-safe routes)
- **Architecture:** MVVM with unidirectional data flow (`ViewModel` + `StateFlow<UiState>`), layers `ui / domain / data`, Hilt for DI
- **Local DB:** Room (SQLite) with KSP, Flow-returning DAOs, exported schemas + migrations
- **Settings:** Preferences DataStore
- **Background & notifications:** `AlarmManager` (exact alarms) + `BroadcastReceiver`s + `WorkManager` for daily maintenance (details in 13.6)
- **Charts / heatmaps / rings:** custom `Canvas` composables (preferred) or Vico for line/bar charts
- **Home-screen widgets:** Jetpack Glance (`androidx.glance:glance-appwidget`, `glance-material3`), pinned from inside the app with `requestPinGlanceAppWidget` (see 5.16 and 13.8)
- **Dates:** `java.time` (Hijri via `HijrahChronology` Umm al-Qura, built into the Android SDK)
- **Fonts:** bundled in `res/font` (serif headline e.g. Fraunces / Instrument Serif; UI font DM Sans / Plus Jakarta Sans)
- **Sound & haptics:** `SoundPool` for the soft tick, Compose `HapticFeedback` / `VibrationEffect`
- **Localization:** English, Arabic (RTL), French via `res/values*`; per-app language with `AppCompatDelegate.setApplicationLocales`
- **SDK levels:** `minSdk 26` (Android 8.0), `targetSdk` and `compileSdk` = latest stable
- **Permissions:** `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`. **No `INTERNET` permission.**
- **Not allowed:** Firebase, analytics/ads SDKs, any network library
- Dark theme default; light and system supported
---

## 2. Design System

### 2.1 Core colors (approximate; tune by eye)

| Token | Value | Use |
|---|---|---|
| `bg` | `#0E0E11` | Page background under lists (almost black) |
| `app-bar` | `#1A1B21` (dark) | Header / app bar area |
| `card` | `#17181D` | Settings cards, neutral cards |
| `surface-2` | `#25262C` | Inactive segments, chips |
| `segment-selected` | `#0F1013` (white bold text) | Selected segment pill in 2–3 option controls |
| `text-primary` | `#F2F2F4` | Titles |
| `text-secondary` | `#8E8F98` | Subtitles, placeholders |
| `accent` (default) | `#E8A8F5` orchid | FAB, toggles ON, selected day chips, active tab text |
| `accent-container` | `#2E2540` | Active tab pill, selected "Count/Percent/Days" chip |
| `streak-fire` | orange 🔥 | Streak counters |

**Accent is user-selectable** (Home Studio → App color). Derive `accent-container` automatically (accent at ~20% over `bg`).

### 2.2 Habit color palette (per habit)
Two rows × 8+ pastel swatches, horizontally scrollable, ~46dp rounded squares radius 12:
- Row 1: lavender `#D6BEFA`, mint `#A8DFB4`, peach `#F5C9A5`, pink `#F2A8C8`, soft yellow `#F5DA8A`, lime `#C6E3A0`, teal `#7FC8BC`, periwinkle `#A5B0E0`, …
- Row 2: sky `#8FBDF0` (default), salmon `#F5B8AE`, aqua `#A0DDDD`, coral `#EE9A9A`, butter `#EEE18A`, sage `#8FBB96`, blue `#7DB4E8`, violet `#B79BE0`, …

**Card tint rule:** a habit card background = dark desaturated tint of the habit color (green → `#16261F`, purple → `#201A2E`, brown/orange → `#2C2018`). Icon tile = slightly lighter tint. Check circles/rings = saturated habit color at 60–70%.

### 2.3 Typography
- Headline: serif (Fraunces / Instrument Serif / DM Serif Display), ~34sp, one *italic* word.
- UI: DM Sans / Plus Jakarta Sans. Card titles 20sp medium.
- Section labels: UPPERCASE, letter-spacing ~3sp, 13sp, secondary color (`PREFERENCES`, `GENERAL`, `HABIT COLOR`).
- Settings group titles ("App bar", "Today layout", "Bottom bar", "Theme"): 22sp medium, white, left aligned outside the card.

### 2.4 Shapes
- Screen padding 16dp. Cards radius 28dp, padding 16–20dp, gap 12dp.
- Icon tile 64dp radius 20dp. Segmented controls radius 28dp, height 56dp (settings inline version: height 48dp).
- Toggles: Material 3 switch style; ON = accent track with white thumb; OFF = dark grey track with white thumb on left.
- Rows inside a card separated by 1dp hairline `#25262C`, inset 16dp.
- Floating bottom tab bar: pill, radius 32, 12dp above bottom.
- Sub-pages (Menu, Settings, Home Studio, Notifications, Recaps) use a **top app bar**: back arrow (←) + title (24sp) on `app-bar` color.

### 2.5 Motion
- Check toggle: fill + checkmark pop (200ms), light haptic, optional soft tick sound (Settings → Sounds).
- Full-screen celebration (when enabled): when the last habit of the day is done, a 1.5s confetti/glow overlay with serif text "All *done.*"; dismiss on tap.
- Bottom sheet slide-up 250ms; page transitions fade-through.

---

## 3. Navigation Map

```
Floating tab bar: Today | Weekly | Monthly | Overall   (reorderable, hideable)
 ├─ Header button ≡ (round outlined)  → Menu
 ├─ Header button +  (accent FAB)     → New Habit sheet
 ├─ Tap habit card                    → Habit Detail (deep analytics)
 │      └─ pencil                     → Edit Habit sheet
Menu
 ├─ Notifications (badge)  → Notifications screen
 ├─ Settings               → Settings → Home Studio, Reorder habits, Backup & restore, Archived habits, Welcome tour, Change language
 ├─ Weekly recap           → Weekly Recap
 ├─ Monthly recap          → Monthly Recap
 ├─ Year in Review         → Year in Review
 ├─ Milestones             → Goals list → Goal Detail → New/Edit Goal, Step editor
 └─ Widgets                → Widgets screen (previews, Add to home screen) → Widget configuration
Habit Detail → "Make this a goal" → New Goal (habit pre-linked)
```
The **≡ button opens Menu** (not a sort menu). Sorting lives in Reorder habits.

---

## 4. Main Tab Screens

### 4.1 TODAY

**Header** (visibility controlled by Home Studio → App bar):
- Date label: `SATURDAY 24 OCTOBER` (uppercase, letter-spaced). If "Show Hijri date" is ON, append the Hijri date, e.g. `SATURDAY 24 OCTOBER · <hijri day> <hijri month> <hijri year>`.
- Serif headline (if "Show greeting" ON): **"Good to *see you.*"** — varies: morning/afternoon/evening, "Almost *there.*" ≥ 75%, "All *done.*" at 100%.
- Right: round outlined **≡** button (→ Menu) and accent **+** FAB.

**Day-signal band** (if ON): one pill segment per habit scheduled today; done = accent, remaining = `#3A3B42`; ~10dp tall, fully rounded, equal widths, 8dp gap.
Caption under it depends on **Headline** setting:
- **Count:** `2 of 11 · 9 small marks left` (done: `All 11 done · well done`)
- **Percent:** `18% · 9 small marks left`
- **Days:** `12-day run · 9 small marks left` (run = consecutive days with ≥1 completion)

**Group chips** (shown only when at least one habit has a Group): a horizontal chip row under the caption — `All | Morning | Evening | …`. Selecting a chip filters Today (and Weekly / Monthly) to that group; the choice is remembered. If Home Studio → **Group habits into sections** is ON, the list instead shows small uppercase section headers (`MORNING`, `EVENING`) and the chips are hidden.

**Habit list** — layout chosen in Home Studio → **Today layout**:
1. **Classic** (default): full-width cards stacked.
   - Left icon tile 64dp; middle: title + streak row (🔥 + `1 Day` / `0 Days`; hide streak if disabled in Settings); right: 64dp check circle.
   - Done: dimmed card, title strikethrough, circle filled with check.
   - "Next up" (first incomplete): thin lavender outline ring.
2. **One thing**: shows a single large focus card (the next incomplete habit, big icon, title, big check button) with two thin line placeholders below showing the next two habits as small rows. Swipe card left/right to see the next/previous habit. After finishing, advance automatically.
3. **Ledger**: dense text list — thin rows (title left, small circle right, streak as small flame + number), no icon tiles, hairline separators.

**Paused habits** are not in the list or the day-signal band; instead a dimmed, collapsed row `Paused (2)` sits at the bottom of the list — tap to expand and show them with a **Resume** button.

Card density: **Comfortable** (card height ~112dp) vs **Compact** (~76dp, icon tile 44dp, smaller text).
Hide completed habits: when ON, done habits disappear from the list (still counted in band).

**Interactions**
- Tap circle: toggle done today.
- Tap card body: Habit Detail.
- Long-press: context menu (Edit, Skip today, Pause, Archive, Reorder, Delete).
- Swipe left: skip today (Undo snackbar).
- **Amount** habit: circle becomes progress ring + `3/8`; tap opens stepper sheet (−/+ and numeric input).
- **Time** habit: tap opens timer sheet (start/stop or manual hh:mm).

**Bottom:** floating pill tab bar (no icons, text only). Selected tab = accent text on `accent-container` pill. List bottom padding ≥ 120dp.

### 4.2 WEEKLY

- Label `19 OCT - 25 OCT` (week begins on the day set in Settings → Week start on: Saturday / Sunday / Monday). Swipe horizontally to change week.
- Headline **"A week that's *working.*"** (alternates: "A week to *begin.*", "A *strong* week.").
- Habit card (tinted): icon tile, title, streak 🔥 (if Weekly streak icon enabled), right-aligned small schedule text (`Mon, Thu, Sun` / `Everyday`).
- 7-column grid: day label + 44dp circle. **Today's column** has a taller rounded capsule behind it.
  - Pending scheduled: outlined ring. Done: filled + check. Not scheduled: faint dash ring, non-interactive. Missed (past): ring with low-opacity fill. Skipped: dashed ring.
  - Tap past/today circles to toggle (back-fill). Future locked.
- Tap card title area → Habit Detail.

### 4.3 MONTHLY (tab)

- Label `OCTOBER 2026`, headline **"A month in *motion.*"**, prev/next arrows.
- Per-habit card with mini calendar grid (7 columns, Monday/Saturday/Sunday start per setting), small circles filled in habit color if done, dim if not scheduled, today highlighted. Streak 🔥 shown if "Monthly" streak icon is ON.
- Tap a day circle: toggle done (back-fill allowed). Tap title: Habit Detail.
- Not the same as Monthly Recap (that is in Menu).

### 4.4 OVERALL (tab)

- Label `ALL TIME`, headline **"The *long* view."**
- Stat cards: total check-ins, best current streak, longest streak ever, overall completion rate, active habits.
- Bar chart: completion % per week, last 12 weeks.
- Per-habit list: icon, title, completion-rate bar (habit color), current / best streak. Tap → Habit Detail.

---

## 5. Other Screens

### 5.5 NEW / EDIT HABIT (bottom sheet, full height)

Top bar: **Cancel** (left) · `NEW HABIT` / `EDIT HABIT` (center, uppercase, letter-spaced) · **Save** pill (right; disabled grey until name is entered, then accent).

Body (scrollable):
0. *(New habit only)* **Browse ideas** pill at the very top → Template gallery (5.19); picking a template pre-fills every field below.
1. **Icon:** 135dp rounded tile with soft glow, default ☆ star icon; pill button **Change icon** (accent text) → icon picker (searchable grid ~200 icons by category + emoji tab).
2. **Name:** large centered serif field, placeholder **"Name it plainly"** (max 40 chars).
3. **Description:** centered small field "Add description" (optional, 200 chars).
4. **HABIT COLOR:** swatch grid (see 2.2); selected = inset + light-blue outer ring + check.
5. **TRACK:** segmented `Task | Amount | Time`.
   - Amount → target number, unit, comparison (at least / at most / exactly).
   - Time → target duration (hh:mm), comparison.
6. **REPEAT:** segmented `Daily | Weekly | Monthly`.
   - Daily: `On these days` + right label (`Everyday`, `Weekdays`, `Mon, Thu, Sun`), 7 chips Mon–Sun (selected = accent fill with dark text). At least 1 required.
   - Weekly: "X times per week" stepper (1–7).
   - Monthly: day-of-month grid (1–31) or "X times per month".
7. **REMINDERS** *(notification settings per habit)*:
   - Master toggle **Remind me** (default OFF).
   - When ON, list of reminder rows. Each row: time (tap → time picker), day chips (defaults to the habit's repeat days; can differ), delete icon. Button **+ Add reminder** (max 5).
   - **Message** (optional custom text; default "Time for {habit name}").
   - **Skip if already done** toggle (default ON).
   - **Remind again if not done after** dropdown: Off / 15 / 30 / 60 min (default Off, max 2 repeats).
   - **Sound** dropdown: Default / Soft / Silent. **Vibrate** toggle.
   - **Show quick actions** toggle: Done · Snooze · Skip buttons on the notification (default ON).
8. **MORE** (collapsed section): Start date, End date, Group (Morning / Evening / custom), Goal streak (number), "Ask for a note when completing" toggle.

Edit mode also shows three rows at the very bottom: **Pause habit** (opens the pause sheet, 5.18), **Archive habit** (confirmation; keeps all history) and a red **Delete habit** row. Delete shows: "This permanently deletes the habit and all its history. Archive it instead?" with **Archive** (primary) and **Delete forever** (red).
Validation: name required; Amount/Time need a target > 0.

### 5.6 HABIT DETAIL — DEEP ANALYTICS

Opened by tapping any habit. Top app bar: ← back · habit title (truncated) · 🔔 bell (scrolls to the REMINDERS card, section N; shows a small accent dot when this habit has active reminders) · pencil (→ Edit Habit) · ⋮ overflow (Skip today, Reset today, Pause / Resume, Archive / Restore, Add widget, Delete). Archived habits open here read-only with a banner `Archived on 12 Oct` and a **Restore** button.

Everything is tinted with the habit's color. Page is one scrollable column of cards. A **period selector** (segmented chips: `7D · 30D · 90D · 1Y · All`) sits at the top (sticky) and drives sections marked ⏱.

**A. Hero**
- Big icon tile, title (serif), description, schedule text (`Mon, Thu, Sun`), started date ("Since 12 Sep 2026 · 42 days").
- Today status button: large check/undo button.
- 4 stat tiles (2×2): **Current streak** 🔥, **Best streak**, **Completion rate** ⏱ (with ▲/▼ delta vs previous period), **Total check-ins**.
- **Momentum score** ring (0–100) with label (Cold / Warming / Strong / On fire) and a one-line hint ("Up 12 pts from last month").
- Streak-at-risk banner (if scheduled today, not done, and current streak > 0): "Your 🔥 12-day streak ends tonight."

**B. Next milestone**
Progress bar toward the next streak milestone (3, 7, 14, 21, 30, 50, 75, 100, 150, 200, 300, 365, 500, 1000): `9 / 14 days · 5 to go`, plus "Best ever: 21". A small text button **Make this a goal** opens New Goal with this habit pre-linked as a habit step. Goals and steps live in Milestones (5.15).

**C. Trend chart ⏱**
Line chart of completion rate per bucket (daily for 7D, weekly for 30D/90D, monthly for 1Y/All), with a 4-point moving-average overlay and dashed previous-period ghost line. Touch to see the value tooltip. Under it: best bucket, worst bucket.

**D. Calendar heatmap**
- Month calendar (nav arrows + month title) — cell states: done (filled), missed (hollow, tinted), skipped (dashed), not scheduled (dim), today (ring). Tap a day → day sheet (status, value, note, completed time, edit/delete entry).
- Toggle `Month | Year`: **Year** view = GitHub-style heatmap (53 columns × 7 rows), intensity = done/not.

**E. Day-of-week performance**
7 vertical bars (Mon–Sun in the user's week order) showing completion % per weekday; highlight best (green tint) and worst (red tint) bars; caption "You're strongest on Mondays (92%) and weakest on Fridays (48%)."

**F. Time-of-day pattern** *(only if ≥10 timestamped check-ins)*
Histogram by hour (0–23) of `completedAt`; highlight the peak window; text "Usually done around 05:30–06:30"; for habits with a reminder, show **average delay after reminder** in minutes.

**G. Streak history**
- Timeline list of all streaks sorted by length: start date → end date, length, and a horizontal bar (longest at 100%). Current streak marked "Active".
- Small chart: streak length over time (step chart).
- Stats: number of streaks, average streak length, longest gap (max consecutive missed scheduled days).

**H. Consistency & resilience**
- **Consistency score** (0–100) — see formula 7.
- **Bounce-back rate** — % of times a missed day was followed by a done day (next scheduled).
- **Perfect weeks** count (all scheduled days done) and perfect months.
- **Recovery time** — average number of scheduled days to get back after a miss.

**I. Volume chart ⏱**
Bar chart of check-ins per week (or per month for 1Y/All) with a dashed target line (scheduled count per bucket).

**J. Amount / Time analytics** *(only for Amount and Time habits)*
- Line chart of value per day with target line.
- Tiles: average, median, min, max, total, **goal-hit rate**, trend (slope per week, ▲/▼ with %).
- Distribution histogram of values.
- Personal best (value + date).

**K. Miss analysis**
- Most-missed weekday, and "after which habit/day" patterns when computable (e.g. "You miss this 70% of the time after a skipped day").
- List of the last 5 misses with date; tap → day sheet.
- Skipped days count.

**L. Insights** (auto-generated text cards, max 5; see rules in section 7.3)
Examples: "You're 38 pts stronger on Mondays than Fridays", "Your best month was March (96%)", "3 more days to beat your record", "Completion is trending up for 4 weeks in a row".

**M. Notes & history**
- Notes list (date + text) if any notes exist.
- Full **history log** (paginated): date, status chip, value, time. Tap row → edit entry. Button "Add missing entry" for back-filling.

**N. Reminders (create and manage right on this screen)**
A card titled `REMINDERS` (same card style as the other sections, tinted with the habit color). No need to open Edit Habit.
- **Header row:** title + master toggle **Remind me** (turning it ON for the first time asks for the OS notification permission; if denied, show an inline note with a button to open system settings).
- **Reminder list:** one row per reminder: bell icon, time (large, e.g. `05:00`), day summary under it (`Everyday`, `Mon, Thu, Sun`), optional message preview, and a per-reminder toggle on the right. Under the list, a small grey line: `Next: tomorrow 05:00`.
  - Tap a row → **Reminder sheet** (edit).
  - Swipe left on a row → delete (Undo snackbar).
- **+ Add reminder** pill button (accent) at the bottom of the card (max 5) → **Reminder sheet** (create).
- **Quick presets** (chips shown in the empty state and above the Add button): `Morning 07:00`, `Noon 12:00`, `Evening 20:00`, `Night 22:00`. Tapping a chip creates a reminder instantly with the habit's repeat days, which can then be edited.
- **Empty state:** "No reminders yet — add a gentle nudge." with the presets and the Add button.
- **Reminder sheet** (bottom sheet, same style as New Habit): top bar `Cancel` · `NEW REMINDER` / `EDIT REMINDER` · `Save` pill. Body:
  1. Time picker (wheel or Material time dial), 12h/24h per system setting.
  2. Day chips Mon–Sun (default = the habit's repeat days; at least one required).
  3. Message field (optional, default "Time for {habit name}").
  4. Toggles/dropdowns: **Skip if already done** (default ON) · **Remind again if not done** (Off / 15 / 30 / 60 min) · **Sound** (Default / Soft / Silent) · **Vibrate** · **Quick actions** (Done · Snooze · Skip).
  5. Red **Delete reminder** row (edit mode only).
- **Single source of truth:** this card and the `REMINDERS` section in Edit Habit use the same component and the same `Reminder` records. Changes made in one place show instantly in the other, and in Notifications → HABIT REMINDERS and the Menu badge count.
- Saving, editing, toggling or deleting a reminder immediately reschedules that habit's notifications (see section 8).

### 5.7 MENU

Top app bar: ← **Menu**. Content (scrollable, `bg` dark):

**GENERAL** card with rows (label left, optional trailing value, chevron right, hairline separators):
1. **Notifications** — trailing accent circular badge with number = count of enabled habit reminders (hide the badge if 0) → Notifications screen
2. **Settings** → Settings screen
3. **Weekly recap** → Weekly Recap
4. **Monthly recap** → Monthly Recap
5. **Year in Review** — trailing small grey year (`2026`) → Year in Review
6. **Milestones** — trailing small grey text `3 active` (hidden if no goals) → Milestones
7. **Widgets** — trailing small grey text `Add to home screen` → Widgets screen

Nothing else (exactly seven rows). No promo card, no PLUS, no community/about/rate/share. (Optionally a tiny grey footer: app name + version.)

### 5.8 NOTIFICATIONS

Top app bar: ← **Notifications**.

1. **Permission card** (only if OS permission missing): "Notifications are off" + accent button **Turn on**; also shows "Exact alarms" status on Android 12+ with a button to open system settings.
2. **DAILY NUDGES** card:
   - Toggle **Daily nudges** ("A morning kickoff and one evening save — never more.")
   - **Morning kickoff** time (default 08:00) + toggle. Text e.g. "11 small marks today. Start with one."
   - **Evening save** time (default 21:00) + toggle. Sent only if habits are still pending; text e.g. "3 small marks left — your 🔥 streaks are waiting."
3. **HABIT REMINDERS** card: one row per habit that has reminders — icon tile (small), name, reminder time chips (`05:00 · 21:30`), toggle (enable/disable all for that habit). Tap row → Edit Habit (REMINDERS). Habits without reminders appear under "No reminder" with an **Add** button.
4. **GOAL REMINDERS** card: one row per goal that has reminders turned on — goal icon, title, summary (`Weekly · Sun 18:00`, `3 days before due`), toggle; tap → that goal's reminders sheet. Goals without reminders sit under "No reminder" with an **Add** button.
5. **PREFERENCES** card:
   - **Quiet hours** toggle + from/to times (default 23:00–06:00; habit reminders inside are delayed or muted — choose "Mute" / "Delay until end").
   - **Snooze length:** 10 / 15 / 30 / 60 min (default 15).
   - **Sound:** Default / Soft / Silent. **Vibrate** toggle.
   - **Quick actions on notifications** toggle (Done · Snooze · Skip).
   - **Recap reminders:** three toggles — **Weekly recap**, **Monthly recap**, **Year in Review** (all default ON).
   - **Goal reminders** master toggle (default ON).
6. **Send test notification** button.

### 5.9 SETTINGS

Top app bar: ← **Settings**.

**Top card:** palette icon, title **Home Studio**, subtitle "Theme, colors and home layout — all in one place.", chevron → Home Studio.

**PREFERENCES** card (rows in order):
1. **Week start on** — subtitle "Affects every grid and recap" — 3-segment control `Saturday | Sunday | Monday` (default Monday).
2. **Sounds** — "A soft tick when you keep a habit" — toggle (default ON).
3. **Daily nudges** — "A morning kickoff and one evening save — never more." — toggle (default ON; mirrors Notifications screen).
4. **Card density** — `Comfortable | Compact` (mirrors Home Studio).
5. **Reorder habits** → Reorder habits screen.
6. **Show streak icon on** (label) with 3 toggle rows: **Today**, **Weekly**, **Monthly** (all default ON).
7. **Show Hijri date** — toggle (default OFF).
8. **Day starts at** — subtitle "Late-night check-ins count for the previous day until this time" — time picker, default 03:00 (applies to Today, widgets, streaks and recaps).
9. **Change Language** — trailing current language (`English`) + chevron → language list (English, العربية, Français). Arabic switches app to RTL.

**DATA** card (below PREFERENCES):
1. **Backup & restore** — trailing `Last backup: 3 days ago` (or `Never`) → Backup & restore screen (5.17)
2. **Archived habits** — trailing count → Archived habits screen (5.18)
3. **Welcome tour** → replays onboarding (5.19)

**Footer text** (small grey, wraps): "Habit Radar works fully offline. Nothing leaves the phone unless you export a backup."

### 5.10 HOME STUDIO

Top app bar: ← **Home Studio**. Scrollable.

1. **Live preview:** a phone-shaped mock (white rounded bezel, ~60% screen width, centered) showing a miniature, non-interactive Today screen rendered with the current settings; updates instantly as options change.
2. **Theme** group card:
   - **Theme:** `System | light | dark` (default dark).
   - **App color:** grid of round swatches, 6 per row, ~56dp, 4+ rows. Selected swatch = white ring + check. Palette (approx.): row1 green `#7ED87E`, mint-teal `#4FD8C4`, cyan `#3DDCF0`, steel `#5BB0D8`, periwinkle `#9CC8FF`, ice `#B8E6FF`; row2 lavender `#B4BCFF`, purple `#B39DDB`, magenta `#F7A8FF`, pink `#FFB0C0`, blush `#EBC4D8`, dusty red `#D98585`; row3 salmon `#FFB8A8`, peach `#FFA894`, apricot `#FFBC78`, mustard `#DDB45C`, orange `#EEA85C`, amber `#FFB300`; row4 lime `#C0D860`, taupe `#8C7B78`. Default = orchid `#E8A8F5` (include it as a swatch). The selected color becomes the global accent. (No lock icons — all free.)
3. **App bar** group card:
   - **Show greeting** toggle (headline).
   - **Show day-signal band** toggle.
   - **Headline:** three chips `Count | Percent | Days` (selected chip = accent text on `accent-container`).
4. **Today layout** group: three selectable thumbnail cards (equal width, ~100dp tall) with labels below — **Classic** (three stacked bars), **One thing** (one big block + two thin bars), **Ledger** (four thin lines). Selected = accent outline + accent label.
5. Card with:
   - **Card density:** `Comfortable | Compact`
   - **Hide completed habits** toggle (default OFF)
   - **Full-screen celebrations** toggle (default ON)
   - **Group habits into sections** toggle (default OFF) — shows section headers by Group instead of filter chips
6. **Reorder habits** row (icon = stacked lines) → Reorder habits.
7. **Bottom bar** group:
   - **Show home tab bar** — subtitle "Hide to keep Today only" — toggle (default ON). When OFF the floating tab bar is hidden and the app shows Today only.
   - **Reorder tabs** row (icon = window/tab) → Reorder tabs screen.

### 5.11 REORDER screens
- **Reorder habits:** vertical list with drag handles (☰), icon + name; saving writes `sortOrder`. Also a "Group by" toggle (Morning/Evening/custom) optional.
- **Reorder tabs:** list of Today, Weekly, Monthly, Overall with drag handles; **Today cannot be hidden**; others have a visibility toggle. The first tab in order is the start screen.

### 5.12 MONTHLY RECAP

Top app bar: ← **Monthly recap**. Month picker chips at top (`‹ October 2026 ›`; can't go beyond the current month or before the first data month). Recap is a vertical scroll of cards (each card has a soft gradient from accent) with count-up animations:

1. **Hero:** serif headline "October, *in review.*" Big completion % ring. Sub: `312 of 380 check-ins`. Delta chip: `▲ 6% vs September`.
2. **Key numbers** (2×3 grid): total check-ins, perfect days (all scheduled habits done), best streak this month, active days, skipped days, new habits started.
3. **Calendar heatmap:** month grid; each day cell shows a small ring segmented per habit (or a single intensity color = % done). Tap a day → sheet of that day's habits.
4. **Week by week:** bars for each week of the month (completion %), best week highlighted.
5. **Habit leaderboard:** habits ranked by completion %, with colored bars, ▲/▼ delta vs previous month. Callouts: **Most consistent** and **Needs love** (lowest).
6. **Best & toughest day:** best weekday and worst weekday this month with %; best single day (date, count).
7. **Time of day:** mini histogram of when check-ins happen; "You do most habits around 06:00".
8. **Streak updates:** streaks that grew, new records set, streaks broken (with habit icons).
9. **Insights:** up to 4 text cards from the rules in 7.3.
10. **Next month focus:** one suggestion ("Try keeping Walk on Fridays — your toughest day").
Empty state: "Not enough data for {Month} yet."
Automatically scheduled push on the 1st of each month at 09:00: "Your {Month} recap is ready" (controlled by the **Monthly recap** toggle in Notifications → PREFERENCES).

### 5.13 YEAR IN REVIEW

Top app bar: ← **Year in Review**; year selector chip (`2026`). Two modes:

**Story mode (opens first):** full-screen pages with thin progress segments at top, tap right/left to advance/go back, hold to pause, ✕ to exit. Each page: dark gradient with accent glow, large serif text, animated numbers/charts. Pages:
1. **Intro:** "Your 2026, *in habits.*" + days tracked.
2. **Total check-ins** (count-up) + "That's N hours/days of small marks" (if Time habits exist).
3. **Overall completion rate** ring + vs previous year (if exists).
4. **Best month / toughest month** (bar chart of 12 months with highlight).
5. **Year heatmap** (53×7) animated fill.
6. **Longest streak of the year** (habit icon, length, date range).
7. **Most consistent habit** and **Most improved habit** (largest rate gain from H1 to H2).
8. **Day-of-week pattern** (bars) + best weekday.
9. **Time of day** (peak hour).
10. **Top 5 habits** podium/list by check-ins.
11. **Perfect days** count and best perfect-day run.
12. **Outro:** "See you in {next year}." button **View summary**.

**Summary mode (scroll):** a single vertical page that repeats the key charts and numbers from the story (cards: totals, 12-month bar chart, year heatmap, habit ranking list, streak records, weekday pattern). Button at top toggles `Story | Summary`.

Availability: show current year anytime (labelled "so far"); previous years if data exists. In December (from Dec 1) push notification "Your {Year} in review is ready" (toggle in Notifications).

### 5.14 WEEKLY RECAP

Top app bar: ← **Weekly recap**. Week picker chips at top: `‹ 19 Oct – 25 Oct ›` (week starts on the day chosen in Settings → Week start on; cannot go past the current week or before the first week with data). The current unfinished week is labelled **So far**. Vertical scroll of cards (soft accent gradient, count-up animations), same card style as Monthly Recap:

1. **Hero:** serif headline "This week, *in review.*" (past weeks: "Week of 12 Oct, *in review.*"). Completion % ring, `47 of 61 check-ins`, delta chip `▲ 8% vs last week`.
2. **Week strip:** 7 columns (in week order). Each = a vertical rounded bar filled to that day's completion %, day label below, today marked. Perfect days get a small ✓ badge; best day is accent-highlighted. Tap a column → day sheet (that day's habits with toggles).
3. **Key numbers** (2×3 grid): total check-ins, perfect days, active days, streaks kept alive, streaks broken, skipped.
4. **Habit leaderboard:** each habit row shows icon, name, 7 mini dots in the habit color (that week's pattern), `5/6`, and ▲/▼ delta vs last week. Callouts: **Most consistent** and **Needs love**.
5. **Best & toughest day:** weekday and % for each.
6. **Time of day:** mini histogram of check-in hours; "Most marks around 06:00".
7. **Streak updates:** streaks that grew, new records, streaks broken (habit icons).
8. **Goal progress:** steps completed this week across goals; each goal as a mini progress bar (tap → Goal Detail). Hidden if no goals.
9. **Insights:** up to 3 text cards from rules in 7.3.
10. **Next week focus:** one gentle suggestion (e.g. "Fridays were your toughest day — protect one habit that day").

Empty state: "Not enough data for this week yet."
Push notification: on the first day of the new week at 09:00, "Your week recap is ready" (controlled by the **Weekly recap** toggle in Notifications → PREFERENCES). Tapping opens that week.

### 5.15 MILESTONES — big goals split into small steps

**Concept.** A **Goal** is a big objective ("Memorize a chapter", "Read 12 books", "Save for a laptop"). It is cut into **Steps** (small goals / milestones) that are completed one by one. Goal progress comes from its steps. Steps can be done manually, or complete **automatically** from a habit's progress, or from a numeric amount.

Entry points: Menu → Milestones, and Habit Detail → "Make this a goal".

#### A. Milestones list screen
- Top app bar: ← **Milestones**; right side accent round **+** button (→ New Goal sheet).
- Serif headline: **"Big goals, *small steps.*"**
- Segmented control: `Active | Done`.
- Summary line under the headline: `3 goals · 12 of 30 steps done` with a thin overall progress bar in the accent color.
- **Goal card** (tinted by goal color, same radius and density rules as habit cards):
  - Left: icon tile. Middle: goal title; second line `4 of 7 steps`.
  - Progress bar in the goal color with `%` at the right end.
  - Chip row: due chip — `12 days left` / `Due today` / `Overdue 3 days` (red tint) / `No date`.
  - **Next step row:** `Next: Finish chapter 3` with a small check circle to complete it right from the list (for manual steps; for habit/amount steps the circle is replaced by a small progress text like `9 / 14 days`).
  - Chevron at the right expands the card inline to show the next 3 unfinished steps with circles. Tap the card body → Goal Detail.
- Order: manual (long-press drag), default = nearest due date first, then newest.
- **Done tab:** completed goals, dimmed with ✓ and completion date; tap → Goal Detail (read-only except **Reopen**).
- **Empty state:** "No goals yet. Pick something big and cut it into small steps." + accent **+** button + three example chips (`Read 12 books`, `Memorize a chapter`, `Save for a laptop`) that open New Goal pre-filled with sample steps.

#### B. New / Edit Goal sheet (full-height bottom sheet, same style as New Habit)
Top bar: **Cancel** · `NEW GOAL` / `EDIT GOAL` · **Save** (disabled until name + at least 1 step).
1. **Icon** tile with **Change icon** (same picker as habits).
2. **Name:** large centered serif field, placeholder **"Name the big goal"**.
3. **Description** (optional).
4. **GOAL COLOR:** same swatch palette as habits.
5. **FINISH BY:** optional date row with date picker; quick chips `1 week · 1 month · 3 months · 1 year`.
6. **STEPS:**
   - List of step rows: drag handle, title field, ✕ remove.
   - **+ Add a small step** row at the bottom.
   - **Break it down** button (offline helper, no AI) opens a small sheet with two modes:
     - *By count:* "How many steps?" stepper (2–30) + name pattern (e.g. "Step {n}") → generates steps.
     - *By amount:* "Total" (e.g. 300) + "Per step" (e.g. 30) + unit (e.g. pages) → generates `Pages 1–30`, `31–60`, … as **Amount** steps.
     - Toggle **Spread due dates evenly until the finish date**.
7. **REMINDERS:** toggle **Remind me about this goal**, then options (each with its own toggle): **Weekly check-in** (day + time, default Sun 18:00), **Before the finish date** (1 / 3 / 7 days before, default 3), **Step due dates** (notify at 09:00 on the day a step is due).
Edit mode shows a red **Delete goal** row (confirmation; deletes its steps).

#### C. Step editor (bottom sheet, opened by tapping a step's title or "+ Add a small step" in Goal Detail)
- **Title**, optional **note**, optional **due date**.
- **Step type** segmented control `Manual | Habit | Amount`:
  - **Manual:** completed by tapping its circle.
  - **Habit:** choose a habit from a picker list, then a rule: `Streak reaches N days` · `Total check-ins reach N` · `Completion rate ≥ P% over the last D days`. The step completes automatically when the rule is met and shows live progress (`9 / 14 days`).
  - **Amount:** target number + unit + current value; stepper +/− and numeric input. Done when current ≥ target; shows `120 / 300 pages`.
- Red **Delete step** row (edit mode).

#### D. Goal Detail screen
Top app bar: ← back · goal title (truncated) · 🔔 bell (goal reminders sheet, same layout as the New Goal REMINDERS section) · pencil (→ Edit Goal) · ⋮ (Mark goal complete, Reopen, Delete).
Cards in one scroll, all tinted with the goal color:
1. **Hero:** big icon tile, serif title, description, progress ring with `%` and `4 of 7 steps`, finish date with countdown, started date.
2. **Next step card:** the first unfinished step in large text with a big check button and its due date; a **Skip for now** text button moves it to the end of the list.
3. **Steps timeline:** vertical list with connected nodes (circles styled like habit check circles, joined by a thin line; done = filled with check). Each row: title, note preview, due chip, type badge (habit steps show a tiny progress bar + flame; amount steps show a tiny bar + `120 / 300`). Done steps are dimmed with strikethrough and show the completion date.
   - Tap circle: toggles a manual step; opens the amount stepper for Amount steps; Habit steps are automatic (circle is read-only with a small info tooltip "Completes automatically").
   - Tap row: Step editor. Long-press: drag to reorder. Swipe left: delete (Undo snackbar).
   - **+ Add a small step** row at the bottom.
4. **Pace & prediction:** steps per week (last 4 weeks bars), projected finish date vs finish date with a chip **Ahead / On track / Behind**, overdue steps count, days active. If fewer than 2 steps are done: "Finish a few steps to see a prediction."
5. **Steps over time:** step chart of cumulative steps completed by date, with a dashed ideal line from start to finish date.
6. **Activity log:** completed steps with dates (most recent first).
When the last step is completed, the goal is auto-marked complete: celebration overlay (if "Full-screen celebrations" is ON; text "Goal *reached.*"), the goal moves to the **Done** tab, and an Undo snackbar allows reopening.

#### E. Rules
- **Step progress:** Manual = 0 or 1. Habit = `clamp(current / target)` (counts as done only when the rule is met). Amount = `clamp(current / target)` (done when ≥ target).
- **Goal progress %** = round(sum(step progress) ÷ number of steps × 100). The `X of N steps` text counts only fully done steps.
- Habit-linked steps are re-evaluated whenever an Entry for the linked habit changes, and by the nightly maintenance job. Once met they stay done (sticky); `doneAt` = the first time the rule was met. The user can manually reopen a step.
- **Overdue** = due date before today and step not done.
- **Projection:** pace = steps completed in the last 28 days ÷ 4 (per week); remaining ÷ pace = weeks to finish → projected date. **Ahead** if projected ≥ 7 days before the finish date, **Behind** if > 7 days after, otherwise **On track**.
- If a linked habit is deleted, its steps become **Manual** (keeping their current done state) with a note "Habit removed".
- Goal notifications: see section 8.

### 5.16 WIDGETS (home screen)

The app provides Android home-screen widgets (reference: the dark **week-grid widget** from the screenshot — header row `Mo Tu We Th Fr Sa Su`, one row per habit with icon tile, title, 🔥 streak and seven day circles). Widgets are built with **Jetpack Glance**, read only the local database, update instantly when data changes, and let the user check habits **without opening the app**.

#### A. In-app Widgets screen (Menu → Widgets)
- Top app bar: ← **Widgets**. Serif headline **"Your habits, *on the home screen.*"**
- **Carousel of widget previews** (horizontal pager). Each card shows a live preview rendered with the same composables as the real widget (demo data, scaled), the widget name, a one-line description, a size chip (`4×4`) and an accent pill button **Add to home screen**.
  - Tap → `requestPinGlanceAppWidget` (system dialog lets the user place it; the configuration screen opens first, see D). On success show a snackbar "Added — long-press it to resize."
  - If the launcher does not support pinning: bottom sheet with 3 steps — "Long-press an empty spot on your home screen → Widgets → Habit Radar → drag the widget in."
- **WIDGET STYLE** card (defaults applied to new widgets):
  - **Theme:** `Follow app | Dark | Light`
  - **Background opacity:** slider 40–100% (default 100%)
  - **Corner radius:** `Rounded | Extra rounded`
  - **Show streak** toggle (default ON)
  - **Hide habit names** toggle (default OFF — shows icons only, for privacy on the home screen)
  - **Allow back-filling from widget** toggle (default ON)
- **ACTIVE WIDGETS** list: one row per widget currently on the home screen (type, size, number of habits) with a **Refresh now** button (updates all widgets).
- Habit Detail → ⋮ → **Add widget** opens this screen on the *Single habit* widget with that habit pre-selected.

#### B. Widget catalogue
| # | Widget | Default / resizable size | Content |
|---|---|---|---|
| 1 | **Week grid** (the reference) | 4×4, resizes 4×2 → 5×6 | Week header + one row per habit with 7 circles |
| 2 | **Today checklist** | 4×3, resizes 4×2 → 4×5 | Date, day-signal band, `2 of 11` caption, rows with icon, title, check circle |
| 3 | **Progress ring** | 2×2 | Completion ring with %, `3 of 8`, `🔥 12-day run` line |
| 4 | **Single habit** | 2×2 | Icon tile, name, streak, big check button, mini 7-dot week row |
| 5 | **Goal progress** | 4×2 | Goal icon + title, progress bar, `4 of 7 steps`, next step with check circle (manual steps) |

#### C. Week grid widget — exact design
- **Container:** dark rounded rectangle (radius 28dp), background = app `card` color (`#1B1B1F`, dark theme) at the chosen opacity, 16dp padding. Light theme: `#F3F0F5` with habit tints at higher saturation.
- **Header row:** seven 2-letter labels `Mo Tu We Th Fr Sa Su`, ordered by Settings → Week start on, aligned exactly above the circle columns (empty space above icon/title). **Today's label is bold white**; the others are grey `#8E8F98`.
- **Habit row** (comfortable ≈ 74dp tall, compact ≈ 56dp), left to right:
  1. **Icon tile** 56dp, radius 16, habit-color tint with the icon in the saturated habit color.
  2. **Title** 18sp white, single line, ellipsized, takes the free width (hidden if "Hide habit names").
  3. **Streak:** 🔥 + number in grey (e.g. `🔥 1`, `🔥 0`).
  4. **Seven circles** 36dp, 8dp gap.
- **Circle states** (H = habit color):
  - *Done:* filled H with a dark ✓ (e.g. purple and orange checks in the screenshot).
  - *Today, not done:* ring 2dp in H over a dark tint — clearly brighter than the other circles.
  - *Past, not done (missed):* filled dark tint of H (≈20% over background), no ring.
  - *Skipped:* dark tint with a small centered dash.
  - *Not scheduled that day:* dim tint (≈10%), not clickable.
  - *Future day:* dimmer tint (≈10%), not clickable (the last column in the screenshot).
- **Interactions:**
  - Tap **today's** circle → toggle done (optimistic update, then DB write).
  - Tap a **past circle of this week** → toggle (back-fill) when "Allow back-filling from widget" is ON.
  - Tap a future circle → ignored.
  - **Amount / Time** habits: tapping a circle opens the app's stepper/timer sheet via deep link instead of toggling.
  - Tap icon or title → Habit Detail (`habitradar://habit/{id}`). Tap the header row → Weekly tab.
- **More habits than fit:** the list scrolls (Glance `LazyColumn`).
- **Responsive sizes:** narrower than ~250dp → compact variant with icon, title, streak and **only today's circle**; shorter than ~180dp → header + as many rows as fit.
- **Empty state:** "No habits yet — tap to add one" → opens New Habit.
- Fonts use the system sans (widgets cannot use the app's bundled serif fonts).

#### D. Widget configuration screen
Shown when a widget is added and from long-press → *Reconfigure*. Uses the same dark style as Home Studio (top app bar `Widget settings`, cards, toggles). Options:
- **Habits:** `All habits | Choose habits` (multi-select list with icon + checkbox). *Single habit* and *Goal progress* widgets use a single-choice list instead.
- Theme, Background opacity, Corner radius, Show streak, Hide habit names, Allow back-filling (initial values come from the WIDGET STYLE defaults).
- Live preview at the top of the screen.
- **Add widget** (primary pill) / **Cancel**.

#### E. Behavior and refresh
- Widgets refresh immediately after any change to entries, habits, goals or steps (from the app, a notification action, or the widget itself) and when settings change (week start, accent, language, theme).
- Also refreshed at the **day-rollover time**, on boot, on time/timezone/locale change, and by a periodic WorkManager safety refresh (hourly).
- Widgets work offline, use no network, and contain no analytics.
- Paused and archived habits do not appear on widgets.
- RTL: layout and day order mirror in Arabic.
- Accessibility: every circle has a content description ("Walk, Saturday, done").

### 5.17 BACKUP & RESTORE

Settings → DATA → **Backup & restore**. Top app bar: ← **Backup & restore**. Everything is stored locally, so this screen is how the user keeps their streaks safe when changing phones or reinstalling.

1. **Status card:** `Last backup: 3 days ago · 12 habits · 4,310 check-ins` (or "No backup yet" in an amber tint).
2. **EXPORT** card:
   - **Export backup (.json)** — accent pill. Creates `habit-radar-backup-YYYY-MM-DD.json` through the system file picker (`ACTION_CREATE_DOCUMENT`; no storage permission) with a share option (Drive, WhatsApp, email…).
   - Toggle **Include settings** (default ON).
   - **Export as CSV** — secondary button; produces a `.zip` with `habits.csv` and `entries.csv` (habit, date, status, value, note, completedAt) for use in spreadsheets.
3. **RESTORE** card:
   - **Restore from backup** → system file picker (`ACTION_OPEN_DOCUMENT`). The file is validated first (format name, schema version, checksum), then a preview sheet shows `12 habits · 4,310 check-ins · 3 goals · exported 20 Oct 2026`.
   - Choose **Replace everything** (default; confirmation dialog; the app first saves a safety snapshot of the current data that can be undone for 24 hours) or **Merge** (adds missing habits, goals and entries by id; if the same habit and date exist in both, the most recent `completedAt` wins).
   - After a restore: reschedule all reminders and goal notifications, update every widget, and show "Restored 12 habits."
   - Newer-schema files are refused with a clear message; older schemas are migrated.
4. **AUTOMATIC BACKUP** card:
   - Toggle **Weekly backup to a folder** (default OFF). Choosing it asks the user to pick a folder via `ACTION_OPEN_DOCUMENT_TREE`; the permission is persisted. A WorkManager job writes a backup weekly and keeps the last **3 / 5 / 10** files (default 5).
   - Toggle **Remind me to back up** (default ON): one gentle notification per month if auto backup is OFF and the last backup is older than 30 days.
5. Text note: "Android may also restore your data automatically when you set up a new phone with Google backup." (Auto Backup is enabled for the database and settings; see 13.9.)

**Backup file format:** JSON `{ "format": "habit-radar-backup", "schemaVersion": 1, "exportedAt": ISO, "checksum": SHA-256 of the data block, "data": { habits, habitPauses, entries, reminders, goals, steps, settings? } }`. IDs are preserved. Streamed read/write so large files do not exhaust memory.

### 5.18 ARCHIVE & PAUSE

**Pause** — for a trip, illness or exams:
- Entry points: long-press on Today, Edit Habit → **Pause habit**, Habit Detail ⋮ → **Pause**.
- **Pause sheet:** options `Tomorrow · 3 days · 1 week · Choose date · Until I resume` and a **Pause** button.
- While paused: the habit disappears from Today, the day-signal band, widgets and Weekly/Monthly grids (shown dimmed in the grids with a pause icon), reminders do not fire, and the paused days are treated like **skipped** days (they neither break streaks nor count in rates).
- It appears in the collapsed `Paused (N)` row at the bottom of Today with a **Resume** button; it resumes automatically after the end date (silently) or manually at any time.
- Recaps mention it: "Paused for 5 days."

**Archive** — for habits no longer wanted but worth keeping history for:
- Entry points: long-press on Today, Edit Habit → **Archive habit**, Habit Detail ⋮ → **Archive**.
- Archived habits disappear from Today, Weekly, Monthly, Overall lists, widgets and notifications, but **all entries stay** and still appear in recaps and Year in Review for the periods when the habit was active.
- Linked goal steps keep their state but stop updating from that habit.

**Archived habits screen** (Settings → DATA → Archived habits): top app bar ← **Archived habits**; list of rows (icon tile, name, `Archived 12 Oct · best streak 21 · 134 check-ins`).
- Tap a row → Habit Detail (read-only, with the Archived banner).
- Swipe or ⋮ per row: **Restore** (returns the habit to active; the archived period is stored as a pause so the streak is not broken) and **Delete permanently** (confirmation).
- Empty state: "Nothing archived."

### 5.19 ONBOARDING & STARTER TEMPLATES

**First launch only** (replay from Settings → DATA → Welcome tour). Full-screen pages on `bg`, serif headline with an *italic* word, accent page dots, **Skip** at top right, accent **Next** pill:
1. **"Small marks, *every day.*"** — one line: private, offline, no account.
2. **"Pick a few to *start.*"** — template picker: multi-select cards grouped by category (see below) plus **Start from scratch**. Selected cards show an accent ring and ✓.
3. **"Gentle *reminders.*"** — explains the two daily nudges, button **Allow notifications** (system permission prompt), then an explainer for exact alarms on Android 12+ ("so reminders arrive on time") with a button that opens the system screen. Both are skippable. Two time pickers: **Morning kickoff** (08:00) and **Evening save** (21:00).
4. **"Make it *yours.*"** — Theme (System / Light / Dark), app color (6 swatches), Today layout (Classic / One thing / Ledger), Week start on, Language; live mini preview above.
5. **"All *set.*"** — summary (`5 habits · reminders on`) and **Start**. This creates the selected habits (with the suggested reminder times if notifications were allowed) and opens Today with a one-time coachmark: "Tap the circle to check · Tap a card for analytics · ≡ for the menu."

**Template gallery** (also opened by **Browse ideas** in New Habit, and by an empty Today screen's **Browse ideas** button): category chips and a list of template cards (icon tile, name, short description, `Daily · Task`). Tapping a card in New Habit pre-fills the form; in onboarding it multi-selects.
- **Templates** (bundled JSON asset `templates.json`, localized EN/AR/FR, no network). Each defines icon, color, track type, target, repeat days, group and a suggested reminder time:
  - *Health:* Drink water (Amount, 8 glasses), Walk (Mon, Thu, Sun), Workout, Morning stretch, Take vitamins
  - *Mind:* Meditate (Time, 10 min), Journal, Read 10 pages (Amount), Review the day
  - *Sleep:* Lights out by 12, Wake up on time, No screens in bed
  - *Money:* Log what I spent
  - *Study:* Study session (Time, 30 min), Learn something new, Review notes
  - *Faith & reflection:* Morning remembrance, Evening remembrance, Daily reading, Give charity weekly (Weekly, 1×)
  - *Home:* Tidy up 10 minutes, Plan tomorrow

---

## 6. Data Model

Implement as **Room entities in Kotlin**. Types below are pseudocode: `date` = `LocalDate`, `datetime` = `Instant`, `uuid` = `String`.

```ts
Habit {
  id: uuid
  name: string
  description?: string
  iconKey: string            // icon name or emoji
  colorKey: string
  trackType: 'task' | 'amount' | 'time'
  targetValue?: number       // amount, or minutes for time
  unit?: string
  comparison?: 'atLeast' | 'atMost' | 'exactly'
  repeatType: 'daily' | 'weekly' | 'monthly'
  daysOfWeek: number[]       // 1..7
  timesPerPeriod?: number
  daysOfMonth?: number[]
  group?: string
  goalStreak?: number
  askNote: boolean
  status: 'active' | 'archived'   // default active
  archivedAt?: datetime
  startDate: date
  endDate?: date
  sortOrder: number
  createdAt: datetime
}

Reminder {
  id, habitId
  time: 'HH:mm'
  daysOfWeek: number[]
  message?: string
  skipIfDone: boolean        // default true
  repeatAfterMin?: 15|30|60
  sound: 'default'|'soft'|'silent'
  vibrate: boolean
  quickActions: boolean
  enabled: boolean
}

Entry {
  id, habitId
  date: 'YYYY-MM-DD'         // local date after day-rollover logic
  status: 'done' | 'skipped' | 'missed'   // missed is derived, not stored (store only done/skipped)
  value?: number
  note?: string
  completedAt?: datetime
}

Settings {
  weekStart: 'sat'|'sun'|'mon'
  sounds, dailyNudges, cardDensity, showStreakToday, showStreakWeekly, showStreakMonthly,
  showHijri, language, theme, accentColor,
  showGreeting, showDaySignalBand, headline: 'count'|'percent'|'days',
  todayLayout: 'classic'|'oneThing'|'ledger',
  hideCompleted, fullScreenCelebrations, showTabBar, tabOrder[], hiddenTabs[]
  morningNudgeTime, eveningNudgeTime, quietHours{enabled,from,to,mode}, snoozeMin,
  notifSound, notifVibrate, notifQuickActions, recapWeekly, recapMonthly, recapYearly, goalReminders,
  dayRollover: 'HH:mm' (default 03:00), groupSections, groupFilter?, onboardingDone,
  autoBackupEnabled, autoBackupFolderUri?, autoBackupKeep (3|5|10), lastBackupAt?, backupNudge
}

Goal {
  id: uuid
  title: string
  description?: string
  iconKey: string
  colorKey: string
  startDate: date
  targetDate?: date
  status: 'active' | 'completed'
  completedAt?: datetime
  sortOrder: number
  remindWeekly: boolean; weeklyDay?: number; weeklyTime?: 'HH:mm'
  remindBeforeDays?: 1 | 3 | 7; beforeTime?: 'HH:mm'
  remindStepDue: boolean
}

Step {
  id: uuid
  goalId: uuid               // FK -> Goal, cascade delete
  title: string
  note?: string
  dueDate?: date
  sortOrder: number
  type: 'manual' | 'habit' | 'amount'
  // habit steps
  linkedHabitId?: uuid       // FK -> Habit, SET NULL (step becomes manual)
  habitMetric?: 'streak' | 'totalCheckins' | 'rate'
  habitTarget?: number       // days, count, or percent
  habitWindowDays?: number   // for 'rate'
  // amount steps
  amountTarget?: number
  amountCurrent?: number
  amountUnit?: string
  done: boolean
  doneAt?: datetime
}

HabitPause {
  id: uuid
  habitId: uuid             // FK -> Habit, cascade
  startDate: date
  endDate?: date            // null = paused until resumed
}

WidgetConfig {
  appWidgetId: int          // from AppWidgetManager
  type: 'weekGrid' | 'todayList' | 'progressRing' | 'singleHabit' | 'goal'
  habitIds: uuid[]          // empty = all habits
  goalId?: uuid
  theme: 'follow' | 'dark' | 'light'
  opacity: number           // 40..100
  cornerStyle: 'rounded' | 'extra'
  showStreak: boolean
  hideNames: boolean
  allowBackfill: boolean
}
```
Indexes: `(habitId, date)` unique on Entry; `(date)`; `(goalId, sortOrder)` on Step; `(linkedHabitId)` on Step.

---

## 7. Analytics Definitions

### 7.1 Core
- **Scheduled day:** a date within [startDate, endDate or today] that matches the habit's schedule. Weekly/monthly "X times" habits are evaluated per period.
- **Completion rate (period)** = done ÷ (scheduled − skipped) × 100.
- **Streak:** consecutive scheduled days done (skipped days neither break nor count; unscheduled days ignored). Current streak stays alive through today until the day ends. For weekly/monthly habits: consecutive periods meeting their target.
- **Perfect day:** every habit scheduled that day is done (skips ignored).
- **Paused days:** days inside a `HabitPause` range are treated like skipped days (not scheduled for rates; streaks neither break nor count). Archived habits keep their history for past periods.
- **Momentum score** = 0.5 × rate(last 14 days) + 0.3 × rate(last 30) + 0.2 × rate(last 90). Labels: <30 Cold · 30–59 Warming · 60–84 Strong · ≥85 On fire.
- **Consistency score** = 100 − min(100, stddev(weekly rates) × 2). (Higher = steadier week to week.)
- **Bounce-back rate** = done on the first scheduled day after a miss ÷ number of misses.
- **Recovery time** = mean scheduled days between a miss and the next done.
- **Longest gap** = max consecutive missed scheduled days.
- **Amount/Time:** goal-hit rate = days meeting the comparison ÷ days with entries; trend = linear-regression slope of daily values, shown per week and as %.
- **Time-of-day:** from `completedAt` in local time; peak window = densest contiguous 2-hour window.
- **Weekday performance:** per weekday, rate over all scheduled occurrences.

### 7.2 Caching
Recompute heavy aggregates (heatmaps, weekday, hour histograms) on a background coroutine dispatcher (or WorkManager) and cache per habit; invalidate on any entry change for that habit. Recaps compute from entries on open and cache per (month/year).

### 7.3 Insight rules (templates; show only the highest-scoring ones)
- Weekday gap ≥ 20 pts → "You're {x} pts stronger on {best} than {worst}."
- Rate up/down ≥ 8 pts vs previous period → "Trending {up/down} — {rate}% this {period}."
- Within 3 days of best streak → "{n} more days to beat your record."
- Best month/week by ≥ 10 pts → "Your best month was {month} ({rate}%)."
- 4+ weeks improving in a row → "Four weeks of steady progress."
- Peak-hour concentration ≥ 50% → "Most check-ins happen around {time}."
- ≥ 3 misses on the same weekday in last 8 weeks → "{Weekday}s are tough for this habit."
Tone: gentle, never shaming.

---

## 8. Notification Engine

- Android channels: `habit_reminders` (high), `daily_nudges` (default), `recaps` (low), `goals` (default).
- Scheduling uses `AlarmManager` (see 13.6): arm the next occurrence of each reminder, re-arm after it fires; reschedule everything on boot, timezone/time change, app update, edit, or settings change.
- At fire time: if `skipIfDone` and today is done → cancel. Quiet hours handled per mode.
- Actions: **Done** (marks entry, updates notification), **Snooze** (settings length, max 3 times), **Skip**.
- Tap notification → Habit Detail (reminders) or Today (nudges).
- Daily nudges: max 2 per day total (morning, evening). Evening only if pending habits exist.
- Recap pushes: weekly on the first day of the new week at 09:00, monthly on the 1st at 09:00, yearly on Dec 1 at 09:00 (and Jan 1 if not opened). Each has its own toggle.
- Goal pushes (channel `goals`): **weekly check-in** ("{Goal}: 4 of 7 steps done"), **before finish date** ("{Goal} is due in 3 days — 2 steps left"), **step due** at 09:00 on the due day (actions: Done · Snooze; Done only for manual steps). Skip if the goal/step is already done. Tap opens `habitradar://goal/{id}`.
- Badge on Menu → Notifications = number of enabled habit reminders.
- Paused habits: no reminders during the pause; re-armed on resume. Archived habits: reminders cancelled and kept disabled (restored with the habit).
- Backup nudge: if automatic backup is OFF and `lastBackupAt` is older than 30 days (or there is none after 14 days of use), send at most one notification per month, "Back up your habits", on channel `recaps`.

---

## 9. Edge Cases & Polish

- Day rollover: default midnight; handle late-night habits ("Lights out by 12") by allowing a configurable rollover time (default 03:00) in Settings (advanced, below Hijri date).
- Back-fill up to 1 year; no future check-ins.
- Undo snackbar for toggles/deletes. Deleting a habit deletes its entries (confirm).
- Empty states with friendly serif copy and an accent "+".
- Accessibility: 48dp targets, content descriptions ("Walk, Saturday, not done"), scalable fonts, color not the only status signal (icons/dash styles).
- RTL: mirror layouts; charts keep time left→right unless Arabic locale (then right→left).
- Performance: virtualized lists, DB indexes, chart downsampling for 1Y/All.
- Data export/import is out of scope; the DB file stays local.

---

## 10. Build Order

1. Theme tokens, typography, app shell with floating tab bar, top app bars.
2. DB + repositories + settings store.
3. New/Edit Habit sheet (without reminders), icon + color pickers.
4. Today (Classic) + check logic + streaks + day-signal band.
5. Weekly, Monthly tab, Overall tab.
6. Settings + Home Studio (live preview, layouts One thing / Ledger, density, theme, accent, tab bar options, reorder screens).
7. Notifications engine + per-habit REMINDERS section + Notifications screen + Menu.
8. Habit Detail: A–C, then D–H, then I–N (analytics modules).
9. Weekly Recap + Monthly Recap (shared recap components).
10. Milestones: Goal + Step tables, Goals list, Goal Detail, New/Edit Goal, Step editor, habit-linked auto-complete, goal reminders.
11. Year in Review (summary mode first, then story mode).
12. Home-screen widgets: Week grid first, then Today checklist, Progress ring, Single habit, Goal progress; Widgets screen and configuration screen.
13. Archive & pause (status, `HabitPause`, Archived habits screen), Backup & restore (export/import, auto backup, Auto Backup rules), Onboarding + template gallery.
14. Localization (EN/AR/FR), Hijri date, polish, tests.

## 11. Acceptance Checklist

- [ ] Menu contains exactly seven rows: Notifications (with badge), Settings, Weekly recap, Monthly recap, Year in Review, Milestones, Widgets.
- [ ] Weekly recap works for current and past weeks, respects Week start on, and shows goal progress.
- [ ] Milestones: create a goal with steps (manual, habit-linked, amount), "Break it down" helper, reorder/delete steps, progress %, pace and projection, completion celebration, Done tab, reopen.
- [ ] Habit-linked steps complete automatically when the habit rule is met and stay done.
- [ ] Goal reminders (weekly check-in, before finish date, step due) fire correctly and are listed in Notifications.
- [ ] App is native Kotlin + Jetpack Compose, minSdk 26, no INTERNET permission, no third-party analytics.
- [ ] Widgets screen shows live previews of all five widgets and **Add to home screen** pins a widget (with fallback instructions when pinning is unsupported).
- [ ] Week grid widget matches the reference: header with today's label bold, icon tile + title + streak + seven circles per habit, correct done / today / missed / future states, scrolls when habits overflow, compact variant when narrow.
- [ ] Tapping today's circle (and past circles when allowed) in a widget toggles the habit and the app, notifications and other widgets update immediately.
- [ ] Widgets refresh at day rollover, after boot, and when data or settings change; widget configuration works when adding and reconfiguring.
- [ ] Backup: export JSON and CSV, restore with Replace and Merge, validation errors for bad or newer files, automatic weekly backup to a chosen folder, monthly backup nudge.
- [ ] Pause: paused habits leave Today, widgets and reminders, do not break streaks, and resume manually or automatically.
- [ ] Archive: archived habits keep all history, appear in the Archived habits screen, and can be restored without breaking the streak or deleted permanently.
- [ ] Delete warns and offers Archive instead.
- [ ] "Day starts at" setting exists in Settings and is respected by Today, widgets, streaks and recaps.
- [ ] Group chips (or section headers) filter and organize Today, Weekly and Monthly.
- [ ] Onboarding runs on first launch, can be skipped or replayed, creates the chosen template habits, and the template gallery is reachable from New Habit and the empty Today screen.
- [ ] Edit Habit has Remind me + multiple reminders + skip-if-done + repeat + quick actions.
- [ ] Notifications screen: permission state, daily nudges, per-habit list, quiet hours, snooze, test button.
- [ ] Settings includes Home Studio, week start, sounds, daily nudges, density, reorder, streak-icon toggles, Hijri date, language.
- [ ] Home Studio: live preview, theme, app color, greeting/band/headline, three Today layouts, density, hide completed, celebrations, tab bar toggle, reorder tabs.
- [ ] Habit Detail shows hero stats, momentum, trend, heatmaps, weekday and time-of-day charts, streak history, consistency, volume, miss analysis, insights, history log, with a working period selector.
- [ ] Monthly Recap and Year in Review (story + summary) generate from real data and handle empty states.
- [ ] Habit Detail has a REMINDERS card where reminders can be added, edited, toggled, deleted and created from presets, without opening Edit Habit.
- [ ] Reminders created on Habit Detail, Edit Habit and Notifications screens stay in sync.
- [ ] No paywall, no promo banner, no network permission.

## 12. UI Freeze Note

The visual design (colors, typography, card styles, layouts, spacing, Home Studio options) is **final and must follow the reference exactly**. Do not redesign or restyle anything. Screens added in later revisions (the reminder card on Habit Detail, Weekly recap, Milestones, Widgets, Backup & restore, Archived habits, Onboarding) must reuse the existing styles, components and spacing; they do not introduce a new look.

---

## 13. Android Implementation Notes (Kotlin / Compose)

### 13.1 Project setup
- Gradle Kotlin DSL with a version catalog (`libs.versions.toml`); Java 17; plugins: Kotlin, Compose compiler, KSP (Room + Hilt), kotlinx-serialization (type-safe navigation).
- Libraries: Compose BOM, Material 3, Navigation-Compose, Lifecycle (ViewModel, runtime-compose), Hilt (+ hilt-navigation-compose), Room, DataStore, WorkManager, coroutines; optional: Vico (charts), a reorderable-list library for drag and drop.
- `enableEdgeToEdge()`, predictive back enabled, release builds with R8.
- No Firebase, analytics or ads SDKs.

### 13.2 Packages
```
app/
 ├─ core/   ui (theme, components) · data (db, datastore) · domain (model, usecase) · notifications · util
 └─ feature/ today · weekly · monthly · overall · habit (edit) · habitdetail · menu · notifications
             · settings · homestudio · reorder · recap (weekly, monthly, year) · milestones (list, detail, edit, step) · widgets (glance, config) · backup · archive · onboarding
```
Each feature has a Screen composable, a `ViewModel` exposing `StateFlow<UiState>`, and use-cases. Streaks, rates, momentum, consistency, weekday/time stats, recap aggregates and goal progress/projection are **pure Kotlin use-cases** with unit tests.

### 13.3 Navigation (type-safe routes)
`Home(tab)`, `HabitDetail(id)`, `EditHabit(id?)` (modal sheet), `Menu`, `Notifications`, `Settings`, `HomeStudio`, `ReorderHabits`, `ReorderTabs`, `WeeklyRecap(weekStart?)`, `MonthlyRecap(yearMonth?)`, `YearInReview(year?)`, `Milestones`, `GoalDetail(id)`, `EditGoal(id?)`, `StepEditor(goalId, stepId?)`, `Widgets`, `BackupRestore`, `ArchivedHabits`, `TemplateGallery`, `Onboarding`. (`WidgetConfigActivity` is a separate Activity launched by the system.)
Notification deep links: `habitradar://today`, `habitradar://habit/{id}`, `habitradar://goal/{id}`, `habitradar://recap/week|month|year`.

### 13.4 Theme and UI in Compose
- `HabitRadarTheme(darkTheme, accent)` builds a custom `ColorScheme` from the chosen accent; helper `habitTint(color)` produces the dark card tints; `CompositionLocal`s for card density and Today layout.
- Serif headlines use `AnnotatedString` with an italic span for the emphasized word.
- Floating tab bar: `Box` overlay with a pill `Surface` (radius 32dp) above the navigation-bar inset; list `contentPadding` bottom ≈ 120dp; hidden when "Show home tab bar" is OFF.
- Segmented controls: custom row to match the selected dark pill style (not the default M3 look). Toggles: M3 `Switch` with custom colors.
- Sheets: `ModalBottomSheet`. Swipe actions: `SwipeToDismissBox`. Reorder: long-press drag in `LazyColumn`.
- Check animation: spring scale via `animateFloatAsState` + haptic; soft tick via `SoundPool` when Sounds is ON.
- Confetti/celebration overlay: `Canvas` particle system, ~1.5 s, dismiss on tap.
- Heatmaps, rings, bars, step charts: custom `Canvas` composables; line charts via Vico or custom.
- Home Studio live preview: render the **real** Today composable with fake in-memory data inside a phone-bezel frame using `graphicsLayer(scaleX = 0.45f, scaleY = 0.45f)` and input blocked, so it always matches the real UI.
- Story mode (Year in Review): `HorizontalPager` with scrolling disabled, custom top progress segments, tap zones via `detectTapGestures`, auto-advance with `LaunchedEffect`.
- Goal steps timeline: `LazyColumn` with a custom `drawBehind` connector line between step nodes.

### 13.5 Data
- `HabitRadarDb` (Room) v1, `exportSchema = true`; type converters for `LocalDate`, `LocalTime`, `Instant`, enums, `List<Int>`.
- Foreign keys: Entry→Habit (cascade), Reminder→Habit (cascade), Step→Goal (cascade), Step.linkedHabitId→Habit (SET NULL, step turns Manual).
- Settings in Preferences DataStore. Day-rollover helper `LogicalDate.now(rollover)` is used everywhere; never call `LocalDate.now()` directly in domain code.
- Hijri date: `HijrahDate.from(localDate)` with `HijrahChronology.INSTANCE` (Umm al-Qura), formatted with `DateTimeFormatter` for the current locale.

### 13.6 Reminders and notifications (Android SDK)
- Runtime permission `POST_NOTIFICATIONS` (Android 13+). Exact alarms: check `AlarmManager.canScheduleExactAlarms()` and, if false, deep-link to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; fall back to `setAndAllowWhileIdle` (inexact) if the user declines.
- `ReminderScheduler` arms only the **next** occurrence of each reminder with `setExactAndAllowWhileIdle`, then re-arms from the receiver when it fires.
- Receivers: `ReminderReceiver` (builds the `NotificationCompat` notification; checks skip-if-done and quiet hours at fire time), `NotificationActionReceiver` (Done / Snooze / Skip, uses `goAsync()` + coroutine to update the DB, then updates or cancels the notification), `BootReceiver` (BOOT_COMPLETED, TIMEZONE_CHANGED, TIME_SET, MY_PACKAGE_REPLACED → reschedule everything).
- `DailyMaintenanceWorker` (WorkManager periodic): re-sync alarms, evaluate habit-linked goal steps, schedule recap and goal pushes, refresh cached aggregates.
- Channels created at app start: `habit_reminders`, `daily_nudges`, `recaps`, `goals`. All `PendingIntent`s use `FLAG_IMMUTABLE`.

### 13.7 Testing
- Unit tests (pure Kotlin): streaks, completion rate, momentum, consistency, weekday/time-of-day stats, recap aggregates, goal progress and projection.
- Room in-memory DAO tests, including cascade and SET NULL behavior.
- Compose UI tests: check toggle, New Habit flow, reminder sheet, goal + steps flow.
- Scheduler tests with a fake `AlarmManager` wrapper.

### 13.8 Widgets (Jetpack Glance)
- Dependencies: `androidx.glance:glance-appwidget` and `glance-material3`.
- Classes: `WeekGridWidget : GlanceAppWidget`, `TodayListWidget`, `ProgressRingWidget`, `SingleHabitWidget`, `GoalWidget`, each with a `GlanceAppWidgetReceiver` subclass declared in the manifest.
- `res/xml/*_widget_info.xml` per widget: `targetCellWidth/Height` (4×4 for the week grid), `minResizeWidth/Height`, `resizeMode="horizontal|vertical"`, `widgetCategory="home_screen"`, `configure` = `WidgetConfigActivity`, `widgetFeatures="reconfigurable|configuration_optional"`, `updatePeriodMillis="0"` (updates are event-driven).
- Sizing: `SizeMode.Responsive(setOf(DpSize(180.dp, 110.dp), DpSize(250.dp, 180.dp), DpSize(320.dp, 300.dp)))`; the composable chooses the compact or full-grid variant from `LocalSize.current`.
- Data: `provideGlance` reads from Room through a Hilt `EntryPoint`. A DAO query returns the week matrix (habit × 7 days → status) in one call. A pure Kotlin `CircleStateMapper` maps (scheduled, done, skipped, past/today/future) to the circle state, with unit tests.
- Drawing: use drawable resources (circle, ring, check) tinted with `ColorFilter.tint`; do not rely on `cornerRadius` modifiers below API 31. Use `LazyColumn` for the habit list.
- Actions: `ToggleHabitAction : ActionCallback` receives `habitId` and `date` as `ActionParameters`, writes the Entry through the repository, then calls `updateAll` on every widget class. Navigation uses `actionStartActivity` with the deep links `habitradar://habit/{id}`, `habitradar://weekly`, `habitradar://new-habit`.
- `WidgetUpdater` is called from the repositories after every write; `DayRolloverWorker` (WorkManager) updates at the logical day change; the boot, time and locale receivers also trigger updates.
- Pinning: `GlanceAppWidgetManager(context).requestPinGlanceAppWidget(receiver, preview, previewState, successCallback)`; check `isRequestPinAppWidgetSupported`. On Android 15+ also register generated previews with `setWidgetPreviews`.
- Tests: unit tests for `CircleStateMapper` and the week-matrix query; Glance composable previews for light/dark and each size.

### 13.9 Backup, archive and onboarding (Android)
- **Serialization:** `kotlinx-serialization` with a versioned `BackupFile` model; the checksum is SHA-256 over the encoded `data` block; read and write with streaming (`InputStream` / `OutputStream` from `ContentResolver`).
- **Import:** inside a single Room transaction (`withTransaction`) so a failed restore changes nothing. Before a Replace restore, copy the current database to an internal `pre_restore_snapshot` file (kept 24 h, offered as **Undo**). After a restore call `ReminderScheduler.rescheduleAll()` and `WidgetUpdater.updateAll()`.
- **Pickers:** Storage Access Framework (`CreateDocument`, `OpenDocument`, `OpenDocumentTree` with `takePersistableUriPermission`); no storage permissions are requested.
- **Auto backup:** `BackupWorker` (periodic WorkManager, weekly, constraint: battery not low) writes to the chosen folder and prunes old files; failures surface as a status line on the Backup screen, not as a crash.
- **Android Auto Backup (Google):** `android:allowBackup="true"`, `android:dataExtractionRules` and `android:fullBackupContent` XML files that include the Room database files and the DataStore preferences and exclude caches and the `pre_restore_snapshot`.
- **Pause / archive:** repository methods `pauseHabit(id, from, until?)`, `resumeHabit(id)`, `archiveHabit(id)`, `restoreHabit(id)` (creates a `HabitPause` from `archivedAt` to today). All queries for Today, Weekly, Monthly, Overall, widgets and the scheduler filter `status = 'active'` and exclude habits with an open pause for the date. An auto-resume check runs at day rollover in `DailyMaintenanceWorker`.
- **Onboarding:** `OnboardingViewModel` with a `StateFlow` for the page and selections; `templates.json` in `assets/` parsed at first use (localized with `templates_ar.json`, `templates_fr.json`); the flag `onboardingDone` lives in DataStore; the start destination chooses `Onboarding` or `Home`.
- **Tests:** backup round-trip (export → import equals original), merge conflict rule, corrupted-checksum rejection, pause and archive effects on streaks and rates, template parsing.
