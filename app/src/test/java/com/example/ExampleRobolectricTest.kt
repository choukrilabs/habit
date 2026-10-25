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
}
