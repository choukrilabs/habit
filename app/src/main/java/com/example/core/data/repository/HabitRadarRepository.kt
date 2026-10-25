package com.example.core.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.HabitRadarDatabase
import com.example.core.data.db.toDomain
import com.example.core.data.db.toEntity
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Goal
import com.example.core.domain.model.GoalStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitCompletionLog
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.MilestoneSummary
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.Step
import com.example.core.domain.model.StepType
import com.example.core.domain.model.WeekStartDay
import com.example.core.domain.model.WeeklyPerformanceSummary
import com.example.core.domain.model.WidgetConfig
import com.example.core.domain.usecase.GoalStepEvaluator
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.MilestoneEngine
import com.example.core.domain.usecase.WeeklyPerformanceCalculator
import com.example.core.data.firebase.HabitCloudSyncRepository
import com.example.core.notifications.ReminderScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class HabitRadarRepository(
  private val context: Context,
  val db: HabitRadarDatabase,
  val settingsDataStore: SettingsDataStore,
  val cloudSyncRepository: HabitCloudSyncRepository? = null,
  private val onDataChangedCallback: suspend () -> Unit = {}
) {
  val habitDao = db.habitDao()
  val entryDao = db.entryDao()
  val reminderDao = db.reminderDao()
  val pauseDao = db.pauseDao()
  val goalDao = db.goalDao()
  val widgetConfigDao = db.widgetConfigDao()

  val habitsFlow: Flow<List<Habit>> = habitDao.observeAllHabits().map { list -> list.map { it.toDomain() } }
  val entriesFlow: Flow<List<Entry>> = entryDao.observeAllEntries().map { list -> list.map { it.toDomain() } }
  val completionLogsFlow: Flow<List<HabitCompletionLog>> = entriesFlow.map { list -> list.map { it.toCompletionLog() } }
  val remindersFlow: Flow<List<Reminder>> = reminderDao.observeAllReminders().map { list -> list.map { it.toDomain() } }
  val pausesFlow: Flow<List<HabitPause>> = pauseDao.observeAllPauses().map { list -> list.map { it.toDomain() } }
  val goalsFlow: Flow<List<Goal>> = goalDao.observeAllGoals().map { list -> list.map { it.toDomain() } }
  val stepsFlow: Flow<List<Step>> = goalDao.observeAllSteps().map { list -> list.map { it.toDomain() } }
  val widgetConfigsFlow: Flow<List<WidgetConfig>> = widgetConfigDao.observeAllConfigs().map { list -> list.map { it.toDomain() } }

  val weeklyPerformanceSummaryFlow: Flow<WeeklyPerformanceSummary> = observeWeeklyPerformanceSummary()

  fun observeWeeklyPerformanceSummary(todayOverride: LocalDate? = null): Flow<WeeklyPerformanceSummary> {
    return combine(
      habitsFlow,
      entriesFlow,
      pausesFlow,
      settingsDataStore.settingsFlow
    ) { habits, entries, pauses, settings ->
      val logicalToday = todayOverride ?: LogicalDateCalculator.now(settings.dayRollover)
      WeeklyPerformanceCalculator.calculate(
        habits = habits,
        entries = entries,
        pauses = pauses,
        today = logicalToday,
        weekStartDay = settings.weekStart
      )
    }
  }

  suspend fun calculateWeeklyPerformanceSummary(
    today: LocalDate? = null,
    weekStartOverride: WeekStartDay? = null
  ): WeeklyPerformanceSummary {
    val settings = settingsDataStore.getSettings()
    val logicalToday = today ?: LogicalDateCalculator.now(settings.dayRollover)
    val weekStartDay = weekStartOverride ?: settings.weekStart
    val weekStartDate = LogicalDateCalculator.startOfWeek(logicalToday, weekStartDay)
    val prevWeekStart = weekStartDate.minusDays(7)
    val weekEndDate = weekStartDate.plusDays(6)

    val activeHabits = habitDao.getHabitsByStatus(HabitStatus.ACTIVE).map { it.toDomain() }
    val rangeEntries = entryDao.getEntriesInDateRange(prevWeekStart, weekEndDate).map { it.toDomain() }
    val pauses = pauseDao.getAllPauses().map { it.toDomain() }

    return WeeklyPerformanceCalculator.calculate(
      habits = activeHabits,
      entries = rangeEntries,
      pauses = pauses,
      today = logicalToday,
      weekStartDay = weekStartDay
    )
  }

  val milestoneSummaryFlow: Flow<MilestoneSummary> = observeMilestoneSummary()

  fun observeMilestoneSummary(todayOverride: LocalDate? = null): Flow<MilestoneSummary> {
    return combine(
      habitsFlow,
      entriesFlow,
      pausesFlow,
      settingsDataStore.settingsFlow
    ) { habits, entries, pauses, settings ->
      val logicalToday = todayOverride ?: LogicalDateCalculator.now(settings.dayRollover)
      MilestoneEngine.evaluateGlobalMilestones(
        habits = habits,
        entries = entries,
        pauses = pauses,
        today = logicalToday,
        weekStartDay = settings.weekStart
      )
    }
  }

  suspend fun calculateMilestoneSummary(today: LocalDate? = null): MilestoneSummary {
    val settings = settingsDataStore.getSettings()
    val logicalToday = today ?: LogicalDateCalculator.now(settings.dayRollover)
    val habits = habitDao.getAllHabits().map { it.toDomain() }
    val entries = entryDao.getAllEntries().map { it.toDomain() }
    val pauses = pauseDao.getAllPauses().map { it.toDomain() }
    return MilestoneEngine.evaluateGlobalMilestones(
      habits = habits,
      entries = entries,
      pauses = pauses,
      today = logicalToday,
      weekStartDay = settings.weekStart
    )
  }

  fun observeCompletionLogsForHabit(habitId: String): Flow<List<HabitCompletionLog>> {
    return entryDao.observeEntriesForHabit(habitId).map { list ->
      list.map { it.toDomain().toCompletionLog() }.sortedByDescending { it.date }
    }
  }

  suspend fun getCompletionLogsForHabit(habitId: String): List<HabitCompletionLog> {
    return entryDao.getEntriesForHabit(habitId)
      .map { it.toDomain().toCompletionLog() }
      .sortedByDescending { it.date }
  }

  suspend fun getLogicalToday(): LocalDate {
    val rollover = settingsDataStore.getSettings().dayRollover
    return LogicalDateCalculator.now(rollover)
  }

  suspend fun saveHabit(habit: Habit, reminders: List<Reminder>? = null) {
    db.withTransaction {
      habitDao.upsertHabit(habit.toEntity())
      if (reminders != null) {
        reminderDao.deleteRemindersForHabit(habit.id)
        reminderDao.upsertReminders(reminders.take(5).map { it.toEntity() })
      }
    }
    cloudSyncRepository?.syncDomainHabitIfSignedIn(habit)
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
  }

  suspend fun updateHabitSortOrders(orderedIds: List<String>) {
    val all = habitDao.getAllHabits().associateBy { it.id }
    val updated = orderedIds.mapIndexedNotNull { idx, id ->
      all[id]?.copy(sortOrder = idx)
    }
    habitDao.upsertHabits(updated)
    onDataChangedCallback()
  }

  suspend fun deleteHabit(habitId: String) {
    db.withTransaction {
      val linkedSteps = goalDao.getStepsForHabit(habitId)
      linkedSteps.forEach { step ->
        val updatedNote = listOfNotNull(step.note?.takeIf { it.isNotBlank() }, "Habit removed").joinToString(" · ")
        goalDao.upsertStep(
          step.copy(
            type = StepType.MANUAL,
            linkedHabitId = null,
            note = updatedNote
          )
        )
      }
      habitDao.deleteHabitById(habitId)
    }
    cloudSyncRepository?.deleteDomainHabitIfSignedIn(habitId)
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
  }

  suspend fun pauseHabit(habitId: String, startDate: LocalDate, endDate: LocalDate?) {
    val pause = HabitPause(
      id = UUID.randomUUID().toString(),
      habitId = habitId,
      startDate = startDate,
      endDate = endDate
    )
    pauseDao.upsertPause(pause.toEntity())
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
  }

  suspend fun resumeHabit(habitId: String) {
    val today = getLogicalToday()
    val pauses = pauseDao.getPausesForHabit(habitId)
    pauses.forEach { p ->
      if (p.endDate == null || !p.endDate.isBefore(today)) {
        if (p.startDate.isAfter(today.minusDays(1))) {
          pauseDao.deletePauseById(p.id)
        } else {
          pauseDao.upsertPause(p.copy(endDate = today.minusDays(1)))
        }
      }
    }
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
  }

  suspend fun archiveHabit(habitId: String) {
    val habit = habitDao.getHabitById(habitId) ?: return
    val updatedEntity = habit.copy(
      status = HabitStatus.ARCHIVED,
      archivedAt = Instant.now()
    )
    db.withTransaction {
      habitDao.upsertHabit(updatedEntity)
      val reminders = reminderDao.getRemindersForHabit(habitId)
      reminderDao.upsertReminders(reminders.map { it.copy(enabled = false) })
    }
    cloudSyncRepository?.syncDomainHabitIfSignedIn(updatedEntity.toDomain())
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
  }

  suspend fun restoreHabit(habitId: String) {
    val habit = habitDao.getHabitById(habitId) ?: return
    val today = getLogicalToday()
    val updatedEntity = habit.copy(
      status = HabitStatus.ACTIVE,
      archivedAt = null
    )
    db.withTransaction {
      val archivedDate = habit.archivedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()
      if (archivedDate != null && !archivedDate.isAfter(today.minusDays(1))) {
        pauseDao.upsertPause(
          HabitPause(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            startDate = archivedDate,
            endDate = today.minusDays(1)
          ).toEntity()
        )
      }
      habitDao.upsertHabit(updatedEntity)
      val reminders = reminderDao.getRemindersForHabit(habitId)
      reminderDao.upsertReminders(reminders.map { it.copy(enabled = true) })
    }
    cloudSyncRepository?.syncDomainHabitIfSignedIn(updatedEntity.toDomain())
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
  }

  suspend fun logHabitCompletion(log: HabitCompletionLog): HabitCompletionLog {
    val targetDate = log.resolvedDate
    val existing = entryDao.getEntry(log.habitId, targetDate)
    val persisted = log.copy(
      id = existing?.id ?: log.id,
      date = targetDate,
      dateStamp = targetDate.toString()
    )
    val entry = persisted.toEntry()
    entryDao.upsertEntry(entry.toEntity())
    cloudSyncRepository?.syncDomainEntryIfSignedIn(entry)
    evaluateHabitLinkedSteps(log.habitId)
    onDataChangedCallback()
    return persisted
  }

  suspend fun logHabitCompletion(
    habitId: String,
    date: LocalDate,
    status: EntryStatus = EntryStatus.DONE,
    value: Double? = null,
    note: String? = null,
    completedAt: Instant = Instant.now()
  ): HabitCompletionLog {
    val habit = habitDao.getHabitById(habitId)?.toDomain()
    val existing = entryDao.getEntry(habitId, date)
    val resolvedValue = when {
      status == EntryStatus.SKIPPED -> null
      value != null -> value
      habit?.trackType == com.example.core.domain.model.TrackType.TASK -> 1.0
      else -> habit?.targetValue ?: 1.0
    }
    val log = HabitCompletionLog(
      id = existing?.id ?: UUID.randomUUID().toString(),
      habitId = habitId,
      date = date,
      dateStamp = date.toString(),
      status = status,
      value = resolvedValue,
      note = note ?: existing?.note,
      completedAt = completedAt
    )
    return logHabitCompletion(log)
  }

  suspend fun markHabitDoneOnDate(
    habitId: String,
    date: LocalDate,
    note: String? = null
  ): Boolean {
    val habit = habitDao.getHabitById(habitId)?.toDomain() ?: return false
    logHabitCompletion(
      habitId = habit.id,
      date = date,
      status = EntryStatus.DONE,
      note = note
    )
    return true
  }

  suspend fun toggleHabitDoneOnDate(
    habitId: String,
    date: LocalDate,
    note: String? = null
  ): Boolean {
    val habit = habitDao.getHabitById(habitId)?.toDomain() ?: return false
    return if (habit.trackType == com.example.core.domain.model.TrackType.TASK) {
      toggleTaskEntry(habitId, date, note)
    } else {
      val existing = entryDao.getEntry(habitId, date)?.toDomain()
      val currentlyDone = com.example.core.domain.usecase.ScheduleMatcher.isEntryDone(habit, existing)
      val nextVal = if (currentlyDone) 0.0 else (habit.targetValue ?: 1.0)
      setQuantitativeEntry(habitId, date, nextVal, note ?: existing?.note)
      !currentlyDone
    }
  }

  suspend fun toggleTaskEntry(habitId: String, date: LocalDate, note: String? = null): Boolean {
    val existing = entryDao.getEntry(habitId, date)
    val nowDone: Boolean
    if (existing != null && existing.status == EntryStatus.DONE) {
      entryDao.deleteEntry(habitId, date)
      cloudSyncRepository?.deleteDomainEntryIfSignedIn(habitId, date.toString())
      nowDone = false
    } else {
      val entry = Entry(
        id = existing?.id ?: UUID.randomUUID().toString(),
        habitId = habitId,
        date = date,
        status = EntryStatus.DONE,
        value = 1.0,
        note = note ?: existing?.note,
        completedAt = Instant.now()
      )
      entryDao.upsertEntry(entry.toEntity())
      cloudSyncRepository?.syncDomainEntryIfSignedIn(entry)
      nowDone = true
    }
    evaluateHabitLinkedSteps(habitId)
    onDataChangedCallback()
    return nowDone
  }

  suspend fun setQuantitativeEntry(
    habitId: String,
    date: LocalDate,
    value: Double,
    note: String? = null
  ) {
    val existing = entryDao.getEntry(habitId, date)
    if (value <= 0.0 && note.isNullOrBlank()) {
      entryDao.deleteEntry(habitId, date)
      cloudSyncRepository?.deleteDomainEntryIfSignedIn(habitId, date.toString())
    } else {
      val entry = Entry(
        id = existing?.id ?: UUID.randomUUID().toString(),
        habitId = habitId,
        date = date,
        status = EntryStatus.DONE,
        value = value,
        note = note ?: existing?.note,
        completedAt = Instant.now()
      )
      entryDao.upsertEntry(entry.toEntity())
      cloudSyncRepository?.syncDomainEntryIfSignedIn(entry)
    }
    evaluateHabitLinkedSteps(habitId)
    onDataChangedCallback()
  }

  suspend fun skipHabitOnDate(habitId: String, date: LocalDate) {
    val existing = entryDao.getEntry(habitId, date)
    val entry = Entry(
      id = existing?.id ?: UUID.randomUUID().toString(),
      habitId = habitId,
      date = date,
      status = EntryStatus.SKIPPED,
      value = null,
      note = existing?.note,
      completedAt = Instant.now()
    )
    entryDao.upsertEntry(entry.toEntity())
    cloudSyncRepository?.syncDomainEntryIfSignedIn(entry)
    evaluateHabitLinkedSteps(habitId)
    onDataChangedCallback()
  }

  suspend fun deleteEntryOnDate(habitId: String, date: LocalDate) {
    entryDao.deleteEntry(habitId, date)
    cloudSyncRepository?.deleteDomainEntryIfSignedIn(habitId, date.toString())
    evaluateHabitLinkedSteps(habitId)
    onDataChangedCallback()
  }

  suspend fun saveReminder(reminder: Reminder) {
    reminderDao.upsertReminder(reminder.toEntity())
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
  }

  suspend fun scheduleDailyHabitReminder(
    habitId: String,
    time: LocalTime,
    daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    message: String? = null
  ): Reminder {
    val reminder = Reminder(
      id = UUID.randomUUID().toString(),
      habitId = habitId,
      time = time,
      daysOfWeek = daysOfWeek.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }.distinct().sorted(),
      message = message?.trim()?.ifBlank { null },
      enabled = true
    )
    reminderDao.upsertReminder(reminder.toEntity())
    ReminderScheduler.scheduleDailyHabitReminder(
      context = context,
      reminderId = reminder.id,
      habitId = reminder.habitId,
      time = reminder.time,
      daysOfWeek = reminder.daysOfWeek,
      message = reminder.message
    )
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
    return reminder
  }

  suspend fun deleteReminder(reminderId: String) {
    reminderDao.deleteReminderById(reminderId)
    ReminderScheduler.cancelHabitReminderWork(context, reminderId)
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
  }

  suspend fun setAllHabitRemindersEnabled(habitId: String, enabled: Boolean) {
    val list = reminderDao.getRemindersForHabit(habitId)
    reminderDao.upsertReminders(list.map { it.copy(enabled = enabled) })
    ReminderScheduler.rescheduleAll(context)
    onDataChangedCallback()
  }

  suspend fun saveGoalWithSteps(goal: Goal, steps: List<Step>) {
    db.withTransaction {
      goalDao.upsertGoal(goal.toEntity())
      val existingSteps = goalDao.getStepsForGoal(goal.id).map { it.id }.toSet()
      val newIds = steps.map { it.id }.toSet()
      (existingSteps - newIds).forEach { goalDao.deleteStepById(it) }
      goalDao.upsertSteps(steps.mapIndexed { idx, s -> s.copy(sortOrder = idx).toEntity() })
    }
    evaluateAllHabitLinkedSteps()
    onDataChangedCallback()
  }

  suspend fun saveStep(step: Step) {
    goalDao.upsertStep(step.toEntity())
    checkAndUpdateGoalCompletion(step.goalId)
    onDataChangedCallback()
  }

  suspend fun deleteStep(stepId: String, goalId: String) {
    goalDao.deleteStepById(stepId)
    checkAndUpdateGoalCompletion(goalId)
    onDataChangedCallback()
  }

  suspend fun toggleManualStep(step: Step): Boolean {
    val nowDone = !step.done
    val updated = step.copy(
      done = nowDone,
      doneAt = if (nowDone) Instant.now() else null
    )
    goalDao.upsertStep(updated.toEntity())
    val goalCompleted = checkAndUpdateGoalCompletion(step.goalId)
    onDataChangedCallback()
    return goalCompleted
  }

  suspend fun toggleStepDone(stepId: String, goalId: String): Boolean {
    val step = goalDao.getStepsForGoal(goalId).map { it.toDomain() }.firstOrNull { it.id == stepId } ?: return false
    return if (step.type == StepType.AMOUNT) {
      val nextVal = if (step.done) 0.0 else (step.amountTarget ?: 1.0)
      updateAmountStepValue(step, nextVal)
    } else {
      toggleManualStep(step)
    }
  }

  suspend fun updateAmountStepValue(step: Step, newValue: Double): Boolean {
    val target = step.amountTarget ?: 1.0
    val nowDone = newValue >= target
    val updated = step.copy(
      amountCurrent = newValue.coerceAtLeast(0.0),
      done = nowDone,
      doneAt = if (nowDone) (step.doneAt ?: Instant.now()) else null
    )
    goalDao.upsertStep(updated.toEntity())
    val goalCompleted = checkAndUpdateGoalCompletion(step.goalId)
    onDataChangedCallback()
    return goalCompleted
  }

  suspend fun skipStepForNow(step: Step) {
    val steps = goalDao.getStepsForGoal(step.goalId).sortedBy { it.sortOrder }.toMutableList()
    val idx = steps.indexOfFirst { it.id == step.id }
    if (idx != -1 && steps.size > 1) {
      val item = steps.removeAt(idx)
      steps.add(item)
      goalDao.upsertSteps(steps.mapIndexed { i, s -> s.copy(sortOrder = i) })
      onDataChangedCallback()
    }
  }

  suspend fun markGoalStatus(goalId: String, status: GoalStatus) {
    val goal = goalDao.getGoalById(goalId) ?: return
    goalDao.upsertGoal(
      goal.copy(
        status = status,
        completedAt = if (status == GoalStatus.COMPLETED) Instant.now() else null
      )
    )
    onDataChangedCallback()
  }

  suspend fun deleteGoal(goalId: String) {
    goalDao.deleteGoalById(goalId)
    onDataChangedCallback()
  }

  suspend fun evaluateHabitLinkedSteps(habitId: String) {
    val habit = habitDao.getHabitById(habitId)?.toDomain() ?: return
    if (habit.status == HabitStatus.ARCHIVED) return
    val entries = entryDao.getEntriesForHabit(habitId).map { it.toDomain() }
    val pauses = pauseDao.getPausesForHabit(habitId).map { it.toDomain() }
    val today = getLogicalToday()
    val linkedSteps = goalDao.getStepsForHabit(habitId).map { it.toDomain() }

    linkedSteps.forEach { step ->
      if (!step.done && step.type == StepType.HABIT) {
        val (ratio, _) = GoalStepEvaluator.evaluateHabitStepProgress(step, habit, entries, pauses, today)
        if (ratio >= 1.0f) {
          goalDao.upsertStep(
            step.copy(
              done = true,
              doneAt = Instant.now()
            ).toEntity()
          )
          checkAndUpdateGoalCompletion(step.goalId)
        }
      }
    }
  }

  suspend fun evaluateAllHabitLinkedSteps() {
    val habits = habitDao.getAllHabits().map { it.id }
    habits.forEach { evaluateHabitLinkedSteps(it) }
  }

  private suspend fun checkAndUpdateGoalCompletion(goalId: String): Boolean {
    val goal = goalDao.getGoalById(goalId) ?: return false
    val steps = goalDao.getStepsForGoal(goalId)
    if (steps.isNotEmpty() && steps.all { it.done } && goal.status != GoalStatus.COMPLETED) {
      goalDao.upsertGoal(
        goal.copy(
          status = GoalStatus.COMPLETED,
          completedAt = Instant.now()
        )
      )
      return true
    }
    return false
  }

  suspend fun saveWidgetConfig(config: WidgetConfig) {
    widgetConfigDao.upsertConfig(config.toEntity())
    onDataChangedCallback()
  }

  suspend fun deleteWidgetConfig(appWidgetId: Int) {
    widgetConfigDao.deleteConfig(appWidgetId)
  }
}
