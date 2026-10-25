package com.example.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.model.HomeTab
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.example.core.ui.theme.formatSerifHeadline
import com.example.core.ui.theme.parseHexColor
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun SerifHeadlineText(
  rawText: String,
  modifier: Modifier = Modifier,
  fontSizeSp: Int = 34,
  color: Color = LocalHabitRadarColors.current.textPrimary,
  textAlign: TextAlign = TextAlign.Start
) {
  Text(
    text = remember(rawText) { formatSerifHeadline(rawText) },
    fontFamily = InstrumentSerifFamily,
    fontSize = fontSizeSp.sp,
    lineHeight = (fontSizeSp + 6).sp,
    color = color,
    textAlign = textAlign,
    modifier = modifier
  )
}

@Composable
fun SectionLabel(
  text: String,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = colors.textSecondary,
    modifier = modifier.padding(bottom = 8.dp)
  )
}

@Composable
fun SubPageTopBar(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable RowScope.() -> Unit = {}
) {
  val colors = LocalHabitRadarColors.current
  Surface(
    color = colors.appBar,
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .windowInsetsPadding(WindowInsets.statusBars)
        .fillMaxWidth()
        .height(64.dp)
        .padding(horizontal = 8.dp)
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(48.dp)
          .testTag("back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = colors.textPrimary
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      actions()
    }
  }
}

@Composable
fun MainTabHeader(
  dateLabel: String,
  headlineText: String?,
  onOpenMenu: () -> Unit,
  onNewHabit: () -> Unit,
  modifier: Modifier = Modifier,
  extraBottomContent: @Composable ColumnScope.() -> Unit = {}
) {
  val colors = LocalHabitRadarColors.current
  Surface(
    color = colors.appBar,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .windowInsetsPadding(WindowInsets.statusBars)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = dateLabel,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            letterSpacing = 2.4.sp,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium
          )
          if (headlineText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            SerifHeadlineText(rawText = headlineText, fontSizeSp = 34)
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Round outlined ≡ button (→ Menu)
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.5.dp, colors.surface2, CircleShape)
            .clickable(onClick = onOpenMenu)
            .testTag("menu_button")
        ) {
          Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Open Menu",
            tint = colors.textPrimary,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        // Accent + FAB
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.accent)
            .clickable(onClick = onNewHabit)
            .testTag("new_habit_fab")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "New Habit",
            tint = colors.onAccent,
            modifier = Modifier.size(24.dp)
          )
        }
      }
      extraBottomContent()
    }
  }
}

@Composable
fun FloatingPillTabBar(
  tabs: List<HomeTab>,
  selectedTab: HomeTab,
  onSelectTab: (HomeTab) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Surface(
      color = colors.card.copy(alpha = 0.95f),
      shape = RoundedCornerShape(32.dp),
      tonalElevation = 8.dp,
      shadowElevation = 12.dp,
      modifier = Modifier
        .fillMaxWidth()
        .height(58.dp)
        .border(1.dp, colors.surface2, RoundedCornerShape(32.dp))
        .testTag("floating_tab_bar")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 6.dp, vertical = 6.dp)
      ) {
        tabs.forEach { tab ->
          val isSelected = tab == selectedTab
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(26.dp))
              .background(if (isSelected) colors.accentContainer else Color.Transparent)
              .clickable { onSelectTab(tab) }
              .testTag("tab_${tab.id}")
          ) {
            Text(
              text = tab.label,
              fontFamily = PlusJakartaSansFamily,
              fontSize = 15.sp,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
              color = if (isSelected) colors.accent else colors.textSecondary
            )
          }
        }
      }
    }
  }
}

@Composable
fun <T> HabitRadarSegmentedControl(
  options: List<T>,
  selected: T,
  labelMapper: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  height: Dp = 52.dp
) {
  val colors = LocalHabitRadarColors.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .height(height)
      .clip(RoundedCornerShape(28.dp))
      .background(colors.surface2)
      .padding(4.dp)
  ) {
    options.forEach { item ->
      val isSelected = item == selected
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(24.dp))
          .background(if (isSelected) colors.segmentSelected else Color.Transparent)
          .clickable { onSelect(item) }
          .padding(horizontal = 10.dp)
      ) {
        Text(
          text = labelMapper(item),
          fontFamily = PlusJakartaSansFamily,
          fontSize = 14.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) colors.textPrimary else colors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun HabitRadarSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = LocalHabitRadarColors.current
  Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    colors = SwitchDefaults.colors(
      checkedThumbColor = Color.White,
      checkedTrackColor = colors.accent,
      checkedBorderColor = colors.accent,
      uncheckedThumbColor = Color.White,
      uncheckedTrackColor = colors.surface2,
      uncheckedBorderColor = colors.surface2
    ),
    modifier = modifier
  )
}

@Composable
fun HabitRadarCard(
  modifier: Modifier = Modifier,
  backgroundColor: Color = LocalHabitRadarColors.current.card,
  borderColor: Color? = null,
  contentPadding: PaddingValues = PaddingValues(18.dp),
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(28.dp)
  val baseMod = modifier
    .fillMaxWidth()
    .clip(shape)
    .background(backgroundColor)
    .then(
      if (borderColor != null) Modifier.border(1.5.dp, borderColor, shape) else Modifier
    )
    .then(
      if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
    .padding(contentPadding)

  Column(modifier = baseMod, content = content)
}

@Composable
fun CardHairline(modifier: Modifier = Modifier) {
  val colors = LocalHabitRadarColors.current
  HorizontalDivider(
    thickness = 1.dp,
    color = colors.surface2,
    modifier = modifier.padding(horizontal = 16.dp)
  )
}

@Composable
fun SettingsNavRow(
  title: String,
  subtitle: String? = null,
  trailingText: String? = null,
  badgeCount: Int? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: (@Composable () -> Unit)? = null
) {
  val colors = LocalHabitRadarColors.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 18.dp, vertical = 16.dp)
  ) {
    if (leadingIcon != null) {
      leadingIcon()
      Spacer(modifier = Modifier.width(14.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontFamily = PlusJakartaSansFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textPrimary
      )
      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          color = colors.textSecondary
        )
      }
    }
    if (badgeCount != null && badgeCount > 0) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(26.dp)
          .clip(CircleShape)
          .background(colors.accent)
      ) {
        Text(
          text = badgeCount.toString(),
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = colors.onAccent
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
    } else if (!trailingText.isNullOrBlank()) {
      Text(
        text = trailingText,
        fontFamily = PlusJakartaSansFamily,
        fontSize = 14.sp,
        color = colors.textSecondary
      )
      Spacer(modifier = Modifier.width(6.dp))
    }
    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = colors.textSecondary,
      modifier = Modifier.size(20.dp)
    )
  }
}

@Composable
fun HabitCheckCircle(
  isDone: Boolean,
  isSkipped: Boolean,
  colorHex: String,
  size: Dp = 56.dp,
  progressRatio: Float? = null,
  progressLabel: String? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scale by animateFloatAsState(
    targetValue = if (isDone) 1.0f else 0.96f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "check_scale"
  )
  val baseColor = parseHexColor(colorHex)
  val fillBg = if (isDone) baseColor.copy(alpha = 0.72f) else Color.Transparent
  val ringColor = if (isDone) baseColor else baseColor.copy(alpha = 0.48f)

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .scale(scale)
      .clip(CircleShape)
      .background(fillBg)
      .clickable(onClick = onClick)
  ) {
    if (progressRatio != null && !isDone) {
      Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
        drawArc(
          color = baseColor.copy(alpha = 0.25f),
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
          color = baseColor,
          startAngle = -90f,
          sweepAngle = 360f * progressRatio.coerceIn(0f, 1f),
          useCenter = false,
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
      }
      if (!progressLabel.isNullOrBlank()) {
        Text(
          text = progressLabel,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White
        )
      }
    } else {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .border(2.dp, ringColor, CircleShape)
      ) {
        if (isDone) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Done",
            tint = Color(0xFF121216),
            modifier = Modifier.size(size * 0.45f)
          )
        } else if (isSkipped) {
          Text(
            text = "—",
            color = baseColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
      }
    }
  }
}

@Composable
fun FullScreenCelebrationOverlay(
  visible: Boolean,
  headlineText: String = "All *done.*",
  subtitleText: String = "Every small mark for today is complete.",
  onDismiss: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val animProgress = remember { Animatable(0f) }

  LaunchedEffect(visible) {
    if (visible) {
      animProgress.snapTo(0f)
      animProgress.animateTo(1f, animationSpec = tween(1400))
      delay(400)
      onDismiss()
    }
  }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(220)),
    exit = fadeOut(tween(250))
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xD90E0E11))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismiss
        )
        .testTag("celebration_overlay")
    ) {
      val particleColors = listOf(
        colors.accent,
        Color(0xFFA8DFB4),
        Color(0xFFF5C9A5),
        Color(0xFF8FBDF0),
        Color(0xFFF5DA8A)
      )
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val p = animProgress.value
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(colors.accent.copy(alpha = 0.28f * (1f - p * 0.4f)), Color.Transparent),
            center = center,
            radius = size.minDimension * 0.65f
          ),
          center = center,
          radius = size.minDimension * 0.65f
        )
        for (i in 0 until 28) {
          val angle = (i * 360f / 28f) * (Math.PI / 180.0)
          val dist = (60f + (i % 5) * 45f) * p * 2.2f
          val x = center.x + (cos(angle) * dist).toFloat()
          val y = center.y + (sin(angle) * dist).toFloat()
          val radius = (1f - p * 0.6f) * (8f + (i % 3) * 4f)
          drawCircle(
            color = particleColors[i % particleColors.size].copy(alpha = (1f - p).coerceIn(0f, 1f)),
            radius = radius,
            center = Offset(x, y)
          )
        }
      }
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
      ) {
        SerifHeadlineText(
          rawText = headlineText,
          fontSizeSp = 46,
          color = colors.textPrimary,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = subtitleText,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 15.sp,
          color = colors.textSecondary,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun HabitIconBadge(
  iconKey: String,
  colorKey: String,
  size: Dp = 36.dp,
  modifier: Modifier = Modifier
) {
  val tint = parseHexColor(colorKey)
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.32f))
      .background(tint.copy(alpha = 0.22f))
  ) {
    HabitIconView(
      iconKey = iconKey,
      tint = tint,
      modifier = Modifier.size(size * 0.56f),
      emojiSize = (size.value * 0.5f).sp
    )
  }
}

