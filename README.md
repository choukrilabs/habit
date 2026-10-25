Habit Radar
A calm, private habit tracker for Android. Small marks, every day.

Habit Radar is a native Android habit tracker designed to help you build routines, understand your consistency, and make progress toward bigger goals. It combines a quiet, customizable interface with detailed habit analytics, gentle reminders, weekly and monthly recaps, and home-screen widgets.
Project status: In development. This repository's implementation should be checked against the full specification before treating any feature as complete.

At a glance
- Platform: Android 8.0 (API 26) and newer
- Language: Kotlin 2.x
- UI: Jetpack Compose and Material 3, with a custom design system
- Architecture: MVVM with unidirectional data flow
- Local storage: Room (SQLite) and Preferences DataStore
- Background work: Android alarms and WorkManager
- Widgets: Jetpack Glance
- Languages planned: English, Arabic (RTL), and French

Track habits your way
- Today, Weekly, Monthly, and Overall views
- Task, amount, and time-based habits
- Daily, weekly, and monthly schedules
- Custom icons, pastel habit colors, descriptions, and groups
- Streaks, skip options, notes, and back-filling previous days
- Reorderable habits and configurable home-tab order
- Pause habits temporarily or archive them without losing their history
Understand your progress
- Current and longest streaks, completion rates, and total check-ins
- Habit detail pages with trend charts, calendar heatmaps, weekday patterns, and streak history
- Consistency and momentum indicators, recovery statistics, and contextual insights
- Weekly and monthly recaps
- A yearly review with story and summary modes
Build bigger goals
- Create goals and break them into smaller steps
- Link steps to habits so progress can update automatically
- Track goal progress, due dates, and milestones
- View active and completed goals in the Milestones section
Reminders that fit your routine
- Multiple reminders per habit
- Morning and evening daily nudges
- Quiet hours, snoozing, optional sound and vibration
- Notification quick actions for Done, Snooze, and Skip
- Goal and recap reminders
- Reminder scheduling that accounts for Android notification and exact-alarm permissions
Make the app feel like yours
- Dark, light, and system themes
- Customizable accent color
- Three Today layouts: Classic, One thing, and Ledger
- Comfortable or compact card density
- Optional celebrations, greeting, day-signal band, and Hijri date
- First-run onboarding and offline starter templates
- English, Arabic, and French localization planned
Home-screen widgets
The specification includes five widget types:
- Week grid
- Today checklist
- Progress ring
- Single habit
- Goal progress
Widgets are intended to reflect changes made in the app and support quick habit check-ins.
Backup and restore
- Export a full JSON backup
- Export habit and check-in data as CSV files
- Restore by replacing data or merging a backup
- Validate backup format and checksum
- Optional automatic backups to a folder selected by the user

