package com.example.feature.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.CardDensity
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitCompletionLog
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HeadlineMode
import com.example.core.domain.model.MilestoneSummary
import com.example.core.domain.model.TodayLayout
import com.example.core.domain.model.TrackType
import com.example.core.domain.model.WeeklyPerformanceSummary
import com.example.core.domain.usecase.AnalyticsCalculator
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.MilestoneEngine
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.domain.usecase.StreakCalculator
import com.example.core.domain.usecase.WeeklyPerformanceCalculator
import com.example.core.ui.components.CardHairline
import com.example.core.ui.components.HabitCheckCircle
import com.example.core.ui.components.HabitIconView
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.components.MainTabHeader
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.components.SerifHeadlineText
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.example.core.ui.theme.habitTint
import com.example.core.ui.theme.parseHexColor
import com.example.feature.habit.HabitCompletionLogBottomSheet
import com.example.feature.milestones.TodayMilestoneShelfCard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun TodayTabScreen(
  habits: List<Habit>,
  entriesByHabit: Map<String, Map<LocalDate, Entry>>,
  entriesListByHabit: Map<String, List<Entry>>,
  pausesByHabit: Map<String, List<HabitPause>>,
  settings: AppSettings,
  today: LocalDate,
  onOpenMenu: () -> Unit,
  onNewHabit: () -> Unit,
  onOpenHabitDetail: (String) -> Unit,
  onToggleHabitToday: (Habit, String?) -> Unit,
  onLogQuantitativeToday: (Habit, Double, String?) -> Unit,
  onSkipToday: (Habit) -> Unit,
  onEditHabit: (Habit) -> Unit,
  onPauseHabitPrompt: (Habit) -> Unit,
  onResumeHabit: (Habit) -> Unit,
  onArchiveHabit: (Habit) -> Unit,
  onReorderHabits: () -> Unit,
  onDeleteHabit: (Habit) -> Unit,
  onSelectGroupFilter: (String?) -> Unit,
  onBrowseIdeas: () -> Unit,
  onDismissCoachmark: () -> Unit,
  onLogCompletion: (HabitCompletionLog) -> Unit = {},
  onClearDateEntry: (Habit, LocalDate) -> Unit = { _, _ -> },
  weeklySummary: WeeklyPerformanceSummary? = null,
  onOpenAnalytics: (() -> Unit)? = null,
  milestoneSummary: MilestoneSummary? = null,
  onOpenMilestonesBadges: (() -> Unit)? = null
) {
  val colors = LocalHabitRadarColors.current

  val pausedTodayHabits = remember(habits, pausesByHabit, today) {
    habits.filter { ScheduleMatcher.isPausedOn(today, pausesByHabit[it.id].orEmpty()) }
  }
  val unpausedHabits = remember(habits, pausesByHabit, today) {
    habits.filter { !ScheduleMatcher.isPausedOn(today, pausesByHabit[it.id].orEmpty()) }
  }
  val scheduledTodayAll = remember(unpausedHabits, pausesByHabit, today) {
    unpausedHabits.filter { ScheduleMatcher.isScheduledOn(it, today, pausesByHabit[it.id].orEmpty()) }
  }

  val availableGroups = remember(scheduledTodayAll) {
    scheduledTodayAll
      .flatMap { it.allOrganizationLabels }
      .distinctBy { it.lowercase() }
  }

  val filteredHabits = remember(
    scheduledTodayAll,
    settings.groupFilter,
    settings.groupSections,
    settings.hideCompleted,
    entriesByHabit,
    today
  ) {
    scheduledTodayAll.filter { h ->
      val groupMatch = settings.groupSections || settings.groupFilter == null || h.matchesCategoryOrTag(settings.groupFilter)
      val isDone = ScheduleMatcher.isEntryDone(h, entriesByHabit[h.id]?.get(today))
      val hideMatch = !settings.hideCompleted || !isDone
      groupMatch && hideMatch
    }
  }

  val totalScheduledCount = scheduledTodayAll.size
  val doneScheduledCount = scheduledTodayAll.count { h ->
    ScheduleMatcher.isEntryDone(h, entriesByHabit[h.id]?.get(today))
  }
  val remainingCount = (totalScheduledCount - doneScheduledCount).coerceAtLeast(0)
  val completionPercent = if (totalScheduledCount > 0) ((doneScheduledCount.toDouble() / totalScheduledCount) * 100).roundToInt() else 0

  val allEntriesFlat = remember(entriesListByHabit) { entriesListByHabit.values.flatten() }
  val allPausesFlat = remember(pausesByHabit) { pausesByHabit.values.flatten() }
  val dayRun = remember(allEntriesFlat, today) { StreakCalculator.calculateOverallDayRun(allEntriesFlat, today) }

  val resolvedWeeklySummary = remember(weeklySummary, habits, allEntriesFlat, allPausesFlat, today, settings.weekStart) {
    weeklySummary ?: WeeklyPerformanceCalculator.calculate(
      habits = habits,
      entries = allEntriesFlat,
      pauses = allPausesFlat,
      today = today,
      weekStartDay = settings.weekStart
    )
  }

  val resolvedMilestoneSummary = remember(milestoneSummary, habits, allEntriesFlat, allPausesFlat, today, settings.weekStart) {
    milestoneSummary ?: MilestoneEngine.evaluateGlobalMilestones(
      habits = habits,
      entries = allEntriesFlat,
      pauses = allPausesFlat,
      today = today,
      weekStartDay = settings.weekStart
    )
  }

  val greetingHeadline = remember(totalScheduledCount, doneScheduledCount, completionPercent) {
    when {
      totalScheduledCount > 0 && doneScheduledCount == totalScheduledCount -> "All *done.*"
      completionPercent >= 75 -> "Almost *there.*"
      else -> "Good to *see you.*"
    }
  }

  val dateHeaderStr = remember(today, settings.showHijri) {
    LogicalDateCalculator.formatHeaderDate(today, settings.showHijri)
  }

  var activeQuantitativeHabit by remember { mutableStateOf<Habit?>(null) }
  var activeNotePromptHabit by remember { mutableStateOf<Habit?>(null) }
  var activeCompletionLogHabit by remember { mutableStateOf<Habit?>(null) }
  var oneThingFocusIndex by remember { mutableIntStateOf(0) }
  var pausedExpanded by remember { mutableStateOf(false) }

  val firstIncompleteId = remember(filteredHabits, entriesByHabit, today) {
    filteredHabits.firstOrNull { h ->
      val e = entriesByHabit[h.id]?.get(today)
      !ScheduleMatcher.isEntryDone(h, e) && e?.status != EntryStatus.SKIPPED
    }?.id
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    MainTabHeader(
      dateLabel = dateHeaderStr,
      headlineText = if (settings.showGreeting) greetingHeadline else null,
      onOpenMenu = onOpenMenu,
      onNewHabit = onNewHabit
    ) {
      if (settings.showDaySignalBand && totalScheduledCount > 0) {
        Spacer(modifier = Modifier.height(14.dp))
        // Day-signal band: one pill segment per habit scheduled today
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("day_signal_band")
        ) {
          for (i in 0 until totalScheduledCount) {
            val filled = i < doneScheduledCount
            Box(
              modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (filled) colors.accent else Color(0xFF3A3B42))
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val caption = if (remainingCount == 0 && totalScheduledCount > 0) {
          "All $totalScheduledCount done · well done"
        } else {
          when (settings.headline) {
            HeadlineMode.COUNT -> "$doneScheduledCount of $totalScheduledCount · $remainingCount small marks left"
            HeadlineMode.PERCENT -> "$completionPercent% · $remainingCount small marks left"
            HeadlineMode.DAYS -> "$dayRun-day run · $remainingCount small marks left"
          }
        }
        Text(
          text = caption,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          color = colors.textSecondary
        )
      }

      // Group filter chips (when at least one habit has a Group and groupSections is OFF)
      if (!settings.groupSections && availableGroups.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            val isAll = settings.groupFilter == null
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (isAll) colors.accentContainer else colors.surface2)
                .clickable { onSelectGroupFilter(null) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Text(
                text = "All",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isAll) colors.accent else colors.textSecondary
              )
            }
          }
          items(availableGroups) { g ->
            val sel = settings.groupFilter.equals(g, ignoreCase = true)
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (sel) colors.accentContainer else colors.surface2)
                .clickable { onSelectGroupFilter(if (sel) null else g) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Text(
                text = g,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (sel) colors.accent else colors.textSecondary
              )
            }
          }
        }
      }
    }

    // One-time coachmark banner after onboarding
    AnimatedVisibility(visible = !settings.coachmarkShown && habits.isNotEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(colors.accentContainer)
          .clickable { onDismissCoachmark() }
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        Text(
          text = "Tap the circle to check · Tap a card for analytics · ≡ for the menu.",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          color = colors.accent
        )
      }
    }

    if (scheduledTodayAll.isEmpty() && pausedTodayHabits.isEmpty()) {
      // Show weekly summary card at the top when enabled in Home Studio
      if (settings.showWeeklyPerformanceCard) {
        WeeklyPerformanceSummaryCard(
          summary = resolvedWeeklySummary,
          onOpenAnalytics = onOpenAnalytics,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
      }
      if (settings.showMilestoneBadgesShelf) {
        TodayMilestoneShelfCard(
          milestoneSummary = resolvedMilestoneSummary,
          onOpenBadgesGallery = { onOpenMilestonesBadges?.invoke() },
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
      }
      // Empty State
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp)
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          SerifHeadlineText(
            rawText = "Start with one *small mark.*",
            fontSizeSp = 30,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Build calm daily momentum — private and 100% offline.",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 14.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(20.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(colors.accent)
                .clickable(onClick = onNewHabit)
                .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
              Text(
                text = "+ New habit",
                fontWeight = FontWeight.Bold,
                color = colors.onAccent
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface2)
                .clickable(onClick = onBrowseIdeas)
                .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
              Text(
                text = "Browse ideas",
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
              )
            }
          }
        }
      }
    } else {
      val triggerHabitAction: (Habit) -> Unit = { habit ->
        if (habit.trackType == TrackType.TASK) {
          val existingEntry = entriesByHabit[habit.id]?.get(today)
          val alreadyDone = ScheduleMatcher.isEntryDone(habit, existingEntry)
          if (!alreadyDone && habit.askNote) {
            activeNotePromptHabit = habit
          } else {
            onToggleHabitToday(habit, null)
          }
        } else {
          activeQuantitativeHabit = habit
        }
      }

      LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        if (settings.showWeeklyPerformanceCard) {
          item(key = "weekly_performance_summary_card_item") {
            WeeklyPerformanceSummaryCard(
              summary = resolvedWeeklySummary,
              onOpenAnalytics = onOpenAnalytics
            )
          }
        }

        if (settings.showMilestoneBadgesShelf) {
          item(key = "today_milestone_shelf_card_item") {
            TodayMilestoneShelfCard(
              milestoneSummary = resolvedMilestoneSummary,
              onOpenBadgesGallery = { onOpenMilestonesBadges?.invoke() }
            )
          }
        }

        when (settings.todayLayout) {
          TodayLayout.CLASSIC -> {
            if (settings.groupSections && availableGroups.isNotEmpty()) {
              val grouped = filteredHabits.groupBy { it.resolvedCategory ?: "General" }
              grouped.forEach { (grpName, grpHabits) ->
                item(key = "header_$grpName") {
                  SectionLabel(grpName, modifier = Modifier.padding(top = 6.dp))
                }
                items(grpHabits, key = { it.id }) { habit ->
                  ClassicHabitCardRow(
                    habit = habit,
                    entry = entriesByHabit[habit.id]?.get(today),
                    streak = StreakCalculator.calculateStreaks(
                      habit,
                      entriesListByHabit[habit.id].orEmpty(),
                      pausesByHabit[habit.id].orEmpty(),
                      today
                    ).currentStreak,
                    isNextUp = habit.id == firstIncompleteId,
                    density = settings.cardDensity,
                    showStreak = settings.showStreakToday,
                    onCardClick = { onOpenHabitDetail(habit.id) },
                    onCircleClick = { triggerHabitAction(habit) },
                    onOpenCompletionLogger = { activeCompletionLogHabit = habit },
                    onEdit = { onEditHabit(habit) },
                    onSkip = { onSkipToday(habit) },
                    onPause = { onPauseHabitPrompt(habit) },
                    onArchive = { onArchiveHabit(habit) },
                    onReorder = onReorderHabits,
                    onDelete = { onDeleteHabit(habit) }
                  )
                }
              }
            } else {
              items(filteredHabits, key = { it.id }) { habit ->
                ClassicHabitCardRow(
                  habit = habit,
                  entry = entriesByHabit[habit.id]?.get(today),
                  streak = StreakCalculator.calculateStreaks(
                    habit,
                    entriesListByHabit[habit.id].orEmpty(),
                    pausesByHabit[habit.id].orEmpty(),
                    today
                  ).currentStreak,
                  isNextUp = habit.id == firstIncompleteId,
                  density = settings.cardDensity,
                  showStreak = settings.showStreakToday,
                  onCardClick = { onOpenHabitDetail(habit.id) },
                  onCircleClick = { triggerHabitAction(habit) },
                  onOpenCompletionLogger = { activeCompletionLogHabit = habit },
                  onEdit = { onEditHabit(habit) },
                  onSkip = { onSkipToday(habit) },
                  onPause = { onPauseHabitPrompt(habit) },
                  onArchive = { onArchiveHabit(habit) },
                  onReorder = onReorderHabits,
                  onDelete = { onDeleteHabit(habit) }
                )
              }
            }
          }
          TodayLayout.ONE_THING -> {
            val incompleteList = filteredHabits.filter {
              !ScheduleMatcher.isEntryDone(it, entriesByHabit[it.id]?.get(today))
            }.ifEmpty { filteredHabits }

            if (incompleteList.isNotEmpty()) {
              val safeIdx = oneThingFocusIndex.coerceIn(0, incompleteList.lastIndex)
              val focusHabit = incompleteList[safeIdx]
              val focusTint = habitTint(focusHabit.colorKey, colors.isDark)
              val focusDone = ScheduleMatcher.isEntryDone(focusHabit, entriesByHabit[focusHabit.id]?.get(today))
              val focusStreak = StreakCalculator.calculateStreaks(
                focusHabit,
                entriesListByHabit[focusHabit.id].orEmpty(),
                pausesByHabit[focusHabit.id].orEmpty(),
                today
              ).currentStreak

              item(key = "one_thing_focus") {
                HabitRadarCard(
                  backgroundColor = focusTint.cardBackground,
                  borderColor = colors.accent.copy(alpha = 0.6f),
                  onClick = { onOpenHabitDetail(focusHabit.id) }
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 12.dp)
                  ) {
                    Row(
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      IconButton(
                        onClick = {
                          oneThingFocusIndex = (safeIdx - 1 + incompleteList.size) % incompleteList.size
                        }
                      ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous", tint = colors.textSecondary)
                      }
                      Text(
                        text = "ONE THING · ${safeIdx + 1} OF ${incompleteList.size}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                      )
                      IconButton(
                        onClick = {
                          oneThingFocusIndex = (safeIdx + 1) % incompleteList.size
                        }
                      ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = colors.textSecondary)
                      }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(focusTint.iconTileBackground)
                    ) {
                      HabitIconView(
                        iconKey = focusHabit.iconKey,
                        tint = focusTint.baseColor,
                        modifier = Modifier.size(42.dp),
                        emojiSize = 34.sp
                      )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                      text = focusHabit.name,
                      fontFamily = InstrumentSerifFamily,
                      fontSize = 30.sp,
                      color = colors.textPrimary,
                      textAlign = TextAlign.Center
                    )
                    if (settings.showStreakToday) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = "🔥 $focusStreak ${if (focusStreak == 1) "Day" else "Days"}",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                      )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    HabitCheckCircle(
                      isDone = focusDone,
                      isSkipped = entriesByHabit[focusHabit.id]?.get(today)?.status == EntryStatus.SKIPPED,
                      colorHex = focusHabit.colorKey,
                      size = 76.dp,
                      onClick = { triggerHabitAction(focusHabit) }
                    )
                  }
                }
              }

              // Next two thin placeholders below
              val nextTwo = incompleteList.drop(safeIdx + 1).take(2)
              items(nextTwo, key = { "preview_${it.id}" }) { nextHabit ->
                HabitRadarCard(
                  backgroundColor = colors.card,
                  contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                  onClick = { onOpenHabitDetail(nextHabit.id) }
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = nextHabit.name,
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 15.sp,
                      color = colors.textSecondary
                    )
                    Text(
                      text = "Up next",
                      fontSize = 12.sp,
                      color = colors.textSecondary.copy(alpha = 0.6f)
                    )
                  }
                }
              }
            }
          }
          TodayLayout.LEDGER -> {
            item(key = "ledger_container") {
              HabitRadarCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                filteredHabits.forEachIndexed { idx, habit ->
                  val entry = entriesByHabit[habit.id]?.get(today)
                  val isDone = ScheduleMatcher.isEntryDone(habit, entry)
                  val streak = StreakCalculator.calculateStreaks(
                    habit,
                    entriesListByHabit[habit.id].orEmpty(),
                    pausesByHabit[habit.id].orEmpty(),
                    today
                  ).currentStreak

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { onOpenHabitDetail(habit.id) }
                      .padding(horizontal = 16.dp, vertical = 12.dp)
                  ) {
                    Text(
                      text = habit.name,
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Medium,
                      color = if (isDone) colors.textSecondary else colors.textPrimary,
                      textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                      modifier = Modifier.weight(1f)
                    )
                    if (settings.showStreakToday) {
                      Text(
                        text = "🔥 $streak",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(end = 12.dp)
                      )
                    }
                    HabitCheckCircle(
                      isDone = isDone,
                      isSkipped = entry?.status == EntryStatus.SKIPPED,
                      colorHex = habit.colorKey,
                      size = 36.dp,
                      onClick = { triggerHabitAction(habit) }
                    )
                  }
                  if (idx < filteredHabits.lastIndex) {
                    CardHairline()
                  }
                }
              }
            }
          }
        }

        // Paused (N) collapsed row at the bottom of Today
        if (pausedTodayHabits.isNotEmpty()) {
          item(key = "paused_section") {
            HabitRadarCard(
              backgroundColor = colors.card.copy(alpha = 0.7f),
              contentPadding = PaddingValues(16.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { pausedExpanded = !pausedExpanded }
              ) {
                Text(
                  text = "Paused (${pausedTodayHabits.size})",
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Medium,
                  color = colors.textSecondary
                )
                Icon(
                  imageVector = if (pausedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                  contentDescription = "Expand paused habits",
                  tint = colors.textSecondary
                )
              }
              if (pausedExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                pausedTodayHabits.forEach { ph ->
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 6.dp)
                  ) {
                    Text(
                      text = ph.name,
                      color = colors.textPrimary,
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 15.sp,
                      modifier = Modifier.weight(1f)
                    )
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accentContainer)
                        .clickable { onResumeHabit(ph) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                      Text(
                        text = "Resume",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Quantitative Stepper / Timer Sheet
  if (activeQuantitativeHabit != null) {
    val qHabit = activeQuantitativeHabit!!
    val existingEntry = entriesByHabit[qHabit.id]?.get(today)
    QuantitativeEntryBottomSheet(
      habit = qHabit,
      currentValue = existingEntry?.value ?: 0.0,
      currentNote = existingEntry?.note ?: "",
      onDismiss = { activeQuantitativeHabit = null },
      onSave = { newVal, newNote ->
        onLogQuantitativeToday(qHabit, newVal, newNote)
        activeQuantitativeHabit = null
      }
    )
  }

  // Optional Note Prompt Sheet when askNote is ON
  if (activeNotePromptHabit != null) {
    val nHabit = activeNotePromptHabit!!
    NotePromptBottomSheet(
      habitName = nHabit.name,
      onDismiss = { activeNotePromptHabit = null },
      onConfirm = { note ->
        onToggleHabitToday(nHabit, note)
        activeNotePromptHabit = null
      }
    )
  }

  if (activeCompletionLogHabit != null) {
    val lHabit = activeCompletionLogHabit!!
    HabitCompletionLogBottomSheet(
      habit = lHabit,
      initialDate = today,
      entries = entriesListByHabit[lHabit.id].orEmpty(),
      pauses = pausesByHabit[lHabit.id].orEmpty(),
      today = today,
      onDismiss = { activeCompletionLogHabit = null },
      onSaveLog = { log ->
        onLogCompletion(log)
        activeCompletionLogHabit = null
      },
      onClearDate = { date ->
        onClearDateEntry(lHabit, date)
        activeCompletionLogHabit = null
      }
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClassicHabitCardRow(
  habit: Habit,
  entry: Entry?,
  streak: Int,
  isNextUp: Boolean,
  density: CardDensity,
  showStreak: Boolean,
  onCardClick: () -> Unit,
  onCircleClick: () -> Unit,
  onOpenCompletionLogger: () -> Unit,
  onEdit: () -> Unit,
  onSkip: () -> Unit,
  onPause: () -> Unit,
  onArchive: () -> Unit,
  onReorder: () -> Unit,
  onDelete: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val tint = habitTint(habit.colorKey, colors.isDark)
  val isDone = ScheduleMatcher.isEntryDone(habit, entry)
  val isSkipped = entry?.status == EntryStatus.SKIPPED
  var showContextMenu by remember { mutableStateOf(false) }

  val isCompact = density == CardDensity.COMPACT
  val iconTileSize = if (isCompact) 44.dp else 64.dp
  val checkSize = if (isCompact) 44.dp else 64.dp
  val vPad = if (isCompact) 12.dp else 18.dp

  val progressRatio: Float? = if (habit.trackType != TrackType.TASK) {
    val target = (habit.targetValue ?: 1.0).coerceAtLeast(0.1)
    val curr = entry?.value ?: 0.0
    (curr / target).toFloat().coerceIn(0f, 1f)
  } else null

  val progressLabel: String? = if (habit.trackType != TrackType.TASK) {
    val target = (habit.targetValue ?: 1.0).roundToInt()
    val curr = (entry?.value ?: 0.0).roundToInt()
    "$curr/$target"
  } else null

  val nextUpBorderColor = if (isNextUp && !isDone) Color(0x99D6BEFA) else null

  Box {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(tint.cardBackground)
        .then(
          if (nextUpBorderColor != null) {
            Modifier.border(1.5.dp, nextUpBorderColor, RoundedCornerShape(28.dp))
          } else Modifier
        )
        .alpha(if (isDone) 0.72f else 1f)
        .combinedClickable(
          onClick = onCardClick,
          onLongClick = { showContextMenu = true }
        )
        .padding(horizontal = 16.dp, vertical = vPad)
        .testTag("habit_card_${habit.id}")
    ) {
      // Left icon tile
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(iconTileSize)
          .clip(RoundedCornerShape(if (isCompact) 14.dp else 20.dp))
          .background(tint.iconTileBackground)
      ) {
        HabitIconView(
          iconKey = habit.iconKey,
          tint = tint.baseColor,
          modifier = Modifier.size(iconTileSize * 0.46f),
          emojiSize = if (isCompact) 18.sp else 24.sp
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      // Middle: Title + Streak
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = habit.name,
          fontFamily = PlusJakartaSansFamily,
          fontSize = if (isCompact) 17.sp else 20.sp,
          fontWeight = FontWeight.Medium,
          color = if (isDone) colors.textSecondary else colors.textPrimary,
          textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (showStreak) {
          Spacer(modifier = Modifier.height(3.dp))
          val dateStampSuffix = if (isDone && entry != null) " · ${entry.dateStamp}" else ""
          Text(
            text = "🔥 $streak ${if (streak == 1) "Day" else "Days"}$dateStampSuffix",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.sp,
            color = colors.textSecondary
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Right check circle
      HabitCheckCircle(
        isDone = isDone,
        isSkipped = isSkipped,
        colorHex = habit.colorKey,
        size = checkSize,
        progressRatio = progressRatio,
        progressLabel = progressLabel,
        onClick = onCircleClick,
        modifier = Modifier.testTag("check_circle_${habit.id}")
      )
    }

    DropdownMenu(
      expanded = showContextMenu,
      onDismissRequest = { showContextMenu = false }
    ) {
      DropdownMenuItem(
        text = { Text("Log completion (date-stamp)") },
        onClick = { showContextMenu = false; onOpenCompletionLogger() }
      )
      DropdownMenuItem(text = { Text("Edit") }, onClick = { showContextMenu = false; onEdit() })
      DropdownMenuItem(text = { Text("Skip today") }, onClick = { showContextMenu = false; onSkip() })
      DropdownMenuItem(text = { Text("Pause") }, onClick = { showContextMenu = false; onPause() })
      DropdownMenuItem(text = { Text("Archive") }, onClick = { showContextMenu = false; onArchive() })
      DropdownMenuItem(text = { Text("Reorder") }, onClick = { showContextMenu = false; onReorder() })
      DropdownMenuItem(
        text = { Text("Delete", color = Color(0xFFEE9A9A)) },
        onClick = { showContextMenu = false; onDelete() }
      )
    }
  }
}

@Composable
fun WeeklyTabScreen(
  habits: List<Habit>,
  entriesByHabit: Map<String, Map<LocalDate, Entry>>,
  entriesListByHabit: Map<String, List<Entry>>,
  pausesByHabit: Map<String, List<HabitPause>>,
  settings: AppSettings,
  today: LocalDate,
  onOpenMenu: () -> Unit,
  onNewHabit: () -> Unit,
  onOpenHabitDetail: (String) -> Unit,
  onToggleDate: (Habit, LocalDate) -> Unit,
  onOpenAnalytics: (() -> Unit)? = null
) {
  val colors = LocalHabitRadarColors.current
  var weekOffset by remember { mutableIntStateOf(0) }

  val currentWeekStart = remember(today, settings.weekStart, weekOffset) {
    LogicalDateCalculator.startOfWeek(today, settings.weekStart).plusWeeks(weekOffset.toLong())
  }
  val weekDays = remember(currentWeekStart) {
    (0..6).map { currentWeekStart.plusDays(it.toLong()) }
  }
  val weekEnd = weekDays.last()

  val allEntriesFlat = remember(entriesListByHabit) { entriesListByHabit.values.flatten() }
  val allPausesFlat = remember(pausesByHabit) { pausesByHabit.values.flatten() }
  val weekSummary = remember(habits, allEntriesFlat, allPausesFlat, today, settings.weekStart, weekOffset) {
    WeeklyPerformanceCalculator.calculate(
      habits = habits,
      entries = allEntriesFlat,
      pauses = allPausesFlat,
      today = today,
      weekStartDay = settings.weekStart,
      weekOffset = weekOffset
    )
  }

  val rangeFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH) }
  val rangeLabel = remember(currentWeekStart, weekEnd) {
    "${currentWeekStart.format(rangeFormatter).uppercase()} - ${weekEnd.format(rangeFormatter).uppercase()}"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    MainTabHeader(
      dateLabel = rangeLabel,
      headlineText = "A week that's *working.*",
      onOpenMenu = onOpenMenu,
      onNewHabit = onNewHabit
    ) {
      Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
      ) {
        IconButton(onClick = { weekOffset-- }, modifier = Modifier.size(36.dp)) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", tint = colors.textSecondary)
        }
        IconButton(
          onClick = { if (weekOffset < 0) weekOffset++ },
          enabled = weekOffset < 0,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week", tint = colors.textSecondary)
        }
      }
    }

    LazyColumn(
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      item(key = "weekly_tab_summary_card") {
        WeeklyPerformanceSummaryCard(
          summary = weekSummary,
          onOpenAnalytics = onOpenAnalytics
        )
      }

      items(habits, key = { it.id }) { habit ->
        val tint = habitTint(habit.colorKey, colors.isDark)
        val hEntries = entriesByHabit[habit.id].orEmpty()
        val hPauses = pausesByHabit[habit.id].orEmpty()
        val streak = StreakCalculator.calculateStreaks(
          habit,
          entriesListByHabit[habit.id].orEmpty(),
          hPauses,
          today
        ).currentStreak

        HabitRadarCard(
          backgroundColor = tint.cardBackground,
          onClick = { onOpenHabitDetail(habit.id) }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tint.iconTileBackground)
            ) {
              HabitIconView(iconKey = habit.iconKey, tint = tint.baseColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = habit.name,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (settings.showStreakWeekly) {
                Text(
                  text = "🔥 $streak ${if (streak == 1) "Day" else "Days"}",
                  fontSize = 13.sp,
                  color = colors.textSecondary
                )
              }
            }
            Text(
              text = ScheduleMatcher.formatScheduleSummary(habit),
              fontFamily = PlusJakartaSansFamily,
              fontSize = 12.sp,
              color = colors.textSecondary
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 7-column day grid with Today's tall rounded capsule highlight (Screenshot 10)
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            weekDays.forEach { d ->
              val isToday = d == today
              val isFuture = d.isAfter(today)
              val entry = hEntries[d]
              val scheduled = ScheduleMatcher.isScheduledOn(habit, d, hPauses)
              val done = ScheduleMatcher.isEntryDone(habit, entry)
              val skipped = entry?.status == EntryStatus.SKIPPED

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(24.dp))
                  .background(
                    if (isToday) tint.baseColor.copy(alpha = 0.16f) else Color.Transparent
                  )
                  .padding(vertical = 8.dp)
              ) {
                Text(
                  text = ScheduleMatcher.dowShortLabel(d.dayOfWeek.value),
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 12.sp,
                  fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                  color = if (isToday) colors.textPrimary else colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val circleSize = 40.dp
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(
                      when {
                        done -> tint.baseColor.copy(alpha = 0.78f)
                        !isFuture && scheduled && !skipped -> tint.baseColor.copy(alpha = 0.10f)
                        else -> Color.Transparent
                      }
                    )
                    .border(
                      width = if (isToday && !done) 2.5.dp else 1.5.dp,
                      color = when {
                        done -> tint.baseColor
                        isToday -> tint.baseColor
                        !scheduled || isFuture -> tint.baseColor.copy(alpha = 0.22f)
                        else -> tint.baseColor.copy(alpha = 0.48f)
                      },
                      shape = CircleShape
                    )
                    .clickable(enabled = !isFuture) {
                      onToggleDate(habit, d)
                    }
                ) {
                  if (done) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Done",
                      tint = Color(0xFF121318),
                      modifier = Modifier.size(20.dp)
                    )
                  } else if (skipped) {
                    Text("—", color = tint.baseColor, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun MonthlyTabScreen(
  habits: List<Habit>,
  entriesByHabit: Map<String, Map<LocalDate, Entry>>,
  entriesListByHabit: Map<String, List<Entry>>,
  pausesByHabit: Map<String, List<HabitPause>>,
  settings: AppSettings,
  today: LocalDate,
  onOpenMenu: () -> Unit,
  onNewHabit: () -> Unit,
  onOpenHabitDetail: (String) -> Unit,
  onToggleDate: (Habit, LocalDate) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var monthOffset by remember { mutableIntStateOf(0) }
  val currentYm = remember(today, monthOffset) {
    YearMonth.from(today).plusMonths(monthOffset.toLong())
  }
  val monthLabel = remember(currentYm) {
    "${currentYm.month.name} ${currentYm.year}"
  }
  val orderedDows = remember(settings.weekStart) {
    LogicalDateCalculator.orderedDaysOfWeek(settings.weekStart)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    MainTabHeader(
      dateLabel = monthLabel,
      headlineText = "A month in *motion.*",
      onOpenMenu = onOpenMenu,
      onNewHabit = onNewHabit
    ) {
      Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
      ) {
        IconButton(onClick = { monthOffset-- }, modifier = Modifier.size(36.dp)) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = colors.textSecondary)
        }
        IconButton(
          onClick = { if (monthOffset < 0) monthOffset++ },
          enabled = monthOffset < 0,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = colors.textSecondary)
        }
      }
    }

    // Confirmed 2-column grid of per-habit mini calendar cards (Screenshot 5 + Spec Section 4.3)
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(habits, key = { it.id }) { habit ->
        val tint = habitTint(habit.colorKey, colors.isDark)
        val hEntries = entriesByHabit[habit.id].orEmpty()
        val hPauses = pausesByHabit[habit.id].orEmpty()
        val streak = StreakCalculator.calculateStreaks(
          habit,
          entriesListByHabit[habit.id].orEmpty(),
          hPauses,
          today
        ).currentStreak
        val doneToday = ScheduleMatcher.isEntryDone(habit, hEntries[today])

        HabitRadarCard(
          backgroundColor = tint.cardBackground,
          contentPadding = PaddingValues(14.dp),
          onClick = { onOpenHabitDetail(habit.id) }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.iconTileBackground)
            ) {
              HabitIconView(
                iconKey = habit.iconKey,
                tint = tint.baseColor,
                modifier = Modifier.size(20.dp),
                emojiSize = 16.sp
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = habit.name,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (settings.showStreakMonthly) {
                Text(
                  text = "🔥 $streak ${if (streak == 1) "Day" else "Days"}",
                  fontSize = 11.sp,
                  color = colors.textSecondary
                )
              }
            }
            HabitCheckCircle(
              isDone = doneToday,
              isSkipped = hEntries[today]?.status == EntryStatus.SKIPPED,
              colorHex = habit.colorKey,
              size = 32.dp,
              onClick = { onToggleDate(habit, today) }
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 7-column mini calendar grid
          val firstDay = currentYm.atDay(1)
          val leadingEmpty = orderedDows.indexOf(firstDay.dayOfWeek).coerceAtLeast(0)
          val daysInMonth = currentYm.lengthOfMonth()
          val rows = (leadingEmpty + daysInMonth + 6) / 7

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until rows) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                for (c in 0..6) {
                  val dayNum = r * 7 + c - leadingEmpty + 1
                  if (dayNum in 1..daysInMonth) {
                    val d = currentYm.atDay(dayNum)
                    val isDone = ScheduleMatcher.isEntryDone(habit, hEntries[d])
                    val isTodayCell = d == today
                    val isFuture = d.isAfter(today)
                    val scheduled = ScheduleMatcher.isScheduledOn(habit, d, hPauses)

                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                          when {
                            isDone -> tint.baseColor
                            !scheduled || isFuture -> colors.surface2.copy(alpha = 0.45f)
                            else -> colors.surface2
                          }
                        )
                        .then(
                          if (isTodayCell && !isDone) {
                            Modifier.border(1.5.dp, tint.baseColor, RoundedCornerShape(5.dp))
                          } else Modifier
                        )
                        .clickable(enabled = !isFuture) {
                          onToggleDate(habit, d)
                        }
                    )
                  } else {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun OverallTabScreen(
  habits: List<Habit>,
  entriesByHabit: Map<String, Map<LocalDate, Entry>>,
  entriesListByHabit: Map<String, List<Entry>>,
  pausesByHabit: Map<String, List<HabitPause>>,
  today: LocalDate,
  onOpenMenu: () -> Unit,
  onNewHabit: () -> Unit,
  onOpenHabitDetail: (String) -> Unit,
  onOpenDataVisualization: () -> Unit = {}
) {
  val colors = LocalHabitRadarColors.current

  var selectedCategoryOrTag by remember { mutableStateOf<String?>(null) }

  val availableCategoriesAndTags = remember(habits) {
    val map = linkedMapOf<String, String>()
    habits.forEach { h ->
      h.allOrganizationLabels.forEach { label ->
        val key = label.lowercase()
        if (!map.containsKey(key)) {
          map[key] = label
        }
      }
    }
    map.values.toList()
  }

  val filteredHabits = remember(habits, selectedCategoryOrTag) {
    if (selectedCategoryOrTag.isNullOrBlank()) {
      habits
    } else {
      habits.filter { it.matchesCategoryOrTag(selectedCategoryOrTag) }
    }
  }

  val filteredEntries = remember(filteredHabits, entriesListByHabit) {
    filteredHabits.flatMap { entriesListByHabit[it.id].orEmpty() }
  }
  val totalCheckIns = remember(filteredEntries) { filteredEntries.count { it.status == EntryStatus.DONE } }

  val habitStats = remember(filteredHabits, entriesByHabit, entriesListByHabit, pausesByHabit, today) {
    filteredHabits.map { h ->
      val s = StreakCalculator.calculateStreaks(h, entriesListByHabit[h.id].orEmpty(), pausesByHabit[h.id].orEmpty(), today)
      val rate = AnalyticsCalculator.computePeriodRate(
        h,
        entriesByHabit[h.id].orEmpty(),
        pausesByHabit[h.id].orEmpty(),
        h.startDate,
        today
      )
      Triple(h, s, rate)
    }
  }

  val bestCurrentStreak = habitStats.maxOfOrNull { it.second.currentStreak } ?: 0
  val longestStreakEver = habitStats.maxOfOrNull { it.second.bestStreak } ?: 0
  val overallCompletionRate = if (habitStats.isNotEmpty()) {
    habitStats.map { it.third }.average().roundToInt()
  } else 0

  var inlineTimeframe by remember {
    mutableStateOf(com.example.feature.analytics.VisualizationTimeframe.WEEKLY)
  }
  var inlineSelectedBarIdx by remember { mutableIntStateOf(-1) }

  val inlineChartSummary = remember(filteredHabits, entriesByHabit, pausesByHabit, inlineTimeframe, today) {
    if (inlineTimeframe == com.example.feature.analytics.VisualizationTimeframe.WEEKLY) {
      com.example.feature.analytics.CompletionRateChartCalculator.computeWeeklyMultiWeekSeries(
        habits = filteredHabits,
        entriesByHabit = entriesByHabit,
        pausesByHabit = pausesByHabit,
        weekStart = com.example.core.domain.model.WeekStartDay.MONDAY,
        today = today,
        weeksCount = 8
      )
    } else {
      com.example.feature.analytics.CompletionRateChartCalculator.computeMonthlySeries(
        habits = filteredHabits,
        entriesByHabit = entriesByHabit,
        pausesByHabit = pausesByHabit,
        today = today,
        monthsCount = 6
      )
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    MainTabHeader(
      dateLabel = "ALL TIME",
      headlineText = "The *long* view.",
      onOpenMenu = onOpenMenu,
      onNewHabit = onNewHabit
    )

    LazyColumn(
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 128.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // Compact category/tag pill filter (only shown when categories or tags exist)
      if (availableCategoriesAndTags.isNotEmpty()) {
        item {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("overall_category_filter_row")
          ) {
            item {
              val allSelected = selectedCategoryOrTag == null
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .background(if (allSelected) colors.accent else colors.surface2)
                  .clickable {
                    selectedCategoryOrTag = null
                    inlineSelectedBarIdx = -1
                  }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
                  .testTag("overall_category_chip_all")
              ) {
                Text(
                  text = "All",
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 12.sp,
                  fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (allSelected) colors.onAccent else colors.textSecondary
                )
              }
            }

            items(availableCategoriesAndTags) { label ->
              val isSelected = selectedCategoryOrTag != null && selectedCategoryOrTag.equals(label, ignoreCase = true)
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .background(if (isSelected) colors.accent else colors.surface2)
                  .clickable {
                    selectedCategoryOrTag = if (isSelected) null else label
                    inlineSelectedBarIdx = -1
                  }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
                  .testTag("overall_category_chip_${label.lowercase().replace(" ", "_")}")
              ) {
                Text(
                  text = label,
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) colors.onAccent else colors.textSecondary
                )
              }
            }
          }
        }
      }

      // Unified At-a-Glance Summary Card: all core metrics in one clean view
      item {
        HabitRadarCard(
          contentPadding = PaddingValues(18.dp),
          modifier = Modifier.testTag("overall_summary_hero_card")
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column {
              Text(
                text = "OVERALL COMPLETION",
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Row(verticalAlignment = Alignment.Bottom) {
                Text(
                  text = "$overallCompletionRate%",
                  fontFamily = InstrumentSerifFamily,
                  fontSize = 42.sp,
                  lineHeight = 44.sp,
                  color = colors.textPrimary
                )
                if (selectedCategoryOrTag != null) {
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "· $selectedCategoryOrTag",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent,
                    modifier = Modifier.padding(bottom = 6.dp)
                  )
                }
              }
            }

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colors.accentContainer)
                .clickable(onClick = onOpenDataVisualization)
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .testTag("open_data_visualization_button")
            ) {
              Text(
                text = "Charts →",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Sleek overall progress bar
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(colors.surface2)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth((overallCompletionRate / 100f).coerceIn(0.03f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(colors.accent)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(colors.borderSubtle)
          )
          Spacer(modifier = Modifier.height(12.dp))

          // 4 compact inline metrics in one row
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            OverallCompactMetric(
              label = "Check-ins",
              value = totalCheckIns.toString(),
              modifier = Modifier.weight(1f)
            )
            OverallCompactMetric(
              label = "Current",
              value = "🔥 ${bestCurrentStreak}d",
              modifier = Modifier.weight(1f)
            )
            OverallCompactMetric(
              label = "Best",
              value = "${longestStreakEver}d",
              modifier = Modifier.weight(1f)
            )
            OverallCompactMetric(
              label = "Habits",
              value = filteredHabits.size.toString(),
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // Clean, distraction-free Trend Card
      item {
        HabitRadarCard(contentPadding = PaddingValues(16.dp)) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "TREND",
              fontSize = 10.sp,
              letterSpacing = 1.4.sp,
              fontWeight = FontWeight.SemiBold,
              color = colors.textSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              com.example.feature.analytics.VisualizationTimeframe.entries.forEach { tf ->
                val isSel = inlineTimeframe == tf
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSel) colors.accent else colors.surface2)
                    .clickable {
                      inlineTimeframe = tf
                      inlineSelectedBarIdx = -1
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("overall_chart_toggle_${tf.name.lowercase()}")
                ) {
                  Text(
                    text = tf.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSel) colors.onAccent else colors.textSecondary
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          com.example.feature.analytics.RechartsBarChart(
            data = inlineChartSummary.dataPoints,
            primaryBarColor = colors.accent,
            showComparisonBars = false,
            showAverageReferenceLine = true,
            averageRate = inlineChartSummary.averageRate,
            selectedBarIndex = inlineSelectedBarIdx,
            onSelectBarIndex = { inlineSelectedBarIdx = it }
          )
        }
      }

      // Clean, compact Per-Habit rows
      if (habitStats.isNotEmpty()) {
        item {
          SectionLabel("HABITS", modifier = Modifier.padding(top = 2.dp))
        }

        items(habitStats, key = { it.first.id }) { (habit, streakSummary, rate) ->
          val tint = habitTint(habit.colorKey, colors.isDark)
          val categoryPrefix = habit.resolvedCategory?.let { "$it · " } ?: ""
          HabitRadarCard(
            backgroundColor = tint.cardBackground,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            onClick = { onOpenHabitDetail(habit.id) }
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(tint.iconTileBackground)
              ) {
                HabitIconView(iconKey = habit.iconKey, tint = tint.baseColor)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = habit.name,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1
                  )
                  Text(
                    text = "$rate%",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = tint.baseColor
                  )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(colors.surface2)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth((rate / 100f).coerceIn(0.04f, 1f))
                      .fillMaxHeight()
                      .clip(RoundedCornerShape(2.5.dp))
                      .background(tint.baseColor)
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${categoryPrefix}🔥 ${streakSummary.currentStreak}d · Best ${streakSummary.bestStreak}d",
                  fontSize = 11.sp,
                  color = colors.textSecondary,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OverallCompactMetric(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  Column(modifier = modifier) {
    Text(
      text = value,
      fontFamily = PlusJakartaSansFamily,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      color = colors.textPrimary
    )
    Text(
      text = label,
      fontSize = 11.sp,
      color = colors.textSecondary
    )
  }
}

@Composable
private fun OverallStatCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  HabitRadarCard(
    modifier = modifier,
    contentPadding = PaddingValues(14.dp)
  ) {
    Text(
      text = label,
      fontSize = 10.sp,
      letterSpacing = 1.6.sp,
      fontWeight = FontWeight.SemiBold,
      color = colors.textSecondary
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = value,
      fontFamily = InstrumentSerifFamily,
      fontSize = 26.sp,
      color = colors.textPrimary
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantitativeEntryBottomSheet(
  habit: Habit,
  currentValue: Double,
  currentNote: String,
  onDismiss: () -> Unit,
  onSave: (Double, String?) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val target = habit.targetValue ?: 8.0
  var value by remember { mutableDoubleStateOf(if (currentValue > 0) currentValue else 0.0) }
  var manualStr by remember { mutableStateOf(value.roundToInt().toString()) }
  var note by remember { mutableStateOf(currentNote) }

  // Timer state for Time habits
  var timerRunning by remember { mutableStateOf(false) }
  var elapsedSeconds by remember { mutableIntStateOf((value * 60).roundToInt()) }

  LaunchedEffect(timerRunning) {
    while (timerRunning) {
      delay(1000)
      elapsedSeconds++
      value = (elapsedSeconds / 60.0)
      manualStr = max(1, (elapsedSeconds / 60)).toString()
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = colors.bg
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
      Text(
        text = habit.name,
        fontFamily = InstrumentSerifFamily,
        fontSize = 28.sp,
        color = colors.textPrimary
      )
      Text(
        text = "Target: ${target.roundToInt()} ${habit.unit ?: if (habit.trackType == TrackType.TIME) "min" else ""}",
        fontSize = 14.sp,
        color = colors.textSecondary
      )
      Spacer(modifier = Modifier.height(20.dp))

      if (habit.trackType == TrackType.TIME) {
        val mm = elapsedSeconds / 60
        val ss = elapsedSeconds % 60
        Text(
          text = String.format("%02d:%02d", mm, ss),
          fontFamily = InstrumentSerifFamily,
          fontSize = 48.sp,
          color = colors.accent
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .clip(RoundedCornerShape(24.dp))
              .background(if (timerRunning) Color(0xFFEE9A9A) else colors.accent)
              .clickable { timerRunning = !timerRunning }
              .padding(horizontal = 24.dp, vertical = 12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (timerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = colors.onAccent
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (timerRunning) "Pause timer" else "Start timer",
                fontWeight = FontWeight.Bold,
                color = colors.onAccent
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(colors.surface2)
            .clickable {
              val next = (value - 1.0).coerceAtLeast(0.0)
              value = next
              manualStr = next.roundToInt().toString()
              elapsedSeconds = (next * 60).roundToInt()
            }
        ) {
          Text("−", fontSize = 24.sp, color = colors.textPrimary)
        }

        OutlinedTextField(
          value = manualStr,
          onValueChange = {
            manualStr = it.filter { ch -> ch.isDigit() || ch == '.' }
            value = manualStr.toDoubleOrNull() ?: 0.0
            elapsedSeconds = (value * 60).roundToInt()
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.width(110.dp)
        )

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(colors.accent)
            .clickable {
              val next = value + 1.0
              value = next
              manualStr = next.roundToInt().toString()
              elapsedSeconds = (next * 60).roundToInt()
            }
        ) {
          Text("+", fontSize = 24.sp, color = colors.onAccent)
        }
      }

      if (habit.askNote) {
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Note (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }

      Spacer(modifier = Modifier.height(20.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(colors.surface2)
            .clickable { onSave(target, note.ifBlank { null }) }
        ) {
          Text("Complete (${target.roundToInt()})", color = colors.textPrimary, fontWeight = FontWeight.Medium)
        }
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(colors.accent)
            .clickable { onSave(value, note.ifBlank { null }) }
        ) {
          Text("Save", color = colors.onAccent, fontWeight = FontWeight.Bold)
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePromptBottomSheet(
  habitName: String,
  onDismiss: () -> Unit,
  onConfirm: (String?) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var note by remember { mutableStateOf("") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = colors.bg
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
      Text(
        text = "Note for $habitName",
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        color = colors.textPrimary
      )
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = note,
        onValueChange = { note = it.take(200) },
        label = { Text("Add a short note (optional)") },
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .clip(RoundedCornerShape(25.dp))
          .background(colors.accent)
          .clickable { onConfirm(note.trim().ifBlank { null }) }
      ) {
        Text("Complete habit", color = colors.onAccent, fontWeight = FontWeight.Bold)
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
