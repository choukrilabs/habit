package com.example.core.domain.usecase

import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Goal
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStepMetric
import com.example.core.domain.model.Step
import com.example.core.domain.model.StepType
import com.example.core.domain.model.TrackType
import com.example.core.domain.model.WeekStartDay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class AnalyticsPeriod(val label: String, val days: Int?) {
  D7("7D", 7),
  D30("30D", 30),
  D90("90D", 90),
  Y1("1Y", 365),
  ALL("All", null)
}

data class TrendBucket(
  val label: String,
  val rate: Float, // 0..100
  val movingAverage: Float,
  val previousRate: Float,
  val checkInsCount: Int,
  val scheduledCount: Int
)

data class WeekdayStat(
  val isoDow: Int,
  val shortName: String,
  val fullName: String,
  val rate: Int,
  val doneCount: Int,
  val scheduledCount: Int
)

data class AmountTimeAnalytics(
  val dailyPoints: List<Pair<LocalDate, Double>>,
  val average: Double,
  val median: Double,
  val min: Double,
  val max: Double,
  val total: Double,
  val goalHitRate: Int,
  val weeklySlope: Double,
  val weeklySlopePercent: Int,
  val histogramBuckets: List<Pair<String, Int>>,
  val personalBestValue: Double,
  val personalBestDate: LocalDate?
)

data class HabitAnalyticsResult(
  val currentStreak: Int,
  val bestStreak: Int,
  val periodCompletionRate: Int,
  val periodDelta: Int,
  val totalCheckIns: Int,
  val momentumScore: Int,
  val momentumLabel: String,
  val momentumHint: String,
  val streakAtRisk: Boolean,
  val nextMilestoneTarget: Int,
  val nextMilestoneRemaining: Int,
  val trendBuckets: List<TrendBucket>,
  val bestBucketLabel: String,
  val worstBucketLabel: String,
  val weekdayStats: List<WeekdayStat>,
  val bestWeekday: WeekdayStat?,
  val worstWeekday: WeekdayStat?,
  val hourlyHistogram: List<Int>, // size 24
  val hasEnoughTimestamps: Boolean,
  val peakWindowLabel: String,
  val avgReminderDelayMinutes: Int?,
  val streakHistory: List<StreakInterval>,
  val avgStreakLength: Double,
  val longestGap: Int,
  val consistencyScore: Int,
  val bounceBackRate: Int,
  val perfectWeeks: Int,
  val perfectMonths: Int,
  val recoveryTimeDays: Double,
  val amountTimeAnalytics: AmountTimeAnalytics?,
  val mostMissedWeekday: String?,
  val postSkipMissPattern: String?,
  val recentMissDates: List<LocalDate>,
  val skippedDaysCount: Int,
  val insights: List<String>
)

object AnalyticsCalculator {

  private val MILESTONE_LADDER = listOf(3, 7, 14, 21, 30, 50, 75, 100, 150, 200, 300, 365, 500, 1000)

  fun computePeriodRate(
    habit: Habit,
    entries: Map<LocalDate, Entry>,
    pauses: List<HabitPause>,
    startDate: LocalDate,
    endDate: LocalDate
  ): Int {
    val effectiveStart = if (startDate.isBefore(habit.startDate)) habit.startDate else startDate
    if (effectiveStart.isAfter(endDate)) return 0

    var scheduled = 0
    var skipped = 0
    var done = 0

    var d = effectiveStart
    while (!d.isAfter(endDate)) {
      if (ScheduleMatcher.isScheduledOn(habit, d, pauses, ignorePause = false)) {
        scheduled++
        val entry = entries[d]
        if (entry?.status == EntryStatus.SKIPPED) {
          skipped++
        } else if (ScheduleMatcher.isEntryDone(habit, entry)) {
          done++
        }
      }
      d = d.plusDays(1)
    }

    val denom = scheduled - skipped
    if (denom <= 0) return if (done > 0) 100 else 0
    return ((done.toDouble() / denom.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
  }

  fun calculateAnalytics(
    habit: Habit,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    firstReminderMinutesOfDay: Int?,
    period: AnalyticsPeriod,
    weekStart: WeekStartDay,
    today: LocalDate
  ): HabitAnalyticsResult {
    val entryMap = entries.associateBy { it.date }
    val streakSummary = StreakCalculator.calculateStreaks(habit, entries, pauses, today)

    val periodDays = period.days ?: max(7, ChronoUnit.DAYS.between(habit.startDate, today).toInt() + 1)
    val periodStart = today.minusDays((periodDays - 1).toLong())
    val prevStart = periodStart.minusDays(periodDays.toLong())
    val prevEnd = periodStart.minusDays(1)

    val currentRate = computePeriodRate(habit, entryMap, pauses, periodStart, today)
    val prevRate = computePeriodRate(habit, entryMap, pauses, prevStart, prevEnd)
    val periodDelta = currentRate - prevRate

    val totalCheckIns = entries.count { ScheduleMatcher.isEntryDone(habit, it) }

    // Momentum score = 0.5 * rate(14) + 0.3 * rate(30) + 0.2 * rate(90)
    val rate14 = computePeriodRate(habit, entryMap, pauses, today.minusDays(13), today)
    val rate30 = computePeriodRate(habit, entryMap, pauses, today.minusDays(29), today)
    val rate90 = computePeriodRate(habit, entryMap, pauses, today.minusDays(89), today)
    val momentumScore = (0.5 * rate14 + 0.3 * rate30 + 0.2 * rate90).roundToInt().coerceIn(0, 100)
    val prevMomentum = (
      0.5 * computePeriodRate(habit, entryMap, pauses, today.minusDays(43), today.minusDays(30)) +
      0.3 * computePeriodRate(habit, entryMap, pauses, today.minusDays(59), today.minusDays(30)) +
      0.2 * computePeriodRate(habit, entryMap, pauses, today.minusDays(119), today.minusDays(30))
    ).roundToInt().coerceIn(0, 100)
    val momDiff = momentumScore - prevMomentum

    val momentumLabel = when {
      momentumScore < 30 -> "Cold"
      momentumScore < 60 -> "Warming"
      momentumScore < 85 -> "Strong"
      else -> "On fire"
    }
    val momentumHint = when {
      momDiff > 0 -> "Up $momDiff pts from last month"
      momDiff < 0 -> "Down ${abs(momDiff)} pts from last month"
      else -> "Holding steady from last month"
    }

    val scheduledToday = ScheduleMatcher.isScheduledOn(habit, today, pauses, ignorePause = false)
    val doneToday = ScheduleMatcher.isEntryDone(habit, entryMap[today])
    val skippedToday = entryMap[today]?.status == EntryStatus.SKIPPED
    val streakAtRisk = scheduledToday && !doneToday && !skippedToday && streakSummary.currentStreak > 0

    val nextMilestone = MILESTONE_LADDER.firstOrNull { it > streakSummary.currentStreak } ?: (streakSummary.currentStreak + 100)
    val nextMilestoneRemaining = max(1, nextMilestone - streakSummary.currentStreak)

    // Trend buckets
    val rawBuckets = buildRawBuckets(habit, entryMap, pauses, period, today)
    val trendBuckets = rawBuckets.mapIndexed { index, b ->
      val window = rawBuckets.subList(max(0, index - 3), index + 1)
      val ma = window.map { it.rate }.average().toFloat()
      b.copy(movingAverage = ma)
    }
    val bestBucket = trendBuckets.maxByOrNull { it.rate }?.label ?: "-"
    val worstBucket = trendBuckets.minByOrNull { it.rate }?.label ?: "-"

    // Weekday stats in user's week order
    val orderedDows = LogicalDateCalculator.orderedDaysOfWeek(weekStart)
    val weekdayStats = orderedDows.map { dow ->
      var sched = 0
      var done = 0
      var cursor = habit.startDate
      while (!cursor.isAfter(today)) {
        if (cursor.dayOfWeek == dow && ScheduleMatcher.isScheduledOn(habit, cursor, pauses)) {
          val e = entryMap[cursor]
          if (e?.status != EntryStatus.SKIPPED) {
            sched++
            if (ScheduleMatcher.isEntryDone(habit, e)) done++
          }
        }
        cursor = cursor.plusDays(1)
      }
      val r = if (sched > 0) ((done.toDouble() / sched) * 100).roundToInt() else 0
      WeekdayStat(
        isoDow = dow.value,
        shortName = ScheduleMatcher.dowShortLabel(dow.value),
        fullName = dow.name.lowercase().replaceFirstChar { it.uppercase() },
        rate = r,
        doneCount = done,
        scheduledCount = sched
      )
    }
    val activeWeekdayStats = weekdayStats.filter { it.scheduledCount > 0 }
    val bestWeekday = activeWeekdayStats.maxByOrNull { it.rate }
    val worstWeekday = activeWeekdayStats.minByOrNull { it.rate }

    // Hourly histogram
    val timestamped = entries.filter { it.status == EntryStatus.DONE && it.completedAt != null }
    val hourly = MutableList(24) { 0 }
    val zone = ZoneId.systemDefault()
    timestamped.forEach { e ->
      val h = e.completedAt!!.atZone(zone).hour
      hourly[h] = hourly[h] + 1
    }
    var bestHourStart = 6
    var maxTwoHourSum = -1
    for (h in 0..22) {
      val sum = hourly[h] + hourly[h + 1]
      if (sum > maxTwoHourSum) {
        maxTwoHourSum = sum
        bestHourStart = h
      }
    }
    val peakWindowLabel = String.format("%02d:00–%02d:00", bestHourStart, (bestHourStart + 2) % 24)
    val avgDelay = if (firstReminderMinutesOfDay != null && timestamped.isNotEmpty()) {
      val delays = timestamped.mapNotNull { e ->
        val lt = e.completedAt!!.atZone(zone).toLocalTime()
        val mins = lt.hour * 60 + lt.minute
        val diff = mins - firstReminderMinutesOfDay
        if (diff in 0..360) diff else null
      }
      if (delays.isNotEmpty()) delays.average().roundToInt() else null
    } else null

    // Consistency, Bounce-back, Recovery, Perfect weeks/months
    val weeklyRates = (0..11).map { w ->
      val wEnd = today.minusDays((w * 7).toLong())
      val wStart = wEnd.minusDays(6)
      computePeriodRate(habit, entryMap, pauses, wStart, wEnd).toDouble()
    }
    val meanWeekly = weeklyRates.average()
    val variance = weeklyRates.map { (it - meanWeekly) * (it - meanWeekly) }.average()
    val stddev = sqrt(variance)
    val consistencyScore = (100.0 - min(100.0, stddev * 2.0)).roundToInt().coerceIn(0, 100)

    var totalMisses = 0
    var bounceBacks = 0
    val recoveryStretches = mutableListOf<Int>()
    var currentMissRun = 0
    val missedDates = mutableListOf<LocalDate>()
    var missesAfterSkip = 0
    var totalSkips = 0
    var prevWasSkip = false

    var cursor = habit.startDate
    while (!cursor.isAfter(today.minusDays(1))) {
      if (ScheduleMatcher.isScheduledOn(habit, cursor, pauses)) {
        val e = entryMap[cursor]
        when {
          e?.status == EntryStatus.SKIPPED -> {
            totalSkips++
            prevWasSkip = true
          }
          ScheduleMatcher.isEntryDone(habit, e) -> {
            if (currentMissRun == 1) bounceBacks++
            if (currentMissRun > 0) {
              recoveryStretches.add(currentMissRun)
              currentMissRun = 0
            }
            prevWasSkip = false
          }
          else -> {
            totalMisses++
            currentMissRun++
            missedDates.add(cursor)
            if (prevWasSkip) missesAfterSkip++
            prevWasSkip = false
          }
        }
      }
      cursor = cursor.plusDays(1)
    }
    if (entryMap[today]?.status == EntryStatus.SKIPPED) totalSkips++

    val bounceBackRate = if (totalMisses > 0) ((bounceBacks.toDouble() / totalMisses) * 100).roundToInt().coerceIn(0, 100) else 100
    val recoveryTime = if (recoveryStretches.isNotEmpty()) recoveryStretches.average() else 0.0

    // Perfect weeks & months
    var perfectWeeks = 0
    var wkStart = LogicalDateCalculator.startOfWeek(habit.startDate, weekStart)
    while (!wkStart.plusDays(6).isAfter(today)) {
      var anySched = false
      var allDone = true
      for (i in 0..6) {
        val d = wkStart.plusDays(i.toLong())
        if (ScheduleMatcher.isScheduledOn(habit, d, pauses)) {
          val e = entryMap[d]
          if (e?.status != EntryStatus.SKIPPED) {
            anySched = true
            if (!ScheduleMatcher.isEntryDone(habit, e)) {
              allDone = false
              break
            }
          }
        }
      }
      if (anySched && allDone) perfectWeeks++
      wkStart = wkStart.plusWeeks(1)
    }

    var perfectMonths = 0
    var ym = YearMonth.from(habit.startDate)
    val currentYm = YearMonth.from(today)
    while (ym.isBefore(currentYm)) {
      var anySched = false
      var allDone = true
      for (day in 1..ym.lengthOfMonth()) {
        val d = ym.atDay(day)
        if (ScheduleMatcher.isScheduledOn(habit, d, pauses)) {
          val e = entryMap[d]
          if (e?.status != EntryStatus.SKIPPED) {
            anySched = true
            if (!ScheduleMatcher.isEntryDone(habit, e)) {
              allDone = false
              break
            }
          }
        }
      }
      if (anySched && allDone) perfectMonths++
      ym = ym.plusMonths(1)
    }

    // Amount / Time analytics
    val amountTimeAnalytics = if (habit.trackType != TrackType.TASK) {
      val valEntries = entries
        .filter { it.value != null && !it.date.isBefore(periodStart) && !it.date.isAfter(today) }
        .sortedBy { it.date }
      val values = valEntries.map { it.value!! }
      if (values.isNotEmpty()) {
        val sortedVals = values.sorted()
        val avg = values.average()
        val median = if (sortedVals.size % 2 == 1) {
          sortedVals[sortedVals.size / 2]
        } else {
          (sortedVals[sortedVals.size / 2 - 1] + sortedVals[sortedVals.size / 2]) / 2.0
        }
        val minVal = sortedVals.first()
        val maxVal = sortedVals.last()
        val totalVal = values.sum()
        val hitCount = valEntries.count { ScheduleMatcher.isEntryDone(habit, it) }
        val hitRate = ((hitCount.toDouble() / valEntries.size) * 100).roundToInt()

        // Linear regression slope per week
        val n = valEntries.size
        val slopePerDay = if (n >= 2) {
          val xMean = (n - 1) / 2.0
          val yMean = avg
          var num = 0.0
          var den = 0.0
          valEntries.forEachIndexed { idx, e ->
            num += (idx - xMean) * (e.value!! - yMean)
            den += (idx - xMean) * (idx - xMean)
          }
          if (den > 0) num / den else 0.0
        } else 0.0
        val weeklySlope = slopePerDay * 7.0
        val slopePct = if (avg > 0) ((weeklySlope / avg) * 100).roundToInt() else 0

        val bestEntry = valEntries.maxByOrNull { it.value!! }
        val step = max(1.0, (maxVal - minVal) / 4.0)
        val hist = (0..3).map { bIdx ->
          val low = minVal + bIdx * step
          val high = if (bIdx == 3) maxVal + 0.01 else minVal + (bIdx + 1) * step
          val count = values.count { it >= low && it < high }
          "${low.roundToInt()}–${high.roundToInt()}" to count
        }

        AmountTimeAnalytics(
          dailyPoints = valEntries.map { it.date to it.value!! },
          average = avg,
          median = median,
          min = minVal,
          max = maxVal,
          total = totalVal,
          goalHitRate = hitRate,
          weeklySlope = weeklySlope,
          weeklySlopePercent = slopePct,
          histogramBuckets = hist,
          personalBestValue = bestEntry?.value ?: 0.0,
          personalBestDate = bestEntry?.date
        )
      } else null
    } else null

    val mostMissedDow = missedDates
      .groupingBy { it.dayOfWeek }
      .eachCount()
      .maxByOrNull { it.value }
      ?.key
      ?.name
      ?.lowercase()
      ?.replaceFirstChar { it.uppercase() }

    val postSkipPattern = if (totalSkips >= 2 && missesAfterSkip > 0) {
      val pct = ((missesAfterSkip.toDouble() / totalSkips) * 100).roundToInt()
      "You miss this $pct% of the time right after a skipped day."
    } else null

    // Insights (Section 7.3)
    val insights = mutableListOf<String>()
    if (bestWeekday != null && worstWeekday != null && (bestWeekday.rate - worstWeekday.rate) >= 20) {
      insights.add("You're ${bestWeekday.rate - worstWeekday.rate} pts stronger on ${bestWeekday.fullName}s than ${worstWeekday.fullName}s.")
    }
    if (abs(periodDelta) >= 8) {
      val dir = if (periodDelta > 0) "up" else "down"
      insights.add("Trending $dir — $currentRate% this period (${if (periodDelta > 0) "+$periodDelta" else "$periodDelta"} pts).")
    }
    val toRecord = streakSummary.bestStreak - streakSummary.currentStreak
    if (streakSummary.bestStreak > 0 && toRecord in 1..3) {
      insights.add("$toRecord more ${if (toRecord == 1) "day" else "days"} to beat your all-time record of ${streakSummary.bestStreak}.")
    }
    if (timestamped.size >= 6 && maxTwoHourSum.toDouble() / timestamped.size >= 0.5) {
      insights.add("Most check-ins happen around $peakWindowLabel.")
    }
    if (mostMissedDow != null && missedDates.count { it.isAfter(today.minusWeeks(8)) } >= 3) {
      insights.add("${mostMissedDow}s have been the toughest day for this habit recently.")
    }
    if (insights.isEmpty()) {
      insights.add("Steady consistency builds momentum — every small mark counts.")
    }

    val avgStreakLen = if (streakSummary.intervals.isNotEmpty()) {
      streakSummary.intervals.map { it.length }.average()
    } else 0.0

    return HabitAnalyticsResult(
      currentStreak = streakSummary.currentStreak,
      bestStreak = streakSummary.bestStreak,
      periodCompletionRate = currentRate,
      periodDelta = periodDelta,
      totalCheckIns = totalCheckIns,
      momentumScore = momentumScore,
      momentumLabel = momentumLabel,
      momentumHint = momentumHint,
      streakAtRisk = streakAtRisk,
      nextMilestoneTarget = nextMilestone,
      nextMilestoneRemaining = nextMilestoneRemaining,
      trendBuckets = trendBuckets,
      bestBucketLabel = bestBucket,
      worstBucketLabel = worstBucket,
      weekdayStats = weekdayStats,
      bestWeekday = bestWeekday,
      worstWeekday = worstWeekday,
      hourlyHistogram = hourly,
      hasEnoughTimestamps = timestamped.size >= 10,
      peakWindowLabel = peakWindowLabel,
      avgReminderDelayMinutes = avgDelay,
      streakHistory = streakSummary.intervals,
      avgStreakLength = avgStreakLen,
      longestGap = streakSummary.longestGap,
      consistencyScore = consistencyScore,
      bounceBackRate = bounceBackRate,
      perfectWeeks = perfectWeeks,
      perfectMonths = perfectMonths,
      recoveryTimeDays = recoveryTime,
      amountTimeAnalytics = amountTimeAnalytics,
      mostMissedWeekday = mostMissedDow,
      postSkipMissPattern = postSkipPattern,
      recentMissDates = missedDates.takeLast(5).reversed(),
      skippedDaysCount = totalSkips,
      insights = insights.take(5)
    )
  }

  private fun buildRawBuckets(
    habit: Habit,
    entryMap: Map<LocalDate, Entry>,
    pauses: List<HabitPause>,
    period: AnalyticsPeriod,
    today: LocalDate
  ): List<TrendBucket> {
    return when (period) {
      AnalyticsPeriod.D7 -> {
        (6 downTo 0).map { offset ->
          val d = today.minusDays(offset.toLong())
          val prevD = d.minusDays(7)
          val rate = computePeriodRate(habit, entryMap, pauses, d, d).toFloat()
          val prevRate = computePeriodRate(habit, entryMap, pauses, prevD, prevD).toFloat()
          val sched = if (ScheduleMatcher.isScheduledOn(habit, d, pauses)) 1 else 0
          val done = if (ScheduleMatcher.isEntryDone(habit, entryMap[d])) 1 else 0
          TrendBucket(
            label = ScheduleMatcher.dowShortLabel(d.dayOfWeek.value),
            rate = rate,
            movingAverage = rate,
            previousRate = prevRate,
            checkInsCount = done,
            scheduledCount = sched
          )
        }
      }
      AnalyticsPeriod.D30, AnalyticsPeriod.D90 -> {
        val weeks = if (period == AnalyticsPeriod.D30) 5 else 12
        (weeks - 1 downTo 0).map { w ->
          val wEnd = today.minusDays((w * 7).toLong())
          val wStart = wEnd.minusDays(6)
          val prevEnd = wStart.minusDays((weeks * 7).toLong() - 6)
          val prevStart = prevEnd.minusDays(6)
          var sched = 0
          var done = 0
          for (i in 0..6) {
            val d = wStart.plusDays(i.toLong())
            if (ScheduleMatcher.isScheduledOn(habit, d, pauses)) {
              sched++
              if (ScheduleMatcher.isEntryDone(habit, entryMap[d])) done++
            }
          }
          val rate = computePeriodRate(habit, entryMap, pauses, wStart, wEnd).toFloat()
          val prevRate = computePeriodRate(habit, entryMap, pauses, prevStart, prevEnd).toFloat()
          TrendBucket(
            label = "${wStart.dayOfMonth}/${wStart.monthValue}",
            rate = rate,
            movingAverage = rate,
            previousRate = prevRate,
            checkInsCount = done,
            scheduledCount = sched
          )
        }
      }
      AnalyticsPeriod.Y1, AnalyticsPeriod.ALL -> {
        (11 downTo 0).map { m ->
          val ym = YearMonth.from(today).minusMonths(m.toLong())
          val mStart = ym.atDay(1)
          val mEnd = if (m == 0) today else ym.atEndOfMonth()
          val prevYm = ym.minusYears(1)
          var sched = 0
          var done = 0
          var d = mStart
          while (!d.isAfter(mEnd)) {
            if (ScheduleMatcher.isScheduledOn(habit, d, pauses)) {
              sched++
              if (ScheduleMatcher.isEntryDone(habit, entryMap[d])) done++
            }
            d = d.plusDays(1)
          }
          val rate = computePeriodRate(habit, entryMap, pauses, mStart, mEnd).toFloat()
          val prevRate = computePeriodRate(habit, entryMap, pauses, prevYm.atDay(1), prevYm.atEndOfMonth()).toFloat()
          TrendBucket(
            label = ym.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
            rate = rate,
            movingAverage = rate,
            previousRate = prevRate,
            checkInsCount = done,
            scheduledCount = sched
          )
        }
      }
    }
  }
}

enum class PaceStatus(val label: String) {
  AHEAD("Ahead"),
  ON_TRACK("On track"),
  BEHIND("Behind"),
  INSUFFICIENT_DATA("Building pace")
}

data class GoalProgressSummary(
  val goal: Goal,
  val steps: List<Step>,
  val stepProgressMap: Map<String, Float>, // stepId -> 0f..1f
  val stepProgressLabelMap: Map<String, String>,
  val overallPercent: Int,
  val completedStepsCount: Int,
  val totalStepsCount: Int,
  val nextStep: Step?,
  val overdueStepsCount: Int,
  val daysRemaining: Long?,
  val projectedFinishDate: LocalDate?,
  val paceStatus: PaceStatus,
  val stepsPerWeekBars: List<Pair<String, Int>>
)

object GoalStepEvaluator {

  fun calculateGoalProgress(steps: List<Step>): Float {
    if (steps.isEmpty()) return 0f
    val sum = steps.sumOf { s ->
      when {
        s.done -> 1.0
        s.type == StepType.AMOUNT -> {
          val target = (s.amountTarget ?: 1.0).coerceAtLeast(0.01)
          ((s.amountCurrent ?: 0.0) / target).coerceIn(0.0, 1.0)
        }
        else -> 0.0
      }
    }
    return (sum / steps.size).toFloat().coerceIn(0f, 1f)
  }

  fun evaluateHabitStepProgress(
    step: Step,
    habit: Habit?,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): Pair<Float, String> {
    if (step.type != StepType.HABIT || habit == null) {
      return (if (step.done) 1f else 0f) to (if (step.done) "Done" else "Manual")
    }
    val target = (step.habitTarget ?: 14).coerceAtLeast(1)
    return when (step.habitMetric ?: HabitStepMetric.STREAK) {
      HabitStepMetric.STREAK -> {
        val streak = StreakCalculator.calculateStreaks(habit, entries, pauses, today).currentStreak
        val current = if (step.done) max(streak, target) else streak
        val ratio = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        ratio to "$current / $target days"
      }
      HabitStepMetric.TOTAL_CHECKINS -> {
        val count = entries.count { ScheduleMatcher.isEntryDone(habit, it) }
        val current = if (step.done) max(count, target) else count
        val ratio = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        ratio to "$current / $target check-ins"
      }
      HabitStepMetric.RATE -> {
        val window = (step.habitWindowDays ?: 30).coerceAtLeast(7)
        val entryMap = entries.associateBy { it.date }
        val rate = AnalyticsCalculator.computePeriodRate(habit, entryMap, pauses, today.minusDays((window - 1).toLong()), today)
        val current = if (step.done) max(rate, target) else rate
        val ratio = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        ratio to "$current% / $target% (${window}d)"
      }
    }
  }

  fun computeGoalSummary(
    goal: Goal,
    steps: List<Step>,
    habitsMap: Map<String, Habit>,
    entriesByHabit: Map<String, List<Entry>>,
    pausesByHabit: Map<String, List<HabitPause>>,
    today: LocalDate
  ): GoalProgressSummary {
    val progressMap = mutableMapOf<String, Float>()
    val labelMap = mutableMapOf<String, String>()

    steps.forEach { step ->
      when (step.type) {
        StepType.MANUAL -> {
          progressMap[step.id] = if (step.done) 1f else 0f
          labelMap[step.id] = if (step.done) "Done" else "Manual"
        }
        StepType.AMOUNT -> {
          val target = (step.amountTarget ?: 1.0).coerceAtLeast(0.01)
          val current = step.amountCurrent ?: 0.0
          val ratio = if (step.done) 1f else (current / target).toFloat().coerceIn(0f, 1f)
          val unitStr = step.amountUnit?.let { " $it" } ?: ""
          progressMap[step.id] = ratio
          labelMap[step.id] = "${current.roundToInt()} / ${target.roundToInt()}$unitStr"
        }
        StepType.HABIT -> {
          val habit = step.linkedHabitId?.let { habitsMap[it] }
          val hEntries = step.linkedHabitId?.let { entriesByHabit[it] }.orEmpty()
          val hPauses = step.linkedHabitId?.let { pausesByHabit[it] }.orEmpty()
          val (ratio, label) = evaluateHabitStepProgress(step, habit, hEntries, hPauses, today)
          progressMap[step.id] = if (step.done) 1f else ratio
          labelMap[step.id] = label
        }
      }
    }

    val totalSteps = steps.size
    val completedCount = steps.count { it.done || (progressMap[it.id] ?: 0f) >= 1f }
    val sumProgress = steps.sumOf { (progressMap[it.id] ?: 0f).toDouble() }
    val overallPercent = if (totalSteps > 0) ((sumProgress / totalSteps) * 100.0).roundToInt().coerceIn(0, 100) else 0

    val nextStep = steps.sortedBy { it.sortOrder }.firstOrNull { !it.done && (progressMap[it.id] ?: 0f) < 1f }
    val overdueCount = steps.count { !it.done && it.dueDate != null && it.dueDate.isBefore(today) }
    val daysRemaining = goal.targetDate?.let { ChronoUnit.DAYS.between(today, it) }

    val zone = ZoneId.systemDefault()
    val doneLast28 = steps.count { s ->
      s.done && s.doneAt != null && !s.doneAt.atZone(zone).toLocalDate().isBefore(today.minusDays(27))
    }
    val remainingSteps = totalSteps - completedCount

    var projectedDate: LocalDate? = null
    var paceStatus = PaceStatus.INSUFFICIENT_DATA
    if (completedCount >= 2 && remainingSteps > 0) {
      val pacePerWeek = max(0.25, doneLast28 / 4.0)
      val weeksToFinish = remainingSteps / pacePerWeek
      val daysToFinish = (weeksToFinish * 7.0).roundToInt().toLong()
      projectedDate = today.plusDays(daysToFinish)
      if (goal.targetDate != null) {
        val diffDays = ChronoUnit.DAYS.between(projectedDate, goal.targetDate)
        paceStatus = when {
          diffDays >= 7 -> PaceStatus.AHEAD
          diffDays < -7 -> PaceStatus.BEHIND
          else -> PaceStatus.ON_TRACK
        }
      } else {
        paceStatus = PaceStatus.ON_TRACK
      }
    } else if (remainingSteps == 0 && totalSteps > 0) {
      projectedDate = today
      paceStatus = PaceStatus.AHEAD
    }

    val bars = (3 downTo 0).map { w ->
      val wEnd = today.minusDays((w * 7).toLong())
      val wStart = wEnd.minusDays(6)
      val count = steps.count { s ->
        val d = s.doneAt?.atZone(zone)?.toLocalDate()
        d != null && !d.isBefore(wStart) && !d.isAfter(wEnd)
      }
      "W${4 - w}" to count
    }

    return GoalProgressSummary(
      goal = goal,
      steps = steps.sortedBy { it.sortOrder },
      stepProgressMap = progressMap,
      stepProgressLabelMap = labelMap,
      overallPercent = overallPercent,
      completedStepsCount = completedCount,
      totalStepsCount = totalSteps,
      nextStep = nextStep,
      overdueStepsCount = overdueCount,
      daysRemaining = daysRemaining,
      projectedFinishDate = projectedDate,
      paceStatus = paceStatus,
      stepsPerWeekBars = bars
    )
  }
}
