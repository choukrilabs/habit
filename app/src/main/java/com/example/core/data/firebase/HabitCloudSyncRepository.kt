package com.example.core.data.firebase

import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.RepeatType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class CloudHabit(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val category: String = "General",
  val iconName: String = "check",
  val colorHex: String = "#A8D8B9",
  val frequencyType: String = "DAILY",
  val targetPerDay: Int = 1,
  @get:PropertyName("isArchived")
  @set:PropertyName("isArchived")
  var isArchived: Boolean = false,
  @ServerTimestamp val createdAt: Date? = null,
  @ServerTimestamp val updatedAt: Date? = null
)

data class CloudEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val habitId: String = "",
  val date: String = "",
  val status: String = "COMPLETED",
  val count: Int = 1,
  val note: String = "",
  @ServerTimestamp val createdAt: Date? = null,
  @ServerTimestamp val updatedAt: Date? = null
)

class HabitCloudSyncRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in")

  fun getHabitsStream(): Flow<List<CloudHabit>> {
    val uid = requireUserId()
    val path = "users/$uid/habits"
    return db.collection(path)
      .snapshots()
      .map { snapshot -> snapshot.documents.mapNotNull { it.toObject<CloudHabit>() } }
      .catch { e -> handleFirestoreError(e, OperationType.LIST, path) }
  }

  fun getEntriesStream(): Flow<List<CloudEntry>> {
    val uid = requireUserId()
    val path = "users/$uid/entries"
    return db.collection(path)
      .snapshots()
      .map { snapshot -> snapshot.documents.mapNotNull { it.toObject<CloudEntry>() } }
      .catch { e -> handleFirestoreError(e, OperationType.LIST, path) }
  }

  suspend fun createHabit(habitId: String, habit: CloudHabit) {
    val uid = requireUserId()
    val safeId = sanitizeFirestoreId(habitId)
    val path = "users/$uid/habits/$safeId"
    try {
      db.document(path).set(habit.copy(id = safeId, userId = uid)).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
    }
  }

  suspend fun updateHabit(
    habitId: String,
    title: String,
    category: String,
    iconName: String,
    colorHex: String,
    frequencyType: String,
    targetPerDay: Int,
    isArchived: Boolean
  ) {
    val uid = requireUserId()
    val safeId = sanitizeFirestoreId(habitId)
    val path = "users/$uid/habits/$safeId"
    try {
      db.document(path).update(
        mapOf(
          "title" to title.take(200).ifBlank { "Habit" },
          "category" to category.take(100).ifBlank { "General" },
          "iconName" to iconName.take(64).ifBlank { "check" },
          "colorHex" to colorHex.take(16).ifBlank { "#A8D8B9" },
          "frequencyType" to frequencyType,
          "targetPerDay" to targetPerDay.coerceIn(1, 1000),
          "isArchived" to isArchived,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
    }
  }

  suspend fun deleteHabit(habitId: String) {
    val uid = requireUserId()
    val safeId = sanitizeFirestoreId(habitId)
    val path = "users/$uid/habits/$safeId"
    try {
      db.document(path).delete().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
    }
  }

  suspend fun createEntry(entryId: String, entry: CloudEntry) {
    val uid = requireUserId()
    val safeId = sanitizeFirestoreId(entryId)
    val path = "users/$uid/entries/$safeId"
    try {
      db.document(path).set(entry.copy(id = safeId, userId = uid)).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
    }
  }

  suspend fun updateEntry(
    entryId: String,
    date: String,
    status: String,
    count: Int,
    note: String
  ) {
    val uid = requireUserId()
    val safeId = sanitizeFirestoreId(entryId)
    val path = "users/$uid/entries/$safeId"
    try {
      db.document(path).update(
        mapOf(
          "date" to date,
          "status" to status,
          "count" to count.coerceIn(0, 10000),
          "note" to note.take(500),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
    }
  }

  suspend fun deleteEntry(entryId: String) {
    val uid = requireUserId()
    val safeId = sanitizeFirestoreId(entryId)
    val path = "users/$uid/entries/$safeId"
    try {
      db.document(path).delete().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
    }
  }

  suspend fun syncDomainHabitIfSignedIn(habit: Habit) {
    val uid = runCatching { auth.currentUser?.uid }.getOrNull() ?: return
    val safeId = sanitizeFirestoreId(habit.id)
    val docRef = db.document("users/$uid/habits/$safeId")
    val freq = when (habit.repeatType) {
      RepeatType.DAILY -> "DAILY"
      RepeatType.WEEKLY -> if (habit.timesPerPeriod != null) "X_PER_PERIOD" else "DAYS_OF_WEEK"
      RepeatType.MONTHLY -> "X_PER_PERIOD"
    }
    val target = (habit.targetValue?.toInt() ?: 1).coerceIn(1, 1000)
    val isArchived = habit.status == HabitStatus.ARCHIVED
    val title = habit.name.take(200).ifBlank { "Habit" }
    val category = (habit.group?.takeIf { it.isNotBlank() } ?: "General").take(100)
    val iconName = habit.iconKey.take(64).ifBlank { "check" }
    val colorHex = habit.colorKey.take(16).ifBlank { "#A8D8B9" }

    runCatching {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        docRef.update(
          mapOf(
            "title" to title,
            "category" to category,
            "iconName" to iconName,
            "colorHex" to colorHex,
            "frequencyType" to freq,
            "targetPerDay" to target,
            "isArchived" to isArchived,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        val cloudHabit = CloudHabit(
          id = safeId,
          userId = uid,
          title = title,
          category = category,
          iconName = iconName,
          colorHex = colorHex,
          frequencyType = freq,
          targetPerDay = target,
          isArchived = isArchived
        )
        docRef.set(cloudHabit, SetOptions.merge()).await()
      }
    }
  }

  suspend fun syncDomainEntryIfSignedIn(entry: Entry) {
    val uid = runCatching { auth.currentUser?.uid }.getOrNull() ?: return
    val safeId = sanitizeFirestoreId("${entry.habitId}_${entry.date}")
    val docRef = db.document("users/$uid/entries/$safeId")
    val cloudStatus = when (entry.status) {
      EntryStatus.DONE -> "COMPLETED"
      EntryStatus.SKIPPED -> "SKIPPED"
      EntryStatus.MISSED -> "MISSED"
    }
    val count = (entry.value?.toInt() ?: 1).coerceIn(0, 10000)
    val note = (entry.note ?: "").take(500)

    runCatching {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        docRef.update(
          mapOf(
            "date" to entry.date.toString(),
            "status" to cloudStatus,
            "count" to count,
            "note" to note,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        val cloudEntry = CloudEntry(
          id = safeId,
          userId = uid,
          habitId = sanitizeFirestoreId(entry.habitId),
          date = entry.date.toString(),
          status = cloudStatus,
          count = count,
          note = note
        )
        docRef.set(cloudEntry, SetOptions.merge()).await()
      }
    }
  }

  suspend fun deleteDomainEntryIfSignedIn(habitId: String, dateIso: String) {
    val uid = runCatching { auth.currentUser?.uid }.getOrNull() ?: return
    val safeId = sanitizeFirestoreId("${habitId}_$dateIso")
    runCatching {
      db.document("users/$uid/entries/$safeId").delete().await()
    }
  }

  suspend fun deleteDomainHabitIfSignedIn(habitId: String) {
    val uid = runCatching { auth.currentUser?.uid }.getOrNull() ?: return
    val safeId = sanitizeFirestoreId(habitId)
    runCatching {
      db.document("users/$uid/habits/$safeId").delete().await()
    }
  }

  suspend fun syncAllToCloud(habits: List<Habit>, entries: List<Entry>): Result<Int> {
    val uid = runCatching { auth.currentUser?.uid }.getOrNull()
      ?: return Result.failure(IllegalStateException("Sign in with Google to enable cloud backup."))
    return runCatching {
      habits.forEach { syncDomainHabitIfSignedIn(it) }
      entries.forEach { syncDomainEntryIfSignedIn(it) }
      habits.size
    }
  }

  companion object {
    fun sanitizeFirestoreId(raw: String): String {
      val cleaned = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(128)
      return cleaned.ifEmpty { "doc_1" }
    }
  }
}
