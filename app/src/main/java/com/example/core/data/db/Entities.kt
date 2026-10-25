package com.example.core.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.core.domain.model.ComparisonType
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Goal
import com.example.core.domain.model.GoalStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.HabitStepMetric
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.ReminderSound
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.Step
import com.example.core.domain.model.StepType
import com.example.core.domain.model.TrackType
import com.example.core.domain.model.WidgetConfig
import com.example.core.domain.model.WidgetCornerStyle
import com.example.core.domain.model.WidgetThemeMode
import com.example.core.domain.model.WidgetType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
  tableName = "habits",
  indices = [
    Index(value = ["status", "sortOrder"]),
    Index(value = ["groupName"]),
    Index(value = ["category"])
  ]
)
data class HabitEntity(
  @PrimaryKey val id: String,
  val name: String,
  val description: String?,
  val iconKey: String,
  val colorKey: String,
  val trackType: TrackType,
  val targetValue: Double?,
  val unit: String?,
  val comparison: ComparisonType,
  val repeatType: RepeatType,
  val daysOfWeek: List<Int>,
  val timesPerPeriod: Int?,
  val daysOfMonth: List<Int>,
  val groupName: String?,
  val category: String? = null,
  val tags: List<String> = emptyList(),
  val goalStreak: Int?,
  val askNote: Boolean,
  val status: HabitStatus,
  val archivedAt: Instant?,
  val startDate: LocalDate,
  val endDate: LocalDate?,
  val sortOrder: Int,
  val createdAt: Instant
)

@Entity(
  tableName = "reminders",
  foreignKeys = [
    ForeignKey(
      entity = HabitEntity::class,
      parentColumns = ["id"],
      childColumns = ["habitId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["habitId"]),
    Index(value = ["enabled"])
  ]
)
data class ReminderEntity(
  @PrimaryKey val id: String,
  val habitId: String,
  val time: LocalTime,
  val daysOfWeek: List<Int>,
  val message: String?,
  val skipIfDone: Boolean,
  val repeatAfterMin: Int?,
  val sound: ReminderSound,
  val vibrate: Boolean,
  val quickActions: Boolean,
  val enabled: Boolean
)

/**
 * Room Entity representing a daily habit completion log (`entries` table in Section 6).
 * Enforces a unique composite index on `(habitId, date)` and cascades on parent habit deletion.
 */
@Entity(
  tableName = "entries",
  foreignKeys = [
    ForeignKey(
      entity = HabitEntity::class,
      parentColumns = ["id"],
      childColumns = ["habitId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["habitId", "date"], unique = true),
    Index(value = ["date"])
  ]
)
data class EntryEntity(
  @PrimaryKey val id: String,
  val habitId: String,
  val date: LocalDate,
  val status: EntryStatus,
  val value: Double?,
  val note: String?,
  val completedAt: Instant?
)

typealias HabitCompletionLogEntity = EntryEntity

@Entity(
  tableName = "habit_pauses",
  foreignKeys = [
    ForeignKey(
      entity = HabitEntity::class,
      parentColumns = ["id"],
      childColumns = ["habitId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["habitId"]),
    Index(value = ["habitId", "startDate", "endDate"])
  ]
)
data class HabitPauseEntity(
  @PrimaryKey val id: String,
  val habitId: String,
  val startDate: LocalDate,
  val endDate: LocalDate?
)

@Entity(
  tableName = "goals",
  indices = [
    Index(value = ["status", "sortOrder"])
  ]
)
data class GoalEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String?,
  val iconKey: String,
  val colorKey: String,
  val startDate: LocalDate,
  val targetDate: LocalDate?,
  val status: GoalStatus,
  val completedAt: Instant?,
  val sortOrder: Int,
  val remindWeekly: Boolean,
  val weeklyDay: Int,
  val weeklyTime: LocalTime,
  val remindBeforeDays: Int?,
  val beforeTime: LocalTime,
  val remindStepDue: Boolean
)

@Entity(
  tableName = "steps",
  foreignKeys = [
    ForeignKey(
      entity = GoalEntity::class,
      parentColumns = ["id"],
      childColumns = ["goalId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = HabitEntity::class,
      parentColumns = ["id"],
      childColumns = ["linkedHabitId"],
      onDelete = ForeignKey.SET_NULL
    )
  ],
  indices = [
    Index(value = ["goalId", "sortOrder"]),
    Index(value = ["linkedHabitId"])
  ]
)
data class StepEntity(
  @PrimaryKey val id: String,
  val goalId: String,
  val title: String,
  val note: String?,
  val dueDate: LocalDate?,
  val sortOrder: Int,
  val type: StepType,
  val linkedHabitId: String?,
  val habitMetric: HabitStepMetric?,
  val habitTarget: Int?,
  val habitWindowDays: Int?,
  val amountTarget: Double?,
  val amountCurrent: Double?,
  val amountUnit: String?,
  val done: Boolean,
  val doneAt: Instant?
)

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
  @PrimaryKey val appWidgetId: Int,
  val type: WidgetType,
  val habitIds: List<String>,
  val goalId: String?,
  val theme: WidgetThemeMode,
  val opacity: Int,
  val cornerStyle: WidgetCornerStyle,
  val showStreak: Boolean,
  val hideNames: Boolean,
  val allowBackfill: Boolean
)

/**
 * Relational projection joining a [HabitEntity] with its completion logs ([EntryEntity]),
 * scheduled [ReminderEntity] items, and [HabitPauseEntity] intervals.
 */
data class HabitWithLogsAndDetails(
  @Embedded val habit: HabitEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "habitId"
  )
  val entries: List<EntryEntity>,
  @Relation(
    parentColumn = "id",
    entityColumn = "habitId"
  )
  val reminders: List<ReminderEntity>,
  @Relation(
    parentColumn = "id",
    entityColumn = "habitId"
  )
  val pauses: List<HabitPauseEntity>
)

/**
 * Relational projection joining a [GoalEntity] with its ordered [StepEntity] items.
 */
data class GoalWithSteps(
  @Embedded val goal: GoalEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "goalId"
  )
  val steps: List<StepEntity>
)

// Domain <-> Room Entity Mappers
fun HabitEntity.toDomain() = Habit(
  id = id,
  name = name,
  description = description,
  iconKey = iconKey,
  colorKey = colorKey,
  trackType = trackType,
  targetValue = targetValue,
  unit = unit,
  comparison = comparison,
  repeatType = repeatType,
  daysOfWeek = daysOfWeek,
  timesPerPeriod = timesPerPeriod,
  daysOfMonth = daysOfMonth,
  group = groupName,
  category = category,
  tags = tags,
  goalStreak = goalStreak,
  askNote = askNote,
  status = status,
  archivedAt = archivedAt,
  startDate = startDate,
  endDate = endDate,
  sortOrder = sortOrder,
  createdAt = createdAt
)

fun Habit.toEntity() = HabitEntity(
  id = id,
  name = name,
  description = description,
  iconKey = iconKey,
  colorKey = colorKey,
  trackType = trackType,
  targetValue = targetValue,
  unit = unit,
  comparison = comparison,
  repeatType = repeatType,
  daysOfWeek = daysOfWeek,
  timesPerPeriod = timesPerPeriod,
  daysOfMonth = daysOfMonth,
  groupName = group,
  category = category,
  tags = tags,
  goalStreak = goalStreak,
  askNote = askNote,
  status = status,
  archivedAt = archivedAt,
  startDate = startDate,
  endDate = endDate,
  sortOrder = sortOrder,
  createdAt = createdAt
)

fun ReminderEntity.toDomain() = Reminder(
  id = id,
  habitId = habitId,
  time = time,
  daysOfWeek = daysOfWeek,
  message = message,
  skipIfDone = skipIfDone,
  repeatAfterMin = repeatAfterMin,
  sound = sound,
  vibrate = vibrate,
  quickActions = quickActions,
  enabled = enabled
)

fun Reminder.toEntity() = ReminderEntity(
  id = id,
  habitId = habitId,
  time = time,
  daysOfWeek = daysOfWeek,
  message = message,
  skipIfDone = skipIfDone,
  repeatAfterMin = repeatAfterMin,
  sound = sound,
  vibrate = vibrate,
  quickActions = quickActions,
  enabled = enabled
)

fun EntryEntity.toDomain() = Entry(
  id = id,
  habitId = habitId,
  date = date,
  status = status,
  value = value,
  note = note,
  completedAt = completedAt
)

fun Entry.toEntity() = EntryEntity(
  id = id,
  habitId = habitId,
  date = date,
  status = status,
  value = value,
  note = note,
  completedAt = completedAt
)

fun HabitPauseEntity.toDomain() = HabitPause(
  id = id,
  habitId = habitId,
  startDate = startDate,
  endDate = endDate
)

fun HabitPause.toEntity() = HabitPauseEntity(
  id = id,
  habitId = habitId,
  startDate = startDate,
  endDate = endDate
)

fun GoalEntity.toDomain() = Goal(
  id = id,
  title = title,
  description = description,
  iconKey = iconKey,
  colorKey = colorKey,
  startDate = startDate,
  targetDate = targetDate,
  status = status,
  completedAt = completedAt,
  sortOrder = sortOrder,
  remindWeekly = remindWeekly,
  weeklyDay = weeklyDay,
  weeklyTime = weeklyTime,
  remindBeforeDays = remindBeforeDays,
  beforeTime = beforeTime,
  remindStepDue = remindStepDue
)

fun Goal.toEntity() = GoalEntity(
  id = id,
  title = title,
  description = description,
  iconKey = iconKey,
  colorKey = colorKey,
  startDate = startDate,
  targetDate = targetDate,
  status = status,
  completedAt = completedAt,
  sortOrder = sortOrder,
  remindWeekly = remindWeekly,
  weeklyDay = weeklyDay,
  weeklyTime = weeklyTime,
  remindBeforeDays = remindBeforeDays,
  beforeTime = beforeTime,
  remindStepDue = remindStepDue
)

fun StepEntity.toDomain() = Step(
  id = id,
  goalId = goalId,
  title = title,
  note = note,
  dueDate = dueDate,
  sortOrder = sortOrder,
  type = if (type == StepType.HABIT && linkedHabitId == null) StepType.MANUAL else type,
  linkedHabitId = linkedHabitId,
  habitMetric = habitMetric,
  habitTarget = habitTarget,
  habitWindowDays = habitWindowDays,
  amountTarget = amountTarget,
  amountCurrent = amountCurrent,
  amountUnit = amountUnit,
  done = done,
  doneAt = doneAt
)

fun Step.toEntity() = StepEntity(
  id = id,
  goalId = goalId,
  title = title,
  note = note,
  dueDate = dueDate,
  sortOrder = sortOrder,
  type = type,
  linkedHabitId = linkedHabitId,
  habitMetric = habitMetric,
  habitTarget = habitTarget,
  habitWindowDays = habitWindowDays,
  amountTarget = amountTarget,
  amountCurrent = amountCurrent,
  amountUnit = amountUnit,
  done = done,
  doneAt = doneAt
)

fun WidgetConfigEntity.toDomain() = WidgetConfig(
  appWidgetId = appWidgetId,
  type = type,
  habitIds = habitIds,
  goalId = goalId,
  theme = theme,
  opacity = opacity,
  cornerStyle = cornerStyle,
  showStreak = showStreak,
  hideNames = hideNames,
  allowBackfill = allowBackfill
)

fun WidgetConfig.toEntity() = WidgetConfigEntity(
  appWidgetId = appWidgetId,
  type = type,
  habitIds = habitIds,
  goalId = goalId,
  theme = theme,
  opacity = opacity,
  cornerStyle = cornerStyle,
  showStreak = showStreak,
  hideNames = hideNames,
  allowBackfill = allowBackfill
)
