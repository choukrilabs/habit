package com.example.feature.extras

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.HabitRadarDatabase
import com.example.core.data.db.toDomain
import com.example.core.di.appContainer
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.Entry
import com.example.core.domain.model.Goal
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitTemplate
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.Step
import com.example.core.domain.model.WeekStartDay
import com.example.core.domain.model.WidgetCornerStyle
import com.example.core.domain.model.WidgetThemeMode
import com.example.core.domain.usecase.GoalStepEvaluator
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.ui.components.HabitIconBadge
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.components.SubPageTopBar
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.InstrumentSerif
import com.example.core.ui.theme.formatSerifHeadline
import com.example.core.ui.theme.habitColor
import com.example.core.ui.theme.resolveAppAccent
import com.example.feature.backup.BackupManager
import com.example.feature.backup.BackupPreviewInfo
import com.example.feature.backup.RestoreMode
import com.example.feature.onboarding.StarterTemplatesCatalog
import com.example.feature.widgets.glance.GoalProgressWidgetReceiver
import com.example.feature.widgets.glance.ProgressRingWidgetReceiver
import com.example.feature.widgets.glance.SingleHabitWidgetReceiver
import com.example.feature.widgets.glance.TodayListWidgetReceiver
import com.example.feature.widgets.glance.WeekGridWidgetReceiver
import com.example.feature.widgets.glance.WidgetUpdater
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.lerp
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.TrackType
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.StreakCalculator
import com.example.core.ui.components.HabitIconView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetsStudioScreen(
  habits: List<Habit>,
  entries: List<Entry>,
  goals: List<Goal>,
  stepsByGoal: Map<String, List<Step>>,
  settings: AppSettings,
  today: LocalDate,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onBack: () -> Unit,
  onToggleHabitOnDate: (Habit, LocalDate) -> Unit = { _, _ -> },
  onToggleGoalStep: (Step) -> Unit = {}
) {
  val colors = LocalHabitRadarColors.current
  val accent = resolveAppAccent(settings.accentColor)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedWidgetToPin by remember { mutableIntStateOf(0) }
  var showManualFallbackSheet by remember { mutableStateOf(false) }
  var showLargeHabitListPreview by remember { mutableStateOf(false) }
  var refreshFeedback by remember { mutableStateOf<String?>(null) }

  // Local interactive state for showcase fallback habits when DB doesn't have them yet
  val demoToggledKeys = remember { mutableStateListOf<String>() }

  val entriesByHabit = remember(entries) {
    entries.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.date } }
  }
  val entriesListByHabit = remember(entries) {
    entries.groupBy { it.habitId }
  }

  val realHabitIds = remember(habits) { habits.map { it.id }.toSet() }

  // Build showcase fallback habits matching Screenshot 1 & Screenshot 2
  val showcaseHabits = remember(today) {
    listOf(
      Habit(
        id = "demo_walk",
        name = "Walk",
        iconKey = "walk",
        colorKey = "#6BBF79",
        repeatType = RepeatType.DAILY,
        daysOfWeek = listOf(1, 4, 7),
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 0
      ),
      Habit(
        id = "demo_lights_out",
        name = "Lights out by 12",
        iconKey = "moon",
        colorKey = "#A98DF0",
        repeatType = RepeatType.DAILY,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 1
      ),
      Habit(
        id = "demo_log_spent",
        name = "Log what I spent",
        iconKey = "savings",
        colorKey = "#EBA85E",
        repeatType = RepeatType.DAILY,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 2
      ),
      Habit(
        id = "demo_fajr",
        name = "Fajr on time",
        iconKey = "sunrise",
        colorKey = "#6BC5B8",
        repeatType = RepeatType.DAILY,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 3
      ),
      Habit(
        id = "demo_quran",
        name = "Read Quran",
        iconKey = "book",
        colorKey = "#7FD18E",
        repeatType = RepeatType.DAILY,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 4
      ),
      Habit(
        id = "demo_morning_adhkar",
        name = "Morning adhkar",
        iconKey = "sun",
        colorKey = "#E8C56B",
        repeatType = RepeatType.DAILY,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 5
      ),
      Habit(
        id = "demo_evening_adhkar",
        name = "Evening adhkar",
        iconKey = "night",
        colorKey = "#A98DF0",
        repeatType = RepeatType.DAILY,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 6
      ),
      Habit(
        id = "demo_sadaqah",
        name = "Give sadaqah weekly",
        iconKey = "charity",
        colorKey = "#E899B8",
        repeatType = RepeatType.WEEKLY,
        timesPerPeriod = 1,
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 7
      ),
      Habit(
        id = "demo_workout",
        name = "Workout",
        iconKey = "workout",
        colorKey = "#6BBF79",
        repeatType = RepeatType.DAILY,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 8
      ),
      Habit(
        id = "demo_stretch",
        name = "Morning Stretch",
        iconKey = "emoji:🙏",
        colorKey = "#7FD18E",
        repeatType = RepeatType.DAILY,
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        trackType = TrackType.TASK,
        startDate = today.minusDays(90),
        sortOrder = 9
      )
    )
  }

  // Compute Monday..Sunday week dates ending at the current week's Sunday
  val weekDays = remember(today, settings.weekStart) {
    val start = LogicalDateCalculator.startOfWeek(today, WeekStartDay.MONDAY)
    (0..6).map { start.plusDays(it.toLong()) }
  }
  val saturdayDate = remember(weekDays) { weekDays.getOrElse(5) { today.minusDays(1) } }

  // Combine user's real habits with showcase habits when needed so all 4 widget cards look complete
  val displayHabitsForShowcase = remember(habits, showcaseHabits) {
    if (habits.size >= 4) {
      habits
    } else {
      val existingNames = habits.map { it.name.lowercase() }.toSet()
      habits + showcaseHabits.filter { it.name.lowercase() !in existingNames }
    }
  }

  val primarySingleHabit = remember(habits, showcaseHabits) {
    habits.firstOrNull { it.name.equals("Walk", ignoreCase = true) }
      ?: habits.firstOrNull()
      ?: showcaseHabits.first()
  }

  fun isHabitDoneOn(habit: Habit, date: LocalDate): Boolean {
    val toggleKey = "${habit.id}_$date"
    val baseDone = if (habit.id in realHabitIds) {
      ScheduleMatcher.isEntryDone(habit, entriesByHabit[habit.id]?.get(date))
    } else {
      // In Screenshot 1 & 2, Saturday (index 5) is completed for habits 1..7 (Lights out, Log spent, Fajr, Quran, Adhkar, Sadaqah)
      date == saturdayDate && habit.id in setOf(
        "demo_lights_out",
        "demo_log_spent",
        "demo_fajr",
        "demo_quran",
        "demo_morning_adhkar",
        "demo_evening_adhkar",
        "demo_sadaqah"
      )
    }
    return if (toggleKey in demoToggledKeys) !baseDone else baseDone
  }

  fun getHabitStreak(habit: Habit): Int {
    if (habit.id in realHabitIds) {
      val s = StreakCalculator.calculateStreaks(
        habit = habit,
        entries = entriesListByHabit[habit.id].orEmpty(),
        pauses = emptyList(),
        today = today
      )
      return s.currentStreak
    }
    val baseStreak = if (habit.id in setOf(
        "demo_lights_out",
        "demo_log_spent",
        "demo_fajr",
        "demo_quran",
        "demo_morning_adhkar",
        "demo_evening_adhkar",
        "demo_sadaqah"
      )
    ) 1 else 0
    val todayToggled = "${habit.id}_$today" in demoToggledKeys
    return if (todayToggled) baseStreak + 1 else baseStreak
  }

  val handleToggleHabit: (Habit, LocalDate) -> Unit = { habit, date ->
    if (habit.id in realHabitIds) {
      onToggleHabitOnDate(habit, date)
      scope.launch {
        context.appContainer.repository.toggleHabitDoneOnDate(habit.id, date)
        WidgetUpdater.updateAll(context)
      }
    } else {
      val key = "${habit.id}_$date"
      if (key in demoToggledKeys) demoToggledKeys.remove(key) else demoToggledKeys.add(key)
    }
  }

  val handlePinWidget: (Class<*>) -> Unit = { receiverClass ->
    val mgr = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, receiverClass)
    if (mgr.isRequestPinAppWidgetSupported) {
      val pinned = mgr.requestPinAppWidget(provider, null, null)
      if (!pinned) showManualFallbackSheet = true
    } else {
      showManualFallbackSheet = true
    }
  }

  val radiusDp = when (settings.defaultWidgetCorner) {
    WidgetCornerStyle.ROUNDED -> 24.dp
    WidgetCornerStyle.EXTRA -> 30.dp
  }

  Scaffold(
    containerColor = Color(0xFF000000),
    topBar = {
      SubPageTopBar(title = "Widgets & sharing", onBack = onBack)
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
    ) {
      // 1. HABIT — Consistency Heatmap with a check button (small & medium)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          HabitHeatmapShowcaseCard(
            habit = primarySingleHabit,
            streak = getHabitStreak(primarySingleHabit),
            doneToday = isHabitDoneOn(primarySingleHabit, today),
            isDoneOnDate = { d -> isHabitDoneOn(primarySingleHabit, d) },
            today = today,
            weekDays = weekDays,
            cornerRadius = radiusDp,
            showStreak = settings.defaultWidgetShowStreak,
            hideNames = settings.defaultWidgetHideNames,
            onToggleToday = { handleToggleHabit(primarySingleHabit, today) }
          )
          Text(
            text = "Habit — consistency heatmap with a check button (small & medium).",
            color = Color(0xFF8E8E96),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
        }
      }

      // 2. HABIT WEEK — This week at a glance; tap today's circle to complete
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          HabitWeekShowcaseCard(
            habit = primarySingleHabit,
            streak = getHabitStreak(primarySingleHabit),
            weekDays = weekDays,
            today = today,
            isDoneOnDate = { d -> isHabitDoneOn(primarySingleHabit, d) },
            cornerRadius = radiusDp,
            showStreak = settings.defaultWidgetShowStreak,
            hideNames = settings.defaultWidgetHideNames,
            onToggleDate = { d -> handleToggleHabit(primarySingleHabit, d) }
          )
          Text(
            text = "Habit Week — this week at a glance; tap today's circle to complete.",
            color = Color(0xFF8E8E96),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
        }
      }

      // 3. HABIT LIST — Up to 10 habits with this week's days (medium & large)
      item {
        val listHabits = if (showLargeHabitListPreview || habits.size > 3) {
          displayHabitsForShowcase.take(10)
        } else {
          displayHabitsForShowcase.take(3)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          HabitListShowcaseCard(
            habits = listHabits,
            weekDays = weekDays,
            today = today,
            showDayHeader = showLargeHabitListPreview || habits.size > 3,
            getStreak = { h -> getHabitStreak(h) },
            isDoneOnDate = { h, d -> isHabitDoneOn(h, d) },
            cornerRadius = radiusDp,
            showStreak = settings.defaultWidgetShowStreak,
            hideNames = settings.defaultWidgetHideNames,
            allowBackfill = settings.defaultWidgetAllowBackfill,
            onToggleHabitOnDate = handleToggleHabit
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Habit List — up to 10 habits with this week's days (medium & large).",
              color = Color(0xFF8E8E96),
              fontSize = 13.sp,
              lineHeight = 18.sp,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = if (showLargeHabitListPreview) "Medium" else "Large (10)",
              color = accent,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier
                .padding(start = 8.dp)
                .clickable { showLargeHabitListPreview = !showLargeHabitListPreview }
            )
          }
        }
      }

      // 4. HABIT RINGS — Up to 8 habits as progress rings with 🔥 streak badges
      item {
        val ringHabits = displayHabitsForShowcase.take(4)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          HabitRingsShowcaseCard(
            habits = ringHabits,
            weekDays = weekDays,
            today = today,
            getStreak = { h -> getHabitStreak(h) },
            isDoneOnDate = { h, d -> isHabitDoneOn(h, d) },
            cornerRadius = radiusDp,
            showStreak = settings.defaultWidgetShowStreak,
            onToggleHabitToday = { h -> handleToggleHabit(h, today) }
          )
          Text(
            text = "Habit Rings — up to 8 habits as progress rings with 🔥 streak badges. Home screen on Android, lock screen on iPhone.",
            color = Color(0xFF8E8E96),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
        }
      }

      // 5. PIN WIDGET TO HOME SCREEN SECTION
      item {
        val widgetPinOptions = listOf(
          "Habit" to SingleHabitWidgetReceiver::class.java,
          "Habit Week" to TodayListWidgetReceiver::class.java,
          "Habit List" to WeekGridWidgetReceiver::class.java,
          "Habit Rings" to ProgressRingWidgetReceiver::class.java
        )
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          SectionLabel("ADD WIDGET TO HOME SCREEN")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            widgetPinOptions.forEachIndexed { idx, (label, _) ->
              val selected = selectedWidgetToPin == idx
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(50))
                  .background(if (selected) accent else Color(0xFF202026))
                  .clickable { selectedWidgetToPin = idx }
                  .padding(vertical = 8.dp, horizontal = 4.dp)
              ) {
                Text(
                  text = label,
                  color = if (selected) Color(0xFF0A0A0D) else Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1
                )
              }
            }
          }
          Button(
            onClick = {
              val receiverClass = widgetPinOptions[selectedWidgetToPin].second
              handlePinWidget(receiverClass)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = accent,
              contentColor = Color(0xFF0A0A0D)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("add_widget_home_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Add ${widgetPinOptions[selectedWidgetToPin].first} widget to home screen",
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // 6. Interactive Quick Mark-Done Widget Checklist Card
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("widgets_quick_checkin_card"),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          val doneCount = habits.count { h ->
            ScheduleMatcher.isEntryDone(h, entriesByHabit[h.id]?.get(today))
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            SectionLabel("MARK DONE IN WIDGET")
            Text(
              text = "$doneCount of ${habits.size} finished today",
              color = accent,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            text = "Tap any circle or check button on the widgets above — or tap a habit below — to mark it done.",
            color = Color(0xFF8E8E96),
            fontSize = 12.sp
          )

          if (habits.isEmpty()) {
            Text(
              text = "Showing interactive preview habits above. Add your own habits on Today to sync them with your widgets.",
              color = Color(0xFF8E8E96),
              fontSize = 13.sp
            )
          } else {
            habits.forEach { h ->
              val isDone = ScheduleMatcher.isEntryDone(h, entriesByHabit[h.id]?.get(today))
              val hColor = habitColor(h.colorKey)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFF1D1D22))
                  .clickable { handleToggleHabit(h, today) }
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("widget_quick_complete_${h.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  HabitIconBadge(iconKey = h.iconKey, colorKey = h.colorKey, size = 32.dp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = h.name,
                      color = if (isDone) Color(0xFF8E8E96) else Color.White,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.SemiBold,
                      textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                      text = if (isDone) "Finished for today" else "Tap to mark done",
                      color = if (isDone) hColor else Color(0xFF8E8E96),
                      fontSize = 11.sp
                    )
                  }
                }
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isDone) hColor else hColor.copy(alpha = 0.16f))
                    .border(1.dp, hColor, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDone) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color(0xFF0A0A0D),
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Done",
                        color = Color(0xFF0A0A0D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                      )
                    } else {
                      Text(
                        text = "Mark done",
                        color = hColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 7. WIDGET STYLE Card
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          SectionLabel("WIDGET STYLE")

          Text("Theme", color = Color(0xFF8E8E96), fontSize = 12.sp)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetThemeMode.entries.forEach { tm ->
              val selected = settings.defaultWidgetTheme == tm
              val label = when (tm) {
                WidgetThemeMode.FOLLOW -> "Follow app"
                WidgetThemeMode.DARK -> "Dark"
                WidgetThemeMode.LIGHT -> "Light"
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .background(if (selected) accent else Color(0xFF202026))
                  .clickable { onUpdateSettings { it.copy(defaultWidgetTheme = tm) } }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  text = label,
                  color = if (selected) Color(0xFF0A0A0D) else Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Background opacity", color = Color(0xFF8E8E96), fontSize = 12.sp)
            Text("${settings.defaultWidgetOpacity}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Slider(
            value = settings.defaultWidgetOpacity.toFloat(),
            onValueChange = { v -> onUpdateSettings { it.copy(defaultWidgetOpacity = v.roundToInt().coerceIn(60, 100)) } },
            valueRange = 60f..100f,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
          )

          Text("Corner style", color = Color(0xFF8E8E96), fontSize = 12.sp)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetCornerStyle.entries.forEach { cs ->
              val selected = settings.defaultWidgetCorner == cs
              val label = if (cs == WidgetCornerStyle.ROUNDED) "Rounded" else "Extra rounded"
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .background(if (selected) accent else Color(0xFF202026))
                  .clickable { onUpdateSettings { it.copy(defaultWidgetCorner = cs) } }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  text = label,
                  color = if (selected) Color(0xFF0A0A0D) else Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Show streak fire", color = Color.White, fontSize = 13.sp)
            Switch(
              checked = settings.defaultWidgetShowStreak,
              onCheckedChange = { v -> onUpdateSettings { it.copy(defaultWidgetShowStreak = v) } },
              colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color(0xFF0A0A0D))
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Hide habit names (privacy)", color = Color.White, fontSize = 13.sp)
            Switch(
              checked = settings.defaultWidgetHideNames,
              onCheckedChange = { v -> onUpdateSettings { it.copy(defaultWidgetHideNames = v) } },
              colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color(0xFF0A0A0D))
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Allow past-day backfill on grid", color = Color.White, fontSize = 13.sp)
            Switch(
              checked = settings.defaultWidgetAllowBackfill,
              onCheckedChange = { v -> onUpdateSettings { it.copy(defaultWidgetAllowBackfill = v) } },
              colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color(0xFF0A0A0D))
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = refreshFeedback ?: "Home screen widgets update automatically.",
              color = Color(0xFF8E8E96),
              fontSize = 12.sp,
              modifier = Modifier.weight(1f)
            )
            TextButton(
              onClick = {
                scope.launch {
                  WidgetUpdater.updateAll(context)
                  refreshFeedback = "All home screen widgets refreshed."
                }
              }
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Refresh all", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }

  if (showManualFallbackSheet) {
    ModalBottomSheet(
      onDismissRequest = { showManualFallbackSheet = false },
      containerColor = colors.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = formatSerifHeadline("Add widget *manually.*", accent),
          color = colors.textPrimary,
          fontFamily = InstrumentSerif,
          fontSize = 26.sp
        )
        Text("1. Long-press an empty area on your Android Home Screen.", color = colors.textPrimary, fontSize = 14.sp)
        Text("2. Tap 'Widgets' and scroll to find Habit Radar.", color = colors.textPrimary, fontSize = 14.sp)
        Text("3. Drag your preferred widget size onto your Home Screen.", color = colors.textPrimary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { showManualFallbackSheet = false },
          colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0A0D)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Got it", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Widget Card 1: "Habit — consistency heatmap with a check button (small & medium)."
 */
@Composable
private fun HabitHeatmapShowcaseCard(
  habit: Habit,
  streak: Int,
  doneToday: Boolean,
  isDoneOnDate: (LocalDate) -> Boolean,
  today: LocalDate,
  weekDays: List<LocalDate>,
  cornerRadius: androidx.compose.ui.unit.Dp,
  showStreak: Boolean,
  hideNames: Boolean,
  onToggleToday: () -> Unit
) {
  val hColor = habitColor(habit.colorKey)
  val cardBg = lerp(Color(0xFF101311), hColor, 0.11f)
  val weekEnd = weekDays.lastOrNull() ?: today
  val columns = 15
  val rows = 7

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(cornerRadius))
      .background(cardBg)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Header Row: Tinted Icon Badge + Title/Streak + Circular Check Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(hColor.copy(alpha = 0.20f))
      ) {
        HabitIconView(
          iconKey = habit.iconKey,
          tint = hColor,
          modifier = Modifier.size(20.dp),
          emojiSize = 18.sp
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (hideNames) "Habit" else habit.name,
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (showStreak) {
          Text(
            text = "🔥 $streak",
            color = Color(0xFFA0A8A2),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(if (doneToday) hColor else hColor.copy(alpha = 0.20f))
          .clickable(onClick = onToggleToday)
          .testTag("widget_single_mark_done_btn")
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Mark ${habit.name} done",
          tint = if (doneToday) Color(0xFF0E1510) else Color(0xFFC2D4C6),
          modifier = Modifier.size(17.dp)
        )
      }
    }

    // 7 x 15 Consistency Heatmap Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      for (col in 0 until columns) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          for (row in 0 until rows) {
            val daysAgo = (columns - 1 - col) * 7 + (rows - 1 - row)
            val cellDate = weekEnd.minusDays(daysAgo.toLong())
            val isBottomRightCell = col == columns - 1 && row == rows - 1
            val isCellActive = isDoneOnDate(cellDate) ||
              (cellDate == today && doneToday) ||
              (isBottomRightCell && (doneToday || habit.id == "demo_walk"))

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(3.5.dp))
                .background(
                  if (isCellActive) hColor else hColor.copy(alpha = 0.16f)
                )
            )
          }
        }
      }
    }
  }
}

/**
 * Widget Card 2: "Habit Week — this week at a glance; tap today's circle to complete."
 */
@Composable
private fun HabitWeekShowcaseCard(
  habit: Habit,
  streak: Int,
  weekDays: List<LocalDate>,
  today: LocalDate,
  isDoneOnDate: (LocalDate) -> Boolean,
  cornerRadius: androidx.compose.ui.unit.Dp,
  showStreak: Boolean,
  hideNames: Boolean,
  onToggleDate: (LocalDate) -> Unit
) {
  val hColor = habitColor(habit.colorKey)
  val cardBg = lerp(Color(0xFF101311), hColor, 0.11f)
  val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

  // Highlight today's column (or Sunday index 6 if today is outside weekDays)
  val highlightedIndex = remember(weekDays, today) {
    val idx = weekDays.indexOf(today)
    if (idx >= 0) idx else 6
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(cornerRadius))
      .background(cardBg)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Top Row: Icon Badge + Title/Streak
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(11.dp))
          .background(hColor.copy(alpha = 0.20f))
      ) {
        HabitIconView(
          iconKey = habit.iconKey,
          tint = hColor,
          modifier = Modifier.size(22.dp),
          emojiSize = 19.sp
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = if (hideNames) "Habit" else habit.name,
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (showStreak) {
          Text(
            text = "🔥 $streak",
            color = Color(0xFFA0A8A2),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    // Bottom Row: 7 Day Columns with vertical rounded pill on Today
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      weekDays.forEachIndexed { idx, date ->
        val isHighlightedToday = idx == highlightedIndex
        val done = isDoneOnDate(date)
        val label = dayLabels.getOrElse(idx) { ScheduleMatcher.dowShortLabel(date.dayOfWeek.value) }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
              if (isHighlightedToday) hColor.copy(alpha = 0.18f) else Color.Transparent
            )
            .clickable { onToggleDate(date) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(
            text = label,
            color = if (isHighlightedToday) Color.White else Color(0xFF8B998E),
            fontSize = 11.sp,
            fontWeight = if (isHighlightedToday) FontWeight.Bold else FontWeight.Medium
          )
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(26.dp)
              .clip(CircleShape)
              .background(if (done) hColor else Color.Transparent)
              .border(
                width = if (isHighlightedToday) 2.dp else 1.5.dp,
                color = when {
                  done -> hColor
                  isHighlightedToday -> hColor
                  else -> hColor.copy(alpha = 0.42f)
                },
                shape = CircleShape
              )
          ) {
            if (done) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Done",
                tint = Color(0xFF0E1510),
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Widget Card 3: "Habit List — up to 10 habits with this week's days (medium & large)."
 * Matches both Screenshot 1 (Card 3) and Screenshot 2 (Large 10-habit home screen widget).
 */
@Composable
private fun HabitListShowcaseCard(
  habits: List<Habit>,
  weekDays: List<LocalDate>,
  today: LocalDate,
  showDayHeader: Boolean,
  getStreak: (Habit) -> Int,
  isDoneOnDate: (Habit, LocalDate) -> Boolean,
  cornerRadius: androidx.compose.ui.unit.Dp,
  showStreak: Boolean,
  hideNames: Boolean,
  allowBackfill: Boolean,
  onToggleHabitOnDate: (Habit, LocalDate) -> Unit
) {
  val shortLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
  val highlightedDayIndex = remember(weekDays, today) {
    val idx = weekDays.indexOf(today)
    if (idx >= 0) idx else 6
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(cornerRadius))
      .background(Color(0xFF141416))
      .padding(horizontal = 14.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    if (showDayHeader) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          weekDays.forEachIndexed { idx, _ ->
            val isTodayCol = idx == highlightedDayIndex
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.width(19.dp)
            ) {
              Text(
                text = shortLabels.getOrElse(idx) { "" },
                color = if (isTodayCol) Color.White else Color(0xFF7E7E86),
                fontSize = 11.sp,
                fontWeight = if (isTodayCol) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }
      }
    }

    habits.forEachIndexed { idx, habit ->
      val hColor = habitColor(habit.colorKey)
      val streak = getStreak(habit)
      val displayTitle = if (hideNames) "Habit ${idx + 1}" else habit.name

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("widget_grid_row_${habit.id}"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: 32dp rounded-square tinted icon badge
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(hColor.copy(alpha = 0.20f))
            .clickable { onToggleHabitOnDate(habit, today) }
        ) {
          HabitIconView(
            iconKey = habit.iconKey,
            tint = hColor,
            modifier = Modifier.size(18.dp),
            emojiSize = 15.sp
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Middle: Habit Name
        Text(
          text = displayTitle,
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f)
            .clickable { onToggleHabitOnDate(habit, today) }
        )

        // Streak label ("🔥 0" / "🔥 1")
        if (showStreak) {
          Text(
            text = "🔥 $streak",
            color = Color(0xFF9A9AA2),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp)
          )
        }

        // Right: 7 day circles
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          weekDays.forEachIndexed { dayIdx, d ->
            val done = isDoneOnDate(habit, d)
            val isTodayCol = dayIdx == highlightedDayIndex
            val canToggle = isTodayCol || allowBackfill

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(19.dp)
                .clip(CircleShape)
                .background(
                  when {
                    done -> hColor
                    isTodayCol -> hColor.copy(alpha = 0.12f)
                    else -> hColor.copy(alpha = 0.22f)
                  }
                )
                .then(
                  if (isTodayCol && !done) {
                    Modifier.border(1.6.dp, hColor, CircleShape)
                  } else {
                    Modifier
                  }
                )
                .let { mod ->
                  if (canToggle) mod.clickable { onToggleHabitOnDate(habit, d) } else mod
                }
                .testTag("widget_grid_cell_${habit.id}_$d")
            ) {
              if (done) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Done",
                  tint = Color(0xFF111216),
                  modifier = Modifier.size(11.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Widget Card 4: "Habit Rings — up to 8 habits as progress rings with 🔥 streak badges."
 */
@Composable
private fun HabitRingsShowcaseCard(
  habits: List<Habit>,
  weekDays: List<LocalDate>,
  today: LocalDate,
  getStreak: (Habit) -> Int,
  isDoneOnDate: (Habit, LocalDate) -> Boolean,
  cornerRadius: androidx.compose.ui.unit.Dp,
  showStreak: Boolean,
  onToggleHabitToday: (Habit) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(cornerRadius))
      .background(Color(0xFF141416))
      .padding(horizontal = 18.dp, vertical = 18.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      habits.take(4).forEach { habit ->
        val hColor = habitColor(habit.colorKey)
        val streak = getStreak(habit)
        val doneDaysInWeek = weekDays.count { d -> isDoneOnDate(habit, d) }
        val targetDays = when (habit.repeatType) {
          RepeatType.WEEKLY -> (habit.timesPerPeriod ?: 1).coerceAtLeast(1)
          else -> habit.daysOfWeek.size.coerceAtLeast(1)
        }
        val progress = (doneDaysInWeek.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(64.dp)
            .clickable { onToggleHabitToday(habit) }
            .testTag("widget_ring_item_${habit.id}")
        ) {
          Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
            val strokePx = 5.5.dp.toPx()
            drawArc(
              color = hColor.copy(alpha = 0.22f),
              startAngle = 0f,
              sweepAngle = 360f,
              useCenter = false,
              style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
              drawArc(
                color = hColor,
                startAngle = -90f,
                sweepAngle = (360f * progress).coerceAtLeast(24f),
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
              )
            }
          }

          HabitIconView(
            iconKey = habit.iconKey,
            tint = hColor,
            modifier = Modifier.size(22.dp),
            emojiSize = 18.sp
          )

          if (showStreak && streak > 0) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .padding(horizontal = 6.dp, vertical = 1.5.dp)
            ) {
              Text(
                text = "🔥 $streak",
                color = Color(0xFF111114),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 12.sp
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
  db: HabitRadarDatabase,
  settingsStore: SettingsDataStore,
  settings: AppSettings,
  habitsCount: Int,
  entriesCount: Int,
  goalsCount: Int,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onBack: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val accent = resolveAppAccent(settings.accentColor)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val authService = remember(context) { context.appContainer.authService }
  val cloudSyncRepo = remember(context) { context.appContainer.cloudSyncRepository }
  val currentUser by authService.authStateFlow().collectAsState(initial = authService.currentUser)
  val isSignedIn = currentUser != null

  var isSigningInOrSyncing by remember { mutableStateOf(false) }
  var includeSettings by remember { mutableStateOf(true) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var previewInfo by remember { mutableStateOf<BackupPreviewInfo?>(null) }

  val snapshotFile = remember { File(context.filesDir, "pre_restore_snapshot.json") }
  var hasUndoSnapshot by remember { mutableStateOf(snapshotFile.exists()) }

  val createJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val res = BackupManager.exportJsonToUri(context, uri, db, settingsStore, includeSettings)
        statusMessage = res.fold(
          onSuccess = { "Backup exported (.json) with SHA-256 checksum." },
          onFailure = { "Export failed: ${it.message}" }
        )
      }
    }
  }

  val createCsvZipLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/zip")
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val res = BackupManager.exportCsvZipToUri(context, uri, db)
        statusMessage = res.fold(
          onSuccess = { "CSV archive exported (habits.csv + entries.csv)." },
          onFailure = { "CSV export failed: ${it.message}" }
        )
      }
    }
  }

  val openJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val rawText = runCatching {
          context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Unable to read file")
        }
        rawText.fold(
          onSuccess = { jsonStr ->
            val validated = BackupManager.validateAndPreviewBackupJson(jsonStr)
            validated.fold(
              onSuccess = { info -> previewInfo = info },
              onFailure = { err -> statusMessage = err.message ?: "Invalid backup file." }
            )
          },
          onFailure = { err -> statusMessage = "Could not open file: ${err.message}" }
        )
      }
    }
  }

  val folderPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      onUpdateSettings { it.copy(autoBackupFolderUri = uri.toString(), autoBackupEnabled = true) }
      statusMessage = "Auto-backup folder selected."
    }
  }

  val dateStamp = remember { LocalDate.now().toString() }

  Scaffold(
    containerColor = colors.background,
    topBar = {
      SubPageTopBar(title = "Backup & restore", onBack = onBack)
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
      // Cloud Account Gate Card
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(
              1.dp,
              if (isSignedIn) accent.copy(alpha = 0.5f) else colors.danger.copy(alpha = 0.45f),
              RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
            .testTag("backup_auth_card"),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          SectionLabel(if (isSignedIn) "GOOGLE CLOUD BACKUP ACTIVE" else "SIGN-IN REQUIRED FOR BACKUP")
          if (!isSignedIn) {
            Text(
              text = "Backup is locked until you sign in",
              color = colors.textPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "You entered without signing in. Your habits and widgets work offline on this device, but Cloud Backup, Export, Restore, and Auto-Backup require signing in with Google.",
              color = colors.textSecondary,
              fontSize = 13.sp
            )
            Button(
              onClick = {
                isSigningInOrSyncing = true
                statusMessage = null
                scope.launch {
                  val res = authService.signInWithGoogle(context)
                  val user = res.getOrNull()
                  if (user != null) {
                    val allHabits = db.habitDao().getAllHabits().map { it.toDomain() }
                    val allEntries = db.entryDao().getAllEntries().map { it.toDomain() }
                    cloudSyncRepo.syncAllToCloud(allHabits, allEntries)
                    onUpdateSettings { it.copy(lastBackupAt = Instant.now()) }
                    statusMessage = "Signed in as ${user.email ?: user.displayName ?: "Google user"}. Backup unlocked & synced!"
                  } else {
                    res.exceptionOrNull()?.let { err ->
                      statusMessage = err.localizedMessage ?: "Sign-in was not completed."
                    }
                  }
                  isSigningInOrSyncing = false
                }
              },
              enabled = !isSigningInOrSyncing,
              colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0A0D)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("backup_sign_in_btn")
            ) {
              Text(
                text = if (isSigningInOrSyncing) "Connecting..." else "Sign in with Google to enable backup",
                fontWeight = FontWeight.Bold
              )
            }
          } else {
            Text(
              text = currentUser?.displayName ?: currentUser?.email ?: "Signed in with Google",
              color = colors.textPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${currentUser?.email ?: "Cloud account connected"} • Cloud Sync & Backup unlocked",
              color = accent,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Button(
              onClick = {
                isSigningInOrSyncing = true
                scope.launch {
                  val allHabits = db.habitDao().getAllHabits().map { it.toDomain() }
                  val allEntries = db.entryDao().getAllEntries().map { it.toDomain() }
                  val syncRes = cloudSyncRepo.syncAllToCloud(allHabits, allEntries)
                  isSigningInOrSyncing = false
                  statusMessage = syncRes.fold(
                    onSuccess = { count ->
                      onUpdateSettings { it.copy(lastBackupAt = Instant.now()) }
                      "Backed up $count habits and ${allEntries.size} check-ins to Cloud."
                    },
                    onFailure = { "Cloud backup failed: ${it.message}" }
                  )
                }
              },
              enabled = !isSigningInOrSyncing,
              colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0A0D)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("cloud_sync_now_btn")
            ) {
              Text(
                text = if (isSigningInOrSyncing) "Syncing..." else "Backup & Sync to Cloud now",
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Status Card
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          SectionLabel("LOCAL STORAGE STATUS")
          val lastBackupLabel = settings.lastBackupAt?.let { inst ->
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
              .withZone(ZoneId.systemDefault())
              .format(inst)
          } ?: "Never"
          Text("Last backup: $lastBackupLabel", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(
            text = "$habitsCount habits • $entriesCount check-ins • $goalsCount goals",
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = if (isSignedIn) "Synced with your Google account and stored locally." else "Stored locally only. Sign in above to unlock backups.",
            color = colors.textSecondary,
            fontSize = 12.sp
          )
          if (statusMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(statusMessage!!, color = colors.success, fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
        }
      }

      // Undo Pre-Restore Snapshot Banner (if available)
      if (hasUndoSnapshot && isSignedIn) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(colors.surfaceElevated)
              .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Pre-restore safety snapshot available", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("You can undo your last Replace restore.", color = colors.textSecondary, fontSize = 11.sp)
            }
            TextButton(
              onClick = {
                scope.launch {
                  runCatching {
                    val raw = snapshotFile.readText()
                    val info = BackupManager.validateAndPreviewBackupJson(raw).getOrThrow()
                    BackupManager.applyRestore(context, db, settingsStore, info.envelope, RestoreMode.REPLACE)
                    snapshotFile.delete()
                    hasUndoSnapshot = false
                    statusMessage = "Previous data restored from safety snapshot."
                  }
                }
              }
            ) {
              Text("Undo", color = accent, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // EXPORT Section
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
          SectionLabel("EXPORT")
          if (!isSignedIn) {
            Text(
              text = "Sign in with Google above to export backups.",
              color = colors.textSecondary,
              fontSize = 12.sp
            )
          }
          Button(
            onClick = { createJsonLauncher.launch("habit-radar-backup-$dateStamp.json") },
            enabled = isSignedIn,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0A0D)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("export_json_btn")
          ) {
            Text("Export backup (.json)", fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = { createCsvZipLauncher.launch("habit-radar-csv-$dateStamp.zip") },
            enabled = isSignedIn,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("export_csv_btn")
          ) {
            Text("Export CSV (.zip)", color = if (isSignedIn) colors.textPrimary else colors.textSecondary)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Include settings & theme", color = colors.textPrimary, fontSize = 14.sp)
            Switch(
              checked = includeSettings,
              onCheckedChange = { includeSettings = it },
              enabled = isSignedIn,
              colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color(0xFF0A0A0D))
            )
          }
        }
      }

      // RESTORE Section
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
          SectionLabel("RESTORE")
          if (!isSignedIn) {
            Text(
              text = "Sign in with Google above to restore from a backup.",
              color = colors.textSecondary,
              fontSize = 12.sp
            )
          }
          OutlinedButton(
            onClick = { openJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
            enabled = isSignedIn,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("restore_json_btn")
          ) {
            Text(
              "Restore from backup (.json)",
              color = if (isSignedIn) colors.textPrimary else colors.textSecondary,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // AUTOMATIC BACKUP Section
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          SectionLabel("AUTOMATIC BACKUP")
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Weekly auto-backup", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
              Text(
                text = if (!isSignedIn) "Sign in with Google to enable auto-backup" else (settings.autoBackupFolderUri ?: "Choose a folder on your device"),
                color = colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Switch(
              checked = settings.autoBackupEnabled && isSignedIn,
              enabled = isSignedIn,
              onCheckedChange = { enabled ->
                if (enabled && settings.autoBackupFolderUri == null) {
                  folderPickerLauncher.launch(null)
                } else {
                  onUpdateSettings { it.copy(autoBackupEnabled = enabled) }
                }
              },
              colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color(0xFF0A0A0D))
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Keep last backups", color = colors.textSecondary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(3, 5, 10).forEach { count ->
                val selected = settings.autoBackupKeep == count
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected && isSignedIn) accent else colors.surfaceElevated)
                    .let { mod ->
                      if (isSignedIn) mod.clickable { onUpdateSettings { it.copy(autoBackupKeep = count) } } else mod
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = "$count",
                    color = if (selected && isSignedIn) Color(0xFF0A0A0D) else colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Restore Preview Sheet
  val currentPreview = previewInfo
  if (currentPreview != null) {
    ModalBottomSheet(
      onDismissRequest = { previewInfo = null },
      containerColor = colors.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = formatSerifHeadline("Restore *backup?*", accent),
          color = colors.textPrimary,
          fontFamily = InstrumentSerif,
          fontSize = 28.sp
        )
        Text(
          text = "Exported: ${currentPreview.exportedAt}",
          color = colors.textSecondary,
          fontSize = 12.sp
        )
        Text(
          text = "Contains ${currentPreview.habitsCount} habits, ${currentPreview.entriesCount} check-ins, and ${currentPreview.goalsCount} goals.",
          color = colors.textPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
          onClick = {
            scope.launch {
              val res = BackupManager.applyRestore(
                context = context,
                db = db,
                settingsStore = settingsStore,
                envelope = currentPreview.envelope,
                mode = RestoreMode.MERGE
              )
              previewInfo = null
              statusMessage = res.fold(
                onSuccess = { "Merged $it habits from backup." },
                onFailure = { "Merge failed: ${it.message}" }
              )
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0A0D)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Merge with current data", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
          onClick = {
            scope.launch {
              val res = BackupManager.applyRestore(
                context = context,
                db = db,
                settingsStore = settingsStore,
                envelope = currentPreview.envelope,
                mode = RestoreMode.REPLACE
              )
              previewInfo = null
              hasUndoSnapshot = snapshotFile.exists()
              statusMessage = res.fold(
                onSuccess = { "Replaced data ($it habits restored). Safety snapshot saved." },
                onFailure = { "Restore failed: ${it.message}" }
              )
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Replace all data", color = colors.danger, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
fun OnboardingScreen(
  settings: AppSettings,
  today: LocalDate,
  onCompleteOnboarding: (
    selectedTemplates: List<HabitTemplate>,
    enableReminders: Boolean,
    weekStart: WeekStartDay,
    rolloverHour: Int,
    showHijri: Boolean
  ) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val accent = resolveAppAccent(settings.accentColor)

  var page by remember { mutableIntStateOf(0) }
  val selectedTemplateIds = remember {
    mutableStateListOf("tpl_water", "tpl_walk", "tpl_read_pages")
  }
  var weekStart by remember { mutableStateOf(settings.weekStart) }
  var rolloverHour by remember { mutableIntStateOf(0) }
  var showHijri by remember { mutableStateOf(settings.showHijri) }
  var enableReminders by remember { mutableStateOf(true) }

  val notifPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { _ ->
    page = 4
  }

  Scaffold(
    containerColor = colors.background
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(24.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top indicator + Skip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          repeat(5) { idx ->
            Box(
              modifier = Modifier
                .width(if (idx == page) 24.dp else 8.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (idx <= page) accent else colors.borderSubtle)
            )
          }
        }
        if (page < 4) {
          Text(
            text = "Skip",
            color = colors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .clickable {
                onCompleteOnboarding(emptyList(), false, weekStart, rolloverHour, showHijri)
              }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("onboarding_skip_btn")
          )
        }
      }

      // Page body
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        when (page) {
          0 -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Text(
                text = formatSerifHeadline("Small steps, *every day.*", accent),
                color = colors.textPrimary,
                fontFamily = InstrumentSerif,
                fontSize = 42.sp,
                textAlign = TextAlign.Center
              )
              Text(
                text = "A calm, 100% private habit studio designed for clarity, consistency, and momentum.",
                color = colors.textSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
              )
            }
          }
          1 -> {
            Column(modifier = Modifier.fillMaxSize()) {
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = formatSerifHeadline("Pick 1–3 *starter habits.*", accent),
                color = colors.textPrimary,
                fontFamily = InstrumentSerif,
                fontSize = 32.sp
              )
              Text(
                text = "Start small. You can customize or add more anytime.",
                color = colors.textSecondary,
                fontSize = 13.sp
              )
              Spacer(modifier = Modifier.height(12.dp))
              LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(StarterTemplatesCatalog.templates, key = { it.id }) { tpl ->
                  val selected = tpl.id in selectedTemplateIds
                  val hColor = habitColor(tpl.colorKey)
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .background(if (selected) colors.surfaceElevated else colors.surface)
                      .border(
                        width = 1.dp,
                        color = if (selected) hColor else colors.borderSubtle,
                        shape = RoundedCornerShape(14.dp)
                      )
                      .clickable {
                        if (selected) selectedTemplateIds.remove(tpl.id)
                        else selectedTemplateIds.add(tpl.id)
                      }
                      .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    HabitIconBadge(iconKey = tpl.iconKey, colorKey = tpl.colorKey, size = 36.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(tpl.name, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                      Text(tpl.description, color = colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (selected) {
                      Icon(Icons.Default.Check, contentDescription = "Selected", tint = hColor)
                    }
                  }
                }
              }
            }
          }
          2 -> {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
              Text(
                text = formatSerifHeadline("Your daily *rhythm.*", accent),
                color = colors.textPrimary,
                fontFamily = InstrumentSerif,
                fontSize = 34.sp
              )

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("WEEK STARTS ON")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  WeekStartDay.entries.forEach { ws ->
                    val sel = weekStart == ws
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) accent else colors.surface)
                        .border(1.dp, if (sel) accent else colors.borderSubtle, RoundedCornerShape(50))
                        .clickable { weekStart = ws }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                      Text(
                        text = ws.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (sel) Color(0xFF0A0A0D) else colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  }
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("DAY ROLLOVER TIME")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf(0 to "Midnight", 2 to "2:00 AM", 4 to "4:00 AM").forEach { (hr, label) ->
                    val sel = rolloverHour == hr
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) accent else colors.surface)
                        .border(1.dp, if (sel) accent else colors.borderSubtle, RoundedCornerShape(50))
                        .clickable { rolloverHour = hr }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                      Text(
                        text = label,
                        color = if (sel) Color(0xFF0A0A0D) else colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  }
                }
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(colors.surface)
                  .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("Show Hijri calendar date", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  Text("Display Umm al-Qura Hijri date alongside Gregorian", color = colors.textSecondary, fontSize = 12.sp)
                }
                Switch(
                  checked = showHijri,
                  onCheckedChange = { showHijri = it },
                  colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color(0xFF0A0A0D))
                )
              }
            }
          }
          3 -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Text(
                text = formatSerifHeadline("Gentle *reminders.*", accent),
                color = colors.textPrimary,
                fontFamily = InstrumentSerif,
                fontSize = 36.sp,
                textAlign = TextAlign.Center
              )
              Text(
                text = "Get a calm nudge at each habit's suggested time, with quick Done & Snooze buttons.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(colors.surface)
                  .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Enable suggested reminders", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Switch(
                  checked = enableReminders,
                  onCheckedChange = { enableReminders = it },
                  colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color(0xFF0A0A0D))
                )
              }
            }
          }
          else -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Text(
                text = formatSerifHeadline("Your radar is *live.*", accent),
                color = colors.textPrimary,
                fontFamily = InstrumentSerif,
                fontSize = 42.sp,
                textAlign = TextAlign.Center
              )
              Text(
                text = "${selectedTemplateIds.size} starter habits ready for today.",
                color = colors.textSecondary,
                fontSize = 15.sp
              )
            }
          }
        }
      }

      // Bottom CTA Button
      Button(
        onClick = {
          when (page) {
            0, 1, 2 -> page++
            3 -> {
              if (enableReminders && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                page = 4
              }
            }
            else -> {
              val chosen = StarterTemplatesCatalog.templates.filter { it.id in selectedTemplateIds }
              onCompleteOnboarding(chosen, enableReminders, weekStart, rolloverHour, showHijri)
            }
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0A0D)),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("onboarding_next_btn")
      ) {
        Text(
          text = if (page == 4) "Enter Habit Radar" else "Continue",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
