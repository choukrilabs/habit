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
import androidx.glance.appwidget.action.actionSendBroadcast
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        paint.color = android.graphics.Color.argb(245, red, green, blue)
        canvas.drawCircle(cx, cy, r, paint)

        // Dark checkmark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sizePx * 0.095f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = android.graphics.Color.rgb(17, 18, 22)
        canvas.drawLine(sizePx * 0.30f, sizePx * 0.52f, sizePx * 0.44f, sizePx * 0.66f, paint)
        canvas.drawLine(sizePx * 0.44f, sizePx * 0.66f, sizePx * 0.71f, sizePx * 0.36f, paint)
      }
      WidgetCircleVisualState.TODAY_PENDING -> {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(30, red, green, blue)
        canvas.drawCircle(cx, cy, r, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sizePx * 0.085f
        paint.color = android.graphics.Color.argb(240, red, green, blue)
        canvas.drawCircle(cx, cy, r - (sizePx * 0.04f), paint)
      }
      WidgetCircleVisualState.PAST_MISSED,
      WidgetCircleVisualState.NOT_SCHEDULED,
      WidgetCircleVisualState.FUTURE -> {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(54, red, green, blue)
        canvas.drawCircle(cx, cy, r, paint)
      }
      WidgetCircleVisualState.SKIPPED -> {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(48, red, green, blue)
        canvas.drawCircle(cx, cy, r, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sizePx * 0.075f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = android.graphics.Color.argb(200, red, green, blue)
        canvas.drawLine(sizePx * 0.34f, cy, sizePx * 0.66f, cy, paint)
      }
    }
    return bmp
  }

  fun renderWeekDayCircleBitmap(
    isDone: Boolean,
    isToday: Boolean,
    habitColorInt: Int,
    sizePx: Int = 84
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val r = (sizePx / 2f) - 5f

    val red = android.graphics.Color.red(habitColorInt)
    val green = android.graphics.Color.green(habitColorInt)
    val blue = android.graphics.Color.blue(habitColorInt)

    if (isDone) {
      paint.style = Paint.Style.FILL
      paint.color = android.graphics.Color.argb(245, red, green, blue)
      canvas.drawCircle(cx, cy, r, paint)

      paint.style = Paint.Style.STROKE
      paint.strokeWidth = sizePx * 0.095f
      paint.strokeCap = Paint.Cap.ROUND
      paint.color = android.graphics.Color.rgb(14, 21, 16)
      canvas.drawLine(sizePx * 0.30f, sizePx * 0.52f, sizePx * 0.44f, sizePx * 0.66f, paint)
      canvas.drawLine(sizePx * 0.44f, sizePx * 0.66f, sizePx * 0.71f, sizePx * 0.36f, paint)
    } else {
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = if (isToday) sizePx * 0.08f else sizePx * 0.06f
      paint.color = if (isToday) {
        android.graphics.Color.argb(245, red, green, blue)
      } else {
        android.graphics.Color.argb(110, red, green, blue)
      }
      canvas.drawCircle(cx, cy, r - paint.strokeWidth / 2f, paint)
    }
    return bmp
  }

  fun renderCheckButtonBitmap(
    isDone: Boolean,
    habitColorInt: Int,
    sizePx: Int = 88
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val r = (sizePx / 2f) - 2f

    val red = android.graphics.Color.red(habitColorInt)
    val green = android.graphics.Color.green(habitColorInt)
    val blue = android.graphics.Color.blue(habitColorInt)

    paint.style = Paint.Style.FILL
    paint.color = if (isDone) {
      android.graphics.Color.argb(245, red, green, blue)
    } else {
      android.graphics.Color.argb(54, red, green, blue)
    }
    canvas.drawCircle(cx, cy, r, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = sizePx * 0.085f
    paint.strokeCap = Paint.Cap.ROUND
    paint.color = if (isDone) {
      android.graphics.Color.rgb(14, 21, 16)
    } else {
      android.graphics.Color.rgb(194, 212, 198)
    }
    canvas.drawLine(sizePx * 0.31f, sizePx * 0.52f, sizePx * 0.45f, sizePx * 0.66f, paint)
    canvas.drawLine(sizePx * 0.45f, sizePx * 0.66f, sizePx * 0.70f, sizePx * 0.37f, paint)
    return bmp
  }

  fun renderHabitIconBadgeBitmap(
    iconKey: String,
    habitColorInt: Int,
    sizePx: Int = 84
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val red = android.graphics.Color.red(habitColorInt)
    val green = android.graphics.Color.green(habitColorInt)
    val blue = android.graphics.Color.blue(habitColorInt)

    val corner = sizePx * 0.28f
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.argb(52, red, green, blue)
    canvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), corner, corner, paint)

    drawHabitIconOnCanvas(
      canvas = canvas,
      iconKey = iconKey,
      colorInt = android.graphics.Color.argb(255, red, green, blue),
      cx = sizePx / 2f,
      cy = sizePx / 2f,
      radius = sizePx * 0.28f
    )
    return bmp
  }

  fun renderHeatmapGridBitmap(
    habit: Habit,
    entriesMap: Map<LocalDate, Entry>,
    today: LocalDate,
    weekDays: List<LocalDate>,
    habitColorInt: Int,
    columns: Int = 15,
    widthPx: Int = 660,
    heightPx: Int = 270
  ): Bitmap {
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    val red = android.graphics.Color.red(habitColorInt)
    val green = android.graphics.Color.green(habitColorInt)
    val blue = android.graphics.Color.blue(habitColorInt)
    val activeColor = android.graphics.Color.argb(245, red, green, blue)
    val inactiveColor = android.graphics.Color.argb(44, red, green, blue)

    val rows = 7
    val gap = widthPx * 0.012f
    val totalGapX = gap * (columns - 1)
    val cellW = (widthPx - totalGapX) / columns
    val totalGapY = gap * (rows - 1)
    val cellH = (heightPx - totalGapY) / rows
    val cellSize = minOf(cellW, cellH)
    val corner = cellSize * 0.28f

    val weekEnd = weekDays.lastOrNull() ?: today
    val doneToday = ScheduleMatcher.isEntryDone(habit, entriesMap[today])

    for (col in 0 until columns) {
      val x = col * (cellW + gap)
      for (row in 0 until rows) {
        val y = row * (cellH + gap)
        val daysAgo = (columns - 1 - col) * 7 + (rows - 1 - row)
        val cellDate = weekEnd.minusDays(daysAgo.toLong())
        val isBottomRight = col == columns - 1 && row == rows - 1
        val isDone = ScheduleMatcher.isEntryDone(habit, entriesMap[cellDate]) ||
          (isBottomRight && (doneToday || entriesMap.isEmpty()))
        paint.color = if (isDone) activeColor else inactiveColor
        canvas.drawRoundRect(
          RectF(x, y, x + cellW, y + cellH),
          corner,
          corner,
          paint
        )
      }
    }
    return bmp
  }

  fun renderHabitRingWithBadgeBitmap(
    iconKey: String,
    habitColorInt: Int,
    progressFraction: Float,
    streak: Int,
    showStreak: Boolean,
    sizePx: Int = 160
  ): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val red = android.graphics.Color.red(habitColorInt)
    val green = android.graphics.Color.green(habitColorInt)
    val blue = android.graphics.Color.blue(habitColorInt)

    val stroke = sizePx * 0.095f
    val pad = stroke * 0.9f
    val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = stroke
      strokeCap = Paint.Cap.ROUND
    }

    // Track circle
    ringPaint.color = android.graphics.Color.argb(56, red, green, blue)
    canvas.drawArc(rect, 0f, 360f, false, ringPaint)

    // Progress arc
    val clamped = progressFraction.coerceIn(0f, 1f)
    if (clamped > 0f) {
      ringPaint.color = android.graphics.Color.argb(255, red, green, blue)
      val sweep = (clamped * 360f).coerceAtLeast(24f)
      canvas.drawArc(rect, -90f, sweep, false, ringPaint)
    }

    // Centered habit icon
    drawHabitIconOnCanvas(
      canvas = canvas,
      iconKey = iconKey,
      colorInt = android.graphics.Color.argb(255, red, green, blue),
      cx = sizePx / 2f,
      cy = sizePx / 2f,
      radius = sizePx * 0.19f
    )

    // White pill badge at bottom-right when streak > 0
    if (showStreak && streak > 0) {
      val badgeText = "🔥 $streak"
      val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(17, 17, 20)
        textSize = sizePx * 0.165f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }
      val textWidth = textPaint.measureText(badgeText)
      val badgeH = sizePx * 0.25f
      val badgeW = (textWidth + sizePx * 0.16f).coerceAtLeast(sizePx * 0.44f)
      val badgeRight = sizePx * 0.98f
      val badgeBottom = sizePx * 0.97f
      val badgeLeft = badgeRight - badgeW
      val badgeTop = badgeBottom - badgeH

      val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
      }
      val pillRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)
      canvas.drawRoundRect(pillRect, badgeH / 2f, badgeH / 2f, pillPaint)

      val textY = pillRect.centerY() + (textPaint.textSize * 0.35f)
      canvas.drawText(badgeText, pillRect.centerX(), textY, textPaint)
    }
    return bmp
  }

  private fun drawHabitIconOnCanvas(
    canvas: Canvas,
    iconKey: String,
    colorInt: Int,
    cx: Float,
    cy: Float,
    radius: Float
  ) {
    if (iconKey.startsWith("emoji:")) {
      val emoji = iconKey.removePrefix("emoji:")
      val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = radius * 1.55f
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText(emoji, cx, cy + emojiPaint.textSize * 0.35f, emojiPaint)
      return
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = colorInt
      style = Paint.Style.STROKE
      strokeWidth = (radius * 0.20f).coerceAtLeast(2.4f)
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = colorInt
      style = Paint.Style.FILL
    }

    when (iconKey) {
      "walk", "run" -> {
        // Head
        canvas.drawCircle(cx + radius * 0.1f, cy - radius * 0.62f, radius * 0.20f, fillPaint)
        // Torso
        canvas.drawLine(cx + radius * 0.05f, cy - radius * 0.35f, cx - radius * 0.08f, cy + radius * 0.18f, paint)
        // Arms
        canvas.drawLine(cx - radius * 0.38f, cy - radius * 0.05f, cx + radius * 0.05f, cy - radius * 0.30f, paint)
        canvas.drawLine(cx + radius * 0.05f, cy - radius * 0.30f, cx + radius * 0.42f, cy - radius * 0.08f, paint)
        // Legs
        canvas.drawLine(cx - radius * 0.08f, cy + radius * 0.18f, cx - radius * 0.38f, cy + radius * 0.72f, paint)
        canvas.drawLine(cx - radius * 0.08f, cy + radius * 0.18f, cx + radius * 0.34f, cy + radius * 0.72f, paint)
      }
      "moon", "night" -> {
        val path = android.graphics.Path()
        val r = radius * 0.75f
        path.addCircle(cx, cy, r, android.graphics.Path.Direction.CW)
        val cut = android.graphics.Path()
        cut.addCircle(cx + r * 0.45f, cy - r * 0.25f, r * 0.75f, android.graphics.Path.Direction.CW)
        path.op(cut, android.graphics.Path.Op.DIFFERENCE)
        canvas.drawPath(path, fillPaint)
      }
      "savings", "wallet" -> {
        // Piggy / coin body
        canvas.drawRoundRect(
          RectF(cx - radius * 0.72f, cy - radius * 0.45f, cx + radius * 0.72f, cy + radius * 0.52f),
          radius * 0.45f,
          radius * 0.45f,
          paint
        )
        // Coin slot on top
        canvas.drawLine(cx - radius * 0.25f, cy - radius * 0.70f, cx + radius * 0.25f, cy - radius * 0.70f, paint)
        // Eye dot
        canvas.drawCircle(cx + radius * 0.30f, cy - radius * 0.10f, radius * 0.10f, fillPaint)
      }
      "sunrise" -> {
        // Horizon line
        canvas.drawLine(cx - radius * 0.80f, cy + radius * 0.35f, cx + radius * 0.80f, cy + radius * 0.35f, paint)
        // Sun semi-circle
        canvas.drawArc(
          RectF(cx - radius * 0.48f, cy - radius * 0.13f, cx + radius * 0.48f, cy + radius * 0.83f),
          180f,
          180f,
          false,
          paint
        )
        // Top rays
        canvas.drawLine(cx, cy - radius * 0.65f, cx, cy - radius * 0.35f, paint)
        canvas.drawLine(cx - radius * 0.52f, cy - radius * 0.42f, cx - radius * 0.32f, cy - radius * 0.22f, paint)
        canvas.drawLine(cx + radius * 0.52f, cy - radius * 0.42f, cx + radius * 0.32f, cy - radius * 0.22f, paint)
      }
      "sun" -> {
        canvas.drawCircle(cx, cy, radius * 0.38f, fillPaint)
        for (i in 0 until 8) {
          val a = Math.toRadians((i * 45).toDouble())
          val x1 = cx + kotlin.math.cos(a).toFloat() * radius * 0.56f
          val y1 = cy + kotlin.math.sin(a).toFloat() * radius * 0.56f
          val x2 = cx + kotlin.math.cos(a).toFloat() * radius * 0.82f
          val y2 = cy + kotlin.math.sin(a).toFloat() * radius * 0.82f
          canvas.drawLine(x1, y1, x2, y2, paint)
        }
      }
      "book", "study", "journal" -> {
        val w = radius * 0.78f
        val h = radius * 0.62f
        canvas.drawRoundRect(RectF(cx - w, cy - h, cx, cy + h), radius * 0.12f, radius * 0.12f, paint)
        canvas.drawRoundRect(RectF(cx, cy - h, cx + w, cy + h), radius * 0.12f, radius * 0.12f, paint)
      }
      "charity", "heart" -> {
        val path = android.graphics.Path()
        val r = radius * 0.72f
        path.moveTo(cx, cy + r * 0.75f)
        path.cubicTo(cx - r * 1.2f, cy + r * 0.05f, cx - r * 0.9f, cy - r * 0.85f, cx, cy - r * 0.25f)
        path.cubicTo(cx + r * 0.9f, cy - r * 0.85f, cx + r * 1.2f, cy + r * 0.05f, cx, cy + r * 0.75f)
        path.close()
        canvas.drawPath(path, fillPaint)
      }
      "workout" -> {
        // Dumbbell bar
        canvas.drawLine(cx - radius * 0.68f, cy, cx + radius * 0.68f, cy, paint)
        // Left & right weights
        canvas.drawLine(cx - radius * 0.52f, cy - radius * 0.45f, cx - radius * 0.52f, cy + radius * 0.45f, paint)
        canvas.drawLine(cx + radius * 0.52f, cy - radius * 0.45f, cx + radius * 0.52f, cy + radius * 0.45f, paint)
      }
      "water" -> {
        val path = android.graphics.Path()
        path.moveTo(cx, cy - radius * 0.78f)
        path.quadTo(cx + radius * 0.68f, cy + radius * 0.05f, cx, cy + radius * 0.78f)
        path.quadTo(cx - radius * 0.68f, cy + radius * 0.05f, cx, cy - radius * 0.78f)
        path.close()
        canvas.drawPath(path, fillPaint)
      }
      else -> {
        // Crisp 4-point star / sparkle
        val path = android.graphics.Path()
        val rOut = radius * 0.78f
        val rIn = radius * 0.28f
        for (i in 0 until 8) {
          val r = if (i % 2 == 0) rOut else rIn
          val a = Math.toRadians((i * 45 - 90).toDouble())
          val x = cx + kotlin.math.cos(a).toFloat() * r
          val y = cy + kotlin.math.sin(a).toFloat() * r
          if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, fillPaint)
      }
    }
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
  val MarkDoneOnlyKey = ActionParameters.Key<Boolean>("mark_done_only")
}

class MarkHabitDoneWidgetAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters
  ) {
    val habitId = parameters[WidgetParams.HabitIdKey] ?: return
    val dateStr = parameters[WidgetParams.DateStrKey]
    val container = context.appContainer
    val repo = container.repository
    val date = dateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
      ?: repo.getLogicalToday()
    repo.markHabitDoneOnDate(habitId, date)
    WidgetUpdater.updateAll(context)
  }
}

class ToggleHabitWidgetAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters
  ) {
    val habitId = parameters[WidgetParams.HabitIdKey] ?: return
    val dateStr = parameters[WidgetParams.DateStrKey]
    val markDoneOnly = parameters[WidgetParams.MarkDoneOnlyKey] ?: false
    val container = context.appContainer
    val repo = container.repository
    val date = dateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
      ?: repo.getLogicalToday()

    if (markDoneOnly) {
      repo.markHabitDoneOnDate(habitId, date)
    } else {
      repo.toggleHabitDoneOnDate(habitId, date)
    }
    WidgetUpdater.updateAll(context)
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
    val repo = container.repository
    repo.toggleStepDone(stepId, goalId)
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
  val showStreak: Boolean,
  val hideNames: Boolean,
  val dayRun: Int
)

private suspend fun loadWidgetSnapshot(context: Context, glanceId: GlanceId? = null): WidgetSnapshot {
  val container = context.appContainer
  val db = container.database
  val settings = container.settingsDataStore.getSettings()
  val today = LogicalDateCalculator.now(settings.dayRollover)
  val weekStartDate = LogicalDateCalculator.startOfWeek(today, settings.weekStart)
  val weekDays = (0..6).map { weekStartDate.plusDays(it.toLong()) }

  val widgetConfig = glanceId?.let { id ->
    runCatching {
      val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
      db.widgetConfigDao().getConfig(appWidgetId)?.toDomain()
    }.getOrNull()
  }

  val allHabits = db.habitDao().getAllHabits().map { it.toDomain() }
  val allEntries = db.entryDao().getAllEntries().map { it.toDomain() }
  val allPauses = db.pauseDao().getAllPauses().map { it.toDomain() }
  val pausesByHabit = allPauses.groupBy { it.habitId }
  val entriesByHabitList = allEntries.groupBy { it.habitId }
  val entriesByHabitMap = entriesByHabitList.mapValues { (_, list) -> list.associateBy { it.date } }

  val configuredHabitIds = widgetConfig?.habitIds?.toSet().orEmpty()
  val activeHabits = allHabits.filter { h ->
    h.status == HabitStatus.ACTIVE &&
      !ScheduleMatcher.isPausedOn(today, pausesByHabit[h.id].orEmpty()) &&
      (configuredHabitIds.isEmpty() || h.id in configuredHabitIds)
  }.sortedBy { it.sortOrder }

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
    allowBackfill = widgetConfig?.allowBackfill ?: settings.defaultWidgetAllowBackfill,
    showStreak = widgetConfig?.showStreak ?: settings.defaultWidgetShowStreak,
    hideNames = widgetConfig?.hideNames ?: settings.defaultWidgetHideNames,
    dayRun = dayRun
  )
}

// 1. HABIT LIST WIDGET ("Habit List — up to 10 habits with this week's days (medium & large)")
class WeekGridGlanceWidget : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(DpSize(180.dp, 110.dp), DpSize(250.dp, 180.dp), DpSize(320.dp, 300.dp))
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context, id)
    provideContent {
      val size = LocalSize.current
      val isCompactNarrow = size.width < 220.dp
      GlanceColumn(
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(24.dp)
          .background(Color(0xFF141416))
          .padding(14.dp)
      ) {
        // Header row: Mo Tu We Th Fr Sa Su aligned over the 7 day circles (Screenshot 2)
        if (!isCompactNarrow) {
          GlanceRow(
            verticalAlignment = GlanceAlignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)
          ) {
            GlanceSpacer(modifier = GlanceModifier.defaultWeight())
            snap.weekDays.forEach { d ->
              val isToday = d == snap.today
              GlanceBox(
                contentAlignment = GlanceAlignment.Center,
                modifier = GlanceModifier.width(24.dp)
              ) {
                GlanceText(
                  text = ScheduleMatcher.dowShortLabel(d.dayOfWeek.value).take(2),
                  style = GlanceTextStyle(
                    color = ColorProvider(if (isToday) Color.White else Color(0xFF7E7E86)),
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
            glanceItems(snap.habits.take(10), itemId = { it.id.hashCode().toLong() }) { habit ->
              val hColor = parseHexColor(habit.colorKey)
              val hColorArgb = hColor.toArgb()
              val streak = snap.streaksByHabit[habit.id] ?: 0
              val hEntries = snap.entriesByHabit[habit.id].orEmpty()
              val hPauses = snap.pausesByHabit[habit.id].orEmpty()
              val todayDone = ScheduleMatcher.isEntryDone(habit, hEntries[snap.today])
              val iconBadgeBmp = WidgetBitmapRenderer.renderHabitIconBadgeBitmap(
                iconKey = habit.iconKey,
                habitColorInt = hColorArgb,
                sizePx = 76
              )

              GlanceRow(
                verticalAlignment = GlanceAlignment.CenterVertically,
                modifier = GlanceModifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                // Rounded-square tinted icon badge
                GlanceBox(
                  contentAlignment = GlanceAlignment.Center,
                  modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(
                      actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                          data = Uri.parse("habitradar://habit/${habit.id}")
                        }
                      )
                    )
                ) {
                  Image(
                    provider = ImageProvider(iconBadgeBmp),
                    contentDescription = habit.name,
                    modifier = GlanceModifier.size(30.dp)
                  )
                }
                GlanceSpacer(modifier = GlanceModifier.width(8.dp))
                if (!snap.hideNames) {
                  GlanceText(
                    text = habit.name,
                    maxLines = 1,
                    style = GlanceTextStyle(
                      color = ColorProvider(Color.White),
                      fontSize = 13.sp,
                      fontWeight = GlanceFontWeight.Medium
                    ),
                    modifier = GlanceModifier
                      .defaultWeight()
                      .clickable(
                        actionSendBroadcast(
                          if (todayDone) {
                            HabitRadarGlanceAppWidgetReceiver.createToggleHabitDoneIntent(
                              context = context,
                              receiverClass = WeekGridWidgetReceiver::class.java,
                              habitId = habit.id,
                              date = snap.today
                            )
                          } else {
                            HabitRadarGlanceAppWidgetReceiver.createMarkHabitDoneIntent(
                              context = context,
                              receiverClass = WeekGridWidgetReceiver::class.java,
                              habitId = habit.id,
                              date = snap.today
                            )
                          }
                        )
                      )
                  )
                } else {
                  GlanceSpacer(modifier = GlanceModifier.defaultWeight())
                }
                if (snap.showStreak) {
                  GlanceText(
                    text = "🔥 $streak",
                    style = GlanceTextStyle(
                      color = ColorProvider(Color(0xFF9A9AA2)),
                      fontSize = 11.sp,
                      fontWeight = GlanceFontWeight.Medium
                    ),
                    modifier = GlanceModifier.padding(end = 6.dp)
                  )
                }

                val daysToShow = if (isCompactNarrow) listOf(snap.today) else snap.weekDays
                daysToShow.forEach { d ->
                  val entry = hEntries[d]
                  val scheduled = ScheduleMatcher.isScheduledOn(habit, d, hPauses)
                  val done = ScheduleMatcher.isEntryDone(habit, entry)
                  val state = CircleStateMapper.mapState(scheduled, entry?.status, done, d, snap.today)
                  val bmp = WidgetBitmapRenderer.renderCircleBitmap(state, hColorArgb, 56)
                  val canClick = d == snap.today || (!d.isAfter(snap.today) && snap.allowBackfill)

                  GlanceBox(
                    contentAlignment = GlanceAlignment.Center,
                    modifier = GlanceModifier
                      .size(24.dp)
                      .padding(2.dp)
                      .let { mod ->
                        if (canClick) {
                          mod.clickable(
                            actionSendBroadcast(
                              if (done) {
                                HabitRadarGlanceAppWidgetReceiver.createToggleHabitDoneIntent(
                                  context = context,
                                  receiverClass = WeekGridWidgetReceiver::class.java,
                                  habitId = habit.id,
                                  date = d
                                )
                              } else {
                                HabitRadarGlanceAppWidgetReceiver.createMarkHabitDoneIntent(
                                  context = context,
                                  receiverClass = WeekGridWidgetReceiver::class.java,
                                  habitId = habit.id,
                                  date = d
                                )
                              }
                            )
                          )
                        } else mod
                      }
                  ) {
                    Image(
                      provider = ImageProvider(bmp),
                      contentDescription = "${habit.name}, $d, ${state.name.lowercase()}",
                      modifier = GlanceModifier.size(20.dp)
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

// 2. HABIT WEEK WIDGET ("Habit Week — this week at a glance; tap today's circle to complete.")
class TodayListGlanceWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context, id)
    val habit = snap.habits.firstOrNull { it.name.equals("Walk", ignoreCase = true) }
      ?: snap.habits.firstOrNull()

    provideContent {
      if (habit == null) {
        GlanceBox(
          contentAlignment = GlanceAlignment.Center,
          modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(Color(0xFF141D16))
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        ) {
          GlanceText(
            text = "Add a habit to preview Habit Week",
            style = GlanceTextStyle(color = ColorProvider(Color.White), fontSize = 13.sp)
          )
        }
      } else {
        val hColor = parseHexColor(habit.colorKey)
        val hColorArgb = hColor.toArgb()
        val streak = snap.streaksByHabit[habit.id] ?: 0
        val hEntries = snap.entriesByHabit[habit.id].orEmpty()
        val iconBmp = WidgetBitmapRenderer.renderHabitIconBadgeBitmap(habit.iconKey, hColorArgb, 84)
        val cardBg = Color(
          red = (0x10 / 255f) * 0.88f + hColor.red * 0.12f,
          green = (0x13 / 255f) * 0.88f + hColor.green * 0.12f,
          blue = (0x11 / 255f) * 0.88f + hColor.blue * 0.12f,
          alpha = 1f
        )

        GlanceColumn(
          modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(cardBg)
            .padding(14.dp)
        ) {
          // Top row: Icon Badge + Title & Streak
          GlanceRow(
            verticalAlignment = GlanceAlignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 10.dp)
          ) {
            Image(
              provider = ImageProvider(iconBmp),
              contentDescription = habit.name,
              modifier = GlanceModifier.size(38.dp)
            )
            GlanceSpacer(modifier = GlanceModifier.width(10.dp))
            GlanceColumn {
              GlanceText(
                text = if (snap.hideNames) "Habit" else habit.name,
                maxLines = 1,
                style = GlanceTextStyle(
                  color = ColorProvider(Color.White),
                  fontSize = 16.sp,
                  fontWeight = GlanceFontWeight.Bold
                )
              )
              if (snap.showStreak) {
                GlanceText(
                  text = "🔥 $streak",
                  style = GlanceTextStyle(
                    color = ColorProvider(Color(0xFFA0A8A2)),
                    fontSize = 12.sp,
                    fontWeight = GlanceFontWeight.Medium
                  )
                )
              }
            }
          }

          // Bottom row: 7 day columns with today's column highlighted in a vertical rounded pill
          GlanceRow(
            verticalAlignment = GlanceAlignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth()
          ) {
            snap.weekDays.forEach { d ->
              val isToday = d == snap.today
              val done = ScheduleMatcher.isEntryDone(habit, hEntries[d])
              val circleBmp = WidgetBitmapRenderer.renderWeekDayCircleBitmap(
                isDone = done,
                isToday = isToday,
                habitColorInt = hColorArgb,
                sizePx = 72
              )
              val clickAction = actionSendBroadcast(
                if (done) {
                  HabitRadarGlanceAppWidgetReceiver.createToggleHabitDoneIntent(
                    context = context,
                    receiverClass = TodayListWidgetReceiver::class.java,
                    habitId = habit.id,
                    date = d
                  )
                } else {
                  HabitRadarGlanceAppWidgetReceiver.createMarkHabitDoneIntent(
                    context = context,
                    receiverClass = TodayListWidgetReceiver::class.java,
                    habitId = habit.id,
                    date = d
                  )
                }
              )

              GlanceBox(
                contentAlignment = GlanceAlignment.Center,
                modifier = GlanceModifier
                  .defaultWeight()
                  .cornerRadius(14.dp)
                  .background(if (isToday) hColor.copy(alpha = 0.18f) else Color.Transparent)
                  .padding(vertical = 6.dp)
                  .clickable(clickAction)
              ) {
                GlanceColumn(horizontalAlignment = GlanceAlignment.CenterHorizontally) {
                  GlanceText(
                    text = ScheduleMatcher.dowShortLabel(d.dayOfWeek.value),
                    style = GlanceTextStyle(
                      color = ColorProvider(if (isToday) Color.White else Color(0xFF8B998E)),
                      fontSize = 11.sp,
                      fontWeight = if (isToday) GlanceFontWeight.Bold else GlanceFontWeight.Medium
                    )
                  )
                  GlanceSpacer(modifier = GlanceModifier.height(6.dp))
                  Image(
                    provider = ImageProvider(circleBmp),
                    contentDescription = "${habit.name} on $d",
                    modifier = GlanceModifier.size(26.dp)
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

// 3. HABIT RINGS WIDGET ("Habit Rings — up to 8 habits as progress rings with 🔥 streak badges.")
class ProgressRingGlanceWidget : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(DpSize(180.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(320.dp, 200.dp))
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context, id)
    val habitsToShow = snap.habits.take(8)

    provideContent {
      val size = LocalSize.current
      val maxRows = if (size.height >= 165.dp && habitsToShow.size > 4) 2 else 1
      val rows = habitsToShow.chunked(4).take(maxRows)

      GlanceColumn(
        horizontalAlignment = GlanceAlignment.CenterHorizontally,
        verticalAlignment = GlanceAlignment.CenterVertically,
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(24.dp)
          .background(Color(0xFF141416))
          .padding(14.dp)
      ) {
        if (habitsToShow.isEmpty()) {
          GlanceText(
            text = "Add habits to see Habit Rings",
            style = GlanceTextStyle(color = ColorProvider(Color(0xFF8E8F98)), fontSize = 13.sp),
            modifier = GlanceModifier.clickable(
              actionStartActivity(Intent(context, MainActivity::class.java))
            )
          )
        } else {
          rows.forEachIndexed { rowIdx, rowHabits ->
            if (rowIdx > 0) {
              GlanceSpacer(modifier = GlanceModifier.height(10.dp))
            }
            GlanceRow(
              verticalAlignment = GlanceAlignment.CenterVertically,
              modifier = GlanceModifier.fillMaxWidth()
            ) {
              rowHabits.forEach { habit ->
                val hColor = parseHexColor(habit.colorKey)
                val streak = snap.streaksByHabit[habit.id] ?: 0
                val hEntries = snap.entriesByHabit[habit.id].orEmpty()
                val doneToday = ScheduleMatcher.isEntryDone(habit, hEntries[snap.today])
                val doneDaysThisWeek = snap.weekDays.count { d ->
                  ScheduleMatcher.isEntryDone(habit, hEntries[d])
                }
                val targetDays = habit.daysOfWeek.size.coerceAtLeast(1)
                val progress = (doneDaysThisWeek.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)
                val ringBmp = WidgetBitmapRenderer.renderHabitRingWithBadgeBitmap(
                  iconKey = habit.iconKey,
                  habitColorInt = hColor.toArgb(),
                  progressFraction = progress,
                  streak = streak,
                  showStreak = snap.showStreak,
                  sizePx = 152
                )

                GlanceBox(
                  contentAlignment = GlanceAlignment.Center,
                  modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                      actionSendBroadcast(
                        if (doneToday) {
                          HabitRadarGlanceAppWidgetReceiver.createToggleHabitDoneIntent(
                            context = context,
                            receiverClass = ProgressRingWidgetReceiver::class.java,
                            habitId = habit.id,
                            date = snap.today
                          )
                        } else {
                          HabitRadarGlanceAppWidgetReceiver.createMarkHabitDoneIntent(
                            context = context,
                            receiverClass = ProgressRingWidgetReceiver::class.java,
                            habitId = habit.id,
                            date = snap.today
                          )
                        }
                      )
                    )
                ) {
                  Image(
                    provider = ImageProvider(ringBmp),
                    contentDescription = "${habit.name} ring",
                    modifier = GlanceModifier.size(62.dp)
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

// 4. SINGLE HABIT HEATMAP WIDGET ("Habit — consistency heatmap with a check button (small & medium).")
class SingleHabitGlanceWidget : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(DpSize(140.dp, 110.dp), DpSize(250.dp, 130.dp), DpSize(320.dp, 160.dp))
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context, id)
    val habit = snap.habits.firstOrNull { it.name.equals("Walk", ignoreCase = true) }
      ?: snap.habits.firstOrNull()

    provideContent {
      val size = LocalSize.current
      val columns = if (size.width < 200.dp) 8 else 15

      if (habit == null) {
        GlanceBox(
          contentAlignment = GlanceAlignment.Center,
          modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(Color(0xFF141D16))
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        ) {
          GlanceText(
            text = "Add a habit",
            style = GlanceTextStyle(color = ColorProvider(Color.White), fontSize = 13.sp)
          )
        }
      } else {
        val hColor = parseHexColor(habit.colorKey)
        val hColorArgb = hColor.toArgb()
        val hEntries = snap.entriesByHabit[habit.id].orEmpty()
        val doneToday = ScheduleMatcher.isEntryDone(habit, hEntries[snap.today])
        val streak = snap.streaksByHabit[habit.id] ?: 0
        val iconBmp = WidgetBitmapRenderer.renderHabitIconBadgeBitmap(habit.iconKey, hColorArgb, 84)
        val checkBtnBmp = WidgetBitmapRenderer.renderCheckButtonBitmap(doneToday, hColorArgb, 84)
        val heatmapBmp = WidgetBitmapRenderer.renderHeatmapGridBitmap(
          habit = habit,
          entriesMap = hEntries,
          today = snap.today,
          weekDays = snap.weekDays,
          habitColorInt = hColorArgb,
          columns = columns,
          widthPx = if (columns == 15) 660 else 360,
          heightPx = 260
        )
        val cardBg = Color(
          red = (0x10 / 255f) * 0.88f + hColor.red * 0.12f,
          green = (0x13 / 255f) * 0.88f + hColor.green * 0.12f,
          blue = (0x11 / 255f) * 0.88f + hColor.blue * 0.12f,
          alpha = 1f
        )
        val toggleAction = actionSendBroadcast(
          if (doneToday) {
            HabitRadarGlanceAppWidgetReceiver.createToggleHabitDoneIntent(
              context = context,
              receiverClass = SingleHabitWidgetReceiver::class.java,
              habitId = habit.id,
              date = snap.today
            )
          } else {
            HabitRadarGlanceAppWidgetReceiver.createMarkHabitDoneIntent(
              context = context,
              receiverClass = SingleHabitWidgetReceiver::class.java,
              habitId = habit.id,
              date = snap.today
            )
          }
        )

        GlanceColumn(
          modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(cardBg)
            .padding(14.dp)
        ) {
          // Top Header Row: Tinted Icon Badge + Title/Streak + Check Button
          GlanceRow(
            verticalAlignment = GlanceAlignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 10.dp)
          ) {
            Image(
              provider = ImageProvider(iconBmp),
              contentDescription = habit.name,
              modifier = GlanceModifier.size(36.dp)
            )
            GlanceSpacer(modifier = GlanceModifier.width(10.dp))
            GlanceColumn(modifier = GlanceModifier.defaultWeight()) {
              GlanceText(
                text = if (snap.hideNames) "Habit" else habit.name,
                maxLines = 1,
                style = GlanceTextStyle(
                  color = ColorProvider(Color.White),
                  fontSize = 15.sp,
                  fontWeight = GlanceFontWeight.Bold
                )
              )
              if (snap.showStreak) {
                GlanceText(
                  text = "🔥 $streak",
                  style = GlanceTextStyle(
                    color = ColorProvider(Color(0xFFA0A8A2)),
                    fontSize = 11.sp,
                    fontWeight = GlanceFontWeight.Medium
                  )
                )
              }
            }
            Image(
              provider = ImageProvider(checkBtnBmp),
              contentDescription = "Mark ${habit.name} done",
              modifier = GlanceModifier
                .size(34.dp)
                .clickable(toggleAction)
            )
          }

          // Consistency Heatmap Grid
          Image(
            provider = ImageProvider(heatmapBmp),
            contentDescription = "${habit.name} consistency heatmap",
            modifier = GlanceModifier
              .fillMaxWidth()
              .defaultWeight()
              .clickable(toggleAction)
          )
        }
      }
    }
  }
}

// 5. GOAL PROGRESS WIDGET
class GoalProgressGlanceWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val snap = loadWidgetSnapshot(context, id)
    val goal = snap.goals.firstOrNull()
    val steps = goal?.let { snap.stepsByGoal[it.id].orEmpty() }.orEmpty()
    val doneCount = steps.count { it.done }
    val nextStep = steps.firstOrNull { !it.done }

    provideContent {
      GlanceColumn(
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(24.dp)
          .background(Color(0xFF141416))
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
                  actionSendBroadcast(
                    HabitRadarGlanceAppWidgetReceiver.createMarkStepDoneIntent(
                      context = context,
                      receiverClass = GoalProgressWidgetReceiver::class.java,
                      stepId = nextStep.id,
                      goalId = goal.id
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
                text = "✓ Mark done",
                style = GlanceTextStyle(
                  color = ColorProvider(Color.White),
                  fontSize = 12.sp,
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

/**
 * Base [GlanceAppWidgetReceiver] that handles direct button interactions from home-screen widgets
 * (such as marking a habit as 'done', toggling a habit entry, or completing a goal step),
 * updating the local Room database immediately and refreshing all Glance widgets.
 */
abstract class HabitRadarGlanceAppWidgetReceiver : GlanceAppWidgetReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ACTION_MARK_HABIT_DONE,
      ACTION_TOGGLE_HABIT_DONE,
      ACTION_MARK_STEP_DONE -> {
        val pendingResult = goAsync()
        try {
          // Immediately update the local Room database so completion state is persisted right away
          runBlocking(Dispatchers.IO) {
            handleWidgetActionIntent(context, intent)
          }
        } finally {
          CoroutineScope(Dispatchers.IO).launch {
            try {
              WidgetUpdater.updateAll(context)
            } finally {
              pendingResult?.finish()
            }
          }
        }
      }
      else -> super.onReceive(context, intent)
    }
  }

  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    super.onDeleted(context, appWidgetIds)
    val repo = context.appContainer.repository
    runBlocking(Dispatchers.IO) {
      appWidgetIds.forEach { appWidgetId ->
        repo.deleteWidgetConfig(appWidgetId)
      }
    }
  }

  companion object {
    const val ACTION_MARK_HABIT_DONE = "com.example.feature.widgets.glance.ACTION_MARK_HABIT_DONE"
    const val ACTION_TOGGLE_HABIT_DONE = "com.example.feature.widgets.glance.ACTION_TOGGLE_HABIT_DONE"
    const val ACTION_MARK_STEP_DONE = "com.example.feature.widgets.glance.ACTION_MARK_STEP_DONE"

    const val EXTRA_HABIT_ID = "habit_id"
    const val EXTRA_DATE_STR = "date_str"
    const val EXTRA_STEP_ID = "step_id"
    const val EXTRA_GOAL_ID = "goal_id"

    suspend fun handleWidgetActionIntent(context: Context, intent: Intent): Boolean {
      val repo = context.appContainer.repository
      return when (intent.action) {
        ACTION_MARK_HABIT_DONE -> {
          val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
            ?: intent.data?.getQueryParameter(EXTRA_HABIT_ID)
            ?: return false
          val dateStr = intent.getStringExtra(EXTRA_DATE_STR)
            ?: intent.data?.getQueryParameter(EXTRA_DATE_STR)
          val date = dateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: repo.getLogicalToday()
          repo.markHabitDoneOnDate(habitId, date)
        }
        ACTION_TOGGLE_HABIT_DONE -> {
          val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
            ?: intent.data?.getQueryParameter(EXTRA_HABIT_ID)
            ?: return false
          val dateStr = intent.getStringExtra(EXTRA_DATE_STR)
            ?: intent.data?.getQueryParameter(EXTRA_DATE_STR)
          val date = dateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: repo.getLogicalToday()
          repo.toggleHabitDoneOnDate(habitId, date)
        }
        ACTION_MARK_STEP_DONE -> {
          val stepId = intent.getStringExtra(EXTRA_STEP_ID)
            ?: intent.data?.getQueryParameter(EXTRA_STEP_ID)
            ?: return false
          val goalId = intent.getStringExtra(EXTRA_GOAL_ID)
            ?: intent.data?.getQueryParameter(EXTRA_GOAL_ID)
            ?: return false
          repo.toggleStepDone(stepId, goalId)
        }
        else -> false
      }
    }

    fun createMarkHabitDoneIntent(
      context: Context,
      receiverClass: Class<out HabitRadarGlanceAppWidgetReceiver> = TodayListWidgetReceiver::class.java,
      habitId: String,
      date: LocalDate? = null
    ): Intent {
      val dateToken = date?.toString() ?: "today"
      return Intent(context, receiverClass).apply {
        action = ACTION_MARK_HABIT_DONE
        data = Uri.parse(
          "habitradar://widget/mark_done?habit_id=${Uri.encode(habitId)}&date_str=${Uri.encode(dateToken)}"
        )
        putExtra(EXTRA_HABIT_ID, habitId)
        if (date != null) {
          putExtra(EXTRA_DATE_STR, date.toString())
        }
      }
    }

    fun createToggleHabitDoneIntent(
      context: Context,
      receiverClass: Class<out HabitRadarGlanceAppWidgetReceiver> = WeekGridWidgetReceiver::class.java,
      habitId: String,
      date: LocalDate? = null
    ): Intent {
      val dateToken = date?.toString() ?: "today"
      return Intent(context, receiverClass).apply {
        action = ACTION_TOGGLE_HABIT_DONE
        data = Uri.parse(
          "habitradar://widget/toggle_done?habit_id=${Uri.encode(habitId)}&date_str=${Uri.encode(dateToken)}"
        )
        putExtra(EXTRA_HABIT_ID, habitId)
        if (date != null) {
          putExtra(EXTRA_DATE_STR, date.toString())
        }
      }
    }

    fun createMarkStepDoneIntent(
      context: Context,
      receiverClass: Class<out HabitRadarGlanceAppWidgetReceiver> = GoalProgressWidgetReceiver::class.java,
      stepId: String,
      goalId: String
    ): Intent {
      return Intent(context, receiverClass).apply {
        action = ACTION_MARK_STEP_DONE
        data = Uri.parse(
          "habitradar://widget/mark_step_done?step_id=${Uri.encode(stepId)}&goal_id=${Uri.encode(goalId)}"
        )
        putExtra(EXTRA_STEP_ID, stepId)
        putExtra(EXTRA_GOAL_ID, goalId)
      }
    }
  }
}

class WeekGridWidgetReceiver : HabitRadarGlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = WeekGridGlanceWidget()
}

class TodayListWidgetReceiver : HabitRadarGlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = TodayListGlanceWidget()
}

class ProgressRingWidgetReceiver : HabitRadarGlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ProgressRingGlanceWidget()
}

class SingleHabitWidgetReceiver : HabitRadarGlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = SingleHabitGlanceWidget()
}

class GoalProgressWidgetReceiver : HabitRadarGlanceAppWidgetReceiver() {
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
