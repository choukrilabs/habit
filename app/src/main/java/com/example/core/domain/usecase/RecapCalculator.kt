package com.example.core.domain.usecase

import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Goal
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.Step
import com.example.core.domain.model.TrackType
import com.example.core.domain.model.WeekStartDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt

data class HabitRecapItem(
  val habit: Habit,
  val completionRate: Int,
  val deltaVsPrevious: Int,
  val doneCount: Int,
  val scheduledCount: Int,
  val weekPattern: List<Boolean> = emptyList() // 7 booleans for weekly recap
)

data class DayStripItem(
  val date: LocalDate,
  val shortLabel: String,
  val completionRate: Int,
  val doneCount: Int,
  val scheduledCount: Int,
  val isPerfect: Boolean,
  val isToday: Boolean
)

data class RecapReport(
  val title: String,
  val subtitle: String,
  val isSoFar: Boolean,
  val hasEnoughData: Boolean,
  val overallRate: Int,
  val deltaVsPrevious: Int,
  val doneCheckIns: Int,
  val scheduledCheckIns: Int,
  val perfectDays: Int,
  val activeDays: Int,
  val bestStreakInPeriod: Int,
  val skippedDays: Int,
  val newHabitsStarted: Int,
  val streaksKeptAlive: Int,
  val streaksBroken: Int,
  val dayStrip: List<DayStripItem>,
  val weekBars: List<Pair<String, Int>>,
  val leaderboard: List<HabitRecapItem>,
  val mostConsistentHabit: HabitRecapItem?,
  val needsLoveHabit: HabitRecapItem?,
  val bestWeekday: Pair<String, Int>?,
  val worstWeekday: Pair<String, Int>?,
  val bestSingleDay: Pair<LocalDate, Int>?,
  val peakHourLabel: String,
  val hourlyHistogram: List<Int>,
  val pausedSummary: String?,
  val insights: List<String>,
  val nextFocusSuggestion: String
)

data class YearInReviewReport(
  val year: Int,
  val isSoFar: Boolean,
  val daysTracked: Int,
  val totalCheckIns: Int,
  val totalMinutesTracked: Int,
  val overallCompletionRate: Int,
  val deltaVsPreviousYear: Int?,
  val monthlyRates: List<Pair<String, Int>>, // 12 months
  val bestMonth: Pair<String, Int>?,
  val toughestMonth: Pair<String, Int>?,
  val dailyIntensityMap: Map<LocalDate, Float>, // for 53x7 heatmap
  val longestStreakHabit: Habit?,
  val longestStreakLength: Int,
  val mostConsistentHabit: HabitRecapItem?,
  val mostImprovedHabit: Pair<Habit, Int>?, // habit + H1->H2 gain
  val weekdayRates: List<Pair<String, Int>>,
  val bestWeekday: String,
  val peakHourLabel: String,
  val topHabitsByCheckIns: List<Pair<Habit, Int>>,
  val perfectDaysCount: Int,
  val bestPerfectDayRun: Int
)

object RecapCalculator {

  fun computeWeeklyRecap(
    weekStart: LocalDate,
    habits: List<Habit>,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): RecapReport {
    val weekEnd = if (weekStart.plusDays(6).isAfter(today)) today else weekStart.plusDays(6)
    val prevWeekStart = weekStart.minusDays(7)
    val prevWeekEnd = weekStart.minusDays(1)
    return buildPeriodRecap(
      startDate = weekStart,
      endDate = weekEnd,
      prevStartDate = prevWeekStart,
      prevEndDate = prevWeekEnd,
      isWeekly = true,
      isSoFar = weekStart.plusDays(6).isAfter(today),
      habits = habits,
      entries = entries,
      pauses = pauses,
      today = today
    )
  }

  fun computeMonthlyRecap(
    yearMonth: YearMonth,
    habits: List<Habit>,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): RecapReport {
    val startDate = yearMonth.atDay(1)
    val rawEnd = yearMonth.atEndOfMonth()
    val endDate = if (rawEnd.isAfter(today)) today else rawEnd
    val prevYm = yearMonth.minusMonths(1)
    return buildPeriodRecap(
      startDate = startDate,
      endDate = endDate,
      prevStartDate = prevYm.atDay(1),
      prevEndDate = prevYm.atEndOfMonth(),
      isWeekly = false,
      isSoFar = rawEnd.isAfter(today),
      habits = habits,
      entries = entries,
      pauses = pauses,
      today = today
    )
  }

  private fun buildPeriodRecap(
    startDate: LocalDate,
    endDate: LocalDate,
    prevStartDate: LocalDate,
    prevEndDate: LocalDate,
    isWeekly: Boolean,
    isSoFar: Boolean,
    habits: List<Habit>,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): RecapReport {
    val activeHabitsInPeriod = habits.filter { h ->
      !h.startDate.isAfter(endDate) && (h.endDate == null || !h.endDate.isBefore(startDate))
    }
    val entriesByHabit = entries.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.date } }
    val pausesByHabit = pauses.groupBy { it.habitId }

    var totalDone = 0
    var totalScheduled = 0
    var skippedCount = 0
    var perfectDays = 0
    var activeDays = 0
    var bestSingleDay: Pair<LocalDate, Int>? = null
    val dayStrip = mutableListOf<DayStripItem>()
    val dowDone = mutableMapOf<DayOfWeek, Int>()
    val dowSched = mutableMapOf<DayOfWeek, Int>()

    val stripEnd = if (isWeekly) startDate.plusDays(6) else endDate
    var d = startDate
    while (!d.isAfter(stripEnd)) {
      var daySched = 0
      var dayDone = 0
      if (!d.isAfter(endDate)) {
        activeHabitsInPeriod.forEach { h ->
          val hPauses = pausesByHabit[h.id].orEmpty()
          if (ScheduleMatcher.isScheduledOn(h, d, hPauses)) {
            val e = entriesByHabit[h.id]?.get(d)
            if (e?.status == EntryStatus.SKIPPED) {
              skippedCount++
            } else {
              daySched++
              totalScheduled++
              dowSched[d.dayOfWeek] = (dowSched[d.dayOfWeek] ?: 0) + 1
              if (ScheduleMatcher.isEntryDone(h, e)) {
                dayDone++
                totalDone++
                dowDone[d.dayOfWeek] = (dowDone[d.dayOfWeek] ?: 0) + 1
              }
            }
          }
        }
        if (dayDone > 0) activeDays++
        if (daySched > 0 && dayDone == daySched) perfectDays++
        if (bestSingleDay == null || dayDone > bestSingleDay!!.second) {
          bestSingleDay = d to dayDone
        }
      }
      val rate = if (daySched > 0) ((dayDone.toDouble() / daySched) * 100).roundToInt() else 0
      dayStrip.add(
        DayStripItem(
          date = d,
          shortLabel = ScheduleMatcher.dowShortLabel(d.dayOfWeek.value),
          completionRate = rate,
          doneCount = dayDone,
          scheduledCount = daySched,
          isPerfect = daySched > 0 && dayDone == daySched && !d.isAfter(endDate),
          isToday = d == today
        )
      )
      d = d.plusDays(1)
    }

    val overallRate = if (totalScheduled > 0) ((totalDone.toDouble() / totalScheduled) * 100).roundToInt() else 0

    // Previous period rate
    var prevDone = 0
    var prevSched = 0
    var pd = prevStartDate
    while (!pd.isAfter(prevEndDate)) {
      activeHabitsInPeriod.forEach { h ->
        val hPauses = pausesByHabit[h.id].orEmpty()
        if (ScheduleMatcher.isScheduledOn(h, pd, hPauses)) {
          val e = entriesByHabit[h.id]?.get(pd)
          if (e?.status != EntryStatus.SKIPPED) {
            prevSched++
            if (ScheduleMatcher.isEntryDone(h, e)) prevDone++
          }
        }
      }
      pd = pd.plusDays(1)
    }
    val prevRate = if (prevSched > 0) ((prevDone.toDouble() / prevSched) * 100).roundToInt() else 0
    val delta = overallRate - prevRate

    // Leaderboard
    val leaderboard = activeHabitsInPeriod.map { h ->
      val hMap = entriesByHabit[h.id].orEmpty()
      val hPauses = pausesByHabit[h.id].orEmpty()
      val hRate = AnalyticsCalculator.computePeriodRate(h, hMap, hPauses, startDate, endDate)
      val hPrevRate = AnalyticsCalculator.computePeriodRate(h, hMap, hPauses, prevStartDate, prevEndDate)
      var hDone = 0
      var hSched = 0
      var cursor = startDate
      while (!cursor.isAfter(endDate)) {
        if (ScheduleMatcher.isScheduledOn(h, cursor, hPauses)) {
          val e = hMap[cursor]
          if (e?.status != EntryStatus.SKIPPED) {
            hSched++
            if (ScheduleMatcher.isEntryDone(h, e)) hDone++
          }
        }
        cursor = cursor.plusDays(1)
      }
      val pattern = if (isWeekly) {
        (0..6).map { offset ->
          val day = startDate.plusDays(offset.toLong())
          ScheduleMatcher.isEntryDone(h, hMap[day])
        }
      } else emptyList()

      HabitRecapItem(
        habit = h,
        completionRate = hRate,
        deltaVsPrevious = hRate - hPrevRate,
        doneCount = hDone,
        scheduledCount = hSched,
        weekPattern = pattern
      )
    }.sortedByDescending { it.completionRate }

    val mostConsistent = leaderboard.firstOrNull { it.scheduledCount > 0 }
    val needsLove = leaderboard.lastOrNull { it.scheduledCount > 0 && leaderboard.size > 1 }

    // Week bars for monthly recap
    val weekBars = mutableListOf<Pair<String, Int>>()
    if (!isWeekly) {
      var wStart = startDate
      var wIndex = 1
      while (!wStart.isAfter(endDate)) {
        val wEnd = minOf(endDate, wStart.plusDays(6))
        var wDone = 0
        var wSched = 0
        var c = wStart
        while (!c.isAfter(wEnd)) {
          activeHabitsInPeriod.forEach { h ->
            if (ScheduleMatcher.isScheduledOn(h, c, pausesByHabit[h.id].orEmpty())) {
              val e = entriesByHabit[h.id]?.get(c)
              if (e?.status != EntryStatus.SKIPPED) {
                wSched++
                if (ScheduleMatcher.isEntryDone(h, e)) wDone++
              }
            }
          }
          c = c.plusDays(1)
        }
        val r = if (wSched > 0) ((wDone.toDouble() / wSched) * 100).roundToInt() else 0
        weekBars.add("Week $wIndex" to r)
        wStart = wStart.plusDays(7)
        wIndex++
      }
    }

    val dowRates = dowSched.mapNotNull { (dow, sched) ->
      if (sched > 0) {
        val done = dowDone[dow] ?: 0
        val r = ((done.toDouble() / sched) * 100).roundToInt()
        dow.name.lowercase().replaceFirstChar { it.uppercase() } to r
      } else null
    }
    val bestDow = dowRates.maxByOrNull { it.second }
    val worstDow = dowRates.minByOrNull { it.second }

    // Hourly histogram
    val periodEntries = entries.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) && it.status == EntryStatus.DONE }
    val hourly = MutableList(24) { 0 }
    val zone = ZoneId.systemDefault()
    periodEntries.forEach { e ->
      val h = e.completedAt?.atZone(zone)?.hour ?: 8
      hourly[h] = hourly[h] + 1
    }
    val peakHour = hourly.indices.maxByOrNull { hourly[it] } ?: 8
    val peakHourLabel = String.format("%02d:00", peakHour)

    // Streaks
    var bestStreakInPeriod = 0
    var streaksKept = 0
    var streaksBroken = 0
    activeHabitsInPeriod.forEach { h ->
      val summary = StreakCalculator.calculateStreaks(h, entries.filter { it.habitId == h.id }, pausesByHabit[h.id].orEmpty(), endDate)
      if (summary.currentStreak > 0) streaksKept++ else if (summary.intervals.isNotEmpty()) streaksBroken++
      if (summary.currentStreak > bestStreakInPeriod) bestStreakInPeriod = summary.currentStreak
    }

    val newHabits = habits.count { !it.startDate.isBefore(startDate) && !it.startDate.isAfter(endDate) }

    val pausedDaysTotal = pauses.sumOf { p ->
      val pStart = maxOf(p.startDate, startDate)
      val pEnd = minOf(p.endDate ?: endDate, endDate)
      if (!pStart.isAfter(pEnd)) (java.time.temporal.ChronoUnit.DAYS.between(pStart, pEnd).toInt() + 1) else 0
    }
    val pausedSummary = if (pausedDaysTotal > 0) "Paused for $pausedDaysTotal days." else null

    val insights = mutableListOf<String>()
    if (mostConsistent != null) {
      insights.add("${mostConsistent.habit.name} led the way at ${mostConsistent.completionRate}%.")
    }
    if (perfectDays > 0) {
      insights.add("You completed every scheduled habit on $perfectDays ${if (perfectDays == 1) "day" else "days"}.")
    }
    if (delta != 0) {
      insights.add("Overall completion is ${if (delta > 0) "up $delta%" else "down ${kotlin.math.abs(delta)}%"} compared to the previous period.")
    }
    if (pausedSummary != null) {
      insights.add(pausedSummary)
    }

    val focusSuggestion = when {
      needsLove != null && worstDow != null ->
        "Try keeping ${needsLove.habit.name} on ${worstDow.first}s — your toughest day."
      worstDow != null ->
        "${worstDow.first}s were your toughest day — protect one small habit that day."
      else ->
        "Keep protecting your first morning mark to anchor the day."
    }

    return RecapReport(
      title = if (isWeekly) "This week, *in review.*" else "${startDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}, *in review.*",
      subtitle = "$totalDone of $totalScheduled check-ins",
      isSoFar = isSoFar,
      hasEnoughData = totalScheduled > 0,
      overallRate = overallRate,
      deltaVsPrevious = delta,
      doneCheckIns = totalDone,
      scheduledCheckIns = totalScheduled,
      perfectDays = perfectDays,
      activeDays = activeDays,
      bestStreakInPeriod = bestStreakInPeriod,
      skippedDays = skippedCount,
      newHabitsStarted = newHabits,
      streaksKeptAlive = streaksKept,
      streaksBroken = streaksBroken,
      dayStrip = dayStrip,
      weekBars = weekBars,
      leaderboard = leaderboard,
      mostConsistentHabit = mostConsistent,
      needsLoveHabit = needsLove,
      bestWeekday = bestDow,
      worstWeekday = worstDow,
      bestSingleDay = bestSingleDay,
      peakHourLabel = peakHourLabel,
      hourlyHistogram = hourly,
      pausedSummary = pausedSummary,
      insights = insights,
      nextFocusSuggestion = focusSuggestion
    )
  }

  fun computeYearInReview(
    year: Int,
    habits: List<Habit>,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): YearInReviewReport {
    val yearStart = LocalDate.of(year, 1, 1)
    val rawYearEnd = LocalDate.of(year, 12, 31)
    val yearEnd = if (rawYearEnd.isAfter(today)) today else rawYearEnd
    val isSoFar = rawYearEnd.isAfter(today)

    val yearEntries = entries.filter { it.date.year == year && it.status == EntryStatus.DONE }
    val daysTracked = yearEntries.map { it.date }.distinct().size
    val totalCheckIns = yearEntries.size

    val habitMap = habits.associateBy { it.id }
    val entriesByHabit = entries.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.date } }
    val pausesByHabit = pauses.groupBy { it.habitId }

    val totalMinutes = yearEntries.sumOf { e ->
      val h = habitMap[e.habitId]
      if (h?.trackType == TrackType.TIME) (e.value ?: h.targetValue ?: 0.0).roundToInt() else 0
    }

    val monthlyRates = (1..12).map { m ->
      val ym = YearMonth.of(year, m)
      val mStart = ym.atDay(1)
      val mEnd = minOf(ym.atEndOfMonth(), yearEnd)
      var done = 0
      var sched = 0
      if (!mStart.isAfter(yearEnd)) {
        var d = mStart
        while (!d.isAfter(mEnd)) {
          habits.forEach { h ->
            if (ScheduleMatcher.isScheduledOn(h, d, pausesByHabit[h.id].orEmpty())) {
              val e = entriesByHabit[h.id]?.get(d)
              if (e?.status != EntryStatus.SKIPPED) {
                sched++
                if (ScheduleMatcher.isEntryDone(h, e)) done++
              }
            }
          }
          d = d.plusDays(1)
        }
      }
      val r = if (sched > 0) ((done.toDouble() / sched) * 100).roundToInt() else 0
      ym.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() } to r
    }

    val activeMonths = monthlyRates.filter { it.second > 0 }
    val bestMonth = activeMonths.maxByOrNull { it.second }
    val toughestMonth = activeMonths.minByOrNull { it.second }

    // Daily intensity map + perfect days + perfect run
    val intensityMap = mutableMapOf<LocalDate, Float>()
    var totalYearDone = 0
    var totalYearSched = 0
    var perfectDaysCount = 0
    var currentPerfectRun = 0
    var bestPerfectRun = 0
    val dowDone = mutableMapOf<DayOfWeek, Int>()
    val dowSched = mutableMapOf<DayOfWeek, Int>()

    var d = yearStart
    while (!d.isAfter(yearEnd)) {
      var dayDone = 0
      var daySched = 0
      habits.forEach { h ->
        if (ScheduleMatcher.isScheduledOn(h, d, pausesByHabit[h.id].orEmpty())) {
          val e = entriesByHabit[h.id]?.get(d)
          if (e?.status != EntryStatus.SKIPPED) {
            daySched++
            totalYearSched++
            dowSched[d.dayOfWeek] = (dowSched[d.dayOfWeek] ?: 0) + 1
            if (ScheduleMatcher.isEntryDone(h, e)) {
              dayDone++
              totalYearDone++
              dowDone[d.dayOfWeek] = (dowDone[d.dayOfWeek] ?: 0) + 1
            }
          }
        }
      }
      val ratio = if (daySched > 0) (dayDone.toFloat() / daySched.toFloat()).coerceIn(0f, 1f) else 0f
      intensityMap[d] = ratio
      if (daySched > 0 && dayDone == daySched) {
        perfectDaysCount++
        currentPerfectRun++
        if (currentPerfectRun > bestPerfectRun) bestPerfectRun = currentPerfectRun
      } else if (daySched > 0) {
        currentPerfectRun = 0
      }
      d = d.plusDays(1)
    }

    val overallRate = if (totalYearSched > 0) ((totalYearDone.toDouble() / totalYearSched) * 100).roundToInt() else 0

    // Longest streak of the year
    var longestHabit: Habit? = null
    var longestLen = 0
    habits.forEach { h ->
      val s = StreakCalculator.calculateStreaks(
        h,
        entries.filter { it.habitId == h.id && it.date.year == year },
        pausesByHabit[h.id].orEmpty(),
        yearEnd
      )
      if (s.bestStreak > longestLen) {
        longestLen = s.bestStreak
        longestHabit = h
      }
    }

    // Most consistent & Most improved (H1 vs H2)
    val h1Start = LocalDate.of(year, 1, 1)
    val h1End = LocalDate.of(year, 6, 30)
    val h2Start = LocalDate.of(year, 7, 1)
    val h2End = yearEnd

    val habitItems = habits.map { h ->
      val hMap = entriesByHabit[h.id].orEmpty()
      val hPauses = pausesByHabit[h.id].orEmpty()
      val r = AnalyticsCalculator.computePeriodRate(h, hMap, hPauses, yearStart, yearEnd)
      val doneCount = yearEntries.count { it.habitId == h.id }
      HabitRecapItem(h, r, 0, doneCount, 1)
    }.sortedByDescending { it.completionRate }

    val mostImproved = habits.mapNotNull { h ->
      if (yearEnd.isBefore(h2Start)) return@mapNotNull null
      val hMap = entriesByHabit[h.id].orEmpty()
      val hPauses = pausesByHabit[h.id].orEmpty()
      val r1 = AnalyticsCalculator.computePeriodRate(h, hMap, hPauses, h1Start, h1End)
      val r2 = AnalyticsCalculator.computePeriodRate(h, hMap, hPauses, h2Start, h2End)
      h to (r2 - r1)
    }.maxByOrNull { it.second }

    val weekdayRates = DayOfWeek.entries.map { dow ->
      val s = dowSched[dow] ?: 0
      val dn = dowDone[dow] ?: 0
      val r = if (s > 0) ((dn.toDouble() / s) * 100).roundToInt() else 0
      ScheduleMatcher.dowShortLabel(dow.value) to r
    }
    val bestWeekday = weekdayRates.maxByOrNull { it.second }?.first ?: "Mon"

    val zone = ZoneId.systemDefault()
    val hourly = MutableList(24) { 0 }
    yearEntries.forEach { e ->
      val hr = e.completedAt?.atZone(zone)?.hour ?: 8
      hourly[hr] = hourly[hr] + 1
    }
    val peakHr = hourly.indices.maxByOrNull { hourly[it] } ?: 8
    val peakHourLabel = String.format("%02d:00", peakHr)

    val topHabits = habits.map { h ->
      h to yearEntries.count { it.habitId == h.id }
    }.sortedByDescending { it.second }.take(5)

    return YearInReviewReport(
      year = year,
      isSoFar = isSoFar,
      daysTracked = daysTracked,
      totalCheckIns = totalCheckIns,
      totalMinutesTracked = totalMinutes,
      overallCompletionRate = overallRate,
      deltaVsPreviousYear = null,
      monthlyRates = monthlyRates,
      bestMonth = bestMonth,
      toughestMonth = toughestMonth,
      dailyIntensityMap = intensityMap,
      longestStreakHabit = longestHabit,
      longestStreakLength = longestLen,
      mostConsistentHabit = habitItems.firstOrNull(),
      mostImprovedHabit = mostImproved,
      weekdayRates = weekdayRates,
      bestWeekday = bestWeekday,
      peakHourLabel = peakHourLabel,
      topHabitsByCheckIns = topHabits,
      perfectDaysCount = perfectDaysCount,
      bestPerfectDayRun = bestPerfectRun
    )
  }
}
