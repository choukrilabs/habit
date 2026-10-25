package com.example.feature.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.HabitRadarDatabase
import com.example.core.data.db.toDomain
import com.example.core.data.db.toEntity
import com.example.core.domain.model.ComparisonType
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Goal
import com.example.core.domain.model.GoalStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.HabitStepMetric
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.ReminderSound
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.Step
import com.example.core.domain.model.StepType
import com.example.core.domain.model.TrackType
import com.example.core.notifications.ReminderScheduler
import com.example.feature.widgets.glance.WidgetUpdater
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupHabitDto(
  val id: String,
  val name: String,
  val description: String? = null,
  val iconKey: String,
  val colorKey: String,
  val trackType: TrackType,
  val targetValue: Double? = null,
  val unit: String? = null,
  val comparison: ComparisonType = ComparisonType.AT_LEAST,
  val repeatType: RepeatType,
  val daysOfWeek: List<Int>,
  val timesPerPeriod: Int? = null,
  val daysOfMonth: List<Int> = emptyList(),
  val group: String? = null,
  val category: String? = null,
  val tags: List<String> = emptyList(),
  val goalStreak: Int? = null,
  val askNote: Boolean = false,
  val status: HabitStatus = HabitStatus.ACTIVE,
  val archivedAtEpochMillis: Long? = null,
  val startDateStr: String,
  val endDateStr: String? = null,
  val sortOrder: Int = 0,
  val createdAtEpochMillis: Long
)

@Serializable
data class BackupEntryDto(
  val id: String,
  val habitId: String,
  val dateStr: String,
  val status: EntryStatus,
  val value: Double? = null,
  val note: String? = null,
  val completedAtEpochMillis: Long? = null
)

@Serializable
data class BackupReminderDto(
  val id: String,
  val habitId: String,
  val timeStr: String,
  val daysOfWeek: List<Int>,
  val message: String? = null,
  val skipIfDone: Boolean = true,
  val repeatAfterMin: Int? = null,
  val sound: ReminderSound = ReminderSound.DEFAULT,
  val vibrate: Boolean = true,
  val quickActions: Boolean = true,
  val enabled: Boolean = true
)

@Serializable
data class BackupPauseDto(
  val id: String,
  val habitId: String,
  val startDateStr: String,
  val endDateStr: String? = null
)

@Serializable
data class BackupGoalDto(
  val id: String,
  val title: String,
  val description: String? = null,
  val iconKey: String,
  val colorKey: String,
  val startDateStr: String,
  val targetDateStr: String? = null,
  val status: GoalStatus,
  val completedAtEpochMillis: Long? = null,
  val sortOrder: Int = 0,
  val remindWeekly: Boolean = false,
  val weeklyDay: Int = 7,
  val weeklyTimeStr: String = "18:00",
  val remindBeforeDays: Int? = 3,
  val beforeTimeStr: String = "09:00",
  val remindStepDue: Boolean = false
)

@Serializable
data class BackupStepDto(
  val id: String,
  val goalId: String,
  val title: String,
  val note: String? = null,
  val dueDateStr: String? = null,
  val sortOrder: Int = 0,
  val type: StepType = StepType.MANUAL,
  val linkedHabitId: String? = null,
  val habitMetric: HabitStepMetric? = null,
  val habitTarget: Int? = null,
  val habitWindowDays: Int? = null,
  val amountTarget: Double? = null,
  val amountCurrent: Double? = null,
  val amountUnit: String? = null,
  val done: Boolean = false,
  val doneAtEpochMillis: Long? = null
)

@Serializable
data class BackupSettingsDto(
  val weekStart: String,
  val accentColor: String,
  val theme: String,
  val todayLayout: String,
  val cardDensity: String,
  val dayRollover: String,
  val showHijri: Boolean
)

@Serializable
data class BackupPayload(
  val habits: List<BackupHabitDto>,
  val habitPauses: List<BackupPauseDto>,
  val entries: List<BackupEntryDto>,
  val reminders: List<BackupReminderDto>,
  val goals: List<BackupGoalDto>,
  val steps: List<BackupStepDto>,
  val settings: BackupSettingsDto? = null
)

@Serializable
data class BackupFileEnvelope(
  val format: String = "habit-radar-backup",
  val schemaVersion: Int = 1,
  val exportedAt: String,
  val checksum: String,
  val data: BackupPayload
)

data class BackupPreviewInfo(
  val envelope: BackupFileEnvelope,
  val habitsCount: Int,
  val entriesCount: Int,
  val goalsCount: Int,
  val exportedAt: String
)

enum class RestoreMode {
  REPLACE,
  MERGE
}

object BackupManager {
  const val CURRENT_SCHEMA_VERSION = 1

  private val jsonFormat = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  suspend fun buildBackupJsonString(
    db: HabitRadarDatabase,
    settingsStore: SettingsDataStore,
    includeSettings: Boolean
  ): String = withContext(Dispatchers.IO) {
    val habits = db.habitDao().getAllHabits().map { it.toDomain().toDto() }
    val pauses = db.pauseDao().getAllPauses().map { it.toDomain().toDto() }
    val entries = db.entryDao().getAllEntries().map { it.toDomain().toDto() }
    val reminders = db.reminderDao().getAllReminders().map { it.toDomain().toDto() }
    val goals = db.goalDao().getAllGoals().map { it.toDomain().toDto() }
    val steps = db.goalDao().getAllSteps().map { it.toDomain().toDto() }
    val s = settingsStore.getSettings()
    val settingsDto = if (includeSettings) {
      BackupSettingsDto(
        weekStart = s.weekStart.name,
        accentColor = s.accentColor,
        theme = s.theme.name,
        todayLayout = s.todayLayout.name,
        cardDensity = s.cardDensity.name,
        dayRollover = s.dayRollover.toString(),
        showHijri = s.showHijri
      )
    } else null

    val payload = BackupPayload(
      habits = habits,
      habitPauses = pauses,
      entries = entries,
      reminders = reminders,
      goals = goals,
      steps = steps,
      settings = settingsDto
    )
    val payloadEncoded = jsonFormat.encodeToString(payload)
    val checksum = sha256Hex(payloadEncoded)
    val envelope = BackupFileEnvelope(
      format = "habit-radar-backup",
      schemaVersion = CURRENT_SCHEMA_VERSION,
      exportedAt = Instant.now().toString(),
      checksum = checksum,
      data = payload
    )
    jsonFormat.encodeToString(envelope)
  }

  suspend fun exportJsonToUri(
    context: Context,
    uri: Uri,
    db: HabitRadarDatabase,
    settingsStore: SettingsDataStore,
    includeSettings: Boolean
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val content = buildBackupJsonString(db, settingsStore, includeSettings)
      context.contentResolver.openOutputStream(uri)?.use { out ->
        out.write(content.toByteArray(Charsets.UTF_8))
      } ?: error("Could not open output stream")
      settingsStore.updateSettings { it.copy(lastBackupAt = Instant.now()) }
    }
  }

  suspend fun exportCsvZipToUri(
    context: Context,
    uri: Uri,
    db: HabitRadarDatabase
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val habits = db.habitDao().getAllHabits().map { it.toDomain() }
      val entries = db.entryDao().getAllEntries().map { it.toDomain() }

      context.contentResolver.openOutputStream(uri)?.use { rawOut ->
        writeCsvZipToStream(rawOut, habits, entries)
      } ?: error("Could not open output stream")
    }
  }

  fun writeCsvZipToStream(out: OutputStream, habits: List<Habit>, entries: List<Entry>) {
    ZipOutputStream(out).use { zos ->
      zos.putNextEntry(ZipEntry("habits.csv"))
      val habitsCsv = buildString {
        appendLine("id,name,trackType,targetValue,unit,repeatType,group,status,startDate")
        habits.forEach { h ->
          appendLine("${h.id},\"${h.name.replace("\"", "\"\"")}\",${h.trackType},${h.targetValue ?: ""},${h.unit ?: ""},${h.repeatType},${h.group ?: ""},${h.status},${h.startDate}")
        }
      }
      zos.write(habitsCsv.toByteArray(Charsets.UTF_8))
      zos.closeEntry()

      zos.putNextEntry(ZipEntry("entries.csv"))
      val entriesCsv = buildString {
        appendLine("id,habitId,date,status,value,note,completedAt")
        entries.forEach { e ->
          appendLine("${e.id},${e.habitId},${e.date},${e.status},${e.value ?: ""},\"${(e.note ?: "").replace("\"", "\"\"")}\",${e.completedAt ?: ""}")
        }
      }
      zos.write(entriesCsv.toByteArray(Charsets.UTF_8))
      zos.closeEntry()
    }
  }

  fun validateAndPreviewBackupJson(rawJson: String): Result<BackupPreviewInfo> {
    return runCatching {
      val envelope = jsonFormat.decodeFromString<BackupFileEnvelope>(rawJson)
      if (envelope.format != "habit-radar-backup") {
        error("Invalid backup format: ${envelope.format}")
      }
      if (envelope.schemaVersion > CURRENT_SCHEMA_VERSION) {
        error("Backup was created by a newer version of Habit Radar (schema v${envelope.schemaVersion}).")
      }
      val payloadEncoded = jsonFormat.encodeToString(envelope.data)
      val computedChecksum = sha256Hex(payloadEncoded)
      if (!computedChecksum.equals(envelope.checksum, ignoreCase = true)) {
        error("Backup checksum mismatch — file appears corrupted or modified.")
      }
      BackupPreviewInfo(
        envelope = envelope,
        habitsCount = envelope.data.habits.size,
        entriesCount = envelope.data.entries.size,
        goalsCount = envelope.data.goals.size,
        exportedAt = envelope.exportedAt
      )
    }
  }

  suspend fun applyRestore(
    context: Context,
    db: HabitRadarDatabase,
    settingsStore: SettingsDataStore,
    envelope: BackupFileEnvelope,
    mode: RestoreMode
  ): Result<Int> = withContext(Dispatchers.IO) {
    runCatching {
      if (mode == RestoreMode.REPLACE) {
        // Save a 24h pre-restore safety snapshot
        val snapshotJson = buildBackupJsonString(db, settingsStore, includeSettings = true)
        File(context.filesDir, "pre_restore_snapshot.json").writeText(snapshotJson)
      }

      val payload = envelope.data
      val habits = payload.habits.map { it.toDomain().toEntity() }
      val pauses = payload.habitPauses.map { it.toDomain().toEntity() }
      val entries = payload.entries.map { it.toDomain().toEntity() }
      val reminders = payload.reminders.map { it.toDomain().toEntity() }
      val goals = payload.goals.map { it.toDomain().toEntity() }
      val steps = payload.steps.map { it.toDomain().toEntity() }

      db.withTransaction {
        if (mode == RestoreMode.REPLACE) {
          db.entryDao().deleteAllEntries()
          db.reminderDao().deleteAllReminders()
          db.pauseDao().deleteAllPauses()
          db.goalDao().deleteAllSteps()
          db.goalDao().deleteAllGoals()
          db.habitDao().deleteAllHabits()

          db.habitDao().upsertHabits(habits)
          db.pauseDao().upsertPauses(pauses)
          db.entryDao().upsertEntries(entries)
          db.reminderDao().upsertReminders(reminders)
          db.goalDao().upsertGoals(goals)
          db.goalDao().upsertSteps(steps)
        } else {
          // MERGE mode: add missing habits/goals; for entries on same habitId+date, most recent completedAt wins
          db.habitDao().upsertHabits(habits)
          db.pauseDao().upsertPauses(pauses)
          db.reminderDao().upsertReminders(reminders)
          db.goalDao().upsertGoals(goals)
          db.goalDao().upsertSteps(steps)

          entries.forEach { incoming ->
            val existing = db.entryDao().getEntry(incoming.habitId, incoming.date)
            if (existing == null) {
              db.entryDao().upsertEntry(incoming)
            } else {
              val incTime = incoming.completedAt ?: Instant.MIN
              val exTime = existing.completedAt ?: Instant.MIN
              if (incTime.isAfter(exTime)) {
                db.entryDao().upsertEntry(incoming.copy(id = existing.id))
              }
            }
          }
        }
      }

      ReminderScheduler.rescheduleAll(context)
      WidgetUpdater.updateAll(context)
      habits.size
    }
  }
}

private fun Habit.toDto() = BackupHabitDto(
  id = id,
  name = name,
  description = description,
  iconKey = iconKey,
  colorKey = colorKey,
  trackType = trackType,
  targetValue = targetValue,
  unit = unit,
  comparison = comparison,
  repeatType = repeatType,
  daysOfWeek = daysOfWeek,
  timesPerPeriod = timesPerPeriod,
  daysOfMonth = daysOfMonth,
  group = group,
  category = category,
  tags = tags,
  goalStreak = goalStreak,
  askNote = askNote,
  status = status,
  archivedAtEpochMillis = archivedAt?.toEpochMilli(),
  startDateStr = startDate.toString(),
  endDateStr = endDate?.toString(),
  sortOrder = sortOrder,
  createdAtEpochMillis = createdAt.toEpochMilli()
)

private fun BackupHabitDto.toDomain() = Habit(
  id = id,
  name = name,
  description = description,
  iconKey = iconKey,
  colorKey = colorKey,
  trackType = trackType,
  targetValue = targetValue,
  unit = unit,
  comparison = comparison,
  repeatType = repeatType,
  daysOfWeek = daysOfWeek,
  timesPerPeriod = timesPerPeriod,
  daysOfMonth = daysOfMonth,
  group = group,
  category = category,
  tags = tags,
  goalStreak = goalStreak,
  askNote = askNote,
  status = status,
  archivedAt = archivedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
  startDate = LocalDate.parse(startDateStr),
  endDate = endDateStr?.let { LocalDate.parse(it) },
  sortOrder = sortOrder,
  createdAt = Instant.ofEpochMilli(createdAtEpochMillis)
)

private fun Entry.toDto() = BackupEntryDto(
  id = id,
  habitId = habitId,
  dateStr = date.toString(),
  status = status,
  value = value,
  note = note,
  completedAtEpochMillis = completedAt?.toEpochMilli()
)

private fun BackupEntryDto.toDomain() = Entry(
  id = id,
  habitId = habitId,
  date = LocalDate.parse(dateStr),
  status = status,
  value = value,
  note = note,
  completedAt = completedAtEpochMillis?.let { Instant.ofEpochMilli(it) }
)

private fun Reminder.toDto() = BackupReminderDto(
  id = id,
  habitId = habitId,
  timeStr = time.toString(),
  daysOfWeek = daysOfWeek,
  message = message,
  skipIfDone = skipIfDone,
  repeatAfterMin = repeatAfterMin,
  sound = sound,
  vibrate = vibrate,
  quickActions = quickActions,
  enabled = enabled
)

private fun BackupReminderDto.toDomain() = Reminder(
  id = id,
  habitId = habitId,
  time = LocalTime.parse(timeStr),
  daysOfWeek = daysOfWeek,
  message = message,
  skipIfDone = skipIfDone,
  repeatAfterMin = repeatAfterMin,
  sound = sound,
  vibrate = vibrate,
  quickActions = quickActions,
  enabled = enabled
)

private fun HabitPause.toDto() = BackupPauseDto(
  id = id,
  habitId = habitId,
  startDateStr = startDate.toString(),
  endDateStr = endDate?.toString()
)

private fun BackupPauseDto.toDomain() = HabitPause(
  id = id,
  habitId = habitId,
  startDate = LocalDate.parse(startDateStr),
  endDate = endDateStr?.let { LocalDate.parse(it) }
)

private fun Goal.toDto() = BackupGoalDto(
  id = id,
  title = title,
  description = description,
  iconKey = iconKey,
  colorKey = colorKey,
  startDateStr = startDate.toString(),
  targetDateStr = targetDate?.toString(),
  status = status,
  completedAtEpochMillis = completedAt?.toEpochMilli(),
  sortOrder = sortOrder,
  remindWeekly = remindWeekly,
  weeklyDay = weeklyDay,
  weeklyTimeStr = weeklyTime.toString(),
  remindBeforeDays = remindBeforeDays,
  beforeTimeStr = beforeTime.toString(),
  remindStepDue = remindStepDue
)

private fun BackupGoalDto.toDomain() = Goal(
  id = id,
  title = title,
  description = description,
  iconKey = iconKey,
  colorKey = colorKey,
  startDate = LocalDate.parse(startDateStr),
  targetDate = targetDateStr?.let { LocalDate.parse(it) },
  status = status,
  completedAt = completedAtEpochMillis?.let { Instant.ofEpochMilli(it) },
  sortOrder = sortOrder,
  remindWeekly = remindWeekly,
  weeklyDay = weeklyDay,
  weeklyTime = LocalTime.parse(weeklyTimeStr),
  remindBeforeDays = remindBeforeDays,
  beforeTime = LocalTime.parse(beforeTimeStr),
  remindStepDue = remindStepDue
)

private fun Step.toDto() = BackupStepDto(
  id = id,
  goalId = goalId,
  title = title,
  note = note,
  dueDateStr = dueDate?.toString(),
  sortOrder = sortOrder,
  type = type,
  linkedHabitId = linkedHabitId,
  habitMetric = habitMetric,
  habitTarget = habitTarget,
  habitWindowDays = habitWindowDays,
  amountTarget = amountTarget,
  amountCurrent = amountCurrent,
  amountUnit = amountUnit,
  done = done,
  doneAtEpochMillis = doneAt?.toEpochMilli()
)

private fun BackupStepDto.toDomain() = Step(
  id = id,
  goalId = goalId,
  title = title,
  note = note,
  dueDate = dueDateStr?.let { LocalDate.parse(it) },
  sortOrder = sortOrder,
  type = type,
  linkedHabitId = linkedHabitId,
  habitMetric = habitMetric,
  habitTarget = habitTarget,
  habitWindowDays = habitWindowDays,
  amountTarget = amountTarget,
  amountCurrent = amountCurrent,
  amountUnit = amountUnit,
  done = done,
  doneAt = doneAtEpochMillis?.let { Instant.ofEpochMilli(it) }
)
