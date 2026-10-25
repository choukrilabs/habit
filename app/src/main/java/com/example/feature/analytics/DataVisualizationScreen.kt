package com.example.feature.analytics

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.Entry
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitCategoryCatalog
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.WeekStartDay
import com.example.core.domain.usecase.AnalyticsCalculator
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.ui.components.HabitIconView
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.components.HabitRadarSegmentedControl
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.components.SubPageTopBar
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.example.core.ui.theme.habitTint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Timeframe modes for the Data Visualization bar chart screen.
 */
enum class VisualizationTimeframe(val label: String) {
  WEEKLY("Weekly"),
  MONTHLY("Monthly")
}

/**
 * Granularity sub-modes for Weekly and Monthly bar charts.
 */
enum class VisualizationGranularity(val label: String) {
  DAILY_IN_WEEK("7 Days"),
  MULTI_WEEK("8 Weeks"),
  SIX_MONTHS("6 Months"),
  TWELVE_MONTHS("12 Months")
}

/**
 * Represents a single bar bucket in the Recharts-style bar chart.
 */
data class BarChartDataPoint(
  val key: String,
  val xAxisLabel: String,
  val dateRangeStamp: String,
  val completionRate: Int,
  val previousCompletionRate: Int,
  val completedCount: Int,
  val scheduledCount: Int,
  val isCurrentBucket: Boolean = false
)

/**
 * Summary statistics for the currently active Weekly or Monthly visualization dataset.
 */
data class CompletionVisualizationSummary(
  val timeframe: VisualizationTimeframe,
  val titleLabel: String,
  val subtitleRange: String,
  val dataPoints: List<BarChartDataPoint>,
  val averageRate: Int,
  val previousAverageRate: Int,
  val peakBucket: BarChartDataPoint?,
  val lowestBucket: BarChartDataPoint?,
  val totalCompleted: Int,
  val totalScheduled: Int
) {
  val periodDelta: Int
    get() = averageRate - previousAverageRate
}

/**
 * Pure domain calculator that transforms date-stamped habit entries into
 * Weekly and Monthly bar chart datasets for the Recharts-style visualization components.
 */
object CompletionRateChartCalculator {

  fun computeWeeklyDailySeries(
    habits: List<Habit>,
    entriesByHabit: Map<String, Map<LocalDate, Entry>>,
    pausesByHabit: Map<String, List<HabitPause>>,
    weekStart: WeekStartDay,
    today: LocalDate,
    weekOffset: Int = 0
  ): CompletionVisualizationSummary {
    val startOfTargetWeek = LogicalDateCalculator.startOfWeek(today, weekStart).plusWeeks(weekOffset.toLong())
    val endOfTargetWeek = startOfTargetWeek.plusDays(6)
    val rangeFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    val points = (0..6).map { dayIdx ->
      val date = startOfTargetWeek.plusDays(dayIdx.toLong())
      val prevDate = date.minusWeeks(1)
      val (rate, doneCount, schedCount) = computeAggregateDayStats(habits, entriesByHabit, pausesByHabit, date, today)
      val (prevRate, _, _) = computeAggregateDayStats(habits, entriesByHabit, pausesByHabit, prevDate, today)

      BarChartDataPoint(
        key = date.toString(),
        xAxisLabel = ScheduleMatcher.dowShortLabel(date.dayOfWeek.value),
        dateRangeStamp = date.toString(),
        completionRate = rate,
        previousCompletionRate = prevRate,
        completedCount = doneCount,
        scheduledCount = schedCount,
        isCurrentBucket = date == today
      )
    }

    return buildSummary(
      timeframe = VisualizationTimeframe.WEEKLY,
      titleLabel = if (weekOffset == 0) "This Week (Daily)" else "Week of ${startOfTargetWeek.format(rangeFmt)}",
      subtitleRange = "${startOfTargetWeek.format(rangeFmt)} – ${endOfTargetWeek.format(rangeFmt)} (${startOfTargetWeek} → ${endOfTargetWeek})",
      points = points
    )
  }

  fun computeWeeklyMultiWeekSeries(
    habits: List<Habit>,
    entriesByHabit: Map<String, Map<LocalDate, Entry>>,
    pausesByHabit: Map<String, List<HabitPause>>,
    weekStart: WeekStartDay,
    today: LocalDate,
    weeksCount: Int = 8,
    weekOffset: Int = 0
  ): CompletionVisualizationSummary {
    val anchorWeekStart = LogicalDateCalculator.startOfWeek(today, weekStart).plusWeeks(weekOffset.toLong())
    val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    val points = ((weeksCount - 1) downTo 0).map { w ->
      val wStart = anchorWeekStart.minusWeeks(w.toLong())
      val wEnd = wStart.plusDays(6).let { if (it.isAfter(today)) today else it }
      val prevStart = wStart.minusWeeks(weeksCount.toLong())
      val prevEnd = prevStart.plusDays(6)

      val (rate, doneCount, schedCount) = computeAggregateRangeStats(habits, entriesByHabit, pausesByHabit, wStart, wEnd)
      val (prevRate, _, _) = computeAggregateRangeStats(habits, entriesByHabit, pausesByHabit, prevStart, prevEnd)

      BarChartDataPoint(
        key = "${wStart}_$wEnd",
        xAxisLabel = wStart.format(DateTimeFormatter.ofPattern("d/M", Locale.ENGLISH)),
        dateRangeStamp = "$wStart → $wEnd",
        completionRate = rate,
        previousCompletionRate = prevRate,
        completedCount = doneCount,
        scheduledCount = schedCount,
        isCurrentBucket = w == 0 && weekOffset == 0
      )
    }

    val firstStart = anchorWeekStart.minusWeeks((weeksCount - 1).toLong())
    val lastEnd = anchorWeekStart.plusDays(6).let { if (it.isAfter(today)) today else it }
    return buildSummary(
      timeframe = VisualizationTimeframe.WEEKLY,
      titleLabel = "Weekly Completion Trend ($weeksCount Weeks)",
      subtitleRange = "${firstStart.format(shortFmt)} – ${lastEnd.format(shortFmt)}",
      points = points
    )
  }

  fun computeMonthlySeries(
    habits: List<Habit>,
    entriesByHabit: Map<String, Map<LocalDate, Entry>>,
    pausesByHabit: Map<String, List<HabitPause>>,
    today: LocalDate,
    monthsCount: Int = 6,
    monthOffset: Int = 0
  ): CompletionVisualizationSummary {
    val anchorYm = YearMonth.from(today).plusMonths(monthOffset.toLong())
    val monthLabelFmt = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    val fullMonthFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

    val points = ((monthsCount - 1) downTo 0).map { m ->
      val ym = anchorYm.minusMonths(m.toLong())
      val mStart = ym.atDay(1)
      val mEnd = ym.atEndOfMonth().let { if (it.isAfter(today)) today else it }

      val prevYm = ym.minusMonths(1)
      val prevStart = prevYm.atDay(1)
      val prevEnd = prevYm.atEndOfMonth()

      val (rate, doneCount, schedCount) = computeAggregateRangeStats(habits, entriesByHabit, pausesByHabit, mStart, mEnd)
      val (prevRate, _, _) = computeAggregateRangeStats(habits, entriesByHabit, pausesByHabit, prevStart, prevEnd)

      BarChartDataPoint(
        key = ym.toString(),
        xAxisLabel = mStart.format(monthLabelFmt),
        dateRangeStamp = "$mStart → $mEnd",
        completionRate = rate,
        previousCompletionRate = prevRate,
        completedCount = doneCount,
        scheduledCount = schedCount,
        isCurrentBucket = ym == YearMonth.from(today)
      )
    }

    val firstYm = anchorYm.minusMonths((monthsCount - 1).toLong())
    return buildSummary(
      timeframe = VisualizationTimeframe.MONTHLY,
      titleLabel = "Monthly Completion Rates ($monthsCount Months)",
      subtitleRange = "${firstYm.atDay(1).format(fullMonthFmt)} – ${anchorYm.atDay(1).format(fullMonthFmt)}",
      points = points
    )
  }

  private fun computeAggregateDayStats(
    habits: List<Habit>,
    entriesByHabit: Map<String, Map<LocalDate, Entry>>,
    pausesByHabit: Map<String, List<HabitPause>>,
    date: LocalDate,
    today: LocalDate
  ): Triple<Int, Int, Int> {
    if (habits.isEmpty() || date.isAfter(today)) return Triple(0, 0, 0)
    var done = 0
    var scheduled = 0
    habits.forEach { habit ->
      val pauses = pausesByHabit[habit.id].orEmpty()
      val entry = entriesByHabit[habit.id]?.get(date)
      val isSkipped = entry?.status == com.example.core.domain.model.EntryStatus.SKIPPED
      if (ScheduleMatcher.isScheduledOn(habit, date, pauses) && !isSkipped) {
        scheduled++
        if (ScheduleMatcher.isEntryDone(habit, entry)) {
          done++
        }
      } else if (ScheduleMatcher.isEntryDone(habit, entry)) {
        scheduled++
        done++
      }
    }
    val rate = if (scheduled > 0) ((done.toDouble() / scheduled.toDouble()) * 100.0).roundToInt().coerceIn(0, 100) else 0
    return Triple(rate, done, scheduled)
  }

  private fun computeAggregateRangeStats(
    habits: List<Habit>,
    entriesByHabit: Map<String, Map<LocalDate, Entry>>,
    pausesByHabit: Map<String, List<HabitPause>>,
    startDate: LocalDate,
    endDate: LocalDate
  ): Triple<Int, Int, Int> {
    if (habits.isEmpty() || endDate.isBefore(startDate)) return Triple(0, 0, 0)
    var totalDone = 0
    var totalScheduled = 0
    val perHabitRates = mutableListOf<Int>()

    habits.forEach { habit ->
      val entriesMap = entriesByHabit[habit.id].orEmpty()
      val pauses = pausesByHabit[habit.id].orEmpty()
      val habitRate = AnalyticsCalculator.computePeriodRate(
        habit,
        entriesMap,
        pauses,
        startDate,
        endDate
      )
      perHabitRates.add(habitRate)

      var d = startDate
      while (!d.isAfter(endDate)) {
        val e = entriesMap[d]
        val skipped = e?.status == com.example.core.domain.model.EntryStatus.SKIPPED
        if (ScheduleMatcher.isScheduledOn(habit, d, pauses) && !skipped) {
          totalScheduled++
          if (ScheduleMatcher.isEntryDone(habit, e)) {
            totalDone++
          }
        }
        d = d.plusDays(1)
      }
    }

    val avgRate = if (perHabitRates.isNotEmpty()) {
      perHabitRates.average().roundToInt().coerceIn(0, 100)
    } else 0

    return Triple(avgRate, totalDone, totalScheduled)
  }

  private fun buildSummary(
    timeframe: VisualizationTimeframe,
    titleLabel: String,
    subtitleRange: String,
    points: List<BarChartDataPoint>
  ): CompletionVisualizationSummary {
    val activePoints = points.filter { it.scheduledCount > 0 || it.completionRate > 0 }
    val avgRate = if (activePoints.isNotEmpty()) {
      activePoints.map { it.completionRate }.average().roundToInt()
    } else if (points.isNotEmpty()) {
      points.map { it.completionRate }.average().roundToInt()
    } else 0

    val prevAvg = if (points.isNotEmpty()) {
      points.map { it.previousCompletionRate }.average().roundToInt()
    } else 0

    val peak = activePoints.maxByOrNull { it.completionRate } ?: points.maxByOrNull { it.completionRate }
    val lowest = activePoints.minByOrNull { it.completionRate } ?: points.minByOrNull { it.completionRate }

    return CompletionVisualizationSummary(
      timeframe = timeframe,
      titleLabel = titleLabel,
      subtitleRange = subtitleRange,
      dataPoints = points,
      averageRate = avgRate,
      previousAverageRate = prevAvg,
      peakBucket = peak,
      lowestBucket = lowest,
      totalCompleted = points.sumOf { it.completedCount },
      totalScheduled = points.sumOf { it.scheduledCount }
    )
  }
}

/**
 * Declarative Recharts-inspired Responsive BarChart composable (`<ResponsiveContainer><BarChart>`).
 * Renders:
 * - `<CartesianGrid strokeDasharray="3 3" />`
 * - `<XAxis dataKey="xAxisLabel" />` and `<YAxis domain={[0, 100]} unit="%" />`
 * - `<Tooltip />` interactive inspector when any bar is tapped
 * - `<Legend />`
 * - `<ReferenceLine y={averageRate} />`
 * - `<Bar dataKey="completionRate" />` and optional comparison `<Bar dataKey="previousCompletionRate" />`
 */
@Composable
fun RechartsBarChart(
  data: List<BarChartDataPoint>,
  primaryBarColor: Color,
  comparisonBarColor: Color = Color(0xFF6E7180),
  showComparisonBars: Boolean = true,
  showAverageReferenceLine: Boolean = true,
  averageRate: Int = 0,
  modifier: Modifier = Modifier,
  selectedBarIndex: Int = -1,
  onSelectBarIndex: (Int) -> Unit = {}
) {
  val colors = LocalHabitRadarColors.current
  if (data.isEmpty()) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier
        .fillMaxWidth()
        .height(220.dp)
        .testTag("recharts_empty_state")
    ) {
      Text(
        text = "No completion data available for this range yet.",
        fontFamily = PlusJakartaSansFamily,
        fontSize = 13.sp,
        color = colors.textSecondary
      )
    }
    return
  }

  val activeIndex = if (selectedBarIndex in data.indices) selectedBarIndex else data.indexOfLast { it.isCurrentBucket }.coerceAtLeast(0)
  val selectedPoint = data.getOrNull(activeIndex)

  val animatedFractions = data.map { point ->
    animateFloatAsState(
      targetValue = (point.completionRate.coerceIn(0, 100) / 100f),
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
      ),
      label = "bar_height_${point.key}"
    ).value
  }

  val animatedPrevFractions = data.map { point ->
    animateFloatAsState(
      targetValue = (point.previousCompletionRate.coerceIn(0, 100) / 100f),
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
      ),
      label = "prev_bar_height_${point.key}"
    ).value
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("recharts_bar_chart_container")
  ) {
    // Recharts <Tooltip /> interactive header card
    if (selectedPoint != null) {
      val delta = selectedPoint.completionRate - selectedPoint.previousCompletionRate
      val deltaStr = if (delta >= 0) "+$delta%" else "$delta%"
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(colors.surface2)
          .border(1.dp, primaryBarColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp)
          .testTag("recharts_tooltip_card")
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${selectedPoint.xAxisLabel.uppercase()} · ${selectedPoint.dateRangeStamp}",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${selectedPoint.completionRate}% Completion (${selectedPoint.completedCount}/${selectedPoint.scheduledCount} check-ins)",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.testTag("recharts_tooltip_value")
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(primaryBarColor.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "$deltaStr vs prev",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBarColor
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
    }

    // Main Plot Area: <YAxis /> + <CartesianGrid /> + <Bar /> + <ReferenceLine />
    Row(
      verticalAlignment = Alignment.Top,
      modifier = Modifier
        .fillMaxWidth()
        .height(210.dp)
    ) {
      // Y-Axis labels (100%, 75%, 50%, 25%, 0%)
      Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
        modifier = Modifier
          .width(38.dp)
          .fillMaxHeight()
          .padding(bottom = 26.dp, end = 6.dp)
          .testTag("recharts_y_axis")
      ) {
        listOf("100%", "75%", "50%", "25%", "0%").forEach { tick ->
          Text(
            text = tick,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 10.sp,
            color = colors.textSecondary
          )
        }
      }

      // Canvas + X-Axis labels column
      Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .pointerInput(data) {
              detectTapGestures { tapOffset ->
                val slotWidth = size.width / data.size.toFloat()
                val tappedIndex = (tapOffset.x / slotWidth).toInt().coerceIn(0, data.lastIndex)
                onSelectBarIndex(tappedIndex)
              }
            }
            .testTag("recharts_bar_canvas")
        ) {
          val chartWidth = size.width
          val chartHeight = size.height
          val count = data.size
          val slotWidth = chartWidth / count.toFloat()
          val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

          // 1. <CartesianGrid strokeDasharray="3 3" />
          listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
            val y = chartHeight * (1f - frac)
            drawLine(
              color = colors.surface2,
              start = Offset(0f, y),
              end = Offset(chartWidth, y),
              strokeWidth = 1.dp.toPx(),
              pathEffect = if (frac > 0f) dashEffect else null
            )
          }

          // 2. Highlight active/selected column band (Recharts cursor highlight)
          if (activeIndex in data.indices) {
            val bandLeft = activeIndex * slotWidth + slotWidth * 0.08f
            drawRoundRect(
              color = primaryBarColor.copy(alpha = 0.08f),
              topLeft = Offset(bandLeft, 0f),
              size = Size(slotWidth * 0.84f, chartHeight),
              cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
          }

          // 3. Draw Bars (<Bar dataKey="previousCompletionRate" /> & <Bar dataKey="completionRate" />)
          data.forEachIndexed { idx, point ->
            val centerX = (idx + 0.5f) * slotWidth
            val isSelected = idx == activeIndex

            if (showComparisonBars) {
              val groupWidth = (slotWidth * 0.64f).coerceAtMost(38.dp.toPx())
              val singleBarW = (groupWidth * 0.44f).coerceAtLeast(4.dp.toPx())
              val gap = groupWidth * 0.12f

              // Previous period bar (left)
              val prevFrac = animatedPrevFractions[idx].coerceIn(0.03f, 1f)
              val prevBarH = chartHeight * prevFrac
              val prevLeft = centerX - (groupWidth / 2f)
              drawRoundRect(
                color = comparisonBarColor.copy(alpha = if (isSelected) 0.65f else 0.4f),
                topLeft = Offset(prevLeft, chartHeight - prevBarH),
                size = Size(singleBarW, prevBarH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
              )

              // Current period bar (right)
              val currFrac = animatedFractions[idx].coerceIn(0.04f, 1f)
              val currBarH = chartHeight * currFrac
              val currLeft = prevLeft + singleBarW + gap
              val barBrush = Brush.verticalGradient(
                colors = listOf(
                  primaryBarColor,
                  primaryBarColor.copy(alpha = if (isSelected || point.isCurrentBucket) 0.92f else 0.68f)
                ),
                startY = chartHeight - currBarH,
                endY = chartHeight
              )
              drawRoundRect(
                brush = barBrush,
                topLeft = Offset(currLeft, chartHeight - currBarH),
                size = Size(singleBarW, currBarH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
              )
            } else {
              val barW = (slotWidth * 0.52f).coerceIn(8.dp.toPx(), 32.dp.toPx())
              val currFrac = animatedFractions[idx].coerceIn(0.04f, 1f)
              val currBarH = chartHeight * currFrac
              val barLeft = centerX - (barW / 2f)
              val barBrush = Brush.verticalGradient(
                colors = listOf(
                  primaryBarColor,
                  primaryBarColor.copy(alpha = if (isSelected || point.isCurrentBucket) 0.95f else 0.7f)
                ),
                startY = chartHeight - currBarH,
                endY = chartHeight
              )
              drawRoundRect(
                brush = barBrush,
                topLeft = Offset(barLeft, chartHeight - currBarH),
                size = Size(barW, currBarH),
                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
              )
            }
          }

          // 4. <ReferenceLine y={averageRate} />
          if (showAverageReferenceLine && averageRate in 1..99) {
            val avgY = chartHeight * (1f - (averageRate / 100f))
            drawLine(
              color = Color(0xFFFFD580),
              start = Offset(0f, avgY),
              end = Offset(chartWidth, avgY),
              strokeWidth = 1.6.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // <XAxis dataKey="xAxisLabel" />
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .testTag("recharts_x_axis")
        ) {
          data.forEachIndexed { idx, point ->
            val isSelected = idx == activeIndex
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .clickable { onSelectBarIndex(idx) }
                .testTag("recharts_bar_item_$idx")
            ) {
              Text(
                text = point.xAxisLabel,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 11.sp,
                fontWeight = if (isSelected || point.isCurrentBucket) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) primaryBarColor else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // <Legend />
    Row(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("recharts_legend")
    ) {
      LegendDotItem(color = primaryBarColor, label = "Completion Rate (%)")
      if (showComparisonBars) {
        LegendDotItem(color = comparisonBarColor, label = "Previous Period (%)")
      }
      if (showAverageReferenceLine) {
        LegendDotItem(color = Color(0xFFFFD580), label = "Avg ($averageRate%)")
      }
    }
  }
}

@Composable
private fun LegendDotItem(color: Color, label: String) {
  val colors = LocalHabitRadarColors.current
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(9.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      fontFamily = PlusJakartaSansFamily,
      fontSize = 11.sp,
      color = colors.textSecondary
    )
  }
}

/**
 * Full-screen Data Visualization Studio screen displaying Weekly and Monthly habit completion
 * rates using the Recharts-inspired BarChart engine.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DataVisualizationScreen(
  habits: List<Habit>,
  entriesByHabit: Map<String, Map<LocalDate, Entry>>,
  pausesByHabit: Map<String, List<HabitPause>>,
  settings: AppSettings,
  today: LocalDate,
  onBack: () -> Unit,
  onOpenHabitDetail: (String) -> Unit = {}
) {
  val colors = LocalHabitRadarColors.current

  var selectedTimeframe by remember { mutableStateOf(VisualizationTimeframe.WEEKLY) }
  var weeklyGranularity by remember { mutableStateOf(VisualizationGranularity.DAILY_IN_WEEK) }
  var monthlyGranularity by remember { mutableStateOf(VisualizationGranularity.SIX_MONTHS) }
  var selectedCategoryOrTag by remember { mutableStateOf<String?>(null) }
  var selectedHabitId by remember { mutableStateOf<String?>(null) }
  var periodOffset by remember { mutableIntStateOf(0) }
  var showComparisonBars by remember { mutableStateOf(true) }
  var showAverageLine by remember { mutableStateOf(true) }
  var selectedBarIndex by remember { mutableIntStateOf(-1) }

  val availableCategoriesAndTags = remember(habits) {
    val labelCounts = linkedMapOf<String, Int>()
    // Include preset categories first if any habit matches them, then all custom categories/tags
    val allLabels = habits
      .flatMap { it.allOrganizationLabels }
      .distinctBy { it.lowercase() }
    val sortedLabels = allLabels.sortedWith(
      compareBy<String> { label ->
        val presetIdx = HabitCategoryCatalog.PRESET_CATEGORIES.indexOfFirst { it.equals(label, ignoreCase = true) }
        if (presetIdx >= 0) presetIdx else 100
      }.thenBy { it.lowercase() }
    )
    sortedLabels.forEach { label ->
      val count = habits.count { it.matchesCategoryOrTag(label) }
      if (count > 0) {
        labelCounts[label] = count
      }
    }
    labelCounts
  }

  val categoryFilteredHabits = remember(habits, selectedCategoryOrTag) {
    if (selectedCategoryOrTag == null) {
      habits
    } else {
      habits.filter { it.matchesCategoryOrTag(selectedCategoryOrTag) }
    }
  }

  val filteredHabits = remember(categoryFilteredHabits, selectedHabitId) {
    if (selectedHabitId == null) {
      categoryFilteredHabits
    } else {
      categoryFilteredHabits.filter { it.id == selectedHabitId }
    }
  }

  val activeColor = remember(selectedHabitId, categoryFilteredHabits, colors.accent, colors.isDark) {
    val selectedHabit = categoryFilteredHabits.firstOrNull { it.id == selectedHabitId }
    if (selectedHabit != null) {
      habitTint(selectedHabit.colorKey, colors.isDark).baseColor
    } else {
      colors.accent
    }
  }

  val summary = remember(
    filteredHabits,
    entriesByHabit,
    pausesByHabit,
    selectedTimeframe,
    weeklyGranularity,
    monthlyGranularity,
    settings.weekStart,
    today,
    periodOffset
  ) {
    when (selectedTimeframe) {
      VisualizationTimeframe.WEEKLY -> {
        if (weeklyGranularity == VisualizationGranularity.DAILY_IN_WEEK) {
          CompletionRateChartCalculator.computeWeeklyDailySeries(
            habits = filteredHabits,
            entriesByHabit = entriesByHabit,
            pausesByHabit = pausesByHabit,
            weekStart = settings.weekStart,
            today = today,
            weekOffset = periodOffset
          )
        } else {
          CompletionRateChartCalculator.computeWeeklyMultiWeekSeries(
            habits = filteredHabits,
            entriesByHabit = entriesByHabit,
            pausesByHabit = pausesByHabit,
            weekStart = settings.weekStart,
            today = today,
            weeksCount = 8,
            weekOffset = periodOffset
          )
        }
      }
      VisualizationTimeframe.MONTHLY -> {
        val count = if (monthlyGranularity == VisualizationGranularity.TWELVE_MONTHS) 12 else 6
        CompletionRateChartCalculator.computeMonthlySeries(
          habits = filteredHabits,
          entriesByHabit = entriesByHabit,
          pausesByHabit = pausesByHabit,
          today = today,
          monthsCount = count,
          monthOffset = periodOffset
        )
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
      .testTag("data_visualization_screen")
  ) {
    SubPageTopBar(
      title = "Data Visualization",
      onBack = onBack
    )

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. Primary Timeframe Switcher (Weekly | Monthly)
      item(key = "viz_timeframe_selector") {
        HabitRadarCard {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            SectionLabel("COMPLETION RATE VIEW")
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface2)
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "RECHARTS BAR ENGINE",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = activeColor
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Weekly vs Monthly pill switcher
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            VisualizationTimeframe.entries.forEach { tf ->
              val isSelected = selectedTimeframe == tf
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .clip(RoundedCornerShape(22.dp))
                  .background(if (isSelected) activeColor else colors.surface2)
                  .clickable {
                    selectedTimeframe = tf
                    periodOffset = 0
                    selectedBarIndex = -1
                  }
                  .testTag("viz_timeframe_${tf.name.lowercase()}")
              ) {
                Text(
                  text = "${tf.label} Rates",
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 14.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color(0xFF101114) else colors.textPrimary
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Sub-granularity & Period Offset Navigation
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            if (selectedTimeframe == VisualizationTimeframe.WEEKLY) {
              HabitRadarSegmentedControl(
                options = listOf(
                  VisualizationGranularity.DAILY_IN_WEEK,
                  VisualizationGranularity.MULTI_WEEK
                ),
                selected = weeklyGranularity,
                labelMapper = { it.label },
                onSelect = {
                  weeklyGranularity = it
                  selectedBarIndex = -1
                },
                height = 36.dp,
                modifier = Modifier.width(180.dp)
              )
            } else {
              HabitRadarSegmentedControl(
                options = listOf(
                  VisualizationGranularity.SIX_MONTHS,
                  VisualizationGranularity.TWELVE_MONTHS
                ),
                selected = monthlyGranularity,
                labelMapper = { it.label },
                onSelect = {
                  monthlyGranularity = it
                  selectedBarIndex = -1
                },
                height = 36.dp,
                modifier = Modifier.width(180.dp)
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  periodOffset--
                  selectedBarIndex = -1
                },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("viz_prev_period_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                  contentDescription = "Previous period",
                  tint = colors.textPrimary
                )
              }
              IconButton(
                onClick = {
                  if (periodOffset < 0) {
                    periodOffset++
                    selectedBarIndex = -1
                  }
                },
                enabled = periodOffset < 0,
                modifier = Modifier
                  .size(36.dp)
                  .testTag("viz_next_period_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = "Next period",
                  tint = if (periodOffset < 0) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f)
                )
              }
            }
          }
        }
      }

      // 2A. Category & Tag Filter Chips (e.g., Fitness, Mindfulness, Learning, or custom tags)
      if (availableCategoriesAndTags.isNotEmpty()) {
        item(key = "viz_category_tag_filters") {
          Column(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("FILTER BY CATEGORY OR TAG")
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("viz_category_tag_filter_row")
            ) {
              item(key = "cat_all") {
                val isAll = selectedCategoryOrTag == null
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isAll) colors.accent else colors.card)
                    .clickable {
                      selectedCategoryOrTag = null
                      selectedHabitId = null
                      selectedBarIndex = -1
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("viz_category_chip_all")
                ) {
                  Text(
                    text = "All Areas (${habits.size})",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 13.sp,
                    fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAll) colors.onAccent else colors.textPrimary
                  )
                }
              }

              items(availableCategoriesAndTags.entries.toList(), key = { "cat_${it.key}" }) { (label, count) ->
                val isSel = selectedCategoryOrTag.equals(label, ignoreCase = true)
                val isPresetCategory = HabitCategoryCatalog.PRESET_CATEGORIES.any {
                  it.equals(label, ignoreCase = true)
                }
                val displayPrefix = if (isPresetCategory) "" else "#"
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSel) colors.accent else colors.card)
                    .border(
                      width = 1.dp,
                      color = if (isSel) colors.accent else colors.surface2,
                      shape = RoundedCornerShape(18.dp)
                    )
                    .clickable {
                      selectedCategoryOrTag = if (isSel) null else label
                      selectedHabitId = null
                      selectedBarIndex = -1
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("viz_category_chip_${label.lowercase().replace(' ', '_')}")
                ) {
                  Text(
                    text = "$displayPrefix$label ($count)",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 13.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSel) colors.onAccent else colors.textPrimary
                  )
                }
              }
            }
          }
        }
      }

      // 2B. Habit Filter Chips (All Habits in selected category + Individual Habits)
      if (categoryFilteredHabits.isNotEmpty()) {
        item(key = "viz_habit_filters") {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("viz_habit_filter_row")
          ) {
            item {
              val isAll = selectedHabitId == null
              val allLabel = if (selectedCategoryOrTag != null) {
                "All $selectedCategoryOrTag (${categoryFilteredHabits.size})"
              } else {
                "All Habits (${categoryFilteredHabits.size})"
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(18.dp))
                  .background(if (isAll) colors.accent else colors.card)
                  .clickable {
                    selectedHabitId = null
                    selectedBarIndex = -1
                  }
                  .padding(horizontal = 14.dp, vertical = 8.dp)
                  .testTag("viz_habit_chip_all")
              ) {
                Text(
                  text = allLabel,
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 13.sp,
                  fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                  color = if (isAll) colors.onAccent else colors.textPrimary
                )
              }
            }

            items(categoryFilteredHabits, key = { it.id }) { habit ->
              val isSelected = selectedHabitId == habit.id
              val hTint = habitTint(habit.colorKey, colors.isDark)
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(18.dp))
                  .background(if (isSelected) hTint.baseColor else colors.card)
                  .clickable {
                    selectedHabitId = if (isSelected) null else habit.id
                    selectedBarIndex = -1
                  }
                  .padding(horizontal = 14.dp, vertical = 8.dp)
                  .testTag("viz_habit_chip_${habit.id}")
              ) {
                Text(
                  text = habit.name,
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color(0xFF101114) else colors.textPrimary
                )
              }
            }
          }
        }
      }

      // 3. Main Recharts Bar Chart Card
      item(key = "viz_main_bar_chart") {
        HabitRadarCard {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = summary.titleLabel,
                fontFamily = InstrumentSerifFamily,
                fontSize = 26.sp,
                color = colors.textPrimary,
                modifier = Modifier.testTag("viz_chart_title")
              )
              Text(
                text = summary.subtitleRange,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.sp,
                color = colors.textSecondary
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "${summary.averageRate}%",
                fontFamily = InstrumentSerifFamily,
                fontSize = 30.sp,
                color = activeColor,
                modifier = Modifier.testTag("viz_average_rate_value")
              )
              val deltaSign = if (summary.periodDelta >= 0) "▲ +${summary.periodDelta}%" else "▼ ${summary.periodDelta}%"
              Text(
                text = deltaSign,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (summary.periodDelta >= 0) Color(0xFFA8DFB4) else Color(0xFFEE9A9A)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          RechartsBarChart(
            data = summary.dataPoints,
            primaryBarColor = activeColor,
            showComparisonBars = showComparisonBars,
            showAverageReferenceLine = showAverageLine,
            averageRate = summary.averageRate,
            selectedBarIndex = selectedBarIndex,
            onSelectBarIndex = { selectedBarIndex = it }
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Chart Options toggles: Compare vs Previous & Reference Avg Line
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (showComparisonBars) activeColor.copy(alpha = 0.2f) else colors.surface2)
                .clickable { showComparisonBars = !showComparisonBars }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("viz_toggle_comparison_bars")
            ) {
              Text(
                text = if (showComparisonBars) "✓ Compare vs Previous" else "Compare vs Previous",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (showComparisonBars) activeColor else colors.textSecondary
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (showAverageLine) Color(0x33FFD580) else colors.surface2)
                .clickable { showAverageLine = !showAverageLine }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("viz_toggle_reference_line")
            ) {
              Text(
                text = if (showAverageLine) "✓ Reference Avg Line" else "Reference Avg Line",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (showAverageLine) Color(0xFFFFD580) else colors.textSecondary
              )
            }
          }
        }
      }

      // 4. KPI Summary Row (Peak Bucket, Lowest Bucket, Total Check-ins)
      item(key = "viz_kpi_tiles") {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          VisualizationStatTile(
            label = "PEAK ${selectedTimeframe.label.uppercase()}",
            value = summary.peakBucket?.let { "${it.xAxisLabel} (${it.completionRate}%)" } ?: "—",
            accentColor = Color(0xFFA8DFB4),
            modifier = Modifier.weight(1f)
          )
          VisualizationStatTile(
            label = "LOWEST ${selectedTimeframe.label.uppercase()}",
            value = summary.lowestBucket?.let { "${it.xAxisLabel} (${it.completionRate}%)" } ?: "—",
            accentColor = Color(0xFFEE9A9A),
            modifier = Modifier.weight(1f)
          )
          VisualizationStatTile(
            label = "CHECK-INS",
            value = "${summary.totalCompleted}/${summary.totalScheduled}",
            accentColor = activeColor,
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 4.5. Category / Area Performance Breakdown Card
      if (availableCategoriesAndTags.isNotEmpty()) {
        item(key = "viz_category_performance_breakdown") {
          val weekStart = LogicalDateCalculator.startOfWeek(today, settings.weekStart)
          val monthStart = YearMonth.from(today).atDay(1)
          HabitRadarCard(modifier = Modifier.testTag("viz_category_breakdown_card")) {
            SectionLabel("PERFORMANCE BY CATEGORY & TAG")
            Text(
              text = "Tap any area to filter the completion rate chart above",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 12.sp,
              color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              availableCategoriesAndTags.entries.forEach { (label, count) ->
                val areaHabits = habits.filter { it.matchesCategoryOrTag(label) }
                val weeklyAreaRate = if (areaHabits.isNotEmpty()) {
                  areaHabits.map { h ->
                    AnalyticsCalculator.computePeriodRate(
                      h,
                      entriesByHabit[h.id].orEmpty(),
                      pausesByHabit[h.id].orEmpty(),
                      weekStart,
                      today
                    )
                  }.average().roundToInt()
                } else 0
                val monthlyAreaRate = if (areaHabits.isNotEmpty()) {
                  areaHabits.map { h ->
                    AnalyticsCalculator.computePeriodRate(
                      h,
                      entriesByHabit[h.id].orEmpty(),
                      pausesByHabit[h.id].orEmpty(),
                      monthStart,
                      today
                    )
                  }.average().roundToInt()
                } else 0
                val isSelectedArea = selectedCategoryOrTag.equals(label, ignoreCase = true)

                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelectedArea) colors.accentContainer else colors.surface2.copy(alpha = 0.65f))
                    .clickable {
                      selectedCategoryOrTag = if (isSelectedArea) null else label
                      selectedHabitId = null
                      selectedBarIndex = -1
                    }
                    .padding(12.dp)
                    .testTag("viz_category_breakdown_row_${label.lowercase().replace(' ', '_')}")
                ) {
                  Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "$label · $count ${if (count == 1) "habit" else "habits"}",
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelectedArea) colors.accent else colors.textPrimary
                    )
                    Text(
                      text = "Week $weeklyAreaRate% · Month $monthlyAreaRate%",
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = colors.accent
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  RateComparisonMiniBar(
                    label = "Weekly",
                    rate = weeklyAreaRate,
                    barColor = colors.accent
                  )
                }
              }
            }
          }
        }
      }

      // 5. Per-Habit Weekly vs Monthly Completion Rate Comparison
      if (categoryFilteredHabits.isNotEmpty()) {
        item(key = "viz_per_habit_header") {
          SectionLabel(
            if (selectedCategoryOrTag != null) {
              "HABIT BREAKDOWN · ${selectedCategoryOrTag!!.uppercase()}"
            } else {
              "HABIT COMPLETION RATE BREAKDOWN"
            }
          )
        }

        items(categoryFilteredHabits, key = { "breakdown_${it.id}" }) { habit ->
          val hTint = habitTint(habit.colorKey, colors.isDark)
          val weekStart = LogicalDateCalculator.startOfWeek(today, settings.weekStart)
          val monthStart = YearMonth.from(today).atDay(1)

          val weeklyRate = remember(habit, entriesByHabit, pausesByHabit, weekStart, today) {
            AnalyticsCalculator.computePeriodRate(
              habit,
              entriesByHabit[habit.id].orEmpty(),
              pausesByHabit[habit.id].orEmpty(),
              weekStart,
              today
            )
          }
          val monthlyRate = remember(habit, entriesByHabit, pausesByHabit, monthStart, today) {
            AnalyticsCalculator.computePeriodRate(
              habit,
              entriesByHabit[habit.id].orEmpty(),
              pausesByHabit[habit.id].orEmpty(),
              monthStart,
              today
            )
          }

          HabitRadarCard(
            backgroundColor = hTint.cardBackground,
            onClick = { onOpenHabitDetail(habit.id) }
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(hTint.iconTileBackground)
              ) {
                HabitIconView(iconKey = habit.iconKey, tint = hTint.baseColor)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = habit.name,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f, fill = false)
                  )
                  val badgesToShow = habit.allOrganizationLabels.take(2)
                  if (badgesToShow.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      badgesToShow.forEach { lbl ->
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface2)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                          Text(
                            text = lbl,
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hTint.baseColor
                          )
                        }
                      }
                    }
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Weekly bar
                RateComparisonMiniBar(
                  label = "Weekly",
                  rate = weeklyRate,
                  barColor = hTint.baseColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Monthly bar
                RateComparisonMiniBar(
                  label = "Monthly",
                  rate = monthlyRate,
                  barColor = hTint.baseColor.copy(alpha = 0.65f)
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
private fun RateComparisonMiniBar(
  label: String,
  rate: Int,
  barColor: Color
) {
  val colors = LocalHabitRadarColors.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      color = colors.textSecondary,
      modifier = Modifier.width(52.dp)
    )
    Box(
      modifier = Modifier
        .weight(1f)
        .height(7.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(colors.surface2)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth((rate / 100f).coerceIn(0.04f, 1f))
          .fillMaxHeight()
          .clip(RoundedCornerShape(4.dp))
          .background(barColor)
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "$rate%",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = colors.textPrimary,
      modifier = Modifier.width(34.dp),
      textAlign = TextAlign.End
    )
  }
}

@Composable
private fun VisualizationStatTile(
  label: String,
  value: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  HabitRadarCard(
    contentPadding = PaddingValues(12.dp),
    modifier = modifier
  ) {
    Text(
      text = label,
      fontSize = 9.sp,
      letterSpacing = 1.2.sp,
      fontWeight = FontWeight.SemiBold,
      color = colors.textSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = value,
      fontFamily = InstrumentSerifFamily,
      fontSize = 20.sp,
      color = accentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
