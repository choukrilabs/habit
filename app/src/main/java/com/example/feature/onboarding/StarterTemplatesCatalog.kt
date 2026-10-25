package com.example.feature.onboarding

import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitTemplate
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.TrackType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

object StarterTemplatesCatalog {

  val templates: List<HabitTemplate> = listOf(
    // Health
    HabitTemplate(
      id = "tpl_water",
      category = "Health",
      name = "Drink water",
      description = "Stay hydrated throughout the day",
      iconKey = "water",
      colorKey = "#8FBDF0",
      trackType = TrackType.AMOUNT,
      targetValue = 8.0,
      unit = "glasses",
      group = "Morning",
      suggestedReminderTime = "09:00"
    ),
    HabitTemplate(
      id = "tpl_walk",
      category = "Health",
      name = "Walk",
      description = "Brisk outdoor walk for fresh air",
      iconKey = "walk",
      colorKey = "#A8DFB4",
      trackType = TrackType.TASK,
      repeatType = RepeatType.DAILY,
      daysOfWeek = listOf(1, 4, 7), // Mon, Thu, Sun
      group = "Morning",
      suggestedReminderTime = "07:30"
    ),
    HabitTemplate(
      id = "tpl_workout",
      category = "Health",
      name = "Workout",
      description = "Strength or bodyweight session",
      iconKey = "workout",
      colorKey = "#F5C9A5",
      trackType = TrackType.TASK,
      group = "Evening",
      suggestedReminderTime = "18:00"
    ),
    HabitTemplate(
      id = "tpl_stretch",
      category = "Health",
      name = "Morning stretch",
      description = "Loosen up joints and spine after waking",
      iconKey = "spa",
      colorKey = "#C6E3A0",
      trackType = TrackType.TASK,
      group = "Morning",
      suggestedReminderTime = "07:00"
    ),
    HabitTemplate(
      id = "tpl_vitamins",
      category = "Health",
      name = "Take vitamins",
      description = "Daily supplements with breakfast",
      iconKey = "vitamins",
      colorKey = "#F5DA8A",
      trackType = TrackType.TASK,
      group = "Morning",
      suggestedReminderTime = "08:30"
    ),

    // Mind
    HabitTemplate(
      id = "tpl_meditate",
      category = "Mind",
      name = "Meditate",
      description = "10 minutes of quiet breath focus",
      iconKey = "meditate",
      colorKey = "#D6BEFA",
      trackType = TrackType.TIME,
      targetValue = 10.0,
      unit = "min",
      group = "Morning",
      suggestedReminderTime = "07:15"
    ),
    HabitTemplate(
      id = "tpl_journal",
      category = "Mind",
      name = "Journal",
      description = "Write a few honest lines",
      iconKey = "journal",
      colorKey = "#F2A8C8",
      trackType = TrackType.TASK,
      group = "Evening",
      suggestedReminderTime = "21:00"
    ),
    HabitTemplate(
      id = "tpl_read_pages",
      category = "Mind",
      name = "Read 10 pages",
      description = "Daily non-fiction or literature reading",
      iconKey = "book",
      colorKey = "#7FC8BC",
      trackType = TrackType.AMOUNT,
      targetValue = 10.0,
      unit = "pages",
      group = "Evening",
      suggestedReminderTime = "21:30"
    ),
    HabitTemplate(
      id = "tpl_review_day",
      category = "Mind",
      name = "Review the day",
      description = "Reflect on what went well today",
      iconKey = "mind",
      colorKey = "#A5B0E0",
      trackType = TrackType.TASK,
      group = "Evening",
      suggestedReminderTime = "22:00"
    ),

    // Sleep
    HabitTemplate(
      id = "tpl_lights_out",
      category = "Sleep",
      name = "Lights out by 12",
      description = "Protect deep rest before midnight",
      iconKey = "moon",
      colorKey = "#B79BE0",
      trackType = TrackType.TASK,
      group = "Evening",
      suggestedReminderTime = "23:30"
    ),
    HabitTemplate(
      id = "tpl_wake_on_time",
      category = "Sleep",
      name = "Wake up on time",
      description = "No snoozing the first alarm",
      iconKey = "alarm",
      colorKey = "#EEE18A",
      trackType = TrackType.TASK,
      group = "Morning",
      suggestedReminderTime = "06:30"
    ),
    HabitTemplate(
      id = "tpl_no_screens",
      category = "Sleep",
      name = "No screens in bed",
      description = "Keep the phone away 30m before sleep",
      iconKey = "noscreen",
      colorKey = "#A0DDDD",
      trackType = TrackType.TASK,
      group = "Evening",
      suggestedReminderTime = "22:30"
    ),

    // Money
    HabitTemplate(
      id = "tpl_log_spent",
      category = "Money",
      name = "Log what I spent",
      description = "Track every expense from today",
      iconKey = "savings",
      colorKey = "#F5C9A5",
      trackType = TrackType.TASK,
      group = "Evening",
      suggestedReminderTime = "20:30"
    ),

    // Study
    HabitTemplate(
      id = "tpl_study_session",
      category = "Study",
      name = "Study session",
      description = "30 minutes of focused learning",
      iconKey = "study",
      colorKey = "#7DB4E8",
      trackType = TrackType.TIME,
      targetValue = 30.0,
      unit = "min",
      suggestedReminderTime = "17:00"
    ),
    HabitTemplate(
      id = "tpl_learn_new",
      category = "Study",
      name = "Learn something new",
      description = "One concept, article, or lesson",
      iconKey = "idea",
      colorKey = "#F5DA8A",
      trackType = TrackType.TASK,
      suggestedReminderTime = "19:00"
    ),
    HabitTemplate(
      id = "tpl_review_notes",
      category = "Study",
      name = "Review notes",
      description = "Spaced repetition of recent notes",
      iconKey = "book",
      colorKey = "#8FBB96",
      trackType = TrackType.TASK,
      suggestedReminderTime = "19:30"
    ),

    // Faith & reflection
    HabitTemplate(
      id = "tpl_morning_adhkar",
      category = "Faith & reflection",
      name = "Morning remembrance",
      description = "Start the morning with calm gratitude",
      iconKey = "sunrise",
      colorKey = "#8FBB96",
      trackType = TrackType.TASK,
      group = "Morning",
      suggestedReminderTime = "06:00"
    ),
    HabitTemplate(
      id = "tpl_evening_adhkar",
      category = "Faith & reflection",
      name = "Evening remembrance",
      description = "End the afternoon in quiet reflection",
      iconKey = "night",
      colorKey = "#7FC8BC",
      trackType = TrackType.TASK,
      group = "Evening",
      suggestedReminderTime = "18:30"
    ),
    HabitTemplate(
      id = "tpl_daily_reading",
      category = "Faith & reflection",
      name = "Daily reading",
      description = "Scripture or spiritual contemplation",
      iconKey = "book",
      colorKey = "#A8DFB4",
      trackType = TrackType.TASK,
      group = "Morning",
      suggestedReminderTime = "06:30"
    ),
    HabitTemplate(
      id = "tpl_charity",
      category = "Faith & reflection",
      name = "Give charity weekly",
      description = "Small weekly act of generosity",
      iconKey = "charity",
      colorKey = "#F2A8C8",
      trackType = TrackType.TASK,
      repeatType = RepeatType.WEEKLY,
      timesPerPeriod = 1,
      suggestedReminderTime = "10:00"
    ),

    // Home
    HabitTemplate(
      id = "tpl_tidy_up",
      category = "Home",
      name = "Tidy up 10 minutes",
      description = "Quick reset of your living space",
      iconKey = "clean",
      colorKey = "#F5B8AE",
      trackType = TrackType.TASK,
      group = "Evening",
      suggestedReminderTime = "20:00"
    ),
    HabitTemplate(
      id = "tpl_plan_tomorrow",
      category = "Home",
      name = "Plan tomorrow",
      description = "Write down your top 3 priorities for tomorrow",
      iconKey = "plan",
      colorKey = "#D6BEFA",
      trackType = TrackType.TASK,
      group = "Evening",
      suggestedReminderTime = "21:45"
    )
  )

  val categories: List<String> = listOf(
    "All",
    "Health",
    "Mind",
    "Sleep",
    "Money",
    "Study",
    "Faith & reflection",
    "Home"
  )

  fun toHabitAndOptionalReminder(
    template: HabitTemplate,
    sortOrder: Int,
    today: LocalDate,
    enableReminder: Boolean
  ): Pair<Habit, Reminder?> {
    val habitId = UUID.randomUUID().toString()
    val normalizedCategory = com.example.core.domain.model.HabitCategoryCatalog
      .normalizeTemplateCategory(template.category)
      .ifBlank { template.category }
    val habit = Habit(
      id = habitId,
      name = template.name,
      description = template.description,
      iconKey = template.iconKey,
      colorKey = template.colorKey,
      trackType = template.trackType,
      targetValue = template.targetValue,
      unit = template.unit,
      repeatType = template.repeatType,
      daysOfWeek = template.daysOfWeek,
      timesPerPeriod = template.timesPerPeriod,
      group = template.group ?: normalizedCategory,
      category = normalizedCategory,
      tags = listOfNotNull(normalizedCategory.takeIf { it.isNotBlank() }, template.group?.takeIf { it.isNotBlank() }).distinct(),
      startDate = today,
      sortOrder = sortOrder,
      createdAt = Instant.now()
    )
    val reminder = if (enableReminder) {
      Reminder(
        id = UUID.randomUUID().toString(),
        habitId = habitId,
        time = runCatching { LocalTime.parse(template.suggestedReminderTime) }.getOrDefault(LocalTime.of(8, 0)),
        daysOfWeek = template.daysOfWeek,
        message = "Time for ${template.name}",
        enabled = true
      )
    } else null
    return habit to reminder
  }
}
