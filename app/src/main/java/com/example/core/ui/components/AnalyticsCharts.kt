package com.example.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.WeekStartDay
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.domain.usecase.TrendBucket
import com.example.core.domain.usecase.WeekdayStat
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun MomentumRingView(
  score: Int,
  color: Color,
  size: Dp = 92.dp,
  strokeWidth: Dp = 9.dp
) {
  val colors = LocalHabitRadarColors.current
  Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
    Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
      drawArc(
        color = colors.surface2,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
      )
      drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
        useCenter = false,
        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = score.toString(),
        fontFamily = PlusJakartaSansFamily,
        fontSize = (size.value * 0.28f).sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
      )
    }
  }
}

@Composable
fun TrendLineChart(
  buckets: List<TrendBucket>,
  primaryColor: Color,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  var selectedIdx by remember(buckets) { mutableIntStateOf(-1) }

  if (buckets.isEmpty()) return

  Column(modifier = modifier.fillMaxWidth()) {
    if (selectedIdx in buckets.indices) {
      val b = buckets[selectedIdx]
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(colors.surface2)
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Text(
          text = b.label,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = colors.textPrimary
        )
        Text(
          text = "Rate: ${b.rate.roundToInt()}% · 4-pt MA: ${b.movingAverage.roundToInt()}% · Prev: ${b.previousRate.roundToInt()}%",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 12.sp,
          color = primaryColor
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
    }

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(170.dp)
        .pointerInput(buckets) {
          detectTapGestures { offset ->
            val stepX = if (buckets.size > 1) size.width / (buckets.size - 1).toFloat() else size.width.toFloat()
            val idx = ((offset.x / stepX).roundToInt()).coerceIn(0, buckets.lastIndex)
            selectedIdx = idx
          }
        }
    ) {
      val w = size.width
      val h = size.height - 24.dp.toPx()
      val n = buckets.size

      // Horizontal grid lines (25%, 50%, 75%, 100%)
      listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { frac ->
        val y = h * (1f - frac)
        drawLine(
          color = colors.surface2,
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1.dp.toPx()
        )
      }

      fun pointAt(idx: Int, value: Float): Offset {
        val x = if (n > 1) (idx.toFloat() / (n - 1)) * w else w / 2f
        val y = h * (1f - (value.coerceIn(0f, 100f) / 100f))
        return Offset(x, y)
      }

      // 1. Previous period dashed ghost line
      if (n > 1) {
        val prevPath = Path()
        buckets.forEachIndexed { idx, b ->
          val pt = pointAt(idx, b.previousRate)
          if (idx == 0) prevPath.moveTo(pt.x, pt.y) else prevPath.lineTo(pt.x, pt.y)
        }
        drawPath(
          path = prevPath,
          color = colors.textSecondary.copy(alpha = 0.45f),
          style = Stroke(
            width = 1.8.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
          )
        )
      }

      // 2. 4-point Moving Average smooth overlay
      if (n > 1) {
        val maPath = Path()
        buckets.forEachIndexed { idx, b ->
          val pt = pointAt(idx, b.movingAverage)
          if (idx == 0) maPath.moveTo(pt.x, pt.y) else maPath.lineTo(pt.x, pt.y)
        }
        drawPath(
          path = maPath,
          color = primaryColor.copy(alpha = 0.45f),
          style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
      }

      // 3. Primary completion rate line + nodes
      if (n > 1) {
        val mainPath = Path()
        buckets.forEachIndexed { idx, b ->
          val pt = pointAt(idx, b.rate)
          if (idx == 0) mainPath.moveTo(pt.x, pt.y) else mainPath.lineTo(pt.x, pt.y)
        }
        drawPath(
          path = mainPath,
          color = primaryColor,
          style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        )
      }

      buckets.forEachIndexed { idx, b ->
        val pt = pointAt(idx, b.rate)
        drawCircle(
          color = if (idx == selectedIdx) Color.White else primaryColor,
          radius = if (idx == selectedIdx) 5.5.dp.toPx() else 3.5.dp.toPx(),
          center = pt
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      val step = max(1, buckets.size / 5)
      buckets.forEachIndexed { idx, b ->
        if (idx == 0 || idx == buckets.lastIndex || idx % step == 0) {
          Text(
            text = b.label,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            color = colors.textSecondary
          )
        }
      }
    }
  }
}

@Composable
fun WeekdayPerformanceBars(
  stats: List<WeekdayStat>,
  bestDow: Int?,
  worstDow: Int?,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  val greenTint = Color(0xFFA8DFB4)
  val redTint = Color(0xFFEE9A9A)

  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom,
    modifier = modifier
      .fillMaxWidth()
      .height(150.dp)
      .padding(top = 8.dp)
  ) {
    stats.forEach { item ->
      val barColor = when (item.isoDow) {
        bestDow -> greenTint
        worstDow -> if (bestDow != worstDow) redTint else accentColor
        else -> accentColor.copy(alpha = 0.65f)
      }
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
      ) {
        Text(
          text = "${item.rate}%",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 11.sp,
          color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
          contentAlignment = Alignment.BottomCenter,
          modifier = Modifier
            .width(24.dp)
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface2)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .fillMaxHeight((item.rate / 100f).coerceIn(0.06f, 1f))
              .clip(RoundedCornerShape(10.dp))
              .background(barColor)
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = item.shortName,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = colors.textPrimary
        )
      }
    }
  }
}

@Composable
fun HourlyHistogramChart(
  hourlyCounts: List<Int>,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  val maxCount = (hourlyCounts.maxOrNull() ?: 1).coerceAtLeast(1)

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.Bottom,
      modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
    ) {
      hourlyCounts.forEachIndexed { hour, count ->
        val ratio = (count.toFloat() / maxCount.toFloat()).coerceIn(0.06f, 1f)
        val isPeak = count == maxCount && count > 0
        Box(
          contentAlignment = Alignment.BottomCenter,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .fillMaxHeight(ratio)
              .clip(RoundedCornerShape(4.dp))
              .background(if (isPeak) accentColor else accentColor.copy(alpha = 0.42f))
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      listOf("00:00", "06:00", "12:00", "18:00", "23:00").forEach { lbl ->
        Text(
          text = lbl,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 11.sp,
          color = colors.textSecondary
        )
      }
    }
  }
}

@Composable
fun VolumeBarChart(
  buckets: List<TrendBucket>,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  if (buckets.isEmpty()) return
  val maxVal = buckets.maxOf { max(it.checkInsCount, it.scheduledCount) }.coerceAtLeast(1)

  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
    ) {
      val w = size.width
      val h = size.height
      val count = buckets.size
      val slotWidth = w / count
      val barWidth = (slotWidth * 0.55f).coerceAtMost(28.dp.toPx())

      buckets.forEachIndexed { idx, b ->
        val xCenter = idx * slotWidth + slotWidth / 2f
        val barH = (b.checkInsCount.toFloat() / maxVal.toFloat()) * (h - 12.dp.toPx())
        drawRoundRect(
          color = accentColor,
          topLeft = Offset(xCenter - barWidth / 2f, h - barH),
          size = Size(barWidth, barH.coerceAtLeast(4.dp.toPx())),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        // Target line marker per bucket
        val targetY = h - (b.scheduledCount.toFloat() / maxVal.toFloat()) * (h - 12.dp.toPx())
        drawLine(
          color = colors.textPrimary.copy(alpha = 0.65f),
          start = Offset(xCenter - slotWidth * 0.38f, targetY),
          end = Offset(xCenter + slotWidth * 0.38f, targetY),
          strokeWidth = 2.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
        )
      }
    }
  }
}

@Composable
fun MonthCalendarHeatmap(
  yearMonth: YearMonth,
  habit: Habit,
  entriesMap: Map<LocalDate, Entry>,
  pauses: List<HabitPause>,
  weekStart: WeekStartDay,
  today: LocalDate,
  habitColor: Color,
  onDayClick: (LocalDate) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  val orderedDows = remember(weekStart) { LogicalDateCalculator.orderedDaysOfWeek(weekStart) }
  val firstOfMonth = yearMonth.atDay(1)
  val leadingEmpty = orderedDows.indexOf(firstOfMonth.dayOfWeek).coerceAtLeast(0)
  val totalDays = yearMonth.lengthOfMonth()
  val totalCells = ((leadingEmpty + totalDays + 6) / 7) * 7

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
      orderedDows.forEach { dow ->
        Text(
          text = ScheduleMatcher.dowShortLabel(dow.value).take(2),
          fontFamily = PlusJakartaSansFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = colors.textSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f)
        )
      }
    }

    for (week in 0 until (totalCells / 7)) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
      ) {
        for (col in 0..6) {
          val cellIndex = week * 7 + col
          val dayNum = cellIndex - leadingEmpty + 1
          if (dayNum in 1..totalDays) {
            val date = yearMonth.atDay(dayNum)
            val entry = entriesMap[date]
            val done = ScheduleMatcher.isEntryDone(habit, entry)
            val skipped = entry?.status == EntryStatus.SKIPPED
            val scheduled = ScheduleMatcher.isScheduledOn(habit, date, pauses)
            val isToday = date == today
            val isFuture = date.isAfter(today)

            val cellBg = when {
              done -> habitColor.copy(alpha = 0.85f)
              skipped -> habitColor.copy(alpha = 0.16f)
              !scheduled || isFuture -> colors.surface2.copy(alpha = 0.45f)
              else -> habitColor.copy(alpha = 0.12f)
            }
            val borderStroke = when {
              isToday -> 2.dp to colors.textPrimary
              skipped -> 1.dp to habitColor.copy(alpha = 0.55f)
              scheduled && !done && !isFuture -> 1.dp to habitColor.copy(alpha = 0.35f)
              else -> 0.dp to Color.Transparent
            }

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(cellBg)
                .then(
                  if (borderStroke.first > 0.dp) {
                    Modifier.border(borderStroke.first, borderStroke.second, RoundedCornerShape(10.dp))
                  } else Modifier
                )
                .clickable(enabled = !isFuture) { onDayClick(date) }
            ) {
              Text(
                text = if (skipped) "—" else dayNum.toString(),
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.sp,
                fontWeight = if (done || isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (done) Color(0xFF101114) else colors.textPrimary.copy(alpha = if (isFuture) 0.35f else 0.9f)
              )
            }
          } else {
            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
          }
        }
      }
    }
  }
}

@Composable
fun YearGitHubHeatmap(
  today: LocalDate,
  color: Color,
  isDoneForDate: (LocalDate) -> Float, // 0f..1f intensity
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  val weekColumns = remember(today) {
    (52 downTo 0).map { w ->
      val colStart = today.minusWeeks(w.toLong()).with(java.time.DayOfWeek.MONDAY)
      (0..6).map { d -> colStart.plusDays(d.toLong()) }
    }
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(3.dp),
      modifier = Modifier.padding(end = 6.dp)
    ) {
      listOf("M", "T", "W", "T", "F", "S", "S").forEach { l ->
        Text(
          text = l,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 9.sp,
          color = colors.textSecondary,
          modifier = Modifier.height(11.dp)
        )
      }
    }
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      modifier = Modifier.weight(1f)
    ) {
      items(weekColumns) { weekDates ->
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          weekDates.forEach { d ->
            val intensity = if (d.isAfter(today)) 0f else isDoneForDate(d).coerceIn(0f, 1f)
            val sqColor = if (intensity > 0f) {
              color.copy(alpha = (0.3f + 0.7f * intensity).coerceIn(0.3f, 1f))
            } else {
              colors.surface2.copy(alpha = 0.65f)
            }
            Box(
              modifier = Modifier
                .size(11.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(sqColor)
            )
          }
        }
      }
    }
  }
}
