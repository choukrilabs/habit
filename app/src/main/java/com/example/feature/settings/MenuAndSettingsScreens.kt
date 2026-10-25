package com.example.feature.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.example.core.di.appContainer
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.CardDensity
import com.example.core.domain.model.Entry
import com.example.core.domain.model.Goal
import com.example.core.domain.model.GoalStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.HeadlineMode
import com.example.core.domain.model.HomeTab
import com.example.core.domain.model.QuietHoursMode
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.ReminderSound
import com.example.core.domain.model.ThemeMode
import com.example.core.domain.model.TodayLayout
import com.example.core.domain.model.WeekStartDay
import com.example.core.domain.usecase.StreakCalculator
import com.example.core.notifications.ReminderScheduler
import com.example.core.ui.components.CardHairline
import com.example.core.ui.components.HabitIconView
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.components.HabitRadarSegmentedControl
import com.example.core.ui.components.HabitRadarSwitch
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.components.SerifHeadlineText
import com.example.core.ui.components.SettingsNavRow
import com.example.core.ui.components.SubPageTopBar
import com.example.core.ui.theme.AppAccentSwatches
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.example.core.ui.theme.habitTint
import com.example.core.ui.theme.parseHexColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

// 5.7 MENU SCREEN (Strictly 7 rows in GENERAL card)
@Composable
fun MenuScreen(
  enabledRemindersCount: Int,
  activeGoalsCount: Int,
  currentYear: Int,
  onBack: () -> Unit,
  onNavigateNotifications: () -> Unit,
  onNavigateSettings: () -> Unit,
  onNavigateWeeklyRecap: () -> Unit,
  onNavigateMonthlyRecap: () -> Unit,
  onNavigateYearInReview: () -> Unit,
  onNavigateMilestones: () -> Unit,
  onNavigateWidgets: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(title = "Menu", onBack = onBack)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      SectionLabel("GENERAL")
      HabitRadarCard(
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.testTag("menu_general_card")
      ) {
        // Row 1: Notifications (badge if > 0)
        SettingsNavRow(
          title = "Notifications",
          badgeCount = if (enabledRemindersCount > 0) enabledRemindersCount else null,
          onClick = onNavigateNotifications,
          modifier = Modifier.testTag("menu_row_notifications")
        )
        CardHairline()
        // Row 2: Settings
        SettingsNavRow(
          title = "Settings",
          onClick = onNavigateSettings,
          modifier = Modifier.testTag("menu_row_settings")
        )
        CardHairline()
        // Row 3: Weekly recap
        SettingsNavRow(
          title = "Weekly recap",
          onClick = onNavigateWeeklyRecap,
          modifier = Modifier.testTag("menu_row_weekly_recap")
        )
        CardHairline()
        // Row 4: Monthly recap
        SettingsNavRow(
          title = "Monthly recap",
          onClick = onNavigateMonthlyRecap,
          modifier = Modifier.testTag("menu_row_monthly_recap")
        )
        CardHairline()
        // Row 5: Year in Review (trailing year)
        SettingsNavRow(
          title = "Year in Review",
          trailingText = currentYear.toString(),
          onClick = onNavigateYearInReview,
          modifier = Modifier.testTag("menu_row_year_in_review")
        )
        CardHairline()
        // Row 6: Milestones (trailing X active if > 0)
        SettingsNavRow(
          title = "Milestones",
          trailingText = if (activeGoalsCount > 0) "$activeGoalsCount active" else null,
          onClick = onNavigateMilestones,
          modifier = Modifier.testTag("menu_row_milestones")
        )
        CardHairline()
        // Row 7: Widgets (trailing Add to home screen)
        SettingsNavRow(
          title = "Widgets",
          trailingText = "Add to home screen",
          onClick = onNavigateWidgets,
          modifier = Modifier.testTag("menu_row_widgets")
        )
      }
    }
  }
}

// 5.9 SETTINGS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  settings: AppSettings,
  archivedCount: Int,
  today: LocalDate,
  onBack: () -> Unit,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onOpenHomeStudio: () -> Unit,
  onOpenReorderHabits: () -> Unit,
  onOpenBackupRestore: () -> Unit,
  onOpenArchivedHabits: () -> Unit,
  onReplayWelcomeTour: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var showRolloverPicker by remember { mutableStateOf(false) }
  var showLanguagePicker by remember { mutableStateOf(false) }

  val lastBackupText = remember(settings.lastBackupAt, today) {
    if (settings.lastBackupAt == null) "Never"
    else {
      val bDate = settings.lastBackupAt.atZone(ZoneId.systemDefault()).toLocalDate()
      val days = ChronoUnit.DAYS.between(bDate, today)
      when {
        days <= 0L -> "Today"
        days == 1L -> "1 day ago"
        else -> "$days days ago"
      }
    }
  }

  val languageLabel = when (settings.language) {
    "ar" -> "العربية"
    "fr" -> "Français"
    else -> "English"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(title = "Settings", onBack = onBack)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      // Top card: Home Studio
      HabitRadarCard(
        contentPadding = PaddingValues(0.dp),
        onClick = onOpenHomeStudio
      ) {
        SettingsNavRow(
          title = "Home Studio",
          subtitle = "Theme, colors and home layout — all in one place.",
          onClick = onOpenHomeStudio,
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = null,
              tint = colors.textPrimary,
              modifier = Modifier.size(24.dp)
            )
          }
        )
      }

      Spacer(modifier = Modifier.height(22.dp))
      SectionLabel("PREFERENCES")

      HabitRadarCard(contentPadding = PaddingValues(0.dp)) {
        // 1. Week start on
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Week start on", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
              Text("Affects every grid and recap", fontSize = 12.sp, color = colors.textSecondary)
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          HabitRadarSegmentedControl(
            options = WeekStartDay.entries,
            selected = settings.weekStart,
            labelMapper = { it.label },
            onSelect = { w -> onUpdateSettings { it.copy(weekStart = w) } },
            height = 44.dp,
            modifier = Modifier.fillMaxWidth()
          )
        }
        CardHairline()

        // 2. Sounds
        SettingsToggleRow(
          title = "Sounds",
          subtitle = "A soft tick when you keep a habit",
          checked = settings.sounds,
          onCheckedChange = { v -> onUpdateSettings { it.copy(sounds = v) } }
        )
        CardHairline()

        // 3. Daily nudges
        SettingsToggleRow(
          title = "Daily nudges",
          subtitle = "A morning kickoff and one evening save — never more.",
          checked = settings.dailyNudges,
          onCheckedChange = { v -> onUpdateSettings { it.copy(dailyNudges = v) } }
        )
        CardHairline()

        // 4. Card density
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
          Text("Card density", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
          HabitRadarSegmentedControl(
            options = CardDensity.entries,
            selected = settings.cardDensity,
            labelMapper = { if (it == CardDensity.COMFORTABLE) "Comfortable" else "Compact" },
            onSelect = { d -> onUpdateSettings { it.copy(cardDensity = d) } },
            height = 40.dp,
            modifier = Modifier.width(210.dp)
          )
        }
        CardHairline()

        // 5. Reorder habits
        SettingsNavRow(
          title = "Reorder habits",
          onClick = onOpenReorderHabits
        )
        CardHairline()

        // 6. Show streak icon on (Today, Weekly, Monthly)
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
          Text(
            text = "Show streak icon on",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
          )
          SettingsToggleRow(
            title = "Today",
            checked = settings.showStreakToday,
            onCheckedChange = { v -> onUpdateSettings { it.copy(showStreakToday = v) } }
          )
          SettingsToggleRow(
            title = "Weekly",
            checked = settings.showStreakWeekly,
            onCheckedChange = { v -> onUpdateSettings { it.copy(showStreakWeekly = v) } }
          )
          SettingsToggleRow(
            title = "Monthly",
            checked = settings.showStreakMonthly,
            onCheckedChange = { v -> onUpdateSettings { it.copy(showStreakMonthly = v) } }
          )
        }
        CardHairline()

        // 7. Show Hijri date
        SettingsToggleRow(
          title = "Show Hijri date",
          checked = settings.showHijri,
          onCheckedChange = { v -> onUpdateSettings { it.copy(showHijri = v) } }
        )
        CardHairline()

        // 8. Day starts at
        SettingsNavRow(
          title = "Day starts at",
          subtitle = "Late-night check-ins count for the previous day until this time",
          trailingText = String.format("%02d:%02d", settings.dayRollover.hour, settings.dayRollover.minute),
          onClick = { showRolloverPicker = true }
        )
        CardHairline()

        // 9. Change Language
        SettingsNavRow(
          title = "Change Language",
          trailingText = languageLabel,
          onClick = { showLanguagePicker = true }
        )
      }

      Spacer(modifier = Modifier.height(22.dp))
      SectionLabel("DATA")

      HabitRadarCard(contentPadding = PaddingValues(0.dp)) {
        SettingsNavRow(
          title = "Backup & restore",
          trailingText = "Last backup: $lastBackupText",
          onClick = onOpenBackupRestore
        )
        CardHairline()
        SettingsNavRow(
          title = "Archived habits",
          trailingText = archivedCount.toString(),
          onClick = onOpenArchivedHabits
        )
        CardHairline()
        SettingsNavRow(
          title = "Welcome tour",
          onClick = onReplayWelcomeTour
        )
      }

      val context = LocalContext.current
      val scope = androidx.compose.runtime.rememberCoroutineScope()
      val authService = remember(context) { context.appContainer.authService }
      val currentUser = remember(authService) { authService.currentUser }
      if (currentUser != null) {
        Spacer(modifier = Modifier.height(22.dp))
        SectionLabel("CLOUD ACCOUNT")
        HabitRadarCard(contentPadding = PaddingValues(0.dp)) {
          SettingsNavRow(
            title = currentUser.displayName ?: currentUser.email ?: "Signed in with Google",
            subtitle = currentUser.email ?: "Cloud sync active",
            trailingText = "Sign out",
            onClick = {
              scope.launch {
                authService.signOut(context)
              }
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text = "Habit Radar syncs your habits and check-ins securely to your personal cloud account.",
        fontFamily = PlusJakartaSansFamily,
        fontSize = 13.sp,
        color = colors.textSecondary,
        modifier = Modifier.padding(horizontal = 4.dp)
      )
      Spacer(modifier = Modifier.height(32.dp))
    }
  }

  if (showRolloverPicker) {
    ModalBottomSheet(onDismissRequest = { showRolloverPicker = false }, containerColor = colors.bg) {
      Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Day starts at", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        listOf(
          LocalTime.of(0, 0) to "00:00 (Midnight)",
          LocalTime.of(2, 0) to "02:00 AM",
          LocalTime.of(3, 0) to "03:00 AM (Default)",
          LocalTime.of(4, 0) to "04:00 AM",
          LocalTime.of(5, 0) to "05:00 AM"
        ).forEach { (lt, label) ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onUpdateSettings { it.copy(dayRollover = lt) }
                showRolloverPicker = false
              }
              .padding(vertical = 12.dp)
          ) {
            Text(label, fontSize = 16.sp, color = colors.textPrimary)
            if (settings.dayRollover == lt) {
              Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent)
            }
          }
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }

  if (showLanguagePicker) {
    ModalBottomSheet(onDismissRequest = { showLanguagePicker = false }, containerColor = colors.bg) {
      Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Change Language", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        listOf(
          "en" to "English",
          "ar" to "العربية (Arabic RTL)",
          "fr" to "Français (French)"
        ).forEach { (code, name) ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onUpdateSettings { it.copy(language = code) }
                runCatching {
                  AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
                }
                showLanguagePicker = false
              }
              .padding(vertical = 12.dp)
          ) {
            Text(name, fontSize = 16.sp, color = colors.textPrimary)
            if (settings.language == code) {
              Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent)
            }
          }
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun SettingsToggleRow(
  title: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 18.dp, vertical = 12.dp)
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, fontFamily = PlusJakartaSansFamily, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, fontFamily = PlusJakartaSansFamily, fontSize = 12.sp, color = colors.textSecondary)
      }
    }
    HabitRadarSwitch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

// 5.10 HOME STUDIO SCREEN (Screenshots 1, 2, 3)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeStudioScreen(
  settings: AppSettings,
  onBack: () -> Unit,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onOpenReorderHabits: () -> Unit,
  onOpenReorderTabs: () -> Unit
) {
  val colors = LocalHabitRadarColors.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(title = "Home Studio", onBack = onBack)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      // 1. Live preview: phone-shaped mock (white rounded bezel, ~60% screen width, centered)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      ) {
        HomeStudioPhoneMockPreview(settings = settings)
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 2. Theme group card
      Text(
        text = "Theme",
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        color = colors.textPrimary,
        modifier = Modifier.padding(bottom = 10.dp)
      )
      HabitRadarCard {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Theme", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
          HabitRadarSegmentedControl(
            options = ThemeMode.entries,
            selected = settings.theme,
            labelMapper = {
              when (it) {
                ThemeMode.SYSTEM -> "System"
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
              }
            },
            onSelect = { t -> onUpdateSettings { it.copy(theme = t) } },
            height = 40.dp,
            modifier = Modifier.width(200.dp)
          )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text = "App color",
          fontSize = 17.sp,
          fontWeight = FontWeight.Medium,
          color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 6 per row swatches, no lock icons (all free)
        FlowRow(
          maxItemsInEachRow = 6,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          AppAccentSwatches.forEach { hex ->
            val sel = settings.accentColor.equals(hex, ignoreCase = true)
            val c = parseHexColor(hex)
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(c)
                .then(
                  if (sel) Modifier.border(3.dp, Color.White, CircleShape) else Modifier
                )
                .clickable { onUpdateSettings { it.copy(accentColor = hex) } }
            ) {
              if (sel) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected app color",
                  tint = Color(0xFF101114),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      // 3. App bar group card
      Text(
        text = "App bar",
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        color = colors.textPrimary,
        modifier = Modifier.padding(bottom = 10.dp)
      )
      HabitRadarCard(contentPadding = PaddingValues(0.dp)) {
        SettingsToggleRow(
          title = "Show greeting",
          checked = settings.showGreeting,
          onCheckedChange = { v -> onUpdateSettings { it.copy(showGreeting = v) } }
        )
        CardHairline()
        SettingsToggleRow(
          title = "Show day-signal band",
          checked = settings.showDaySignalBand,
          onCheckedChange = { v -> onUpdateSettings { it.copy(showDaySignalBand = v) } }
        )
        CardHairline()
        Column(modifier = Modifier.padding(18.dp)) {
          Text("Headline", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            HeadlineMode.entries.forEach { hm ->
              val sel = hm == settings.headline
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(if (sel) colors.accentContainer else colors.surface2)
                  .clickable { onUpdateSettings { it.copy(headline = hm) } }
              ) {
                Text(
                  text = hm.name.lowercase().replaceFirstChar { it.uppercase() },
                  fontSize = 14.sp,
                  fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                  color = if (sel) colors.accent else colors.textSecondary
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      // 4. Today layout group (3 selectable thumbnail cards)
      Text(
        text = "Today layout",
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        color = colors.textPrimary,
        modifier = Modifier.padding(bottom = 10.dp)
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        TodayLayout.entries.forEach { layout ->
          val sel = settings.todayLayout == layout
          val label = when (layout) {
            TodayLayout.CLASSIC -> "Classic"
            TodayLayout.ONE_THING -> "One thing"
            TodayLayout.LEDGER -> "Ledger"
          }
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .weight(1f)
              .clickable { onUpdateSettings { it.copy(todayLayout = layout) } }
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.card)
                .border(
                  width = if (sel) 2.dp else 1.dp,
                  color = if (sel) colors.accent else colors.surface2,
                  shape = RoundedCornerShape(20.dp)
                )
                .padding(12.dp)
            ) {
              when (layout) {
                TodayLayout.CLASSIC -> {
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(3) {
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(16.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(if (sel) colors.accentContainer else colors.surface2)
                      )
                    }
                  }
                }
                TodayLayout.ONE_THING -> {
                  Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) colors.accentContainer else colors.surface2)
                    )
                    repeat(2) {
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(6.dp)
                          .clip(RoundedCornerShape(3.dp))
                          .background(colors.surface2)
                      )
                    }
                  }
                }
                TodayLayout.LEDGER -> {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(4) {
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(6.dp)
                          .clip(RoundedCornerShape(3.dp))
                          .background(if (sel) colors.accentContainer else colors.surface2)
                      )
                    }
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = label,
              fontSize = 13.sp,
              fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
              color = if (sel) colors.accent else colors.textSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 5. Card with density, hide completed, celebrations, group sections
      HabitRadarCard(contentPadding = PaddingValues(0.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
          Text("Card density", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
          HabitRadarSegmentedControl(
            options = CardDensity.entries,
            selected = settings.cardDensity,
            labelMapper = { if (it == CardDensity.COMFORTABLE) "Comfortable" else "Compact" },
            onSelect = { d -> onUpdateSettings { it.copy(cardDensity = d) } },
            height = 40.dp,
            modifier = Modifier.width(200.dp)
          )
        }
        CardHairline()
        SettingsToggleRow(
          title = "Hide completed habits",
          checked = settings.hideCompleted,
          onCheckedChange = { v -> onUpdateSettings { it.copy(hideCompleted = v) } }
        )
        CardHairline()
        SettingsToggleRow(
          title = "Full-screen celebrations",
          checked = settings.fullScreenCelebrations,
          onCheckedChange = { v -> onUpdateSettings { it.copy(fullScreenCelebrations = v) } }
        )
        CardHairline()
        SettingsToggleRow(
          title = "Group habits into sections",
          checked = settings.groupSections,
          onCheckedChange = { v -> onUpdateSettings { it.copy(groupSections = v) } }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 6. Reorder habits row
      HabitRadarCard(contentPadding = PaddingValues(0.dp), onClick = onOpenReorderHabits) {
        SettingsNavRow(
          title = "Reorder habits",
          onClick = onOpenReorderHabits,
          leadingIcon = {
            Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = colors.textPrimary)
          }
        )
      }

      Spacer(modifier = Modifier.height(22.dp))

      // 7. Bottom bar group
      Text(
        text = "Bottom bar",
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        color = colors.textPrimary,
        modifier = Modifier.padding(bottom = 10.dp)
      )
      HabitRadarCard(contentPadding = PaddingValues(0.dp)) {
        SettingsToggleRow(
          title = "Show home tab bar",
          subtitle = "Hide to keep Today only",
          checked = settings.showTabBar,
          onCheckedChange = { v -> onUpdateSettings { it.copy(showTabBar = v) } }
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      HabitRadarCard(contentPadding = PaddingValues(0.dp), onClick = onOpenReorderTabs) {
        SettingsNavRow(
          title = "Reorder tabs",
          onClick = onOpenReorderTabs,
          leadingIcon = {
            Icon(Icons.Default.Folder, contentDescription = null, tint = colors.textPrimary)
          }
        )
      }

      Spacer(modifier = Modifier.height(36.dp))
    }
  }
}

@Composable
private fun HomeStudioPhoneMockPreview(settings: AppSettings) {
  val colors = LocalHabitRadarColors.current
  val demoHabits = listOf(
    Triple("Lights out by 12", "#B79BE0", true),
    Triple("Log what I spent", "#F5C9A5", true),
    Triple("Fajr on time", "#7FC8BC", false),
    Triple("Read Quran", "#A8DFB4", false),
    Triple("Workout", "#EE9A9A", false)
  )

  Box(
    modifier = Modifier
      .width(236.dp)
      .clip(RoundedCornerShape(36.dp))
      .border(6.dp, Color(0xFFEAEAF0), RoundedCornerShape(36.dp))
      .background(colors.bg)
      .padding(12.dp)
  ) {
    Column {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("SATURDAY 24 OCTOBER", fontSize = 7.sp, color = colors.textSecondary, letterSpacing = 1.sp)
          if (settings.showGreeting) {
            SerifHeadlineText(rawText = "Good to *see you.*", fontSizeSp = 18)
          }
        }
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(colors.accent)
        )
      }
      if (settings.showDaySignalBand) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
          repeat(8) { idx ->
            Box(
              modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (idx < 2) colors.accent else Color(0xFF3A3B42))
            )
          }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = when (settings.headline) {
            HeadlineMode.COUNT -> "2 of 8 · 6 small marks left"
            HeadlineMode.PERCENT -> "25% · 6 small marks left"
            HeadlineMode.DAYS -> "5-day run · 6 small marks left"
          },
          fontSize = 7.sp,
          color = colors.textSecondary
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      val visibleDemo = if (settings.hideCompleted) demoHabits.filter { !it.third } else demoHabits
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        visibleDemo.forEach { (title, hex, done) ->
          val t = habitTint(hex, colors.isDark)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(t.cardBackground)
              .padding(horizontal = 8.dp, vertical = if (settings.cardDensity == CardDensity.COMPACT) 5.dp else 8.dp)
          ) {
            if (settings.todayLayout != TodayLayout.LEDGER) {
              Box(
                modifier = Modifier
                  .size(20.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(t.iconTileBackground)
              )
              Spacer(modifier = Modifier.width(6.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = title,
                fontSize = 9.sp,
                color = if (done) colors.textSecondary else colors.textPrimary,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1
              )
              Text("🔥 1 Day", fontSize = 7.sp, color = colors.textSecondary)
            }
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (done) t.baseColor else Color.Transparent)
                .border(1.dp, t.baseColor, CircleShape)
            )
          }
        }
      }

      if (settings.showTabBar) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(2.dp)
        ) {
          listOf("Today", "Weekly", "Monthly", "Overall").forEachIndexed { idx, l ->
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (idx == 0) colors.accentContainer else Color.Transparent)
            ) {
              Text(l, fontSize = 7.sp, color = if (idx == 0) colors.accent else colors.textSecondary)
            }
          }
        }
      }
    }
  }
}

// 5.8 NOTIFICATIONS SCREEN
@Composable
fun NotificationsScreen(
  habits: List<Habit>,
  reminders: List<Reminder>,
  goals: List<Goal>,
  settings: AppSettings,
  onBack: () -> Unit,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onToggleHabitReminders: (String, Boolean) -> Unit,
  onOpenHabitDetailReminders: (String) -> Unit,
  onOpenGoalDetail: (String) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val context = LocalContext.current

  var notifPermissionGranted by remember { mutableStateOf(true) }
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted -> notifPermissionGranted = granted }

  val exactAlarmsAllowed = remember { ReminderScheduler.canScheduleExactAlarms(context) }
  val remindersByHabit = remember(reminders) { reminders.groupBy { it.habitId } }

  val activeHabits = remember(habits) { habits.filter { it.status == HabitStatus.ACTIVE } }
  val habitsWithReminders = remember(activeHabits, remindersByHabit) {
    activeHabits.filter { remindersByHabit[it.id]?.isNotEmpty() == true }
  }
  val habitsWithoutReminders = remember(activeHabits, remindersByHabit) {
    activeHabits.filter { remindersByHabit[it.id].isNullOrEmpty() }
  }

  val activeGoals = remember(goals) { goals.filter { it.status == GoalStatus.ACTIVE } }
  val goalsWithReminders = remember(activeGoals) {
    activeGoals.filter { it.remindWeekly || it.remindStepDue || it.remindBeforeDays != null }
  }
  val goalsWithoutReminders = remember(activeGoals) {
    activeGoals.filter { !it.remindWeekly && !it.remindStepDue && it.remindBeforeDays == null }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(title = "Notifications", onBack = onBack)

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. Permission card
      if (!notifPermissionGranted || !exactAlarmsAllowed) {
        item {
          HabitRadarCard(backgroundColor = colors.accentContainer) {
            Text(
              text = "Permissions & Exact Alarms",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Allow notifications and exact alarms so gentle nudges arrive right on time.",
              fontSize = 13.sp,
              color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.accent)
                    .clickable { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                  Text("Turn on", color = colors.onAccent, fontWeight = FontWeight.Bold)
                }
              }
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmsAllowed) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface2)
                    .clickable {
                      runCatching {
                        context.startActivity(
                          Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                          }
                        )
                      }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                  Text("Exact alarms", color = colors.textPrimary)
                }
              }
            }
          }
        }
      }

      // 2. DAILY NUDGES card
      item {
        SectionLabel("DAILY NUDGES")
        HabitRadarCard(contentPadding = PaddingValues(0.dp)) {
          SettingsToggleRow(
            title = "Daily nudges",
            subtitle = "A morning kickoff and one evening save — never more.",
            checked = settings.dailyNudges,
            onCheckedChange = { v -> onUpdateSettings { it.copy(dailyNudges = v) } }
          )
          if (settings.dailyNudges) {
            CardHairline()
            SettingsToggleRow(
              title = "Morning kickoff (${String.format("%02d:%02d", settings.morningNudgeTime.hour, settings.morningNudgeTime.minute)})",
              subtitle = "“${activeHabits.size} small marks today. Start with one.”",
              checked = settings.morningNudgeEnabled,
              onCheckedChange = { v -> onUpdateSettings { it.copy(morningNudgeEnabled = v) } }
            )
            CardHairline()
            SettingsToggleRow(
              title = "Evening save (${String.format("%02d:%02d", settings.eveningNudgeTime.hour, settings.eveningNudgeTime.minute)})",
              subtitle = "Sent only if habits are still pending",
              checked = settings.eveningNudgeEnabled,
              onCheckedChange = { v -> onUpdateSettings { it.copy(eveningNudgeEnabled = v) } }
            )
          }
        }
      }

      // 3. HABIT REMINDERS card
      item {
        SectionLabel("HABIT REMINDERS")
        HabitRadarCard {
          if (habitsWithReminders.isEmpty()) {
            Text("No active habit reminders yet.", fontSize = 14.sp, color = colors.textSecondary)
          } else {
            habitsWithReminders.forEach { h ->
              val hRems = remindersByHabit[h.id].orEmpty()
              val anyEnabled = hRems.any { it.enabled }
              val chipsStr = hRems.joinToString(" · ") { String.format("%02d:%02d", it.time.hour, it.time.minute) }
              val tint = habitTint(h.colorKey, colors.isDark)

              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onOpenHabitDetailReminders(h.id) }
                  .padding(vertical = 8.dp)
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.iconTileBackground)
                ) {
                  HabitIconView(iconKey = h.iconKey, tint = tint.baseColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(h.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                  Text(chipsStr, fontSize = 12.sp, color = colors.accent)
                }
                HabitRadarSwitch(
                  checked = anyEnabled,
                  onCheckedChange = { onToggleHabitReminders(h.id, it) }
                )
              }
            }
          }

          if (habitsWithoutReminders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("No reminder", fontSize = 12.sp, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            habitsWithoutReminders.forEach { h ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
              ) {
                Text(h.name, fontSize = 15.sp, color = colors.textPrimary)
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .clickable { onOpenHabitDetailReminders(h.id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text("Add", fontSize = 12.sp, color = colors.accent, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }

      // 4. GOAL REMINDERS card
      item {
        SectionLabel("GOAL REMINDERS")
        HabitRadarCard {
          if (goalsWithReminders.isEmpty()) {
            Text("No goal reminders enabled.", fontSize = 14.sp, color = colors.textSecondary)
          } else {
            goalsWithReminders.forEach { g ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onOpenGoalDetail(g.id) }
                  .padding(vertical = 8.dp)
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(g.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                  Text(
                    text = buildList {
                      if (g.remindWeekly) add("Weekly")
                      if (g.remindBeforeDays != null) add("${g.remindBeforeDays}d before due")
                      if (g.remindStepDue) add("Step due dates")
                    }.joinToString(" · "),
                    fontSize = 12.sp,
                    color = colors.textSecondary
                  )
                }
              }
            }
          }
          if (goalsWithoutReminders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            goalsWithoutReminders.forEach { g ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
              ) {
                Text(g.title, fontSize = 15.sp, color = colors.textPrimary)
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface2)
                    .clickable { onOpenGoalDetail(g.id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text("Add", fontSize = 12.sp, color = colors.accent)
                }
              }
            }
          }
        }
      }

      // 5. PREFERENCES card
      item {
        SectionLabel("PREFERENCES")
        HabitRadarCard(contentPadding = PaddingValues(0.dp)) {
          SettingsToggleRow(
            title = "Quiet hours (23:00 – 06:00)",
            subtitle = "Mode: ${settings.quietHoursMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
            checked = settings.quietHoursEnabled,
            onCheckedChange = { v -> onUpdateSettings { it.copy(quietHoursEnabled = v) } }
          )
          if (settings.quietHoursEnabled) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
              HabitRadarSegmentedControl(
                options = QuietHoursMode.entries,
                selected = settings.quietHoursMode,
                labelMapper = { if (it == QuietHoursMode.MUTE) "Mute" else "Delay until end" },
                onSelect = { m -> onUpdateSettings { it.copy(quietHoursMode = m) } },
                height = 38.dp,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
          CardHairline()
          Column(modifier = Modifier.padding(18.dp)) {
            Text("Snooze length", fontSize = 15.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            HabitRadarSegmentedControl(
              options = listOf(10, 15, 30, 60),
              selected = settings.snoozeMin,
              labelMapper = { "${it}m" },
              onSelect = { s -> onUpdateSettings { it.copy(snoozeMin = s) } },
              height = 40.dp,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Sound", fontSize = 15.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            HabitRadarSegmentedControl(
              options = ReminderSound.entries,
              selected = settings.notifSound,
              labelMapper = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
              onSelect = { s -> onUpdateSettings { it.copy(notifSound = s) } },
              height = 40.dp,
              modifier = Modifier.fillMaxWidth()
            )
          }
          CardHairline()
          SettingsToggleRow(
            title = "Vibrate",
            checked = settings.notifVibrate,
            onCheckedChange = { v -> onUpdateSettings { it.copy(notifVibrate = v) } }
          )
          CardHairline()
          SettingsToggleRow(
            title = "Quick actions on notifications",
            subtitle = "Done · Snooze · Skip",
            checked = settings.notifQuickActions,
            onCheckedChange = { v -> onUpdateSettings { it.copy(notifQuickActions = v) } }
          )
          CardHairline()
          SettingsToggleRow(
            title = "Weekly recap reminder",
            checked = settings.recapWeekly,
            onCheckedChange = { v -> onUpdateSettings { it.copy(recapWeekly = v) } }
          )
          SettingsToggleRow(
            title = "Monthly recap reminder",
            checked = settings.recapMonthly,
            onCheckedChange = { v -> onUpdateSettings { it.copy(recapMonthly = v) } }
          )
          SettingsToggleRow(
            title = "Year in Review reminder",
            checked = settings.recapYearly,
            onCheckedChange = { v -> onUpdateSettings { it.copy(recapYearly = v) } }
          )
          CardHairline()
          SettingsToggleRow(
            title = "Goal reminders",
            checked = settings.goalReminders,
            onCheckedChange = { v -> onUpdateSettings { it.copy(goalReminders = v) } }
          )
        }
      }

      // 6. Send test notification button
      item {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colors.accent)
            .clickable { ReminderScheduler.sendTestNotification(context) }
            .testTag("send_test_notification_button")
        ) {
          Text(
            text = "Send test notification",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onAccent
          )
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

// 5.11 REORDER HABITS & REORDER TABS SCREENS
@Composable
fun ReorderHabitsScreen(
  habits: List<Habit>,
  onBack: () -> Unit,
  onSaveOrder: (List<String>) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val ordered = remember(habits) {
    mutableStateListOf<Habit>().apply { addAll(habits.filter { it.status == HabitStatus.ACTIVE }) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(
      title = "Reorder habits",
      onBack = {
        onSaveOrder(ordered.map { it.id })
        onBack()
      }
    )

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      itemsIndexed(ordered, key = { _, h -> h.id }) { idx, habit ->
        val tint = habitTint(habit.colorKey, colors.isDark)
        HabitRadarCard(
          backgroundColor = tint.cardBackground,
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DragHandle,
              contentDescription = "Reorder handle",
              tint = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.iconTileBackground)
            ) {
              HabitIconView(iconKey = habit.iconKey, tint = tint.baseColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = habit.name,
              fontFamily = PlusJakartaSansFamily,
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium,
              color = colors.textPrimary,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = {
                if (idx > 0) {
                  val item = ordered.removeAt(idx)
                  ordered.add(idx - 1, item)
                  onSaveOrder(ordered.map { it.id })
                }
              },
              enabled = idx > 0
            ) {
              Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", tint = colors.textPrimary)
            }
            IconButton(
              onClick = {
                if (idx < ordered.lastIndex) {
                  val item = ordered.removeAt(idx)
                  ordered.add(idx + 1, item)
                  onSaveOrder(ordered.map { it.id })
                }
              },
              enabled = idx < ordered.lastIndex
            ) {
              Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", tint = colors.textPrimary)
            }
          }
        }
      }
    }
  }
}

@Composable
fun ReorderTabsScreen(
  settings: AppSettings,
  onBack: () -> Unit,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val tabOrder = remember(settings.tabOrder) {
    mutableStateListOf<HomeTab>().apply { addAll(settings.tabOrder) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(title = "Reorder tabs", onBack = onBack)

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      itemsIndexed(tabOrder, key = { _, t -> t.id }) { idx, tab ->
        val isVisible = tab !in settings.hiddenTabs
        HabitRadarCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DragHandle, contentDescription = null, tint = colors.textSecondary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = tab.label,
              fontSize = 17.sp,
              fontWeight = FontWeight.Medium,
              color = colors.textPrimary,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = {
                if (idx > 0) {
                  val item = tabOrder.removeAt(idx)
                  tabOrder.add(idx - 1, item)
                  onUpdateSettings { it.copy(tabOrder = tabOrder.toList()) }
                }
              },
              enabled = idx > 0
            ) {
              Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", tint = colors.textPrimary)
            }
            IconButton(
              onClick = {
                if (idx < tabOrder.lastIndex) {
                  val item = tabOrder.removeAt(idx)
                  tabOrder.add(idx + 1, item)
                  onUpdateSettings { it.copy(tabOrder = tabOrder.toList()) }
                }
              },
              enabled = idx < tabOrder.lastIndex
            ) {
              Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", tint = colors.textPrimary)
            }
            if (tab != HomeTab.TODAY) {
              HabitRadarSwitch(
                checked = isVisible,
                onCheckedChange = { vis ->
                  val newHidden = settings.hiddenTabs.toMutableSet()
                  if (vis) newHidden.remove(tab) else newHidden.add(tab)
                  onUpdateSettings { it.copy(hiddenTabs = newHidden) }
                }
              )
            } else {
              Text("Required", fontSize = 12.sp, color = colors.textSecondary)
            }
          }
        }
      }
    }
  }
}

// 5.18 ARCHIVED HABITS SCREEN & PAUSE SHEET
@Composable
fun ArchivedHabitsScreen(
  archivedHabits: List<Habit>,
  entriesByHabit: Map<String, List<Entry>>,
  today: LocalDate,
  onBack: () -> Unit,
  onOpenHabitDetail: (String) -> Unit,
  onRestoreHabit: (Habit) -> Unit,
  onDeleteForever: (Habit) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var habitToDelete by remember { mutableStateOf<Habit?>(null) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(title = "Archived habits", onBack = onBack)

    if (archivedHabits.isEmpty()) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(32.dp)) {
        SerifHeadlineText(rawText = "Nothing *archived.*", fontSizeSp = 28)
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(archivedHabits, key = { it.id }) { habit ->
          val tint = habitTint(habit.colorKey, colors.isDark)
          val hEntries = entriesByHabit[habit.id].orEmpty()
          val s = StreakCalculator.calculateStreaks(habit, hEntries, emptyList(), today)
          val archivedDateStr = habit.archivedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()
            ?.format(DateTimeFormatter.ofPattern("d MMM")) ?: ""

          HabitRadarCard(
            backgroundColor = tint.cardBackground,
            onClick = { onOpenHabitDetail(habit.id) }
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(tint.iconTileBackground)
              ) {
                HabitIconView(iconKey = habit.iconKey, tint = tint.baseColor)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(
                  text = "Archived $archivedDateStr · best streak ${s.bestStreak} · ${hEntries.size} check-ins",
                  fontSize = 12.sp,
                  color = colors.textSecondary
                )
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .weight(1f)
                  .height(40.dp)
                  .clip(RoundedCornerShape(20.dp))
                  .background(colors.accent)
                  .clickable { onRestoreHabit(habit) }
              ) {
                Text("Restore", fontWeight = FontWeight.Bold, color = colors.onAccent, fontSize = 13.sp)
              }
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .weight(1f)
                  .height(40.dp)
                  .clip(RoundedCornerShape(20.dp))
                  .background(Color(0x26EE9A9A))
                  .clickable { habitToDelete = habit }
              ) {
                Text("Delete permanently", fontWeight = FontWeight.Medium, color = Color(0xFFEE9A9A), fontSize = 13.sp)
              }
            }
          }
        }
      }
    }
  }

  if (habitToDelete != null) {
    val h = habitToDelete!!
    AlertDialog(
      onDismissRequest = { habitToDelete = null },
      title = { Text("Delete ${h.name} permanently?") },
      text = { Text("This cannot be undone.") },
      confirmButton = {
        TextButton(onClick = { onDeleteForever(h); habitToDelete = null }) {
          Text("Delete forever", color = Color(0xFFEE9A9A))
        }
      },
      dismissButton = {
        TextButton(onClick = { habitToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseHabitBottomSheet(
  habit: Habit,
  today: LocalDate,
  onDismiss: () -> Unit,
  onConfirmPause: (LocalDate, LocalDate?) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val options = listOf(
    "Tomorrow" to today.plusDays(1),
    "3 days" to today.plusDays(2),
    "1 week" to today.plusDays(6),
    "2 weeks" to today.plusDays(13),
    "Until I resume" to null
  )
  var selectedIdx by remember { mutableIntStateOf(2) }

  ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.bg) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
      Text(
        text = "Pause ${habit.name}",
        fontFamily = InstrumentSerifFamily,
        fontSize = 28.sp,
        color = colors.textPrimary
      )
      Text(
        text = "Paused days neither break your streak nor lower completion rates.",
        fontSize = 13.sp,
        color = colors.textSecondary
      )
      Spacer(modifier = Modifier.height(16.dp))
      options.forEachIndexed { idx, (label, _) ->
        val sel = idx == selectedIdx
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (sel) colors.accentContainer else colors.surface2)
            .clickable { selectedIdx = idx }
            .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
          Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (sel) colors.accent else colors.textPrimary)
          if (sel) Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent)
        }
        Spacer(modifier = Modifier.height(8.dp))
      }
      Spacer(modifier = Modifier.height(12.dp))
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .clip(RoundedCornerShape(25.dp))
          .background(colors.accent)
          .clickable {
            onConfirmPause(today, options[selectedIdx].second)
          }
      ) {
        Text("Pause", color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
