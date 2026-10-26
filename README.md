# Habit Radar

**A calm, private, offline-first habit tracker for Android.** *Small marks, every day.*

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Database](https://img.shields.io/badge/Database-Room%20SQLite%20v3%20%2B%20DataStore-FF6F00)
![Widgets](https://img.shields.io/badge/Widgets-Jetpack%20Glance-9C27B0)

**Habit Radar** is a native Android habit tracker crafted to help you build sustainable routines, understand your long-term consistency, and make steady progress toward meaningful goals. It pairs a quiet, editorial design system (**Instrument Serif** + **Plus Jakarta Sans**, adaptive **Dark** & **Light** palettes, and 21 custom accent swatches) with deep analytics, category & multi-tag organization, milestone achievement badges, gentle WorkManager/AlarmManager reminders, recaps, and interactive Jetpack Glance home-screen widgets.

---

## Table of Contents

1. [At a Glance](#1-at-a-glance)
2. [How to Add & Publish `.apk` Files in GitHub Releases](#2-how-to-add--publish-apk-files-in-github-releases)
   - [Method A: Automated GitHub Actions Release (1-Click or Git Tag)](#method-a-automated-github-actions-release-1-click-or-git-tag)
   - [Method B: Manual APK Upload via GitHub Web UI](#method-b-manual-apk-upload-via-github-web-ui)
   - [Optional: Custom Release Signing with GitHub Secrets](#optional-custom-release-signing-with-github-secrets)
3. [Complete Feature Guide](#3-complete-feature-guide)
   - [3.1 Multi-Horizon Habit Tracking (Today, Weekly, Monthly, Overall)](#31-multi-horizon-habit-tracking-today-weekly-monthly-overall)
   - [3.2 Category & Multi-Tag Organization System](#32-category--multi-tag-organization-system)
   - [3.3 Weekly Habit Performance Summary Card](#33-weekly-habit-performance-summary-card)
   - [3.4 Milestone Tracking & Visual Achievement Badges](#34-milestone-tracking--visual-achievement-badges)
   - [3.5 Data Visualization Studio (Recharts Bar Charts)](#35-data-visualization-studio-recharts-bar-charts)
   - [3.6 Home Studio & Calm Theme Customization (Light / Dark / System)](#36-home-studio--calm-theme-customization-light--dark--system)
   - [3.7 Smart Reminders, Daily Nudges & Quiet Hours (WorkManager)](#37-smart-reminders-daily-nudges--quiet-hours-workmanager)
   - [3.8 Goals & Linked Habit Milestones](#38-goals--linked-habit-milestones)
   - [3.9 Weekly, Monthly & Year-in-Review Recaps](#39-weekly-monthly--year-in-review-recaps)
   - [3.10 Interactive Home-Screen Widgets (Jetpack Glance)](#310-interactive-home-screen-widgets-jetpack-glance)
   - [3.11 Backup, Restore, CSV Export & Optional Cloud Sync](#311-backup-restore-csv-export--optional-cloud-sync)
4. [System Architecture & Room Database Schema](#4-system-architecture--room-database-schema)
5. [Local Build, Verification & Testing](#5-local-build-verification--testing)

---

## 1. At a Glance

| Aspect | Details |
| :--- | :--- |
| **Platform** | Android 8.0 (API 26) through Android 16 (API 36) |
| **Language** | 100% Kotlin with Coroutines & `StateFlow` |
| **UI Framework** | Jetpack Compose + Material Design 3 + Custom `HabitRadarTheme` |
| **Typography** | Bundled local Google Fonts: **Instrument Serif** (display headlines) & **Plus Jakarta Sans** (UI/body) |
| **Architecture** | Offline-first MVVM with unidirectional data flow and constructor-injected `AppContainer` |
| **Local Persistence** | Room Database (`HabitRadarDatabase` v3 with `MIGRATION_1_2` & `MIGRATION_2_3`) + Preferences DataStore |
| **Background Scheduling** | `WorkManager` (`HabitReminderWorker`) + `AlarmManager` (`ReminderReceiver` / `DailyNudgeReceiver`) |
| **Home-Screen Widgets** | 5 interactive `GlanceAppWidget` receivers with direct Room check-in toggles |
| **Localization** | English, Arabic (RTL), and French + optional Hijri calendar header |

---

## 2. How to Add & Publish `.apk` Files in GitHub Releases

This repository includes both an **automated GitHub Actions workflow** (`.github/workflows/release-apk.yml`) that builds and attaches signed Release and Debug APKs automatically, and support for **manual APK uploads**.

### Method A: Automated GitHub Actions Release (1-Click or Git Tag)

The workflow in `.github/workflows/release-apk.yml` automatically builds:
- `HabitRadar-<version>-release.apk` (Signed Release APK)
- `HabitRadar-<version>-debug.apk` (Debug APK)
- `SHA256SUMS.txt` (SHA-256 integrity checksums)

You can trigger it in **two ways**:

#### Option 1: Trigger Directly from the GitHub Browser UI (No Terminal Required)
1. Push this repository to GitHub.
2. Open your repository on GitHub and click the **Actions** tab at the top.
3. In the left sidebar, select **Build & Publish Release APKs**.
4. Click the **Run workflow** dropdown button on the right, enter your version tag (for example, `v1.0.0`), and click **Run workflow**.
5. Once the workflow finishes (~2–3 minutes), go to your repository's **Releases** section on the right sidebar — your new release `Habit Radar v1.0.0` will be live with both `.apk` files attached under **Assets**.

#### Option 2: Trigger by Pushing a Git Version Tag
Run the following commands in your terminal whenever you want to publish a new version:

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will automatically detect the `v*` tag, compile the signed Release and Debug APKs, and publish the GitHub Release with the `.apk` downloads attached.

---

### Method B: Manual APK Upload via GitHub Web UI

If you already generated an `.apk` file (either from **Google AI Studio's Export/Generate APK menu** or on your machine via Gradle) and want to attach it manually to a GitHub Release:

1. **Generate the `.apk` file**:
   - **In Google AI Studio**: Open the **Settings / Export** menu and select **Download APK**, or
   - **Using Gradle locally**:
     ```bash
     gradle :app:assembleDebug :app:assembleRelease
     ```
     Your APKs will be created at:
     - `app/build/outputs/apk/release/app-release.apk`
     - `app/build/outputs/apk/debug/app-debug.apk`
2. **Open GitHub Releases**:
   - Go to your GitHub repository page (`https://github.com/<your-username>/<your-repo>`).
   - In the right-hand sidebar under **Releases**, click **Create a new release** (or **Draft a new release**).
3. **Choose a Tag & Release Title**:
   - Click **Choose a tag**, type `v1.0.0`, and click **+ Create new tag: v1.0.0 on publish**.
   - Enter a **Release title** (e.g., `Habit Radar v1.0.0`).
   - Click **Generate release notes** (or write a summary of changes).
4. **Attach the `.apk` File**:
   - Near the bottom of the page, locate the box labeled **"Attach binaries by dropping them here or selecting them"**.
   - Drag and drop your `.apk` file (e.g., `HabitRadar-v1.0.0-release.apk` and/or `HabitRadar-v1.0.0-debug.apk`) into that box and wait for the upload bar to complete.
5. **Publish**:
   - Click the green **Publish release** button. Anyone visiting your repository can now download and install the `.apk` directly from the **Releases** page!

---

### Optional: Custom Release Signing with GitHub Secrets

> [!TIP]
> **Zero-Config Fallback Included**: If you do not configure any GitHub Secrets, `.github/workflows/release-apk.yml` automatically generates a valid signing key during the build so your release workflow works immediately out of the box.

If you want the GitHub Actions workflow to sign `HabitRadar-release.apk` with your own persistent keystore so users can upgrade across future releases without uninstalling:

1. Generate an upload keystore locally (if you don't already have one):
   ```bash
   keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Base64-encode the keystore file:
   ```bash
   base64 -w 0 my-upload-key.jks > keystore.b64
   ```
3. In your GitHub repository, navigate to **Settings → Secrets and variables → Actions → New repository secret** and add:
   - `KEYSTORE_BASE64`: Contents of `keystore.b64`
   - `STORE_PASSWORD`: Your keystore password
   - `KEY_PASSWORD`: Your key password (under alias `upload`)

---

## 3. Complete Feature Guide

### 3.1 Multi-Horizon Habit Tracking (Today, Weekly, Monthly, Overall)
- **Today View**:
  - Supports **Task** (single tap check-in), **Amount** (quantitative target with unit, e.g., *8 glasses*, *20 pages*), and **Time** (duration target with built-in timer & manual entry, e.g., *15 min meditation*) habits.
  - Flexible scheduling: **Daily** (specific days of the week), **Weekly** ($N$ times per week), or **Monthly** (specific calendar days of the month).
  - Rich interactions: One-tap check-in with optional soft audio tick, **Skip today** (preserves streak without counting as a miss), completion notes, and back-filling historical dates.
  - **Pause & Archive**: Temporarily pause a habit over a date range (protecting streaks and excluding paused days from completion rate denominators) or archive completed habits without losing historical check-ins.
- **Weekly View**:
  - 7-day interactive matrix grid aligned to your chosen week-start day (**Monday**, **Sunday**, or **Saturday**) with tap-to-toggle back-filling for any day of the week.
- **Monthly View**:
  - Full monthly calendar matrix showing daily completion density and per-habit monthly consistency.
- **Overall View (Clean At-a-Glance Dashboard)**:
  - **Unified Summary Hero Card**: Displays **Overall Completion %**, a sleek progress bar, **Check-ins**, **Current Streak**, **Best Streak**, and **Active Habits** in one uncluttered, scannable card at the top of the screen, plus a direct **Charts →** pill to launch the Data Visualization Studio.
  - **Streamlined Trend Card**: Single-series **Weekly** (last 8 weeks) and **Monthly** (last 6 months) bar chart with an average reference line.
  - **Compact Habit Performance List**: Single-row cards with habit icon tiles, completion percentage, category prefix, slim progress bar, and current/best streak counts.

### 3.2 Category & Multi-Tag Organization System
- **Preset & Custom Categories**:
  - Assign habits to preset life areas (**Fitness**, **Mindfulness**, **Learning**, **Health**, **Sleep**, **Productivity**, **Finance**, **Creativity**) or type any custom category in the **Category & Tags** section of the habit creation form.
- **Multi-Tagging (`#tags`)**:
  - Attach multiple custom tags (e.g., `#Cardio`, `#Morning`, `#Deep Work`, `#Breathwork`) with category-aware smart suggestions and one-tap removable `#tag` chips.
- **Cross-App Analytics Filtering**:
  - Filter habits, KPI cards, and completion rate charts by any category or `#tag` in the **Today** tab, **Overall** tab, and **Data Visualization Studio**.

### 3.3 Weekly Habit Performance Summary Card
- Located at the top of the **Today** screen (and toggleable in **Home Studio**):
  - Calculates real-time weekly completion percentage (`completed / scheduled`) from the Room database.
  - Displays week-over-week delta badge (e.g., `+14% vs last wk`), a 7-day mini spark bar strip from week start through week end, and a motivational status message.

### 3.4 Milestone Tracking & Visual Achievement Badges
- **14 Global Badges Across 3 Achievement Categories** (`MilestoneEngine`):
  - **Streak Milestones**: *3-Day Spark* (3d), *7-Day Streak* (7d), *Fortnight Flow* (14d), *Monthly Master* (30d), *Quarter Century* (60d), and *100-Day Legend* (100d).
  - **Total Volume Milestones**: *First Small Mark* (1), *Getting Grounded* (10), *Quarter Hundred* (25), *Half Century* (50), *Century Club* (100 completions total), and *250 Club* (250 completions).
  - **Consistency & Perfect Week Achievements**: *First Perfect Day* (100% of scheduled habits completed on a single day), *High Consistency Week* (80%+ weekly completion rate), and *Perfect Week* (100% completion across all 7 days of the week).
- **Per-Habit Milestone Shelves & Instant Celebration Modal**:
  - Displays unlocked badges and next-milestone progress on the **Today** screen, **Habit Detail** screen, and the dedicated **Milestones & Badges** gallery.
  - Triggers an animated **Milestone Unlocked Celebration Modal** the moment a new badge is earned.

### 3.5 Data Visualization Studio (Recharts Bar Charts)
- Dedicated analytics screen (`DataVisualizationScreen`) featuring a custom Compose Canvas **RechartsBarChart**:
  - **Timeframes**: **Weekly (Daily Mon–Sun)**, **Weekly (8-Week Trend)**, and **Monthly (6-Month Trend)**.
  - **Interactive Features**: Tap-to-inspect tooltips with exact `completed / scheduled` ratios, optional previous-period comparison bars, dashed average reference line, **Category & Tag Filter** chip bar, and a **Performance by Category & Tag** comparative breakdown card.

### 3.6 Home Studio & Calm Theme Customization (Light / Dark / System)
- **Main Menu Calm Theme Toggle**:
  - Switch between **Dark Mode** (*Restful obsidian contrast for calm focus*), **Light Mode** (*Soft warm paper tones for daylight clarity*), and **System** directly from the **Appearance** card at the top of the Main Menu or in **Settings**.
- **Home Studio (`Settings → Home Studio`)**:
  - Interactive live phone preview at the top of the screen that updates in real time as you customize:
    - **Theme & 21 Free Accent Color Swatches** (Orchid, Mint-Teal, Periwinkle, Sage, Amber, Coral, etc.).
    - **App Bar Controls**: Toggle greeting, day-signal band, and headline mode (*Count*, *Percent*, or *Days*).
    - **3 Today Layouts**: **Classic** (rich cards), **One Thing** (focused single-habit spotlight), and **Ledger** (minimalist checklist).
    - **Card Density & Visibility Toggles**: Switch between *Comfortable* and *Compact* density, hide completed habits, group habits into category sections, and **show or hide the Weekly Habit Performance card and Milestone Badges shelf** on the Home screen.

### 3.7 Smart Reminders, Daily Nudges & Quiet Hours (WorkManager)
- **WorkManager Daily Habit Reminders (`HabitReminderWorker`)**:
  - Schedule specific daily times and days of the week for each habit, with automatic calculation of initial delay, smart skip-if-already-done checks against Room, and self-rescheduling for the next day.
- **Exact Alarm & Notification Quick Actions**:
  - Notification action buttons for **Done**, **Snooze**, and **Skip** directly from the Android notification shade without opening the app.
- **Morning Kickoff, Evening Save & Quiet Hours**:
  - Configurable morning and evening daily nudges plus **Quiet Hours** (*Mute* or *Delay* modes).

### 3.8 Goals & Linked Habit Milestones
- Create long-term **Goals** with target dates and break them down into actionable **Steps**:
  - **Manual Steps**, **Quantitative Steps**, and **Habit-Linked Steps** (automatically evaluated by `GoalStepEvaluator` based on a linked habit's streak or completion count).

### 3.9 Weekly, Monthly & Year-in-Review Recaps
- **Weekly & Monthly Recaps**: Summarize completion rates, best habits, most consistent weekdays, and streak highlights.
- **Year in Review**: Interactive multi-slide **Story Mode** and **Summary Mode** celebrating your annual check-ins, top habits, and monthly rhythm.

### 3.10 Interactive Home-Screen Widgets (Jetpack Glance)
Five configurable home-screen widgets built with `androidx.glance.appwidget`:
1. **Week Grid Widget** (`WeekGridWidgetReceiver`)
2. **Today Checklist Widget** (`TodayListWidgetReceiver`)
3. **Progress Ring Widget** (`ProgressRingWidgetReceiver`)
4. **Single Habit Streak Widget** (`SingleHabitWidgetReceiver`)
5. **Goal Progress Widget** (`GoalProgressWidgetReceiver`)

All habit widgets support direct tap-to-complete / toggle actions that update the Room database and refresh widget state immediately.

### 3.11 Backup, Restore, CSV Export & Optional Cloud Sync
- **Full JSON Backup & Restore (`BackupManager`)**:
  - Exports all habits (including categories & tags), entries, reminders, pauses, goals, steps, and settings into a versioned JSON envelope with a **SHA-256 checksum**.
  - Supports **Replace** and **Merge** restore modes plus optional automatic folder backups.
- **CSV Export**: Export habits and check-in logs as standard `.csv` files for external spreadsheet analysis.
- **Offline-First with Optional Google Sign-In Cloud Sync**:
  - Works 100% offline in guest mode with zero sign-in gates; users can optionally sign in with Google via Jetpack `CredentialManager` to sync backups to Firebase Firestore.

---

## 4. System Architecture & Room Database Schema

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   UI Layer (Jetpack Compose + Material 3)                  │
│  • Main Tabs: Today (Classic/One-Thing/Ledger), Weekly, Monthly, Overall   │
│  • Analytics: Recharts Bar Chart Studio, Category & Tag Filtering          │
│  • Motivation: Milestone Badges Gallery, Unlock Celebration Modal          │
│  • Customization: Main Menu Theme Toggle + Home Studio Live Preview        │
│  • Glance AppWidgets: Week Grid, Today List, Progress Ring, Single, Goal   │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ StateFlow / Unidirectional Events
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     Domain & Use-Case Calculators                          │
│  • ScheduleMatcher · StreakCalculator · LogicalDateCalculator              │
│  • WeeklyPerformanceCalculator · CompletionRateChartCalculator             │
│  • MilestoneEngine (14 Global + Per-Habit Badges) · GoalStepEvaluator      │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ Coroutines / Flow
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                Offline-First Persistence & Background Layer                │
│  • Room Database (HabitRadarDatabase v3, MIGRATION_1_2, MIGRATION_2_3):    │
│    - habits (indexed on category, stores tags list)                        │
│    - entries (unique index on [habitId, date], CASCADE on habit delete)    │
│    - reminders & habit_pauses (CASCADE on habit delete)                    │
│    - goals & steps (SET NULL on linkedHabitId if habit is deleted)         │
│  • Preferences DataStore (SettingsDataStore):                              │
│    - ThemeMode, Accent, Layout, Home Studio Card Visibility, Rollover      │
│  • WorkManager (HabitReminderWorker) + AlarmManager BroadcastReceivers     │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Local Build, Verification & Testing

### Prerequisites
- **JDK 17** or newer
- **Android SDK** (API 36 compileSdk, API 26 minSdk)
- **Gradle 8.11+**

### Build Commands

```bash
# Compile and assemble the debug APK
gradle :app:assembleDebug

# Compile and assemble the signed release APK
gradle :app:assembleRelease
```

### Running Unit & Robolectric Tests
The project includes a comprehensive suite of local JVM unit tests (`ExampleUnitTest`) and Robolectric integration tests (`ExampleRobolectricTest`) covering Room DAO cascades, `WorkManager` reminder scheduling, weekly performance calculations, milestone badge unlocking, category/tag analytics filtering, Glance widget receivers, and `SettingsDataStore` persistence:

```bash
gradle :app:testDebugUnitTest --tests "com.example.ExampleRobolectricTest" --tests "com.example.ExampleUnitTest"
```
