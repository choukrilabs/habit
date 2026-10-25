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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.HabitRadarDatabase
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
  onBack: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val accent = resolveAppAccent(settings.accentColor)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val pagerState = rememberPagerState(pageCount = { 5 })
  var showManualFallbackSheet by remember { mutableStateOf(false) }
  var refreshFeedback by remember { mutableStateOf<String?>(null) }

  val widgetTitles = listOf(
    "Week Grid (4×4)",
    "Today Checklist (4×3)",
    "Progress Ring (2×2)",
    "Single Habit (2×2)",
    "Goal Progress (4×2)"
  )
  val widgetDescriptions = listOf(
    "Up to 4 habits with a 7-day interactive grid",
    "Today's scheduled checklist with streak badges",
    "Compact circular progress ring for today",
    "One habit's icon, streak, and 7-day mini strip",
    "One Milestone goal's progress bar & next step"
  )

  Scaffold(
    containerColor = colors.background,
    topBar = {
      SubPageTopBar(title = "Widgets", onBack = onBack)
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 48.dp)
    ) {
      // 1. Live Preview Pager
      item {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          HorizontalPager(
            state = pagerState,
            modifier = Modifier
              .fillMaxWidth()
              .height(210.dp)
          ) { page ->
            LiveWidgetPreviewCard(
              pageIndex = page,
              habits = habits,
              entries = entries,
              goals = goals,
              stepsByGoal = stepsByGoal,
              settings = settings,
              today = today,
              accent = accent
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = widgetTitles[pagerState.currentPage],
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = widgetDescriptions[pagerState.currentPage],
            color = colors.textSecondary,
            fontSize = 12.sp
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(5) { idx ->
              Box(
                modifier = Modifier
                  .size(if (idx == pagerState.currentPage) 8.dp else 6.dp)
                  .clip(CircleShape)
                  .background(if (idx == pagerState.currentPage) accent else colors.borderSubtle)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = {
              val receiverClass = when (pagerState.currentPage) {
                0 -> WeekGridWidgetReceiver::class.java
                1 -> TodayListWidgetReceiver::class.java
                2 -> ProgressRingWidgetReceiver::class.java
                3 -> SingleHabitWidgetReceiver::class.java
                else -> GoalProgressWidgetReceiver::class.java
              }
              val mgr = AppWidgetManager.getInstance(context)
              val provider = ComponentName(context, receiverClass)
              if (mgr.isRequestPinAppWidgetSupported) {
                val pinned = mgr.requestPinAppWidget(provider, null, null)
                if (!pinned) showManualFallbackSheet = true
              } else {
                showManualFallbackSheet = true
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0A0D)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("add_widget_home_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add to home screen", fontWeight = FontWeight.Bold)
          }
        }
      }

      // 2. WIDGET STYLE Card
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
          SectionLabel("WIDGET STYLE")

          Text("Theme", color = colors.textSecondary, fontSize = 12.sp)
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
                  .background(if (selected) accent else colors.surfaceElevated)
                  .clickable { onUpdateSettings { it.copy(defaultWidgetTheme = tm) } }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  text = label,
                  color = if (selected) Color(0xFF0A0A0D) else colors.textPrimary,
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
            Text("Background opacity", color = colors.textSecondary, fontSize = 12.sp)
            Text("${settings.defaultWidgetOpacity}%", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Slider(
            value = settings.defaultWidgetOpacity.toFloat(),
            onValueChange = { v -> onUpdateSettings { it.copy(defaultWidgetOpacity = v.roundToInt().coerceIn(60, 100)) } },
            valueRange = 60f..100f,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
          )

          Text("Corner style", color = colors.textSecondary, fontSize = 12.sp)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetCornerStyle.entries.forEach { cs ->
              val selected = settings.defaultWidgetCorner == cs
              val label = if (cs == WidgetCornerStyle.ROUNDED) "Rounded" else "Extra rounded"
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .background(if (selected) accent else colors.surfaceElevated)
                  .clickable { onUpdateSettings { it.copy(defaultWidgetCorner = cs) } }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  text = label,
                  color = if (selected) Color(0xFF0A0A0D) else colors.textPrimary,
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
            Text("Show streak fire", color = colors.textPrimary, fontSize = 13.sp)
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
            Text("Hide habit names (privacy)", color = colors.textPrimary, fontSize = 13.sp)
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
            Text("Allow past-day backfill on grid", color = colors.textPrimary, fontSize = 13.sp)
            Switch(
              checked = settings.defaultWidgetAllowBackfill,
              onCheckedChange = { v -> onUpdateSettings { it.copy(defaultWidgetAllowBackfill = v) } },
              colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color(0xFF0A0A0D))
            )
          }
        }
      }

      // 3. ACTIVE WIDGETS Card
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
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            SectionLabel("ACTIVE WIDGETS")
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

          Text(
            text = refreshFeedback ?: "Home screen widgets update automatically whenever you mark a habit or goal step.",
            color = colors.textSecondary,
            fontSize = 12.sp
          )
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

@Composable
private fun LiveWidgetPreviewCard(
  pageIndex: Int,
  habits: List<Habit>,
  entries: List<Entry>,
  goals: List<Goal>,
  stepsByGoal: Map<String, List<Step>>,
  settings: AppSettings,
  today: LocalDate,
  accent: Color
) {
  val radiusDp = when (settings.defaultWidgetCorner) {
    WidgetCornerStyle.ROUNDED -> 22.dp
    WidgetCornerStyle.EXTRA -> 30.dp
  }
  val bgAlpha = (settings.defaultWidgetOpacity / 100f).coerceIn(0.6f, 1f)
  val cardBg = when (settings.defaultWidgetTheme) {
    WidgetThemeMode.LIGHT -> Color(0xFFF6F6F9).copy(alpha = bgAlpha)
    else -> Color(0xFF141419).copy(alpha = bgAlpha)
  }
  val textColor = if (settings.defaultWidgetTheme == WidgetThemeMode.LIGHT) Color(0xFF111114) else Color(0xFFF5F5F8)
  val subColor = if (settings.defaultWidgetTheme == WidgetThemeMode.LIGHT) Color(0xFF5B5B66) else Color(0xFF9A9AA6)

  val activeHabits = habits.take(4)
  val entriesByHabit = remember(entries) {
    entries.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.date } }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 8.dp)
      .clip(RoundedCornerShape(radiusDp))
      .background(cardBg)
      .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(radiusDp))
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    when (pageIndex) {
      0 -> {
        // Week Grid 4x4
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("THIS WEEK", color = subColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("M  T  W  T  F  S  S", color = subColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
          if (activeHabits.isEmpty()) {
            Text("Create habits to preview Week Grid", color = subColor, fontSize = 12.sp)
          } else {
            activeHabits.take(3).forEachIndexed { idx, h ->
              val hColor = habitColor(h.colorKey)
              val displayTitle = if (settings.defaultWidgetHideNames) "Habit ${idx + 1}" else h.name
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(displayTitle, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                  for (i in 6 downTo 0) {
                    val d = today.minusDays(i.toLong())
                    val done = ScheduleMatcher.isEntryDone(h, entriesByHabit[h.id]?.get(d))
                    Box(
                      modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (done) hColor else Color.Transparent)
                        .border(1.dp, if (done) hColor else subColor.copy(alpha = 0.4f), CircleShape)
                    )
                  }
                }
              }
            }
          }
        }
      }
      1 -> {
        // Today List 4x3
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Text("TODAY'S HABITS", color = subColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          if (activeHabits.isEmpty()) {
            Text("Create habits to preview Today List", color = subColor, fontSize = 12.sp)
          } else {
            activeHabits.take(3).forEachIndexed { idx, h ->
              val done = ScheduleMatcher.isEntryDone(h, entriesByHabit[h.id]?.get(today))
              val hColor = habitColor(h.colorKey)
              val displayTitle = if (settings.defaultWidgetHideNames) "Habit ${idx + 1}" else h.name
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (done) hColor else Color.Transparent)
                    .border(1.5.dp, hColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(displayTitle, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (settings.defaultWidgetShowStreak) {
                  Text("🔥", fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
      2 -> {
        // Progress Ring 2x2
        val total = habits.size.coerceAtLeast(1)
        val doneCount = habits.count { ScheduleMatcher.isEntryDone(it, entriesByHabit[it.id]?.get(today)) }
        val pct = ((doneCount.toFloat() / total) * 100).roundToInt()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawArc(
                color = subColor.copy(alpha = 0.25f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
              )
              drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * (pct / 100f),
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
              )
            }
            Text("$pct%", color = textColor, fontFamily = InstrumentSerif, fontSize = 24.sp)
          }
          Column {
            Text("TODAY", color = subColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("$doneCount of ${habits.size} done", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
      3 -> {
        // Single Habit 2x2
        val h = habits.firstOrNull()
        if (h == null) {
          Text("Create a habit to preview Single Habit widget", color = subColor, fontSize = 12.sp)
        } else {
          val hColor = habitColor(h.colorKey)
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HabitIconBadge(iconKey = h.iconKey, colorKey = h.colorKey, size = 38.dp)
            Text(if (settings.defaultWidgetHideNames) "Habit 1" else h.name, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              for (i in 6 downTo 0) {
                val d = today.minusDays(i.toLong())
                val done = ScheduleMatcher.isEntryDone(h, entriesByHabit[h.id]?.get(d))
                Box(
                  modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (done) hColor else subColor.copy(alpha = 0.3f))
                )
              }
            }
          }
        }
      }
      else -> {
        // Goal Progress 4x2
        val g = goals.firstOrNull()
        if (g == null) {
          Text("Create a Milestone goal to preview Goal Progress widget", color = subColor, fontSize = 12.sp)
        } else {
          val gSteps = stepsByGoal[g.id].orEmpty()
          val progress = GoalStepEvaluator.calculateGoalProgress(gSteps)
          val gColor = habitColor(g.colorKey)
          Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
              Text(g.title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
              Text("${(progress * 100).roundToInt()}%", color = gColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(subColor.copy(alpha = 0.25f))
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(progress.coerceIn(0f, 1f))
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(gColor)
              )
            }
            val nextStep = gSteps.firstOrNull { !it.done }
            Text(
              text = if (nextStep != null) "Next: ${nextStep.title}" else "All steps complete!",
              color = subColor,
              fontSize = 12.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
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
            text = "All data is stored locally on this device.",
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
      if (hasUndoSnapshot) {
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
          Button(
            onClick = { createJsonLauncher.launch("habit-radar-backup-$dateStamp.json") },
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF0A0A0D)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("export_json_btn")
          ) {
            Text("Export backup (.json)", fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = { createCsvZipLauncher.launch("habit-radar-csv-$dateStamp.zip") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("export_csv_btn")
          ) {
            Text("Export CSV (.zip)", color = colors.textPrimary)
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
          OutlinedButton(
            onClick = { openJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("restore_json_btn")
          ) {
            Text("Restore from backup (.json)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
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
                text = settings.autoBackupFolderUri ?: "Choose a folder on your device",
                color = colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Switch(
              checked = settings.autoBackupEnabled,
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
                    .background(if (selected) accent else colors.surfaceElevated)
                    .clickable { onUpdateSettings { it.copy(autoBackupKeep = count) } }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = "$count",
                    color = if (selected) Color(0xFF0A0A0D) else colors.textPrimary,
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
