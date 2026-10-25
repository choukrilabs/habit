package com.example.core.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.HabitRadarDatabase
import com.example.core.data.repository.HabitRadarRepository
import com.example.core.di.AppContainer
import com.example.core.di.appContainer
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.Entry
import com.example.core.domain.model.Goal
import com.example.core.domain.model.GoalStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.Step
import com.example.core.domain.model.WidgetConfig
import com.example.core.domain.usecase.AnalyticsCalculator
import com.example.core.domain.usecase.AnalyticsPeriod
import com.example.core.domain.usecase.HabitAnalyticsResult
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.RecapCalculator
import com.example.core.domain.usecase.RecapReport
import com.example.core.domain.usecase.YearInReviewReport
import com.example.core.util.SoundFeedbackManager
import com.example.feature.backup.BackupManager
import com.example.feature.backup.BackupPreviewInfo
import com.example.feature.backup.RestoreMode
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Primary application ViewModel injected via [HabitRadarViewModelFactory] from [AppContainer].
 * Exposes reactive [StateFlow] streams and domain mutation actions across all screens.
 */
class MainAppViewModel(
  val repository: HabitRadarRepository,
  val settingsDataStore: SettingsDataStore,
  val soundFeedbackManager: SoundFeedbackManager,
  val authService: com.example.core.data.firebase.AuthService? = null
) : ViewModel() {

  val habitRepository: HabitRadarRepository
    get() = repository

  val settingsFlow: StateFlow<AppSettings> = settingsDataStore.settingsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = AppSettings(onboardingDone = true)
    )

  val allHabitsFlow: StateFlow<List<Habit>> = repository.habitsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  val activeHabitsFlow: StateFlow<List<Habit>> = repository.habitsFlow
    .map { list -> list.filter { it.status == HabitStatus.ACTIVE }.sortedBy { it.sortOrder } }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  val archivedHabitsFlow: StateFlow<List<Habit>> = repository.habitsFlow
    .map { list -> list.filter { it.status == HabitStatus.ARCHIVED } }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  val allEntriesFlow: StateFlow<List<Entry>> = repository.entriesFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  val allRemindersFlow: StateFlow<List<Reminder>> = repository.remindersFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  val allPausesFlow: StateFlow<List<HabitPause>> = repository.pausesFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  val allGoalsFlow: StateFlow<List<Goal>> = repository.goalsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  val allStepsFlow: StateFlow<List<Step>> = repository.stepsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  val widgetConfigsFlow: StateFlow<List<WidgetConfig>> = repository.widgetConfigsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  fun updateSettings(transform: (AppSettings) -> AppSettings) {
    viewModelScope.launch {
      settingsDataStore.updateSettings(transform)
    }
  }

  fun playCheckSoundIfEnabled() {
    if (settingsFlow.value.sounds) {
      soundFeedbackManager.playSoftTick(true)
    }
  }

  fun saveHabit(habit: Habit, reminders: List<Reminder>? = null, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.saveHabit(habit, reminders)
      onComplete()
    }
  }

  fun toggleTaskEntry(
    habitId: String,
    date: LocalDate,
    note: String? = null,
    onResult: (Boolean) -> Unit = {}
  ) {
    viewModelScope.launch {
      val nowDone = repository.toggleTaskEntry(habitId, date, note)
      if (nowDone) playCheckSoundIfEnabled()
      onResult(nowDone)
    }
  }

  fun setQuantitativeEntry(
    habitId: String,
    date: LocalDate,
    value: Double,
    note: String? = null,
    onComplete: () -> Unit = {}
  ) {
    viewModelScope.launch {
      repository.setQuantitativeEntry(habitId, date, value, note)
      onComplete()
    }
  }

  fun skipHabitOnDate(habitId: String, date: LocalDate) {
    viewModelScope.launch {
      repository.skipHabitOnDate(habitId, date)
    }
  }

  fun deleteEntryOnDate(habitId: String, date: LocalDate) {
    viewModelScope.launch {
      repository.deleteEntryOnDate(habitId, date)
    }
  }

  fun pauseHabit(habitId: String, startDate: LocalDate, endDate: LocalDate?) {
    viewModelScope.launch {
      repository.pauseHabit(habitId, startDate, endDate)
    }
  }

  fun resumeHabit(habitId: String) {
    viewModelScope.launch {
      repository.resumeHabit(habitId)
    }
  }

  fun archiveHabit(habitId: String) {
    viewModelScope.launch {
      repository.archiveHabit(habitId)
    }
  }

  fun restoreHabit(habitId: String) {
    viewModelScope.launch {
      repository.restoreHabit(habitId)
    }
  }

  fun deleteHabit(habitId: String) {
    viewModelScope.launch {
      repository.deleteHabit(habitId)
    }
  }

  fun updateHabitSortOrders(orderedIds: List<String>) {
    viewModelScope.launch {
      repository.updateHabitSortOrders(orderedIds)
    }
  }

  fun saveReminder(reminder: Reminder) {
    viewModelScope.launch {
      repository.saveReminder(reminder)
    }
  }

  fun deleteReminder(reminderId: String) {
    viewModelScope.launch {
      repository.deleteReminder(reminderId)
    }
  }

  fun setAllHabitRemindersEnabled(habitId: String, enabled: Boolean) {
    viewModelScope.launch {
      repository.setAllHabitRemindersEnabled(habitId, enabled)
    }
  }

  fun saveGoalWithSteps(goal: Goal, steps: List<Step>) {
    viewModelScope.launch {
      repository.saveGoalWithSteps(goal, steps)
    }
  }

  fun saveStep(step: Step) {
    viewModelScope.launch {
      repository.saveStep(step)
    }
  }

  fun deleteStep(stepId: String, goalId: String) {
    viewModelScope.launch {
      repository.deleteStep(stepId, goalId)
    }
  }

  fun toggleManualStep(step: Step, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val goalCompleted = repository.toggleManualStep(step)
      onResult(goalCompleted)
    }
  }

  fun updateAmountStepValue(step: Step, newValue: Double, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val goalCompleted = repository.updateAmountStepValue(step, newValue)
      onResult(goalCompleted)
    }
  }

  fun skipStepForNow(step: Step) {
    viewModelScope.launch {
      repository.skipStepForNow(step)
    }
  }

  fun markGoalStatus(goalId: String, status: GoalStatus) {
    viewModelScope.launch {
      repository.markGoalStatus(goalId, status)
    }
  }

  fun deleteGoal(goalId: String) {
    viewModelScope.launch {
      repository.deleteGoal(goalId)
    }
  }
}

/**
 * ViewModel for Habit Detail analytics and per-habit actions, created via [HabitRadarViewModelFactory].
 */
class HabitDetailViewModel(
  val repository: HabitRadarRepository,
  val settingsDataStore: SettingsDataStore
) : ViewModel() {

  private val _selectedPeriod = MutableStateFlow(AnalyticsPeriod.D30)
  val selectedPeriod: StateFlow<AnalyticsPeriod> = _selectedPeriod.asStateFlow()

  fun setPeriod(period: AnalyticsPeriod) {
    _selectedPeriod.value = period
  }

  fun computeAnalytics(
    habit: Habit,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    reminders: List<Reminder>,
    settings: AppSettings
  ): HabitAnalyticsResult {
    val today = LogicalDateCalculator.now(settings.dayRollover)
    val firstRemMins = reminders.filter { it.enabled }.minOfOrNull { it.time.hour * 60 + it.time.minute }
    return AnalyticsCalculator.calculateAnalytics(
      habit = habit,
      entries = entries,
      pauses = pauses,
      firstReminderMinutesOfDay = firstRemMins,
      period = _selectedPeriod.value,
      weekStart = settings.weekStart,
      today = today
    )
  }
}

/**
 * ViewModel for Weekly, Monthly, and Year-in-Review recaps, created via [HabitRadarViewModelFactory].
 */
class RecapViewModel(
  val repository: HabitRadarRepository,
  val settingsDataStore: SettingsDataStore
) : ViewModel() {

  fun computeWeeklyRecap(
    weekStart: LocalDate,
    habits: List<Habit>,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): RecapReport = RecapCalculator.computeWeeklyRecap(weekStart, habits, entries, pauses, today)

  fun computeMonthlyRecap(
    yearMonth: YearMonth,
    habits: List<Habit>,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): RecapReport = RecapCalculator.computeMonthlyRecap(yearMonth, habits, entries, pauses, today)

  fun computeYearInReview(
    year: Int,
    habits: List<Habit>,
    entries: List<Entry>,
    pauses: List<HabitPause>,
    today: LocalDate
  ): YearInReviewReport = RecapCalculator.computeYearInReview(year, habits, entries, pauses, today)
}

/**
 * ViewModel for Backup & Restore operations, created via [HabitRadarViewModelFactory].
 */
class BackupRestoreViewModel(
  val database: HabitRadarDatabase,
  val settingsDataStore: SettingsDataStore,
  val backupManager: BackupManager
) : ViewModel() {

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  fun exportJsonBackup(
    context: android.content.Context,
    targetUri: Uri,
    includeSettings: Boolean,
    onDone: (Result<Unit>) -> Unit = {}
  ) {
    viewModelScope.launch {
      val result = backupManager.exportJsonToUri(context, targetUri, database, settingsDataStore, includeSettings)
      _statusMessage.value = if (result.isSuccess) "Backup exported successfully." else "Export failed: ${result.exceptionOrNull()?.message}"
      onDone(result)
    }
  }

  fun exportCsvZip(
    context: android.content.Context,
    targetUri: Uri,
    onDone: (Result<Unit>) -> Unit = {}
  ) {
    viewModelScope.launch {
      val result = backupManager.exportCsvZipToUri(context, targetUri, database)
      _statusMessage.value = if (result.isSuccess) "CSV archive exported successfully." else "CSV export failed: ${result.exceptionOrNull()?.message}"
      onDone(result)
    }
  }

  fun inspectBackupJson(
    rawJson: String,
    onResult: (Result<BackupPreviewInfo>) -> Unit
  ) {
    val res = backupManager.validateAndPreviewBackupJson(rawJson)
    onResult(res)
  }

  fun applyRestore(
    context: android.content.Context,
    preview: BackupPreviewInfo,
    mode: RestoreMode,
    onResult: (Result<Int>) -> Unit
  ) {
    viewModelScope.launch {
      val res = backupManager.applyRestore(context, database, settingsDataStore, preview.envelope, mode)
      _statusMessage.value = if (res.isSuccess) "Restore completed (${mode.name.lowercase()})." else "Restore failed: ${res.exceptionOrNull()?.message}"
      onResult(res)
    }
  }
}

/**
 * Manual DI [ViewModelProvider.Factory] that resolves dependencies from [AppContainer]
 * without Hilt or reflection-heavy annotation processors.
 */
class HabitRadarViewModelFactory(
  private val container: AppContainer? = null
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
    val resolvedContainer = container ?: run {
      val application = checkNotNull(extras[APPLICATION_KEY]) {
        "Application must be present in CreationExtras when no explicit AppContainer is passed."
      }
      application.appContainer
    }
    return createWithContainer(modelClass, resolvedContainer)
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    val resolvedContainer = checkNotNull(container) {
      "An explicit AppContainer is required when calling create(modelClass) without CreationExtras."
    }
    return createWithContainer(modelClass, resolvedContainer)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T : ViewModel> createWithContainer(
    modelClass: Class<T>,
    c: AppContainer
  ): T {
    return when {
      modelClass.isAssignableFrom(MainAppViewModel::class.java) -> {
        MainAppViewModel(
          repository = c.habitRepository,
          settingsDataStore = c.settingsDataStore,
          soundFeedbackManager = c.soundFeedbackManager,
          authService = c.authService
        ) as T
      }
      modelClass.isAssignableFrom(HabitDetailViewModel::class.java) -> {
        HabitDetailViewModel(
          repository = c.repository,
          settingsDataStore = c.settingsDataStore
        ) as T
      }
      modelClass.isAssignableFrom(RecapViewModel::class.java) -> {
        RecapViewModel(
          repository = c.repository,
          settingsDataStore = c.settingsDataStore
        ) as T
      }
      modelClass.isAssignableFrom(BackupRestoreViewModel::class.java) -> {
        BackupRestoreViewModel(
          database = c.database,
          settingsDataStore = c.settingsDataStore,
          backupManager = c.backupManager
        ) as T
      }
      else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }

  companion object {
    val Factory: ViewModelProvider.Factory = HabitRadarViewModelFactory()
  }
}
