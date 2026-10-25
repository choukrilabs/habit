package com.example.feature.habitdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.TrackType
import com.example.core.domain.usecase.AnalyticsCalculator
import com.example.core.domain.usecase.AnalyticsPeriod
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.ui.components.HabitCheckCircle
import com.example.core.ui.components.HabitIconView
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.components.HabitRadarSegmentedControl
import com.example.core.ui.components.HabitRadarSwitch
import com.example.core.ui.components.HourlyHistogramChart
import com.example.core.ui.components.MomentumRingView
import com.example.core.ui.components.MonthCalendarHeatmap
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.components.SubPageTopBar
import com.example.core.ui.components.TrendLineChart
import com.example.core.ui.components.VolumeBarChart
import com.example.core.ui.components.WeekdayPerformanceBars
import com.example.core.ui.components.YearGitHubHeatmap
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.example.core.ui.theme.habitTint
import com.example.feature.habit.ReminderEditorBottomSheet
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitDetailScreen(
  habit: Habit,
  entries: List<Entry>,
  pauses: List<HabitPause>,
  reminders: List<Reminder>,
  settings: AppSettings,
  today: LocalDate,
  onBack: () -> Unit,
  onEditHabit: () -> Unit,
  onToggleDate: (LocalDate, String?) -> Unit,
  onSkipDate: (LocalDate) -> Unit,
  onDeleteDateEntry: (LocalDate) -> Unit,
  onPauseRequest: () -> Unit,
  onResumeHabit: () -> Unit,
  onArchiveHabit: () -> Unit,
  onRestoreHabit: () -> Unit,
  onAddWidgetForHabit: () -> Unit,
  onDeleteHabit: () -> Unit,
  onMakeGoalFromHabit: () -> Unit,
  onSaveReminder: (Reminder) -> Unit,
  onDeleteReminder: (String) -> Unit,
  onToggleAllReminders: (Boolean) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val tint = habitTint(habit.colorKey, colors.isDark)
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.D30) }
  var heatmapYearMode by remember { mutableStateOf(false) }
  var calendarMonthOffset by remember { mutableIntStateOf(0) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var selectedDaySheetDate by remember { mutableStateOf<LocalDate?>(null) }
  var editingReminder by remember { mutableStateOf<Reminder?>(null) }
  var showNewReminderSheet by remember { mutableStateOf(false) }

  val entriesMap = remember(entries) { entries.associateBy { it.date } }
  val firstReminderMinutes = remember(reminders) {
    reminders.filter { it.enabled }.minByOrNull { it.time }?.let { it.time.hour * 60 + it.time.minute }
  }

  val analytics = remember(habit, entries, pauses, firstReminderMinutes, selectedPeriod, settings.weekStart, today) {
    AnalyticsCalculator.calculateAnalytics(
      habit = habit,
      entries = entries,
      pauses = pauses,
      firstReminderMinutesOfDay = firstReminderMinutes,
      period = selectedPeriod,
      weekStart = settings.weekStart,
      today = today
    )
  }

  val hasActiveReminders = reminders.any { it.enabled }
  val isArchived = habit.status == HabitStatus.ARCHIVED
  val isPausedToday = ScheduleMatcher.isPausedOn(today, pauses)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(
      title = habit.name,
      onBack = onBack
    ) {
      // Bell button -> scrolls to Section N (Reminders); shows accent dot when reminders active
      Box(contentAlignment = Alignment.TopEnd) {
        IconButton(
          onClick = {
            scope.launch { listState.animateScrollToItem(13) }
          }
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Scroll to reminders",
            tint = if (hasActiveReminders) tint.baseColor else colors.textSecondary
          )
        }
        if (hasActiveReminders) {
          Box(
            modifier = Modifier
              .padding(top = 10.dp, end = 10.dp)
              .size(8.dp)
              .clip(CircleShape)
              .background(colors.accent)
          )
        }
      }

      if (!isArchived) {
        IconButton(onClick = onEditHabit) {
          Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Habit", tint = colors.textPrimary)
        }
      }

      Box {
        IconButton(onClick = { showOverflowMenu = true }) {
          Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options", tint = colors.textPrimary)
        }
        DropdownMenu(
          expanded = showOverflowMenu,
          onDismissRequest = { showOverflowMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("Skip today") },
            onClick = { showOverflowMenu = false; onSkipDate(today) }
          )
          DropdownMenuItem(
            text = { Text("Reset today") },
            onClick = { showOverflowMenu = false; onDeleteDateEntry(today) }
          )
          if (isPausedToday) {
            DropdownMenuItem(
              text = { Text("Resume habit") },
              onClick = { showOverflowMenu = false; onResumeHabit() }
            )
          } else {
            DropdownMenuItem(
              text = { Text("Pause habit") },
              onClick = { showOverflowMenu = false; onPauseRequest() }
            )
          }
          if (isArchived) {
            DropdownMenuItem(
              text = { Text("Restore habit") },
              onClick = { showOverflowMenu = false; onRestoreHabit() }
            )
          } else {
            DropdownMenuItem(
              text = { Text("Archive habit") },
              onClick = { showOverflowMenu = false; onArchiveHabit() }
            )
          }
          DropdownMenuItem(
            text = { Text("Add widget") },
            onClick = { showOverflowMenu = false; onAddWidgetForHabit() }
          )
          DropdownMenuItem(
            text = { Text("Delete habit", color = Color(0xFFEE9A9A)) },
            onClick = { showOverflowMenu = false; onDeleteHabit() }
          )
        }
      }
    }

    // Archived read-only banner
    if (isArchived) {
      val archivedDateStr = habit.archivedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()
        ?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: ""
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .background(colors.accentContainer)
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        Text(
          text = "Archived on $archivedDateStr",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 14.sp,
          color = colors.textPrimary
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.accent)
            .clickable(onClick = onRestoreHabit)
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Text("Restore", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onAccent)
        }
      }
    }

    // Sticky period selector: 7D · 30D · 90D · 1Y · All
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .background(colors.appBar)
        .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
      AnalyticsPeriod.entries.forEach { p ->
        val sel = p == selectedPeriod
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (sel) tint.baseColor else colors.surface2)
            .clickable { selectedPeriod = p }
        ) {
          Text(
            text = p.label,
            fontSize = 13.sp,
            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
            color = if (sel) Color(0xFF101114) else colors.textSecondary
          )
        }
      }
    }

    LazyColumn(
      state = listState,
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // SECTION A. HERO
      item(key = "sec_a_hero") {
        val daysSinceStart = (ChronoUnit.DAYS.between(habit.startDate, today).toInt() + 1).coerceAtLeast(1)
        val doneToday = ScheduleMatcher.isEntryDone(habit, entriesMap[today])
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(tint.iconTileBackground)
            ) {
              HabitIconView(
                iconKey = habit.iconKey,
                tint = tint.baseColor,
                modifier = Modifier.size(34.dp),
                emojiSize = 28.sp
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = habit.name,
                fontFamily = InstrumentSerifFamily,
                fontSize = 28.sp,
                color = colors.textPrimary
              )
              if (!habit.description.isNullOrBlank()) {
                Text(
                  text = habit.description,
                  fontSize = 13.sp,
                  color = colors.textSecondary
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${ScheduleMatcher.formatScheduleSummary(habit)} · Since ${habit.startDate.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} · $daysSinceStart days",
                fontSize = 12.sp,
                color = colors.textSecondary
              )
            }
            if (!isArchived) {
              HabitCheckCircle(
                isDone = doneToday,
                isSkipped = entriesMap[today]?.status == EntryStatus.SKIPPED,
                colorHex = habit.colorKey,
                size = 56.dp,
                onClick = { onToggleDate(today, null) }
              )
            }
          }

          if (analytics.streakAtRisk) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x33FF8A3D))
                .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
              Text(
                text = "Your 🔥 ${analytics.currentStreak}-day streak ends tonight.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFB878)
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 4 stat tiles (2x2)
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DetailStatBox(
              label = "CURRENT STREAK",
              value = "🔥 ${analytics.currentStreak}d",
              bg = tint.subtleElementBg,
              modifier = Modifier.weight(1f)
            )
            DetailStatBox(
              label = "BEST STREAK",
              value = "${analytics.bestStreak}d",
              bg = tint.subtleElementBg,
              modifier = Modifier.weight(1f)
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            val deltaPrefix = if (analytics.periodDelta >= 0) "▲ +${analytics.periodDelta}%" else "▼ ${analytics.periodDelta}%"
            DetailStatBox(
              label = "COMPLETION (${selectedPeriod.label})",
              value = "${analytics.periodCompletionRate}% ($deltaPrefix)",
              bg = tint.subtleElementBg,
              modifier = Modifier.weight(1f)
            )
            DetailStatBox(
              label = "TOTAL CHECK-INS",
              value = analytics.totalCheckIns.toString(),
              bg = tint.subtleElementBg,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Momentum score ring
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .background(tint.subtleElementBg)
              .padding(14.dp)
          ) {
            MomentumRingView(score = analytics.momentumScore, color = tint.baseColor, size = 76.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                text = "MOMENTUM · ${analytics.momentumLabel.uppercase()}",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = tint.baseColor
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = analytics.momentumHint,
                fontSize = 14.sp,
                color = colors.textPrimary
              )
            }
          }
        }
      }

      // SECTION B. NEXT MILESTONE
      item(key = "sec_b_milestone") {
        val ratio = (analytics.currentStreak.toFloat() / analytics.nextMilestoneTarget.toFloat()).coerceIn(0.05f, 1f)
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            SectionLabel("NEXT MILESTONE")
            Text(
              text = "Make this a goal",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = tint.baseColor,
              modifier = Modifier.clickable(onClick = onMakeGoalFromHabit)
            )
          }
          Text(
            text = "${analytics.currentStreak} / ${analytics.nextMilestoneTarget} days · ${analytics.nextMilestoneRemaining} to go",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
          )
          Spacer(modifier = Modifier.height(8.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(colors.surface2)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(ratio)
                .fillMaxHeight()
                .clip(RoundedCornerShape(5.dp))
                .background(tint.baseColor)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Best ever: ${analytics.bestStreak} days",
            fontSize = 12.sp,
            color = colors.textSecondary
          )
        }
      }

      // SECTION C. TREND CHART ⏱
      item(key = "sec_c_trend") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          SectionLabel("TREND CHART (${selectedPeriod.label}) ⏱")
          TrendLineChart(buckets = analytics.trendBuckets, primaryColor = tint.baseColor)
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Best: ${analytics.bestBucketLabel}",
              fontSize = 12.sp,
              color = Color(0xFFA8DFB4)
            )
            Text(
              text = "Worst: ${analytics.worstBucketLabel}",
              fontSize = 12.sp,
              color = Color(0xFFEE9A9A)
            )
          }
        }
      }

      // SECTION D. CALENDAR HEATMAP (Month | Year)
      item(key = "sec_d_heatmap") {
        val currentYm = remember(today, calendarMonthOffset) {
          YearMonth.from(today).plusMonths(calendarMonthOffset.toLong())
        }
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            SectionLabel("CALENDAR HEATMAP")
            HabitRadarSegmentedControl(
              options = listOf(false, true),
              selected = heatmapYearMode,
              labelMapper = { if (it) "Year" else "Month" },
              onSelect = { heatmapYearMode = it },
              height = 36.dp,
              modifier = Modifier.width(150.dp)
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          if (!heatmapYearMode) {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
              IconButton(onClick = { calendarMonthOffset-- }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month", tint = colors.textPrimary)
              }
              Text(
                text = "${currentYm.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentYm.year}",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
              )
              IconButton(
                onClick = { if (calendarMonthOffset < 0) calendarMonthOffset++ },
                enabled = calendarMonthOffset < 0,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month", tint = colors.textPrimary)
              }
            }
            MonthCalendarHeatmap(
              yearMonth = currentYm,
              habit = habit,
              entriesMap = entriesMap,
              pauses = pauses,
              weekStart = settings.weekStart,
              today = today,
              habitColor = tint.baseColor,
              onDayClick = { selectedDaySheetDate = it }
            )
          } else {
            YearGitHubHeatmap(
              today = today,
              color = tint.baseColor,
              isDoneForDate = { d ->
                if (ScheduleMatcher.isEntryDone(habit, entriesMap[d])) 1f else 0f
              }
            )
          }
        }
      }

      // SECTION E. DAY-OF-WEEK PERFORMANCE
      item(key = "sec_e_weekday") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          SectionLabel("DAY-OF-WEEK PERFORMANCE")
          WeekdayPerformanceBars(
            stats = analytics.weekdayStats,
            bestDow = analytics.bestWeekday?.isoDow,
            worstDow = analytics.worstWeekday?.isoDow,
            accentColor = tint.baseColor
          )
          if (analytics.bestWeekday != null && analytics.worstWeekday != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "You're strongest on ${analytics.bestWeekday.fullName}s (${analytics.bestWeekday.rate}%) and weakest on ${analytics.worstWeekday.fullName}s (${analytics.worstWeekday.rate}%).",
              fontSize = 13.sp,
              color = colors.textSecondary
            )
          }
        }
      }

      // SECTION F. TIME-OF-DAY PATTERN
      item(key = "sec_f_time_of_day") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          SectionLabel("TIME-OF-DAY PATTERN")
          HourlyHistogramChart(
            hourlyCounts = analytics.hourlyHistogram,
            accentColor = tint.baseColor
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (analytics.hasEnoughTimestamps) {
              "Usually done around ${analytics.peakWindowLabel}."
            } else {
              "Peak window so far: ${analytics.peakWindowLabel} (log 10+ check-ins for full precision)."
            },
            fontSize = 13.sp,
            color = colors.textSecondary
          )
          if (analytics.avgReminderDelayMinutes != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Average delay after reminder: ${analytics.avgReminderDelayMinutes} min",
              fontSize = 12.sp,
              color = tint.baseColor
            )
          }
        }
      }

      // SECTION G. STREAK HISTORY
      item(key = "sec_g_streak_history") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          SectionLabel("STREAK HISTORY")
          val maxLen = (analytics.streakHistory.maxOfOrNull { it.length } ?: 1).coerceAtLeast(1)
          if (analytics.streakHistory.isEmpty()) {
            Text("No completed streaks yet.", fontSize = 13.sp, color = colors.textSecondary)
          } else {
            analytics.streakHistory.take(5).forEach { interval ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
              ) {
                Text(
                  text = "${interval.startDate.format(DateTimeFormatter.ofPattern("d MMM"))} → ${interval.endDate.format(DateTimeFormatter.ofPattern("d MMM"))}",
                  fontSize = 12.sp,
                  color = colors.textSecondary,
                  modifier = Modifier.width(120.dp)
                )
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.surface2)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth((interval.length.toFloat() / maxLen.toFloat()).coerceIn(0.08f, 1f))
                      .fillMaxHeight()
                      .clip(RoundedCornerShape(5.dp))
                      .background(tint.baseColor)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "${interval.length}d" + if (interval.isActive) " (Active)" else "",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (interval.isActive) tint.baseColor else colors.textPrimary
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "${analytics.streakHistory.size} streaks · Avg ${String.format(Locale.US, "%.1f", analytics.avgStreakLength)}d · Longest gap ${analytics.longestGap}d",
            fontSize = 12.sp,
            color = colors.textSecondary
          )
        }
      }

      // SECTION H. CONSISTENCY & RESILIENCE
      item(key = "sec_h_consistency") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          SectionLabel("CONSISTENCY & RESILIENCE")
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DetailStatBox(
              label = "CONSISTENCY",
              value = "${analytics.consistencyScore}/100",
              bg = tint.subtleElementBg,
              modifier = Modifier.weight(1f)
            )
            DetailStatBox(
              label = "BOUNCE-BACK",
              value = "${analytics.bounceBackRate}%",
              bg = tint.subtleElementBg,
              modifier = Modifier.weight(1f)
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DetailStatBox(
              label = "PERFECT WEEKS / MO",
              value = "${analytics.perfectWeeks}w · ${analytics.perfectMonths}m",
              bg = tint.subtleElementBg,
              modifier = Modifier.weight(1f)
            )
            DetailStatBox(
              label = "RECOVERY TIME",
              value = "${String.format(Locale.US, "%.1f", analytics.recoveryTimeDays)}d",
              bg = tint.subtleElementBg,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // SECTION I. VOLUME CHART ⏱
      item(key = "sec_i_volume") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          SectionLabel("VOLUME vs TARGET (${selectedPeriod.label}) ⏱")
          VolumeBarChart(buckets = analytics.trendBuckets, accentColor = tint.baseColor)
        }
      }

      // SECTION J. AMOUNT / TIME ANALYTICS (only for Amount & Time habits)
      if (habit.trackType != TrackType.TASK && analytics.amountTimeAnalytics != null) {
        val at = analytics.amountTimeAnalytics
        item(key = "sec_j_quantitative") {
          HabitRadarCard(backgroundColor = tint.cardBackground) {
            SectionLabel("${habit.trackType.name} ANALYTICS")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
              DetailStatBox(
                label = "AVG / MEDIAN",
                value = "${String.format(Locale.US, "%.1f", at.average)} / ${String.format(Locale.US, "%.1f", at.median)}",
                bg = tint.subtleElementBg,
                modifier = Modifier.weight(1f)
              )
              DetailStatBox(
                label = "MIN / MAX",
                value = "${at.min.roundToInt()} / ${at.max.roundToInt()}",
                bg = tint.subtleElementBg,
                modifier = Modifier.weight(1f)
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
              DetailStatBox(
                label = "TOTAL (${habit.unit ?: ""})",
                value = at.total.roundToInt().toString(),
                bg = tint.subtleElementBg,
                modifier = Modifier.weight(1f)
              )
              DetailStatBox(
                label = "GOAL-HIT RATE",
                value = "${at.goalHitRate}%",
                bg = tint.subtleElementBg,
                modifier = Modifier.weight(1f)
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Personal best: ${at.personalBestValue.roundToInt()} ${habit.unit ?: ""} (${at.personalBestDate ?: "-"}) · Weekly trend: ${if (at.weeklySlopePercent >= 0) "▲ +${at.weeklySlopePercent}%" else "▼ ${at.weeklySlopePercent}%"}",
              fontSize = 12.sp,
              color = colors.textSecondary
            )
          }
        }
      }

      // SECTION K. MISS ANALYSIS
      item(key = "sec_k_miss") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          SectionLabel("MISS ANALYSIS")
          Text(
            text = "Most-missed weekday: ${analytics.mostMissedWeekday ?: "None yet"} · Skipped days: ${analytics.skippedDaysCount}",
            fontSize = 14.sp,
            color = colors.textPrimary
          )
          if (analytics.postSkipMissPattern != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = analytics.postSkipMissPattern,
              fontSize = 13.sp,
              color = colors.textSecondary
            )
          }
          if (analytics.recentMissDates.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              analytics.recentMissDates.forEach { md ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.subtleElementBg)
                    .clickable { selectedDaySheetDate = md }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = md.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                    fontSize = 12.sp,
                    color = Color(0xFFEE9A9A)
                  )
                }
              }
            }
          }
        }
      }

      // SECTION L. INSIGHTS
      item(key = "sec_l_insights") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          SectionLabel("INSIGHTS")
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            analytics.insights.forEach { insight ->
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(tint.subtleElementBg)
                  .padding(14.dp)
              ) {
                Text(
                  text = insight,
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 14.sp,
                  color = colors.textPrimary
                )
              }
            }
          }
        }
      }

      // SECTION M. NOTES & HISTORY
      item(key = "sec_m_history") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            SectionLabel("NOTES & HISTORY")
            Text(
              text = "+ Add missing entry",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = tint.baseColor,
              modifier = Modifier.clickable { selectedDaySheetDate = today.minusDays(1) }
            )
          }

          val sortedHistory = entries.sortedByDescending { it.date }.take(12)
          if (sortedHistory.isEmpty()) {
            Text("No entries recorded yet.", fontSize = 13.sp, color = colors.textSecondary)
          } else {
            sortedHistory.forEach { e ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedDaySheetDate = e.date }
                  .padding(vertical = 8.dp)
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = e.date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                  )
                  if (!e.note.isNullOrBlank()) {
                    Text(
                      text = e.note,
                      fontSize = 12.sp,
                      color = colors.textSecondary
                    )
                  }
                }
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.subtleElementBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = if (e.status == EntryStatus.SKIPPED) "Skipped" else "Done",
                    fontSize = 12.sp,
                    color = tint.baseColor
                  )
                }
              }
            }
          }
        }
      }

      // SECTION N. REMINDERS (create and manage right on this screen)
      item(key = "sec_n_reminders") {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            SectionLabel("REMINDERS")
            HabitRadarSwitch(
              checked = hasActiveReminders,
              onCheckedChange = { onToggleAllReminders(it) }
            )
          }

          if (reminders.isEmpty()) {
            Text(
              text = "No reminders yet — add a gentle nudge.",
              fontSize = 14.sp,
              color = colors.textSecondary
            )
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              reminders.forEach { r ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint.subtleElementBg)
                    .clickable { editingReminder = r }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = tint.baseColor,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = String.format("%02d:%02d", r.time.hour, r.time.minute),
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold,
                      color = colors.textPrimary
                    )
                    Text(
                      text = if (r.daysOfWeek.size == 7) "Everyday" else r.daysOfWeek.sorted().joinToString(", ") { ScheduleMatcher.dowShortLabel(it) },
                      fontSize = 12.sp,
                      color = colors.textSecondary
                    )
                    if (!r.message.isNullOrBlank()) {
                      Text(
                        text = r.message,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                  HabitRadarSwitch(
                    checked = r.enabled,
                    onCheckedChange = { onSaveReminder(r.copy(enabled = it)) }
                  )
                  IconButton(onClick = { onDeleteReminder(r.id) }) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Delete reminder",
                      tint = colors.textSecondary
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Quick presets chips
          if (reminders.size < 5) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf(
                "Morning 07:00" to LocalTime.of(7, 0),
                "Noon 12:00" to LocalTime.of(12, 0),
                "Evening 20:00" to LocalTime.of(20, 0),
                "Night 22:00" to LocalTime.of(22, 0)
              ).forEach { (label, presetTime) ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint.subtleElementBg)
                    .clickable {
                      onSaveReminder(
                        Reminder(
                          id = UUID.randomUUID().toString(),
                          habitId = habit.id,
                          time = presetTime,
                          daysOfWeek = habit.daysOfWeek.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
                          enabled = true
                        )
                      )
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(text = label, fontSize = 12.sp, color = tint.baseColor, fontWeight = FontWeight.Medium)
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(tint.baseColor)
                .clickable { showNewReminderSheet = true }
            ) {
              Text(
                text = "+ Add reminder",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101114)
              )
            }
          }
        }
      }
    }
  }

  if (selectedDaySheetDate != null) {
    val date = selectedDaySheetDate!!
    val existing = entriesMap[date]
    DayEntryEditorSheet(
      date = date,
      existingEntry = existing,
      onDismiss = { selectedDaySheetDate = null },
      onMarkDone = { note ->
        onToggleDate(date, note)
        selectedDaySheetDate = null
      },
      onMarkSkipped = {
        onSkipDate(date)
        selectedDaySheetDate = null
      },
      onClear = {
        onDeleteDateEntry(date)
        selectedDaySheetDate = null
      }
    )
  }

  if (showNewReminderSheet || editingReminder != null) {
    ReminderEditorBottomSheet(
      initialReminder = editingReminder,
      habitId = habit.id,
      habitName = habit.name,
      defaultDays = habit.daysOfWeek,
      onDismiss = {
        showNewReminderSheet = false
        editingReminder = null
      },
      onSave = { saved ->
        onSaveReminder(saved)
        showNewReminderSheet = false
        editingReminder = null
      },
      onDelete = { toDel ->
        onDeleteReminder(toDel.id)
        editingReminder = null
      }
    )
  }
}

@Composable
private fun DetailStatBox(
  label: String,
  value: String,
  bg: Color,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(bg)
      .padding(14.dp)
  ) {
    Text(
      text = label,
      fontSize = 10.sp,
      letterSpacing = 1.5.sp,
      fontWeight = FontWeight.SemiBold,
      color = colors.textSecondary
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = value,
      fontFamily = InstrumentSerifFamily,
      fontSize = 24.sp,
      color = colors.textPrimary
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayEntryEditorSheet(
  date: LocalDate,
  existingEntry: Entry?,
  onDismiss: () -> Unit,
  onMarkDone: (String?) -> Unit,
  onMarkSkipped: () -> Unit,
  onClear: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var note by remember { mutableStateOf(existingEntry?.note ?: "") }

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
        text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
        fontFamily = InstrumentSerifFamily,
        fontSize = 26.sp,
        color = colors.textPrimary
      )
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = note,
        onValueChange = { note = it.take(200) },
        label = { Text("Note for this day") },
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.accent)
            .clickable { onMarkDone(note.trim().ifBlank { null }) }
        ) {
          Text("Mark Done", color = colors.onAccent, fontWeight = FontWeight.Bold)
        }
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface2)
            .clickable(onClick = onMarkSkipped)
        ) {
          Text("Skip", color = colors.textPrimary, fontWeight = FontWeight.Medium)
        }
        if (existingEntry != null) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(Color(0x26EE9A9A))
              .clickable(onClick = onClear)
          ) {
            Text("Clear", color = Color(0xFFEE9A9A), fontWeight = FontWeight.Medium)
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
