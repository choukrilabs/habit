package com.example.core.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.HabitRadarApplication
import com.example.R
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.AppDatabase
import com.example.core.data.db.EntryDao
import com.example.core.data.db.GoalDao
import com.example.core.data.db.HabitDao
import com.example.core.data.db.HabitRadarDatabase
import com.example.core.data.db.PauseDao
import com.example.core.data.db.ReminderDao
import com.example.core.data.db.WidgetConfigDao
import com.example.core.data.firebase.AuthService
import com.example.core.data.firebase.AuthenticationService
import com.example.core.data.firebase.HabitCloudSyncRepository
import com.example.core.data.repository.HabitRadarRepository
import com.example.core.notifications.ReminderScheduler
import com.example.core.util.SoundFeedbackManager
import com.example.feature.backup.BackupManager
import com.example.feature.widgets.glance.WidgetUpdater
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Manual Dependency Injection container that initializes and provides application-scoped
 * instances of the Room database ([HabitRadarDatabase] / [AppDatabase]), DAOs ([HabitDao], etc.),
 * habit repository ([HabitRadarRepository] and [HabitCloudSyncRepository]), and authentication
 * service ([AuthService] / [AuthenticationService]) across the application without Hilt.
 */
open class AppContainer(
  context: Context,
  customDatabase: HabitRadarDatabase? = null,
  customSettingsDataStore: SettingsDataStore? = null,
  customFirestore: FirebaseFirestore? = null,
  customAuthService: AuthService? = null
) {

  open val applicationContext: Context = context.applicationContext

  private fun ensureFirebaseInitialized() {
    runCatching {
      if (FirebaseApp.getApps(applicationContext).isEmpty()) {
        FirebaseApp.initializeApp(applicationContext)
      }
    }
  }

  open val database: HabitRadarDatabase by lazy {
    customDatabase ?: HabitRadarDatabase.getInstance(applicationContext)
  }

  open val appDatabase: AppDatabase
    get() = database

  open val habitDao: HabitDao by lazy { database.habitDao() }
  open val entryDao: EntryDao by lazy { database.entryDao() }
  open val reminderDao: ReminderDao by lazy { database.reminderDao() }
  open val pauseDao: PauseDao by lazy { database.pauseDao() }
  open val goalDao: GoalDao by lazy { database.goalDao() }
  open val widgetConfigDao: WidgetConfigDao by lazy { database.widgetConfigDao() }

  open val settingsDataStore: SettingsDataStore by lazy {
    customSettingsDataStore ?: SettingsDataStore(applicationContext)
  }

  open val authService: AuthService by lazy {
    ensureFirebaseInitialized()
    customAuthService ?: AuthService(applicationContext)
  }

  open val authenticationService: AuthenticationService
    get() = authService

  open val firestore: FirebaseFirestore by lazy {
    ensureFirebaseInitialized()
    customFirestore
      ?: FirebaseFirestore.getInstance(
        applicationContext.getString(R.string.firestore_database_id)
      )
  }

  open val cloudSyncRepository: HabitCloudSyncRepository by lazy {
    ensureFirebaseInitialized()
    HabitCloudSyncRepository(
      db = firestore,
      auth = authService.firebaseAuth ?: FirebaseAuth.getInstance()
    )
  }

  open val repository: HabitRadarRepository by lazy {
    HabitRadarRepository(
      context = applicationContext,
      db = database,
      settingsDataStore = settingsDataStore,
      cloudSyncRepository = runCatching { cloudSyncRepository }.getOrNull(),
      onDataChangedCallback = {
        ReminderScheduler.rescheduleAll(applicationContext)
        WidgetUpdater.updateAll(applicationContext)
      }
    )
  }

  open val habitRepository: HabitRadarRepository
    get() = repository

  open val backupManager: BackupManager = BackupManager

  open val soundFeedbackManager: SoundFeedbackManager = SoundFeedbackManager
}

/**
 * Default concrete subclass of [AppContainer] preserved for backwards compatibility.
 */
class DefaultAppContainer(
  context: Context,
  customDatabase: HabitRadarDatabase? = null,
  customSettingsDataStore: SettingsDataStore? = null,
  customFirestore: FirebaseFirestore? = null,
  customAuthService: AuthService? = null
) : AppContainer(
  context = context,
  customDatabase = customDatabase,
  customSettingsDataStore = customSettingsDataStore,
  customFirestore = customFirestore,
  customAuthService = customAuthService
)

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
  error("No AppContainer provided in CompositionLocal")
}

private var fallbackContainer: AppContainer? = null
private val fallbackLock = Any()

/**
 * Retrieves the application's [AppContainer] from [HabitRadarApplication], or lazily
 * initializes a thread-safe fallback container for isolated BroadcastReceivers / tests.
 */
val Context.appContainer: AppContainer
  get() {
    val app = applicationContext
    if (app is HabitRadarApplication && app.isContainerInitialized()) {
      return app.container
    }
    return synchronized(fallbackLock) {
      fallbackContainer ?: AppContainer(app).also { fallbackContainer = it }
    }
  }
