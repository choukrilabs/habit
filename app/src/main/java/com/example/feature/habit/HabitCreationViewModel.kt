package com.example.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.repository.HabitRadarRepository
import com.example.core.domain.model.ComparisonType
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.HabitTemplate
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.TrackType
import com.example.core.domain.usecase.ScheduleMatcher
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Quick-select frequency presets for the Habit Creation form.
 */
enum class FrequencyPreset(
  val label: String,
  val repeatType: RepeatType,
  val daysOfWeek: List<Int>,
  val timesPerWeek: Int,
  val daysOfMonth: List<Int>
) {
  EVERY_DAY(
    label = "Every day",
    repeatType = RepeatType.DAILY,
    daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
    timesPerWeek = 7,
    daysOfMonth = listOf(1, 15)
  ),
  WEEKDAYS(
    label = "Weekdays",
    repeatType = RepeatType.DAILY,
    daysOfWeek = listOf(1, 2, 3, 4, 5),
    timesPerWeek = 5,
    daysOfMonth = listOf(1, 15)
  ),
  WEEKENDS(
    label = "Weekends",
    repeatType = RepeatType.DAILY,
    daysOfWeek = listOf(6, 7),
    timesPerWeek = 2,
    daysOfMonth = listOf(1, 15)
  ),
  THREE_TIMES_A_WEEK(
    label = "3x / week",
    repeatType = RepeatType.WEEKLY,
    daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
    timesPerWeek = 3,
    daysOfMonth = listOf(1, 15)
  ),
  FIVE_TIMES_A_WEEK(
    label = "5x / week",
    repeatType = RepeatType.WEEKLY,
    daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
    timesPerWeek = 5,
    daysOfMonth = listOf(1, 15)
  ),
  TWICE_A_MONTH(
    label = "2x / month",
    repeatType = RepeatType.MONTHLY,
    daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
    timesPerWeek = 2,
    daysOfMonth = listOf(1, 15)
  )
}

/**
 * Immutable UI state for the offline-first Habit Creation and Edit screen.
 */
data class HabitCreationUiState(
  val existingHabit: Habit? = null,
  val name: String = "",
  val description: String = "",
  val iconKey: String = "star",
  val colorKey: String = "#8FBDF0",
  val trackType: TrackType = TrackType.TASK,
  val targetValueInput: String = "8",
  val unit: String = "glasses",
  val comparison: ComparisonType = ComparisonType.AT_LEAST,
  val repeatType: RepeatType = RepeatType.DAILY,
  val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
  val timesPerPeriod: Int = 3,
  val daysOfMonth: List<Int> = listOf(1, 15),
  val reminders: List<Reminder> = emptyList(),
  val remindMeMaster: Boolean = false,
  val groupName: String = "",
  val category: String = "",
  val tags: List<String> = emptyList(),
  val tagDraftInput: String = "",
  val goalStreakInput: String = "",
  val askNote: Boolean = false,
  val startDate: LocalDate = LocalDate.now(),
  val sortOrder: Int = 0,
  val isSaving: Boolean = false,
  val isSaved: Boolean = false,
  val savedHabit: Habit? = null,
  val errorMessage: String? = null
) {
  val isEditMode: Boolean
    get() = existingHabit != null && existingHabit.id.isNotBlank()

  val isNameValid: Boolean
    get() = name.trim().isNotEmpty()

  val isTargetValid: Boolean
    get() = when (trackType) {
      TrackType.TASK -> true
      TrackType.AMOUNT, TrackType.TIME -> (targetValueInput.toDoubleOrNull() ?: 0.0) > 0.0
    }

  val isFrequencyValid: Boolean
    get() = when (repeatType) {
      RepeatType.DAILY -> daysOfWeek.isNotEmpty() && daysOfWeek.all { it in 1..7 }
      RepeatType.WEEKLY -> timesPerPeriod in 1..7
      RepeatType.MONTHLY -> daysOfMonth.isNotEmpty() && daysOfMonth.all { it in 1..31 }
    }

  val isValid: Boolean
    get() = isNameValid && isTargetValid && isFrequencyValid

  val frequencySummary: String
    get() = when (repeatType) {
      RepeatType.DAILY -> {
        val sorted = daysOfWeek.distinct().sorted()
        when {
          sorted.size == 7 -> "Every day"
          sorted == listOf(1, 2, 3, 4, 5) -> "Weekdays (Mon–Fri)"
          sorted == listOf(6, 7) -> "Weekends (Sat–Sun)"
          sorted.isEmpty() -> "Select at least 1 day"
          else -> sorted.joinToString(", ") { ScheduleMatcher.dowShortLabel(it) }
        }
      }
      RepeatType.WEEKLY -> {
        if (timesPerPeriod == 1) "1 time per week" else "$timesPerPeriod times per week"
      }
      RepeatType.MONTHLY -> {
        val sorted = daysOfMonth.distinct().sorted()
        if (sorted.isEmpty()) {
          "Select at least 1 day of month"
        } else {
          "Monthly on day ${sorted.joinToString(", ")}"
        }
      }
    }

  val activePreset: FrequencyPreset?
    get() = FrequencyPreset.entries.firstOrNull { preset ->
      when (preset.repeatType) {
        RepeatType.DAILY ->
          repeatType == RepeatType.DAILY && daysOfWeek.distinct().sorted() == preset.daysOfWeek
        RepeatType.WEEKLY ->
          repeatType == RepeatType.WEEKLY && timesPerPeriod == preset.timesPerWeek
        RepeatType.MONTHLY ->
          repeatType == RepeatType.MONTHLY && daysOfMonth.distinct().sorted() == preset.daysOfMonth
      }
    }

  fun toHabit(resolvedSortOrder: Int = sortOrder): Habit {
    val habitId = existingHabit?.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
    val parsedTarget = targetValueInput.toDoubleOrNull()
    val resolvedUnit = when (trackType) {
      TrackType.TASK -> null
      TrackType.AMOUNT -> unit.trim().ifBlank { "units" }
      TrackType.TIME -> "min"
    }
    val cleanedCategory = category.trim().take(28).ifBlank { null }
    val cleanedGroup = groupName.trim().take(24).ifBlank { cleanedCategory }
    val cleanedTags = tags
      .map { it.trim().removePrefix("#").trim() }
      .filter { it.isNotEmpty() }
      .distinctBy { it.lowercase() }
    return Habit(
      id = habitId,
      name = name.trim().take(40),
      description = description.trim().take(200).ifBlank { null },
      iconKey = iconKey,
      colorKey = colorKey,
      trackType = trackType,
      targetValue = if (trackType == TrackType.TASK) null else parsedTarget,
      unit = resolvedUnit,
      comparison = comparison,
      repeatType = repeatType,
      daysOfWeek = daysOfWeek.distinct().sorted(),
      timesPerPeriod = if (repeatType == RepeatType.WEEKLY) timesPerPeriod.coerceIn(1, 7) else null,
      daysOfMonth = if (repeatType == RepeatType.MONTHLY) daysOfMonth.distinct().sorted() else emptyList(),
      group = cleanedGroup,
      category = cleanedCategory,
      tags = cleanedTags,
      goalStreak = goalStreakInput.toIntOrNull(),
      askNote = askNote,
      status = existingHabit?.status ?: HabitStatus.ACTIVE,
      archivedAt = existingHabit?.archivedAt,
      startDate = existingHabit?.startDate ?: startDate,
      endDate = existingHabit?.endDate,
      sortOrder = existingHabit?.sortOrder ?: resolvedSortOrder
    )
  }

  fun toReminders(resolvedHabitId: String): List<Reminder> {
    return reminders.take(5).map { reminder ->
      reminder.copy(
        habitId = resolvedHabitId,
        enabled = remindMeMaster
      )
    }
  }
}

/**
 * Offline-first ViewModel for creating and editing habits.
 * Persists habits and reminders directly to the local Room database via [HabitRadarRepository]
 * without requiring network connectivity or cloud authentication.
 */
class HabitCreationViewModel(
  val repository: HabitRadarRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(HabitCreationUiState())
  val uiState: StateFlow<HabitCreationUiState> = _uiState.asStateFlow()

  fun resetForm(
    today: LocalDate = LocalDate.now(),
    existingHabit: Habit? = null,
    existingReminders: List<Reminder> = emptyList(),
    isDraftTemplate: Boolean = false
  ) {
    if (existingHabit == null) {
      _uiState.value = HabitCreationUiState(startDate = today)
    } else {
      val formattedTarget = existingHabit.targetValue?.let {
        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
      } ?: if (existingHabit.trackType == TrackType.TIME) "15" else "8"

      _uiState.value = HabitCreationUiState(
        existingHabit = if (isDraftTemplate) null else existingHabit,
        name = existingHabit.name,
        description = existingHabit.description ?: "",
        iconKey = existingHabit.iconKey,
        colorKey = existingHabit.colorKey,
        trackType = existingHabit.trackType,
        targetValueInput = formattedTarget,
        unit = existingHabit.unit ?: "glasses",
        comparison = existingHabit.comparison,
        repeatType = existingHabit.repeatType,
        daysOfWeek = existingHabit.daysOfWeek.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
        timesPerPeriod = existingHabit.timesPerPeriod ?: 3,
        daysOfMonth = existingHabit.daysOfMonth.ifEmpty { listOf(1, 15) },
        reminders = existingReminders,
        remindMeMaster = existingReminders.any { it.enabled },
        groupName = existingHabit.group ?: "",
        category = existingHabit.category ?: existingHabit.group ?: "",
        tags = existingHabit.tags,
        tagDraftInput = "",
        goalStreakInput = existingHabit.goalStreak?.toString() ?: "",
        askNote = existingHabit.askNote,
        startDate = existingHabit.startDate,
        sortOrder = existingHabit.sortOrder
      )
    }
  }

  fun updateName(name: String) {
    _uiState.update { state ->
      state.copy(
        name = name.take(40),
        errorMessage = null
      )
    }
  }

  fun setHabitName(name: String) = updateName(name)

  fun updateDescription(description: String) {
    _uiState.update { it.copy(description = description.take(200)) }
  }

  fun selectIcon(iconKey: String) {
    _uiState.update { it.copy(iconKey = iconKey) }
  }

  fun selectColor(colorKey: String) {
    _uiState.update { it.copy(colorKey = colorKey) }
  }

  fun selectTrackType(trackType: TrackType) {
    _uiState.update { state ->
      val nextTarget = if (trackType == TrackType.TIME && state.targetValueInput == "8") {
        "15"
      } else {
        state.targetValueInput
      }
      state.copy(
        trackType = trackType,
        targetValueInput = nextTarget,
        errorMessage = null
      )
    }
  }

  fun updateTargetValue(targetValue: String) {
    val sanitized = targetValue.filter { it.isDigit() || it == '.' }
    _uiState.update { it.copy(targetValueInput = sanitized, errorMessage = null) }
  }

  fun updateUnit(unit: String) {
    _uiState.update { it.copy(unit = unit.take(16)) }
  }

  fun selectComparison(comparison: ComparisonType) {
    _uiState.update { it.copy(comparison = comparison) }
  }

  fun selectFrequency(repeatType: RepeatType) {
    _uiState.update { state ->
      state.copy(
        repeatType = repeatType,
        daysOfWeek = state.daysOfWeek.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
        daysOfMonth = state.daysOfMonth.ifEmpty { listOf(1, 15) },
        errorMessage = null
      )
    }
  }

  fun setRepeatType(repeatType: RepeatType) = selectFrequency(repeatType)

  fun selectFrequencyPreset(preset: FrequencyPreset) {
    _uiState.update { state ->
      state.copy(
        repeatType = preset.repeatType,
        daysOfWeek = preset.daysOfWeek,
        timesPerPeriod = preset.timesPerWeek,
        daysOfMonth = preset.daysOfMonth,
        errorMessage = null
      )
    }
  }

  fun toggleDayOfWeek(dayIso: Int) {
    if (dayIso !in 1..7) return
    _uiState.update { state ->
      val current = state.daysOfWeek.toMutableList()
      if (dayIso in current) {
        if (current.size > 1) {
          current.remove(dayIso)
        }
      } else {
        current.add(dayIso)
      }
      state.copy(
        daysOfWeek = current.distinct().sorted(),
        errorMessage = null
      )
    }
  }

  fun setDaysOfWeek(days: List<Int>) {
    val validDays = days.filter { it in 1..7 }.distinct().sorted()
    _uiState.update { state ->
      state.copy(
        repeatType = RepeatType.DAILY,
        daysOfWeek = validDays,
        errorMessage = null
      )
    }
  }

  fun setTimesPerWeek(times: Int) {
    _uiState.update { state ->
      state.copy(
        repeatType = RepeatType.WEEKLY,
        timesPerPeriod = times.coerceIn(1, 7),
        errorMessage = null
      )
    }
  }

  fun incrementTimesPerWeek() {
    _uiState.update { state ->
      state.copy(
        timesPerPeriod = (state.timesPerPeriod + 1).coerceAtMost(7),
        errorMessage = null
      )
    }
  }

  fun decrementTimesPerWeek() {
    _uiState.update { state ->
      state.copy(
        timesPerPeriod = (state.timesPerPeriod - 1).coerceAtLeast(1),
        errorMessage = null
      )
    }
  }

  fun toggleDayOfMonth(dayOfMonth: Int) {
    if (dayOfMonth !in 1..31) return
    _uiState.update { state ->
      val current = state.daysOfMonth.toMutableList()
      if (dayOfMonth in current) {
        if (current.size > 1) {
          current.remove(dayOfMonth)
        }
      } else {
        current.add(dayOfMonth)
      }
      state.copy(
        daysOfMonth = current.distinct().sorted(),
        errorMessage = null
      )
    }
  }

  fun setDaysOfMonth(days: List<Int>) {
    val validDays = days.filter { it in 1..31 }.distinct().sorted()
    _uiState.update { state ->
      state.copy(
        repeatType = RepeatType.MONTHLY,
        daysOfMonth = validDays,
        errorMessage = null
      )
    }
  }

  fun setRemindMeMaster(enabled: Boolean) {
    _uiState.update { state ->
      val updatedReminders = if (enabled && state.reminders.isEmpty()) {
        listOf(
          Reminder(
            id = UUID.randomUUID().toString(),
            habitId = state.existingHabit?.id ?: "",
            time = LocalTime.of(8, 0),
            daysOfWeek = state.daysOfWeek.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }
          )
        )
      } else {
        state.reminders
      }
      state.copy(
        remindMeMaster = enabled,
        reminders = updatedReminders
      )
    }
  }

  fun addOrUpdateReminder(reminder: Reminder) {
    _uiState.update { state ->
      val list = state.reminders.toMutableList()
      val existingIdx = list.indexOfFirst { it.id == reminder.id }
      if (existingIdx >= 0) {
        list[existingIdx] = reminder
      } else if (list.size < 5) {
        list.add(reminder)
      }
      state.copy(reminders = list, remindMeMaster = true)
    }
  }

  fun removeReminder(reminderId: String) {
    _uiState.update { state ->
      val updated = state.reminders.filterNot { it.id == reminderId }
      state.copy(
        reminders = updated,
        remindMeMaster = if (updated.isEmpty()) false else state.remindMeMaster
      )
    }
  }

  fun updateGroup(group: String) {
    _uiState.update { it.copy(groupName = group.take(24)) }
  }

  fun selectCategory(categoryName: String) {
    _uiState.update { state ->
      val nextCat = if (state.category.equals(categoryName, ignoreCase = true)) "" else categoryName.take(28)
      state.copy(category = nextCat)
    }
  }

  fun updateCategory(categoryName: String) {
    _uiState.update { it.copy(category = categoryName.take(28)) }
  }

  fun setCategory(categoryName: String) = updateCategory(categoryName)

  fun updateTagDraft(draft: String) {
    _uiState.update { it.copy(tagDraftInput = draft.take(24)) }
  }

  fun addTag(rawTag: String = _uiState.value.tagDraftInput) {
    val clean = rawTag.trim().removePrefix("#").trim().take(24)
    if (clean.isEmpty()) return
    _uiState.update { state ->
      val updated = if (state.tags.any { it.equals(clean, ignoreCase = true) }) {
        state.tags
      } else {
        (state.tags + clean).take(10)
      }
      state.copy(tags = updated, tagDraftInput = "")
    }
  }

  fun removeTag(tag: String) {
    _uiState.update { state ->
      state.copy(tags = state.tags.filterNot { it.equals(tag, ignoreCase = true) })
    }
  }

  fun toggleTag(tag: String) {
    val clean = tag.trim().removePrefix("#").trim().take(24)
    if (clean.isEmpty()) return
    _uiState.update { state ->
      val exists = state.tags.any { it.equals(clean, ignoreCase = true) }
      val updated = if (exists) {
        state.tags.filterNot { it.equals(clean, ignoreCase = true) }
      } else {
        (state.tags + clean).take(10)
      }
      state.copy(tags = updated)
    }
  }

  fun updateGoalStreak(streak: String) {
    _uiState.update { it.copy(goalStreakInput = streak.filter { ch -> ch.isDigit() }) }
  }

  fun setAskNote(askNote: Boolean) {
    _uiState.update { it.copy(askNote = askNote) }
  }

  fun applyTemplate(template: HabitTemplate) {
    val normalizedCategory = com.example.core.domain.model.HabitCategoryCatalog
      .normalizeTemplateCategory(template.category)
      .ifBlank { template.category }
    _uiState.update { state ->
      state.copy(
        name = template.name,
        description = template.description,
        iconKey = template.iconKey,
        colorKey = template.colorKey,
        trackType = template.trackType,
        targetValueInput = template.targetValue?.toInt()?.toString() ?: state.targetValueInput,
        unit = template.unit ?: state.unit,
        repeatType = template.repeatType,
        daysOfWeek = template.daysOfWeek.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
        timesPerPeriod = template.timesPerPeriod ?: state.timesPerPeriod,
        groupName = template.group ?: normalizedCategory,
        category = normalizedCategory,
        tags = listOfNotNull(
          normalizedCategory.takeIf { it.isNotBlank() },
          template.group?.takeIf { it.isNotBlank() }
        ).distinct(),
        errorMessage = null
      )
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  /**
   * Validates the current form state and persists the habit + reminders directly to
   * the local Room database (offline-first).
   */
  suspend fun saveHabitOfflineFirst(): Result<Habit> {
    val current = _uiState.value
    if (!current.isNameValid) {
      val msg = "Please enter a habit name."
      _uiState.update { it.copy(errorMessage = msg) }
      return Result.failure(IllegalArgumentException(msg))
    }
    if (!current.isTargetValid) {
      val msg = "Please enter a target value greater than 0."
      _uiState.update { it.copy(errorMessage = msg) }
      return Result.failure(IllegalArgumentException(msg))
    }
    if (!current.isFrequencyValid) {
      val msg = "Please select at least one scheduled day for this frequency."
      _uiState.update { it.copy(errorMessage = msg) }
      return Result.failure(IllegalArgumentException(msg))
    }

    _uiState.update { it.copy(isSaving = true, errorMessage = null) }
    return try {
      val nextSortOrder = if (current.isEditMode) {
        current.existingHabit?.sortOrder ?: current.sortOrder
      } else {
        repository.habitDao.getAllHabits().size
      }
      val habitToSave = current.toHabit(resolvedSortOrder = nextSortOrder)
      val remindersToSave = current.toReminders(resolvedHabitId = habitToSave.id)

      repository.saveHabit(habitToSave, remindersToSave)

      _uiState.update {
        it.copy(
          isSaving = false,
          isSaved = true,
          savedHabit = habitToSave,
          errorMessage = null
        )
      }
      Result.success(habitToSave)
    } catch (t: Throwable) {
      val msg = t.message ?: "Failed to save habit locally."
      _uiState.update { it.copy(isSaving = false, errorMessage = msg) }
      Result.failure(t)
    }
  }

  fun saveHabit(
    onSuccess: (Habit) -> Unit = {},
    onError: (String) -> Unit = {}
  ) {
    viewModelScope.launch {
      val result = saveHabitOfflineFirst()
      result.onSuccess(onSuccess).onFailure { err ->
        onError(err.message ?: "Failed to save habit.")
      }
    }
  }
}
