package com.example.feature.widgets.glance

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn as GlanceLazyColumn
import androidx.glance.appwidget.lazy.items as glanceItems
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment as GlanceAlignment
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight as GlanceFontWeight
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextStyle as GlanceTextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.HabitRadarDatabase
import com.example.core.data.db.toDomain
import com.example.core.data.repository.HabitRadarRepository
import com.example.core.di.appContainer
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Goal
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.Step
import com.example.core.domain.model.TrackType
import com.example.core.domain.model.WidgetConfig
import com.example.core.domain.model.WidgetCornerStyle
import com.example.core.domain.model.WidgetThemeMode
import com.example.core.domain.model.WidgetType
import com.example.core.domain.usecase.GoalStepEvaluator
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.domain.usecase.StreakCalculator
import com.example.core.ui.components.HabitIconView
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.components.HabitRadarSegmentedControl
import com.example.core.ui.components.HabitRadarSwitch
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.components.SubPageTopBar
import com.example.core.ui.theme.HabitRadarTheme
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.example.core.ui.theme.parseHexColor
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class WidgetCircleVisualState {
  DONE,
  TODAY_PENDING,
  PAST_MISSED,
  SKIPPED,
  NOT_SCHEDULED,
  FUTURE
}

object CircleStateMapper {
  fun mapState(
    isScheduled: Boolean,
    entryStatus: EntryStatus?,
    isSatisfiedDone: Boolean,
    date: LocalDate,
    today: LocalDate
  ): WidgetCircleVisualState {
    if (isSatisfiedDone) return WidgetCircleVisualState.DONE
    if (entryStatus == EntryStatus.SKIPPED) return WidgetCircleVisualState.SKIPPED
    if (date.isAfter(today)) return WidgetCircleVisualState.FUTURE
    if (!isScheduled) return WidgetCircleVisualState.NOT_SCHEDULED
    return if (date == today) WidgetCircleVisualState.TODAY_PENDING else WidgetCircleVisualState.PAST_MISSED
  }
}

object WidgetBitmapRenderer {

  fun renderCircleBitmap(
    state: WidgetCircleVisualState,
    habitColorInt: Int,
    sizePx: Int = 72
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val r = (sizePx / 2f) - 4f

    val red = android.graphics.Color.red(habitColorInt)
    val green = android.graphics.Color.green(habitColorInt)
    val blue = android.graphics.Color.blue(habitColorInt)

    when (state) {
      WidgetCircleVisualState.DONE -> {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(230, red, green, blue)
        canvas.drawCircle(cx, cy, r, paint)

        // Dark checkmark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sizePx * 0.09f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = android.graphics.Color.rgb(18, 19, 23)
        canvas.drawLine(sizePx * 0.30f, sizePx * 0.52f, sizePx * 0.45f, sizePx * 0.66f, paint)
        canvas.drawLine(sizePx * 0.45f, sizePx * 0.66f, sizePx * 0.72f, sizePx * 0.36f, paint)
      }
      WidgetCircleVisualState.TODAY_PENDING -> {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(48, red, green, blue)
        canvas.drawCircle(cx, cy, r, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sizePx * 0.07f
        paint.color = android.graphics.Color.argb(245, red, green, blue)
        canvas.drawCircle(cx, cy, r - 2f, paint)
      }
      WidgetCircleVisualState.PAST_MISSED -> {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(52, red, green, blue)
        canvas.drawCircle(cx, cy, r, paint)
      }
      WidgetCircleVisualState.SKIPPED -> {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(45, red, green, blue)
        canvas.drawCircle(cx, cy, r, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sizePx * 0.07f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = android.graphics.Color.argb(200, red, green, blue)
        canvas.drawLine(sizePx * 0.34f, cy, sizePx * 0.66f, cy, paint)
      }
      WidgetCircleVisualState.NOT_SCHEDULED, WidgetCircleVisualState.FUTURE -> {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(26, red, green, blue)
        canvas.drawCircle(cx, cy, r, paint)
      }
    }
    return bmp
  }

  fun renderProgressRingBitmap(
    percent: Int,
    accentColorInt: Int,
    sizePx: Int = 180
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val stroke = sizePx * 0.11f
    val rect = RectF(stroke, stroke, sizePx - stroke, sizePx - stroke)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = stroke
      strokeCap = Paint.Cap.ROUND
    }
    paint.color = android.graphics.Color.argb(55, 255, 255, 255)
    canvas.drawArc(rect, 0f, 360f, false, paint)

    paint.color = accentColorInt
    canvas.drawArc(rect, -90f, 360f * (percent.coerceIn(0, 100) / 100f), false, paint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.WHITE
      textSize = sizePx * 0.25f
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
    }
    canvas.drawText("$percent%", sizePx / 2f, sizePx / 2f + (textPaint.textSize * 0.35f), textPaint)
    return bmp
  }
}

object WidgetParams {
  val HabitIdKey = ActionParameters.Key<String>("habit_id")
  val DateStrKey = ActionParameters.Key<String>("date_str")
  val StepIdKey = ActionParameters.Key<String>("step_id")
  val GoalIdKey = ActionParameters.Key<String>("goal_id")
}

class ToggleHabitWidgetAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters
  ) {
    val habitId = parameters[WidgetParams.HabitIdKey] ?: return
    val dateStr = parameters[WidgetParams.DateStrKey] ?: return
    val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return

    val container = context.appContainer
    val db = container.database
    val repo = container.repository
    val habit = db.habitDao().getHabitById(habitId)?.toDomain() ?: return

    if (habit.trackType == TrackType.TASK) {
      repo.toggleTaskEntry(habitId, date)
      WidgetUpdater.updateAll(context)
    } else {
      val openIntent = Intent(context, MainActivity::class.java).apply {
        data = Uri.parse("habitradar://habit/$habitId")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      context.startActivity(openIntent)
    }
  }
}

class ToggleStepWidgetAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters
  ) {
    val stepId = parameters[WidgetParams.StepIdKey] ?: return
    val goalId = parameters[WidgetParams.GoalIdKey] ?: return
    val container = context.appContainer
    val db = container.database
    val repo = container.repository
    val step = db.goalDao().getStepsForGoal(goalId).map { it.toDomain() }.firstOrNull { it.id == stepId } ?: return
    repo.toggleManualStep(step)
    WidgetUpdater.updateAll(context)
  }
}

object WidgetUpdater {
  suspend fun updateAll(context: Context) {
    runCatching {
      WeekGridGlanceWidget().updateAll(context)
      TodayListGlanceWidget().updateAll(context)
      ProgressRingGlanceWidget().updateAll(context)
      SingleHabitGlanceWidget().updateAll(context)
      GoalProgressGlanceWidget().updateAll(context)
    }
  }

  fun requestPinWidget(context: Context, type: WidgetType): Boolean {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
    val receiverClass = when (type) {
      WidgetType.WEEK_GRID -> WeekGridWidgetReceiver::class.java
      WidgetType.TODAY_LIST -> TodayListWidgetReceiver::class.java
      WidgetType.PROGRESS_RING -> ProgressRingWidgetReceiver::class.java
      WidgetType.SINGLE_HABIT -> SingleHabitWidgetReceiver::class.java
      WidgetType.GOAL -> GoalProgressWidgetReceiver::class.java
    }
    val provider = ComponentName(context, receiverClass)
    return appWidgetManager.requestPinAppWidget(provider, null, null)
  }
}

private data class WidgetSnapshot(
  val habits: List<Habit>,
  val entriesByHabit: Map<String, Map<LocalDate, Entry>>,
  val pausesByHabit: Map<String, List<HabitPause>>,
  val streaksByHabit: Map<String, Int>,
  val goals: List<Goal>,
  val stepsByGoal: Map<String, List<Step>>,
  val today: LocalDate,
  val weekDays: List<LocalDate>,
  val accentColor: String,
  val allowBackfill: Boolean,
  val dayRun: Int
)

private suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
  val container = context.appContainer
  val db = container.database
  val settings = container.settingsDataStore.getSettings()
  val today = LogicalDateCalculator.now(settings.dayRollover)
  val weekStartDate = LogicalDateCalculator.startOfWeek(today, settings.weekStart)
  val weekDays = (0..6).map { weekStartDate.plusDays(it.toLong()) }

  val allHabits = db.habitDao().getAllHabits().map { it.toDomain() }
  val allEntries = db.entryDao().getAllEntries().map { it.toDomain() }
  val allPauses = db.pauseDao().getAllPauses().map { it.toDomain() }
  val pausesByHabit = allPauses.groupBy { it.habitId }
  val entriesByHabitList = allEntries.groupBy { it.habitId }
  val entriesByHabitMap = entriesByHabitList.mapValues { (_, list) -> list.associateBy { it.date } }

  val activeHabits = allHabits.filter { h ->
    h.status == HabitStatus.ACTIVE && !ScheduleMatcher.isPausedOn(today, pausesByHabit[h.id].orEmpty())
  }

  val streaks = activeHabits.associate { h ->
    val s = StreakCalculator.calculateStreaks(
      h,
      entriesByHabitList[h.id].orEmpty(),
      pausesByHabit[h.id].orEmpty(),
      today
    )
    h.id to s.currentStreak
  }

  val goals = db.goalDao().getAllGoals().map { it.toDomain() }
  val steps = db.goalDao().getAllSteps().map { it.toDomain() }.groupBy { it.goalId }
  val dayRun = StreakCalculator.calculateOverallDayRun(allEntries, today)

  return WidgetSnapshot(
    habits = activeHabits,
    entriesByHabit = entriesByHabitMap,
    pausesByHabit = pausesByHabit,
    streaksByHabit = streaks,
    goals = goals,
    stepsByGoal = steps,
    today = today,
    weekDays = weekDays,
    accentColor = settings.accentColor,
    allowBackfill = settings.defaultWidgetAllowBackfill,
    dayRun = dayRun
  )
}

// 1. WEEK GRID WIDGET (Reference Design — Section 5.16 C)
class WeekGridGlanceWidget : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(DpSize(180.dp, 110.dp), DpSize(250.dp, 180.dp), DpSize(320.dp, 300.dp))
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context)
    provideContent {
      val size = LocalSize.current
      val isCompactNarrow = size.width < 245.dp
      GlanceColumn(
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(28.dp)
          .background(Color(0xFF1B1B1F))
          .padding(14.dp)
      ) {
        // Header row: Mo Tu We Th Fr Sa Su
        if (!isCompactNarrow) {
          GlanceRow(
            verticalAlignment = GlanceAlignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)
          ) {
            GlanceSpacer(modifier = GlanceModifier.defaultWeight())
            snap.weekDays.forEach { d ->
              val isToday = d == snap.today
              GlanceBox(
                contentAlignment = GlanceAlignment.Center,
                modifier = GlanceModifier.width(32.dp)
              ) {
                GlanceText(
                  text = ScheduleMatcher.dowShortLabel(d.dayOfWeek.value).take(2),
                  style = GlanceTextStyle(
                    color = ColorProvider(if (isToday) Color.White else Color(0xFF8E8F98)),
                    fontSize = 11.sp,
                    fontWeight = if (isToday) GlanceFontWeight.Bold else GlanceFontWeight.Medium
                  )
                )
              }
            }
          }
        }

        if (snap.habits.isEmpty()) {
          GlanceBox(
            contentAlignment = GlanceAlignment.Center,
            modifier = GlanceModifier
              .fillMaxSize()
              .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
          ) {
            GlanceText(
              text = "No habits yet — tap to add one",
              style = GlanceTextStyle(color = ColorProvider(Color(0xFF8E8F98)), fontSize = 13.sp)
            )
          }
        } else {
          GlanceLazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            glanceItems(snap.habits, itemId = { it.id.hashCode().toLong() }) { habit ->
              val hColor = parseHexColor(habit.colorKey)
              val hColorArgb = hColor.toArgb()
              val streak = snap.streaksByHabit[habit.id] ?: 0
              val hEntries = snap.entriesByHabit[habit.id].orEmpty()
              val hPauses = snap.pausesByHabit[habit.id].orEmpty()

              GlanceRow(
                verticalAlignment = GlanceAlignment.CenterVertically,
                modifier = GlanceModifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp)
              ) {
                // Icon tile
                GlanceBox(
                  contentAlignment = GlanceAlignment.Center,
                  modifier = GlanceModifier
                    .size(38.dp)
                    .cornerRadius(12.dp)
                    .background(hColor.copy(alpha = 0.22f))
                    .clickable(
                      actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                          data = Uri.parse("habitradar://habit/${habit.id}")
                        }
                      )
                    )
                ) {
                  GlanceText(
                    text = habit.name.take(1).uppercase(),
                    style = GlanceTextStyle(
                      color = ColorProvider(hColor),
                      fontSize = 15.sp,
                      fontWeight = GlanceFontWeight.Bold
                    )
                  )
                }
                GlanceSpacer(modifier = GlanceModifier.width(8.dp))
                GlanceText(
                  text = habit.name,
                  maxLines = 1,
                  style = GlanceTextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 14.sp,
                    fontWeight = GlanceFontWeight.Medium
                  ),
                  modifier = GlanceModifier.defaultWeight()
                )
                GlanceText(
                  text = "🔥$streak",
                  style = GlanceTextStyle(
                    color = ColorProvider(Color(0xFF8E8F98)),
                    fontSize = 11.sp
                  ),
                  modifier = GlanceModifier.padding(end = 6.dp)
                )

                val daysToShow = if (isCompactNarrow) listOf(snap.today) else snap.weekDays
                daysToShow.forEach { d ->
                  val entry = hEntries[d]
                  val scheduled = ScheduleMatcher.isScheduledOn(habit, d, hPauses)
                  val done = ScheduleMatcher.isEntryDone(habit, entry)
                  val state = CircleStateMapper.mapState(scheduled, entry?.status, done, d, snap.today)
                  val bmp = WidgetBitmapRenderer.renderCircleBitmap(state, hColorArgb, 60)
                  val canClick = d == snap.today || (!d.isAfter(snap.today) && snap.allowBackfill && scheduled)

                  GlanceBox(
                    contentAlignment = GlanceAlignment.Center,
                    modifier = GlanceModifier
                      .size(32.dp)
                      .padding(2.dp)
                      .let { mod ->
                        if (canClick) {
                          mod.clickable(
                            actionRunCallback<ToggleHabitWidgetAction>(
                              actionParametersOf(
                                WidgetParams.HabitIdKey to habit.id,
                                WidgetParams.DateStrKey to d.toString()
                              )
                            )
                          )
                        } else mod
                      }
                  ) {
                    Image(
                      provider = ImageProvider(bmp),
                      contentDescription = "${habit.name}, $d, ${state.name.lowercase()}",
                      modifier = GlanceModifier.size(28.dp)
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

// 2. TODAY CHECKLIST WIDGET
class TodayListGlanceWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context)
    val scheduledToday = snap.habits.filter {
      ScheduleMatcher.isScheduledOn(it, snap.today, snap.pausesByHabit[it.id].orEmpty())
    }
    val doneToday = scheduledToday.count { h ->
      ScheduleMatcher.isEntryDone(h, snap.entriesByHabit[h.id]?.get(snap.today))
    }

    provideContent {
      GlanceColumn(
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(28.dp)
          .background(Color(0xFF17181D))
          .padding(14.dp)
      ) {
        GlanceText(
          text = "$doneToday of ${scheduledToday.size} done today",
          style = GlanceTextStyle(
            color = ColorProvider(Color.White),
            fontSize = 14.sp,
            fontWeight = GlanceFontWeight.Bold
          ),
          modifier = GlanceModifier.padding(bottom = 8.dp)
        )
        GlanceLazyColumn(modifier = GlanceModifier.fillMaxSize()) {
          glanceItems(scheduledToday, itemId = { it.id.hashCode().toLong() }) { habit ->
            val hColor = parseHexColor(habit.colorKey)
            val entry = snap.entriesByHabit[habit.id]?.get(snap.today)
            val done = ScheduleMatcher.isEntryDone(habit, entry)
            val bmp = WidgetBitmapRenderer.renderCircleBitmap(
              if (done) WidgetCircleVisualState.DONE else WidgetCircleVisualState.TODAY_PENDING,
              hColor.toArgb(),
              64
            )
            GlanceRow(
              verticalAlignment = GlanceAlignment.CenterVertically,
              modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
              GlanceText(
                text = habit.name,
                maxLines = 1,
                style = GlanceTextStyle(
                  color = ColorProvider(if (done) Color(0xFF8E8F98) else Color.White),
                  fontSize = 14.sp
                ),
                modifier = GlanceModifier.defaultWeight()
              )
              GlanceBox(
                modifier = GlanceModifier
                  .size(32.dp)
                  .clickable(
                    actionRunCallback<ToggleHabitWidgetAction>(
                      actionParametersOf(
                        WidgetParams.HabitIdKey to habit.id,
                        WidgetParams.DateStrKey to snap.today.toString()
                      )
                    )
                  )
              ) {
                Image(
                  provider = ImageProvider(bmp),
                  contentDescription = habit.name,
                  modifier = GlanceModifier.size(30.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

// 3. PROGRESS RING WIDGET
class ProgressRingGlanceWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context)
    val scheduled = snap.habits.filter {
      ScheduleMatcher.isScheduledOn(it, snap.today, snap.pausesByHabit[it.id].orEmpty())
    }
    val done = scheduled.count { h ->
      ScheduleMatcher.isEntryDone(h, snap.entriesByHabit[h.id]?.get(snap.today))
    }
    val pct = if (scheduled.isNotEmpty()) ((done.toDouble() / scheduled.size) * 100).roundToInt() else 0
    val accent = parseHexColor(snap.accentColor).toArgb()
    val ringBmp = WidgetBitmapRenderer.renderProgressRingBitmap(pct, accent, 160)

    provideContent {
      GlanceColumn(
        horizontalAlignment = GlanceAlignment.CenterHorizontally,
        verticalAlignment = GlanceAlignment.CenterVertically,
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(28.dp)
          .background(Color(0xFF17181D))
          .padding(12.dp)
          .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
      ) {
        Image(
          provider = ImageProvider(ringBmp),
          contentDescription = "Today completion $pct%",
          modifier = GlanceModifier.size(76.dp)
        )
        GlanceSpacer(modifier = GlanceModifier.height(6.dp))
        GlanceText(
          text = "$done of ${scheduled.size} · 🔥 ${snap.dayRun}d run",
          style = GlanceTextStyle(color = ColorProvider(Color(0xFF8E8F98)), fontSize = 11.sp)
        )
      }
    }
  }
}

// 4. SINGLE HABIT WIDGET
class SingleHabitGlanceWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context)
    val habit = snap.habits.firstOrNull()
    provideContent {
      GlanceColumn(
        horizontalAlignment = GlanceAlignment.CenterHorizontally,
        verticalAlignment = GlanceAlignment.CenterVertically,
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(28.dp)
          .background(Color(0xFF17181D))
          .padding(12.dp)
      ) {
        if (habit == null) {
          GlanceText(
            text = "Add a habit",
            style = GlanceTextStyle(color = ColorProvider(Color.White), fontSize = 13.sp)
          )
        } else {
          val hColor = parseHexColor(habit.colorKey)
          val done = ScheduleMatcher.isEntryDone(habit, snap.entriesByHabit[habit.id]?.get(snap.today))
          val streak = snap.streaksByHabit[habit.id] ?: 0
          val bmp = WidgetBitmapRenderer.renderCircleBitmap(
            if (done) WidgetCircleVisualState.DONE else WidgetCircleVisualState.TODAY_PENDING,
            hColor.toArgb(),
            84
          )
          GlanceText(
            text = habit.name,
            maxLines = 1,
            style = GlanceTextStyle(
              color = ColorProvider(Color.White),
              fontSize = 15.sp,
              fontWeight = GlanceFontWeight.Bold
            )
          )
          GlanceText(
            text = "🔥 $streak Days",
            style = GlanceTextStyle(color = ColorProvider(Color(0xFF8E8F98)), fontSize = 12.sp),
            modifier = GlanceModifier.padding(bottom = 8.dp)
          )
          GlanceBox(
            modifier = GlanceModifier
              .size(48.dp)
              .clickable(
                actionRunCallback<ToggleHabitWidgetAction>(
                  actionParametersOf(
                    WidgetParams.HabitIdKey to habit.id,
                    WidgetParams.DateStrKey to snap.today.toString()
                  )
                )
              )
          ) {
            Image(
              provider = ImageProvider(bmp),
              contentDescription = "Toggle ${habit.name}",
              modifier = GlanceModifier.size(48.dp)
            )
          }
        }
      }
    }
  }
}

// 5. GOAL PROGRESS WIDGET
class GoalProgressGlanceWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context)
    val goal = snap.goals.firstOrNull()
    val steps = goal?.let { snap.stepsByGoal[it.id].orEmpty() }.orEmpty()
    val doneCount = steps.count { it.done }
    val nextStep = steps.firstOrNull { !it.done }

    provideContent {
      GlanceColumn(
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(28.dp)
          .background(Color(0xFF17181D))
          .padding(14.dp)
      ) {
        if (goal == null) {
          GlanceText(
            text = "No active goals yet",
            style = GlanceTextStyle(color = ColorProvider(Color(0xFF8E8F98)), fontSize = 13.sp)
          )
        } else {
          GlanceRow(
            verticalAlignment = GlanceAlignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth()
          ) {
            GlanceText(
              text = goal.title,
              maxLines = 1,
              style = GlanceTextStyle(
                color = ColorProvider(Color.White),
                fontSize = 15.sp,
                fontWeight = GlanceFontWeight.Bold
              ),
              modifier = GlanceModifier.defaultWeight()
            )
            GlanceText(
              text = "$doneCount of ${steps.size} steps",
              style = GlanceTextStyle(color = ColorProvider(Color(0xFF8E8F98)), fontSize = 12.sp)
            )
          }
          GlanceSpacer(modifier = GlanceModifier.height(8.dp))
          if (nextStep != null) {
            GlanceRow(
              verticalAlignment = GlanceAlignment.CenterVertically,
              modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(
                  actionRunCallback<ToggleStepWidgetAction>(
                    actionParametersOf(
                      WidgetParams.StepIdKey to nextStep.id,
                      WidgetParams.GoalIdKey to goal.id
                    )
                  )
                )
            ) {
              GlanceText(
                text = "Next: ${nextStep.title}",
                maxLines = 1,
                style = GlanceTextStyle(color = ColorProvider(Color(0xFFE8A8F5)), fontSize = 13.sp),
                modifier = GlanceModifier.defaultWeight()
              )
              GlanceText(
                text = "✓",
                style = GlanceTextStyle(
                  color = ColorProvider(Color.White),
                  fontSize = 14.sp,
                  fontWeight = GlanceFontWeight.Bold
                )
              )
            }
          } else {
            GlanceText(
              text = "All steps complete!",
              style = GlanceTextStyle(color = ColorProvider(Color(0xFFA8DFB4)), fontSize = 13.sp)
            )
          }
        }
      }
    }
  }
}

class WeekGridWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = WeekGridGlanceWidget()
}

class TodayListWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = TodayListGlanceWidget()
}

class ProgressRingWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ProgressRingGlanceWidget()
}

class SingleHabitWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = SingleHabitGlanceWidget()
}

class GoalProgressWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = GoalProgressGlanceWidget()
}

class WidgetConfigActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val appWidgetId = intent?.extras?.getInt(
      AppWidgetManager.EXTRA_APPWIDGET_ID,
      AppWidgetManager.INVALID_APPWIDGET_ID
    ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish()
      return
    }

    val container = appContainer
    val db = container.database
    val settingsStore = container.settingsDataStore
    val repo = container.repository

    setContent {
      HabitRadarTheme {
        val colors = LocalHabitRadarColors.current
        val scope = rememberCoroutineScope()
        var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }
        var chooseSpecific by remember { mutableStateOf(false) }
        val selectedHabitIds = remember { mutableStateListOf<String>() }
        var themeMode by remember { mutableStateOf(WidgetThemeMode.FOLLOW) }
        var opacity by remember { mutableIntStateOf(100) }
        var cornerStyle by remember { mutableStateOf(WidgetCornerStyle.ROUNDED) }
        var showStreak by remember { mutableStateOf(true) }
        var hideNames by remember { mutableStateOf(false) }
        var allowBackfill by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
          habits = db.habitDao().getAllHabits().map { it.toDomain() }.filter { it.status == HabitStatus.ACTIVE }
          val s = settingsStore.getSettings()
          themeMode = s.defaultWidgetTheme
          opacity = s.defaultWidgetOpacity
          cornerStyle = s.defaultWidgetCorner
          showStreak = s.defaultWidgetShowStreak
          hideNames = s.defaultWidgetHideNames
          allowBackfill = s.defaultWidgetAllowBackfill
        }

        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
        ) {
          SubPageTopBar(
            title = "Widget settings",
            onBack = {
              setResult(RESULT_CANCELED)
              finish()
            }
          )
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            item {
              SectionLabel("HABITS")
              HabitRadarCard {
                HabitRadarSegmentedControl(
                  options = listOf(false, true),
                  selected = chooseSpecific,
                  labelMapper = { if (it) "Choose habits" else "All habits" },
                  onSelect = { chooseSpecific = it },
                  modifier = Modifier.fillMaxWidth()
                )
                if (chooseSpecific) {
                  Spacer(modifier = Modifier.height(12.dp))
                  habits.forEach { h ->
                    val checked = h.id in selectedHabitIds
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          if (checked) selectedHabitIds.remove(h.id) else selectedHabitIds.add(h.id)
                        }
                        .padding(vertical = 6.dp)
                    ) {
                      Checkbox(
                        checked = checked,
                        onCheckedChange = {
                          if (it) selectedHabitIds.add(h.id) else selectedHabitIds.remove(h.id)
                        }
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(text = h.name, color = colors.textPrimary, fontFamily = PlusJakartaSansFamily)
                    }
                  }
                }
              }
            }

            item {
              SectionLabel("WIDGET STYLE")
              HabitRadarCard {
                Text("Theme", color = colors.textPrimary, fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                HabitRadarSegmentedControl(
                  options = WidgetThemeMode.entries,
                  selected = themeMode,
                  labelMapper = {
                    when (it) {
                      WidgetThemeMode.FOLLOW -> "Follow app"
                      WidgetThemeMode.DARK -> "Dark"
                      WidgetThemeMode.LIGHT -> "Light"
                    }
                  },
                  onSelect = { themeMode = it },
                  modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("Background opacity: $opacity%", color = colors.textPrimary, fontFamily = PlusJakartaSansFamily)
                Slider(
                  value = opacity.toFloat(),
                  onValueChange = { opacity = it.roundToInt() },
                  valueRange = 40f..100f
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text("Show streak", color = colors.textPrimary, fontFamily = PlusJakartaSansFamily)
                  HabitRadarSwitch(checked = showStreak, onCheckedChange = { showStreak = it })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text("Hide habit names", color = colors.textPrimary, fontFamily = PlusJakartaSansFamily)
                  HabitRadarSwitch(checked = hideNames, onCheckedChange = { hideNames = it })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text("Allow back-filling from widget", color = colors.textPrimary, fontFamily = PlusJakartaSansFamily)
                  HabitRadarSwitch(checked = allowBackfill, onCheckedChange = { allowBackfill = it })
                }
              }
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(colors.surface2)
                .clickable {
                  setResult(RESULT_CANCELED)
                  finish()
                }
            ) {
              Text("Cancel", color = colors.textPrimary, fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Medium)
            }
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(colors.accent)
                .clickable {
                  scope.launch {
                    repo.saveWidgetConfig(
                      WidgetConfig(
                        appWidgetId = appWidgetId,
                        type = WidgetType.WEEK_GRID,
                        habitIds = if (chooseSpecific) selectedHabitIds.toList() else emptyList(),
                        theme = themeMode,
                        opacity = opacity,
                        cornerStyle = cornerStyle,
                        showStreak = showStreak,
                        hideNames = hideNames,
                        allowBackfill = allowBackfill
                      )
                    )
                    WidgetUpdater.updateAll(this@WidgetConfigActivity)
                    val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    setResult(RESULT_OK, resultValue)
                    finish()
                  }
                }
            ) {
              Text("Add widget", color = colors.onAccent, fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
