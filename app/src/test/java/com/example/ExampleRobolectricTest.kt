package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.EntryEntity
import com.example.core.data.db.GoalEntity
import com.example.core.data.db.HabitPauseEntity
import com.example.core.data.db.HabitRadarDatabase
import com.example.core.data.db.ReminderEntity
import com.example.core.data.db.StepEntity
import com.example.core.data.db.toDomain
import com.example.core.data.db.toEntity
import com.example.core.data.repository.HabitRadarRepository
import com.example.core.di.AppContainer
import com.example.core.di.DefaultAppContainer
import com.example.core.di.appContainer
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.GoalStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.HabitStepMetric
import com.example.core.domain.model.ReminderSound
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.StepType
import com.example.core.domain.model.TrackType
import com.example.core.ui.viewmodel.BackupRestoreViewModel
import com.example.core.ui.viewmodel.HabitDetailViewModel
import com.example.core.ui.viewmodel.HabitRadarViewModelFactory
import com.example.core.ui.viewmodel.MainAppViewModel
import com.example.core.ui.viewmodel.RecapViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var database: HabitRadarDatabase
  private lateinit var container: AppContainer
  private lateinit var repository: HabitRadarRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    database = Room.inMemoryDatabaseBuilder(context, HabitRadarDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    container = DefaultAppContainer(
      context = context,
      customDatabase = database,
      customSettingsDataStore = SettingsDataStore(context)
    )
    (context as? HabitRadarApplication)?.container = container
    repository = container.repository
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context and resolve AppContainer from Application`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("Habit Radar", appName)
    assertSame(container, context.appContainer)
    assertSame(database, context.appContainer.database)
    assertSame(database, context.appContainer.appDatabase)
    assertSame(repository, context.appContainer.repository)
    assertSame(repository, context.appContainer.habitRepository)
    assertNotNull(context.appContainer.authService)
    assertSame(context.appContainer.authService, context.appContainer.authenticationService)
  }

  @Test
  fun `manual DI ViewModelFactory creates all ViewModels and MainAppViewModel mutates state`() = runBlocking {
    val factory = HabitRadarViewModelFactory(container)
    val mainVm = factory.create(MainAppViewModel::class.java)
    val detailVm = factory.create(HabitDetailViewModel::class.java)
    val recapVm = factory.create(RecapViewModel::class.java)
    val backupVm = factory.create(BackupRestoreViewModel::class.java)

    assertSame(repository, mainVm.repository)
    assertSame(repository, detailVm.repository)
    assertSame(repository, recapVm.repository)
    assertSame(database, backupVm.database)

    val startDate = LocalDate.of(2026, 5, 1)
    val habit = Habit(
      id = "h_test_1",
      name = "Morning Meditation",
      iconKey = "meditation",
      colorKey = "#D6BEFA",
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      trackType = TrackType.TASK,
      startDate = startDate
    )
    repository.saveHabit(habit, emptyList())

    val habits = repository.habitsFlow.first()
    assertEquals(1, habits.size)
    assertEquals("Morning Meditation", habits.first().name)

    val done = repository.toggleTaskEntry(habit.id, startDate, "Felt calm")
    assertTrue(done)
    val entries = repository.entriesFlow.first()
    assertEquals(1, entries.size)
    assertEquals("Felt calm", entries.first().note)

    val analytics = detailVm.computeAnalytics(
      habit = habit,
      entries = entries,
      pauses = emptyList(),
      reminders = emptyList(),
      settings = AppSettings()
    )
    assertEquals(1, analytics.totalCheckIns)

    repository.archiveHabit(habit.id)
    val archived = repository.habitsFlow.first().first { it.id == habit.id }
    assertEquals(HabitStatus.ARCHIVED, archived.status)
    assertNotNull(archived.archivedAt)

    repository.restoreHabit(habit.id)
    val restored = repository.habitsFlow.first().first { it.id == habit.id }
    assertEquals(HabitStatus.ACTIVE, restored.status)
    assertNull(restored.archivedAt)
  }

  @Test
  fun `room dao enforces unique entry index relational queries cascade delete and set null`() = runBlocking {
    val habitDao = database.habitDao()
    val entryDao = database.entryDao()
    val reminderDao = database.reminderDao()
    val pauseDao = database.pauseDao()
    val goalDao = database.goalDao()

    val d1 = LocalDate.of(2026, 6, 1)
    val d2 = LocalDate.of(2026, 6, 2)

    val habit = Habit(
      id = "h_schema",
      name = "Hydrate 8 glasses",
      iconKey = "water",
      colorKey = "#8FBDF0",
      trackType = TrackType.AMOUNT,
      targetValue = 8.0,
      unit = "glasses",
      startDate = d1
    )
    habitDao.upsertHabit(habit.toEntity())

    // Insert two entries on the same (habitId, date) — unique index should replace the first
    entryDao.upsertEntry(
      EntryEntity(
        id = "e_1",
        habitId = "h_schema",
        date = d1,
        status = EntryStatus.DONE,
        value = 4.0,
        note = "First",
        completedAt = Instant.now()
      )
    )
    entryDao.upsertEntry(
      EntryEntity(
        id = "e_2",
        habitId = "h_schema",
        date = d1,
        status = EntryStatus.DONE,
        value = 8.0,
        note = "Updated",
        completedAt = Instant.now()
      )
    )
    entryDao.upsertEntry(
      EntryEntity(
        id = "e_3",
        habitId = "h_schema",
        date = d2,
        status = EntryStatus.SKIPPED,
        value = null,
        note = null,
        completedAt = Instant.now()
      )
    )

    val rangeEntries = entryDao.getEntriesInDateRange(d1, d2)
    assertEquals(2, rangeEntries.size)
    assertEquals("Updated", entryDao.getEntry("h_schema", d1)?.note)

    reminderDao.upsertReminder(
      ReminderEntity(
        id = "r_1",
        habitId = "h_schema",
        time = LocalTime.of(9, 0),
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        message = "Drink water",
        skipIfDone = true,
        repeatAfterMin = null,
        sound = ReminderSound.DEFAULT,
        vibrate = true,
        quickActions = true,
        enabled = true
      )
    )
    pauseDao.upsertPause(
      HabitPauseEntity(
        id = "p_1",
        habitId = "h_schema",
        startDate = d2.plusDays(1),
        endDate = d2.plusDays(2)
      )
    )

    // Verify @Relation projection HabitWithLogsAndDetails
    val withDetails = habitDao.getHabitWithDetails("h_schema")
    assertNotNull(withDetails)
    assertEquals(2, withDetails!!.entries.size)
    assertEquals(1, withDetails.reminders.size)
    assertEquals(1, withDetails.pauses.size)

    // Create a Goal + Habit-linked Step referencing h_schema
    goalDao.upsertGoal(
      GoalEntity(
        id = "g_1",
        title = "Summer Hydration",
        description = null,
        iconKey = "flag",
        colorKey = "#8FBDF0",
        startDate = d1,
        targetDate = d1.plusDays(30),
        status = GoalStatus.ACTIVE,
        completedAt = null,
        sortOrder = 0,
        remindWeekly = false,
        weeklyDay = 7,
        weeklyTime = LocalTime.of(18, 0),
        remindBeforeDays = 3,
        beforeTime = LocalTime.of(9, 0),
        remindStepDue = false
      )
    )
    goalDao.upsertStep(
      StepEntity(
        id = "s_1",
        goalId = "g_1",
        title = "14-day hydration streak",
        note = null,
        dueDate = null,
        sortOrder = 0,
        type = StepType.HABIT,
        linkedHabitId = "h_schema",
        habitMetric = HabitStepMetric.STREAK,
        habitTarget = 14,
        habitWindowDays = null,
        amountTarget = null,
        amountCurrent = null,
        amountUnit = null,
        done = false,
        doneAt = null
      )
    )

    val goalWithSteps = goalDao.getGoalWithSteps("g_1")
    assertNotNull(goalWithSteps)
    assertEquals(1, goalWithSteps!!.steps.size)

    // Delete Habit -> cascades to entries, reminders, pauses; sets Step.linkedHabitId to NULL
    habitDao.deleteHabitById("h_schema")
    assertEquals(0, entryDao.getAllEntries().size)
    assertEquals(0, reminderDao.getAllReminders().size)
    assertEquals(0, pauseDao.getAllPauses().size)

    val stepAfterHabitDelete = goalDao.getStepsForGoal("g_1").first()
    assertNull(stepAfterHabitDelete.linkedHabitId)
    // Domain mapper automatically converts orphaned habit step to StepType.MANUAL
    assertEquals(StepType.MANUAL, stepAfterHabitDelete.toDomain().type)

    // Delete Goal -> cascades to steps
    goalDao.deleteGoalById("g_1")
    assertEquals(0, goalDao.getAllSteps().size)
  }

  @Test
  fun `guest mode bypasses sign in gate backup requires sign in and widgets mark all habit types done`() = runBlocking {
    val authService = container.authService
    authService.requireSignInPrompt()
    org.junit.Assert.assertFalse(authService.isSkippedSignIn)

    // User chooses Continue without signing in
    authService.continueWithoutSignIn()
    assertTrue(authService.isSkippedSignIn)
    assertTrue(authService.skippedSignInFlow.value)

    // Cloud backup fails until user signs in
    val syncResult = container.cloudSyncRepository.syncAllToCloud(emptyList(), emptyList())
    assertTrue(syncResult.isFailure)

    // Widget completion works for both TASK and AMOUNT habits
    val today = LocalDate.of(2026, 6, 15)
    val taskHabit = Habit(
      id = "h_widget_task",
      name = "Make bed",
      iconKey = "check",
      colorKey = "#A8DFB4",
      trackType = TrackType.TASK,
      startDate = today
    )
    val amountHabit = Habit(
      id = "h_widget_amount",
      name = "Drink 8 glasses",
      iconKey = "water",
      colorKey = "#8FBDF0",
      trackType = TrackType.AMOUNT,
      targetValue = 8.0,
      unit = "glasses",
      startDate = today
    )
    repository.saveHabit(taskHabit, emptyList())
    repository.saveHabit(amountHabit, emptyList())

    assertTrue(repository.toggleHabitDoneOnDate("h_widget_task", today))
    assertTrue(repository.toggleHabitDoneOnDate("h_widget_amount", today))

    val taskEntry = database.entryDao().getEntry("h_widget_task", today)
    val amountEntry = database.entryDao().getEntry("h_widget_amount", today)
    assertNotNull(taskEntry)
    assertEquals(EntryStatus.DONE, taskEntry?.status)
    assertNotNull(amountEntry)
    assertEquals(8.0, amountEntry?.value ?: 0.0, 0.01)

    // Toggling again from widget unmarks them
    org.junit.Assert.assertFalse(repository.toggleHabitDoneOnDate("h_widget_task", today))
    org.junit.Assert.assertFalse(repository.toggleHabitDoneOnDate("h_widget_amount", today))
  }

  @Test
  fun `glance app widget receivers handle mark done and toggle button interactions updating Room instantly`() = runBlocking {
    val today = LocalDate.of(2026, 6, 20)
    val taskHabit = Habit(
      id = "h_glance_rx_task",
      name = "Evening Stretch",
      iconKey = "fitness",
      colorKey = "#D6BEFA",
      trackType = TrackType.TASK,
      startDate = today
    )
    val amountHabit = Habit(
      id = "h_glance_rx_amount",
      name = "Read 20 pages",
      iconKey = "book",
      colorKey = "#F5B895",
      trackType = TrackType.AMOUNT,
      targetValue = 20.0,
      unit = "pages",
      startDate = today
    )
    repository.saveHabit(taskHabit, emptyList())
    repository.saveHabit(amountHabit, emptyList())

    // 1. TodayListWidgetReceiver handles ACTION_MARK_HABIT_DONE and updates Room immediately
    val todayListReceiver = com.example.feature.widgets.glance.TodayListWidgetReceiver()
    val markTaskIntent = com.example.feature.widgets.glance.HabitRadarGlanceAppWidgetReceiver.createMarkHabitDoneIntent(
      context = context,
      receiverClass = com.example.feature.widgets.glance.TodayListWidgetReceiver::class.java,
      habitId = taskHabit.id,
      date = today
    )
    todayListReceiver.onReceive(context, markTaskIntent)

    val taskEntryAfterMark = database.entryDao().getEntry(taskHabit.id, today)
    assertNotNull(taskEntryAfterMark)
    assertEquals(EntryStatus.DONE, taskEntryAfterMark?.status)
    assertEquals(1.0, taskEntryAfterMark?.value ?: 0.0, 0.01)

    // 2. ProgressRingWidgetReceiver handles ACTION_MARK_HABIT_DONE for quantitative habits
    val progressRingReceiver = com.example.feature.widgets.glance.ProgressRingWidgetReceiver()
    val markAmountIntent = com.example.feature.widgets.glance.HabitRadarGlanceAppWidgetReceiver.createMarkHabitDoneIntent(
      context = context,
      receiverClass = com.example.feature.widgets.glance.ProgressRingWidgetReceiver::class.java,
      habitId = amountHabit.id,
      date = today
    )
    progressRingReceiver.onReceive(context, markAmountIntent)

    val amountEntryAfterMark = database.entryDao().getEntry(amountHabit.id, today)
    assertNotNull(amountEntryAfterMark)
    assertEquals(EntryStatus.DONE, amountEntryAfterMark?.status)
    assertEquals(20.0, amountEntryAfterMark?.value ?: 0.0, 0.01)

    // 3. WeekGridWidgetReceiver handles ACTION_TOGGLE_HABIT_DONE to unmark an already-done habit
    val weekGridReceiver = com.example.feature.widgets.glance.WeekGridWidgetReceiver()
    val toggleTaskIntent = com.example.feature.widgets.glance.HabitRadarGlanceAppWidgetReceiver.createToggleHabitDoneIntent(
      context = context,
      receiverClass = com.example.feature.widgets.glance.WeekGridWidgetReceiver::class.java,
      habitId = taskHabit.id,
      date = today
    )
    weekGridReceiver.onReceive(context, toggleTaskIntent)

    val taskEntryAfterToggle = database.entryDao().getEntry(taskHabit.id, today)
    assertNull(taskEntryAfterToggle)
  }

  @Test
  fun `habitCreationViewModel validates name configures frequencies and saves offline-first to Room without sign-in`() = runBlocking {
    // Ensure user is in offline/guest mode (no cloud sign-in)
    container.authService.continueWithoutSignIn()
    assertNull(container.authService.currentUser)

    val factory = HabitRadarViewModelFactory(container)
    val creationVm = factory.create(com.example.feature.habit.HabitCreationViewModel::class.java)
    val today = LocalDate.of(2026, 7, 1)
    creationVm.resetForm(today = today)

    // 1. Attempting to save with blank name fails validation and sets errorMessage
    val emptySaveResult = creationVm.saveHabitOfflineFirst()
    assertTrue(emptySaveResult.isFailure)
    assertNotNull(creationVm.uiState.value.errorMessage)

    // 2. Set habit name and configure Weekly frequency (4 times per week)
    creationVm.updateName("Strength Training")
    creationVm.updateDescription("Upper & lower body split")
    creationVm.selectFrequencyPreset(com.example.feature.habit.FrequencyPreset.THREE_TIMES_A_WEEK)
    assertEquals(RepeatType.WEEKLY, creationVm.uiState.value.repeatType)
    assertEquals(3, creationVm.uiState.value.timesPerPeriod)

    creationVm.incrementTimesPerWeek()
    assertEquals(4, creationVm.uiState.value.timesPerPeriod)
    assertEquals("4 times per week", creationVm.uiState.value.frequencySummary)

    // Save offline-first to local Room DB
    val weeklySaveResult = creationVm.saveHabitOfflineFirst()
    assertTrue(weeklySaveResult.isSuccess)
    val savedWeeklyHabit = weeklySaveResult.getOrThrow()
    assertEquals("Strength Training", savedWeeklyHabit.name)
    assertEquals(RepeatType.WEEKLY, savedWeeklyHabit.repeatType)
    assertEquals(4, savedWeeklyHabit.timesPerPeriod)

    val persistedWeekly = database.habitDao().getHabitById(savedWeeklyHabit.id)?.toDomain()
    assertNotNull(persistedWeekly)
    assertEquals("Strength Training", persistedWeekly?.name)
    assertEquals(RepeatType.WEEKLY, persistedWeekly?.repeatType)
    assertEquals(4, persistedWeekly?.timesPerPeriod)

    // 3. Reset form and create a Daily Weekdays habit with a reminder offline-first
    creationVm.resetForm(today = today)
    creationVm.updateName("Deep Work Session")
    creationVm.selectFrequencyPreset(com.example.feature.habit.FrequencyPreset.WEEKDAYS)
    assertEquals(RepeatType.DAILY, creationVm.uiState.value.repeatType)
    assertEquals(listOf(1, 2, 3, 4, 5), creationVm.uiState.value.daysOfWeek)
    assertEquals("Weekdays (Mon–Fri)", creationVm.uiState.value.frequencySummary)

    // Toggle Friday off -> Mon..Thu
    creationVm.toggleDayOfWeek(5)
    assertEquals(listOf(1, 2, 3, 4), creationVm.uiState.value.daysOfWeek)

    // Enable reminder master toggle
    creationVm.setRemindMeMaster(true)
    assertTrue(creationVm.uiState.value.remindMeMaster)
    assertEquals(1, creationVm.uiState.value.reminders.size)

    val dailySaveResult = creationVm.saveHabitOfflineFirst()
    assertTrue(dailySaveResult.isSuccess)
    val savedDailyHabit = dailySaveResult.getOrThrow()

    val allHabitsInRoom = repository.habitsFlow.first()
    assertEquals(2, allHabitsInRoom.size)
    assertEquals(1, savedDailyHabit.sortOrder)
    val remindersInRoom = database.reminderDao().getRemindersForHabit(savedDailyHabit.id)
    assertEquals(1, remindersInRoom.size)
    assertTrue(remindersInRoom.first().enabled)
  }

  @Test
  fun `habitCompletionLog persists date-stamped completions and drives streak and analytics calculations`() = runBlocking {
    val today = LocalDate.of(2026, 8, 10)
    val dMinus2 = today.minusDays(2) // 2026-08-08
    val dMinus1 = today.minusDays(1) // 2026-08-09

    val habit = Habit(
      id = "h_completion_log",
      name = "Journal 15 min",
      iconKey = "edit",
      colorKey = "#E8A8F5",
      trackType = TrackType.TIME,
      targetValue = 15.0,
      unit = "min",
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      startDate = dMinus2
    )
    repository.saveHabit(habit, emptyList())

    // 1. Log completion using ISO-8601 dateStamp factory for 2 days ago
    val logDay1 = com.example.core.domain.model.HabitCompletionLog.fromDateStamp(
      habitId = habit.id,
      dateStamp = "2026-08-08",
      value = 15.0,
      note = "Day 1 reflection"
    )
    assertEquals(dMinus2, logDay1.resolvedDate)
    assertEquals("2026-08-08", logDay1.dateStamp)
    assertTrue(logDay1.isCompleted)
    repository.logHabitCompletion(logDay1)

    // 2. Log completion for yesterday and today with explicit LocalDate + dateStamp
    repository.logHabitCompletion(
      habitId = habit.id,
      date = dMinus1,
      status = EntryStatus.DONE,
      value = 20.0,
      note = "Day 2 reflection"
    )
    val persistedTodayLog = repository.logHabitCompletion(
      habitId = habit.id,
      date = today,
      status = EntryStatus.DONE,
      value = 15.0,
      note = "Day 3 reflection"
    )
    assertEquals("2026-08-10", persistedTodayLog.dateStamp)

    // 3. Verify completionLogsFlow and getCompletionLogsForHabit return all 3 date-stamped logs
    val logs = repository.getCompletionLogsForHabit(habit.id)
    assertEquals(3, logs.size)
    assertEquals(listOf("2026-08-10", "2026-08-09", "2026-08-08"), logs.map { it.dateStamp })

    // 4. Verify date-stamped logs feed into StreakCalculator and AnalyticsCalculator accurately
    val entries = logs.map { it.toEntry() }
    val streakResult = com.example.core.domain.usecase.StreakCalculator.calculateStreaks(
      habit = habit,
      entries = entries,
      pauses = emptyList(),
      today = today
    )
    assertEquals(3, streakResult.currentStreak)
    assertEquals(3, streakResult.bestStreak)

    val analytics = com.example.core.domain.usecase.AnalyticsCalculator.calculateAnalytics(
      habit = habit,
      entries = entries,
      pauses = emptyList(),
      firstReminderMinutesOfDay = null,
      period = com.example.core.domain.usecase.AnalyticsPeriod.D30,
      weekStart = com.example.core.domain.model.WeekStartDay.MONDAY,
      today = today
    )
    assertEquals(3, analytics.currentStreak)
    assertEquals(3, analytics.totalCheckIns)
    assertEquals(100, analytics.periodCompletionRate)
    assertEquals(50.0, analytics.amountTimeAnalytics?.total ?: 0.0, 0.01)

    // 5. Verify Data Visualization weekly & monthly bar chart series from persisted entries
    val entriesByHabit = mapOf(habit.id to entries.associateBy { it.date })
    val pausesByHabit = emptyMap<String, List<com.example.core.domain.model.HabitPause>>()

    val weeklyDailySeries = com.example.feature.analytics.CompletionRateChartCalculator.computeWeeklyDailySeries(
      habits = listOf(habit),
      entriesByHabit = entriesByHabit,
      pausesByHabit = pausesByHabit,
      weekStart = com.example.core.domain.model.WeekStartDay.MONDAY,
      today = today
    )
    assertEquals(7, weeklyDailySeries.dataPoints.size)
    val mondayPoint = weeklyDailySeries.dataPoints.first { it.key == "2026-08-10" }
    assertEquals(100, mondayPoint.completionRate)
    assertTrue(mondayPoint.isCurrentBucket)

    val monthlySeries = com.example.feature.analytics.CompletionRateChartCalculator.computeMonthlySeries(
      habits = listOf(habit),
      entriesByHabit = entriesByHabit,
      pausesByHabit = pausesByHabit,
      today = today,
      monthsCount = 6
    )
    assertEquals(6, monthlySeries.dataPoints.size)
    val augustBucket = monthlySeries.dataPoints.last()
    assertEquals("2026-08", augustBucket.key)
    assertEquals(100, augustBucket.completionRate)
    assertEquals(3, augustBucket.completedCount)
  }

  @Test
  fun `workManager habit reminder system schedules specific daily times computes delays and dispatches notifications`() = runBlocking {
    val startDate = LocalDate.now().minusDays(2)
    val habit = Habit(
      id = "h_workmanager_rem",
      name = "Evening Stretch",
      iconKey = "yoga",
      colorKey = "#90CAF9",
      trackType = TrackType.TASK,
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      startDate = startDate
    )
    repository.saveHabit(habit, emptyList())

    // 1. Schedule a daily reminder at 20:30 for all days of the week via repository + ReminderScheduler
    val reminderTime = LocalTime.of(20, 30)
    val scheduledReminder = repository.scheduleDailyHabitReminder(
      habitId = habit.id,
      time = reminderTime,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      message = "Stay on track with your evening goal!"
    )

    val savedReminders = repository.remindersFlow.first().filter { it.habitId == habit.id }
    assertEquals(1, savedReminders.size)
    assertEquals(reminderTime, savedReminders.first().time)
    assertEquals("Stay on track with your evening goal!", savedReminders.first().message)
    assertTrue(savedReminders.first().enabled)

    // 2. Verify ReminderScheduler.computeNextOccurrence and computeInitialDelayMillis
    val fixedNow = java.time.LocalDateTime.of(2026, 8, 10, 18, 0, 0)
    val nextOccurrenceDt = com.example.core.notifications.ReminderScheduler.computeNextOccurrence(
      reminder = scheduledReminder,
      pauses = emptyList(),
      now = fixedNow
    )
    assertNotNull(nextOccurrenceDt)
    val delayMillis = com.example.core.notifications.ReminderScheduler.computeInitialDelayMillis(
      triggerAt = nextOccurrenceDt!!,
      now = fixedNow
    )
    // From 18:00 to 20:30 is exactly 2.5 hours = 9,000,000 ms
    assertEquals(9_000_000L, delayMillis)

    // 3. Verify unique WorkManager work name formatting
    val uniqueWorkName = com.example.core.notifications.ReminderScheduler.uniqueWorkNameForReminder(scheduledReminder.id)
    assertEquals("habit_reminder_work_${scheduledReminder.id}", uniqueWorkName)

    // 4. Execute processAndDispatchHabitReminder when habit is NOT yet completed today -> should dispatch notification
    com.example.core.notifications.NotificationChannels.ensureCreated(context)
    val dispatchedWhenPending = com.example.core.notifications.ReminderScheduler.processAndDispatchHabitReminder(
      context = context,
      reminderId = scheduledReminder.id,
      habitId = habit.id
    )
    assertTrue(dispatchedWhenPending)

    // 5. Mark habit done today and verify skipIfDone prevents redundant reminder notification
    val logicalToday = repository.getLogicalToday()
    repository.markHabitDoneOnDate(habit.id, logicalToday, "Completed before reminder")
    val dispatchedAfterDone = com.example.core.notifications.ReminderScheduler.processAndDispatchHabitReminder(
      context = context,
      reminderId = scheduledReminder.id,
      habitId = habit.id
    )
    assertEquals(false, dispatchedAfterDone)
  }

  @Test
  fun `weeklyPerformanceSummary calculates completion percentages delta and daily breakdown from Room database`() = runBlocking {
    // Use Wednesday 2026-08-12 as logical today (Week starts Monday 2026-08-10)
    val wednesdayToday = LocalDate.of(2026, 8, 12)
    val monday = LocalDate.of(2026, 8, 10)
    val tuesday = LocalDate.of(2026, 8, 11)
    val prevMonday = monday.minusDays(7)

    val habit1 = Habit(
      id = "h_weekly_1",
      name = "Read 20 Pages",
      iconKey = "book",
      colorKey = "#D6BEFA",
      trackType = TrackType.TASK,
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      startDate = prevMonday
    )
    val habit2 = Habit(
      id = "h_weekly_2",
      name = "Evening Walk",
      iconKey = "walk",
      colorKey = "#8FBDF0",
      trackType = TrackType.TASK,
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      startDate = prevMonday
    )
    repository.saveHabit(habit1, emptyList())
    repository.saveHabit(habit2, emptyList())

    // Current week elapsed days: Mon, Tue, Wed -> 3 days * 2 habits = 6 scheduled slots
    // Complete both habits on Monday (100% perfect day), 1 habit on Tuesday (50%), 1 habit on Wednesday (50%) -> 4/6 = 67%
    repository.markHabitDoneOnDate(habit1.id, monday)
    repository.markHabitDoneOnDate(habit2.id, monday)
    repository.markHabitDoneOnDate(habit1.id, tuesday)
    repository.markHabitDoneOnDate(habit2.id, wednesdayToday)

    // Previous week: 7 days * 2 habits = 14 scheduled slots; complete 7 of 14 = 50%
    for (i in 0 until 7) {
      repository.markHabitDoneOnDate(habit1.id, prevMonday.plusDays(i.toLong()))
    }

    // Verify Room DAO count query in current week range
    val completedInWeekDaoCount = database.entryDao().getCompletedEntryCountInRange(monday, monday.plusDays(6))
    assertEquals(4, completedInWeekDaoCount)

    // Verify repository suspend & Flow weekly performance summary calculation from Room
    val summary = repository.calculateWeeklyPerformanceSummary(
      today = wednesdayToday,
      weekStartOverride = com.example.core.domain.model.WeekStartDay.MONDAY
    )
    assertEquals(monday, summary.weekStartDate)
    assertEquals(monday.plusDays(6), summary.weekEndDate)
    assertEquals(4, summary.completedCount)
    assertEquals(6, summary.scheduledCount)
    assertEquals(67, summary.completionPercentage)
    assertEquals(50, summary.previousWeekPercentage)
    assertEquals(17, summary.deltaPercentage)
    assertEquals(1, summary.perfectDaysCount)
    assertEquals("Mon", summary.bestDayLabel)
    assertEquals(100, summary.bestDayPercentage)
    assertEquals("On Track", summary.statusLabel)
    assertEquals(7, summary.dailyBreakdown.size)

    val monPoint = summary.dailyBreakdown[0]
    assertEquals(100, monPoint.completionPercentage)
    val wedPoint = summary.dailyBreakdown[2]
    assertEquals(50, wedPoint.completionPercentage)
    assertTrue(wedPoint.isToday)

    val flowSummary = repository.observeWeeklyPerformanceSummary(todayOverride = wednesdayToday).first()
    assertEquals(67, flowSummary.completionPercentage)
    assertEquals(4, flowSummary.completedCount)
  }

  @Test
  fun `milestoneTrackingSystem awards visual badges for 7 day streak 100 completions total and consistency achievements`() = runBlocking {
    val today = LocalDate.of(2026, 9, 20)
    val startDate = today.minusDays(120)

    val habit1 = Habit(
      id = "h_milestone_1",
      name = "Morning Run",
      iconKey = "run",
      colorKey = "#F5A623",
      trackType = TrackType.TASK,
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      startDate = startDate
    )
    val habit2 = Habit(
      id = "h_milestone_2",
      name = "Hydration",
      iconKey = "water",
      colorKey = "#8FBDF0",
      trackType = TrackType.TASK,
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
      startDate = startDate
    )
    repository.saveHabit(habit1, emptyList())
    repository.saveHabit(habit2, emptyList())

    // Initially 0 unlocked badges
    val initialSummary = repository.calculateMilestoneSummary(today = today)
    assertEquals(0, initialSummary.unlockedCount)
    assertTrue(initialSummary.totalCount >= 14)

    // Log 6 consecutive days ending yesterday on habit1 -> unlocks 3-day streak badge, approaching 7-day streak
    for (i in 1..6) {
      repository.markHabitDoneOnDate(habit1.id, today.minusDays(i.toLong()))
    }
    val summaryBeforeDay7 = repository.calculateMilestoneSummary(today = today)
    assertTrue(summaryBeforeDay7.unlockedBadges.any { it.id == com.example.core.domain.usecase.MilestoneEngine.BADGE_STREAK_3 })
    org.junit.Assert.assertFalse(summaryBeforeDay7.unlockedBadges.any { it.id == com.example.core.domain.usecase.MilestoneEngine.BADGE_STREAK_7 })

    val prevIdsBeforeDay7 = summaryBeforeDay7.unlockedBadges.map { it.id }.toSet()

    // Complete today (7th consecutive day) -> unlocks '7-Day Streak' badge!
    repository.markHabitDoneOnDate(habit1.id, today)
    val summaryAtDay7 = repository.calculateMilestoneSummary(today = today)
    val streak7Badge = summaryAtDay7.allBadges.first { it.id == com.example.core.domain.usecase.MilestoneEngine.BADGE_STREAK_7 }
    assertTrue(streak7Badge.isUnlocked)
    assertEquals("7-Day Streak", streak7Badge.title)
    assertEquals(today, streak7Badge.unlockedDate)

    // Verify detectNewlyUnlockedBadges detects 'badge_streak_7'
    val newlyUnlockedOnDay7 = com.example.core.domain.usecase.MilestoneEngine.detectNewlyUnlockedBadges(
      previouslyUnlockedIds = prevIdsBeforeDay7,
      updatedSummary = summaryAtDay7
    )
    assertTrue(newlyUnlockedOnDay7.any { it.id == com.example.core.domain.usecase.MilestoneEngine.BADGE_STREAK_7 })

    // Also verify per-habit milestone badge shelf unlocks habit-specific 7-day streak
    val habit1Entries = database.entryDao().getEntriesForHabit(habit1.id).map { it.toDomain() }
    val habit1Badges = com.example.core.domain.usecase.MilestoneEngine.evaluateHabitMilestones(
      habit = habit1,
      entries = habit1Entries,
      pauses = emptyList(),
      today = today
    )
    assertTrue(habit1Badges.any { it.id.endsWith("streak_7") && it.isUnlocked })

    // Now log additional historical completions across habit1 & habit2 to reach 100 completions total
    // Currently we have 7 completions. Add 93 more historical completions (50 on habit1, 43 on habit2 including today).
    for (i in 10..59) {
      repository.markHabitDoneOnDate(habit1.id, today.minusDays(i.toLong()))
    }
    for (i in 0..42) {
      repository.markHabitDoneOnDate(habit2.id, today.minusDays(i.toLong()))
    }

    val summaryAt100 = repository.calculateMilestoneSummary(today = today)
    val completions100Badge = summaryAt100.allBadges.first { it.id == com.example.core.domain.usecase.MilestoneEngine.BADGE_COMPLETIONS_100 }
    assertTrue(completions100Badge.isUnlocked)
    assertEquals("Century Club", completions100Badge.title)
    assertEquals("100 completions total", completions100Badge.subtitle)
    assertEquals(100, completions100Badge.currentValue)

    // Verify consistency achievements (First Perfect Day, 80%+ Weekly Rate, 7-Day Perfect Week) are also unlocked
    assertTrue(summaryAt100.unlockedBadges.any { it.id == com.example.core.domain.usecase.MilestoneEngine.BADGE_FIRST_PERFECT_DAY })
    assertTrue(summaryAt100.unlockedBadges.any { it.id == com.example.core.domain.usecase.MilestoneEngine.BADGE_HIGH_CONSISTENCY_WEEK })
    assertTrue(summaryAt100.unlockedBadges.any { it.id == com.example.core.domain.usecase.MilestoneEngine.BADGE_PERFECT_WEEK })
  }

  @Test
  fun `habitCreationViewModel saves category and tags to Room and filters analytics by Fitness Mindfulness and Learning`() = runBlocking {
    val today = LocalDate.of(2026, 10, 14) // Wednesday
    val factory = HabitRadarViewModelFactory(container)
    val creationVm = factory.create(com.example.feature.habit.HabitCreationViewModel::class.java)

    // 1. Create a 'Fitness' habit with tags ["Cardio", "Morning"]
    creationVm.resetForm(today = today.minusDays(7))
    creationVm.updateName("Morning HIIT")
    creationVm.selectCategory("Fitness")
    creationVm.addTag("Cardio")
    creationVm.updateTagDraft("#Morning")
    creationVm.addTag()
    assertEquals("Fitness", creationVm.uiState.value.category)
    assertEquals(listOf("Cardio", "Morning"), creationVm.uiState.value.tags)

    val fitnessHabit = creationVm.saveHabitOfflineFirst().getOrThrow()

    // 2. Create a 'Mindfulness' habit with tag ["Breathwork"]
    creationVm.resetForm(today = today.minusDays(7))
    creationVm.updateName("Box Breathing")
    creationVm.selectCategory("Mindfulness")
    creationVm.toggleTag("Breathwork")
    val mindfulnessHabit = creationVm.saveHabitOfflineFirst().getOrThrow()

    // 3. Create a 'Learning' habit with tags ["Reading", "Deep Work"]
    creationVm.resetForm(today = today.minusDays(7))
    creationVm.updateName("Read Technical Book")
    creationVm.selectCategory("Learning")
    creationVm.addTag("Reading")
    creationVm.addTag("Deep Work")
    val learningHabit = creationVm.saveHabitOfflineFirst().getOrThrow()

    // Verify Room persistence of category and tags
    val savedFitness = database.habitDao().getHabitById(fitnessHabit.id)!!.toDomain()
    assertEquals("Fitness", savedFitness.category)
    assertEquals("Fitness", savedFitness.resolvedCategory)
    assertEquals(listOf("Cardio", "Morning"), savedFitness.tags)
    assertTrue(savedFitness.matchesCategoryOrTag("Fitness"))
    assertTrue(savedFitness.matchesCategoryOrTag("cardio"))
    org.junit.Assert.assertFalse(savedFitness.matchesCategoryOrTag("Mindfulness"))

    val fitnessByCategory = database.habitDao().getActiveHabitsByCategory("Fitness")
    assertEquals(1, fitnessByCategory.size)
    assertEquals("Morning HIIT", fitnessByCategory.first().name)

    // Log completions: Fitness completed 3 of 3 days this week (100%), Mindfulness 0 of 3 (0%), Learning 3 of 3 (100%)
    val monday = today.minusDays(2)
    val tuesday = today.minusDays(1)
    listOf(monday, tuesday, today).forEach { d ->
      repository.markHabitDoneOnDate(fitnessHabit.id, d)
      repository.markHabitDoneOnDate(learningHabit.id, d)
    }

    val allHabits = database.habitDao().getAllHabits().map { it.toDomain() }
    val allEntriesMap = database.entryDao().getAllEntries()
      .map { it.toDomain() }
      .groupBy { it.habitId }
      .mapValues { (_, list) -> list.associateBy { it.date } }

    // Filter analytics by 'Fitness' category -> 100% weekly rate across completed days
    val fitnessHabits = allHabits.filter { it.matchesCategoryOrTag("Fitness") }
    assertEquals(1, fitnessHabits.size)
    val fitnessChart = com.example.feature.analytics.CompletionRateChartCalculator.computeWeeklyDailySeries(
      habits = fitnessHabits,
      entriesByHabit = allEntriesMap,
      pausesByHabit = emptyMap(),
      weekStart = com.example.core.domain.model.WeekStartDay.MONDAY,
      today = today
    )
    assertEquals(3, fitnessChart.totalCompleted)
    assertTrue(fitnessChart.averageRate > 0)

    // Filter analytics by 'Mindfulness' category -> 0% weekly rate
    val mindfulnessHabits = allHabits.filter { it.matchesCategoryOrTag("Mindfulness") }
    assertEquals(1, mindfulnessHabits.size)
    val mindfulnessChart = com.example.feature.analytics.CompletionRateChartCalculator.computeWeeklyDailySeries(
      habits = mindfulnessHabits,
      entriesByHabit = allEntriesMap,
      pausesByHabit = emptyMap(),
      weekStart = com.example.core.domain.model.WeekStartDay.MONDAY,
      today = today
    )
    assertEquals(0, mindfulnessChart.totalCompleted)
    assertEquals(0, mindfulnessChart.averageRate)

    // Filter analytics by tag 'Deep Work' -> matches Learning habit
    val deepWorkHabits = allHabits.filter { it.matchesCategoryOrTag("Deep Work") }
    assertEquals(1, deepWorkHabits.size)
    assertEquals(learningHabit.id, deepWorkHabits.first().id)
    assertEquals("Mindfulness", mindfulnessHabit.resolvedCategory)

    // Verify Home Studio visibility toggles for weekly habit performance and milestone badges
    val initialSettings = container.settingsDataStore.getSettings()
    assertTrue(initialSettings.showWeeklyPerformanceCard)
    assertTrue(initialSettings.showMilestoneBadgesShelf)

    container.settingsDataStore.updateSettings {
      it.copy(showWeeklyPerformanceCard = false, showMilestoneBadgesShelf = false)
    }
    val hiddenSettings = container.settingsDataStore.getSettings()
    org.junit.Assert.assertFalse(hiddenSettings.showWeeklyPerformanceCard)
    org.junit.Assert.assertFalse(hiddenSettings.showMilestoneBadgesShelf)

    container.settingsDataStore.updateSettings {
      it.copy(showWeeklyPerformanceCard = true, showMilestoneBadgesShelf = true)
    }
    val restoredSettings = container.settingsDataStore.getSettings()
    assertTrue(restoredSettings.showWeeklyPerformanceCard)
    assertTrue(restoredSettings.showMilestoneBadgesShelf)

    // Verify main menu theme toggle between Light, Dark, and System modes
    assertEquals(com.example.core.domain.model.ThemeMode.DARK, restoredSettings.theme)
    container.settingsDataStore.updateSettings {
      it.copy(theme = com.example.core.domain.model.ThemeMode.LIGHT)
    }
    assertEquals(
      com.example.core.domain.model.ThemeMode.LIGHT,
      container.settingsDataStore.getSettings().theme
    )
    container.settingsDataStore.updateSettings {
      it.copy(theme = com.example.core.domain.model.ThemeMode.DARK)
    }
    assertEquals(
      com.example.core.domain.model.ThemeMode.DARK,
      container.settingsDataStore.getSettings().theme
    )
  }
}
