package com.example.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.domain.model.CardDensity
import com.example.core.domain.model.ThemeMode
import com.example.core.domain.model.TodayLayout

// Design System Core Tokens (Section 2.1)
val DarkBg = Color(0xFF0E0E11)
val DarkAppBar = Color(0xFF1A1B21)
val DarkCard = Color(0xFF17181D)
val DarkSurface2 = Color(0xFF25262C)
val DarkSegmentSelected = Color(0xFF0F1013)
val DarkTextPrimary = Color(0xFFF2F2F4)
val DarkTextSecondary = Color(0xFF8E8F98)
val StreakFireOrange = Color(0xFFFF8A3D)
val DefaultOrchidAccent = Color(0xFFE8A8F5)

val LightBg = Color(0xFFF6F5F8)
val LightAppBar = Color(0xFFECEAF0)
val LightCard = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFE4E2EA)
val LightSegmentSelected = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF151519)
val LightTextSecondary = Color(0xFF63646E)

// Habit Color Swatches (Section 2.2 - 2 rows x 8 swatches)
val HabitPaletteRow1 = listOf(
  "#D6BEFA", // lavender
  "#A8DFB4", // mint
  "#F5C9A5", // peach
  "#F2A8C8", // pink
  "#F5DA8A", // soft yellow
  "#C6E3A0", // lime
  "#7FC8BC", // teal
  "#A5B0E0"  // periwinkle
)

val HabitPaletteRow2 = listOf(
  "#8FBDF0", // sky (default)
  "#F5B8AE", // salmon
  "#A0DDDD", // aqua
  "#EE9A9A", // coral
  "#EEE18A", // butter
  "#8FBB96", // sage
  "#7DB4E8", // blue
  "#B79BE0"  // violet
)

val AllHabitColorSwatches = HabitPaletteRow1 + HabitPaletteRow2

// Home Studio App Accent Swatches (Section 5.10 - 21 swatches, all free)
val AppAccentSwatches = listOf(
  "#E8A8F5", // default orchid
  "#7ED87E", // green
  "#4FD8C4", // mint-teal
  "#3DDCF0", // cyan
  "#5BB0D8", // steel
  "#9CC8FF", // periwinkle
  "#B8E6FF", // ice
  "#B4BCFF", // lavender
  "#B39DDB", // purple
  "#F7A8FF", // magenta
  "#FFB0C0", // pink
  "#EBC4D8", // blush
  "#D98585", // dusty red
  "#FFB8A8", // salmon
  "#FFA894", // peach
  "#FFBC78", // apricot
  "#DDB45C", // mustard
  "#EEA85C", // orange
  "#FFB300", // amber
  "#C0D860", // lime
  "#8C7B78"  // taupe
)

fun parseHexColor(hex: String, fallback: Color = DefaultOrchidAccent): Color {
  return try {
    val clean = hex.removePrefix("#").trim()
    val longVal = clean.toLong(16)
    if (clean.length == 6) {
      Color(0xFF000000 or longVal)
    } else if (clean.length == 8) {
      Color(longVal)
    } else fallback
  } catch (_: Exception) {
    fallback
  }
}

fun blendColors(fg: Color, bg: Color, ratio: Float): Color {
  val r = (fg.red * ratio + bg.red * (1f - ratio)).coerceIn(0f, 1f)
  val g = (fg.green * ratio + bg.green * (1f - ratio)).coerceIn(0f, 1f)
  val b = (fg.blue * ratio + bg.blue * (1f - ratio)).coerceIn(0f, 1f)
  return Color(r, g, b, 1f)
}

@Immutable
data class HabitTintColors(
  val baseColor: Color,
  val cardBackground: Color,
  val iconTileBackground: Color,
  val checkFilledBackground: Color,
  val checkRingStroke: Color,
  val subtleElementBg: Color
)

fun habitTint(colorHex: String, isDark: Boolean = true): HabitTintColors {
  val base = parseHexColor(colorHex, Color(0xFF8FBDF0))
  return if (isDark) {
    HabitTintColors(
      baseColor = base,
      cardBackground = blendColors(base, Color(0xFF121317), 0.14f),
      iconTileBackground = blendColors(base, Color(0xFF1A1B21), 0.24f),
      checkFilledBackground = blendColors(base, Color(0xFF101114), 0.72f),
      checkRingStroke = blendColors(base, Color(0xFF25262C), 0.55f),
      subtleElementBg = blendColors(base, Color(0xFF16171C), 0.20f)
    )
  } else {
    HabitTintColors(
      baseColor = base,
      cardBackground = blendColors(base, Color(0xFFFFFFFF), 0.22f),
      iconTileBackground = blendColors(base, Color(0xFFFFFFFF), 0.38f),
      checkFilledBackground = blendColors(base, Color(0xFF151519), 0.78f),
      checkRingStroke = blendColors(base, Color(0xFF888890), 0.65f),
      subtleElementBg = blendColors(base, Color(0xFFF6F5F8), 0.30f)
    )
  }
}

val InstrumentSerifFamily = FontFamily(
  Font(R.font.instrument_serif, FontWeight.Normal)
)

val PlusJakartaSansFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val InstrumentSerif = InstrumentSerifFamily

fun habitColor(hex: String): Color = parseHexColor(hex, Color(0xFF8FBDF0))

fun resolveAppAccent(hex: String): Color = parseHexColor(hex, DefaultOrchidAccent)

/**
 * Formats a headline containing `*italic*` emphasis markers using Instrument Serif.
 */
fun formatSerifHeadline(raw: String, accentColor: Color? = null): AnnotatedString {
  return buildAnnotatedString {
    var i = 0
    while (i < raw.length) {
      val startStar = raw.indexOf('*', i)
      if (startStar == -1) {
        append(raw.substring(i))
        break
      }
      val endStar = raw.indexOf('*', startStar + 1)
      if (endStar == -1) {
        append(raw.substring(i))
        break
      }
      append(raw.substring(i, startStar))
      val italicWord = raw.substring(startStar + 1, endStar)
      if (accentColor != null) {
        pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = accentColor))
      } else {
        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
      }
      append(italicWord)
      pop()
      i = endStar + 1
    }
  }
}

val HabitRadarTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = InstrumentSerifFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 34.sp,
    lineHeight = 40.sp
  ),
  headlineMedium = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    lineHeight = 30.sp
  ),
  titleLarge = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 22.sp,
    lineHeight = 28.sp
  ),
  titleMedium = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 20.sp,
    lineHeight = 25.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  labelSmall = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    letterSpacing = 3.sp,
    lineHeight = 18.sp
  )
)

@Immutable
data class HabitRadarColors(
  val isDark: Boolean,
  val bg: Color,
  val appBar: Color,
  val card: Color,
  val surface2: Color,
  val segmentSelected: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val accent: Color,
  val accentContainer: Color,
  val onAccent: Color,
  val streakFire: Color
) {
  val background: Color get() = bg
  val surface: Color get() = card
  val surfaceElevated: Color get() = surface2
  val borderSubtle: Color get() = surface2
  val textTertiary: Color get() = textSecondary.copy(alpha = 0.6f)
  val success: Color get() = Color(0xFF7ED87E)
  val warning: Color get() = Color(0xFFFFB870)
  val danger: Color get() = Color(0xFFEE8A8A)
}

val LocalHabitRadarColors = staticCompositionLocalOf {
  HabitRadarColors(
    isDark = true,
    bg = DarkBg,
    appBar = DarkAppBar,
    card = DarkCard,
    surface2 = DarkSurface2,
    segmentSelected = DarkSegmentSelected,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    accent = DefaultOrchidAccent,
    accentContainer = Color(0xFF2E2540),
    onAccent = Color(0xFF121016),
    streakFire = StreakFireOrange
  )
}

val LocalCardDensity = staticCompositionLocalOf { CardDensity.COMFORTABLE }
val LocalTodayLayout = staticCompositionLocalOf { TodayLayout.CLASSIC }

@Composable
fun HabitRadarTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  accentHex: String = "#E8A8F5",
  cardDensity: CardDensity = CardDensity.COMFORTABLE,
  todayLayout: TodayLayout = TodayLayout.CLASSIC,
  content: @Composable () -> Unit
) {
  val isDark = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val accent = parseHexColor(accentHex, DefaultOrchidAccent)
  val bg = if (isDark) DarkBg else LightBg
  val accentContainer = blendColors(accent, bg, if (isDark) 0.22f else 0.26f)
  val onAccent = if (accent.luminance() > 0.45f) Color(0xFF101014) else Color.White

  val customColors = HabitRadarColors(
    isDark = isDark,
    bg = bg,
    appBar = if (isDark) DarkAppBar else LightAppBar,
    card = if (isDark) DarkCard else LightCard,
    surface2 = if (isDark) DarkSurface2 else LightSurface2,
    segmentSelected = if (isDark) DarkSegmentSelected else LightSegmentSelected,
    textPrimary = if (isDark) DarkTextPrimary else LightTextPrimary,
    textSecondary = if (isDark) DarkTextSecondary else LightTextSecondary,
    accent = accent,
    accentContainer = accentContainer,
    onAccent = onAccent,
    streakFire = StreakFireOrange
  )

  val colorScheme = if (isDark) {
    darkColorScheme(
      primary = accent,
      onPrimary = onAccent,
      primaryContainer = accentContainer,
      onPrimaryContainer = accent,
      background = customColors.bg,
      onBackground = customColors.textPrimary,
      surface = customColors.card,
      onSurface = customColors.textPrimary,
      surfaceVariant = customColors.surface2,
      onSurfaceVariant = customColors.textSecondary
    )
  } else {
    lightColorScheme(
      primary = accent,
      onPrimary = onAccent,
      primaryContainer = accentContainer,
      onPrimaryContainer = customColors.textPrimary,
      background = customColors.bg,
      onBackground = customColors.textPrimary,
      surface = customColors.card,
      onSurface = customColors.textPrimary,
      surfaceVariant = customColors.surface2,
      onSurfaceVariant = customColors.textSecondary
    )
  }

  CompositionLocalProvider(
    LocalHabitRadarColors provides customColors,
    LocalCardDensity provides cardDensity,
    LocalTodayLayout provides todayLayout
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = HabitRadarTypography,
      content = content
    )
  }
}
