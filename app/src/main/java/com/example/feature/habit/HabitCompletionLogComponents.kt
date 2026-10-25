package com.example.feature.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.model.Entry
import com.example.core.domain.model.EntryStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitCompletionLog
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.TrackType
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.domain.usecase.StreakCalculator
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.example.core.ui.theme.habitTint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Interactive UI card component for logging habit completions with an explicit
 * ISO-8601 date-stamp (`YYYY-MM-DD`) and timestamp to support streak calculation
 * and longitudinal analytics.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitCompletionLogCard(
  habit: Habit,
  entries: List<Entry>,
  pauses: List<HabitPause>,
  today: LocalDate,
  onLogCompletion: (HabitCompletionLog) -> Unit,
  onClearDate: (LocalDate) -> Unit,
  modifier: Modifier = Modifier,
  initialSelectedDate: LocalDate = today
) {
  val colors = LocalHabitRadarColors.current
  val tint = habitTint(habit.colorKey, colors.isDark)

  var selectedDate by remember(initialSelectedDate) { mutableStateOf(initialSelectedDate) }
  val entriesMap = remember(entries) { entries.associateBy { it.date } }
  val existingEntry = entriesMap[selectedDate]
  val existingLog = remember(existingEntry) { existingEntry?.toCompletionLog() }

  val defaultTarget = remember(habit) {
    if (habit.trackType == TrackType.TASK) 1.0 else (habit.targetValue ?: 1.0)
  }

  var quantitativeValue by remember(selectedDate, existingEntry, defaultTarget) {
    mutableDoubleStateOf(existingEntry?.value ?: defaultTarget)
  }
  var noteText by remember(selectedDate, existingEntry) {
    mutableStateOf(existingEntry?.note ?: "")
  }
  var showNoteInput by remember(selectedDate, existingEntry) {
    mutableStateOf(!existingEntry?.note.isNullOrBlank() || habit.askNote)
  }

  val isDoneOnSelectedDate = remember(habit, existingEntry) {
    ScheduleMatcher.isEntryDone(habit, existingEntry)
  }
  val isSkippedOnSelectedDate = existingEntry?.status == EntryStatus.SKIPPED

  val currentStreakResult = remember(habit, entries, pauses, today) {
    StreakCalculator.calculateStreaks(habit, entries, pauses, today)
  }

  // Project streak if the selected date were logged as DONE right now
  val projectedStreakResult = remember(habit, entries, pauses, today, selectedDate, defaultTarget) {
    val simulatedEntry = Entry(
      id = existingEntry?.id ?: "sim",
      habitId = habit.id,
      date = selectedDate,
      status = EntryStatus.DONE,
      value = defaultTarget,
      completedAt = Instant.now()
    )
    val simulatedList = entries.filterNot { it.date == selectedDate } + simulatedEntry
    StreakCalculator.calculateStreaks(habit, simulatedList, pauses, today)
  }

  val dateStampStr = selectedDate.toString() // YYYY-MM-DD
  val friendlyDateStr = remember(selectedDate, today) {
    val formatted = selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH))
    when (selectedDate) {
      today -> "Today · $formatted"
      today.minusDays(1) -> "Yesterday · $formatted"
      else -> formatted
    }
  }

  val loggedTimestampLabel = remember(existingEntry) {
    existingEntry?.completedAt?.let { instant ->
      val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
      String.format(Locale.US, "%02d:%02d", localTime.hour, localTime.minute)
    }
  }

  HabitRadarCard(
    backgroundColor = tint.cardBackground,
    modifier = modifier.testTag("habit_completion_log_card")
  ) {
    // Header row: Section label + ISO Date-Stamp badge
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      SectionLabel("LOG COMPLETION")
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(tint.subtleElementBg)
          .border(1.dp, tint.baseColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
          .testTag("completion_log_date_stamp_badge")
      ) {
        Text(
          text = if (loggedTimestampLabel != null) {
            "STAMP $dateStampStr · $loggedTimestampLabel"
          } else {
            "STAMP $dateStampStr"
          },
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = tint.baseColor
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Date navigation row + quick date pills
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      IconButton(
        onClick = { selectedDate = selectedDate.minusDays(1) },
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(tint.subtleElementBg)
          .testTag("completion_log_prev_day_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
          contentDescription = "Previous date",
          tint = colors.textPrimary
        )
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
      ) {
        Text(
          text = friendlyDateStr,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = if (isDoneOnSelectedDate) {
            "Completed on $dateStampStr"
          } else if (isSkippedOnSelectedDate) {
            "Skipped on $dateStampStr"
          } else {
            "Not logged yet for $dateStampStr"
          },
          fontSize = 12.sp,
          color = if (isDoneOnSelectedDate) tint.baseColor else colors.textSecondary
        )
      }

      IconButton(
        onClick = {
          if (selectedDate.isBefore(today)) {
            selectedDate = selectedDate.plusDays(1)
          }
        },
        enabled = selectedDate.isBefore(today),
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(tint.subtleElementBg)
          .testTag("completion_log_next_day_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "Next date",
          tint = if (selectedDate.isBefore(today)) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Quick Date Switcher Chips (Today / Yesterday / 2d ago) + Streak Impact Badge
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      listOf(
        Triple("Today", today, "completion_log_today_chip"),
        Triple("Yesterday", today.minusDays(1), "completion_log_yesterday_chip"),
        Triple("2d ago", today.minusDays(2), "completion_log_two_days_ago_chip")
      ).forEach { (label, targetDate, tag) ->
        val isSelected = selectedDate == targetDate
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) tint.baseColor else tint.subtleElementBg)
            .clickable { selectedDate = targetDate }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag)
        ) {
          Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF101114) else colors.textPrimary
          )
        }
      }

      // Live Streak & Analytics Impact Pill
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(14.dp))
          .background(tint.subtleElementBg)
          .padding(horizontal = 12.dp, vertical = 6.dp)
          .testTag("completion_log_streak_badge")
      ) {
        val streakText = if (isDoneOnSelectedDate) {
          "🔥 ${currentStreakResult.currentStreak}d streak · Best ${currentStreakResult.bestStreak}d"
        } else {
          "🔥 ${currentStreakResult.currentStreak}d → ${projectedStreakResult.currentStreak}d if logged"
        }
        Text(
          text = streakText,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = tint.baseColor
        )
      }
    }

    // Quantitative Stepper (for AMOUNT and TIME habits)
    if (habit.trackType != TrackType.TASK) {
      Spacer(modifier = Modifier.height(12.dp))
      val unitLabel = habit.unit?.takeIf { it.isNotBlank() }
        ?: if (habit.trackType == TrackType.TIME) "min" else "units"
      val stepDelta = if (defaultTarget >= 20.0) 5.0 else 1.0

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(tint.subtleElementBg)
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Column {
          Text(
            text = "PROGRESS VALUE",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = colors.textSecondary
          )
          Text(
            text = "${quantitativeValue.roundToInt()} / ${defaultTarget.roundToInt()} $unitLabel",
            fontFamily = InstrumentSerifFamily,
            fontSize = 22.sp,
            color = colors.textPrimary,
            modifier = Modifier.testTag("completion_log_value_text")
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(colors.surface2)
              .clickable {
                quantitativeValue = (quantitativeValue - stepDelta).coerceAtLeast(0.0)
              }
              .testTag("completion_log_decrement_button")
          ) {
            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
          }
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(tint.baseColor)
              .clickable {
                quantitativeValue += stepDelta
              }
              .testTag("completion_log_increment_button")
          ) {
            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101114))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Note toggle or field
    if (showNoteInput) {
      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it.take(200) },
        label = { Text("Completion note (optional)") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("completion_log_note_input")
      )
      Spacer(modifier = Modifier.height(12.dp))
    } else {
      Text(
        text = "+ Add note to date-stamped log",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = tint.baseColor,
        modifier = Modifier
          .clickable { showNoteInput = true }
          .padding(bottom = 10.dp)
          .testTag("completion_log_add_note_toggle")
      )
    }

    // Action Buttons: Log Completion / Skip / Clear
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(1.4f)
          .height(48.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(tint.baseColor)
          .clickable {
            val loggedVal = if (habit.trackType == TrackType.TASK) 1.0 else quantitativeValue.coerceAtLeast(defaultTarget)
            val newLog = HabitCompletionLog(
              id = existingLog?.id ?: UUID.randomUUID().toString(),
              habitId = habit.id,
              date = selectedDate,
              dateStamp = selectedDate.toString(),
              status = EntryStatus.DONE,
              value = loggedVal,
              note = noteText.trim().ifBlank { null },
              completedAt = Instant.now()
            )
            onLogCompletion(newLog)
          }
          .testTag("completion_log_submit_button")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF101114),
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = if (isDoneOnSelectedDate) "Update Log ($dateStampStr)" else "Log Done ($dateStampStr)",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF101114)
          )
        }
      }

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(0.8f)
          .height(48.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(tint.subtleElementBg)
          .clickable {
            val skipLog = HabitCompletionLog(
              id = existingLog?.id ?: UUID.randomUUID().toString(),
              habitId = habit.id,
              date = selectedDate,
              dateStamp = selectedDate.toString(),
              status = EntryStatus.SKIPPED,
              value = null,
              note = noteText.trim().ifBlank { null },
              completedAt = Instant.now()
            )
            onLogCompletion(skipLog)
          }
          .testTag("completion_log_skip_button")
      ) {
        Text(
          text = "Skip",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = colors.textPrimary
        )
      }

      if (existingEntry != null) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(0.7f)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x26EE9A9A))
            .clickable { onClearDate(selectedDate) }
            .testTag("completion_log_clear_button")
        ) {
          Text(
            text = "Clear",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFEE9A9A)
          )
        }
      }
    }
  }
}

/**
 * Modal bottom sheet for logging or editing a date-stamped habit completion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCompletionLogBottomSheet(
  habit: Habit,
  initialDate: LocalDate,
  entries: List<Entry>,
  pauses: List<HabitPause>,
  today: LocalDate,
  onDismiss: () -> Unit,
  onSaveLog: (HabitCompletionLog) -> Unit,
  onClearDate: (LocalDate) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = colors.bg,
    modifier = Modifier.testTag("habit_completion_log_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      Text(
        text = habit.name,
        fontFamily = InstrumentSerifFamily,
        fontSize = 26.sp,
        color = colors.textPrimary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
      )
      HabitCompletionLogCard(
        habit = habit,
        entries = entries,
        pauses = pauses,
        today = today,
        initialSelectedDate = initialDate,
        onLogCompletion = { log ->
          onSaveLog(log)
          onDismiss()
        },
        onClearDate = { date ->
          onClearDate(date)
          onDismiss()
        }
      )
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
