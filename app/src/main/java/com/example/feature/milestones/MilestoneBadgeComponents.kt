package com.example.feature.milestones

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.domain.model.BadgeEmblemStyle
import com.example.core.domain.model.BadgeTier
import com.example.core.domain.model.Entry
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.MilestoneBadge
import com.example.core.domain.model.MilestoneCategory
import com.example.core.domain.model.MilestoneSummary
import com.example.core.domain.usecase.MilestoneEngine
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class TierPalette(
  val primary: Color,
  val secondary: Color,
  val glow: Color,
  val label: String
)

fun badgeTierPalette(tier: BadgeTier): TierPalette = when (tier) {
  BadgeTier.BRONZE -> TierPalette(
    primary = Color(0xFFE6A15C),
    secondary = Color(0xFFB87333),
    glow = Color(0x33E6A15C),
    label = "BRONZE TIER"
  )
  BadgeTier.SILVER -> TierPalette(
    primary = Color(0xFFB8C7E0),
    secondary = Color(0xFF8DA2C4),
    glow = Color(0x33B8C7E0),
    label = "SILVER TIER"
  )
  BadgeTier.GOLD -> TierPalette(
    primary = Color(0xFFF5C96B),
    secondary = Color(0xFFD99B26),
    glow = Color(0x38F5C96B),
    label = "GOLD TIER"
  )
  BadgeTier.PRISMATIC -> TierPalette(
    primary = Color(0xFFE8A8F5),
    secondary = Color(0xFF90CAF9),
    glow = Color(0x3DE8A8F5),
    label = "PRISMATIC TIER"
  )
}

/**
 * Custom Canvas geometric emblem for a [MilestoneBadge].
 */
@Composable
fun MilestoneBadgeEmblem(
  badge: MilestoneBadge,
  size: Dp = 56.dp,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  val palette = badgeTierPalette(badge.tier)
  val isUnlocked = badge.isUnlocked
  val activeColor = if (isUnlocked) palette.primary else colors.textSecondary.copy(alpha = 0.45f)
  val secondaryColor = if (isUnlocked) palette.secondary else colors.surface2

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(size)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val radius = this.size.minDimension / 2f
      val center = Offset(this.size.width / 2f, this.size.height / 2f)
      val strokePx = (this.size.minDimension * 0.08f).coerceAtLeast(3f)

      // Radial aura for unlocked badges
      if (isUnlocked) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(palette.glow, Color.Transparent),
            center = center,
            radius = radius
          ),
          radius = radius,
          center = center
        )
      }

      // Inner medallion disc
      drawCircle(
        brush = Brush.linearGradient(
          colors = if (isUnlocked) {
            listOf(activeColor.copy(alpha = 0.28f), secondaryColor.copy(alpha = 0.14f))
          } else {
            listOf(colors.surface2.copy(alpha = 0.7f), colors.surface2.copy(alpha = 0.35f))
          }
        ),
        radius = radius * 0.82f,
        center = center
      )

      // Progress ring (full ring when unlocked, partial arc when locked)
      val arcInset = strokePx
      val arcSize = Size((radius * 1.68f), (radius * 1.68f))
      val arcTopLeft = Offset(center.x - arcSize.width / 2f, center.y - arcSize.height / 2f)

      drawArc(
        color = colors.surface2,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokePx * 0.85f, cap = StrokeCap.Round)
      )

      val sweep = if (isUnlocked) 360f else (360f * badge.progressFraction.coerceIn(0f, 1f))
      if (sweep > 0f) {
        drawArc(
          color = if (isUnlocked) activeColor else palette.primary.copy(alpha = 0.65f),
          startAngle = -90f,
          sweepAngle = sweep,
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = strokePx * 0.85f, cap = StrokeCap.Round)
        )
      }

      // Center geometric crest based on emblemStyle
      val crestRadius = radius * 0.38f
      when (badge.emblemStyle) {
        BadgeEmblemStyle.STAR -> {
          val path = Path()
          val points = 5
          val innerRadius = crestRadius * 0.45f
          for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) crestRadius else innerRadius
            val angle = Math.toRadians((i * 36.0) - 90.0)
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          path.close()
          drawPath(path = path, color = activeColor)
        }
        BadgeEmblemStyle.DIAMOND -> {
          val path = Path().apply {
            moveTo(center.x, center.y - crestRadius)
            lineTo(center.x + crestRadius * 0.82f, center.y)
            lineTo(center.x, center.y + crestRadius)
            lineTo(center.x - crestRadius * 0.82f, center.y)
            close()
          }
          drawPath(path = path, color = activeColor)
        }
        BadgeEmblemStyle.SHIELD -> {
          val path = Path().apply {
            moveTo(center.x, center.y - crestRadius)
            lineTo(center.x + crestRadius * 0.85f, center.y - crestRadius * 0.45f)
            lineTo(center.x + crestRadius * 0.68f, center.y + crestRadius * 0.35f)
            lineTo(center.x, center.y + crestRadius)
            lineTo(center.x - crestRadius * 0.68f, center.y + crestRadius * 0.35f)
            lineTo(center.x - crestRadius * 0.85f, center.y - crestRadius * 0.45f)
            close()
          }
          drawPath(
            path = path,
            color = activeColor,
            style = Stroke(width = strokePx * 0.8f, join = StrokeJoin.Round)
          )
          drawCircle(color = activeColor, radius = crestRadius * 0.28f, center = center)
        }
        BadgeEmblemStyle.CROWN -> {
          val path = Path().apply {
            moveTo(center.x - crestRadius * 0.85f, center.y + crestRadius * 0.55f)
            lineTo(center.x - crestRadius * 0.90f, center.y - crestRadius * 0.35f)
            lineTo(center.x - crestRadius * 0.38f, center.y + crestRadius * 0.08f)
            lineTo(center.x, center.y - crestRadius * 0.75f)
            lineTo(center.x + crestRadius * 0.38f, center.y + crestRadius * 0.08f)
            lineTo(center.x + crestRadius * 0.90f, center.y - crestRadius * 0.35f)
            lineTo(center.x + crestRadius * 0.85f, center.y + crestRadius * 0.55f)
            close()
          }
          drawPath(path = path, color = activeColor)
        }
        BadgeEmblemStyle.FLAME -> {
          val path = Path().apply {
            moveTo(center.x, center.y - crestRadius)
            cubicTo(
              center.x + crestRadius * 0.95f, center.y - crestRadius * 0.25f,
              center.x + crestRadius * 0.75f, center.y + crestRadius * 0.85f,
              center.x, center.y + crestRadius
            )
            cubicTo(
              center.x - crestRadius * 0.75f, center.y + crestRadius * 0.85f,
              center.x - crestRadius * 0.95f, center.y - crestRadius * 0.25f,
              center.x, center.y - crestRadius
            )
            close()
          }
          drawPath(path = path, color = activeColor)
        }
        BadgeEmblemStyle.LAUREL -> {
          drawCircle(
            color = activeColor,
            radius = crestRadius * 0.78f,
            center = center,
            style = Stroke(width = strokePx * 0.8f)
          )
          drawCircle(
            color = activeColor,
            radius = crestRadius * 0.34f,
            center = center
          )
        }
      }
    }
  }
}

/**
 * Compact milestone badge shelf displayed on the Today screen.
 */
@Composable
fun TodayMilestoneShelfCard(
  milestoneSummary: MilestoneSummary,
  onOpenBadgesGallery: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  val displayBadges = remember(milestoneSummary.allBadges) {
    val unlocked = milestoneSummary.unlockedBadges.sortedByDescending { it.tier.ordinal * 1000 + it.targetValue }
    val locked = milestoneSummary.lockedBadges.sortedByDescending { it.progressFraction }
    (unlocked + locked).take(4)
  }
  val nextUp = milestoneSummary.nextUpBadge

  HabitRadarCard(
    backgroundColor = colors.card,
    onClick = onOpenBadgesGallery,
    modifier = modifier.testTag("today_milestone_shelf_card")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "MILESTONE BADGES",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.8.sp,
          color = colors.accent
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${milestoneSummary.unlockedCount} of ${milestoneSummary.totalCount} unlocked",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = colors.textPrimary,
          modifier = Modifier.testTag("today_milestone_unlocked_count")
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(14.dp))
          .background(colors.surface2)
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Text(
          text = "Trophy Case →",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = colors.accent
        )
      }
    }

    if (displayBadges.isNotEmpty()) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        displayBadges.forEach { badge ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 4.dp)
          ) {
            MilestoneBadgeEmblem(badge = badge, size = 46.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = badge.title,
              fontFamily = PlusJakartaSansFamily,
              fontSize = 11.sp,
              fontWeight = if (badge.isUnlocked) FontWeight.SemiBold else FontWeight.Normal,
              color = if (badge.isUnlocked) colors.textPrimary else colors.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center
            )
            Text(
              text = if (badge.isUnlocked) "Earned" else "${badge.currentValue}/${badge.targetValue}",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 10.sp,
              color = if (badge.isUnlocked) colors.accent else colors.textSecondary
            )
          }
        }
      }
    }

    if (nextUp != null) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Next badge: ${nextUp.title} (${nextUp.subtitle})",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 12.sp,
          color = colors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${nextUp.currentValue}/${nextUp.targetValue}",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = colors.accent
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(colors.surface2)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(nextUp.progressFraction.coerceIn(0f, 1f))
            .fillMaxHeight()
            .clip(RoundedCornerShape(3.dp))
            .background(colors.accent)
        )
      }
    }
  }
}

/**
 * Per-habit milestone badge shelf displayed on [com.example.feature.habitdetail.HabitDetailScreen].
 */
@Composable
fun HabitMilestoneBadgeShelf(
  habit: Habit,
  entries: List<Entry>,
  pauses: List<HabitPause>,
  today: LocalDate,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  val habitBadges = remember(habit, entries, pauses, today) {
    MilestoneEngine.evaluateHabitMilestones(habit, entries, pauses, today)
  }
  val unlockedCount = remember(habitBadges) { habitBadges.count { it.isUnlocked } }
  val nextHabitBadge = remember(habitBadges) {
    habitBadges.filter { !it.isUnlocked }.maxByOrNull { it.progressFraction }
  }
  var selectedBadge by remember { mutableStateOf<MilestoneBadge?>(null) }

  HabitRadarCard(modifier = modifier.testTag("habit_milestone_badge_shelf")) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        SectionLabel("HABIT MILESTONE BADGES")
        Text(
          text = "$unlockedCount of ${habitBadges.size} badges earned for ${habit.name}",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          color = colors.textSecondary
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
    ) {
      habitBadges.forEach { badge ->
        val palette = badgeTierPalette(badge.tier)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (badge.isUnlocked) colors.surface2 else colors.surface2.copy(alpha = 0.45f))
            .border(
              width = 1.dp,
              color = if (badge.isUnlocked) palette.primary.copy(alpha = 0.55f) else Color.Transparent,
              shape = RoundedCornerShape(18.dp)
            )
            .clickable { selectedBadge = badge }
            .padding(vertical = 10.dp, horizontal = 8.dp)
            .testTag("habit_badge_chip_${badge.id}")
        ) {
          MilestoneBadgeEmblem(badge = badge, size = 48.dp)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = badge.title,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (badge.isUnlocked) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
          )
          Text(
            text = if (badge.isUnlocked) "Unlocked" else "${badge.currentValue}/${badge.targetValue}",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            color = if (badge.isUnlocked) palette.primary else colors.textSecondary
          )
        }
      }
    }

    if (nextHabitBadge != null) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Next up: ${nextHabitBadge.title} (${nextHabitBadge.remainingCount} ${nextHabitBadge.unitLabel} left)",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 12.sp,
          color = colors.textSecondary
        )
        Text(
          text = "${nextHabitBadge.progressPercentage}%",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = colors.accent
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(colors.surface2)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(nextHabitBadge.progressFraction.coerceIn(0f, 1f))
            .fillMaxHeight()
            .clip(RoundedCornerShape(3.dp))
            .background(colors.accent)
        )
      }
    }
  }

  if (selectedBadge != null) {
    BadgeDetailBottomSheet(
      badge = selectedBadge!!,
      onDismiss = { selectedBadge = null }
    )
  }
}

/**
 * Dedicated Badges & Achievements Gallery section used in MilestonesListScreen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgesGallerySection(
  milestoneSummary: MilestoneSummary,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  var selectedCategory by remember { mutableStateOf<MilestoneCategory?>(null) }
  var inspectedBadge by remember { mutableStateOf<MilestoneBadge?>(null) }

  val filteredBadges = remember(milestoneSummary.allBadges, selectedCategory) {
    if (selectedCategory == null) {
      milestoneSummary.allBadges
    } else {
      milestoneSummary.allBadges.filter { it.category == selectedCategory }
    }
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("badges_gallery_section")
  ) {
    // Summary Hero Card
    HabitRadarCard {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "ACHIEVEMENT TROPHY CASE",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = colors.accent
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${milestoneSummary.unlockedCount} of ${milestoneSummary.totalCount} Badges",
            fontFamily = InstrumentSerifFamily,
            fontSize = 32.sp,
            color = colors.textPrimary,
            modifier = Modifier.testTag("badges_gallery_unlocked_headline")
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Best streak: ${milestoneSummary.bestStreakOverall}d · Total completions: ${milestoneSummary.totalCompletionsOverall}",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.sp,
            color = colors.textSecondary
          )
        }
      }
    }

    // Category filter pills
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
    ) {
      val categories: List<MilestoneCategory?> = listOf(null) + MilestoneCategory.entries
      categories.forEach { cat ->
        val isSel = selectedCategory == cat
        val label = cat?.label ?: "All (${milestoneSummary.totalCount})"
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSel) colors.accent else colors.surface2)
            .clickable { selectedCategory = cat }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("badge_category_filter_${cat?.name ?: "ALL"}")
        ) {
          Text(
            text = label,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSel) colors.onAccent else colors.textPrimary
          )
        }
      }
    }

    // 2-column grid of badges
    val chunkedBadges = remember(filteredBadges) { filteredBadges.chunked(2) }
    chunkedBadges.forEach { rowBadges ->
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        rowBadges.forEach { badge ->
          MilestoneBadgeGridCard(
            badge = badge,
            onClick = { inspectedBadge = badge },
            modifier = Modifier.weight(1f)
          )
        }
        if (rowBadges.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }

  if (inspectedBadge != null) {
    BadgeDetailBottomSheet(
      badge = inspectedBadge!!,
      onDismiss = { inspectedBadge = null }
    )
  }
}

@Composable
private fun MilestoneBadgeGridCard(
  badge: MilestoneBadge,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  val palette = badgeTierPalette(badge.tier)
  val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH) }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(24.dp))
      .background(if (badge.isUnlocked) colors.card else colors.card.copy(alpha = 0.65f))
      .border(
        width = if (badge.isUnlocked) 1.5.dp else 1.dp,
        color = if (badge.isUnlocked) palette.primary.copy(alpha = 0.55f) else colors.surface2,
        shape = RoundedCornerShape(24.dp)
      )
      .clickable(onClick = onClick)
      .padding(14.dp)
      .testTag("milestone_badge_card_${badge.id}")
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth()
    ) {
      MilestoneBadgeEmblem(badge = badge, size = 60.dp)
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = palette.label,
        fontFamily = PlusJakartaSansFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = if (badge.isUnlocked) palette.primary else colors.textSecondary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = badge.title,
        fontFamily = PlusJakartaSansFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = badge.subtitle,
        fontFamily = PlusJakartaSansFamily,
        fontSize = 12.sp,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(10.dp))

      if (badge.isUnlocked) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.glow)
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = badge.unlockedDate?.let { "Earned ${it.format(dateFormatter)}" } ?: "Unlocked",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.primary
          )
        }
      } else {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "${badge.currentValue}/${badge.targetValue}",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 11.sp,
              color = colors.textSecondary
            )
            Text(
              text = "${badge.progressPercentage}%",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = colors.accent
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(colors.surface2)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(badge.progressFraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(colors.accent)
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeDetailBottomSheet(
  badge: MilestoneBadge,
  onDismiss: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val palette = badgeTierPalette(badge.tier)
  val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = colors.bg
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      MilestoneBadgeEmblem(badge = badge, size = 96.dp)
      Spacer(modifier = Modifier.height(14.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(14.dp))
          .background(palette.glow)
          .padding(horizontal = 12.dp, vertical = 5.dp)
      ) {
        Text(
          text = "${palette.label} · ${badge.category.label.uppercase()}",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = palette.primary
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = badge.title,
        fontFamily = InstrumentSerifFamily,
        fontSize = 34.sp,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = badge.description,
        fontFamily = PlusJakartaSansFamily,
        fontSize = 14.sp,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(18.dp))

      HabitRadarCard {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = if (badge.isUnlocked) "Status: Unlocked" else "Progress: ${badge.currentValue} / ${badge.targetValue} ${badge.unitLabel}",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
          )
          Text(
            text = "${badge.progressPercentage}%",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primary
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.surface2)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(badge.progressFraction.coerceIn(0f, 1f))
              .fillMaxHeight()
              .clip(RoundedCornerShape(4.dp))
              .background(palette.primary)
          )
        }
        if (badge.isUnlocked && badge.unlockedDate != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Earned on ${badge.unlockedDate.format(dateFormatter)}",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            color = colors.textSecondary
          )
        } else if (!badge.isUnlocked) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "${badge.remainingCount} more ${badge.unitLabel} to unlock this badge.",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            color = colors.textSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .clip(RoundedCornerShape(25.dp))
          .background(colors.accent)
          .clickable(onClick = onDismiss)
      ) {
        Text(
          text = "Done",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = colors.onAccent
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

/**
 * Instant celebration modal displayed when a user unlocks a new milestone badge.
 */
@Composable
fun BadgeUnlockCelebrationModal(
  badge: MilestoneBadge,
  onDismiss: () -> Unit,
  onViewAllBadges: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val palette = badgeTierPalette(badge.tier)
  var animateIn by remember { mutableStateOf(false) }
  LaunchedEffect(badge.id) {
    animateIn = true
  }
  val scale by animateFloatAsState(
    targetValue = if (animateIn) 1f else 0.72f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "badge_unlock_scale"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.72f))
        .clickable(onClick = onDismiss)
        .padding(24.dp)
        .testTag("badge_unlock_celebration_modal")
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .scale(scale)
          .clip(RoundedCornerShape(32.dp))
          .background(colors.card)
          .border(1.5.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(32.dp))
          .clickable(enabled = false) {}
          .padding(horizontal = 24.dp, vertical = 28.dp)
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.glow)
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Text(
            text = "NEW MILESTONE UNLOCKED · ${palette.label}",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = palette.primary
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        MilestoneBadgeEmblem(
          badge = badge.copy(isUnlocked = true),
          size = 112.dp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = badge.title,
          fontFamily = InstrumentSerifFamily,
          fontSize = 36.sp,
          color = colors.textPrimary,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("celebration_badge_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = badge.description,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 14.sp,
          color = colors.textSecondary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(colors.surface2)
              .clickable(onClick = onViewAllBadges)
              .testTag("celebration_view_badges_button")
          ) {
            Text(
              text = "Trophy Case",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = colors.textPrimary
            )
          }

          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(palette.primary)
              .clickable(onClick = onDismiss)
              .testTag("celebration_keep_going_button")
          ) {
            Text(
              text = "Keep Going",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF141418)
            )
          }
        }
      }
    }
  }
}
