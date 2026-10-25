package com.example

import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStepMetric
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.Step
import com.example.core.domain.model.StepType
import com.example.core.domain.model.TrackType
import com.example.core.domain.model.WeekStartDay
import com.example.core.domain.usecase.GoalStepEvaluator
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.domain.usecase.StreakCalculator
import com.example.feature.widgets.glance.CircleStateMapper
import com.example.feature.widgets.glance.WidgetCircleVisualState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun logicalDateRollover_respectsDayStartRolloverTime() {
    val rollover = LocalTime.of(3, 0)
    val beforeRollover = LocalDateTime.of(2026, 5, 10, 2, 59)
    val atRollover = LocalDateTime.of(2026, 5, 10, 3, 0)

    assertEquals(
      LocalDate.of(2026, 5, 9),
      LogicalDateCalculator.now(rollover, beforeRollover)
    )
    assertEquals(
      LocalDate.of(2026, 5, 10),
      LogicalDateCalculator.now(rollover, atRollover)
    )
  }

  @Test
  fun hijriFormatting_includesBothGregorianAndHijriWhenEnabled() {
    val formattedWithHijri = LogicalDateCalculator.formatHeaderDate(
      date = LocalDate.of(2026, 3, 20),
      showHijri = true
    )
    assertTrue(formattedWithHijri.contains("·"))
  }

  @Test
  fun streakCalculator_preservesStreakAcrossSkipAndPause_andBreaksOnMiss() {
    val habit = Habit(
      id = "h1",
      name = "Read 20 pages",
      iconKey = "book",
      colorKey = "#F4C28E",
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      trackType = TrackType.TASK,
      startDate = LocalDate.of(2026, 5, 1)
    )
    val d1 = LocalDate.of(2026, 5, 1)
    val d2 = LocalDate.of(2026, 5, 2) // Skipped
    val d3 = LocalDate.of(2026, 5, 3) // Paused
    val d4 = LocalDate.of(2026, 5, 4) // Done

    val entries = listOf(
      Entry(id = "e1", habitId = "h1", date = d1, status = EntryStatus.DONE, value = 1.0, completedAt = Instant.now()),
      Entry(id = "e2", habitId = "h1", date = d2, status = EntryStatus.SKIPPED, value = 0.0, completedAt = Instant.now()),
      Entry(id = "e4", habitId = "h1", date = d4, status = EntryStatus.DONE, value = 1.0, completedAt = Instant.now())
    )
    val pauses = listOf(
      HabitPause(id = "p1", habitId = "h1", startDate = d3, endDate = d3)
    )

    val summary = StreakCalculator.calculateStreaks(habit, entries, pauses, d4)
    assertEquals(2, summary.currentStreak)
    assertEquals(2, summary.bestStreak)

    val d6 = LocalDate.of(2026, 5, 6)
    val summaryAfterMiss = StreakCalculator.calculateStreaks(habit, entries, pauses, d6)
    assertEquals(0, summaryAfterMiss.currentStreak)
    assertEquals(2, summaryAfterMiss.bestStreak)
  }

  @Test
  fun goalStepEvaluator_computesProgressAndHabitStepCompletion() {
    val habit = Habit(
      id = "h_run",
      name = "Morning Run",
      iconKey = "run",
      colorKey = "#9EE2C8",
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      trackType = TrackType.TASK,
      startDate = LocalDate.of(2026, 1, 1)
    )
    val step = Step(
      id = "s1",
      goalId = "g1",
      title = "Complete 3 runs",
      type = StepType.HABIT,
      linkedHabitId = "h_run",
      habitMetric = HabitStepMetric.TOTAL_CHECKINS,
      habitTarget = 3,
      sortOrder = 0
    )
    val entries = (1..3).map { day ->
      Entry(
        id = "e_$day",
        habitId = "h_run",
        date = LocalDate.of(2026, 1, day),
        status = EntryStatus.DONE,
        value = 1.0,
        completedAt = Instant.now()
      )
    }

    val (ratio, label) = GoalStepEvaluator.evaluateHabitStepProgress(
      step = step,
      habit = habit,
      entries = entries,
      pauses = emptyList(),
      today = LocalDate.of(2026, 1, 3)
    )
    assertEquals(1.0f, ratio, 0.001f)
    assertTrue(label.contains("3 / 3"))
  }

  @Test
  fun circleStateMapper_mapsAllSixWidgetStates() {
    val today = LocalDate.of(2026, 5, 10)
    assertEquals(
      WidgetCircleVisualState.DONE,
      CircleStateMapper.mapState(
        isScheduled = true,
        entryStatus = EntryStatus.DONE,
        isSatisfiedDone = true,
        date = today,
        today = today
      )
    )
    assertEquals(
      WidgetCircleVisualState.SKIPPED,
      CircleStateMapper.mapState(
        isScheduled = true,
        entryStatus = EntryStatus.SKIPPED,
        isSatisfiedDone = false,
        date = today,
        today = today
      )
    )
    assertEquals(
      WidgetCircleVisualState.TODAY_PENDING,
      CircleStateMapper.mapState(
        isScheduled = true,
        entryStatus = null,
        isSatisfiedDone = false,
        date = today,
        today = today
      )
    )
    assertEquals(
      WidgetCircleVisualState.PAST_MISSED,
      CircleStateMapper.mapState(
        isScheduled = true,
        entryStatus = null,
        isSatisfiedDone = false,
        date = today.minusDays(1),
        today = today
      )
    )
    assertEquals(
      WidgetCircleVisualState.NOT_SCHEDULED,
      CircleStateMapper.mapState(
        isScheduled = false,
        entryStatus = null,
        isSatisfiedDone = false,
        date = today.minusDays(1),
        today = today
      )
    )
    assertEquals(
      WidgetCircleVisualState.FUTURE,
      CircleStateMapper.mapState(
        isScheduled = true,
        entryStatus = null,
        isSatisfiedDone = false,
        date = today.plusDays(1),
        today = today
      )
    )
  }

  @Test
  fun scheduleMatcher_respectsStartEndAndPauses() {
    val habit = Habit(
      id = "h2",
      name = "Gym",
      iconKey = "dumbbell",
      colorKey = "#F39C9C",
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 3, 5), // Mon, Wed, Fri
      trackType = TrackType.TASK,
      startDate = LocalDate.of(2026, 5, 4) // Monday
    )
    val mon = LocalDate.of(2026, 5, 4)
    val tue = LocalDate.of(2026, 5, 5)
    val wed = LocalDate.of(2026, 5, 6)
    val pauses = listOf(HabitPause(id = "p", habitId = "h2", startDate = wed, endDate = wed))

    assertTrue(ScheduleMatcher.isScheduledOn(habit, mon, pauses))
    assertFalse(ScheduleMatcher.isScheduledOn(habit, tue, pauses))
    assertFalse(ScheduleMatcher.isScheduledOn(habit, wed, pauses))
    assertEquals(7, LogicalDateCalculator.orderedDaysOfWeek(WeekStartDay.MONDAY).size)
  }
}
