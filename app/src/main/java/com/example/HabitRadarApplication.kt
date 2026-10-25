package com.example

import android.app.Application
import com.example.core.di.AppContainer
import com.example.core.notifications.DailyMaintenanceWorker
import com.example.core.notifications.NotificationChannels

class HabitRadarApplication : Application() {

  lateinit var container: AppContainer

  fun isContainerInitialized(): Boolean = ::container.isInitialized

  override fun onCreate() {
    super.onCreate()
    if (!::container.isInitialized) {
      container = AppContainer(this)
    }
    runCatching {
      NotificationChannels.ensureCreated(this)
      DailyMaintenanceWorker.enqueuePeriodic(this)
    }
  }
}
