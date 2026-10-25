# Habit Radar (v5) — Architectural Blueprint & Implementation Roadmap

**Habit Radar** is a calm, private, 100% offline-first Android habit and milestone tracker built with Kotlin and Jetpack Compose. It pairs a high-craft dark aesthetic—soft pastel habit tints, serif headlines with a single *italicized* emphasis word, uppercase letter-spaced labels, and a floating pill tab bar—with deep statistical analytics, goal decomposition (Milestones), exact local reminders, interactive Jetpack Glance home-screen widgets, and zero-permission local backup/restore.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and product decisions have been confirmed and locked in for the implementation phases:

- **Confirmed Decision 1 — Monthly & Overall Tab Layouts**:
  - **Monthly Tab**: Uses the **2-column compact card grid** from the visual reference (each card showing the habit icon tile, title, streak flame, top-right check toggle for today, and a 7-column mini calendar grid of rounded squares for the month) combined with the specification's month navigation (`‹ OCTOBER 2026 ›`) and tap-to-backfill support.
  - **Overall Tab**: Implements the **Section 4.4 specification** (`ALL TIME` label, *"The long view."* serif headline, 5 summary stat cards, 12-week completion % bar chart, and per-habit completion-rate progress bars with current/best streak). The 53×7 GitHub-style year heatmap lives inside **Habit Detail (Section D)** and **Year in Review (Section 5.13)**.
- **Confirmed Decision 2 — Dependency Injection Architecture**:
  - **Manual DI Container (`AppContainer`)**: Uses a lightweight, type-safe Kotlin `AppContainer` initialized in `HabitRadarApplication` (providing Room DAOs, DataStore, repositories, domain use-cases, and schedulers to ViewModels and Glance widgets) rather than adding the Hilt Gradle plugin, avoiding AGP 9.1.1 plugin compatibility conflicts and keeping builds fast.
- **Confirmed Decision 3 — Custom Compose Canvas Charts**:
  - **100% Custom `Canvas` Visuals**: All trend line charts (with 4-point moving average and previous-period ghost lines), bar charts, 53×7 heatmaps, momentum rings, and goal step timelines will be built with custom Jetpack Compose `Canvas` components for pixel-perfect fidelity to the dark/pastel aesthetic.
- **Strict Scope Enforcement (Spec vs. Visual References)**:
  - **Excluded from Visual References**: We will **NOT** implement the *"Receive your gift! 63% off lifetime"* floating banner, any `🔒 PLUS` badges/locks (in Menu or Home Studio App Color), the purple promo banner at the top of Menu, or Menu rows for *What's new*, *Archive* (lives in Settings → DATA), *Account & Sync*, *COMMUNITY / WhatsApp Channel*, or *ABOUT (Rate / Share)*. Menu contains **exactly the 7 rows** specified in Section 5.7.

---

## 1. Overview & Core Concept

- **What It Does**: Allows users to track daily, weekly, and monthly habits (Task, Amount, or Time) alongside multi-step long-term Goals (Milestones). Provides real-time streak tracking, logical day rollover (default 03:00 AM for late-night habits), Hijri calendar integration, customizable home layouts (Classic, One thing, Ledger), deep per-habit analytics, automated Weekly/Monthly/Yearly recaps, 5 interactive home-screen widgets, and local JSON/CSV backups.
- **Target Audience / Persona**: Privacy-conscious individuals seeking a calm, distraction-free, non-gamified yet deeply analytical system to build daily consistency and break down large life goals into actionable steps without cloud accounts, subscriptions, or paywalls.
- **Key Value**: Complete ownership of personal data (no `INTERNET` permission), zero-latency local interactions, flexible scheduling (pauses and archives that protect streaks), and seamless synchronization between Habits and Goal Steps.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **First-Run Onboarding & Starter Templates**: 5-step skippable onboarding (*Small marks, every day* → *Pick a few to start* from localized categories → *Gentle reminders* with OS notification & exact alarm permissions → *Make it yours* with live preview → *All set* summary + one-time Today coachmark).
2. **Daily Check-In & Logical Day Rollover**: On the **Today** tab, users view their **day-signal band** (1 pill segment per scheduled habit today), filter by **Group chips** (or view section headers), and tap check circles (for Tasks) or open the **Amount stepper** / **Time timer sheet**. Completing the final habit triggers a 1.5-second *"All done."* confetti/glow overlay, soft tick sound, and haptic pop.
3. **Deep Habit Analytics & Inline Reminders**: Tapping a habit card opens **Habit Detail**, completely tinted in the habit's pastel color. Users switch between `7D · 30D · 90D · 1Y · All` to inspect momentum rings, moving-average trends, calendar/year heatmaps, weekday/hour histograms, consistency scores, miss analysis, and manage reminders directly in Section N without leaving the screen.
4. **Milestones & Automatic Habit-Linked Steps**: Users create a big **Goal** with a target date, use the offline **"Break it down"** generator (by count or amount), and link steps to Habits (`Streak reaches N`, `Total check-ins reach N`, or `Completion rate ≥ P%`). Whenever a habit entry is logged, linked goal steps update and auto-complete sticky with `doneAt`.
5. **Home Studio Live Customization**: Users customize Theme (`System | light | dark`), App accent color (20+ free pastel swatches), App bar elements, Today layout (`Classic | One thing | Ledger`), and Card density (`Comfortable | Compact`) while watching a scaled, live Today screen preview inside a phone bezel update in real time.

### Visual Identity & Theme
- **Aesthetic Direction**: Calm, editorial luxury dark UI with soft desaturated card surfaces, crisp 1dp hairlines, and high-contrast serif headlines.
- **Color Palette & Mood**:
  - `--background` (`bg`): `#0E0E11` (deep obsidian page background)
  - `--app-bar`: `#1A1B21` (header & top bar surface)
  - `--card`: `#17181D` (settings & neutral container cards)
  - `--surface-2`: `#25262C` (inactive segmented control track, hairlines, chips)
  - `--segment-selected`: `#0F1013` with bold `#F2F2F4` text
  - `--text-primary`: `#F2F2F4` | `--text-secondary`: `#8E8F98`
  - `--accent` (default): `#E8A8F5` (orchid, user-selectable from 21 swatches in Home Studio)
  - `--accent-container`: Derived dynamically (`accent` blended at ~20% opacity over `#0E0E11`, default `#2E2540`)
  - **Dynamic Habit Card Tinting**: Every habit/goal color (`H`) generates a dark desaturated card surface (`H` blended at 14% over `#121317`, e.g., green → `#16261F`, purple → `#201A2E`, orange → `#2C2018`), a slightly lighter icon tile background (`22%` blend), and a check circle fill at `65%–75%` saturation.
- **Typography & Hierarchy**:
  - **Headline Font**: Bundled Google Font **Instrument Serif** / **Fraunces** (regular + italic `.ttf` installed locally via `font-util`), `34sp`, rendered with a custom helper that automatically styles the designated emphasis word in *italic*.
  - **UI & Body Font**: Bundled **DM Sans** / **Plus Jakarta Sans** (`20sp` Medium for card titles, `15sp` Regular for body, `13sp` SemiBold uppercase with `3sp` letter-spacing for section labels like `PREFERENCES`, `GENERAL`, `HABIT COLOR`).
- **Component Styling & Layout**:
  - Screen horizontal padding `16dp`; card corner radius `28dp`; icon tile `64dp` (`20dp` radius in Comfortable, `44dp` in Compact).
  - Floating bottom pill tab bar (`radius 32dp`, text-only tabs, `12dp` above navigation bar insets, lists padded `120dp` at bottom).

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Logical Day Rollover (`LogicalDate`) Architecture
- **Chosen Approach**: Centralize all date/time queries through a `LogicalTimeProvider` that subtracts the user's `dayRollover` offset (default `03:00`) from the current `LocalDateTime` before deriving `LocalDate`.
- **Why**: Late-night habits (e.g., *"Lights out by 12"* checked at 01:15 AM) must count toward the previous calendar day across Today, Streaks, Recaps, Reminders, and Glance Widgets without clock drift.
- **Alternatives Considered**: Raw `LocalDate.now()` calls scattered across ViewModels (rejected because it causes midnight split-brain bugs between widgets and UI).

### Decision 2: Missing Details & Specification Conflict Resolutions
During analysis of the v5 specification and visual references, we identified and resolved the following edge cases without inventing out-of-scope features:
1. **MinSdk Alignment (`minSdk 26` vs Template `24`)**: The template defaults to `minSdk = 24`, while Spec Section 1 mandates `minSdk 26` for native `java.time` and `HijrahChronology`. We will update `minSdk = 26` in Phase 1.
2. **Firebase / Network Removal**: The starter template includes `firebase-bom`, `retrofit`, `okhttp`, and `google-services` in `app/build.gradle.kts`. Per Spec Section 1 (*"Not allowed: Firebase, analytics/ads SDKs, any network library. No INTERNET permission"*), Phase 1 strips all network and Firebase dependencies from the build configuration.
3. **Weekly & Monthly Quota Streak Evaluation**: For habits repeating *"X times per week"* or *"X times per month"*, Spec Section 7.1 defines streaks as consecutive periods meeting their target, while daily cards show a flame counter. We will compute both period streaks and cumulative period progress (`2 / 3 this week`) so the Today/Weekly cards and Habit Detail remain consistent.
4. **Menu Navigation vs Archive Access**: Screenshot 7 shows *Archive* in Menu, whereas Spec Section 5.7 explicitly locks Menu to **seven rows** (*Notifications, Settings, Weekly recap, Monthly recap, Year in Review, Milestones, Widgets*) and places **Archived habits** under `Settings → DATA`. We strictly follow Section 5.7.
5. **Data Export Scope Note**: Spec Section 9 contains a leftover bullet (*"Data export/import is out of scope"*) that contradicts Sections 0.13, 5.9, 5.17, 11, and 13.9, which fully specify **Backup & Restore (JSON/CSV + SAF Auto-Backup)**. Per Section 0 (*Scope IN #13*) and the user prompt explicitly requesting a Backup and Restore phase, full local Backup & Restore is implemented.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                         UI LAYER (Jetpack Compose + Glance)                       │
│                                                                                   │
│  ┌─────────────────────────┐  ┌──────────────────────────┐  ┌──────────────────┐  │
│  │   Main Tab Screens      │  │   Deep Analytics & Goal  │  │  Jetpack Glance  │  │
│  │ Today (Classic/One/Ledg)│  │ HabitDetail (Sec A–N)    │  │ 5 AppWidgets +   │  │
│  │ Weekly / Monthly (2-col)│  │ Milestones / GoalDetail  │  │ ConfigActivity + │  │
│  │ Overall (Stats + Chart) │  │ Weekly / Monthly / Year  │  │ Pin Previews     │  │
│  └────────────┬────────────┘  └─────────────┬────────────┘  └────────┬─────────┘  │
│               │                             │                        │            │
│               ▼                             ▼                        ▼            │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │              ViewModels (Unidirectional StateFlow<UiState>)                 │  │
│  └──────────────────────────────────────────┬──────────────────────────────────┘  │
└─────────────────────────────────────────────┼─────────────────────────────────────┘
                                              │
┌─────────────────────────────────────────────┼─────────────────────────────────────┐
│                                 DOMAIN & USE-CASE LAYER                           │
│                                             ▼                                     │
│  ┌─────────────────────────┐  ┌──────────────────────────┐  ┌──────────────────┐  │
│  │   Habit & Streak Engine │  │   Analytics & Insights   │  │ Goal Step Engine │  │
│  │ LogicalDateCalculator   │  │ Momentum & Consistency   │  │ Auto-Complete    │  │
│  │ ScheduleMatcher         │  │ Trend & Heatmap Builder  │  │ Pace & Finish    │  │
│  │ Streak & Run Calculator │  │ Recap Generator (W/M/Y)  │  │ Date Projector   │  │
│  └────────────┬────────────┘  └─────────────┬────────────┘  └────────┬─────────┘  │
└───────────────┼─────────────────────────────┼────────────────────────┼────────────┘
                │                             │                        │
┌───────────────┼─────────────────────────────┼────────────────────────┼────────────┐
│                                   DATA & SYSTEM LAYER                             │
│               ▼                             ▼                        ▼            │
│  ┌─────────────────────────┐  ┌──────────────────────────┐  ┌──────────────────┐  │
│  │   HabitRadarDatabase    │  │  Preferences DataStore   │  │ System Services  │  │
│  │ Habits, Entries, Pauses │  │ Theme, Accent, Layout,   │  │ AlarmManager     │  │
│  │ Reminders, Goals, Steps │  │ Rollover, Nudges, Quiet, │  │ WorkManager      │  │
│  │ WidgetConfigs (Room)    │  │ TabOrder, AutoBackup     │  │ SoundPool / SAF  │  │
│  └─────────────────────────┘  └──────────────────────────┘  └──────────────────┘  │
└───────────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State
- **Room Database (`HabitRadarDatabase` v1, `exportSchema = true`)**:
  - `HabitEntity`: `id`, `name`, `description`, `iconKey`, `colorKey`, `trackType` (`TASK`, `AMOUNT`, `TIME`), `targetValue`, `unit`, `comparison` (`AT_LEAST`, `AT_MOST`, `EXACTLY`), `repeatType` (`DAILY`, `WEEKLY`, `MONTHLY`), `daysOfWeek` (`List<Int>`), `timesPerPeriod`, `daysOfMonth` (`List<Int>`), `groupName`, `goalStreak`, `askNote`, `status` (`ACTIVE`, `ARCHIVED`), `archivedAt`, `startDate`, `endDate`, `sortOrder`, `createdAt`.
  - `EntryEntity`: `id`, `habitId` (FK Cascade), `date` (`LocalDate`), `status` (`DONE`, `SKIPPED`), `value` (`Double?`), `note`, `completedAt` (`Instant?`). Unique index on `(habitId, date)` and index on `(date)`.
  - `ReminderEntity`: `id`, `habitId` (FK Cascade), `time` (`LocalTime`), `daysOfWeek`, `message`, `skipIfDone`, `repeatAfterMin`, `sound`, `vibrate`, `quickActions`, `enabled`.
  - `HabitPauseEntity`: `id`, `habitId` (FK Cascade), `startDate`, `endDate` (`LocalDate?`).
  - `GoalEntity`: `id`, `title`, `description`, `iconKey`, `colorKey`, `startDate`, `targetDate`, `status` (`ACTIVE`, `COMPLETED`), `completedAt`, `sortOrder`, `remindWeekly`, `weeklyDay`, `weeklyTime`, `remindBeforeDays`, `beforeTime`, `remindStepDue`.
  - `StepEntity`: `id`, `goalId` (FK Cascade), `title`, `note`, `dueDate`, `sortOrder`, `type` (`MANUAL`, `HABIT`, `AMOUNT`), `linkedHabitId` (FK `SET NULL`), `habitMetric` (`STREAK`, `TOTAL_CHECKINS`, `RATE`), `habitTarget`, `habitWindowDays`, `amountTarget`, `amountCurrent`, `amountUnit`, `done`, `doneAt`.
  - `WidgetConfigEntity`: `appWidgetId` (PK), `type`, `habitIds`, `goalId`, `theme`, `opacity`, `cornerStyle`, `showStreak`, `hideNames`, `allowBackfill`.

---

## 5. Step-by-Step Implementation Roadmap (12 Phases)

### Implementation Order & Milestone Overview

| Phase | Milestone Name | Core Focus | Prerequisite |
| :--- | :--- | :--- | :--- |
| **Phase 1** | Project Setup, Offline Sanitization & Architecture | Gradle cleanup, `minSdk 26`, fonts, app icon, DI container | Initial Workspace |
| **Phase 2** | Database, DataStore & Core Domain Engine | Room 7 tables, `LogicalDate`, Streak & Schedule calculators | Phase 1 |
| **Phase 3** | Design System, App Shell & Habit Creation | Theme tokens, dynamic tints, floating tab bar, New/Edit Habit | Phase 2 |
| **Phase 4** | Core Tab Screens (Today, Weekly, Monthly, Overall) | Classic/One thing/Ledger, 2-col Monthly, Overall stats, sound/haptics | Phase 3 |
| **Phase 5** | Settings, Home Studio, Reorder & Pause/Archive | Live phone preview, 21 accents, Pause/Archive flows & screens | Phase 4 |
| **Phase 6** | Notification Engine, Per-Habit Reminders & Menu | `AlarmManager`, receivers, quick actions, Notifications screen, Menu | Phase 5 |
| **Phase 7** | Habit Detail — Deep Analytics (Sections A–N) | Momentum, custom Canvas charts, heatmaps, insights, inline reminders | Phase 6 |
| **Phase 8** | Milestones (Goals, Steps, Breakdown & Auto-Sync) | Goal CRUD, "Break it down", timeline, pace projection, habit-step sync | Phase 7 |
| **Phase 9** | Automated Recaps (Weekly, Monthly & Year in Review) | Weekly/Monthly scroll cards, Year Story pager + Summary mode | Phase 8 |
| **Phase 10** | Home-Screen Widgets (Jetpack Glance) | 5 Glance widgets, Week Grid reference design, pinning, ConfigActivity | Phase 6 & 8 |
| **Phase 11** | Backup & Restore + Onboarding & Starter Templates | JSON/CSV SAF export/import, auto-backup, 5-page onboarding, templates | Phase 8 & 10 |
| **Phase 12** | Localization (EN/AR/FR), Hijri, Testing & Release | Full RTL Arabic + French strings, Robolectric/unit tests, R8 release | All Phases |

---

### Phase 1: Project Setup, Offline Sanitization & Core Architecture

#### Specific Implementation Tasks
1. **Sanitize Gradle & Permissions for Strict Offline Compliance**:
   - Update `app/build.gradle.kts`: set `applicationId = "com.aistudio.habitradar.kxmpzq"`, `minSdk = 26`, enable `kotlinx-serialization` plugin, and remove/comment out all Firebase (`firebase-bom`, `firebase-ai`, `firebase-appcheck`), Retrofit, and OkHttp dependencies.
   - Add required offline libraries to `gradle/libs.versions.toml` and `app/build.gradle.kts`: `androidx.navigation.compose`, `androidx.datastore.preferences`, `androidx.work.runtime.ktx`, `androidx.glance.appwidget`, `androidx.glance.material3`, `androidx.appcompat` (for per-app language locales), and `kotlinx.serialization.json`.
   - Configure `AndroidManifest.xml`: declare `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, and `VIBRATE`. Explicitly verify **no `INTERNET` permission** exists.
2. **Sync App Identity & Custom Adaptive Launcher Icon**:
   - Update `metadata.json` and `app/src/main/res/values/strings.xml` (`app_name` = `"Habit Radar"`) and `settings.gradle.kts` (`rootProject.name = "Habit Radar"`).
   - Generate and configure a custom adaptive launcher icon matching the dark orchid/radar aesthetic (`ic_launcher_background.xml`, `ic_launcher_foreground.xml`, and density PNGs).
3. **Install Bundled Local Google Fonts**:
   - Run `font-util install "Instrument Serif" --target app/src/main/res/font` and `font-util install "Plus Jakarta Sans" --target app/src/main/res/font` (or `DM Sans`) so headlines and body text work 100% offline.
4. **Bootstrap Manual DI & Navigation Shell**:
   - Create `HabitRadarApplication` and `AppContainer` for constructor-injected repositories and use-cases.
   - Define type-safe `@Serializable` navigation routes for all 21 screens/sheets and deep-link URI patterns (`habitradar://today`, `habitradar://habit/{id}`, `habitradar://goal/{id}`, `habitradar://recap/{type}`).

#### Dependencies & Prerequisites
- Existing project workspace (`AGP 9.1.1`, `Kotlin 2.2.10`, `KSP 2.3.5`).

#### Expected Deliverables
- Clean, offline-only `build.gradle.kts` and `libs.versions.toml`.
- Local `.ttf` font files in `res/font/` and custom adaptive launcher icon across all `mipmap-*` folders.
- `HabitRadarApplication`, `AppContainer`, and type-safe route definitions compiling cleanly.

#### Acceptance Criteria & Tests
- **Definition of Completion**: `compile_applet` succeeds with zero network/Firebase libraries in the dependency graph; merged `AndroidManifest.xml` contains zero `android.permission.INTERNET` entries; fonts and adaptive icon resources resolve without AAPT2 errors.

#### Technical Risks & Possible Solutions
- **Risk**: `androidx.glance` Compose compiler compatibility with Kotlin 2.2.10.
- **Solution**: Since the project uses `org.jetbrains.kotlin.plugin.compose`, Glance composables compile natively with the Kotlin 2.2 Compose compiler plugin; keep Glance UI classes isolated in `feature/widgets/glance` with standard Glance modifiers.

---

### Phase 2: Database Layer, Preferences DataStore & Core Domain Engine

#### Specific Implementation Tasks
1. **Implement Room Database (`HabitRadarDatabase`)**:
   - Create all 7 Room entities (`HabitEntity`, `EntryEntity`, `ReminderEntity`, `HabitPauseEntity`, `GoalEntity`, `StepEntity`, `WidgetConfigEntity`) with full foreign key constraints (`CASCADE` on Habit/Goal children, `SET NULL` on `StepEntity.linkedHabitId`) and required composite indexes.
   - Write `Converters.kt` for `LocalDate`, `LocalTime`, `Instant`, enums, and `List<Int>` / `List<String>`.
   - Create reactive DAOs (`HabitDao`, `EntryDao`, `ReminderDao`, `PauseDao`, `GoalDao`, `WidgetConfigDao`) returning `Flow<T>` and transactional batch queries for week/month matrices.
2. **Implement `SettingsDataStore`**:
   - Persist all 35+ settings fields defined in Section 6 (theme, accent hex, week start, sounds, daily nudges, card density, streak visibility per tab, Hijri toggle, day rollover time, greeting/band/headline mode, today layout, hide completed, celebrations, group sections/filter, tab order/visibility, quiet hours, snooze, recap toggles, onboarding & backup metadata).
3. **Build Pure Kotlin Domain Calculators**:
   - `LogicalDateCalculator`: Computes current logical `LocalDate` using `dayRollover` (default `03:00`) and formats optional Umm al-Qura `HijrahDate`.
   - `ScheduleMatcher`: Evaluates whether a `Habit` is scheduled on a given `LocalDate`, accounting for `startDate`, `endDate`, `status`, active `HabitPause` ranges, and `Daily` / `Weekly` / `Monthly` repeat rules.
   - `StreakCalculator`: Computes current streak, longest streak ever, streak history intervals, and "consecutive day run" (where skipped and paused days neither break nor increment streaks, and current streak remains alive through today).

#### Dependencies & Prerequisites
- Phase 1 completed.

#### Expected Deliverables
- Complete `core/data/db` and `core/data/datastore` packages.
- Pure Kotlin domain engine in `core/domain/usecase` with repository implementations (`HabitRepository`, `GoalRepository`, `SettingsRepository`).

#### Acceptance Criteria & Tests
- **Unit & Robolectric Tests**:
  - `LogicalDateCalculatorTest`: Verify that 02:30 AM with a 03:00 rollover resolves to yesterday's `LocalDate`, and 03:01 AM resolves to today.
  - `StreakCalculatorTest`: Verify daily, Mon/Thu/Sun, and X-times-per-week streaks across done, missed, skipped, and paused date ranges.
  - `HabitDaoTest` (Robolectric in-memory Room): Verify unique `(habitId, date)` replacement, cascade deletes, and `StepEntity.linkedHabitId` turning `NULL` (and step converting to `MANUAL`) when a linked habit is deleted.
- **Definition of Completion**: All DAO and domain unit tests pass via `gradle :app:testDebugUnitTest`.

#### Technical Risks & Possible Solutions
- **Risk**: N+1 SQLite queries when computing streaks across dozens of habits on the Today/Weekly tabs.
- **Solution**: Fetch all `HabitEntity`, `EntryEntity`, and `HabitPauseEntity` records reactively in bulk `Flow` streams and compute per-habit streaks in-memory on `Dispatchers.Default`.

---

### Phase 3: Design System, App Shell & Habit Creation Flow

#### Specific Implementation Tasks
1. **Build `HabitRadarTheme` & Color Tint Engine**:
   - Implement `Color.kt` and `Theme.kt` with the exact dark tokens (`#0E0E11` bg, `#1A1B21` app-bar, `#17181D` card, `#25262C` surface-2, `#E8A8F5` default orchid accent) plus light theme counterparts.
   - Write `habitTint(colorHex, isDark)`: deterministic HSL/RGB blending that turns any of the 16+ pastel habit swatches into its dark desaturated card background, icon tile surface, and check circle stroke/fill.
   - Create `SerifHeadline(text: String)` composable that parses `*italic*` markdown tokens into an `AnnotatedString` with `InstrumentSerifItalic`.
2. **Build Reusable UI Components & Floating Pill Tab Bar**:
   - Custom segmented pill control (`#25262C` container, `#0F1013` selected pill, `28dp` radius, `56dp` / `48dp` height).
   - Custom Material 3 switch matching the orchid-on / dark-grey-off aesthetic.
   - Floating bottom pill tab bar (`radius 32dp`, dynamic tab order and visibility from DataStore, hidden when *Show home tab bar* is OFF).
3. **Implement `New / Edit Habit` Full-Height Sheet (Section 5.5)**:
   - Top bar (`Cancel` · `NEW HABIT` / `EDIT HABIT` · `Save` pill disabled until valid).
   - *Browse ideas* pill (hooks into Template Gallery), `135dp` glowing icon tile + searchable **Icon Picker** (~200 categorized Material symbols + Emoji tab), centered serif name input (*"Name it plainly"*), description, 2-row horizontal pastel swatch grid (`46dp` rounded squares with inset + light-blue outer selection ring).
   - `TRACK` (`Task | Amount | Time` with target, unit, and `at least / at most / exactly` comparison), `REPEAT` (`Daily` 7-day chips with dynamic summary like `Everyday` / `Mon, Thu, Sun`, `Weekly` 1–7 stepper, `Monthly` 1–31 grid or X times), `MORE` collapsible section (start/end dates, Group chips + custom group, goal streak, ask for note).
   - Edit mode footer: **Pause habit**, **Archive habit**, and **Delete habit** (with mandatory dialog offering **Archive** as primary vs **Delete forever** in red).

#### Dependencies & Prerequisites
- Phase 2 completed.

#### Expected Deliverables
- Complete `core/ui/theme` and `core/ui/components` library.
- Working `EditHabitSheet` and `IconPickerSheet` connected to `HabitRepository`.

#### Acceptance Criteria & Tests
- **Definition of Completion**: Users can create and edit Task, Amount, and Time habits with any repeat schedule, color swatch, and icon; validation prevents saving empty names or 0 targets; visual styling matches Screenshot 8 (`NEW HABIT`).

#### Technical Risks & Possible Solutions
- **Risk**: Full-height bottom sheet keyboard overlap when editing fields inside `MORE` or `REMINDERS`.
- **Solution**: Apply `Modifier.imePadding()` and `verticalScroll` inside the sheet content with automatic `BringIntoViewRequester` on focused text fields.

---

### Phase 4: Core Tab Screens (Today, Weekly, Monthly, Overall) & Check-In Mechanics

#### Specific Implementation Tasks
1. **Build `TodayScreen` (Section 4.1 & Screenshot 9)**:
   - App bar with uppercase date (`SATURDAY 24 OCTOBER` + optional Hijri date), dynamic serif greeting (*"Good to see you."*, *"Almost there."*, *"All done."*), outlined `≡` Menu button, and accent `+` FAB.
   - **Day-signal band**: equal-width `10dp`-tall rounded segments for today's scheduled habits + dynamic caption (`Count`, `Percent`, or `Days` run).
   - **Group filter chips** (`All | Morning | Evening | …`) or **Section headers** (`MORNING`, `EVENING`) per Home Studio setting.
   - **Three Today Layouts**:
     - *Classic*: Stacked `28dp`-radius tinted cards (`112dp` Comfortable / `76dp` Compact), `64dp` icon tile, title (strikethrough when done), 🔥 streak counter, `64dp` check circle (or progress ring `3/8` for Amount / duration for Time), thin lavender outline ring on the first incomplete ("Next up") card.
     - *One thing*: Single large focus card with swipe left/right navigation + two thin preview rows below, auto-advancing on completion.
     - *Ledger*: Dense text rows with hairline separators, small flame + streak, and compact check circle.
   - **Paused (`N`)** expandable footer row with **Resume** button.
   - **Interactions**: Tap circle (spring pop animation + haptic + `SoundPool` soft tick + optional note prompt), Amount stepper bottom sheet, Time timer/manual bottom sheet (with active countdown state), swipe-left to skip today (with Undo snackbar), long-press context menu, and 1.5s *"All done."* full-screen Canvas celebration overlay when the last habit is completed.
2. **Build `WeeklyScreen` (Section 4.2 & Screenshot 10)**:
   - Week range label (`19 OCT - 25 OCT` respecting `weekStart`), horizontal swipe to change weeks, serif headline (*"A week that's working."*), tinted habit cards with right-aligned schedule summary (`Mon, Thu, Sun` / `Everyday`), and 7-column day circles (`44dp`) with a vertical rounded capsule highlighting **Today's column**. Support tapping past/today circles to back-fill (up to 1 year) and lock future days.
3. **Build `MonthlyScreen` (Section 4.3 & Screenshot 5 — Confirmed 2-Column Grid)**:
   - Month label (`OCTOBER 2026`) with prev/next arrows, serif headline (*"A month in motion."*), and a **2-column LazyVerticalGrid of tinted habit cards**. Each card displays its icon tile, title, streak 🔥, top-right today check circle, and a 7-column mini calendar grid of rounded squares (`done` filled in habit color, `not scheduled` dim, `today` highlighted). Tapping any valid day square toggles completion (back-fill); tapping the card header opens Habit Detail.
4. **Build `OverallScreen` (Section 4.4 — Confirmed Spec Layout)**:
   - Label `ALL TIME`, serif headline *"The long view."*, 5 summary stat cards (total check-ins, best current streak, longest streak ever, overall completion rate, active habits), custom Canvas **12-week completion % bar chart**, and per-habit list showing icon, title, habit-colored completion-rate bar, and current/best streak.

#### Dependencies & Prerequisites
- Phase 3 completed.

#### Expected Deliverables
- Fully interactive `TodayScreen`, `WeeklyScreen`, `MonthlyScreen`, and `OverallScreen` with audio/haptic feedback and celebration overlay.

#### Acceptance Criteria & Tests
- **Definition of Completion**: Toggling a habit on Today, Weekly, or Monthly immediately updates streaks, the day-signal band, and Overall stats; Amount stepper and Time timer sheets log quantitative values accurately; swipe-to-skip and Undo snackbars work; UI matches Screenshots 5, 9, and 10 (without the promo banner).

#### Technical Risks & Possible Solutions
- **Risk**: Touch target conflicts on the 2-column Monthly mini-calendar grid where 7 columns fit inside half the screen width.
- **Solution**: Provide both direct tap on individual mini-grid cells and a quick day-picker bottom sheet if the user taps the calendar body on narrow screens, ensuring effortless back-filling while maintaining Accessibility compliance.

---

### Phase 5: Settings, Home Studio (Live Preview), Reorder & Pause/Archive Flows

#### Specific Implementation Tasks
1. **Build `SettingsScreen` (Section 5.9 & Screenshot 4)**:
   - Top **Home Studio** entry card, **PREFERENCES** card (*Week start on* 3-segment control, *Sounds*, *Daily nudges*, *Card density*, *Reorder habits*, *Show streak icon on* Today/Weekly/Monthly toggles, *Show Hijri date*, *Day starts at* time picker default `03:00`, *Change Language* sheet for English / العربية / Français), **DATA** card (*Backup & restore* with last backup status, *Archived habits* with count, *Welcome tour*), and offline privacy footer.
2. **Build `HomeStudioScreen` (Section 5.10 & Screenshots 1, 2, 3)**:
   - **Live Preview**: Centered phone bezel (`~60%` width, white rounded frame) rendering the actual Today composable scaled via `graphicsLayer` with non-interactive demo state that reacts instantaneously to every setting change below.
   - **Theme & App Color**: `System | light | dark` segmented control and 21 free pastel accent swatches (6 per row, white ring + check on selected, **no `PLUS` lock badge**).
   - **App Bar, Today Layout & Density**: Greeting toggle, day-signal band toggle, Headline chips (`Count | Percent | Days`), 3 visual layout selector cards (`Classic`, `One thing`, `Ledger`), Card density (`Comfortable | Compact`), *Hide completed habits*, *Full-screen celebrations*, *Group habits into sections*, *Reorder habits* row, and **Bottom bar** group (*Show home tab bar* toggle + *Reorder tabs* row).
3. **Build `ReorderHabitsScreen` & `ReorderTabsScreen` (Section 5.11)**:
   - Drag-and-drop list (`☰` handles) updating `HabitEntity.sortOrder` and optional Group assignment; tab reorder list where `Today` is locked visible and `Weekly / Monthly / Overall` can be reordered or hidden.
4. **Implement Pause & Archive Flows + `ArchivedHabitsScreen` (Section 5.18)**:
   - **Pause Sheet**: `Tomorrow · 3 days · 1 week · Choose date · Until I resume` creating a `HabitPauseEntity` record.
   - **Archived Habits Screen**: Lists archived habits (`Archived 12 Oct · best streak 21 · 134 check-ins`) with tap for read-only Habit Detail, **Restore** (inserting a `HabitPause` over the archived duration so streaks never break), and **Delete permanently**.

#### Dependencies & Prerequisites
- Phase 4 completed.

#### Expected Deliverables
- Complete `SettingsScreen`, `HomeStudioScreen`, `ReorderHabitsScreen`, `ReorderTabsScreen`, `PauseHabitSheet`, and `ArchivedHabitsScreen`.

#### Acceptance Criteria & Tests
- **Definition of Completion**: Changing any option in Home Studio updates both the miniature phone preview and the real Today screen; pausing a habit hides it into `Paused (N)` and protects its streak; restoring an archived habit preserves its pre-archive streak.

#### Technical Risks & Possible Solutions
- **Risk**: Recomposition lag in Home Studio when dragging or rapidly tapping 21 color swatches while rendering the scaled Today preview.
- **Solution**: Pass a lightweight, immutable `PreviewUiState` to the miniature Today preview composable and use `graphicsLayer` for scaling so layout measurement only runs when structural toggles (like `todayLayout` or `cardDensity`) change.

---

### Phase 6: Notification Engine, Per-Habit Reminders, Notifications Screen & Menu

#### Specific Implementation Tasks
1. **Build `MenuScreen` (Section 5.7 & Screenshot 7 Reference)**:
   - Render the `GENERAL` card with **exact seven rows**: *Notifications* (with accent circular badge showing count of enabled habit reminders; hidden if 0), *Settings*, *Weekly recap*, *Monthly recap*, *Year in Review* (with trailing `2026`), *Milestones* (with trailing `X active` if > 0), and *Widgets* (`Add to home screen`). Exclude all paywall/community/promo elements.
2. **Implement Android `AlarmManager` Notification Engine (Sections 8 & 13.6)**:
   - Create notification channels at startup: `habit_reminders` (High), `daily_nudges` (Default), `recaps` (Low), `goals` (Default).
   - Build `ReminderScheduler` using `setExactAndAllowWhileIdle` (with graceful fallback to `setAndAllowWhileIdle` if exact alarm permission is denied on Android 12+).
   - Implement `ReminderReceiver` (verifies `skipIfDone`, habit active/unpaused status, and **Quiet Hours** *Mute* vs *Delay until end* mode; builds notification with optional **Done · Snooze · Skip** action buttons and schedules repeat-after-N-min follow-up up to 2 times).
   - Implement `NotificationActionReceiver` (`goAsync()` + coroutine to mark Entry `DONE` or `SKIPPED`, or snooze by `snoozeMin` up to 3 times, then refresh widgets) and `BootReceiver` (`BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`).
   - Implement `DailyMaintenanceWorker` (WorkManager) for day-rollover alarm re-sync, auto-resuming expired pauses, evaluating habit-linked steps, and scheduling Morning Kickoff, Evening Save, Recap, Goal, and monthly Backup Nudge notifications.
3. **Build Per-Habit Reminders UI & `NotificationsScreen` (Sections 5.5 #7, 5.6 #N, 5.8)**:
   - Reusable `HabitRemindersSection` and `ReminderEditorSheet` (shared between Edit Habit and Habit Detail Section N, with quick presets `Morning 07:00`, `Noon 12:00`, `Evening 20:00`, `Night 22:00`, max 5 reminders).
   - `NotificationsScreen`: OS Permission & Exact Alarm status banner, **DAILY NUDGES** card (Morning kickoff `08:00` & Evening save `21:00`), **HABIT REMINDERS** list (with time chips, toggle, and "No reminder" section with `Add`), **GOAL REMINDERS** list, **PREFERENCES** card (Quiet hours, Snooze length, Sound, Vibrate, Quick actions, Weekly/Monthly/Yearly recap toggles, Goal master toggle), and **Send test notification** button.

#### Dependencies & Prerequisites
- Phase 5 completed.

#### Expected Deliverables
- Full `core/notifications` package, manifest declarations, `MenuScreen`, `NotificationsScreen`, and shared `ReminderEditorSheet`.

#### Acceptance Criteria & Tests
- **Unit & Robolectric Tests**:
  - `ReminderSchedulerTest`: Verify next-fire timestamp calculation across weekdays, quiet-hours delay mode, and paused habits.
- **Definition of Completion**: Adding/toggling a reminder in Edit Habit, Habit Detail, or Notifications screen stays 100% synchronized, updates the Menu badge count, and schedules the exact alarm; tapping "Send test notification" displays a working notification with Done/Snooze/Skip actions.

#### Technical Risks & Possible Solutions
- **Risk**: Android 14+ crashes if `setExactAndAllowWhileIdle` is called without `canScheduleExactAlarms() == true`.
- **Solution**: Always guard exact alarm calls with `Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()`, falling back to `setAndAllowWhileIdle` and surfacing a clear one-tap settings banner in `NotificationsScreen`.

---

### Phase 7: Habit Detail — Deep Analytics (Sections A–N)

#### Specific Implementation Tasks
1. **Build Analytics Calculation & Caching Use-Case (`CalculateHabitAnalyticsUseCase`)**:
   - Compute all Section 7 metrics on `Dispatchers.Default`: period completion rate + delta vs previous period, **Momentum score** (`0.5×rate14 + 0.3×rate30 + 0.2×rate90` with *Cold / Warming / Strong / On fire* labels), next streak milestone (`3, 7, 14, 21, 30, 50, 75, 100, 150, 200, 300, 365, 500, 1000`), bucketed trend series + 4-point moving average + previous-period ghost series, Month & 53×7 Year heatmaps, Weekday performance bars, 24-hour `completedAt` histogram + peak 2-hour window + average delay after reminder, Streak history timeline + longest gap, **Consistency score** (`100 - min(100, stddev(weeklyRates) * 2)`), **Bounce-back rate**, **Perfect weeks/months**, **Recovery time**, Volume chart vs target, Amount/Time statistics (mean, median, min, max, total, goal-hit rate, linear-regression weekly trend slope, histogram, personal best), Miss analysis (most-missed weekday, post-skip correlation, last 5 misses), and scored **Insight Cards** (top 5 from Section 7.3 templates).
2. **Build `HabitDetailScreen` (Section 5.6)**:
   - Top app bar (`←` back, title, `🔔` bell scrolling to Section N with active dot indicator, `✏️` edit, `⋮` overflow with *Skip today, Reset today, Pause/Resume, Archive/Restore, Add widget, Delete*), sticky period selector (`7D · 30D · 90D · 1Y · All`), and read-only banner for archived habits.
   - Render all **14 tinted cards (Sections A through N)** with interactive touch tooltips on charts, a **Day Entry Editor Sheet** when tapping any calendar/miss/history date, "Make this a goal" button in Section B, and the inline **REMINDERS** management card in Section N.

#### Dependencies & Prerequisites
- Phase 6 completed.

#### Expected Deliverables
- `CalculateHabitAnalyticsUseCase`, custom Compose `Canvas` chart components, and complete `HabitDetailScreen`.

#### Acceptance Criteria & Tests
- **Unit Tests**:
  - Verify Momentum formula, Consistency stddev formula, Bounce-back rate, Recovery time, linear regression slope for Amount/Time habits, and Insight template scoring on deterministic synthetic datasets.
- **Definition of Completion**: All 14 sections (A–N) render accurately with the habit's tint; switching `7D / 30D / 90D / 1Y / All` updates sections marked `⏱`; tapping any calendar cell or history row opens the entry editor; Section N manages reminders inline.

#### Technical Risks & Possible Solutions
- **Risk**: Scroll jank when rendering 14 complex cards with multiple Canvas charts in a single column.
- **Solution**: Use `LazyColumn` with stable item keys (`section_a` .. `section_n`), memoize path and coordinate calculations inside `remember(period, analyticsData)`, and downsample `1Y / All` line chart points.

---

### Phase 8: Milestones — Goals, Steps, Breakdown Helper & Habit Auto-Sync

#### Specific Implementation Tasks
1. **Build `MilestonesListScreen` (Section 5.15 A)**:
   - Top bar (`← Milestones`, `+` accent button), serif headline *"Big goals, small steps."*, `Active | Done` segmented control, overall summary progress bar (`3 goals · 12 of 30 steps done`), tinted **Goal cards** (icon tile, title, `4 of 7 steps`, progress bar with `%`, due chip `12 days left` / `Overdue 3 days`, inline `Next: ...` step check circle or habit progress `9 / 14 days`, expandable chevron showing next 3 unfinished steps, drag-to-reorder), and empty state with 3 pre-filled starter example chips (`Read 12 books`, `Memorize a chapter`, `Save for a laptop`).
2. **Build `EditGoalSheet`, `"Break it down"` Helper & `StepEditorSheet` (Section 5.15 B & C)**:
   - Goal form: icon, serif name (*"Name the big goal"*), description, color swatch, Finish By date picker + quick chips (`1 week · 1 month · 3 months · 1 year`), draggable steps list, offline **Break it down** generator sheet (*By count* `2–30` with pattern `Step {n}` or *By amount* `Total / Per step / unit`, plus *Spread due dates evenly*), and Goal Reminders toggles (*Weekly check-in*, *Before finish date*, *Step due dates*).
   - `StepEditorSheet`: title, note, due date, and `Manual | Habit | Amount` segmented type selector (with Habit picker + rule selector: `Streak reaches N`, `Total check-ins reach N`, `Completion rate ≥ P% over D days`).
3. **Build `GoalDetailScreen` & Habit-to-Step Auto-Evaluation (Section 5.15 D & E)**:
   - Tinted scroll cards: **Hero** (ring, countdown), **Next step card** (big check button + *Skip for now*), **Steps timeline** (custom `drawBehind` vertical connector line, read-only badge for automatic Habit steps, stepper for Amount steps, drag reorder, swipe delete), **Pace & prediction** (4-week steps bar chart, projected finish date with `Ahead / On track / Behind` chip), **Steps over time** cumulative chart with dashed ideal line, and **Activity log**.
   - Auto-complete celebration overlay (*"Goal reached."*) when the final step finishes + hook into `EntryRepository` so any habit check-in immediately re-evaluates linked `StepEntity` rules and marks them done sticky.

#### Dependencies & Prerequisites
- Phase 7 completed (including "Make this a goal" entry point from Habit Detail).

#### Expected Deliverables
- Complete `feature/milestones` package (`MilestonesListScreen`, `GoalDetailScreen`, `EditGoalSheet`, `BreakItDownSheet`, `StepEditorSheet`) and `EvaluateGoalStepsUseCase`.

#### Acceptance Criteria & Tests
- **Unit Tests**:
  - `GoalProgressAndProjectionTest`: Verify weighted step progress clamping (`clamp(current / target)`), 28-day pace projection (`Ahead / On track / Behind`), and sticky auto-completion of habit-linked steps.
- **Definition of Completion**: Users can create goals manually, via starter chips, via "Break it down", or from Habit Detail; habit-linked steps update live and auto-complete when the habit metric is reached; Goal reminders appear in `NotificationsScreen`.

#### Technical Risks & Possible Solutions
- **Risk**: Deleting a habit that is linked to a Goal step could break Goal progress calculations.
- **Solution**: Room foreign key `onDelete = ForeignKey.SET_NULL` combined with a repository trigger converts any orphaned habit step to `type = MANUAL`, preserving its current `done` state and appending `"Habit removed"` to its note per Section 5.15 E.

---

### Phase 9: Automated Recaps (Weekly Recap, Monthly Recap & Year in Review)

#### Specific Implementation Tasks
1. **Build Shared Recap Engine & Components (`CalculateRecapUseCase`)**:
   - Aggregate data for any week, month, or year (including archived habits that were active during that period, and reporting paused durations).
   - Create shared count-up number animations and soft accent-gradient card containers.
2. **Build `WeeklyRecapScreen` (Section 5.14)**:
   - Week selector (`‹ 19 Oct – 25 Oct ›` with `So far` badge on current week), **Hero** (*"This week, in review."* + ring + delta chip), **Week strip** (7 vertical fill bars with perfect-day `✓` badge and tap-for-day-sheet), **Key numbers** (2×3 grid), **Habit leaderboard** (7 mini dots per habit + *Most consistent* / *Needs love* callouts), **Best & toughest day**, **Time of day** histogram, **Streak updates**, **Goal progress** across active goals, **Insights**, and **Next week focus**.
3. **Build `MonthlyRecapScreen` (Section 5.12)**:
   - Month selector (`‹ October 2026 ›`), **Hero** (*"October, in review."*), **Key numbers** (2×3 grid), **Calendar heatmap** (intensity/segmented rings per day + tap for day sheet), **Week by week** bars, **Habit leaderboard**, **Best & toughest day**, **Time of day**, **Streak updates**, **Insights**, and **Next month focus**.
4. **Build `YearInReviewScreen` (Section 5.13)**:
   - Mode toggle (`Story | Summary`) and year chip (`2026`).
   - **Story Mode**: 12 full-screen animated pages with top progress segments, tap left/right navigation, hold-to-pause, and `✕` exit (Intro, Total check-ins + hours, Completion rate ring, Best/toughest month 12-bar chart, animated 53×7 Year heatmap, Longest streak, Most consistent & Most improved H1→H2 habit, Weekday pattern, Peak hour, Top 5 podium, Perfect days & run, Outro with *View summary* button).
   - **Summary Mode**: Single scrollable page with all key charts and rankings.

#### Dependencies & Prerequisites
- Phase 8 completed.

#### Expected Deliverables
- `WeeklyRecapScreen`, `MonthlyRecapScreen`, `YearInReviewScreen`, and `CalculateRecapUseCase`.

#### Acceptance Criteria & Tests
- **Definition of Completion**: Weekly, Monthly, and Yearly recaps compute accurate real-time metrics from local entries, respect `weekStart` and `dayRollover`, handle empty states gracefully, and support Story + Summary modes in Year in Review.

#### Technical Risks & Possible Solutions
- **Risk**: Touch gesture conflict in Year in Review Story Mode between tap-left/right, hold-to-pause, and top bar buttons.
- **Solution**: Use `pointerInput(Unit) { detectTapGestures(onPress = { pauseTimer(); tryAwaitRelease(); resumeTimer() }, onTap = { offset -> if (offset.x < size.width / 3) prevPage() else nextPage() }) }` below the top bar overlay.

---

### Phase 10: Home-Screen Widgets (Jetpack Glance) & In-App Widget Studio

#### Specific Implementation Tasks
1. **Build the 5 Jetpack Glance AppWidgets (Sections 5.16 & 13.8)**:
   - **#1 `WeekGridWidget` (Reference Design — Screenshot 11)**: Dark rounded container (`#1B1B1F` / `#F3F0F5` light, configurable opacity & corner radius), 7-column header (`Mo Tu We Th Fr Sa Su` ordered by `weekStart`, today bold white), scrollable `LazyColumn` of habit rows (`56dp` tinted icon tile, single-line title or hidden for privacy, `🔥 N` streak, and 7 `36dp` day circles mapped via `CircleStateMapper`: *Done* filled with dark `✓`, *Today pending* bright 2dp ring, *Past missed* 20% dark tint, *Skipped* centered dash, *Not scheduled / Future* 10% dim tint). Responsive compact fallback (`< 250dp` width shows icon, title, streak, and today's circle only).
   - **#2 `TodayListWidget` (`4×3`)**: Date, day-signal band, `2 of 11` caption, and scrollable checklist rows.
   - **#3 `ProgressRingWidget` (`2×2`)**: Bitmap-rendered completion ring with `%`, `3 of 8`, and `🔥 12-day run`.
   - **#4 `SingleHabitWidget` (`2×2`)**: Icon tile, name, streak, big check button, and 7-dot mini week strip.
   - **#5 `GoalProgressWidget` (`4×2`)**: Goal icon + title, progress bar, `4 of 7 steps`, and next step with manual check action.
2. **Implement Widget Interactivity, `WidgetConfigActivity` & `WidgetsScreen`**:
   - `ToggleHabitWidgetAction : ActionCallback`: Toggles today's (or past week's if `allowBackfill` is ON) entry in Room and triggers `WidgetUpdater.updateAll(context)`. Amount/Time habits deep-link straight to the app's stepper/timer sheet.
   - `WidgetConfigActivity`: Launched when pinning or reconfiguring a widget; allows choosing `All habits | Choose habits` (or single habit/goal) and overriding theme, opacity, corner radius, streak visibility, name privacy, and back-filling with a live preview.
   - In-app `WidgetsScreen` (Menu → Widgets): Horizontal pager carousel of all 5 widgets with **Add to home screen** (`requestPinGlanceAppWidget` + fallback instructions sheet), global **WIDGET STYLE** defaults card, and **ACTIVE WIDGETS** list with **Refresh now**.

#### Dependencies & Prerequisites
- Phases 6 & 8 completed.

#### Expected Deliverables
- 5 Glance widgets + receivers + `res/xml/*_widget_info.xml`, `CircleStateMapper`, `WidgetConfigActivity`, and in-app `WidgetsScreen`.

#### Acceptance Criteria & Tests
- **Unit Tests**:
  - `CircleStateMapperTest`: Exhaustively test all combinations of `(scheduled, entryStatus, isPast/isToday/isFuture, allowBackfill)` against the 6 visual circle states.
- **Definition of Completion**: Week grid widget matches Screenshot 11; tapping circles on the home screen toggles habits offline and updates the app and all widgets immediately; in-app widget previews and pinning work.

#### Technical Risks & Possible Solutions
- **Risk**: Jetpack Glance does not support arbitrary Compose `Canvas` drawing directly inside `GlanceComposable` (needed for `ProgressRingWidget` and custom tinted check circles).
- **Solution**: Use XML vector drawables with `ColorFilter.tint` for `WeekGridWidget` circles/checks (per Section 13.8), and render the `ProgressRingWidget` arc onto an offscreen Android `Bitmap` via standard `android.graphics.Canvas` before passing `ImageProvider(bitmap)` to Glance `Image`.

---

### Phase 11: Backup & Restore (JSON/CSV + Auto-Backup) & Onboarding / Starter Templates

#### Specific Implementation Tasks
1. **Build Backup & Restore Engine & Screen (Sections 5.17 & 13.9)**:
   - Define `@Serializable` `BackupFile` (`format = "habit-radar-backup"`, `schemaVersion = 1`, `exportedAt`, `checksum` SHA-256 over the serialized `data` payload, and `BackupData` containing habits, pauses, entries, reminders, goals, steps, and optional settings).
   - **Export**: SAF `ACTION_CREATE_DOCUMENT` for `.json` backup and `.zip` containing `habits.csv` + `entries.csv`.
   - **Restore**: SAF `ACTION_OPEN_DOCUMENT` with SHA-256 checksum & schema version validation, preview sheet (`12 habits · 4,310 check-ins · 3 goals`), **Replace everything** (which saves an internal `pre_restore_snapshot` undoable for 24 hours inside a single Room `withTransaction` block) vs **Merge** (conflict resolution: latest `completedAt` wins), followed by rescheduling all alarms and refreshing all widgets.
   - **Automatic Weekly Backup**: SAF `ACTION_OPEN_DOCUMENT_TREE` with `takePersistableUriPermission`, weekly `BackupWorker` keeping the last `3 / 5 / 10` files, and Android Auto Backup XML rules (`backup_rules.xml` & `data_extraction_rules.xml`).
2. **Build Onboarding & Starter Templates Gallery (Section 5.19)**:
   - Create localized `assets/templates_en.json`, `templates_ar.json`, and `templates_fr.json` covering all 7 categories (*Health, Mind, Sleep, Money, Study, Faith & reflection, Home* — 21 templates total with icons, colors, track types, targets, schedules, groups, and suggested reminder times).
   - Build 5-page `OnboardingScreen` (*Small marks, every day.* → *Pick a few to start.* → *Gentle reminders.* → *Make it yours.* → *All set.*), one-time coachmark overlay on Today, and `TemplateGallerySheet` accessible from Onboarding, New Habit (*Browse ideas*), and Today's empty state.

#### Dependencies & Prerequisites
- Phases 8 & 10 completed.

#### Expected Deliverables
- `BackupManager`, `BackupWorker`, `BackupRestoreScreen`, `assets/templates_*.json`, `OnboardingScreen`, and `TemplateGallerySheet`.

#### Acceptance Criteria & Tests
- **Unit & Robolectric Tests**:
  - `BackupRoundTripTest`: Export database state to JSON → verify SHA-256 checksum → mutate DB → restore with Replace and Merge → assert exact entity equivalence and rejection of tampered checksums.
- **Definition of Completion**: JSON and CSV exports succeed without storage permissions; Replace (with 24h Undo snapshot) and Merge restores work atomically; onboarding runs on first launch and replays from Settings → Welcome tour.

#### Technical Risks & Possible Solutions
- **Risk**: Restoring thousands of check-in entries could block the main thread or trigger intermediate Flow emissions mid-restore.
- **Solution**: Stream JSON parsing on `Dispatchers.IO` and execute all DAO deletions and bulk insertions inside a single `database.withTransaction { ... }` block so Room emits only once after commit.

---

### Phase 12: Localization (EN/AR/FR), Hijri Calendar, Accessibility, Testing & Release Polish

#### Specific Implementation Tasks
1. **Complete Localization & RTL Support**:
   - Provide full string resources in `res/values/strings.xml` (English), `res/values-ar/strings.xml` (Arabic with full RTL layout mirroring and RTL chart orientation), and `res/values-fr/strings.xml` (French).
   - Wire `AppCompatDelegate.setApplicationLocales` in Settings → Change Language and Onboarding Page 4.
2. **Accessibility & UI TestTags Audit**:
   - Ensure all interactive controls have `48dp` minimum touch targets, descriptive `contentDescription`s (e.g., `"Walk, Saturday, done"`), and unique `Modifier.testTag(...)` identifiers across all primary actions.
   - Verify that color is never the sole indicator of state (filled checkmarks for done, dashed rings for skipped, hollow rings for pending, pause icons for paused).
3. **Comprehensive Verification & Release Configuration**:
   - Execute full unit and Robolectric test suite (`gradle :app:testDebugUnitTest`) covering every item in the Section 11 Acceptance Checklist.
   - Verify ProGuard / R8 rules (`proguard-rules.pro`) keep `kotlinx.serialization` backup models and Room entities intact, and run `compile_applet`.

#### Dependencies & Prerequisites
- Phases 1–11 completed.

#### Expected Deliverables
- Localized `strings.xml` (EN, AR, FR), complete Robolectric CUJ test suite, and verified build via `compile_applet`.

#### Acceptance Criteria & Tests
- **End-to-End Robolectric CUJ Tests**:
  - Onboarding flow → creating template habits → checking habits on Today → verifying streak & goal step auto-completion → verifying Menu 7 rows → exporting and restoring backup.
- **Definition of Completion**: All 26 items in the Specification Section 11 Acceptance Checklist pass verification; `gradle :app:testDebugUnitTest` and `compile_applet` succeed with zero errors.
