# GitHub Release APK Workflow & Comprehensive Project README

This plan sets up an automated GitHub Actions release pipeline that builds and attaches both signed **Release** and **Debug** `.apk` files to GitHub Releases, paired with a comprehensive, production-grade `README.md` documenting every feature, architectural layer, and step-by-step APK release workflow for **Habit Radar**.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions reflect your confirmed choices and ensure that publishing `.apk` files on GitHub works both automatically via CI/CD and manually via the GitHub web interface or AI Studio export.

- **Confirmed Decision 1 — Both Automated GitHub Actions & Manual Release Guide**: We will create a ready-to-use GitHub Actions workflow for automated `.apk` releases on version tags (`v*`) or manual dispatch, alongside a clear step-by-step manual upload guide in `README.md`.
- **Confirmed Decision 2 — Both Signed Release APK & Debug APK**: Each release will generate and attach both `HabitRadar-release.apk` (signed) and `HabitRadar-debug.apk`, plus a `SHA256SUMS.txt` checksum file.
- **Confirmed Decision 3 — Zero-Config Keystore Fallback in CI**: If custom signing secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) are configured in GitHub repository secrets, the workflow uses them; if not yet configured, it automatically generates a valid signing key so your first GitHub Release succeeds immediately without failing on missing secrets.
- **Confirmed Decision 4 — Comprehensive README Scope**: `README.md` will be expanded with a full feature breakdown, architecture & Room v3 data flow diagrams, and a complete APK download and GitHub Release publishing guide.

---

### 1. Overview & Core Concept

- **What It Does**:
  1. Adds a GitHub Actions workflow that compiles the Android project on Ubuntu runners with JDK 17, builds both the signed Release APK and Debug APK, renames them with the release version tag, generates SHA-256 checksums, and attaches all artifacts directly to a GitHub Release.
  2. Transforms `README.md` into a detailed, well-structured GitHub documentation hub covering every implemented feature of Habit Radar, its MVVM + Room v3 architecture, and step-by-step instructions for creating GitHub Releases with `.apk` downloads.
- **Target Audience / Persona**: Developers, contributors, and end users visiting the GitHub repository who want to understand the project deeply or download/publish installable Android `.apk` builds.
- **Key Value**: Eliminates manual build friction when publishing new versions on GitHub while giving visitors a clear, authoritative guide to the entire application.

---

### 2. User Experience & Documentation Structure

- **Key User Flows**:
  1. **Automated Tag Release Flow**: Push a git tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`) or click **Run workflow** in the GitHub Actions tab → GitHub Actions compiles the app and publishes a GitHub Release with `HabitRadar-v1.0.0-release.apk`, `HabitRadar-v1.0.0-debug.apk`, and `SHA256SUMS.txt` attached under **Assets**.
  2. **Manual GitHub Release Flow (via AI Studio or Local Gradle)**: Export the APK from Google AI Studio (or run `gradle assembleRelease assembleDebug`) → go to GitHub **Releases → Draft a new release** → drag-and-drop the `.apk` into the binaries box → click **Publish release**.
- **Visual Identity & Formatting in `README.md`**:
  - Clean status badges (Platform API 26+, Kotlin, Jetpack Compose, Room v3, Glance Widgets, License).
  - Scannable tables for feature matrices, Home Studio settings, milestone badge tiers, and widget types.
  - Copy-pasteable terminal snippets and clear callout alerts (`[!TIP]`, `[!NOTE]`, `[!IMPORTANT]`).

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual-Trigger GitHub Actions Workflow (`push.tags` + `workflow_dispatch`)**
  - *Chosen Approach*: Trigger automatically on any `v*.*.*` tag push **and** allow one-click manual execution from the GitHub Actions UI by entering a tag name (e.g., `v1.0.0`).
  - *Why*: Lets you create a release directly from the GitHub browser interface without even needing a local terminal, while still supporting standard git tag workflows.
- **Decision 2: Resilient Signing Strategy for Release & Debug APKs**
  - *Chosen Approach*: Ensure `debug.keystore` and `my-upload-key.jks` exist before Gradle runs—using repository secrets if provided or generating a fallback keystore via `keytool` when secrets are absent.
  - *Why*: Matches the existing `signingConfigs` in the project's Gradle configuration without modifying the app's build script constraints, ensuring CI builds never break out-of-the-box.

---

### 4. Technical Architecture & Release Pipeline *(Technical Reference)*

- **Release Pipeline & Repository Architecture Diagram**:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        GitHub Repository & CI Pipeline                     │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  Trigger: git push tag "v*"  OR  GitHub UI "Run workflow" (tag: v1.0.0)    │
│                                     │                                      │
│                                     ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │               GitHub Actions Runner (ubuntu-latest)                  │  │
│  │  1. Checkout repo & configure JDK 17 + Android SDK                   │  │
│  │  2. Prepare debug.keystore & release keystore (Secrets or keytool)   │  │
│  │  3. Run Gradle: assembleDebug & assembleRelease                      │  │
│  │  4. Package:                                                         │  │
│  │     • HabitRadar-<tag>-release.apk                                   │  │
│  │     • HabitRadar-<tag>-debug.apk                                     │  │
│  │     • SHA256SUMS.txt                                                 │  │
│  └──────────────────────────────────┬───────────────────────────────────┘  │
│                                     │                                      │
│                                     ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │             GitHub Releases Page (softprops/action-gh-release)       │  │
│  │  • Auto-generated release notes + attached APK download assets       │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

- **Habit Radar Application Architecture Documented in `README.md`**:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   UI Layer (Jetpack Compose + Material 3)                  │
│  • Main Tabs: Today (Classic/One-Thing/Ledger), Weekly, Monthly, Overall   │
│  • Analytics: Recharts Bar Chart Studio, Category & Tag Filtering          │
│  • Motivation: Milestone Badges Gallery, Unlock Celebration Modal          │
│  • Customization: Home Studio (Theme, 21 Accents, Card Visibility)         │
│  • Glance AppWidgets: Week Grid, Today List, Progress Ring, Single, Goal   │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ Unidirectional StateFlow / Events
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
│  • Room Database (v3, MIGRATION_1_2, MIGRATION_2_3):                       │
│    Habits (with Category & Tags), Entries, Reminders, Pauses, Goals, Steps │
│  • Preferences DataStore: Theme, Accent, Layout, Home Studio Toggles       │
│  • WorkManager + AlarmManager: Daily Habit Reminders & Morning/Evening     │
│  • BackupManager & Optional Firebase Cloud Sync (JSON/CSV + SHA-256)       │
└────────────────────────────────────────────────────────────────────────────┘
```
