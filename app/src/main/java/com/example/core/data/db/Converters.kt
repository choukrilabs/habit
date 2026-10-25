package com.example.core.data.db

import androidx.room.TypeConverter
import com.example.core.domain.model.ComparisonType
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.GoalStatus
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.HabitStepMetric
import com.example.core.domain.model.ReminderSound
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.StepType
import com.example.core.domain.model.TrackType
import com.example.core.domain.model.WidgetCornerStyle
import com.example.core.domain.model.WidgetThemeMode
import com.example.core.domain.model.WidgetType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class Converters {
  @TypeConverter
  fun fromLocalDate(value: LocalDate?): String? = value?.toString()

  @TypeConverter
  fun toLocalDate(value: String?): LocalDate? = value?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }

  @TypeConverter
  fun fromLocalTime(value: LocalTime?): String? = value?.toString()

  @TypeConverter
  fun toLocalTime(value: String?): LocalTime? = value?.takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) }

  @TypeConverter
  fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

  @TypeConverter
  fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

  @TypeConverter
  fun fromIntList(list: List<Int>?): String = list?.joinToString(",") ?: ""

  @TypeConverter
  fun toIntList(data: String?): List<Int> =
    if (data.isNullOrBlank()) emptyList()
    else data.split(",").mapNotNull { it.trim().toIntOrNull() }

  @TypeConverter
  fun fromStringList(list: List<String>?): String = list?.joinToString(",") ?: ""

  @TypeConverter
  fun toStringList(data: String?): List<String> =
    if (data.isNullOrBlank()) emptyList()
    else data.split(",").map { it.trim() }.filter { it.isNotEmpty() }

  @TypeConverter fun fromTrackType(v: TrackType): String = v.name
  @TypeConverter fun toTrackType(v: String): TrackType = TrackType.valueOf(v)

  @TypeConverter fun fromComparisonType(v: ComparisonType): String = v.name
  @TypeConverter fun toComparisonType(v: String): ComparisonType = ComparisonType.valueOf(v)

  @TypeConverter fun fromRepeatType(v: RepeatType): String = v.name
  @TypeConverter fun toRepeatType(v: String): RepeatType = RepeatType.valueOf(v)

  @TypeConverter fun fromHabitStatus(v: HabitStatus): String = v.name
  @TypeConverter fun toHabitStatus(v: String): HabitStatus = HabitStatus.valueOf(v)

  @TypeConverter fun fromEntryStatus(v: EntryStatus): String = v.name
  @TypeConverter fun toEntryStatus(v: String): EntryStatus = EntryStatus.valueOf(v)

  @TypeConverter fun fromReminderSound(v: ReminderSound): String = v.name
  @TypeConverter fun toReminderSound(v: String): ReminderSound = ReminderSound.valueOf(v)

  @TypeConverter fun fromGoalStatus(v: GoalStatus): String = v.name
  @TypeConverter fun toGoalStatus(v: String): GoalStatus = GoalStatus.valueOf(v)

  @TypeConverter fun fromStepType(v: StepType): String = v.name
  @TypeConverter fun toStepType(v: String): StepType = StepType.valueOf(v)

  @TypeConverter fun fromHabitStepMetric(v: HabitStepMetric?): String? = v?.name
  @TypeConverter fun toHabitStepMetric(v: String?): HabitStepMetric? = v?.let { HabitStepMetric.valueOf(it) }

  @TypeConverter fun fromWidgetType(v: WidgetType): String = v.name
  @TypeConverter fun toWidgetType(v: String): WidgetType = WidgetType.valueOf(v)

  @TypeConverter fun fromWidgetThemeMode(v: WidgetThemeMode): String = v.name
  @TypeConverter fun toWidgetThemeMode(v: String): WidgetThemeMode = WidgetThemeMode.valueOf(v)

  @TypeConverter fun fromWidgetCornerStyle(v: WidgetCornerStyle): String = v.name
  @TypeConverter fun toWidgetCornerStyle(v: String): WidgetCornerStyle = WidgetCornerStyle.valueOf(v)
}
