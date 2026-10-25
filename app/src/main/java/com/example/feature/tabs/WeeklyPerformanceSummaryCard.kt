package com.example.feature.tabs

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.model.DailyWeeklyPerformancePoint
import com.example.core.domain.model.WeeklyPerformanceSummary
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Summary card displayed at the top of the main screen showing the user's weekly
 * habit completion performance using calculated percentages from the Room database.
 */
@Composable
fun WeeklyPerformanceSummaryCard(
  summary: WeeklyPerformanceSummary,
  modifier: Modifier = Modifier,
  onOpenAnalytics: (() -> Unit)? = null
) {
  val colors = LocalHabitRadarColors.current
  val rangeFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH) }
  val weekRangeLabel = remember(summary.weekStartDate, summary.weekEndDate) {
    "${summary.weekStartDate.format(rangeFormatter).uppercase()} – ${summary.weekEndDate.format(rangeFormatter).uppercase()}"
  }

  val animatedProgress by animateFloatAsState(
    targetValue = (summary.completionPercentage.coerceIn(0, 100) / 100f),
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "weekly_performance_progress"
  )

  val cardShape = RoundedCornerShape(26.dp)
  val gradientBrush = Brush.linearGradient(
    colors = listOf(
      colors.accent.copy(alpha = if (colors.isDark) 0.16f else 0.12f),
      colors.card,
      colors.surface2.copy(alpha = 0.85f)
    )
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(cardShape)
      .background(brush = gradientBrush)
      .border(1.dp, colors.accent.copy(alpha = 0.28f), cardShape)
      .then(
        if (onOpenAnalytics != null) {
          Modifier.clickable(onClick = onOpenAnalytics)
        } else {
          Modifier
        }
      )
      .padding(horizontal = 18.dp, vertical = 16.dp)
      .testTag("weekly_performance_summary_card")
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Top row: Overline + Date Range + Status Badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = "WEEKLY HABIT PERFORMANCE",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = colors.accent
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = weekRangeLabel,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            color = colors.textSecondary
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.accentContainer)
            .padding(horizontal = 11.dp, vertical = 5.dp)
            .testTag("weekly_performance_status_badge")
        ) {
          Text(
            text = summary.statusLabel,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Middle row: Big Percentage + Delta + Sub-metrics on left, Radial Ring Gauge on right
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "${summary.completionPercentage}%",
              fontFamily = InstrumentSerifFamily,
              fontSize = 44.sp,
              lineHeight = 44.sp,
              color = colors.textPrimary,
              modifier = Modifier.testTag("weekly_performance_percentage_text")
            )

            val deltaText = when {
              summary.deltaPercentage > 0 -> "+${summary.deltaPercentage}% vs last wk"
              summary.deltaPercentage < 0 -> "-${abs(summary.deltaPercentage)}% vs last wk"
              else -> "Steady vs last wk"
            }
            val deltaColor = when {
              summary.deltaPercentage > 0 -> Color(0xFF81C784)
              summary.deltaPercentage < 0 -> Color(0xFFE57373)
              else -> colors.textSecondary
            }

            Box(
              modifier = Modifier
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface2)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("weekly_performance_delta_badge")
            ) {
              Text(
                text = deltaText,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = deltaColor
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Counts & Best day metadata
          val bestDaySuffix = if (summary.bestDayLabel != null) {
            " · Best: ${summary.bestDayLabel} (${summary.bestDayPercentage}%)"
          } else {
            ""
          }
          Text(
            text = "${summary.completedCount}/${summary.scheduledCount} completed · ${summary.perfectDaysCount} perfect ${if (summary.perfectDaysCount == 1) "day" else "days"}$bestDaySuffix",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.testTag("weekly_performance_counts_text")
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Radial circular completion ring gauge
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(64.dp)
            .testTag("weekly_performance_ring_gauge")
        ) {
          val trackColor = colors.surface2
          val ringColor = colors.accent
          Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 7.dp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
              color = trackColor,
              startAngle = -90f,
              sweepAngle = 360f,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
              color = ringColor,
              startAngle = -90f,
              sweepAngle = 360f * animatedProgress,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
          }
          Text(
            text = "${summary.completedCount}/${summary.scheduledCount}",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
          )
        }
      }

      if (summary.dailyBreakdown.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))

        // 7-day daily completion percentage mini-bars
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.Bottom,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_performance_daily_strip")
        ) {
          summary.dailyBreakdown.forEach { dayPoint ->
            DailyWeeklyBarColumn(
              point = dayPoint,
              accentColor = colors.accent,
              trackColor = colors.surface2,
              textPrimary = colors.textPrimary,
              textSecondary = colors.textSecondary,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DailyWeeklyBarColumn(
  point: DailyWeeklyPerformancePoint,
  accentColor: Color,
  trackColor: Color,
  textPrimary: Color,
  textSecondary: Color,
  modifier: Modifier = Modifier
) {
  val fillFraction = (point.completionPercentage.coerceIn(0, 100) / 100f)
  val pctText = if (point.isFuture && point.completedCount == 0) {
    "–"
  } else {
    "${point.completionPercentage}%"
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (point.isToday) accentColor.copy(alpha = 0.14f) else Color.Transparent)
      .padding(vertical = 6.dp, horizontal = 2.dp)
      .testTag("weekly_day_column_${point.dayShortLabel}")
  ) {
    Text(
      text = pctText,
      fontFamily = PlusJakartaSansFamily,
      fontSize = 10.sp,
      fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Medium,
      color = if (point.isToday) accentColor else textSecondary,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(4.dp))
    Box(
      contentAlignment = Alignment.BottomCenter,
      modifier = Modifier
        .width(10.dp)
        .height(28.dp)
        .clip(CircleShape)
        .background(trackColor)
    ) {
      if (fillFraction > 0f) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(fillFraction.coerceAtLeast(0.15f))
            .clip(CircleShape)
            .background(if (point.completionPercentage == 100) accentColor else accentColor.copy(alpha = 0.72f))
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = point.dayShortLabel,
      fontFamily = PlusJakartaSansFamily,
      fontSize = 11.sp,
      fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Medium,
      color = if (point.isToday) textPrimary else textSecondary,
      textAlign = TextAlign.Center
    )
  }
}
