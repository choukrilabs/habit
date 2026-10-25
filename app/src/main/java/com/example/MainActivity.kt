package com.example

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.core.data.datastore.SettingsDataStore
import com.example.core.data.db.HabitRadarDatabase
import com.example.core.data.repository.HabitRadarRepository
import com.example.core.domain.model.AppSettings
import com.example.core.domain.model.Goal
import com.example.core.domain.model.GoalStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitStatus
import com.example.core.domain.model.HomeTab
import com.example.core.domain.model.Step
import com.example.core.domain.model.TrackType
import com.example.core.domain.usecase.LogicalDateCalculator
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.notifications.DailyMaintenanceWorker
import com.example.core.notifications.NotificationChannels
import com.example.core.notifications.ReminderScheduler
import com.example.core.ui.components.FloatingPillTabBar
import com.example.core.ui.components.FullScreenCelebrationOverlay
import com.example.core.ui.theme.HabitRadarTheme
import com.example.core.util.SoundFeedbackManager
import com.example.feature.extras.BackupRestoreScreen
import com.example.feature.extras.OnboardingScreen
import com.example.feature.extras.WidgetsStudioScreen
import com.example.feature.habit.EditHabitBottomSheet
import com.example.feature.habit.TemplateGalleryBottomSheet
import com.example.feature.habitdetail.HabitDetailScreen
import com.example.feature.milestones.EditGoalBottomSheet
import com.example.feature.milestones.GoalDetailScreen
import com.example.feature.milestones.MilestonesListScreen
import com.example.feature.onboarding.StarterTemplatesCatalog
import com.example.feature.recap.MonthlyRecapScreen
import com.example.feature.recap.WeeklyRecapScreen
import com.example.feature.recap.YearInReviewScreen
import com.example.feature.settings.ArchivedHabitsScreen
import com.example.feature.settings.HomeStudioScreen
import com.example.feature.settings.MenuScreen
import com.example.feature.settings.NotificationsScreen
import com.example.feature.settings.PauseHabitBottomSheet
import com.example.feature.settings.ReorderHabitsScreen
import com.example.feature.settings.ReorderTabsScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.tabs.MonthlyTabScreen
import com.example.feature.tabs.OverallTabScreen
import com.example.feature.tabs.TodayTabScreen
import com.example.feature.tabs.WeeklyTabScreen
import com.example.feature.widgets.glance.SingleHabitWidgetReceiver
import com.example.feature.widgets.glance.WidgetUpdater
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.di.LocalAppContainer
import com.example.core.di.appContainer
import com.example.core.ui.viewmodel.HabitRadarViewModelFactory
import com.example.core.ui.viewmodel.MainAppViewModel
import java.time.LocalTime
import kotlinx.coroutines.launch

sealed interface ScreenRoute {
  data object HomeTabs : ScreenRoute
  data object Menu : ScreenRoute
  data object Settings : ScreenRoute
  data object HomeStudio : ScreenRoute
  data object Notifications : ScreenRoute
  data object ReorderHabits : ScreenRoute
  data object ReorderTabs : ScreenRoute
  data object ArchivedHabits : ScreenRoute
  data object WeeklyRecap : ScreenRoute
  data object MonthlyRecap : ScreenRoute
  data object YearInReview : ScreenRoute
  data object MilestonesList : ScreenRoute
  data class GoalDetail(val goalId: String) : ScreenRoute
  data object WidgetsStudio : ScreenRoute
  data object BackupRestore : ScreenRoute
  data class HabitDetail(val habitId: String) : ScreenRoute
  data object WelcomeTour : ScreenRoute
}

class MainActivity : ComponentActivity() {

  private var deepLinkUriState = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    NotificationChannels.ensureCreated(this)
    DailyMaintenanceWorker.enqueuePeriodic(this)

    val container = appContainer
    deepLinkUriState.value = intent?.data?.toString()

    setContent {
      CompositionLocalProvider(LocalAppContainer provides container) {
        val mainViewModel: MainAppViewModel = viewModel(
          factory = HabitRadarViewModelFactory(container)
        )
        val currentUser by container.authService.authStateFlow()
          .collectAsState(initial = container.authService.currentUser)

        if (currentUser == null) {
          val settings by mainViewModel.settingsFlow.collectAsState()
          HabitRadarTheme(
            themeMode = settings.theme,
            accentHex = settings.accentColor,
            cardDensity = settings.cardDensity,
            todayLayout = settings.todayLayout
          ) {
            com.example.feature.auth.GoogleAuthScreen(
              authService = container.authService
            )
          }
        } else {
          HabitRadarAppRoot(
            viewModel = mainViewModel,
            repository = container.habitRepository,
            settingsDataStore = container.settingsDataStore,
            deepLinkUri = deepLinkUriState.value,
            onConsumeDeepLink = { deepLinkUriState.value = null }
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    deepLinkUriState.value = intent.data?.toString()
  }
}

@Composable
fun HabitRadarAppRoot(
  repository: HabitRadarRepository,
  settingsDataStore: SettingsDataStore,
  deepLinkUri: String?,
  onConsumeDeepLink: () -> Unit,
  viewModel: MainAppViewModel = viewModel(
    factory = HabitRadarViewModelFactory(
      LocalContext.current.appContainer
    )
  )
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val settings by viewModel.settingsFlow.collectAsState()
  val allHabits by viewModel.allHabitsFlow.collectAsState()
  val allEntries by viewModel.allEntriesFlow.collectAsState()
  val allReminders by viewModel.allRemindersFlow.collectAsState()
  val allPauses by viewModel.allPausesFlow.collectAsState()
  val allGoals by viewModel.allGoalsFlow.collectAsState()
  val allSteps by viewModel.allStepsFlow.collectAsState()

  val today = remember(settings.dayRollover) {
    LogicalDateCalculator.now(settings.dayRollover)
  }

  val activeHabits = remember(allHabits) {
    allHabits.filter { it.status == HabitStatus.ACTIVE }.sortedBy { it.sortOrder }
  }
  val archivedHabits = remember(allHabits) {
    allHabits.filter { it.status == HabitStatus.ARCHIVED }
  }
  val habitsMap = remember(allHabits) { allHabits.associateBy { it.id } }

  val entriesListByHabit = remember(allEntries) { allEntries.groupBy { it.habitId } }
  val entriesByHabit = remember(allEntries) {
    allEntries.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.date } }
  }
  val pausesByHabit = remember(allPauses) { allPauses.groupBy { it.habitId } }
  val remindersByHabit = remember(allReminders) { allReminders.groupBy { it.habitId } }
  val stepsByGoal = remember(allSteps) { allSteps.groupBy { it.goalId } }

  val navStack = remember { mutableStateListOf<ScreenRoute>(ScreenRoute.HomeTabs) }
  val currentRoute = navStack.lastOrNull() ?: ScreenRoute.HomeTabs

  fun navigateTo(route: ScreenRoute) {
    if (navStack.lastOrNull() != route) {
      navStack.add(route)
    }
  }

  fun popBack() {
    if (navStack.size > 1) {
      navStack.removeAt(navStack.lastIndex)
    }
  }

  var selectedTab by remember { mutableStateOf(HomeTab.TODAY) }

  // Modal bottom sheets & dialogs state
  var editingHabit by remember { mutableStateOf<Habit?>(null) }
  var showEditHabitSheet by remember { mutableStateOf(false) }
  var showTemplateGallerySheet by remember { mutableStateOf(false) }
  var pausingHabit by remember { mutableStateOf<Habit?>(null) }
  var deletingHabitConfirm by remember { mutableStateOf<Habit?>(null) }

  var showEditGoalSheet by remember { mutableStateOf(false) }
  var editingGoal by remember { mutableStateOf<Goal?>(null) }
  var editingGoalInitialSteps by remember { mutableStateOf<List<Step>>(emptyList()) }
  var prelinkedHabitForGoal by remember { mutableStateOf<Habit?>(null) }

  // Celebration overlay state
  var celebrationHeadline by remember { mutableStateOf<String?>(null) }
  var celebrationSubtitle by remember { mutableStateOf<String?>(null) }

  // Handle deep links (Section 7.6 & 8)
  LaunchedEffect(deepLinkUri) {
    val uri = deepLinkUri ?: return@LaunchedEffect
    when {
      uri.startsWith("habitradar://today") -> {
        navStack.clear()
        navStack.add(ScreenRoute.HomeTabs)
        selectedTab = HomeTab.TODAY
      }
      uri.startsWith("habitradar://habit/") -> {
        val hId = uri.removePrefix("habitradar://habit/").substringBefore("?")
        if (hId.isNotBlank()) {
          navigateTo(ScreenRoute.HabitDetail(hId))
        }
      }
      uri.startsWith("habitradar://goal/") -> {
        val gId = uri.removePrefix("habitradar://goal/").substringBefore("?")
        if (gId.isNotBlank()) {
          navigateTo(ScreenRoute.GoalDetail(gId))
        }
      }
      uri.startsWith("habitradar://milestones") -> {
        navigateTo(ScreenRoute.MilestonesList)
      }
      uri.startsWith("habitradar://recap/weekly") -> {
        navigateTo(ScreenRoute.WeeklyRecap)
      }
      uri.startsWith("habitradar://recap/monthly") -> {
        navigateTo(ScreenRoute.MonthlyRecap)
      }
      uri.startsWith("habitradar://recap/yearly") -> {
        navigateTo(ScreenRoute.YearInReview)
      }
    }
    onConsumeDeepLink()
  }

  val updateSettings: ((AppSettings) -> AppSettings) -> Unit = { transform ->
    scope.launch {
      settingsDataStore.updateSettings(transform)
      ReminderScheduler.rescheduleAll(context)
      WidgetUpdater.updateAll(context)
    }
  }

  fun checkAllTodayDoneAndCelebrate(justCompletedHabitId: String) {
    if (!settings.fullScreenCelebrations) return
    val scheduledToday = activeHabits.filter { h ->
      !ScheduleMatcher.isPausedOn(today, pausesByHabit[h.id].orEmpty()) &&
        ScheduleMatcher.isScheduledOn(h, today, pausesByHabit[h.id].orEmpty())
    }
    if (scheduledToday.isEmpty()) return
    val allDone = scheduledToday.all { h ->
      if (h.id == justCompletedHabitId) true
      else ScheduleMatcher.isEntryDone(h, entriesByHabit[h.id]?.get(today))
    }
    if (allDone) {
      celebrationHeadline = "All *done.*"
      celebrationSubtitle = "Every scheduled habit for today is complete."
    }
  }

  HabitRadarTheme(
    themeMode = settings.theme,
    accentHex = settings.accentColor,
    cardDensity = settings.cardDensity,
    todayLayout = settings.todayLayout
  ) {
    if (!settings.onboardingDone || currentRoute is ScreenRoute.WelcomeTour) {
      BackHandler(enabled = currentRoute is ScreenRoute.WelcomeTour) {
        popBack()
      }
      OnboardingScreen(
        settings = settings,
        today = today,
        onCompleteOnboarding = { chosenTemplates, enableRems, weekStart, rolloverHour, showHijri ->
          scope.launch {
            settingsDataStore.updateSettings {
              it.copy(
                onboardingDone = true,
                weekStart = weekStart,
                dayRollover = LocalTime.of(rolloverHour, 0),
                showHijri = showHijri
              )
            }
            val startIdx = allHabits.size
            chosenTemplates.forEachIndexed { idx, tpl ->
              val (habit, rem) = StarterTemplatesCatalog.toHabitAndOptionalReminder(
                template = tpl,
                sortOrder = startIdx + idx,
                today = today,
                enableReminder = enableRems
              )
              repository.saveHabit(habit, listOfNotNull(rem))
            }
            if (currentRoute is ScreenRoute.WelcomeTour) {
              popBack()
            }
          }
        }
      )
    } else {
      BackHandler(enabled = navStack.size > 1) {
        popBack()
      }

      Box(modifier = Modifier.fillMaxSize()) {
        when (val route = currentRoute) {
          ScreenRoute.HomeTabs -> {
            val visibleTabs = remember(settings.tabOrder, settings.hiddenTabs) {
              val filtered = settings.tabOrder.filter { it !in settings.hiddenTabs }
              if (filtered.isEmpty()) listOf(HomeTab.TODAY) else filtered
            }
            val activeTab = if (selectedTab in visibleTabs) selectedTab else HomeTab.TODAY

            when (activeTab) {
              HomeTab.TODAY -> {
                TodayTabScreen(
                  habits = activeHabits,
                  entriesByHabit = entriesByHabit,
                  entriesListByHabit = entriesListByHabit,
                  pausesByHabit = pausesByHabit,
                  settings = settings,
                  today = today,
                  onOpenMenu = { navigateTo(ScreenRoute.Menu) },
                  onNewHabit = {
                    editingHabit = null
                    showEditHabitSheet = true
                  },
                  onOpenHabitDetail = { hId -> navigateTo(ScreenRoute.HabitDetail(hId)) },
                  onToggleHabitToday = { habit, note ->
                    scope.launch {
                      val nowDone = repository.toggleTaskEntry(habit.id, today, note)
                      if (nowDone) {
                        if (settings.sounds) SoundFeedbackManager.playSoftTick(true)
                        checkAllTodayDoneAndCelebrate(habit.id)
                      }
                    }
                  },
                  onLogQuantitativeToday = { habit, value, note ->
                    scope.launch {
                      repository.setQuantitativeEntry(habit.id, today, value, note)
                      if (value >= (habit.targetValue ?: 1.0)) {
                        if (settings.sounds) SoundFeedbackManager.playSoftTick(true)
                        checkAllTodayDoneAndCelebrate(habit.id)
                      }
                    }
                  },
                  onSkipToday = { habit ->
                    scope.launch { repository.skipHabitOnDate(habit.id, today) }
                  },
                  onEditHabit = { habit ->
                    editingHabit = habit
                    showEditHabitSheet = true
                  },
                  onPauseHabitPrompt = { habit -> pausingHabit = habit },
                  onResumeHabit = { habit ->
                    scope.launch { repository.resumeHabit(habit.id) }
                  },
                  onArchiveHabit = { habit ->
                    scope.launch { repository.archiveHabit(habit.id) }
                  },
                  onReorderHabits = { navigateTo(ScreenRoute.ReorderHabits) },
                  onDeleteHabit = { habit -> deletingHabitConfirm = habit },
                  onSelectGroupFilter = { grp ->
                    updateSettings { it.copy(groupFilter = grp) }
                  },
                  onBrowseIdeas = { showTemplateGallerySheet = true },
                  onDismissCoachmark = {
                    updateSettings { it.copy(coachmarkShown = true) }
                  }
                )
              }
              HomeTab.WEEKLY -> {
                WeeklyTabScreen(
                  habits = activeHabits,
                  entriesByHabit = entriesByHabit,
                  entriesListByHabit = entriesListByHabit,
                  pausesByHabit = pausesByHabit,
                  settings = settings,
                  today = today,
                  onOpenMenu = { navigateTo(ScreenRoute.Menu) },
                  onNewHabit = {
                    editingHabit = null
                    showEditHabitSheet = true
                  },
                  onOpenHabitDetail = { hId -> navigateTo(ScreenRoute.HabitDetail(hId)) },
                  onToggleDate = { habit, date ->
                    scope.launch {
                      if (habit.trackType == TrackType.TASK) {
                        val done = repository.toggleTaskEntry(habit.id, date, null)
                        if (done && settings.sounds) SoundFeedbackManager.playSoftTick(true)
                      } else {
                        val existing = entriesByHabit[habit.id]?.get(date)
                        val nextVal = if (ScheduleMatcher.isEntryDone(habit, existing)) 0.0 else (habit.targetValue ?: 1.0)
                        repository.setQuantitativeEntry(habit.id, date, nextVal, existing?.note)
                      }
                    }
                  }
                )
              }
              HomeTab.MONTHLY -> {
                MonthlyTabScreen(
                  habits = activeHabits,
                  entriesByHabit = entriesByHabit,
                  entriesListByHabit = entriesListByHabit,
                  pausesByHabit = pausesByHabit,
                  settings = settings,
                  today = today,
                  onOpenMenu = { navigateTo(ScreenRoute.Menu) },
                  onNewHabit = {
                    editingHabit = null
                    showEditHabitSheet = true
                  },
                  onOpenHabitDetail = { hId -> navigateTo(ScreenRoute.HabitDetail(hId)) },
                  onToggleDate = { habit, date ->
                    scope.launch {
                      if (habit.trackType == TrackType.TASK) {
                        val done = repository.toggleTaskEntry(habit.id, date, null)
                        if (done && settings.sounds) SoundFeedbackManager.playSoftTick(true)
                      } else {
                        val existing = entriesByHabit[habit.id]?.get(date)
                        val nextVal = if (ScheduleMatcher.isEntryDone(habit, existing)) 0.0 else (habit.targetValue ?: 1.0)
                        repository.setQuantitativeEntry(habit.id, date, nextVal, existing?.note)
                      }
                    }
                  }
                )
              }
              HomeTab.OVERALL -> {
                OverallTabScreen(
                  habits = activeHabits,
                  entriesByHabit = entriesByHabit,
                  entriesListByHabit = entriesListByHabit,
                  pausesByHabit = pausesByHabit,
                  today = today,
                  onOpenMenu = { navigateTo(ScreenRoute.Menu) },
                  onNewHabit = {
                    editingHabit = null
                    showEditHabitSheet = true
                  },
                  onOpenHabitDetail = { hId -> navigateTo(ScreenRoute.HabitDetail(hId)) }
                )
              }
            }

            if (settings.showTabBar && visibleTabs.size > 1) {
              FloatingPillTabBar(
                tabs = visibleTabs,
                selectedTab = activeTab,
                onSelectTab = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
              )
            }
          }

          ScreenRoute.Menu -> {
            MenuScreen(
              enabledRemindersCount = allReminders.count { it.enabled },
              activeGoalsCount = allGoals.count { it.status == GoalStatus.ACTIVE },
              currentYear = today.year,
              onBack = { popBack() },
              onNavigateNotifications = { navigateTo(ScreenRoute.Notifications) },
              onNavigateSettings = { navigateTo(ScreenRoute.Settings) },
              onNavigateWeeklyRecap = { navigateTo(ScreenRoute.WeeklyRecap) },
              onNavigateMonthlyRecap = { navigateTo(ScreenRoute.MonthlyRecap) },
              onNavigateYearInReview = { navigateTo(ScreenRoute.YearInReview) },
              onNavigateMilestones = { navigateTo(ScreenRoute.MilestonesList) },
              onNavigateWidgets = { navigateTo(ScreenRoute.WidgetsStudio) }
            )
          }

          ScreenRoute.Settings -> {
            SettingsScreen(
              settings = settings,
              archivedCount = archivedHabits.size,
              today = today,
              onBack = { popBack() },
              onUpdateSettings = updateSettings,
              onOpenHomeStudio = { navigateTo(ScreenRoute.HomeStudio) },
              onOpenReorderHabits = { navigateTo(ScreenRoute.ReorderHabits) },
              onOpenBackupRestore = { navigateTo(ScreenRoute.BackupRestore) },
              onOpenArchivedHabits = { navigateTo(ScreenRoute.ArchivedHabits) },
              onReplayWelcomeTour = { navigateTo(ScreenRoute.WelcomeTour) }
            )
          }

          ScreenRoute.HomeStudio -> {
            HomeStudioScreen(
              settings = settings,
              onBack = { popBack() },
              onUpdateSettings = updateSettings,
              onOpenReorderHabits = { navigateTo(ScreenRoute.ReorderHabits) },
              onOpenReorderTabs = { navigateTo(ScreenRoute.ReorderTabs) }
            )
          }

          ScreenRoute.Notifications -> {
            NotificationsScreen(
              habits = activeHabits,
              reminders = allReminders,
              goals = allGoals,
              settings = settings,
              onBack = { popBack() },
              onUpdateSettings = updateSettings,
              onToggleHabitReminders = { hId, enabled ->
                scope.launch { repository.setAllHabitRemindersEnabled(hId, enabled) }
              },
              onOpenHabitDetailReminders = { hId -> navigateTo(ScreenRoute.HabitDetail(hId)) },
              onOpenGoalDetail = { gId -> navigateTo(ScreenRoute.GoalDetail(gId)) }
            )
          }

          ScreenRoute.ReorderHabits -> {
            ReorderHabitsScreen(
              habits = activeHabits,
              onBack = { popBack() },
              onSaveOrder = { orderedIds ->
                scope.launch { repository.updateHabitSortOrders(orderedIds) }
              }
            )
          }

          ScreenRoute.ReorderTabs -> {
            ReorderTabsScreen(
              settings = settings,
              onBack = { popBack() },
              onUpdateSettings = updateSettings
            )
          }

          ScreenRoute.ArchivedHabits -> {
            ArchivedHabitsScreen(
              archivedHabits = archivedHabits,
              entriesByHabit = entriesListByHabit,
              today = today,
              onBack = { popBack() },
              onOpenHabitDetail = { hId -> navigateTo(ScreenRoute.HabitDetail(hId)) },
              onRestoreHabit = { habit ->
                scope.launch { repository.restoreHabit(habit.id) }
              },
              onDeleteForever = { habit ->
                scope.launch { repository.deleteHabit(habit.id) }
              }
            )
          }

          ScreenRoute.WeeklyRecap -> {
            WeeklyRecapScreen(
              habits = activeHabits,
              entries = allEntries,
              pauses = allPauses,
              settings = settings,
              today = today,
              onBack = { popBack() },
              onOpenHabitDetail = { hId -> navigateTo(ScreenRoute.HabitDetail(hId)) }
            )
          }

          ScreenRoute.MonthlyRecap -> {
            MonthlyRecapScreen(
              habits = activeHabits,
              entries = allEntries,
              pauses = allPauses,
              settings = settings,
              today = today,
              onBack = { popBack() },
              onOpenHabitDetail = { hId -> navigateTo(ScreenRoute.HabitDetail(hId)) }
            )
          }

          ScreenRoute.YearInReview -> {
            YearInReviewScreen(
              habits = activeHabits,
              entries = allEntries,
              pauses = allPauses,
              goals = allGoals,
              stepsByGoal = stepsByGoal,
              settings = settings,
              today = today,
              onBack = { popBack() }
            )
          }

          ScreenRoute.MilestonesList -> {
            MilestonesListScreen(
              goals = allGoals,
              stepsByGoal = stepsByGoal,
              habitsMap = habitsMap,
              entriesByHabit = entriesListByHabit,
              pausesByHabit = pausesByHabit,
              today = today,
              onBack = { popBack() },
              onOpenNewGoal = { sampleGoal, sampleSteps ->
                editingGoal = sampleGoal
                editingGoalInitialSteps = sampleSteps ?: emptyList()
                prelinkedHabitForGoal = null
                showEditGoalSheet = true
              },
              onOpenGoalDetail = { gId -> navigateTo(ScreenRoute.GoalDetail(gId)) },
              onToggleManualStep = { step ->
                scope.launch {
                  val goalCompleted = repository.toggleManualStep(step)
                  if (goalCompleted && settings.fullScreenCelebrations) {
                    celebrationHeadline = "Goal *complete.*"
                    celebrationSubtitle = "Every milestone step is done!"
                  }
                }
              }
            )
          }

          is ScreenRoute.GoalDetail -> {
            val goal = allGoals.firstOrNull { it.id == route.goalId }
            if (goal == null) {
              LaunchedEffect(Unit) { popBack() }
            } else {
              val gSteps = stepsByGoal[goal.id].orEmpty()
              GoalDetailScreen(
                goal = goal,
                steps = gSteps,
                habitsMap = habitsMap,
                entriesByHabit = entriesListByHabit,
                pausesByHabit = pausesByHabit,
                today = today,
                onBack = { popBack() },
                onEditGoal = {
                  editingGoal = goal
                  editingGoalInitialSteps = gSteps
                  prelinkedHabitForGoal = null
                  showEditGoalSheet = true
                },
                onToggleManualStep = { step ->
                  scope.launch {
                    val goalCompleted = repository.toggleManualStep(step)
                    if (goalCompleted && settings.fullScreenCelebrations) {
                      celebrationHeadline = "Goal *complete.*"
                      celebrationSubtitle = "You reached ${goal.title}!"
                    }
                  }
                },
                onUpdateAmountStep = { step, newVal ->
                  scope.launch {
                    val goalCompleted = repository.updateAmountStepValue(step, newVal)
                    if (goalCompleted && settings.fullScreenCelebrations) {
                      celebrationHeadline = "Goal *complete.*"
                      celebrationSubtitle = "You reached ${goal.title}!"
                    }
                  }
                },
                onSkipStepForNow = { step ->
                  scope.launch { repository.skipStepForNow(step) }
                },
                onSaveStep = { step ->
                  scope.launch { repository.saveStep(step) }
                },
                onDeleteStep = { step ->
                  scope.launch { repository.deleteStep(step.id, goal.id) }
                },
                onMarkGoalStatus = { status ->
                  scope.launch {
                    repository.markGoalStatus(goal.id, status)
                    if (status == GoalStatus.COMPLETED && settings.fullScreenCelebrations) {
                      celebrationHeadline = "Goal *complete.*"
                      celebrationSubtitle = "You reached ${goal.title}!"
                    }
                  }
                },
                onDeleteGoal = {
                  scope.launch {
                    repository.deleteGoal(goal.id)
                    popBack()
                  }
                }
              )
            }
          }

          ScreenRoute.WidgetsStudio -> {
            WidgetsStudioScreen(
              habits = activeHabits,
              entries = allEntries,
              goals = allGoals,
              stepsByGoal = stepsByGoal,
              settings = settings,
              today = today,
              onUpdateSettings = updateSettings,
              onBack = { popBack() }
            )
          }

          ScreenRoute.BackupRestore -> {
            BackupRestoreScreen(
              db = repository.db,
              settingsStore = settingsDataStore,
              settings = settings,
              habitsCount = allHabits.size,
              entriesCount = allEntries.size,
              goalsCount = allGoals.size,
              onUpdateSettings = updateSettings,
              onBack = { popBack() }
            )
          }

          is ScreenRoute.HabitDetail -> {
            val habit = habitsMap[route.habitId]
            if (habit == null) {
              LaunchedEffect(Unit) { popBack() }
            } else {
              HabitDetailScreen(
                habit = habit,
                entries = entriesListByHabit[habit.id].orEmpty(),
                pauses = pausesByHabit[habit.id].orEmpty(),
                reminders = remindersByHabit[habit.id].orEmpty(),
                settings = settings,
                today = today,
                onBack = { popBack() },
                onEditHabit = {
                  editingHabit = habit
                  showEditHabitSheet = true
                },
                onToggleDate = { date, note ->
                  scope.launch {
                    if (habit.trackType == TrackType.TASK) {
                      repository.toggleTaskEntry(habit.id, date, note)
                    } else {
                      val existing = entriesByHabit[habit.id]?.get(date)
                      val nextVal = if (ScheduleMatcher.isEntryDone(habit, existing)) 0.0 else (habit.targetValue ?: 1.0)
                      repository.setQuantitativeEntry(habit.id, date, nextVal, note ?: existing?.note)
                    }
                  }
                },
                onSkipDate = { date ->
                  scope.launch { repository.skipHabitOnDate(habit.id, date) }
                },
                onDeleteDateEntry = { date ->
                  scope.launch { repository.deleteEntryOnDate(habit.id, date) }
                },
                onPauseRequest = { pausingHabit = habit },
                onResumeHabit = {
                  scope.launch { repository.resumeHabit(habit.id) }
                },
                onArchiveHabit = {
                  scope.launch {
                    repository.archiveHabit(habit.id)
                    popBack()
                  }
                },
                onRestoreHabit = {
                  scope.launch { repository.restoreHabit(habit.id) }
                },
                onAddWidgetForHabit = {
                  val mgr = AppWidgetManager.getInstance(context)
                  val provider = ComponentName(context, SingleHabitWidgetReceiver::class.java)
                  if (mgr.isRequestPinAppWidgetSupported) {
                    mgr.requestPinAppWidget(provider, null, null)
                  } else {
                    navigateTo(ScreenRoute.WidgetsStudio)
                  }
                },
                onDeleteHabit = { deletingHabitConfirm = habit },
                onMakeGoalFromHabit = {
                  editingGoal = null
                  editingGoalInitialSteps = emptyList()
                  prelinkedHabitForGoal = habit
                  showEditGoalSheet = true
                },
                onSaveReminder = { rem ->
                  scope.launch { repository.saveReminder(rem) }
                },
                onDeleteReminder = { remId ->
                  scope.launch { repository.deleteReminder(remId) }
                },
                onToggleAllReminders = { enabled ->
                  scope.launch { repository.setAllHabitRemindersEnabled(habit.id, enabled) }
                }
              )
            }
          }

          ScreenRoute.WelcomeTour -> {
            // Handled above in if (!settings.onboardingDone || currentRoute is ScreenRoute.WelcomeTour)
          }
        }

        // Global Sheets & Dialogs
        if (showEditHabitSheet) {
          val h = editingHabit
          EditHabitBottomSheet(
            existingHabit = h,
            existingReminders = if (h != null) remindersByHabit[h.id].orEmpty() else emptyList(),
            today = today,
            onDismiss = {
              showEditHabitSheet = false
              editingHabit = null
            },
            onSave = { savedHabit, savedReminders ->
              scope.launch {
                val finalHabit = if (h == null) savedHabit.copy(sortOrder = allHabits.size) else savedHabit
                repository.saveHabit(finalHabit, savedReminders)
                showEditHabitSheet = false
                editingHabit = null
              }
            },
            onPauseRequest = { target ->
              showEditHabitSheet = false
              pausingHabit = target
            },
            onArchiveRequest = { target ->
              scope.launch {
                repository.archiveHabit(target.id)
                showEditHabitSheet = false
                editingHabit = null
              }
            },
            onDeleteForeverRequest = { target ->
              showEditHabitSheet = false
              deletingHabitConfirm = target
            }
          )
        }

        if (showTemplateGallerySheet) {
          TemplateGalleryBottomSheet(
            onDismiss = { showTemplateGallerySheet = false },
            onPickTemplate = { tpl ->
              showTemplateGallerySheet = false
              val (draftHabit, _) = StarterTemplatesCatalog.toHabitAndOptionalReminder(
                template = tpl,
                sortOrder = allHabits.size,
                today = today,
                enableReminder = true
              )
              editingHabit = draftHabit
              showEditHabitSheet = true
            }
          )
        }

        if (pausingHabit != null) {
          val target = pausingHabit!!
          PauseHabitBottomSheet(
            habit = target,
            today = today,
            onDismiss = { pausingHabit = null },
            onConfirmPause = { startDate, endDate ->
              scope.launch {
                repository.pauseHabit(target.id, startDate, endDate)
                pausingHabit = null
              }
            }
          )
        }

        if (deletingHabitConfirm != null) {
          val target = deletingHabitConfirm!!
          AlertDialog(
            onDismissRequest = { deletingHabitConfirm = null },
            title = { Text("Delete ${target.name} permanently?") },
            text = { Text("All check-ins and streak history for this habit will be removed.") },
            confirmButton = {
              TextButton(
                onClick = {
                  scope.launch {
                    repository.deleteHabit(target.id)
                    deletingHabitConfirm = null
                    if (currentRoute is ScreenRoute.HabitDetail) {
                      popBack()
                    }
                  }
                }
              ) {
                Text("Delete forever", color = Color(0xFFEE9A9A))
              }
            },
            dismissButton = {
              TextButton(onClick = { deletingHabitConfirm = null }) {
                Text("Cancel")
              }
            }
          )
        }

        if (showEditGoalSheet) {
          EditGoalBottomSheet(
            existingGoal = editingGoal,
            existingSteps = editingGoalInitialSteps,
            availableHabits = activeHabits,
            prelinkedHabit = prelinkedHabitForGoal,
            today = today,
            onDismiss = {
              showEditGoalSheet = false
              editingGoal = null
              prelinkedHabitForGoal = null
            },
            onSave = { savedGoal, savedSteps ->
              scope.launch {
                repository.saveGoalWithSteps(savedGoal, savedSteps)
                showEditGoalSheet = false
                editingGoal = null
                prelinkedHabitForGoal = null
              }
            }
          )
        }

        // Full-Screen Celebration Overlay
        FullScreenCelebrationOverlay(
          visible = celebrationHeadline != null,
          headlineText = celebrationHeadline ?: "All *done.*",
          subtitleText = celebrationSubtitle ?: "Every small mark for today is complete.",
          onDismiss = {
            celebrationHeadline = null
            celebrationSubtitle = null
          }
        )
      }
    }
  }
}
