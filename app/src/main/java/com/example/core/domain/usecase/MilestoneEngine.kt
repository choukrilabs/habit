package com.example.core.domain.usecase

import com.example.core.domain.model.BadgeEmblemStyle
import com.example.core.domain.model.BadgeTier
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.MilestoneBadge
import com.example.core.domain.model.MilestoneCategory
import com.example.core.domain.model.MilestoneSummary
import com.example.core.domain.model.WeekStartDay
import java.time.LocalDate
import kotlin.math.max

/**
 * Evaluates user streaks, total completions, and weekly/daily consistency to award
 * visual milestone badges and track progress toward upcoming achievements.
 */
object MilestoneEngine {

  const val BADGE_STREAK_3 = "badge_streak_3"
  const val BADGE_STREAK_7 = "badge_streak_7"
  const val BADGE_STREAK_14 = "badge_streak_14"
  const val BADGE_STREAK_30 = "badge_streak_30"
  const val BADGE_STREAK_100 = "badge_streak_100"

  const val BADGE_COMPLETIONS_10 = "badge_completions_10"
  const val BADGE_COMPLETIONS_25 = "badge_completions_25"
  const val BADGE_COMPLETIONS_50 = "badge_completions_50"
  const val BADGE_COMPLETIONS_100 = "badge_completions_100"
  const val BADGE_COMPLETIONS_250 = "badge_completions_250"
  const val BADGE_COMPLETIONS_500 = "badge_completions_500"

  const val BADGE_FIRST_PERFECT_DAY = "badge_consistency_perfect_day"
  const val BADGE_PERFECT_WEEK = "badge_consistency_perfect_week"
  const val BADGE_HIGH_CONSISTENCY_WEEK = "badge_consistency_80_week"
  const val BADGE_MULTI_HABIT_RHYTHM = "badge_consistency_multi_habit"

  fun evaluateGlobalMilestones(
    habits: List<Habit>,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate,
    weekStartDay: WeekStartDay = WeekStartDay.MONDAY
  ): MilestoneSummary {
    val activeHabits = habits.filter { it.status == HabitStatus.ACTIVE }
    val habitsById = habits.associateBy { it.id }
    val entriesByHabit = entries.groupBy { it.habitId }
    val pausesByHabit = pauses.groupBy { it.habitId }

    // 1. Compute best streak & current streak across all habits + overall day run
    var bestHabitStreak = 0
    var bestStreakHabitName: String? = null
    var latestStreakUnlockDate: LocalDate? = null

    habits.forEach { habit ->
      val hEntries = entriesByHabit[habit.id].orEmpty()
      val hPauses = pausesByHabit[habit.id].orEmpty()
      val streakResult = StreakCalculator.calculateStreaks(habit, hEntries, hPauses, today)
      val peakForHabit = max(streakResult.currentStreak, streakResult.bestStreak)
      if (peakForHabit > bestHabitStreak) {
        bestHabitStreak = peakForHabit
        bestStreakHabitName = habit.name
        latestStreakUnlockDate = hEntries
          .filter { ScheduleMatcher.isEntryDone(habit, it) }
          .maxByOrNull { it.date }?.date
      }
    }

    val overallDayRun = StreakCalculator.calculateOverallDayRun(entries, today)
    val effectiveBestStreak = max(bestHabitStreak, overallDayRun)

    // 2. Compute total completed check-ins across the database
    val completedEntries = entries
      .filter { entry ->
        val h = habitsById[entry.habitId]
        if (h != null) {
          ScheduleMatcher.isEntryDone(h, entry)
        } else {
          entry.status == EntryStatus.DONE
        }
      }
      .sortedBy { it.date }

    val totalCompletions = completedEntries.size

    fun completionUnlockDateFor(target: Int): LocalDate? {
      return if (totalCompletions >= target && target > 0) {
        completedEntries.getOrNull(target - 1)?.date ?: today
      } else null
    }

    // 3. Compute consistency & perfect-week metrics
    val weeklySummary = WeeklyPerformanceCalculator.calculate(
      habits = activeHabits,
      entries = entries,
      pauses = pauses,
      today = today,
      weekStartDay = weekStartDay
    )

    val perfectDaysThisWeek = weeklySummary.perfectDaysCount
    val hasPerfectWeek = weeklySummary.scheduledCount >= 7 &&
      weeklySummary.completionPercentage == 100 &&
      perfectDaysThisWeek >= 7

    val activeHabitsWith3DayStreakCount = activeHabits.count { h ->
      val sr = StreakCalculator.calculateStreaks(
        h,
        entriesByHabit[h.id].orEmpty(),
        pausesByHabit[h.id].orEmpty(),
        today
      )
      max(sr.currentStreak, sr.bestStreak) >= 3
    }

    val badges = listOf(
      // --- STREAK MILESTONES ---
      MilestoneBadge(
        id = BADGE_STREAK_3,
        title = "3-Day Spark",
        subtitle = "3-day streak",
        description = "Maintain a 3-day consecutive streak on any habit.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.BRONZE,
        emblemStyle = BadgeEmblemStyle.FLAME,
        targetValue = 3,
        currentValue = effectiveBestStreak.coerceAtMost(3),
        unitLabel = "days",
        isUnlocked = effectiveBestStreak >= 3,
        unlockedDate = if (effectiveBestStreak >= 3) (latestStreakUnlockDate ?: today) else null,
        habitName = bestStreakHabitName
      ),
      MilestoneBadge(
        id = BADGE_STREAK_7,
        title = "7-Day Streak",
        subtitle = "Full week on fire",
        description = "Reach a 7-day streak on a habit to lock in your weekly rhythm.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.SILVER,
        emblemStyle = BadgeEmblemStyle.FLAME,
        targetValue = 7,
        currentValue = effectiveBestStreak.coerceAtMost(7),
        unitLabel = "days",
        isUnlocked = effectiveBestStreak >= 7,
        unlockedDate = if (effectiveBestStreak >= 7) (latestStreakUnlockDate ?: today) else null,
        habitName = bestStreakHabitName
      ),
      MilestoneBadge(
        id = BADGE_STREAK_14,
        title = "14-Day Fortnight",
        subtitle = "Two weeks strong",
        description = "Sustain a 14-day streak without breaking your chain.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.GOLD,
        emblemStyle = BadgeEmblemStyle.SHIELD,
        targetValue = 14,
        currentValue = effectiveBestStreak.coerceAtMost(14),
        unitLabel = "days",
        isUnlocked = effectiveBestStreak >= 14,
        unlockedDate = if (effectiveBestStreak >= 14) (latestStreakUnlockDate ?: today) else null,
        habitName = bestStreakHabitName
      ),
      MilestoneBadge(
        id = BADGE_STREAK_30,
        title = "30-Day Mastery",
        subtitle = "Monthly pillar",
        description = "Complete 30 consecutive scheduled days on any habit.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.GOLD,
        emblemStyle = BadgeEmblemStyle.CROWN,
        targetValue = 30,
        currentValue = effectiveBestStreak.coerceAtMost(30),
        unitLabel = "days",
        isUnlocked = effectiveBestStreak >= 30,
        unlockedDate = if (effectiveBestStreak >= 30) (latestStreakUnlockDate ?: today) else null,
        habitName = bestStreakHabitName
      ),
      MilestoneBadge(
        id = BADGE_STREAK_100,
        title = "100-Day Legend",
        subtitle = "Unbreakable chain",
        description = "Reach a monumental 100-day streak.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.PRISMATIC,
        emblemStyle = BadgeEmblemStyle.DIAMOND,
        targetValue = 100,
        currentValue = effectiveBestStreak.coerceAtMost(100),
        unitLabel = "days",
        isUnlocked = effectiveBestStreak >= 100,
        unlockedDate = if (effectiveBestStreak >= 100) (latestStreakUnlockDate ?: today) else null,
        habitName = bestStreakHabitName
      ),

      // --- TOTAL COMPLETION MILESTONES ---
      MilestoneBadge(
        id = BADGE_COMPLETIONS_10,
        title = "First 10 Marks",
        subtitle = "10 completions total",
        description = "Log 10 habit completions across your routine.",
        category = MilestoneCategory.TOTAL_COMPLETIONS,
        tier = BadgeTier.BRONZE,
        emblemStyle = BadgeEmblemStyle.STAR,
        targetValue = 10,
        currentValue = totalCompletions.coerceAtMost(10),
        unitLabel = "completions",
        isUnlocked = totalCompletions >= 10,
        unlockedDate = completionUnlockDateFor(10)
      ),
      MilestoneBadge(
        id = BADGE_COMPLETIONS_25,
        title = "Quarter Century",
        subtitle = "25 completions total",
        description = "Log 25 total habit completions.",
        category = MilestoneCategory.TOTAL_COMPLETIONS,
        tier = BadgeTier.SILVER,
        emblemStyle = BadgeEmblemStyle.STAR,
        targetValue = 25,
        currentValue = totalCompletions.coerceAtMost(25),
        unitLabel = "completions",
        isUnlocked = totalCompletions >= 25,
        unlockedDate = completionUnlockDateFor(25)
      ),
      MilestoneBadge(
        id = BADGE_COMPLETIONS_50,
        title = "50 Marks Club",
        subtitle = "50 completions total",
        description = "Reach 50 total logged habit completions.",
        category = MilestoneCategory.TOTAL_COMPLETIONS,
        tier = BadgeTier.SILVER,
        emblemStyle = BadgeEmblemStyle.SHIELD,
        targetValue = 50,
        currentValue = totalCompletions.coerceAtMost(50),
        unitLabel = "completions",
        isUnlocked = totalCompletions >= 50,
        unlockedDate = completionUnlockDateFor(50)
      ),
      MilestoneBadge(
        id = BADGE_COMPLETIONS_100,
        title = "Century Club",
        subtitle = "100 completions total",
        description = "Complete 100 total habit check-ins across your habits.",
        category = MilestoneCategory.TOTAL_COMPLETIONS,
        tier = BadgeTier.GOLD,
        emblemStyle = BadgeEmblemStyle.CROWN,
        targetValue = 100,
        currentValue = totalCompletions.coerceAtMost(100),
        unitLabel = "completions",
        isUnlocked = totalCompletions >= 100,
        unlockedDate = completionUnlockDateFor(100)
      ),
      MilestoneBadge(
        id = BADGE_COMPLETIONS_250,
        title = "250 Milestone",
        subtitle = "250 completions total",
        description = "Accumulate 250 total habit completions.",
        category = MilestoneCategory.TOTAL_COMPLETIONS,
        tier = BadgeTier.PRISMATIC,
        emblemStyle = BadgeEmblemStyle.LAUREL,
        targetValue = 250,
        currentValue = totalCompletions.coerceAtMost(250),
        unitLabel = "completions",
        isUnlocked = totalCompletions >= 250,
        unlockedDate = completionUnlockDateFor(250)
      ),
      MilestoneBadge(
        id = BADGE_COMPLETIONS_500,
        title = "500 Apex",
        subtitle = "500 completions total",
        description = "Reach 500 total habit completions — true craft and dedication.",
        category = MilestoneCategory.TOTAL_COMPLETIONS,
        tier = BadgeTier.PRISMATIC,
        emblemStyle = BadgeEmblemStyle.DIAMOND,
        targetValue = 500,
        currentValue = totalCompletions.coerceAtMost(500),
        unitLabel = "completions",
        isUnlocked = totalCompletions >= 500,
        unlockedDate = completionUnlockDateFor(500)
      ),

      // --- PERFECT WEEK & CONSISTENCY ACHIEVEMENTS ---
      MilestoneBadge(
        id = BADGE_FIRST_PERFECT_DAY,
        title = "Clean Sweep",
        subtitle = "First perfect day",
        description = "Complete 100% of your scheduled habits on a single day.",
        category = MilestoneCategory.CONSISTENCY,
        tier = BadgeTier.BRONZE,
        emblemStyle = BadgeEmblemStyle.LAUREL,
        targetValue = 1,
        currentValue = perfectDaysThisWeek.coerceAtMost(1),
        unitLabel = "perfect day",
        isUnlocked = perfectDaysThisWeek >= 1,
        unlockedDate = if (perfectDaysThisWeek >= 1) today else null
      ),
      MilestoneBadge(
        id = BADGE_HIGH_CONSISTENCY_WEEK,
        title = "Strong Rhythm",
        subtitle = "80%+ weekly rate",
        description = "Maintain at least an 80% completion rate across the current week.",
        category = MilestoneCategory.CONSISTENCY,
        tier = BadgeTier.SILVER,
        emblemStyle = BadgeEmblemStyle.SHIELD,
        targetValue = 80,
        currentValue = weeklySummary.completionPercentage.coerceAtMost(80),
        unitLabel = "% rate",
        isUnlocked = weeklySummary.scheduledCount > 0 && weeklySummary.completionPercentage >= 80,
        unlockedDate = if (weeklySummary.scheduledCount > 0 && weeklySummary.completionPercentage >= 80) today else null
      ),
      MilestoneBadge(
        id = BADGE_PERFECT_WEEK,
        title = "Perfect Week",
        subtitle = "7 flawless days",
        description = "Achieve 7 perfect days in a single week with every scheduled habit completed.",
        category = MilestoneCategory.CONSISTENCY,
        tier = BadgeTier.GOLD,
        emblemStyle = BadgeEmblemStyle.CROWN,
        targetValue = 7,
        currentValue = if (hasPerfectWeek) 7 else perfectDaysThisWeek.coerceAtMost(7),
        unitLabel = "perfect days",
        isUnlocked = hasPerfectWeek || perfectDaysThisWeek >= 7,
        unlockedDate = if (hasPerfectWeek || perfectDaysThisWeek >= 7) today else null
      ),
      MilestoneBadge(
        id = BADGE_MULTI_HABIT_RHYTHM,
        title = "Multi-Habit Harmony",
        subtitle = "3 habits at 3+ day streak",
        description = "Build a 3-day or longer streak on at least 3 active habits.",
        category = MilestoneCategory.CONSISTENCY,
        tier = BadgeTier.GOLD,
        emblemStyle = BadgeEmblemStyle.LAUREL,
        targetValue = 3,
        currentValue = activeHabitsWith3DayStreakCount.coerceAtMost(3),
        unitLabel = "habits",
        isUnlocked = activeHabitsWith3DayStreakCount >= 3,
        unlockedDate = if (activeHabitsWith3DayStreakCount >= 3) today else null
      )
    )

    val unlocked = badges.filter { it.isUnlocked }
    val locked = badges.filter { !it.isUnlocked }
    val nextUp = locked.maxByOrNull { it.progressFraction }

    return MilestoneSummary(
      allBadges = badges,
      unlockedBadges = unlocked,
      lockedBadges = locked,
      nextUpBadge = nextUp,
      unlockedCount = unlocked.size,
      totalCount = badges.size,
      bestStreakOverall = effectiveBestStreak,
      totalCompletionsOverall = totalCompletions,
      perfectDaysThisWeek = perfectDaysThisWeek
    )
  }

  /**
   * Evaluates milestone badges earned specifically by a single [habit] for the Habit Detail shelf.
   */
  fun evaluateHabitMilestones(
    habit: Habit,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): List<MilestoneBadge> {
    val streakResult = StreakCalculator.calculateStreaks(habit, entries, pauses, today)
    val bestStreak = max(streakResult.currentStreak, streakResult.bestStreak)
    val doneEntries = entries
      .filter { ScheduleMatcher.isEntryDone(habit, it) }
      .sortedBy { it.date }
    val totalDone = doneEntries.size
    val latestDoneDate = doneEntries.lastOrNull()?.date

    return listOf(
      MilestoneBadge(
        id = "${habit.id}_streak_3",
        title = "3-Day Spark",
        subtitle = "3d streak",
        description = "Reach a 3-day streak on ${habit.name}.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.BRONZE,
        emblemStyle = BadgeEmblemStyle.FLAME,
        targetValue = 3,
        currentValue = bestStreak.coerceAtMost(3),
        unitLabel = "days",
        isUnlocked = bestStreak >= 3,
        unlockedDate = if (bestStreak >= 3) latestDoneDate else null,
        habitId = habit.id,
        habitName = habit.name
      ),
      MilestoneBadge(
        id = "${habit.id}_streak_7",
        title = "7-Day Streak",
        subtitle = "7d streak",
        description = "Reach a 7-day streak on ${habit.name}.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.SILVER,
        emblemStyle = BadgeEmblemStyle.FLAME,
        targetValue = 7,
        currentValue = bestStreak.coerceAtMost(7),
        unitLabel = "days",
        isUnlocked = bestStreak >= 7,
        unlockedDate = if (bestStreak >= 7) latestDoneDate else null,
        habitId = habit.id,
        habitName = habit.name
      ),
      MilestoneBadge(
        id = "${habit.id}_streak_14",
        title = "14-Day Shield",
        subtitle = "14d streak",
        description = "Reach a 14-day streak on ${habit.name}.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.GOLD,
        emblemStyle = BadgeEmblemStyle.SHIELD,
        targetValue = 14,
        currentValue = bestStreak.coerceAtMost(14),
        unitLabel = "days",
        isUnlocked = bestStreak >= 14,
        unlockedDate = if (bestStreak >= 14) latestDoneDate else null,
        habitId = habit.id,
        habitName = habit.name
      ),
      MilestoneBadge(
        id = "${habit.id}_streak_30",
        title = "30-Day Mastery",
        subtitle = "30d streak",
        description = "Reach a 30-day streak on ${habit.name}.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.GOLD,
        emblemStyle = BadgeEmblemStyle.CROWN,
        targetValue = 30,
        currentValue = bestStreak.coerceAtMost(30),
        unitLabel = "days",
        isUnlocked = bestStreak >= 30,
        unlockedDate = if (bestStreak >= 30) latestDoneDate else null,
        habitId = habit.id,
        habitName = habit.name
      ),
      MilestoneBadge(
        id = "${habit.id}_streak_100",
        title = "100-Day Legend",
        subtitle = "100d streak",
        description = "Reach a 100-day streak on ${habit.name}.",
        category = MilestoneCategory.STREAK,
        tier = BadgeTier.PRISMATIC,
        emblemStyle = BadgeEmblemStyle.DIAMOND,
        targetValue = 100,
        currentValue = bestStreak.coerceAtMost(100),
        unitLabel = "days",
        isUnlocked = bestStreak >= 100,
        unlockedDate = if (bestStreak >= 100) latestDoneDate else null,
        habitId = habit.id,
        habitName = habit.name
      ),
      MilestoneBadge(
        id = "${habit.id}_comp_10",
        title = "10 Check-Ins",
        subtitle = "10 total",
        description = "Complete ${habit.name} 10 times.",
        category = MilestoneCategory.TOTAL_COMPLETIONS,
        tier = BadgeTier.BRONZE,
        emblemStyle = BadgeEmblemStyle.STAR,
        targetValue = 10,
        currentValue = totalDone.coerceAtMost(10),
        unitLabel = "completions",
        isUnlocked = totalDone >= 10,
        unlockedDate = if (totalDone >= 10) doneEntries.getOrNull(9)?.date else null,
        habitId = habit.id,
        habitName = habit.name
      ),
      MilestoneBadge(
        id = "${habit.id}_comp_50",
        title = "50 Check-Ins",
        subtitle = "50 total",
        description = "Complete ${habit.name} 50 times.",
        category = MilestoneCategory.TOTAL_COMPLETIONS,
        tier = BadgeTier.SILVER,
        emblemStyle = BadgeEmblemStyle.LAUREL,
        targetValue = 50,
        currentValue = totalDone.coerceAtMost(50),
        unitLabel = "completions",
        isUnlocked = totalDone >= 50,
        unlockedDate = if (totalDone >= 50) doneEntries.getOrNull(49)?.date else null,
        habitId = habit.id,
        habitName = habit.name
      ),
      MilestoneBadge(
        id = "${habit.id}_comp_100",
        title = "100 Completions",
        subtitle = "100 total",
        description = "Complete ${habit.name} 100 times.",
        category = MilestoneCategory.TOTAL_COMPLETIONS,
        tier = BadgeTier.GOLD,
        emblemStyle = BadgeEmblemStyle.CROWN,
        targetValue = 100,
        currentValue = totalDone.coerceAtMost(100),
        unitLabel = "completions",
        isUnlocked = totalDone >= 100,
        unlockedDate = if (totalDone >= 100) doneEntries.getOrNull(99)?.date else null,
        habitId = habit.id,
        habitName = habit.name
      )
    )
  }

  /**
   * Returns any newly unlocked badges by comparing [previouslyUnlockedIds] with [updatedSummary].
   * Prioritizes higher-tier badges first so the celebration modal showcases the most prestigious unlock.
   */
  fun detectNewlyUnlockedBadges(
    previouslyUnlockedIds: Set<String>,
    updatedSummary: MilestoneSummary
  ): List<MilestoneBadge> {
    return updatedSummary.unlockedBadges
      .filter { it.id !in previouslyUnlockedIds }
      .sortedByDescending { it.tier.ordinal * 1000 + it.targetValue }
  }
}
