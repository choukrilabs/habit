package com.example.core.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.core.domain.model.HabitStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
  @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
  fun observeAllHabits(): Flow<List<HabitEntity>>

  @Query("SELECT * FROM habits WHERE status = :status ORDER BY sortOrder ASC, createdAt ASC")
  fun observeHabitsByStatus(status: HabitStatus = HabitStatus.ACTIVE): Flow<List<HabitEntity>>

  @Query("SELECT * FROM habits WHERE groupName = :groupName AND status = 'ACTIVE' ORDER BY sortOrder ASC")
  fun observeActiveHabitsByGroup(groupName: String): Flow<List<HabitEntity>>

  @Query("SELECT * FROM habits WHERE category = :category AND status = 'ACTIVE' ORDER BY sortOrder ASC")
  fun observeActiveHabitsByCategory(category: String): Flow<List<HabitEntity>>

  @Query("SELECT * FROM habits WHERE category = :category AND status = 'ACTIVE' ORDER BY sortOrder ASC")
  suspend fun getActiveHabitsByCategory(category: String): List<HabitEntity>

  @Transaction
  @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
  fun observeAllHabitsWithDetails(): Flow<List<HabitWithLogsAndDetails>>

  @Transaction
  @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
  fun observeHabitWithDetails(id: String): Flow<HabitWithLogsAndDetails?>

  @Transaction
  @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
  suspend fun getHabitWithDetails(id: String): HabitWithLogsAndDetails?

  @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
  suspend fun getAllHabits(): List<HabitEntity>

  @Query("SELECT * FROM habits WHERE status = :status ORDER BY sortOrder ASC, createdAt ASC")
  suspend fun getHabitsByStatus(status: HabitStatus = HabitStatus.ACTIVE): List<HabitEntity>

  @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
  fun observeHabitById(id: String): Flow<HabitEntity?>

  @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
  suspend fun getHabitById(id: String): HabitEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHabit(habit: HabitEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHabits(habits: List<HabitEntity>)

  @Update
  suspend fun updateHabit(habit: HabitEntity)

  @Query("DELETE FROM habits WHERE id = :id")
  suspend fun deleteHabitById(id: String)

  @Query("DELETE FROM habits")
  suspend fun deleteAllHabits()
}

@Dao
interface EntryDao {
  @Query("SELECT * FROM entries ORDER BY date DESC")
  fun observeAllEntries(): Flow<List<EntryEntity>>

  @Query("SELECT * FROM entries ORDER BY date DESC")
  suspend fun getAllEntries(): List<EntryEntity>

  @Query("SELECT * FROM entries WHERE habitId = :habitId ORDER BY date DESC")
  fun observeEntriesForHabit(habitId: String): Flow<List<EntryEntity>>

  @Query("SELECT * FROM entries WHERE habitId = :habitId ORDER BY date DESC")
  suspend fun getEntriesForHabit(habitId: String): List<EntryEntity>

  @Query("SELECT * FROM entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
  fun observeEntriesInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<EntryEntity>>

  @Query("SELECT * FROM entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
  suspend fun getEntriesInDateRange(startDate: LocalDate, endDate: LocalDate): List<EntryEntity>

  @Query("SELECT * FROM entries WHERE habitId = :habitId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
  fun observeEntriesForHabitInDateRange(
    habitId: String,
    startDate: LocalDate,
    endDate: LocalDate
  ): Flow<List<EntryEntity>>

  @Query("SELECT * FROM entries WHERE habitId = :habitId AND date = :date LIMIT 1")
  fun observeEntry(habitId: String, date: LocalDate): Flow<EntryEntity?>

  @Query("SELECT * FROM entries WHERE habitId = :habitId AND date = :date LIMIT 1")
  suspend fun getEntry(habitId: String, date: LocalDate): EntryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertEntry(entry: EntryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertEntries(entries: List<EntryEntity>)

  @Query("SELECT COUNT(*) FROM entries WHERE status = 'DONE' AND date >= :startDate AND date <= :endDate")
  fun observeCompletedEntryCountInRange(startDate: LocalDate, endDate: LocalDate): Flow<Int>

  @Query("SELECT COUNT(*) FROM entries WHERE status = 'DONE' AND date >= :startDate AND date <= :endDate")
  suspend fun getCompletedEntryCountInRange(startDate: LocalDate, endDate: LocalDate): Int

  @Query("DELETE FROM entries WHERE habitId = :habitId AND date = :date")
  suspend fun deleteEntry(habitId: String, date: LocalDate)

  @Query("DELETE FROM entries WHERE id = :id")
  suspend fun deleteEntryById(id: String)

  @Query("DELETE FROM entries")
  suspend fun deleteAllEntries()
}

@Dao
interface ReminderDao {
  @Query("SELECT * FROM reminders ORDER BY time ASC")
  fun observeAllReminders(): Flow<List<ReminderEntity>>

  @Query("SELECT * FROM reminders ORDER BY time ASC")
  suspend fun getAllReminders(): List<ReminderEntity>

  @Query("SELECT * FROM reminders WHERE enabled = 1 ORDER BY time ASC")
  fun observeEnabledReminders(): Flow<List<ReminderEntity>>

  @Query("SELECT * FROM reminders WHERE habitId = :habitId ORDER BY time ASC")
  fun observeRemindersForHabit(habitId: String): Flow<List<ReminderEntity>>

  @Query("SELECT * FROM reminders WHERE habitId = :habitId ORDER BY time ASC")
  suspend fun getRemindersForHabit(habitId: String): List<ReminderEntity>

  @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
  suspend fun getReminderById(id: String): ReminderEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertReminder(reminder: ReminderEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertReminders(reminders: List<ReminderEntity>)

  @Query("DELETE FROM reminders WHERE id = :id")
  suspend fun deleteReminderById(id: String)

  @Query("DELETE FROM reminders WHERE habitId = :habitId")
  suspend fun deleteRemindersForHabit(habitId: String)

  @Query("DELETE FROM reminders")
  suspend fun deleteAllReminders()
}

@Dao
interface PauseDao {
  @Query("SELECT * FROM habit_pauses ORDER BY startDate DESC")
  fun observeAllPauses(): Flow<List<HabitPauseEntity>>

  @Query("SELECT * FROM habit_pauses ORDER BY startDate DESC")
  suspend fun getAllPauses(): List<HabitPauseEntity>

  @Query("SELECT * FROM habit_pauses WHERE habitId = :habitId ORDER BY startDate DESC")
  fun observePausesForHabit(habitId: String): Flow<List<HabitPauseEntity>>

  @Query("SELECT * FROM habit_pauses WHERE habitId = :habitId ORDER BY startDate DESC")
  suspend fun getPausesForHabit(habitId: String): List<HabitPauseEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPause(pause: HabitPauseEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPauses(pauses: List<HabitPauseEntity>)

  @Query("DELETE FROM habit_pauses WHERE id = :id")
  suspend fun deletePauseById(id: String)

  @Query("DELETE FROM habit_pauses")
  suspend fun deleteAllPauses()
}

@Dao
interface GoalDao {
  @Query("SELECT * FROM goals ORDER BY sortOrder ASC, startDate DESC")
  fun observeAllGoals(): Flow<List<GoalEntity>>

  @Query("SELECT * FROM goals ORDER BY sortOrder ASC, startDate DESC")
  suspend fun getAllGoals(): List<GoalEntity>

  @Transaction
  @Query("SELECT * FROM goals ORDER BY sortOrder ASC, startDate DESC")
  fun observeAllGoalsWithSteps(): Flow<List<GoalWithSteps>>

  @Transaction
  @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
  suspend fun getGoalWithSteps(id: String): GoalWithSteps?

  @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
  fun observeGoalById(id: String): Flow<GoalEntity?>

  @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
  suspend fun getGoalById(id: String): GoalEntity?

  @Query("SELECT * FROM steps ORDER BY sortOrder ASC")
  fun observeAllSteps(): Flow<List<StepEntity>>

  @Query("SELECT * FROM steps ORDER BY sortOrder ASC")
  suspend fun getAllSteps(): List<StepEntity>

  @Query("SELECT * FROM steps WHERE goalId = :goalId ORDER BY sortOrder ASC")
  fun observeStepsForGoal(goalId: String): Flow<List<StepEntity>>

  @Query("SELECT * FROM steps WHERE goalId = :goalId ORDER BY sortOrder ASC")
  suspend fun getStepsForGoal(goalId: String): List<StepEntity>

  @Query("SELECT * FROM steps WHERE linkedHabitId = :habitId")
  suspend fun getStepsForHabit(habitId: String): List<StepEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertGoal(goal: GoalEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertGoals(goals: List<GoalEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertStep(step: StepEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSteps(steps: List<StepEntity>)

  @Query("DELETE FROM goals WHERE id = :id")
  suspend fun deleteGoalById(id: String)

  @Query("DELETE FROM steps WHERE id = :id")
  suspend fun deleteStepById(id: String)

  @Query("DELETE FROM steps WHERE goalId = :goalId")
  suspend fun deleteStepsForGoal(goalId: String)

  @Query("DELETE FROM goals")
  suspend fun deleteAllGoals()

  @Query("DELETE FROM steps")
  suspend fun deleteAllSteps()
}

@Dao
interface WidgetConfigDao {
  @Query("SELECT * FROM widget_configs")
  fun observeAllConfigs(): Flow<List<WidgetConfigEntity>>

  @Query("SELECT * FROM widget_configs")
  suspend fun getAllConfigs(): List<WidgetConfigEntity>

  @Query("SELECT * FROM widget_configs WHERE appWidgetId = :appWidgetId LIMIT 1")
  suspend fun getConfig(appWidgetId: Int): WidgetConfigEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertConfig(config: WidgetConfigEntity)

  @Query("DELETE FROM widget_configs WHERE appWidgetId = :appWidgetId")
  suspend fun deleteConfig(appWidgetId: Int)
}

@Database(
  entities = [
    HabitEntity::class,
    ReminderEntity::class,
    EntryEntity::class,
    HabitPauseEntity::class,
    GoalEntity::class,
    StepEntity::class,
    WidgetConfigEntity::class
  ],
  version = 3,
  exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitRadarDatabase : RoomDatabase() {
  abstract fun habitDao(): HabitDao
  abstract fun entryDao(): EntryDao
  abstract fun reminderDao(): ReminderDao
  abstract fun pauseDao(): PauseDao
  abstract fun goalDao(): GoalDao
  abstract fun widgetConfigDao(): WidgetConfigDao

  companion object {
    @Volatile
    private var INSTANCE: HabitRadarDatabase? = null

    val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
      override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_status_sortOrder` ON `habits` (`status`, `sortOrder`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_groupName` ON `habits` (`groupName`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_enabled` ON `reminders` (`enabled`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_pauses_habitId_startDate_endDate` ON `habit_pauses` (`habitId`, `startDate`, `endDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_status_sortOrder` ON `goals` (`status`, `sortOrder`)")
      }
    }

    val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
      override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `habits` ADD COLUMN `category` TEXT")
        db.execSQL("ALTER TABLE `habits` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_category` ON `habits` (`category`)")
      }
    }

    fun getInstance(context: Context): HabitRadarDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          HabitRadarDatabase::class.java,
          "habit_radar.db"
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
          .fallbackToDestructiveMigration(dropAllTables = true)
          .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}

typealias AppDatabase = HabitRadarDatabase

