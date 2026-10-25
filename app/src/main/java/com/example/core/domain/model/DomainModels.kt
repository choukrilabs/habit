package com.example.core.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable

@Serializable
enum class TrackType {
  TASK,
  AMOUNT,
  TIME
}

@Serializable
enum class ComparisonType {
  AT_LEAST,
  AT_MOST,
  EXACTLY
}

@Serializable
enum class RepeatType {
  DAILY,
  WEEKLY,
  MONTHLY
}

@Serializable
enum class HabitStatus {
  ACTIVE,
  ARCHIVED
}

@Serializable
enum class EntryStatus {
  DONE,
  SKIPPED,
  MISSED
}

@Serializable
enum class ReminderSound {
  DEFAULT,
  SOFT,
  SILENT
}

@Serializable
enum class WeekStartDay(val isoDayOfWeek: Int, val label: String) {
  SATURDAY(6, "Saturday"),
  SUNDAY(7, "Sunday"),
  MONDAY(1, "Monday")
}

@Serializable
enum class CardDensity {
  COMFORTABLE,
  COMPACT
}

@Serializable
enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK
}

@Serializable
enum class HeadlineMode {
  COUNT,
  PERCENT,
  DAYS
}

@Serializable
enum class TodayLayout {
  CLASSIC,
  ONE_THING,
  LEDGER
}

@Serializable
enum class QuietHoursMode {
  MUTE,
  DELAY
}

@Serializable
enum class GoalStatus {
  ACTIVE,
  COMPLETED
}

@Serializable
enum class StepType {
  MANUAL,
  HABIT,
  AMOUNT
}

@Serializable
enum class HabitStepMetric {
  STREAK,
  TOTAL_CHECKINS,
  RATE
}

@Serializable
enum class WidgetType(val title: String, val sizeChip: String) {
  WEEK_GRID("Week grid", "4×4"),
  TODAY_LIST("Today checklist", "4×3"),
  PROGRESS_RING("Progress ring", "2×2"),
  SINGLE_HABIT("Single habit", "2×2"),
  GOAL("Goal progress", "4×2")
}

@Serializable
enum class WidgetThemeMode {
  FOLLOW,
  DARK,
  LIGHT
}

@Serializable
enum class WidgetCornerStyle {
  ROUNDED,
  EXTRA
}

@Serializable
enum class HomeTab(val id: String, val label: String) {
  TODAY("today", "Today"),
  WEEKLY("weekly", "Weekly"),
  MONTHLY("monthly", "Monthly"),
  OVERALL("overall", "Overall")
}

data class Habit(
  val id: String,
  val name: String,
  val description: String? = null,
  val iconKey: String = "star",
  val colorKey: String = "#8FBDF0",
  val trackType: TrackType = TrackType.TASK,
  val targetValue: Double? = null,
  val unit: String? = null,
  val comparison: ComparisonType = ComparisonType.AT_LEAST,
  val repeatType: RepeatType = RepeatType.DAILY,
  val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
  val timesPerPeriod: Int? = null,
  val daysOfMonth: List<Int> = emptyList(),
  val group: String? = null,
  val goalStreak: Int? = null,
  val askNote: Boolean = false,
  val status: HabitStatus = HabitStatus.ACTIVE,
  val archivedAt: Instant? = null,
  val startDate: LocalDate = LocalDate.now(),
  val endDate: LocalDate? = null,
  val sortOrder: Int = 0,
  val createdAt: Instant = Instant.now()
)

data class Reminder(
  val id: String,
  val habitId: String,
  val time: LocalTime = LocalTime.of(8, 0),
  val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
  val message: String? = null,
  val skipIfDone: Boolean = true,
  val repeatAfterMin: Int? = null,
  val sound: ReminderSound = ReminderSound.DEFAULT,
  val vibrate: Boolean = true,
  val quickActions: Boolean = true,
  val enabled: Boolean = true
)

data class Entry(
  val id: String,
  val habitId: String,
  val date: LocalDate,
  val status: EntryStatus,
  val value: Double? = null,
  val note: String? = null,
  val completedAt: Instant? = null
)

data class HabitPause(
  val id: String,
  val habitId: String,
  val startDate: LocalDate,
  val endDate: LocalDate? = null
)

data class Goal(
  val id: String,
  val title: String,
  val description: String? = null,
  val iconKey: String = "flag",
  val colorKey: String = "#D6BEFA",
  val startDate: LocalDate = LocalDate.now(),
  val targetDate: LocalDate? = null,
  val status: GoalStatus = GoalStatus.ACTIVE,
  val completedAt: Instant? = null,
  val sortOrder: Int = 0,
  val remindWeekly: Boolean = false,
  val weeklyDay: Int = 7,
  val weeklyTime: LocalTime = LocalTime.of(18, 0),
  val remindBeforeDays: Int? = 3,
  val beforeTime: LocalTime = LocalTime.of(9, 0),
  val remindStepDue: Boolean = false
)

data class Step(
  val id: String,
  val goalId: String,
  val title: String,
  val note: String? = null,
  val dueDate: LocalDate? = null,
  val sortOrder: Int = 0,
  val type: StepType = StepType.MANUAL,
  val linkedHabitId: String? = null,
  val habitMetric: HabitStepMetric? = null,
  val habitTarget: Int? = null,
  val habitWindowDays: Int? = null,
  val amountTarget: Double? = null,
  val amountCurrent: Double? = null,
  val amountUnit: String? = null,
  val done: Boolean = false,
  val doneAt: Instant? = null
)

data class WidgetConfig(
  val appWidgetId: Int,
  val type: WidgetType = WidgetType.WEEK_GRID,
  val habitIds: List<String> = emptyList(),
  val goalId: String? = null,
  val theme: WidgetThemeMode = WidgetThemeMode.FOLLOW,
  val opacity: Int = 100,
  val cornerStyle: WidgetCornerStyle = WidgetCornerStyle.ROUNDED,
  val showStreak: Boolean = true,
  val hideNames: Boolean = false,
  val allowBackfill: Boolean = true
)

data class AppSettings(
  val weekStart: WeekStartDay = WeekStartDay.MONDAY,
  val sounds: Boolean = true,
  val dailyNudges: Boolean = true,
  val cardDensity: CardDensity = CardDensity.COMFORTABLE,
  val showStreakToday: Boolean = true,
  val showStreakWeekly: Boolean = true,
  val showStreakMonthly: Boolean = true,
  val showHijri: Boolean = false,
  val language: String = "en",
  val theme: ThemeMode = ThemeMode.DARK,
  val accentColor: String = "#E8A8F5",
  val showGreeting: Boolean = true,
  val showDaySignalBand: Boolean = true,
  val headline: HeadlineMode = HeadlineMode.COUNT,
  val todayLayout: TodayLayout = TodayLayout.CLASSIC,
  val hideCompleted: Boolean = false,
  val fullScreenCelebrations: Boolean = true,
  val showTabBar: Boolean = true,
  val tabOrder: List<HomeTab> = listOf(HomeTab.TODAY, HomeTab.WEEKLY, HomeTab.MONTHLY, HomeTab.OVERALL),
  val hiddenTabs: Set<HomeTab> = emptySet(),
  val morningNudgeEnabled: Boolean = true,
  val morningNudgeTime: LocalTime = LocalTime.of(8, 0),
  val eveningNudgeEnabled: Boolean = true,
  val eveningNudgeTime: LocalTime = LocalTime.of(21, 0),
  val quietHoursEnabled: Boolean = false,
  val quietHoursFrom: LocalTime = LocalTime.of(23, 0),
  val quietHoursTo: LocalTime = LocalTime.of(6, 0),
  val quietHoursMode: QuietHoursMode = QuietHoursMode.MUTE,
  val snoozeMin: Int = 15,
  val notifSound: ReminderSound = ReminderSound.DEFAULT,
  val notifVibrate: Boolean = true,
  val notifQuickActions: Boolean = true,
  val recapWeekly: Boolean = true,
  val recapMonthly: Boolean = true,
  val recapYearly: Boolean = true,
  val goalReminders: Boolean = true,
  val dayRollover: LocalTime = LocalTime.of(3, 0),
  val groupSections: Boolean = false,
  val groupFilter: String? = null,
  val onboardingDone: Boolean = false,
  val coachmarkShown: Boolean = false,
  val autoBackupEnabled: Boolean = false,
  val autoBackupFolderUri: String? = null,
  val autoBackupKeep: Int = 5,
  val lastBackupAt: Instant? = null,
  val backupNudge: Boolean = true,
  val defaultWidgetTheme: WidgetThemeMode = WidgetThemeMode.FOLLOW,
  val defaultWidgetOpacity: Int = 100,
  val defaultWidgetCorner: WidgetCornerStyle = WidgetCornerStyle.ROUNDED,
  val defaultWidgetShowStreak: Boolean = true,
  val defaultWidgetHideNames: Boolean = false,
  val defaultWidgetAllowBackfill: Boolean = true
)

@Serializable
data class HabitTemplate(
  val id: String,
  val category: String,
  val name: String,
  val description: String,
  val iconKey: String,
  val colorKey: String,
  val trackType: TrackType = TrackType.TASK,
  val targetValue: Double? = null,
  val unit: String? = null,
  val repeatType: RepeatType = RepeatType.DAILY,
  val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
  val timesPerPeriod: Int? = null,
  val group: String? = null,
  val suggestedReminderTime: String = "08:00"
)
