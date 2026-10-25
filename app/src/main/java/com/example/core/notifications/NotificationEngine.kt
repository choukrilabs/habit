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
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.core.data.db.toDomain
import com.example.core.data.db.toEntity
import com.example.core.di.appContainer
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.QuietHoursMode
import com.example.core.domain.model.Reminder
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.feature.widgets.glance.WidgetUpdater
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
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
      ).apply { description = "Scheduled daily reminders for individual habits" },
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

  const val WORK_PREFIX_HABIT_REMINDER = "habit_reminder_work_"
  const val WORK_TAG_HABIT_REMINDERS = "tag_habit_reminders"
  const val WORK_NAME_MORNING_NUDGE = "habit_radar_morning_nudge_work"
  const val WORK_NAME_EVENING_NUDGE = "habit_radar_evening_nudge_work"

  const val KEY_REMINDER_ID = "reminder_id"
  const val KEY_HABIT_ID = "habit_id"
  const val KEY_NUDGE_TYPE = "nudge_type"
  const val NUDGE_TYPE_MORNING = "morning"
  const val NUDGE_TYPE_EVENING = "evening"

  fun uniqueWorkNameForReminder(reminderId: String): String =
    "$WORK_PREFIX_HABIT_REMINDER$reminderId"

  fun habitWorkTag(habitId: String): String = "habit_reminder_tag_$habitId"

  fun canScheduleExactAlarms(context: Context): Boolean {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      am.canScheduleExactAlarms()
    } else true
  }

  /**
   * Computes the initial delay in milliseconds between [now] and [triggerAt],
   * clamped to at least 1,000 ms for WorkManager execution.
   */
  fun computeInitialDelayMillis(
    triggerAt: LocalDateTime,
    now: LocalDateTime = LocalDateTime.now()
  ): Long {
    val diffMillis = Duration.between(now, triggerAt).toMillis()
    return diffMillis.coerceAtLeast(1_000L)
  }

  /**
   * Creates or updates a daily [Reminder] at the specified [time] in Room and immediately
   * schedules its execution via [WorkManager].
   */
  suspend fun scheduleDailyHabitReminder(
    context: Context,
    habitId: String,
    time: LocalTime,
    daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    message: String? = null,
    reminderId: String = UUID.randomUUID().toString()
  ): Reminder {
    val container = context.appContainer
    val db = container.database
    val reminder = Reminder(
      id = reminderId,
      habitId = habitId,
      time = time,
      daysOfWeek = daysOfWeek.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
      message = message,
      enabled = true
    )
    db.reminderDao().upsertReminder(reminder.toEntity())
    val pauses = db.pauseDao().getPausesForHabit(habitId).map { it.toDomain() }
    val now = LocalDateTime.now()
    val nextOccurrence = computeNextOccurrence(reminder, pauses, now)
    if (nextOccurrence != null) {
      enqueueHabitReminderWork(context, reminder.id, habitId, nextOccurrence, now)
      scheduleAlarmForReminder(context, reminder.id, habitId, nextOccurrence)
    }
    return reminder
  }

  /**
   * Reschedules all active habit reminders and daily nudges using both WorkManager
   * and AlarmManager fallback.
   */
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
          enqueueHabitReminderWork(context, reminder.id, habit.id, nextTime, now)
          scheduleAlarmForReminder(context, reminder.id, habit.id, nextTime)
        } else {
          cancelHabitReminderWork(context, reminder.id)
          cancelAlarmForReminder(context, reminder.id)
        }
      } else {
        cancelHabitReminderWork(context, reminder.id)
        cancelAlarmForReminder(context, reminder.id)
      }
    }

    if (settings.dailyNudges && settings.morningNudgeEnabled) {
      enqueueDailyNudgeWork(context, NUDGE_TYPE_MORNING, settings.morningNudgeTime, now)
      scheduleDailyNudge(context, ACTION_MORNING_NUDGE, 9001, settings.morningNudgeTime, now)
    } else {
      cancelDailyNudgeWork(context, NUDGE_TYPE_MORNING)
    }

    if (settings.dailyNudges && settings.eveningNudgeEnabled) {
      enqueueDailyNudgeWork(context, NUDGE_TYPE_EVENING, settings.eveningNudgeTime, now)
      scheduleDailyNudge(context, ACTION_EVENING_NUDGE, 9002, settings.eveningNudgeTime, now)
    } else {
      cancelDailyNudgeWork(context, NUDGE_TYPE_EVENING)
    }
  }

  fun computeNextOccurrence(
    reminder: Reminder,
    pauses: List<HabitPause>,
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

  /**
   * Enqueues a WorkManager [OneTimeWorkRequest] for a specific habit reminder occurrence.
   */
  fun enqueueHabitReminderWork(
    context: Context,
    reminderId: String,
    habitId: String,
    triggerAt: LocalDateTime,
    now: LocalDateTime = LocalDateTime.now()
  ) {
    runCatching {
      val delayMillis = computeInitialDelayMillis(triggerAt, now)
      val inputData: Data = workDataOf(
        KEY_REMINDER_ID to reminderId,
        KEY_HABIT_ID to habitId
      )
      val workRequest = OneTimeWorkRequestBuilder<HabitReminderWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(inputData)
        .addTag(WORK_TAG_HABIT_REMINDERS)
        .addTag(habitWorkTag(habitId))
        .build()

      WorkManager.getInstance(context).enqueueUniqueWork(
        uniqueWorkNameForReminder(reminderId),
        ExistingWorkPolicy.REPLACE,
        workRequest
      )
    }
  }

  fun cancelHabitReminderWork(context: Context, reminderId: String) {
    runCatching {
      WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkNameForReminder(reminderId))
    }
  }

  fun cancelAllRemindersForHabit(context: Context, habitId: String) {
    runCatching {
      WorkManager.getInstance(context).cancelAllWorkByTag(habitWorkTag(habitId))
    }
  }

  fun enqueueDailyNudgeWork(
    context: Context,
    nudgeType: String,
    time: LocalTime,
    now: LocalDateTime = LocalDateTime.now()
  ) {
    runCatching {
      var targetDt = LocalDateTime.of(now.toLocalDate(), time)
      if (!targetDt.isAfter(now.plusSeconds(5))) {
        targetDt = targetDt.plusDays(1)
      }
      val delayMillis = computeInitialDelayMillis(targetDt, now)
      val workName = if (nudgeType == NUDGE_TYPE_EVENING) WORK_NAME_EVENING_NUDGE else WORK_NAME_MORNING_NUDGE
      val req = OneTimeWorkRequestBuilder<DailyNudgeWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(workDataOf(KEY_NUDGE_TYPE to nudgeType))
        .addTag(WORK_TAG_HABIT_REMINDERS)
        .build()

      WorkManager.getInstance(context).enqueueUniqueWork(
        workName,
        ExistingWorkPolicy.REPLACE,
        req
      )
    }
  }

  fun cancelDailyNudgeWork(context: Context, nudgeType: String) {
    runCatching {
      val workName = if (nudgeType == NUDGE_TYPE_EVENING) WORK_NAME_EVENING_NUDGE else WORK_NAME_MORNING_NUDGE
      WorkManager.getInstance(context).cancelUniqueWork(workName)
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

  internal suspend fun processAndDispatchHabitReminder(
    context: Context,
    reminderId: String,
    habitId: String
  ): Boolean {
    NotificationChannels.ensureCreated(context)
    val container = context.appContainer
    val db = container.database
    val settings = container.settingsDataStore.getSettings()
    val today = LogicalDateCalculator.now(settings.dayRollover)

    val reminder = db.reminderDao().getReminderById(reminderId)?.toDomain() ?: return false
    val habit = db.habitDao().getHabitById(habitId)?.toDomain() ?: return false
    val pauses = db.pauseDao().getPausesForHabit(habitId).map { it.toDomain() }

    // Re-arm next daily occurrence immediately in WorkManager and AlarmManager
    val nextTime = computeNextOccurrence(reminder, pauses, LocalDateTime.now().plusMinutes(1))
    if (nextTime != null) {
      enqueueHabitReminderWork(context, reminder.id, habit.id, nextTime)
      scheduleAlarmForReminder(context, reminder.id, habit.id, nextTime)
    }

    if (!reminder.enabled || habit.status != HabitStatus.ACTIVE || ScheduleMatcher.isPausedOn(today, pauses)) {
      return false
    }

    val entry = db.entryDao().getEntry(habitId, today)?.toDomain()
    if (reminder.skipIfDone && ScheduleMatcher.isEntryDone(habit, entry)) {
      return false
    }

    val nowTime = LocalTime.now()
    if (settings.quietHoursEnabled &&
      isWithinQuietHours(nowTime, settings.quietHoursFrom, settings.quietHoursTo)
    ) {
      if (settings.quietHoursMode == QuietHoursMode.MUTE) return false
      val delayedDt = LocalDateTime.of(LocalDate.now(), settings.quietHoursTo).let {
        if (it.isBefore(LocalDateTime.now())) it.plusDays(1) else it
      }
      enqueueHabitReminderWork(context, reminder.id, habit.id, delayedDt)
      scheduleAlarmForReminder(context, reminder.id, habit.id, delayedDt)
      return false
    }

    postHabitReminderNotification(context, habit, reminder, settings.notifQuickActions)
    return true
  }

  internal fun postHabitReminderNotification(
    context: Context,
    habit: Habit,
    reminder: Reminder,
    globalQuickActionsEnabled: Boolean
  ) {
    NotificationChannels.ensureCreated(context)
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

    if (reminder.quickActions && globalQuickActionsEnabled) {
      val donePi = buildActionPendingIntent(context, ACTION_DONE, habit.id, reminder.id, notifId, 1)
      val snoozePi = buildActionPendingIntent(context, ACTION_SNOOZE, habit.id, reminder.id, notifId, 2)
      val skipPi = buildActionPendingIntent(context, ACTION_SKIP, habit.id, reminder.id, notifId, 3)
      builder.addAction(0, "Done", donePi)
      builder.addAction(0, "Snooze", snoozePi)
      builder.addAction(0, "Skip", skipPi)
    }

    try {
      NotificationManagerCompat.from(context).notify(notifId, builder.build())
    } catch (_: SecurityException) {
    }
  }

  internal suspend fun processAndDispatchDailyNudge(
    context: Context,
    isEvening: Boolean
  ): Boolean {
    NotificationChannels.ensureCreated(context)
    val container = context.appContainer
    val db = container.database
    val settings = container.settingsDataStore.getSettings()
    val today = LogicalDateCalculator.now(settings.dayRollover)

    val habits = db.habitDao().getAllHabits().map { it.toDomain() }.filter { it.status == HabitStatus.ACTIVE }
    val entries = db.entryDao().getAllEntries().map { it.toDomain() }.filter { it.date == today }.associateBy { it.habitId }
    val pauses = db.pauseDao().getAllPauses().map { it.toDomain() }.groupBy { it.habitId }

    val scheduledToday = habits.filter { ScheduleMatcher.isScheduledOn(it, today, pauses[it.id].orEmpty()) }
    val pendingCount = scheduledToday.count {
      !ScheduleMatcher.isEntryDone(it, entries[it.id]) &&
        entries[it.id]?.status != com.example.core.domain.model.EntryStatus.SKIPPED
    }

    var notified = false
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
        notified = true
      } catch (_: SecurityException) {
      }
    }
    rescheduleAll(context)
    return notified
  }

  internal fun buildActionPendingIntent(
    context: Context,
    action: String,
    habitId: String,
    reminderId: String,
    notifId: Int,
    offset: Int
  ): PendingIntent {
    val intent = Intent(context, NotificationActionReceiver::class.java).apply {
      this.action = action
      putExtra(EXTRA_HABIT_ID, habitId)
      putExtra(EXTRA_REMINDER_ID, reminderId)
      putExtra(EXTRA_NOTIF_ID, notifId)
    }
    return PendingIntent.getBroadcast(
      context,
      notifId + offset,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
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
      .setContentText("Your WorkManager reminders are working right on schedule.")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(contentPi)

    try {
      NotificationManagerCompat.from(context).notify(7777, builder.build())
    } catch (_: SecurityException) {
    }
  }
}

/**
 * WorkManager [CoroutineWorker] that fires a scheduled daily habit reminder notification
 * at the user's chosen time and automatically chains the next day's reminder occurrence.
 */
class HabitReminderWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val reminderId = inputData.getString(ReminderScheduler.KEY_REMINDER_ID)
      ?: return Result.failure()
    val habitId = inputData.getString(ReminderScheduler.KEY_HABIT_ID)
      ?: return Result.failure()

    ReminderScheduler.processAndDispatchHabitReminder(
      context = applicationContext,
      reminderId = reminderId,
      habitId = habitId
    )
    return Result.success()
  }
}

/**
 * WorkManager [CoroutineWorker] that fires the Morning Kickoff or Evening Save daily nudge
 * and re-enqueues the next day's nudge.
 */
class DailyNudgeWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val nudgeType = inputData.getString(ReminderScheduler.KEY_NUDGE_TYPE)
      ?: ReminderScheduler.NUDGE_TYPE_MORNING
    val isEvening = nudgeType == ReminderScheduler.NUDGE_TYPE_EVENING
    ReminderScheduler.processAndDispatchDailyNudge(applicationContext, isEvening)
    return Result.success()
  }
}

class ReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        when (intent.action) {
          ReminderScheduler.ACTION_HABIT_REMINDER -> {
            val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return@launch
            val habitId = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_ID) ?: return@launch
            ReminderScheduler.processAndDispatchHabitReminder(context, reminderId, habitId)
          }
          ReminderScheduler.ACTION_MORNING_NUDGE, ReminderScheduler.ACTION_EVENING_NUDGE -> {
            val isEvening = intent.action == ReminderScheduler.ACTION_EVENING_NUDGE
            ReminderScheduler.processAndDispatchDailyNudge(context, isEvening)
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
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
            repo.markHabitDoneOnDate(habitId, today)
          }
          ReminderScheduler.ACTION_SKIP -> {
            repo.skipHabitOnDate(habitId, today)
          }
          ReminderScheduler.ACTION_SNOOZE -> {
            val snoozeMin = settingsStore.getSettings().snoozeMin.toLong()
            val snoozeAt = LocalDateTime.now().plusMinutes(snoozeMin)
            ReminderScheduler.enqueueHabitReminderWork(context, reminderId, habitId, snoozeAt)
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
