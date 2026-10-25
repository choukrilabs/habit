package com.example.core.domain.usecase

import com.example.core.domain.model.ComparisonType
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.TrackType
import com.example.core.domain.model.WeekStartDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object LogicalDateCalculator {

  fun now(rollover: LocalTime, currentDateTime: LocalDateTime = LocalDateTime.now()): LocalDate {
    val currentTime = currentDateTime.toLocalTime()
    val rawDate = currentDateTime.toLocalDate()
    return if (rollover != LocalTime.MIDNIGHT && currentTime.isBefore(rollover)) {
      rawDate.minusDays(1)
    } else {
      rawDate
    }
  }

  fun formatHeaderDate(
    date: LocalDate,
    showHijri: Boolean,
    locale: Locale = Locale.getDefault()
  ): String {
    val gregorianFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", locale)
    val gregorian = date.format(gregorianFormatter).uppercase(locale)
    if (!showHijri) return gregorian
    return try {
      val hijriDate: HijrahDate = HijrahChronology.INSTANCE.date(date)
      val hijriFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
        .withChronology(HijrahChronology.INSTANCE)
      val hijriStr = hijriFormatter.format(hijriDate).uppercase(locale)
      "$gregorian · $hijriStr"
    } catch (e: Exception) {
      gregorian
    }
  }

  fun startOfWeek(date: LocalDate, weekStart: WeekStartDay): LocalDate {
    val targetDow = DayOfWeek.of(weekStart.isoDayOfWeek)
    return date.with(TemporalAdjusters.previousOrSame(targetDow))
  }

  fun orderedDaysOfWeek(weekStart: WeekStartDay): List<DayOfWeek> {
    val first = DayOfWeek.of(weekStart.isoDayOfWeek)
    return (0..6).map { first.plus(it.toLong()) }
  }
}

object ScheduleMatcher {

  fun isPausedOn(date: LocalDate, pauses: List<HabitPause>): Boolean {
    return pauses.any { pause ->
      !date.isBefore(pause.startDate) && (pause.endDate == null || !date.isAfter(pause.endDate))
    }
  }

  fun isScheduledOn(
    habit: Habit,
    date: LocalDate,
    pauses: List<HabitPause> = emptyList(),
    ignorePause: Boolean = false
  ): Boolean {
    if (date.isBefore(habit.startDate)) return false
    if (habit.endDate != null && date.isAfter(habit.endDate)) return false
    if (!ignorePause && isPausedOn(date, pauses)) return false

    return when (habit.repeatType) {
      RepeatType.DAILY -> {
        val dow = date.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        if (habit.daysOfWeek.isEmpty()) true else dow in habit.daysOfWeek
      }
      RepeatType.WEEKLY -> {
        // Weekly quota habits can be checked on any day of the week
        true
      }
      RepeatType.MONTHLY -> {
        if (habit.daysOfMonth.isNotEmpty()) {
          date.dayOfMonth in habit.daysOfMonth
        } else {
          true
        }
      }
    }
  }

  fun isEntryDone(habit: Habit, entry: Entry?): Boolean {
    if (entry == null) return false
    if (entry.status != EntryStatus.DONE) return false
    return when (habit.trackType) {
      TrackType.TASK -> true
      TrackType.AMOUNT, TrackType.TIME -> {
        val target = habit.targetValue ?: return true
        val actual = entry.value ?: target
        when (habit.comparison) {
          ComparisonType.AT_LEAST -> actual >= target
          ComparisonType.AT_MOST -> actual <= target
          ComparisonType.EXACTLY -> kotlin.math.abs(actual - target) < 0.001
        }
      }
    }
  }

  fun formatScheduleSummary(habit: Habit): String {
    return when (habit.repeatType) {
      RepeatType.DAILY -> {
        val days = habit.daysOfWeek.sorted()
        when {
          days.size == 7 || days.isEmpty() -> "Everyday"
          days == listOf(1, 2, 3, 4, 5) -> "Weekdays"
          days == listOf(6, 7) -> "Weekends"
          else -> days.joinToString(", ") { dowShortLabel(it) }
        }
      }
      RepeatType.WEEKLY -> {
        val times = habit.timesPerPeriod ?: 3
        "$times× per week"
      }
      RepeatType.MONTHLY -> {
        if (habit.daysOfMonth.isNotEmpty()) {
          "Days ${habit.daysOfMonth.sorted().joinToString(", ")}"
        } else {
          val times = habit.timesPerPeriod ?: 10
          "$times× per month"
        }
      }
    }
  }

  fun dowShortLabel(isoDow: Int): String = when (isoDow) {
    1 -> "Mon"
    2 -> "Tue"
    3 -> "Wed"
    4 -> "Thu"
    5 -> "Fri"
    6 -> "Sat"
    7 -> "Sun"
    else -> "Mon"
  }
}

data class StreakInterval(
  val startDate: LocalDate,
  val endDate: LocalDate,
  val length: Int,
  val isActive: Boolean
)

data class StreakSummary(
  val currentStreak: Int,
  val bestStreak: Int,
  val intervals: List<StreakInterval>,
  val longestGap: Int
)

object StreakCalculator {

  fun calculateStreaks(
    habit: Habit,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): StreakSummary {
    val entryMap = entries.associateBy { it.date }
    val start = if (habit.startDate.isAfter(today)) today else habit.startDate
    val end = if (habit.endDate != null && habit.endDate.isBefore(today)) habit.endDate else today

    if (start.isAfter(end)) {
      return StreakSummary(0, 0, emptyList(), 0)
    }

    val intervals = mutableListOf<StreakInterval>()
    var runStart: LocalDate? = null
    var runEnd: LocalDate? = null
    var runLen = 0

    var currentGap = 0
    var maxGap = 0

    var cursor = start
    while (!cursor.isAfter(end)) {
      val paused = ScheduleMatcher.isPausedOn(cursor, pauses)
      val scheduled = ScheduleMatcher.isScheduledOn(habit, cursor, pauses, ignorePause = false)
      val entry = entryMap[cursor]
      val skipped = entry?.status == EntryStatus.SKIPPED
      val done = ScheduleMatcher.isEntryDone(habit, entry)

      if (done) {
        if (runStart == null) runStart = cursor
        runEnd = cursor
        runLen++
        if (currentGap > maxGap) maxGap = currentGap
        currentGap = 0
      } else if (paused || skipped || !scheduled) {
        // Skipped, paused, or unscheduled days neither break nor count
      } else {
        // Scheduled day that was not done and not skipped/paused
        if (cursor == today) {
          // Current streak stays alive through today until the day ends
        } else {
          if (runLen > 0 && runStart != null && runEnd != null) {
            intervals.add(StreakInterval(runStart, runEnd, runLen, isActive = false))
          }
          runStart = null
          runEnd = null
          runLen = 0
          currentGap++
        }
      }
      cursor = cursor.plusDays(1)
    }

    if (currentGap > maxGap) maxGap = currentGap

    val currentStreak = runLen
    if (runLen > 0 && runStart != null && runEnd != null) {
      intervals.add(StreakInterval(runStart, runEnd, runLen, isActive = true))
    }

    val bestStreak = intervals.maxOfOrNull { it.length } ?: 0
    val sortedIntervals = intervals.sortedWith(
      compareByDescending<StreakInterval> { it.length }.thenByDescending { it.endDate }
    )

    return StreakSummary(
      currentStreak = currentStreak,
      bestStreak = bestStreak,
      intervals = sortedIntervals,
      longestGap = maxGap
    )
  }

  /**
   * Consecutive days ending today (or yesterday if today has 0 completions yet)
   * where at least 1 habit was completed.
   */
  fun calculateOverallDayRun(entries: List<Entry>, today: LocalDate): Int {
    val doneDates = entries
      .filter { it.status == EntryStatus.DONE }
      .map { it.date }
      .toSet()
    if (doneDates.isEmpty()) return 0

    var cursor = if (today in doneDates) today else today.minusDays(1)
    var run = 0
    while (cursor in doneDates) {
      run++
      cursor = cursor.minusDays(1)
    }
    return run
  }
}
