package com.example.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.CardDensity
import com.example.core.domain.model.HeadlineMode
import com.example.core.domain.model.HomeTab
import com.example.core.domain.model.QuietHoursMode
import com.example.core.domain.model.ReminderSound
import com.example.core.domain.model.ThemeMode
import com.example.core.domain.model.TodayLayout
import com.example.core.domain.model.WeekStartDay
import com.example.core.domain.model.WidgetCornerStyle
import com.example.core.domain.model.WidgetThemeMode
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "habit_radar_settings")

class SettingsDataStore(private val context: Context) {

  private object Keys {
    val WEEK_START = stringPreferencesKey("week_start")
    val SOUNDS = booleanPreferencesKey("sounds")
    val DAILY_NUDGES = booleanPreferencesKey("daily_nudges")
    val CARD_DENSITY = stringPreferencesKey("card_density")
    val SHOW_STREAK_TODAY = booleanPreferencesKey("show_streak_today")
    val SHOW_STREAK_WEEKLY = booleanPreferencesKey("show_streak_weekly")
    val SHOW_STREAK_MONTHLY = booleanPreferencesKey("show_streak_monthly")
    val SHOW_HIJRI = booleanPreferencesKey("show_hijri")
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme")
    val ACCENT_COLOR = stringPreferencesKey("accent_color")
    val SHOW_GREETING = booleanPreferencesKey("show_greeting")
    val SHOW_DAY_SIGNAL_BAND = booleanPreferencesKey("show_day_signal_band")
    val HEADLINE = stringPreferencesKey("headline")
    val TODAY_LAYOUT = stringPreferencesKey("today_layout")
    val HIDE_COMPLETED = booleanPreferencesKey("hide_completed")
    val FULL_SCREEN_CELEBRATIONS = booleanPreferencesKey("full_screen_celebrations")
    val SHOW_TAB_BAR = booleanPreferencesKey("show_tab_bar")
    val TAB_ORDER = stringPreferencesKey("tab_order")
    val HIDDEN_TABS = stringPreferencesKey("hidden_tabs")
    val MORNING_NUDGE_ENABLED = booleanPreferencesKey("morning_nudge_enabled")
    val MORNING_NUDGE_TIME = stringPreferencesKey("morning_nudge_time")
    val EVENING_NUDGE_ENABLED = booleanPreferencesKey("evening_nudge_enabled")
    val EVENING_NUDGE_TIME = stringPreferencesKey("evening_nudge_time")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_HOURS_FROM = stringPreferencesKey("quiet_hours_from")
    val QUIET_HOURS_TO = stringPreferencesKey("quiet_hours_to")
    val QUIET_HOURS_MODE = stringPreferencesKey("quiet_hours_mode")
    val SNOOZE_MIN = intPreferencesKey("snooze_min")
    val NOTIF_SOUND = stringPreferencesKey("notif_sound")
    val NOTIF_VIBRATE = booleanPreferencesKey("notif_vibrate")
    val NOTIF_QUICK_ACTIONS = booleanPreferencesKey("notif_quick_actions")
    val RECAP_WEEKLY = booleanPreferencesKey("recap_weekly")
    val RECAP_MONTHLY = booleanPreferencesKey("recap_monthly")
    val RECAP_YEARLY = booleanPreferencesKey("recap_yearly")
    val GOAL_REMINDERS = booleanPreferencesKey("goal_reminders")
    val DAY_ROLLOVER = stringPreferencesKey("day_rollover")
    val GROUP_SECTIONS = booleanPreferencesKey("group_sections")
    val GROUP_FILTER = stringPreferencesKey("group_filter")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    val COACHMARK_SHOWN = booleanPreferencesKey("coachmark_shown")
    val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
    val AUTO_BACKUP_FOLDER_URI = stringPreferencesKey("auto_backup_folder_uri")
    val AUTO_BACKUP_KEEP = intPreferencesKey("auto_backup_keep")
    val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    val BACKUP_NUDGE = booleanPreferencesKey("backup_nudge")
    val DEFAULT_WIDGET_THEME = stringPreferencesKey("default_widget_theme")
    val DEFAULT_WIDGET_OPACITY = intPreferencesKey("default_widget_opacity")
    val DEFAULT_WIDGET_CORNER = stringPreferencesKey("default_widget_corner")
    val DEFAULT_WIDGET_SHOW_STREAK = booleanPreferencesKey("default_widget_show_streak")
    val DEFAULT_WIDGET_HIDE_NAMES = booleanPreferencesKey("default_widget_hide_names")
    val DEFAULT_WIDGET_ALLOW_BACKFILL = booleanPreferencesKey("default_widget_allow_backfill")
  }

  val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
    val tabOrderList = prefs[Keys.TAB_ORDER]
      ?.split(",")
      ?.mapNotNull { runCatching { HomeTab.valueOf(it) }.getOrNull() }
      ?.takeIf { it.isNotEmpty() }
      ?: listOf(HomeTab.TODAY, HomeTab.WEEKLY, HomeTab.MONTHLY, HomeTab.OVERALL)

    val hiddenSet = prefs[Keys.HIDDEN_TABS]
      ?.split(",")
      ?.filter { it.isNotBlank() }
      ?.mapNotNull { runCatching { HomeTab.valueOf(it) }.getOrNull() }
      ?.filter { it != HomeTab.TODAY }
      ?.toSet()
      ?: emptySet()

    AppSettings(
      weekStart = prefs[Keys.WEEK_START]?.let { runCatching { WeekStartDay.valueOf(it) }.getOrNull() } ?: WeekStartDay.MONDAY,
      sounds = prefs[Keys.SOUNDS] ?: true,
      dailyNudges = prefs[Keys.DAILY_NUDGES] ?: true,
      cardDensity = prefs[Keys.CARD_DENSITY]?.let { runCatching { CardDensity.valueOf(it) }.getOrNull() } ?: CardDensity.COMFORTABLE,
      showStreakToday = prefs[Keys.SHOW_STREAK_TODAY] ?: true,
      showStreakWeekly = prefs[Keys.SHOW_STREAK_WEEKLY] ?: true,
      showStreakMonthly = prefs[Keys.SHOW_STREAK_MONTHLY] ?: true,
      showHijri = prefs[Keys.SHOW_HIJRI] ?: false,
      language = prefs[Keys.LANGUAGE] ?: "en",
      theme = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
      accentColor = prefs[Keys.ACCENT_COLOR] ?: "#E8A8F5",
      showGreeting = prefs[Keys.SHOW_GREETING] ?: true,
      showDaySignalBand = prefs[Keys.SHOW_DAY_SIGNAL_BAND] ?: true,
      headline = prefs[Keys.HEADLINE]?.let { runCatching { HeadlineMode.valueOf(it) }.getOrNull() } ?: HeadlineMode.COUNT,
      todayLayout = prefs[Keys.TODAY_LAYOUT]?.let { runCatching { TodayLayout.valueOf(it) }.getOrNull() } ?: TodayLayout.CLASSIC,
      hideCompleted = prefs[Keys.HIDE_COMPLETED] ?: false,
      fullScreenCelebrations = prefs[Keys.FULL_SCREEN_CELEBRATIONS] ?: true,
      showTabBar = prefs[Keys.SHOW_TAB_BAR] ?: true,
      tabOrder = tabOrderList,
      hiddenTabs = hiddenSet,
      morningNudgeEnabled = prefs[Keys.MORNING_NUDGE_ENABLED] ?: true,
      morningNudgeTime = prefs[Keys.MORNING_NUDGE_TIME]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.of(8, 0),
      eveningNudgeEnabled = prefs[Keys.EVENING_NUDGE_ENABLED] ?: true,
      eveningNudgeTime = prefs[Keys.EVENING_NUDGE_TIME]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.of(21, 0),
      quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: false,
      quietHoursFrom = prefs[Keys.QUIET_HOURS_FROM]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.of(23, 0),
      quietHoursTo = prefs[Keys.QUIET_HOURS_TO]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.of(6, 0),
      quietHoursMode = prefs[Keys.QUIET_HOURS_MODE]?.let { runCatching { QuietHoursMode.valueOf(it) }.getOrNull() } ?: QuietHoursMode.MUTE,
      snoozeMin = prefs[Keys.SNOOZE_MIN] ?: 15,
      notifSound = prefs[Keys.NOTIF_SOUND]?.let { runCatching { ReminderSound.valueOf(it) }.getOrNull() } ?: ReminderSound.DEFAULT,
      notifVibrate = prefs[Keys.NOTIF_VIBRATE] ?: true,
      notifQuickActions = prefs[Keys.NOTIF_QUICK_ACTIONS] ?: true,
      recapWeekly = prefs[Keys.RECAP_WEEKLY] ?: true,
      recapMonthly = prefs[Keys.RECAP_MONTHLY] ?: true,
      recapYearly = prefs[Keys.RECAP_YEARLY] ?: true,
      goalReminders = prefs[Keys.GOAL_REMINDERS] ?: true,
      dayRollover = prefs[Keys.DAY_ROLLOVER]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.of(3, 0),
      groupSections = prefs[Keys.GROUP_SECTIONS] ?: false,
      groupFilter = prefs[Keys.GROUP_FILTER]?.takeIf { it.isNotBlank() },
      onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
      coachmarkShown = prefs[Keys.COACHMARK_SHOWN] ?: false,
      autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: false,
      autoBackupFolderUri = prefs[Keys.AUTO_BACKUP_FOLDER_URI]?.takeIf { it.isNotBlank() },
      autoBackupKeep = prefs[Keys.AUTO_BACKUP_KEEP] ?: 5,
      lastBackupAt = prefs[Keys.LAST_BACKUP_AT]?.let { Instant.ofEpochMilli(it) },
      backupNudge = prefs[Keys.BACKUP_NUDGE] ?: true,
      defaultWidgetTheme = prefs[Keys.DEFAULT_WIDGET_THEME]?.let { runCatching { WidgetThemeMode.valueOf(it) }.getOrNull() } ?: WidgetThemeMode.FOLLOW,
      defaultWidgetOpacity = prefs[Keys.DEFAULT_WIDGET_OPACITY] ?: 100,
      defaultWidgetCorner = prefs[Keys.DEFAULT_WIDGET_CORNER]?.let { runCatching { WidgetCornerStyle.valueOf(it) }.getOrNull() } ?: WidgetCornerStyle.ROUNDED,
      defaultWidgetShowStreak = prefs[Keys.DEFAULT_WIDGET_SHOW_STREAK] ?: true,
      defaultWidgetHideNames = prefs[Keys.DEFAULT_WIDGET_HIDE_NAMES] ?: false,
      defaultWidgetAllowBackfill = prefs[Keys.DEFAULT_WIDGET_ALLOW_BACKFILL] ?: true
    )
  }

  suspend fun getSettings(): AppSettings = settingsFlow.first()

  suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
    val current = getSettings()
    val updated = transform(current)
    saveSettings(updated)
  }

  suspend fun saveSettings(settings: AppSettings) {
    context.dataStore.edit { prefs ->
      prefs[Keys.WEEK_START] = settings.weekStart.name
      prefs[Keys.SOUNDS] = settings.sounds
      prefs[Keys.DAILY_NUDGES] = settings.dailyNudges
      prefs[Keys.CARD_DENSITY] = settings.cardDensity.name
      prefs[Keys.SHOW_STREAK_TODAY] = settings.showStreakToday
      prefs[Keys.SHOW_STREAK_WEEKLY] = settings.showStreakWeekly
      prefs[Keys.SHOW_STREAK_MONTHLY] = settings.showStreakMonthly
      prefs[Keys.SHOW_HIJRI] = settings.showHijri
      prefs[Keys.LANGUAGE] = settings.language
      prefs[Keys.THEME] = settings.theme.name
      prefs[Keys.ACCENT_COLOR] = settings.accentColor
      prefs[Keys.SHOW_GREETING] = settings.showGreeting
      prefs[Keys.SHOW_DAY_SIGNAL_BAND] = settings.showDaySignalBand
      prefs[Keys.HEADLINE] = settings.headline.name
      prefs[Keys.TODAY_LAYOUT] = settings.todayLayout.name
      prefs[Keys.HIDE_COMPLETED] = settings.hideCompleted
      prefs[Keys.FULL_SCREEN_CELEBRATIONS] = settings.fullScreenCelebrations
      prefs[Keys.SHOW_TAB_BAR] = settings.showTabBar
      prefs[Keys.TAB_ORDER] = settings.tabOrder.joinToString(",") { it.name }
      prefs[Keys.HIDDEN_TABS] = settings.hiddenTabs.filter { it != HomeTab.TODAY }.joinToString(",") { it.name }
      prefs[Keys.MORNING_NUDGE_ENABLED] = settings.morningNudgeEnabled
      prefs[Keys.MORNING_NUDGE_TIME] = settings.morningNudgeTime.toString()
      prefs[Keys.EVENING_NUDGE_ENABLED] = settings.eveningNudgeEnabled
      prefs[Keys.EVENING_NUDGE_TIME] = settings.eveningNudgeTime.toString()
      prefs[Keys.QUIET_HOURS_ENABLED] = settings.quietHoursEnabled
      prefs[Keys.QUIET_HOURS_FROM] = settings.quietHoursFrom.toString()
      prefs[Keys.QUIET_HOURS_TO] = settings.quietHoursTo.toString()
      prefs[Keys.QUIET_HOURS_MODE] = settings.quietHoursMode.name
      prefs[Keys.SNOOZE_MIN] = settings.snoozeMin
      prefs[Keys.NOTIF_SOUND] = settings.notifSound.name
      prefs[Keys.NOTIF_VIBRATE] = settings.notifVibrate
      prefs[Keys.NOTIF_QUICK_ACTIONS] = settings.notifQuickActions
      prefs[Keys.RECAP_WEEKLY] = settings.recapWeekly
      prefs[Keys.RECAP_MONTHLY] = settings.recapMonthly
      prefs[Keys.RECAP_YEARLY] = settings.recapYearly
      prefs[Keys.GOAL_REMINDERS] = settings.goalReminders
      prefs[Keys.DAY_ROLLOVER] = settings.dayRollover.toString()
      prefs[Keys.GROUP_SECTIONS] = settings.groupSections
      prefs[Keys.GROUP_FILTER] = settings.groupFilter ?: ""
      prefs[Keys.ONBOARDING_DONE] = settings.onboardingDone
      prefs[Keys.COACHMARK_SHOWN] = settings.coachmarkShown
      prefs[Keys.AUTO_BACKUP_ENABLED] = settings.autoBackupEnabled
      prefs[Keys.AUTO_BACKUP_FOLDER_URI] = settings.autoBackupFolderUri ?: ""
      prefs[Keys.AUTO_BACKUP_KEEP] = settings.autoBackupKeep
      if (settings.lastBackupAt != null) {
        prefs[Keys.LAST_BACKUP_AT] = settings.lastBackupAt.toEpochMilli()
      }
      prefs[Keys.BACKUP_NUDGE] = settings.backupNudge
      prefs[Keys.DEFAULT_WIDGET_THEME] = settings.defaultWidgetTheme.name
      prefs[Keys.DEFAULT_WIDGET_OPACITY] = settings.defaultWidgetOpacity
      prefs[Keys.DEFAULT_WIDGET_CORNER] = settings.defaultWidgetCorner.name
      prefs[Keys.DEFAULT_WIDGET_SHOW_STREAK] = settings.defaultWidgetShowStreak
      prefs[Keys.DEFAULT_WIDGET_HIDE_NAMES] = settings.defaultWidgetHideNames
      prefs[Keys.DEFAULT_WIDGET_ALLOW_BACKFILL] = settings.defaultWidgetAllowBackfill
    }
  }
}
