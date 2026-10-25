# Milestone Badge & Achievement Tracking System

A motivation-focused achievement engine that evaluates real-time habit completions, streaks, and weekly consistency from the local database to award custom visual badges, surface progress shelves across the Today and Habit Detail screens, provide a dedicated Badges & Trophy Gallery, and trigger an instant celebration modal whenever a new milestone badge is unlocked.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following product and UX decisions incorporate your confirmed preferences from the clarification step alongside the core requirements from your request:

- **Confirmed Decision 1 — Achievement Categories**: Track and award visual badges across **Streak Milestones** (3, 7, 14, 30, and 100-day streaks), **Total Completions** (10, 25, 50, 100, 250, and 500 total check-ins, honoring the "100 completions total" milestone), and **Perfect Week & Consistency Achievements** (First Perfect Day, 7-Day Perfect Week, 80%+ Monthly Consistency, and Multi-Habit Rhythm).
- **Confirmed Decision 2 — Badge Placement & Surfaces**: Provide a **Dedicated Badges Gallery** (accessible from the Milestones screen and main menu), a **Today Screen Milestone Shelf** highlighting recently earned badges and the closest next unlock, and a **Per-Habit Milestone Badge Shelf** on the Habit Detail screen.
- **Confirmed Decision 3 — Unlock Celebration**: Trigger an **Instant Unlock Celebration Modal** featuring the custom badge emblem, metallic/gem tier styling, achievement title, and unlock date whenever a habit check-in crosses a milestone threshold.

---

### 1. Overview & Core Concept

- **What It Does**: Automatically evaluates user progress across all active habits and historical completion logs to award tiered visual badges. Users can inspect both unlocked achievements (with earned timestamps) and locked achievements (with live progress bars and remaining counts, such as `5 / 7 days` toward *7-Day Streak* or `84 / 100 check-ins` toward *Century Club: 100 Completions*).
- **Target Audience / Persona**: Habit Radar users who want tangible visual rewards, clear short- and long-term milestones, and celebratory feedback as they build daily routines.
- **Key Value**: Bridges the gap between single daily check-ins and long-term habit mastery by turning raw streak and completion counts into collectible visual emblems and immediate positive reinforcement.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Daily Check-In & Instant Unlock Modal**: When a user marks a habit as done on the Today screen, Weekly tab, or Habit Detail screen, the milestone evaluator compares their previous and updated achievement states. If a new badge threshold is reached (e.g., *7-Day Streak* or *100 Completions Total*), an animated **Badge Unlock Celebration Modal** appears with the custom-drawn badge crest, tier label, and motivational copy.
  2. **Today Screen Milestone Progress Shelf**: Below the weekly performance summary on the main Today screen, a compact interactive **Milestone Badge Shelf** displays the user's unlocked badge count, miniature emblems of recently earned badges, and a progress pill for the next closest milestone. Tapping the shelf opens the full Badges Gallery.
  3. **Habit Detail Badge Shelf**: Inside each individual habit's detail view, a **Habit Milestones Card** shows streak and completion badges earned specifically by that habit (3d, 7d, 14d, 30d, 100d streaks; 10, 50, 100 completions) along with a progress bar to the next tier.
  4. **Dedicated Badges & Trophy Gallery**: Accessible via a top segmented switcher inside the Milestones screen (*Badges & Achievements* vs. *Goals & Steps*) or directly from the Today shelf, users can filter badges by category (*All*, *Streaks*, *Total Completions*, *Consistency*) and tap any badge to inspect its full requirements, current progress ratio, and unlock date.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: High-craft dark/light editorial luxury aligned with Habit Radar's calm atmospheric aesthetic—custom geometric vector badge emblems (`Canvas` shields, stars, laurels, and flame crests) paired with soft radial glows rather than generic flat icons.
  - *Color Palette & Mood*:
    - **Bronze Tier** (Starter milestones: 3-Day Streak, 10 Completions, First Perfect Day): Warm Amber-Bronze (`#E6A15C`) with subtle warm container glow.
    - **Silver Tier** (Committed milestones: 7-Day Streak, 25 & 50 Completions): Luminous Slate-Silver (`#B8C7E0`) with crisp border highlights.
    - **Gold Tier** (Mastery milestones: 14 & 30-Day Streaks, 100 Completions Total, Perfect Week): Radiant Champagne-Gold (`#F5C96B`) with layered radial aura.
    - **Prismatic / Amethyst Tier** (Elite milestones: 100-Day Streak, 250 & 500 Completions, 4 Perfect Weeks): Signature Lavender-Amethyst (`#E8A8F5` / `#90CAF9`) gradient.
  - *Typography & Hierarchy*:
    - Display titles and unlock headlines rendered in **Instrument Serif** (`32sp`–`38sp`).
    - Badge titles, progress counters (`84 / 100`), and category pills rendered in **Plus Jakarta Sans** (`12sp`–`16sp`, `SemiBold`).
  - *Interactive Feedback & Motion*:
    - Spring-animated scale and glow reveal (`spring(dampingRatio = 0.7f)`) on the custom `Canvas` badge crest inside the unlock celebration modal.
    - Smooth progress bar fills on locked badges showing exact percentage toward unlock.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Deterministic Calculation + Newly-Unlocked Diff Detection**
  - *Chosen Approach*: Evaluate badge progress deterministically from the Room database (`habits`, `entries`, and `habit_pauses`) while tracking unlocked badge IDs in app state/preferences so the app accurately detects *newly unlocked* badges the moment a check-in occurs without firing duplicate celebrations.
  - *Why*: Guarantees that backfilled completions, imported backups, and offline check-ins immediately reflect accurate badge progress and earned dates while ensuring the celebration modal only triggers once per newly earned achievement.
  - *Alternatives Considered*: Maintaining a separate mutable counter table that only increments on button clicks—rejected because editing or backfilling date-stamped logs could desynchronize badge counts from actual Room completion records.

- **Decision 2: Unified Global + Per-Habit Milestone Evaluation**
  - *Chosen Approach*: Support both account-wide achievements (e.g., *100 Completions Total*, *7-Day Perfect Week*, *7-Day Streak on Any Habit*) and habit-specific milestone badges displayed on the Habit Detail screen.
  - *Why*: Users with multiple habits reach *100 Completions Total* across their routine while also wanting to see which specific habit earned a *7-Day* or *30-Day Streak* badge.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                           Room Persistence Layer                           │
│   ┌──────────────┐       ┌────────────────────┐       ┌────────────────┐   │
│   │  habits DAO  │       │    entries DAO     │       │habit_pauses DAO│   │
│   └──────┬───────┘       └─────────┬──────────┘       └───────┬────────┘   │
└──────────┼─────────────────────────┼──────────────────────────┼────────────┘
           └─────────────────────────┼──────────────────────────┘
                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   HabitRadarRepository & MilestoneEngine                   │
│  • Evaluates Streak Milestones (3, 7, 14, 30, 100 days)                    │
│  • Evaluates Total Completion Milestones (10, 25, 50, 100, 250, 500)       │
│  • Evaluates Consistency & Perfect Week Badges                             │
│  • Detects newly unlocked badges on check-in and emits celebration event   │
└───────────────┬───────────────────────────────┬────────────────────────────┘
                │                               │
                ▼                               ▼
┌───────────────────────────────┐ ┌──────────────────────────────────────────┐
│        MainAppViewModel       │ │        UI Surfaces & Celebration         │
│  • milestoneSummaryFlow       │ │  • TodayMilestoneShelfCard (Today Tab)   │
│  • newlyUnlockedBadgeState    │ │  • HabitMilestoneBadgeShelf (Habit View) │
│  • dismissBadgeCelebration()  │ │  • BadgesGalleryView (Milestones Screen) │
└───────────────────────────────┘ │  • BadgeUnlockCelebrationModal (Overlay) │
                                  └──────────────────────────────────────────┘
```

- **Data Model & State**:
  - `MilestoneCategory`: Enum (`STREAK`, `TOTAL_COMPLETIONS`, `CONSISTENCY`).
  - `BadgeTier`: Enum (`BRONZE`, `SILVER`, `GOLD`, `PRISMATIC`) governing metallic/gem color palettes and custom `Canvas` emblem rings.
  - `MilestoneBadge`: Domain model containing `id`, `title`, `description`, `category`, `tier`, `targetValue`, `currentValue`, `progressFraction` (`0f..1f`), `isUnlocked`, `unlockedDate`, and `iconType`.
  - `MilestoneSummary`: Aggregated state containing `allBadges`, `unlockedBadges`, `nextUpBadge`, `unlockedCount`, `totalCount`, and `newlyUnlockedBadge` for instant celebration triggers.

- **Interactive Component & State Mapping**:
  - **Check-In Trigger -> Unlock Detection**: When `toggleTaskEntry`, `setQuantitativeEntry`, or `logHabitCompletion` completes on the Today or Habit Detail screen, `MilestoneEngine` compares the unlocked badge set before and after the entry mutation; if one or more badges transition from locked to unlocked, the highest-tier newly unlocked `MilestoneBadge` is pushed to `newlyUnlockedBadge` state to display `BadgeUnlockCelebrationModal`.
  - **Today Screen Shelf Interaction**: Displays earned badge count, top 4 badge emblems, and next-milestone progress bar; tapping opens the Badges Gallery screen (`ScreenRoute.MilestonesList` focused on the Badges tab).
  - **Badges Gallery Filtering & Inspection**: Category filter chips (*All*, *Streaks*, *Completions*, *Consistency*) filter the 2-column badge grid in real time; tapping any badge opens a detail bottom sheet showing the large custom `Canvas` emblem, exact progress count (`currentValue / targetValue`), and earned date or remaining target tip.
