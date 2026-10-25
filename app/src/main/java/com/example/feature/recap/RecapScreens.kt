package com.example.feature.recap

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.Entry
import com.example.core.domain.model.Goal
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.Step
import com.example.core.domain.usecase.HabitRecapItem
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.RecapCalculator
import com.example.core.domain.usecase.RecapReport
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.domain.usecase.YearInReviewReport
import com.example.core.ui.components.HabitIconBadge
import com.example.core.ui.components.HourlyHistogramChart
import com.example.core.ui.components.MonthCalendarHeatmap
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.components.SubPageTopBar
import com.example.core.ui.components.WeekdayPerformanceBars
import com.example.core.ui.components.YearGitHubHeatmap
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.InstrumentSerif
import com.example.core.ui.theme.formatSerifHeadline
import com.example.core.ui.theme.habitColor
import com.example.core.ui.theme.resolveAppAccent
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun WeeklyRecapScreen(
  habits: List<Habit>,
  entries: List<Entry>,
  pauses: List<HabitPause>,
  settings: AppSettings,
  today: LocalDate,
  onBack: () -> Unit,
  onOpenHabitDetail: (String) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val accent = resolveAppAccent(settings.accentColor)
  val context = LocalContext.current

  val currentWeekStart = remember(today, settings.weekStart) {
    LogicalDateCalculator.startOfWeek(today, settings.weekStart)
  }
  var selectedWeekStart by remember { mutableStateOf(currentWeekStart) }

  val report = remember(selectedWeekStart, habits, entries, pauses, today) {
    RecapCalculator.computeWeeklyRecap(selectedWeekStart, habits, entries, pauses, today)
  }

  val weekRangeLabel = remember(selectedWeekStart) {
    val end = selectedWeekStart.plusDays(6)
    val fmt = DateTimeFormatter.ofPattern("MMM d")
    "${selectedWeekStart.format(fmt)} – ${end.format(fmt)}, ${end.year}"
  }

  Scaffold(
    containerColor = colors.background,
    topBar = {
      SubPageTopBar(
        title = "Weekly recap",
        onBack = onBack,
        actions = {
          IconButton(
            onClick = {
              shareRecapSummaryImage(
                context = context,
                periodLabel = weekRangeLabel,
                headline = "This week in review",
                rate = report.overallRate,
                doneCount = report.doneCheckIns,
                schedCount = report.scheduledCheckIns,
                perfectDays = report.perfectDays,
                bestStreak = report.bestStreakInPeriod,
                topHabitName = report.mostConsistentHabit?.habit?.name ?: "Active habits"
              )
            },
            modifier = Modifier.testTag("weekly_recap_share_btn")
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share weekly recap", tint = colors.textPrimary)
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(bottom = 48.dp)
    ) {
      // 1. Week Navigator
      item {
        PeriodNavigatorRow(
          label = weekRangeLabel,
          isSoFar = report.isSoFar,
          canGoNext = selectedWeekStart.isBefore(currentWeekStart),
          onPrev = { selectedWeekStart = selectedWeekStart.minusWeeks(1) },
          onNext = {
            if (selectedWeekStart.isBefore(currentWeekStart)) {
              selectedWeekStart = selectedWeekStart.plusWeeks(1)
            }
          }
        )
      }

      if (!report.hasEnoughData) {
        item {
          EmptyRecapCard("Complete a few days of habits in this week to unlock your weekly recap.")
        }
      } else {
        // 2. Hero Card
        item {
          RecapHeroCard(
            headline = report.title,
            overallRate = report.overallRate,
            delta = report.deltaVsPrevious,
            deltaLabel = "vs last week",
            subtitle = report.subtitle,
            accent = accent
          )
        }

        // 3. 4-Tile KPI Grid
        item {
          RecapKpiGrid(
            tile1Value = "${report.perfectDays} / 7",
            tile1Label = "Perfect days",
            tile2Value = "${report.activeDays} / 7",
            tile2Label = "Active days",
            tile3Value = "${report.bestStreakInPeriod}d",
            tile3Label = "Best streak",
            tile4Value = "${report.skippedDays}",
            tile4Label = "Skipped"
          )
        }

        // 4. 7-Day Strip
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(colors.surface)
              .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
              .padding(16.dp)
          ) {
            SectionLabel("7-DAY STRIP")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              report.dayStrip.forEach { day ->
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(day.shortLabel, color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                  Box(
                    modifier = Modifier
                      .width(34.dp)
                      .height(68.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(colors.surfaceElevated)
                      .border(
                        width = if (day.isPerfect) 1.5.dp else 0.dp,
                        color = if (day.isPerfect) colors.success else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                      ),
                    contentAlignment = Alignment.BottomCenter
                  ) {
                    val fraction = (day.completionRate / 100f).coerceIn(0f, 1f)
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height((68.dp * fraction))
                        .background(if (day.isPerfect) colors.success else accent)
                    )
                  }
                  Text(
                    text = "${day.doneCount}/${day.scheduledCount}",
                    color = colors.textSecondary,
                    fontSize = 10.sp
                  )
                }
              }
            }
          }
        }

        // 5. Per-Habit Week Card
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(colors.surface)
              .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            SectionLabel("PER-HABIT WEEK")
            report.leaderboard.forEach { item ->
              val hColor = habitColor(item.habit.colorKey)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(colors.surfaceElevated)
                  .clickable { onOpenHabitDetail(item.habit.id) }
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                HabitIconBadge(iconKey = item.habit.iconKey, colorKey = item.habit.colorKey, size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.habit.name,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    item.weekPattern.forEach { done ->
                      Box(
                        modifier = Modifier
                          .size(10.dp)
                          .clip(CircleShape)
                          .background(if (done) hColor else colors.borderSubtle)
                      )
                    }
                  }
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("${item.doneCount}/${item.scheduledCount}", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  val dStr = if (item.deltaVsPrevious >= 0) "+${item.deltaVsPrevious}%" else "${item.deltaVsPrevious}%"
                  Text(
                    text = dStr,
                    color = if (item.deltaVsPrevious >= 0) colors.success else colors.warning,
                    fontSize = 11.sp
                  )
                }
              }
            }
          }
        }

        // 6. Highlight Cards
        item {
          HighlightPairRow(
            mostConsistent = report.mostConsistentHabit,
            needsLove = report.needsLoveHabit
          )
        }

        // 7. Streaks Status Card
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(colors.surface)
              .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("🔥 ${report.streaksKeptAlive}", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
              Text("Streaks kept alive", color = colors.textSecondary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("💔 ${report.streaksBroken}", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
              Text("Streaks broken", color = colors.textSecondary, fontSize = 12.sp)
            }
          }
        }

        // 8. Time-of-Day Card
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(colors.surface)
              .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
              .padding(16.dp)
          ) {
            SectionLabel("TIME OF DAY • PEAK ${report.peakHourLabel}")
            Spacer(modifier = Modifier.height(10.dp))
            HourlyHistogramChart(hourlyCounts = report.hourlyHistogram, accentColor = accent)
          }
        }

        // 9. Focus for Next Week Card
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(colors.surfaceElevated)
              .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
              .padding(16.dp)
          ) {
            SectionLabel("FOCUS FOR NEXT WEEK")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = report.nextFocusSuggestion,
              color = colors.textPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}

@Composable
fun MonthlyRecapScreen(
  habits: List<Habit>,
  entries: List<Entry>,
  pauses: List<HabitPause>,
  settings: AppSettings,
  today: LocalDate,
  onBack: () -> Unit,
  onOpenHabitDetail: (String) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val accent = resolveAppAccent(settings.accentColor)
  val context = LocalContext.current

  val currentYm = remember(today) { YearMonth.from(today) }
  var selectedYm by remember { mutableStateOf(currentYm) }

  val report = remember(selectedYm, habits, entries, pauses, today) {
    RecapCalculator.computeMonthlyRecap(selectedYm, habits, entries, pauses, today)
  }

  val monthLabel = remember(selectedYm) {
    val m = selectedYm.month.name.lowercase().replaceFirstChar { it.uppercase() }
    "$m ${selectedYm.year}"
  }

  Scaffold(
    containerColor = colors.background,
    topBar = {
      SubPageTopBar(
        title = "Monthly recap",
        onBack = onBack,
        actions = {
          IconButton(
            onClick = {
              shareRecapSummaryImage(
                context = context,
                periodLabel = monthLabel,
                headline = "$monthLabel in review",
                rate = report.overallRate,
                doneCount = report.doneCheckIns,
                schedCount = report.scheduledCheckIns,
                perfectDays = report.perfectDays,
                bestStreak = report.bestStreakInPeriod,
                topHabitName = report.mostConsistentHabit?.habit?.name ?: "Active habits"
              )
            },
            modifier = Modifier.testTag("monthly_recap_share_btn")
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share monthly recap", tint = colors.textPrimary)
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(bottom = 48.dp)
    ) {
      // 1. Month Navigator
      item {
        PeriodNavigatorRow(
          label = monthLabel,
          isSoFar = report.isSoFar,
          canGoNext = selectedYm.isBefore(currentYm),
          onPrev = { selectedYm = selectedYm.minusMonths(1) },
          onNext = {
            if (selectedYm.isBefore(currentYm)) {
              selectedYm = selectedYm.plusMonths(1)
            }
          }
        )
      }

      if (!report.hasEnoughData) {
        item {
          EmptyRecapCard("Complete at least a few habit check-ins in $monthLabel to generate your monthly recap.")
        }
      } else {
        // 2. Hero Card
        item {
          RecapHeroCard(
            headline = report.title,
            overallRate = report.overallRate,
            delta = report.deltaVsPrevious,
            deltaLabel = "vs previous month",
            subtitle = report.subtitle,
            accent = accent
          )
        }

        // 3. 4-Tile KPI Grid
        item {
          RecapKpiGrid(
            tile1Value = "${report.perfectDays} / ${selectedYm.lengthOfMonth()}",
            tile1Label = "Perfect days",
            tile2Value = "${report.activeDays} / ${selectedYm.lengthOfMonth()}",
            tile2Label = "Active days",
            tile3Value = "${report.bestStreakInPeriod}d",
            tile3Label = "Best streak",
            tile4Value = "${report.newHabitsStarted}",
            tile4Label = "New habits"
          )
        }

        // 4. Week-by-Week Breakdown
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(colors.surface)
              .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            SectionLabel("WEEK-BY-WEEK BREAKDOWN")
            report.weekBars.forEach { (wLabel, wRate) ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(wLabel, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(64.dp))
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.surfaceElevated)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth((wRate / 100f).coerceIn(0f, 1f))
                      .height(10.dp)
                      .clip(RoundedCornerShape(5.dp))
                      .background(accent)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("$wRate%", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
              }
            }
          }
        }

        // 5. Habit Leaderboard
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(colors.surface)
              .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            SectionLabel("HABIT LEADERBOARD")
            report.leaderboard.forEach { item ->
              val hColor = habitColor(item.habit.colorKey)
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(colors.surfaceElevated)
                  .clickable { onOpenHabitDetail(item.habit.id) }
                  .padding(10.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  HabitIconBadge(iconKey = item.habit.iconKey, colorKey = item.habit.colorKey, size = 32.dp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = item.habit.name,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                  )
                  Text(
                    text = "${item.completionRate}% (${item.doneCount}/${item.scheduledCount})",
                    color = hColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.borderSubtle)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth((item.completionRate / 100f).coerceIn(0f, 1f))
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp))
                      .background(hColor)
                  )
                }
              }
            }
          }
        }

        // 6. Highlight Cards
        item {
          HighlightPairRow(
            mostConsistent = report.mostConsistentHabit,
            needsLove = report.needsLoveHabit
          )
        }

        // 7. Auto-generated Insights Card
        if (report.insights.isNotEmpty()) {
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              SectionLabel("MONTHLY INSIGHTS")
              report.insights.forEach { line ->
                Row(verticalAlignment = Alignment.Top) {
                  Text("• ", color = accent, fontWeight = FontWeight.Bold)
                  Text(line, color = colors.textPrimary, fontSize = 13.sp)
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
fun YearInReviewScreen(
  habits: List<Habit>,
  entries: List<Entry>,
  pauses: List<HabitPause>,
  goals: List<Goal>,
  stepsByGoal: Map<String, List<Step>>,
  settings: AppSettings,
  today: LocalDate,
  onBack: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val accent = resolveAppAccent(settings.accentColor)
  val context = LocalContext.current

  val availableYears = remember(entries, today) {
    val yrs = entries.map { it.date.year }.distinct().sorted()
    if (yrs.isEmpty()) listOf(today.year) else (yrs + today.year).distinct().sorted()
  }
  var selectedYear by remember { mutableIntStateOf(today.year) }
  var summaryMode by remember { mutableStateOf(false) }

  val report = remember(selectedYear, habits, entries, pauses, today) {
    RecapCalculator.computeYearInReview(selectedYear, habits, entries, pauses, today)
  }

  val pagerState = rememberPagerState(pageCount = { 12 })

  Scaffold(
    containerColor = colors.background,
    topBar = {
      SubPageTopBar(
        title = "Year in Review",
        onBack = onBack,
        actions = {
          Text(
            text = if (summaryMode) "Story" else "Summary",
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .clickable { summaryMode = !summaryMode }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("year_review_mode_toggle")
          )
          IconButton(
            onClick = {
              shareRecapSummaryImage(
                context = context,
                periodLabel = "${report.year}${if (report.isSoFar) " (so far)" else ""}",
                headline = "${report.year} in review",
                rate = report.overallCompletionRate,
                doneCount = report.totalCheckIns,
                schedCount = report.daysTracked,
                perfectDays = report.perfectDaysCount,
                bestStreak = report.longestStreakLength,
                topHabitName = report.mostConsistentHabit?.habit?.name ?: "Habit Radar"
              )
            },
            modifier = Modifier.testTag("year_review_share_btn")
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share year in review", tint = colors.textPrimary)
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Year selector pills
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        availableYears.forEach { yr ->
          val selected = yr == selectedYear
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(if (selected) accent else colors.surface)
              .border(1.dp, if (selected) accent else colors.borderSubtle, RoundedCornerShape(50))
              .clickable { selectedYear = yr }
              .padding(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text(
              text = if (yr == today.year) "$yr (so far)" else "$yr",
              color = if (selected) Color(0xFF0A0A0D) else colors.textPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      if (summaryMode) {
        // Scrollable single-page Summary Mode
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
          for (pageIdx in 0 until 12) {
            item {
              YearStoryCardContent(
                pageIndex = pageIdx,
                report = report,
                goals = goals,
                accent = accent,
                isCompactCard = true,
                onShare = {
                  shareRecapSummaryImage(
                    context = context,
                    periodLabel = "${report.year}",
                    headline = "${report.year} in review",
                    rate = report.overallCompletionRate,
                    doneCount = report.totalCheckIns,
                    schedCount = report.daysTracked,
                    perfectDays = report.perfectDaysCount,
                    bestStreak = report.longestStreakLength,
                    topHabitName = report.mostConsistentHabit?.habit?.name ?: "Habit Radar"
                  )
                }
              )
            }
          }
        }
      } else {
        // 12-Page Full-screen Story Mode
        // Progress dots at top
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          repeat(12) { idx ->
            val active = idx <= pagerState.currentPage
            Box(
              modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (active) accent else colors.borderSubtle)
            )
          }
        }

        HorizontalPager(
          state = pagerState,
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) { pageIdx ->
          YearStoryCardContent(
            pageIndex = pageIdx,
            report = report,
            goals = goals,
            accent = accent,
            isCompactCard = false,
            onShare = {
              shareRecapSummaryImage(
                context = context,
                periodLabel = "${report.year}",
                headline = "${report.year} in review",
                rate = report.overallCompletionRate,
                doneCount = report.totalCheckIns,
                schedCount = report.daysTracked,
                perfectDays = report.perfectDaysCount,
                bestStreak = report.longestStreakLength,
                topHabitName = report.mostConsistentHabit?.habit?.name ?: "Habit Radar"
              )
            }
          )
        }
      }
    }
  }
}

@Composable
private fun YearStoryCardContent(
  pageIndex: Int,
  report: YearInReviewReport,
  goals: List<Goal>,
  accent: Color,
  isCompactCard: Boolean,
  onShare: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (isCompactCard) Modifier else Modifier.fillMaxSize())
      .clip(RoundedCornerShape(24.dp))
      .background(colors.surface)
      .border(1.dp, colors.borderSubtle, RoundedCornerShape(24.dp))
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      when (pageIndex) {
        0 -> {
          SectionLabel("PAGE 1 • COVER")
          Text(
            text = formatSerifHeadline("Your ${report.year}, *in habits.*", accent),
            color = colors.textPrimary,
            fontFamily = InstrumentSerif,
            fontSize = 36.sp,
            textAlign = TextAlign.Center
          )
          Text(
            text = "${report.daysTracked} active days tracked${if (report.isSoFar) " so far" else ""}",
            color = colors.textSecondary,
            fontSize = 15.sp
          )
        }
        1 -> {
          SectionLabel("PAGE 2 • TOTAL CHECK-INS")
          Text(
            text = "${report.totalCheckIns}",
            color = accent,
            fontFamily = InstrumentSerif,
            fontSize = 54.sp
          )
          Text(
            text = "Times you showed up this year",
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
          )
          if (report.totalMinutesTracked > 0) {
            Text(
              text = "Plus ${report.totalMinutesTracked} minutes logged in timed habits",
              color = colors.textSecondary,
              fontSize = 13.sp
            )
          }
        }
        2 -> {
          SectionLabel("PAGE 3 • COMPLETION RATE")
          Text(
            text = "${report.overallCompletionRate}%",
            color = accent,
            fontFamily = InstrumentSerif,
            fontSize = 52.sp
          )
          Text(
            text = "Overall completion rate across ${report.year}",
            color = colors.textSecondary,
            fontSize = 14.sp
          )
        }
        3 -> {
          SectionLabel("PAGE 4 • MONTH BY MONTH")
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            report.monthlyRates.forEach { (mName, mRate) ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
              ) {
                Box(
                  modifier = Modifier
                    .width(16.dp)
                    .height((75.dp * (mRate / 100f).coerceIn(0.06f, 1f)))
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(mName.take(1), color = colors.textSecondary, fontSize = 10.sp)
              }
            }
          }
          val bestM = report.bestMonth
          if (bestM != null) {
            Text("Best month: ${bestM.first} (${bestM.second}%)", color = colors.success, fontSize = 13.sp)
          }
        }
        4 -> {
          SectionLabel("PAGE 5 • 365-DAY MOSAIC")
          val refToday = remember(report.year) {
            val endOfYr = LocalDate.of(report.year, 12, 31)
            val now = LocalDate.now()
            if (endOfYr.isAfter(now)) now else endOfYr
          }
          YearGitHubHeatmap(
            today = refToday,
            color = accent,
            isDoneForDate = { d -> report.dailyIntensityMap[d] ?: 0f }
          )
        }
        5 -> {
          SectionLabel("PAGE 6 • LONGEST STREAK")
          Text(
            text = "🔥 ${report.longestStreakLength} days",
            color = accent,
            fontFamily = InstrumentSerif,
            fontSize = 44.sp
          )
          Text(
            text = report.longestStreakHabit?.name ?: "Keep building your streak",
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
        6 -> {
          SectionLabel("PAGE 7 • MOST CONSISTENT HABIT")
          val top = report.mostConsistentHabit
          if (top != null) {
            HabitIconBadge(iconKey = top.habit.iconKey, colorKey = top.habit.colorKey, size = 56.dp)
            Text(top.habit.name, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${top.completionRate}% completion (${top.doneCount} check-ins)", color = accent, fontSize = 15.sp)
          } else {
            Text("Log habits to see your most consistent anchor.", color = colors.textSecondary, fontSize = 14.sp)
          }
        }
        7 -> {
          SectionLabel("PAGE 8 • MOST IMPROVED HABIT")
          val imp = report.mostImprovedHabit
          if (imp != null) {
            HabitIconBadge(iconKey = imp.first.iconKey, colorKey = imp.first.colorKey, size = 52.dp)
            Text(imp.first.name, color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val sign = if (imp.second >= 0) "+${imp.second}%" else "${imp.second}%"
            Text("$sign second-half momentum", color = colors.success, fontSize = 14.sp)
          } else {
            Text("Comparing H1 vs H2 unlocks in the second half of the year.", color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
          }
        }
        8 -> {
          SectionLabel("PAGE 9 • RHYTHM & TIMING")
          Text(
            text = formatSerifHeadline("You're a *${report.bestWeekday}* person.", accent),
            color = colors.textPrimary,
            fontFamily = InstrumentSerif,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
          )
          Text("Peak check-in hour: ${report.peakHourLabel}", color = colors.textSecondary, fontSize = 13.sp)
          val weekdayStats = remember(report.weekdayRates) {
            report.weekdayRates.mapIndexed { index, (label, rate) ->
              com.example.core.domain.usecase.WeekdayStat(
                isoDow = index + 1,
                shortName = label.take(3),
                fullName = label,
                rate = rate,
                doneCount = rate,
                scheduledCount = 100
              )
            }
          }
          val bestDow = weekdayStats.maxByOrNull { it.rate }?.isoDow
          val worstDow = weekdayStats.minByOrNull { it.rate }?.isoDow
          WeekdayPerformanceBars(
            stats = weekdayStats,
            bestDow = bestDow,
            worstDow = worstDow,
            accentColor = accent
          )
        }
        9 -> {
          SectionLabel("PAGE 10 • TOP 5 HABITS")
          Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            report.topHabitsByCheckIns.forEachIndexed { idx, (h, count) ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(colors.surfaceElevated)
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("#${idx + 1}", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                Text(h.name, color = colors.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("$count", color = colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
        10 -> {
          SectionLabel("PAGE 11 • PERFECT DAYS & GOALS")
          Text(
            text = "${report.perfectDaysCount} Perfect Days",
            color = accent,
            fontFamily = InstrumentSerif,
            fontSize = 36.sp
          )
          Text(
            text = "Longest unbroken perfect run: ${report.bestPerfectDayRun} days • ${goals.size} Milestone goals tracked",
            color = colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
          )
        }
        else -> {
          SectionLabel("PAGE 12 • CLOSING SUMMARY")
          Text(
            text = formatSerifHeadline("Here's to *${report.year + 1}.*", accent),
            color = colors.textPrimary,
            fontFamily = InstrumentSerif,
            fontSize = 34.sp
          )
          Text(
            text = "${report.totalCheckIns} check-ins • ${report.overallCompletionRate}% completion • ${report.perfectDaysCount} perfect days",
            color = colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0A0D))
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share Year Card", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun PeriodNavigatorRow(
  label: String,
  isSoFar: Boolean,
  canGoNext: Boolean,
  onPrev: () -> Unit,
  onNext: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(colors.surface)
      .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    IconButton(onClick = onPrev) {
      Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous period", tint = colors.textPrimary)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(label, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
      if (isSoFar) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceElevated)
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text("So far", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
    IconButton(onClick = onNext, enabled = canGoNext) {
      Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "Next period",
        tint = if (canGoNext) colors.textPrimary else colors.textTertiary
      )
    }
  }
}

@Composable
private fun RecapHeroCard(
  headline: String,
  overallRate: Int,
  delta: Int,
  deltaLabel: String,
  subtitle: String,
  accent: Color
) {
  val colors = LocalHabitRadarColors.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(colors.surface)
      .border(1.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = formatSerifHeadline(headline, accent),
      color = colors.textPrimary,
      fontFamily = InstrumentSerif,
      fontSize = 30.sp,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "$overallRate%",
      color = accent,
      fontFamily = InstrumentSerif,
      fontSize = 52.sp
    )
    val sign = if (delta >= 0) "+$delta%" else "$delta%"
    Text(
      text = "$sign $deltaLabel • $subtitle",
      color = if (delta >= 0) colors.success else colors.warning,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
private fun RecapKpiGrid(
  tile1Value: String,
  tile1Label: String,
  tile2Value: String,
  tile2Label: String,
  tile3Value: String,
  tile3Label: String,
  tile4Value: String,
  tile4Label: String
) {
  val colors = LocalHabitRadarColors.current
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      KpiTile(tile1Value, tile1Label, Modifier.weight(1f))
      KpiTile(tile2Value, tile2Label, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      KpiTile(tile3Value, tile3Label, Modifier.weight(1f))
      KpiTile(tile4Value, tile4Label, Modifier.weight(1f))
    }
  }
}

@Composable
private fun KpiTile(value: String, label: String, modifier: Modifier = Modifier) {
  val colors = LocalHabitRadarColors.current
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(colors.surface)
      .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    Text(value, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(2.dp))
    Text(label, color = colors.textSecondary, fontSize = 12.sp)
  }
}

@Composable
private fun HighlightPairRow(
  mostConsistent: HabitRecapItem?,
  needsLove: HabitRecapItem?
) {
  val colors = LocalHabitRadarColors.current
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(16.dp))
        .background(colors.surface)
        .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
        .padding(14.dp)
    ) {
      SectionLabel("MOST CONSISTENT")
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = mostConsistent?.habit?.name ?: "—",
        color = colors.textPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = if (mostConsistent != null) "${mostConsistent.completionRate}% (${mostConsistent.doneCount}/${mostConsistent.scheduledCount})" else "No data",
        color = colors.success,
        fontSize = 12.sp
      )
    }
    Column(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(16.dp))
        .background(colors.surface)
        .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
        .padding(14.dp)
    ) {
      SectionLabel("NEEDS SOME LOVE")
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = needsLove?.habit?.name ?: "All on track",
        color = colors.textPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = if (needsLove != null) "${needsLove.completionRate}% (${needsLove.doneCount}/${needsLove.scheduledCount})" else "Great job!",
        color = colors.warning,
        fontSize = 12.sp
      )
    }
  }
}

@Composable
private fun EmptyRecapCard(message: String) {
  val colors = LocalHabitRadarColors.current
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(colors.surface)
      .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
      .padding(28.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(message, color = colors.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
  }
}

fun shareRecapSummaryImage(
  context: Context,
  periodLabel: String,
  headline: String,
  rate: Int,
  doneCount: Int,
  schedCount: Int,
  perfectDays: Int,
  bestStreak: Int,
  topHabitName: String
) {
  runCatching {
    val width = 1080
    val height = 1350
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Background #0A0A0D
    canvas.drawColor(android.graphics.Color.parseColor("#0A0A0D"))

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.parseColor("#141419")
      style = Paint.Style.FILL
    }
    canvas.drawRoundRect(RectF(72f, 96f, width - 72f, height - 96f), 56f, 56f, cardPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.parseColor("#9A9AA6")
      textSize = 36f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(periodLabel.uppercase(), 140f, 220f, titlePaint)

    val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.parseColor("#F5F5F8")
      textSize = 72f
      typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    }
    canvas.drawText(headline, 140f, 340f, heroPaint)

    val ratePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.parseColor("#8ED9A6")
      textSize = 190f
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    canvas.drawText("$rate%", 140f, 580f, ratePaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.parseColor("#F5F5F8")
      textSize = 42f
    }
    canvas.drawText("Check-ins completed: $doneCount / $schedCount", 140f, 720f, subPaint)
    canvas.drawText("Perfect days: $perfectDays", 140f, 810f, subPaint)
    canvas.drawText("Longest streak: ${bestStreak}d", 140f, 900f, subPaint)
    canvas.drawText("Top anchor: $topHabitName", 140f, 990f, subPaint)

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.parseColor("#5C5C68")
      textSize = 32f
    }
    canvas.drawText("Habit Radar", 140f, height - 160f, footerPaint)

    val cacheDir = File(context.cacheDir, "shared_recaps").apply { mkdirs() }
    val outFile = File(cacheDir, "habit_radar_recap.png")
    FileOutputStream(outFile).use { out ->
      bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "image/png"
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Recap"))
  }
}
