package com.example.core.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.HabitRadarDatabase
import com.example.core.data.db.toDomain
import com.example.core.data.repository.HabitRadarRepository
import com.example.core.di.appContainer
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.QuietHoursMode
import com.example.core.domain.model.Reminder
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.feature.widgets.glance.WidgetUpdater
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationChannels {
  const val HABIT_REMINDERS = "habit_reminders"
  const val DAILY_NUDGES = "daily_nudges"
  const val RECAPS = "recaps"
  const val GOALS = "goals"

  fun ensureCreated(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channels = listOf(
      NotificationChannel(
        HABIT_REMINDERS,
        "Habit Reminders",
        NotificationManager.IMPORTANCE_HIGH
      ).apply { description = "Scheduled reminders for individual habits" },
      NotificationChannel(
        DAILY_NUDGES,
        "Daily Nudges",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply { description = "Morning kickoff and evening save nudges" },
      NotificationChannel(
        RECAPS,
        "Recaps & Backups",
        NotificationManager.IMPORTANCE_LOW
      ).apply { description = "Weekly, monthly, and yearly review notifications" },
      NotificationChannel(
        GOALS,
        "Milestones & Goals",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply { description = "Goal check-ins and step due date reminders" }
    )
    nm.createNotificationChannels(channels)
  }
}

object ReminderScheduler {

  const val ACTION_HABIT_REMINDER = "com.example.action.HABIT_REMINDER"
  const val ACTION_MORNING_NUDGE = "com.example.action.MORNING_NUDGE"
  const val ACTION_EVENING_NUDGE = "com.example.action.EVENING_NUDGE"
  const val ACTION_DONE = "com.example.action.NOTIF_DONE"
  const val ACTION_SNOOZE = "com.example.action.NOTIF_SNOOZE"
  const val ACTION_SKIP = "com.example.action.NOTIF_SKIP"

  const val EXTRA_REMINDER_ID = "extra_reminder_id"
  const val EXTRA_HABIT_ID = "extra_habit_id"
  const val EXTRA_NOTIF_ID = "extra_notif_id"
  const val EXTRA_SNOOZE_COUNT = "extra_snooze_count"

  fun canScheduleExactAlarms(context: Context): Boolean {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      am.canScheduleExactAlarms()
    } else true
  }

  suspend fun rescheduleAll(context: Context) {
    val container = context.appContainer
    val db = container.database
    val settings = container.settingsDataStore.getSettings()
    val habits = db.habitDao().getAllHabits().map { it.toDomain() }.associateBy { it.id }
    val pauses = db.pauseDao().getAllPauses().map { it.toDomain() }.groupBy { it.habitId }
    val reminders = db.reminderDao().getAllReminders().map { it.toDomain() }
    val now = LocalDateTime.now()

    reminders.forEach { reminder ->
      val habit = habits[reminder.habitId]
      if (habit != null && habit.status == HabitStatus.ACTIVE && reminder.enabled) {
        val nextTime = computeNextOccurrence(reminder, pauses[habit.id].orEmpty(), now)
        if (nextTime != null) {
          scheduleAlarmForReminder(context, reminder.id, habit.id, nextTime)
        } else {
          cancelAlarmForReminder(context, reminder.id)
        }
      } else {
        cancelAlarmForReminder(context, reminder.id)
      }
    }

    if (settings.dailyNudges && settings.morningNudgeEnabled) {
      scheduleDailyNudge(context, ACTION_MORNING_NUDGE, 9001, settings.morningNudgeTime, now)
    }
    if (settings.dailyNudges && settings.eveningNudgeEnabled) {
      scheduleDailyNudge(context, ACTION_EVENING_NUDGE, 9002, settings.eveningNudgeTime, now)
    }
  }

  fun computeNextOccurrence(
    reminder: Reminder,
    pauses: List<com.example.core.domain.model.HabitPause>,
    now: LocalDateTime
  ): LocalDateTime? {
    if (!reminder.enabled || reminder.daysOfWeek.isEmpty()) return null
    for (dayOffset in 0..14) {
      val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())
      if (candidateDate.dayOfWeek.value in reminder.daysOfWeek && !ScheduleMatcher.isPausedOn(candidateDate, pauses)) {
        val candidateDt = LocalDateTime.of(candidateDate, reminder.time)
        if (candidateDt.isAfter(now.plusSeconds(5))) {
          return candidateDt
        }
      }
    }
    return null
  }

  fun isWithinQuietHours(time: LocalTime, from: LocalTime, to: LocalTime): Boolean {
    return if (from.isBefore(to)) {
      !time.isBefore(from) && time.isBefore(to)
    } else {
      // Spans midnight (e.g. 23:00 to 06:00)
      !time.isBefore(from) || time.isBefore(to)
    }
  }

  fun scheduleAlarmForReminder(
    context: Context,
    reminderId: String,
    habitId: String,
    triggerAt: LocalDateTime
  ) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).apply {
      action = ACTION_HABIT_REMINDER
      putExtra(EXTRA_REMINDER_ID, reminderId)
      putExtra(EXTRA_HABIT_ID, habitId)
    }
    val pi = PendingIntent.getBroadcast(
      context,
      reminderId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val millis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    scheduleSafeExact(am, millis, pi)
  }

  fun cancelAlarmForReminder(context: Context, reminderId: String) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).apply {
      action = ACTION_HABIT_REMINDER
    }
    val pi = PendingIntent.getBroadcast(
      context,
      reminderId.hashCode(),
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pi != null) am.cancel(pi)
  }

  private fun scheduleDailyNudge(
    context: Context,
    action: String,
    reqCode: Int,
    time: LocalTime,
    now: LocalDateTime
  ) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    var dt = LocalDateTime.of(now.toLocalDate(), time)
    if (!dt.isAfter(now)) dt = dt.plusDays(1)
    val intent = Intent(context, ReminderReceiver::class.java).apply { this.action = action }
    val pi = PendingIntent.getBroadcast(
      context,
      reqCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val millis = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    scheduleSafeExact(am, millis, pi)
  }

  private fun scheduleSafeExact(am: AlarmManager, triggerMillis: Long, pi: PendingIntent) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
      } else {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
      }
    } catch (_: SecurityException) {
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
    }
  }

  fun sendTestNotification(context: Context) {
    NotificationChannels.ensureCreated(context)
    val openIntent = Intent(context, MainActivity::class.java).apply {
      data = Uri.parse("habitradar://today")
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPi = PendingIntent.getActivity(
      context,
      7777,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val builder = NotificationCompat.Builder(context, NotificationChannels.HABIT_REMINDERS)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Habit Radar · Gentle Nudge")
      .setContentText("Your reminders are working right on schedule.")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(contentPi)

    try {
      NotificationManagerCompat.from(context).notify(7777, builder.build())
    } catch (_: SecurityException) {
    }
  }
}

class ReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        NotificationChannels.ensureCreated(context)
        val container = context.appContainer
        val db = container.database
        val settings = container.settingsDataStore.getSettings()
        val today = LogicalDateCalculator.now(settings.dayRollover)

        when (intent.action) {
          ReminderScheduler.ACTION_HABIT_REMINDER -> {
            val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return@launch
            val habitId = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_ID) ?: return@launch
            val reminder = db.reminderDao().getReminderById(reminderId)?.toDomain() ?: return@launch
            val habit = db.habitDao().getHabitById(habitId)?.toDomain() ?: return@launch
            val pauses = db.pauseDao().getPausesForHabit(habitId).map { it.toDomain() }

            // Re-arm next occurrence immediately
            val nextTime = ReminderScheduler.computeNextOccurrence(reminder, pauses, LocalDateTime.now().plusMinutes(1))
            if (nextTime != null) {
              ReminderScheduler.scheduleAlarmForReminder(context, reminder.id, habit.id, nextTime)
            }

            if (!reminder.enabled || habit.status != HabitStatus.ACTIVE || ScheduleMatcher.isPausedOn(today, pauses)) {
              return@launch
            }

            val entry = db.entryDao().getEntry(habitId, today)?.toDomain()
            if (reminder.skipIfDone && ScheduleMatcher.isEntryDone(habit, entry)) {
              return@launch
            }

            val nowTime = LocalTime.now()
            if (settings.quietHoursEnabled &&
              ReminderScheduler.isWithinQuietHours(nowTime, settings.quietHoursFrom, settings.quietHoursTo)
            ) {
              if (settings.quietHoursMode == QuietHoursMode.MUTE) return@launch
              val delayedDt = LocalDateTime.of(LocalDate.now(), settings.quietHoursTo).let {
                if (it.isBefore(LocalDateTime.now())) it.plusDays(1) else it
              }
              ReminderScheduler.scheduleAlarmForReminder(context, reminder.id, habit.id, delayedDt)
              return@launch
            }

            val notifId = reminder.id.hashCode()
            val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
              data = Uri.parse("habitradar://habit/${habit.id}")
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPi = PendingIntent.getActivity(
              context,
              notifId,
              deepLinkIntent,
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val text = reminder.message?.takeIf { it.isNotBlank() } ?: "Time for ${habit.name}"
            val builder = NotificationCompat.Builder(context, NotificationChannels.HABIT_REMINDERS)
              .setSmallIcon(R.drawable.ic_launcher_foreground)
              .setContentTitle(habit.name)
              .setContentText(text)
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setAutoCancel(true)
              .setContentIntent(contentPi)

            if (reminder.quickActions && settings.notifQuickActions) {
              val donePi = buildActionPendingIntent(context, ReminderScheduler.ACTION_DONE, habit.id, reminder.id, notifId, 1)
              val snoozePi = buildActionPendingIntent(context, ReminderScheduler.ACTION_SNOOZE, habit.id, reminder.id, notifId, 2)
              val skipPi = buildActionPendingIntent(context, ReminderScheduler.ACTION_SKIP, habit.id, reminder.id, notifId, 3)
              builder.addAction(0, "Done", donePi)
              builder.addAction(0, "Snooze", snoozePi)
              builder.addAction(0, "Skip", skipPi)
            }

            try {
              NotificationManagerCompat.from(context).notify(notifId, builder.build())
            } catch (_: SecurityException) {
            }
          }
          ReminderScheduler.ACTION_MORNING_NUDGE, ReminderScheduler.ACTION_EVENING_NUDGE -> {
            val isEvening = intent.action == ReminderScheduler.ACTION_EVENING_NUDGE
            val habits = db.habitDao().getAllHabits().map { it.toDomain() }.filter { it.status == HabitStatus.ACTIVE }
            val entries = db.entryDao().getAllEntries().map { it.toDomain() }.filter { it.date == today }.associateBy { it.habitId }
            val pauses = db.pauseDao().getAllPauses().map { it.toDomain() }.groupBy { it.habitId }

            val scheduledToday = habits.filter { ScheduleMatcher.isScheduledOn(it, today, pauses[it.id].orEmpty()) }
            val pendingCount = scheduledToday.count { !ScheduleMatcher.isEntryDone(it, entries[it.id]) && entries[it.id]?.status != com.example.core.domain.model.EntryStatus.SKIPPED }

            if (scheduledToday.isNotEmpty() && (!isEvening || pendingCount > 0)) {
              val title = if (isEvening) "Evening save" else "Morning kickoff"
              val body = if (isEvening) {
                "$pendingCount small marks left — your 🔥 streaks are waiting."
              } else {
                "${scheduledToday.size} small marks today. Start with one."
              }
              val openIntent = Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("habitradar://today")
              }
              val pi = PendingIntent.getActivity(
                context,
                if (isEvening) 9002 else 9001,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
              )
              val builder = NotificationCompat.Builder(context, NotificationChannels.DAILY_NUDGES)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pi)
              try {
                NotificationManagerCompat.from(context).notify(if (isEvening) 9002 else 9001, builder.build())
              } catch (_: SecurityException) {
              }
            }
            ReminderScheduler.rescheduleAll(context)
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  private fun buildActionPendingIntent(
    context: Context,
    action: String,
    habitId: String,
    reminderId: String,
    notifId: Int,
    offset: Int
  ): PendingIntent {
    val intent = Intent(context, NotificationActionReceiver::class.java).apply {
      this.action = action
      putExtra(ReminderScheduler.EXTRA_HABIT_ID, habitId)
      putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
      putExtra(ReminderScheduler.EXTRA_NOTIF_ID, notifId)
    }
    return PendingIntent.getBroadcast(
      context,
      notifId + offset,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }
}

class NotificationActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val habitId = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_ID) ?: return@launch
        val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: ""
        val notifId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIF_ID, 0)

        val container = context.appContainer
        val settingsStore = container.settingsDataStore
        val repo = container.repository
        val today = repo.getLogicalToday()

        when (intent.action) {
          ReminderScheduler.ACTION_DONE -> {
            repo.toggleTaskEntry(habitId, today)
          }
          ReminderScheduler.ACTION_SKIP -> {
            repo.skipHabitOnDate(habitId, today)
          }
          ReminderScheduler.ACTION_SNOOZE -> {
            val snoozeMin = settingsStore.getSettings().snoozeMin.toLong()
            val snoozeAt = LocalDateTime.now().plusMinutes(snoozeMin)
            ReminderScheduler.scheduleAlarmForReminder(context, reminderId, habitId, snoozeAt)
          }
        }
        NotificationManagerCompat.from(context).cancel(notifId)
      } finally {
        pendingResult.finish()
      }
    }
  }
}

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        ReminderScheduler.rescheduleAll(context)
        WidgetUpdater.updateAll(context)
      } finally {
        pendingResult.finish()
      }
    }
  }
}

class DailyMaintenanceWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val container = applicationContext.appContainer
    val repo = container.repository
    repo.evaluateAllHabitLinkedSteps()
    ReminderScheduler.rescheduleAll(applicationContext)
    WidgetUpdater.updateAll(applicationContext)
    return Result.success()
  }

  companion object {
    fun enqueuePeriodic(context: Context) {
      runCatching {
        val req = PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
          "habit_radar_daily_maintenance",
          ExistingPeriodicWorkPolicy.KEEP,
          req
        )
      }
    }
  }
}
