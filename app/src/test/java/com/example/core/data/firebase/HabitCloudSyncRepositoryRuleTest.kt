package com.example.core.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class HabitCloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private lateinit var repository: HabitCloudSyncRepository

  @Before
  override fun setUp() {
    super.setUp()
    repository = HabitCloudSyncRepository(db, auth)
  }

  @Test
  fun createListUpdateAndDeleteHabit_whenAuthenticatedOwner_succeeds() = runEmulatorTest {
    val uid = signInTestUser("owner@habitradar.test")

    val habit = CloudHabit(
      id = "habit_meditate",
      userId = uid,
      title = "Morning Meditation",
      category = "Mindfulness",
      iconName = "self_improvement",
      colorHex = "#B6A6E8",
      frequencyType = "DAILY",
      targetPerDay = 1,
      isArchived = false
    )

    repository.createHabit("habit_meditate", habit)

    val list = repository.getHabitsStream().first { it.isNotEmpty() }
    assertEquals(1, list.size)
    assertEquals("Morning Meditation", list.first().title)
    assertEquals(uid, list.first().userId)

    repository.updateHabit(
      habitId = "habit_meditate",
      title = "Evening Meditation",
      category = "Mindfulness",
      iconName = "self_improvement",
      colorHex = "#B6A6E8",
      frequencyType = "DAILY",
      targetPerDay = 2,
      isArchived = false
    )

    val updatedList = repository.getHabitsStream().first { it.firstOrNull()?.title == "Evening Meditation" }
    assertEquals(2, updatedList.first().targetPerDay)

    repository.deleteHabit("habit_meditate")
    val emptyList = repository.getHabitsStream().first { it.isEmpty() }
    assertTrue(emptyList.isEmpty())
  }

  @Test
  fun createAndUpdateEntry_whenAuthenticatedOwner_succeeds() = runEmulatorTest {
    val uid = signInTestUser("entryowner@habitradar.test")

    val entry = CloudEntry(
      id = "entry_1",
      userId = uid,
      habitId = "habit_meditate",
      date = "2026-10-25",
      status = "COMPLETED",
      count = 1,
      note = "Great session"
    )

    repository.createEntry("entry_1", entry)

    val entries = repository.getEntriesStream().first { it.isNotEmpty() }
    assertEquals(1, entries.size)
    assertEquals("2026-10-25", entries.first().date)

    repository.updateEntry(
      entryId = "entry_1",
      date = "2026-10-25",
      status = "COMPLETED",
      count = 2,
      note = "Updated note"
    )

    val updated = repository.getEntriesStream().first { it.firstOrNull()?.count == 2 }
    assertEquals("Updated note", updated.first().note)

    repository.deleteEntry("entry_1")
  }

  @Test
  fun createHabit_whenUnauthenticated_throwsIllegalStateException() = runEmulatorTest {
    auth.signOut()
    try {
      repository.createHabit(
        "habit_unauthed",
        CloudHabit(title = "Should Fail")
      )
      fail("Expected IllegalStateException when unauthenticated")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("signed in"))
    }
  }
}
