package com.example.feature.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.model.ComparisonType
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitTemplate
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.ReminderSound
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.TrackType
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.ui.components.CardHairline
import com.example.core.ui.components.HabitIconView
import com.example.core.ui.components.HabitIconsCatalog
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.components.HabitRadarSegmentedControl
import com.example.core.ui.components.HabitRadarSwitch
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.theme.HabitPaletteRow1
import com.example.core.ui.theme.HabitPaletteRow2
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.example.core.ui.theme.habitTint
import com.example.core.ui.theme.parseHexColor
import com.example.feature.onboarding.StarterTemplatesCatalog
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditHabitBottomSheet(
  existingHabit: Habit?,
  existingReminders: List<Reminder>,
  today: LocalDate,
  onDismiss: () -> Unit,
  onSave: (Habit, List<Reminder>) -> Unit,
  onPauseRequest: ((Habit) -> Unit)? = null,
  onArchiveRequest: ((Habit) -> Unit)? = null,
  onDeleteForeverRequest: ((Habit) -> Unit)? = null
) {
  val colors = LocalHabitRadarColors.current
  val isEdit = existingHabit != null

  var name by remember { mutableStateOf(existingHabit?.name ?: "") }
  var description by remember { mutableStateOf(existingHabit?.description ?: "") }
  var iconKey by remember { mutableStateOf(existingHabit?.iconKey ?: "star") }
  var colorKey by remember { mutableStateOf(existingHabit?.colorKey ?: "#8FBDF0") }
  var trackType by remember { mutableStateOf(existingHabit?.trackType ?: TrackType.TASK) }
  var targetValueStr by remember {
    mutableStateOf(
      existingHabit?.targetValue?.let {
        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
      } ?: "8"
    )
  }
  var unit by remember { mutableStateOf(existingHabit?.unit ?: "glasses") }
  var comparison by remember { mutableStateOf(existingHabit?.comparison ?: ComparisonType.AT_LEAST) }
  var repeatType by remember { mutableStateOf(existingHabit?.repeatType ?: RepeatType.DAILY) }
  val selectedDaysOfWeek = remember {
    mutableStateListOf<Int>().apply {
      addAll(existingHabit?.daysOfWeek ?: listOf(1, 2, 3, 4, 5, 6, 7))
    }
  }
  var timesPerPeriod by remember { mutableIntStateOf(existingHabit?.timesPerPeriod ?: 3) }
  val selectedDaysOfMonth = remember {
    mutableStateListOf<Int>().apply {
      addAll(existingHabit?.daysOfMonth ?: listOf(1, 15))
    }
  }

  // Reminders state
  val remindersList = remember {
    mutableStateListOf<Reminder>().apply { addAll(existingReminders) }
  }
  var remindMeMaster by remember { mutableStateOf(existingReminders.any { it.enabled }) }
  var editingReminder by remember { mutableStateOf<Reminder?>(null) }
  var showNewReminderSheet by remember { mutableStateOf(false) }

  // More section state
  var showMoreSection by remember { mutableStateOf(false) }
  var groupName by remember { mutableStateOf(existingHabit?.group ?: "") }
  var goalStreakStr by remember { mutableStateOf(existingHabit?.goalStreak?.toString() ?: "") }
  var askNote by remember { mutableStateOf(existingHabit?.askNote ?: false) }

  // Sub-sheets & dialogs
  var showIconPicker by remember { mutableStateOf(false) }
  var showTemplateGallery by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  val targetNumberValid = when (trackType) {
    TrackType.TASK -> true
    TrackType.AMOUNT, TrackType.TIME -> (targetValueStr.toDoubleOrNull() ?: 0.0) > 0.0
  }
  val isValid = name.trim().isNotEmpty() && targetNumberValid &&
    (repeatType != RepeatType.DAILY || selectedDaysOfWeek.isNotEmpty())

  val tint = habitTint(colorKey, colors.isDark)

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.bg,
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(colors.bg)
        .imePadding()
    ) {
      // Top Bar: Cancel · NEW HABIT / EDIT HABIT · Save pill
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Text(
          text = "Cancel",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 16.sp,
          color = colors.textSecondary,
          modifier = Modifier
            .clickable(onClick = onDismiss)
            .padding(8.dp)
            .testTag("cancel_habit_button")
        )
        Text(
          text = if (isEdit) "EDIT HABIT" else "NEW HABIT",
          style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
          color = colors.textSecondary
        )
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isValid) colors.accent else colors.surface2)
            .clickable(enabled = isValid) {
              val habitId = existingHabit?.id ?: UUID.randomUUID().toString()
              val savedHabit = Habit(
                id = habitId,
                name = name.trim().take(40),
                description = description.trim().take(200).ifBlank { null },
                iconKey = iconKey,
                colorKey = colorKey,
                trackType = trackType,
                targetValue = if (trackType == TrackType.TASK) null else targetValueStr.toDoubleOrNull(),
                unit = if (trackType == TrackType.AMOUNT) unit.trim().ifBlank { "units" } else if (trackType == TrackType.TIME) "min" else null,
                comparison = comparison,
                repeatType = repeatType,
                daysOfWeek = selectedDaysOfWeek.sorted(),
                timesPerPeriod = if (repeatType != RepeatType.DAILY) timesPerPeriod else null,
                daysOfMonth = if (repeatType == RepeatType.MONTHLY) selectedDaysOfMonth.sorted() else emptyList(),
                group = groupName.trim().ifBlank { null },
                goalStreak = goalStreakStr.toIntOrNull(),
                askNote = askNote,
                status = existingHabit?.status ?: com.example.core.domain.model.HabitStatus.ACTIVE,
                archivedAt = existingHabit?.archivedAt,
                startDate = existingHabit?.startDate ?: today,
                endDate = existingHabit?.endDate,
                sortOrder = existingHabit?.sortOrder ?: 0
              )
              val finalReminders = if (remindMeMaster) {
                remindersList.map { it.copy(habitId = habitId, enabled = true) }
              } else {
                remindersList.map { it.copy(habitId = habitId, enabled = false) }
              }
              onSave(savedHabit, finalReminders)
            }
            .padding(horizontal = 20.dp, vertical = 9.dp)
            .testTag("save_habit_button")
        ) {
          Text(
            text = "Save",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isValid) colors.onAccent else colors.textSecondary
          )
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        // 0. Browse ideas pill (New habit only)
        if (!isEdit) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(colors.accentContainer)
              .clickable { showTemplateGallery = true }
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .testTag("browse_ideas_button")
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = colors.accent,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Browse ideas",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = colors.accent
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        // 1. 135dp rounded icon tile with soft glow + Change icon pill
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(135.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(
              Brush.radialGradient(
                colors = listOf(
                  tint.baseColor.copy(alpha = 0.34f),
                  tint.iconTileBackground
                )
              )
            )
            .clickable { showIconPicker = true }
        ) {
          HabitIconView(
            iconKey = iconKey,
            tint = Color.White,
            modifier = Modifier.size(58.dp),
            emojiSize = 44.sp
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface2)
            .clickable { showIconPicker = true }
            .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
          Text(
            text = "Change icon",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.accent
          )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 2. Large centered serif Name field ("Name it plainly")
        BasicTextField(
          value = name,
          onValueChange = { if (it.length <= 40) name = it },
          textStyle = TextStyle(
            fontFamily = InstrumentSerifFamily,
            fontSize = 32.sp,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
          ),
          cursorBrush = SolidColor(colors.accent),
          decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
              if (name.isEmpty()) {
                Text(
                  text = "Name it plainly",
                  fontFamily = InstrumentSerifFamily,
                  fontSize = 32.sp,
                  color = colors.textSecondary.copy(alpha = 0.6f),
                  textAlign = TextAlign.Center
                )
              }
              innerTextField()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_name_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Description field
        BasicTextField(
          value = description,
          onValueChange = { if (it.length <= 200) description = it },
          textStyle = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontSize = 15.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
          ),
          cursorBrush = SolidColor(colors.accent),
          decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
              if (description.isEmpty()) {
                Text(
                  text = "Add description",
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 15.sp,
                  color = colors.textSecondary.copy(alpha = 0.55f),
                  textAlign = TextAlign.Center
                )
              }
              inner()
            }
          },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(22.dp))
        CardHairline()
        Spacer(modifier = Modifier.height(18.dp))

        // 4. HABIT COLOR (2 rows x 8 swatches, horizontally scrollable, 46dp rounded squares)
        Column(modifier = Modifier.fillMaxWidth()) {
          SectionLabel("HABIT COLOR")
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              listOf(HabitPaletteRow1, HabitPaletteRow2).forEach { rowSwatches ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  rowSwatches.forEach { hex ->
                    val isSelected = colorKey.equals(hex, ignoreCase = true)
                    val c = parseHexColor(hex)
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .size(52.dp)
                        .then(
                          if (isSelected) {
                            Modifier.border(2.dp, Color(0xFF8FBDF0), RoundedCornerShape(16.dp))
                          } else Modifier
                        )
                        .padding(4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c)
                        .clickable { colorKey = hex }
                    ) {
                      if (isSelected) {
                        Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = "Selected color",
                          tint = Color(0xFF14151A),
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))
        CardHairline()
        Spacer(modifier = Modifier.height(18.dp))

        // 5. TRACK segmented: Task | Amount | Time
        Column(modifier = Modifier.fillMaxWidth()) {
          SectionLabel("TRACK")
          HabitRadarSegmentedControl(
            options = TrackType.entries,
            selected = trackType,
            labelMapper = {
              when (it) {
                TrackType.TASK -> "Task"
                TrackType.AMOUNT -> "Amount"
                TrackType.TIME -> "Time"
              }
            },
            onSelect = {
              trackType = it
              if (it == TrackType.TIME && targetValueStr == "8") targetValueStr = "15"
            },
            modifier = Modifier.fillMaxWidth()
          )

          if (trackType != TrackType.TASK) {
            Spacer(modifier = Modifier.height(14.dp))
            HabitRadarCard {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                OutlinedTextField(
                  value = targetValueStr,
                  onValueChange = { targetValueStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                  label = { Text(if (trackType == TrackType.AMOUNT) "Target amount" else "Target minutes") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
                if (trackType == TrackType.AMOUNT) {
                  OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it.take(16) },
                    label = { Text("Unit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
              Spacer(modifier = Modifier.height(12.dp))
              HabitRadarSegmentedControl(
                options = ComparisonType.entries,
                selected = comparison,
                labelMapper = {
                  when (it) {
                    ComparisonType.AT_LEAST -> "At least"
                    ComparisonType.AT_MOST -> "At most"
                    ComparisonType.EXACTLY -> "Exactly"
                  }
                },
                onSelect = { comparison = it },
                height = 44.dp,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))
        CardHairline()
        Spacer(modifier = Modifier.height(18.dp))

        // 6. REPEAT segmented: Daily | Weekly | Monthly
        Column(modifier = Modifier.fillMaxWidth()) {
          SectionLabel("REPEAT")
          HabitRadarSegmentedControl(
            options = RepeatType.entries,
            selected = repeatType,
            labelMapper = {
              when (it) {
                RepeatType.DAILY -> "Daily"
                RepeatType.WEEKLY -> "Weekly"
                RepeatType.MONTHLY -> "Monthly"
              }
            },
            onSelect = { repeatType = it },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(16.dp))

          when (repeatType) {
            RepeatType.DAILY -> {
              val summaryLabel = when {
                selectedDaysOfWeek.size == 7 -> "Everyday"
                selectedDaysOfWeek.sorted() == listOf(1, 2, 3, 4, 5) -> "Weekdays"
                else -> selectedDaysOfWeek.sorted().joinToString(", ") { ScheduleMatcher.dowShortLabel(it) }
              }
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "On these days",
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Medium,
                  color = colors.textPrimary
                )
                Text(
                  text = summaryLabel,
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 13.sp,
                  color = colors.textSecondary
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                (1..7).forEach { dow ->
                  val isSelected = dow in selectedDaysOfWeek
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(if (isSelected) colors.accent else colors.surface2)
                      .clickable {
                        if (isSelected && selectedDaysOfWeek.size > 1) {
                          selectedDaysOfWeek.remove(dow)
                        } else if (!isSelected) {
                          selectedDaysOfWeek.add(dow)
                        }
                      }
                  ) {
                    Text(
                      text = ScheduleMatcher.dowShortLabel(dow),
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isSelected) colors.onAccent else colors.textPrimary
                    )
                  }
                }
              }
            }
            RepeatType.WEEKLY -> {
              HabitRadarCard {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "$timesPerPeriod times per week",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 16.sp,
                    color = colors.textPrimary
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .clickable { if (timesPerPeriod > 1) timesPerPeriod-- }
                    ) {
                      Text("−", fontSize = 18.sp, color = colors.textPrimary)
                    }
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable { if (timesPerPeriod < 7) timesPerPeriod++ }
                    ) {
                      Text("+", fontSize = 18.sp, color = colors.onAccent)
                    }
                  }
                }
              }
            }
            RepeatType.MONTHLY -> {
              Text(
                text = "Days of month",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 14.sp,
                color = colors.textSecondary
              )
              Spacer(modifier = Modifier.height(8.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                (1..31).forEach { day ->
                  val sel = day in selectedDaysOfMonth
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(38.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (sel) colors.accent else colors.surface2)
                      .clickable {
                        if (sel && selectedDaysOfMonth.size > 1) selectedDaysOfMonth.remove(day)
                        else if (!sel) selectedDaysOfMonth.add(day)
                      }
                  ) {
                    Text(
                      text = day.toString(),
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (sel) colors.onAccent else colors.textPrimary
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))
        CardHairline()
        Spacer(modifier = Modifier.height(18.dp))

        // 7. REMINDERS section (shared logic with Habit Detail Section N)
        Column(modifier = Modifier.fillMaxWidth()) {
          SectionLabel("REMINDERS")
          HabitRadarCard {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Remind me",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
              )
              HabitRadarSwitch(
                checked = remindMeMaster,
                onCheckedChange = {
                  remindMeMaster = it
                  if (it && remindersList.isEmpty()) {
                    remindersList.add(
                      Reminder(
                        id = UUID.randomUUID().toString(),
                        habitId = existingHabit?.id ?: "",
                        time = LocalTime.of(8, 0),
                        daysOfWeek = selectedDaysOfWeek.toList()
                      )
                    )
                  }
                }
              )
            }

            if (remindMeMaster) {
              Spacer(modifier = Modifier.height(12.dp))
              remindersList.forEach { r ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface2)
                    .clickable { editingReminder = r }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = String.format("%02d:%02d", r.time.hour, r.time.minute),
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold,
                      color = colors.textPrimary
                    )
                    Text(
                      text = if (r.daysOfWeek.size == 7) "Everyday" else r.daysOfWeek.sorted().joinToString(", ") { ScheduleMatcher.dowShortLabel(it) },
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 12.sp,
                      color = colors.textSecondary
                    )
                  }
                  IconButton(onClick = { remindersList.remove(r) }) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Delete reminder",
                      tint = colors.textSecondary
                    )
                  }
                }
                Spacer(modifier = Modifier.height(8.dp))
              }

              if (remindersList.size < 5) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.accentContainer)
                    .clickable { showNewReminderSheet = true }
                ) {
                  Text(
                    text = "+ Add reminder",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 8. MORE (collapsed section)
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showMoreSection = !showMoreSection }
              .padding(vertical = 8.dp)
          ) {
            SectionLabel("MORE")
            Icon(
              imageVector = if (showMoreSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = "Toggle More",
              tint = colors.textSecondary
            )
          }
          if (showMoreSection) {
            HabitRadarCard {
              Text(
                text = "Group",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 14.sp,
                color = colors.textSecondary
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Morning", "Evening", "Afternoon").forEach { preset ->
                  val sel = groupName.equals(preset, ignoreCase = true)
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(16.dp))
                      .background(if (sel) colors.accentContainer else colors.surface2)
                      .clickable { groupName = if (sel) "" else preset }
                      .padding(horizontal = 14.dp, vertical = 8.dp)
                  ) {
                    Text(
                      text = preset,
                      fontSize = 13.sp,
                      color = if (sel) colors.accent else colors.textPrimary
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(12.dp))
              OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it.take(24) },
                label = { Text("Custom group (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              Spacer(modifier = Modifier.height(12.dp))
              OutlinedTextField(
                value = goalStreakStr,
                onValueChange = { goalStreakStr = it.filter { ch -> ch.isDigit() } },
                label = { Text("Goal streak (days)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Ask for a note when completing",
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 15.sp,
                  color = colors.textPrimary
                )
                HabitRadarSwitch(checked = askNote, onCheckedChange = { askNote = it })
              }
            }
          }
        }

        // Edit mode actions: Pause, Archive, Delete
        if (isEdit && existingHabit != null) {
          Spacer(modifier = Modifier.height(22.dp))
          HabitRadarCard(contentPadding = PaddingValues(0.dp)) {
            if (onPauseRequest != null) {
              Text(
                text = "Pause habit",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 16.sp,
                color = colors.textPrimary,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onPauseRequest(existingHabit) }
                  .padding(18.dp)
              )
              CardHairline()
            }
            if (onArchiveRequest != null) {
              Text(
                text = "Archive habit",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 16.sp,
                color = colors.textPrimary,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onArchiveRequest(existingHabit) }
                  .padding(18.dp)
              )
              CardHairline()
            }
            Text(
              text = "Delete habit",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFFEE9A9A),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showDeleteDialog = true }
                .padding(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(48.dp))
      }
    }
  }

  if (showIconPicker) {
    IconPickerBottomSheet(
      selectedKey = iconKey,
      accentColor = parseHexColor(colorKey),
      onSelect = {
        iconKey = it
        showIconPicker = false
      },
      onDismiss = { showIconPicker = false }
    )
  }

  if (showTemplateGallery) {
    TemplateGalleryBottomSheet(
      onPickTemplate = { tpl ->
        name = tpl.name
        description = tpl.description
        iconKey = tpl.iconKey
        colorKey = tpl.colorKey
        trackType = tpl.trackType
        if (tpl.targetValue != null) targetValueStr = tpl.targetValue.toInt().toString()
        if (tpl.unit != null) unit = tpl.unit
        repeatType = tpl.repeatType
        selectedDaysOfWeek.clear()
        selectedDaysOfWeek.addAll(tpl.daysOfWeek)
        if (tpl.timesPerPeriod != null) timesPerPeriod = tpl.timesPerPeriod
        groupName = tpl.group ?: ""
        showTemplateGallery = false
      },
      onDismiss = { showTemplateGallery = false }
    )
  }

  if (showNewReminderSheet || editingReminder != null) {
    ReminderEditorBottomSheet(
      initialReminder = editingReminder,
      habitId = existingHabit?.id ?: "",
      habitName = name.ifBlank { "this habit" },
      defaultDays = selectedDaysOfWeek.toList(),
      onDismiss = {
        showNewReminderSheet = false
        editingReminder = null
      },
      onSave = { saved ->
        val idx = remindersList.indexOfFirst { it.id == saved.id }
        if (idx != -1) remindersList[idx] = saved else if (remindersList.size < 5) remindersList.add(saved)
        showNewReminderSheet = false
        editingReminder = null
      },
      onDelete = { toDel ->
        remindersList.removeAll { it.id == toDel.id }
        editingReminder = null
      }
    )
  }

  if (showDeleteDialog && existingHabit != null) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete ${existingHabit.name}?") },
      text = {
        Text("This permanently deletes the habit and all its history. Archive it instead?")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onArchiveRequest?.invoke(existingHabit)
          }
        ) {
          Text("Archive", color = colors.accent, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onDeleteForeverRequest?.invoke(existingHabit)
          }
        ) {
          Text("Delete forever", color = Color(0xFFEE9A9A))
        }
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorBottomSheet(
  initialReminder: Reminder?,
  habitId: String,
  habitName: String,
  defaultDays: List<Int>,
  onDismiss: () -> Unit,
  onSave: (Reminder) -> Unit,
  onDelete: ((Reminder) -> Unit)? = null
) {
  val colors = LocalHabitRadarColors.current
  val isEdit = initialReminder != null

  var hour by remember { mutableIntStateOf(initialReminder?.time?.hour ?: 8) }
  var minute by remember { mutableIntStateOf(initialReminder?.time?.minute ?: 0) }
  val days = remember {
    mutableStateListOf<Int>().apply {
      addAll(initialReminder?.daysOfWeek ?: defaultDays.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) })
    }
  }
  var message by remember { mutableStateOf(initialReminder?.message ?: "") }
  var skipIfDone by remember { mutableStateOf(initialReminder?.skipIfDone ?: true) }
  var repeatAfterMin by remember { mutableStateOf(initialReminder?.repeatAfterMin) }
  var sound by remember { mutableStateOf(initialReminder?.sound ?: ReminderSound.DEFAULT) }
  var vibrate by remember { mutableStateOf(initialReminder?.vibrate ?: true) }
  var quickActions by remember { mutableStateOf(initialReminder?.quickActions ?: true) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = colors.bg
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Cancel",
          color = colors.textSecondary,
          modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp)
        )
        Text(
          text = if (isEdit) "EDIT REMINDER" else "NEW REMINDER",
          style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
          color = colors.textSecondary
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (days.isNotEmpty()) colors.accent else colors.surface2)
            .clickable(enabled = days.isNotEmpty()) {
              onSave(
                Reminder(
                  id = initialReminder?.id ?: UUID.randomUUID().toString(),
                  habitId = habitId,
                  time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)),
                  daysOfWeek = days.sorted(),
                  message = message.trim().ifBlank { null },
                  skipIfDone = skipIfDone,
                  repeatAfterMin = repeatAfterMin,
                  sound = sound,
                  vibrate = vibrate,
                  quickActions = quickActions,
                  enabled = initialReminder?.enabled ?: true
                )
              )
            }
            .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
          Text("Save", color = colors.onAccent, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Time dial / wheel controls
      HabitRadarCard {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              "▲",
              color = colors.accent,
              fontSize = 18.sp,
              modifier = Modifier.clickable { hour = (hour + 1) % 24 }.padding(8.dp)
            )
            Text(
              text = String.format("%02d", hour),
              fontFamily = InstrumentSerifFamily,
              fontSize = 44.sp,
              color = colors.textPrimary
            )
            Text(
              "▼",
              color = colors.accent,
              fontSize = 18.sp,
              modifier = Modifier.clickable { hour = (hour + 23) % 24 }.padding(8.dp)
            )
          }
          Text(
            text = " : ",
            fontFamily = InstrumentSerifFamily,
            fontSize = 40.sp,
            color = colors.textSecondary
          )
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              "▲",
              color = colors.accent,
              fontSize = 18.sp,
              modifier = Modifier.clickable { minute = (minute + 5) % 60 }.padding(8.dp)
            )
            Text(
              text = String.format("%02d", minute),
              fontFamily = InstrumentSerifFamily,
              fontSize = 44.sp,
              color = colors.textPrimary
            )
            Text(
              "▼",
              color = colors.accent,
              fontSize = 18.sp,
              modifier = Modifier.clickable { minute = (minute + 55) % 60 }.padding(8.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Day chips Mon-Sun
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        (1..7).forEach { dow ->
          val sel = dow in days
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(if (sel) colors.accent else colors.surface2)
              .clickable {
                if (sel && days.size > 1) days.remove(dow) else if (!sel) days.add(dow)
              }
          ) {
            Text(
              text = ScheduleMatcher.dowShortLabel(dow),
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (sel) colors.onAccent else colors.textPrimary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = message,
        onValueChange = { message = it.take(80) },
        label = { Text("Message (default: Time for $habitName)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      HabitRadarCard {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Skip if already done", color = colors.textPrimary, fontFamily = PlusJakartaSansFamily)
          HabitRadarSwitch(checked = skipIfDone, onCheckedChange = { skipIfDone = it })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Remind again if not done after", color = colors.textSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        HabitRadarSegmentedControl(
          options = listOf(null, 15, 30, 60),
          selected = repeatAfterMin,
          labelMapper = { if (it == null) "Off" else "${it}m" },
          onSelect = { repeatAfterMin = it },
          height = 42.dp,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Sound", color = colors.textSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        HabitRadarSegmentedControl(
          options = ReminderSound.entries,
          selected = sound,
          labelMapper = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
          onSelect = { sound = it },
          height = 42.dp,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Vibrate", color = colors.textPrimary, fontFamily = PlusJakartaSansFamily)
          HabitRadarSwitch(checked = vibrate, onCheckedChange = { vibrate = it })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Show quick actions (Done · Snooze · Skip)", color = colors.textPrimary, fontFamily = PlusJakartaSansFamily, fontSize = 14.sp)
          HabitRadarSwitch(checked = quickActions, onCheckedChange = { quickActions = it })
        }
      }

      if (isEdit && initialReminder != null && onDelete != null) {
        Spacer(modifier = Modifier.height(14.dp))
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x26EE9A9A))
            .clickable { onDelete(initialReminder) }
        ) {
          Text("Delete reminder", color = Color(0xFFEE9A9A), fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerBottomSheet(
  selectedKey: String,
  accentColor: Color,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var query by remember { mutableStateOf("") }
  var selectedCat by remember { mutableStateOf("All") }

  val filtered = remember(query, selectedCat) {
    HabitIconsCatalog.icons.filter { entry ->
      val catMatch = selectedCat == "All" || entry.category.equals(selectedCat, ignoreCase = true)
      val qMatch = query.isBlank() || entry.label.contains(query, ignoreCase = true) || entry.key.contains(query, ignoreCase = true)
      catMatch && qMatch
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = colors.bg
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(540.dp)
        .padding(horizontal = 16.dp)
    ) {
      Text(
        text = "Choose an icon",
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        color = colors.textPrimary,
        modifier = Modifier.padding(vertical = 8.dp)
      )
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search icons or emoji") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(10.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(HabitIconsCatalog.categories) { cat ->
          val sel = cat == selectedCat
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(18.dp))
              .background(if (sel) colors.accentContainer else colors.surface2)
              .clickable { selectedCat = cat }
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Text(
              text = cat,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = if (sel) colors.accent else colors.textSecondary
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(filtered) { item ->
          val isSel = item.key == selectedKey
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(64.dp)
              .clip(RoundedCornerShape(18.dp))
              .background(if (isSel) accentColor.copy(alpha = 0.25f) else colors.card)
              .then(if (isSel) Modifier.border(2.dp, accentColor, RoundedCornerShape(18.dp)) else Modifier)
              .clickable { onSelect(item.key) }
          ) {
            HabitIconView(iconKey = item.key, tint = if (isSel) accentColor else colors.textPrimary)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateGalleryBottomSheet(
  onPickTemplate: (HabitTemplate) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var selectedCategory by remember { mutableStateOf("All") }

  val list = remember(selectedCategory) {
    StarterTemplatesCatalog.templates.filter {
      selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = colors.bg
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(580.dp)
        .padding(horizontal = 16.dp)
    ) {
      Text(
        text = "Habit ideas",
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        color = colors.textPrimary,
        modifier = Modifier.padding(vertical = 8.dp)
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(StarterTemplatesCatalog.categories) { cat ->
          val sel = cat == selectedCategory
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(18.dp))
              .background(if (sel) colors.accentContainer else colors.surface2)
              .clickable { selectedCategory = cat }
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Text(
              text = cat,
              fontSize = 13.sp,
              color = if (sel) colors.accent else colors.textSecondary
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
      ) {
        list.forEach { tpl ->
          val tint = habitTint(tpl.colorKey, colors.isDark)
          HabitRadarCard(
            backgroundColor = tint.cardBackground,
            onClick = { onPickTemplate(tpl) }
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(tint.iconTileBackground)
              ) {
                HabitIconView(iconKey = tpl.iconKey, tint = tint.baseColor)
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = tpl.name,
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = colors.textPrimary
                )
                Text(
                  text = tpl.description,
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 13.sp,
                  color = colors.textSecondary
                )
              }
              Text(
                text = "${tpl.repeatType.name.lowercase().replaceFirstChar { it.uppercase() }} · ${tpl.trackType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                fontSize = 11.sp,
                color = tint.baseColor
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
