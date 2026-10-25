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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.di.appContainer
import com.example.core.domain.model.ComparisonType
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitCategoryCatalog
import com.example.core.domain.model.HabitTemplate
import com.example.core.domain.model.Reminder
import com.example.core.domain.model.RepeatType
import com.example.core.domain.model.TrackType
import com.example.core.domain.usecase.ScheduleMatcher
import com.example.core.ui.components.CardHairline
import com.example.core.ui.components.HabitIconView
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
import com.example.core.ui.viewmodel.HabitRadarViewModelFactory
import java.time.LocalDate

private val QUICK_NAME_SUGGESTIONS = listOf(
  "Drink Water",
  "Morning Walk",
  "Read 20 Mins",
  "Meditate",
  "Stretch",
  "Journal"
)

/**
 * Full-screen offline-first Habit Creation and Edit Screen backed by [HabitCreationViewModel].
 * Allows users to name a habit, configure its frequency (Daily specific days, Weekly N times,
 * Monthly specific dates, or 1-tap presets), customize tracking & reminders, and persist directly
 * to the local Room database without requiring network access or sign-in.
 */
@Composable
fun HabitCreationScreen(
  today: LocalDate,
  existingHabit: Habit? = null,
  existingReminders: List<Reminder> = emptyList(),
  isDraftTemplate: Boolean = false,
  onBack: () -> Unit,
  onHabitSaved: (Habit) -> Unit = { onBack() },
  onPauseRequest: ((Habit) -> Unit)? = null,
  onArchiveRequest: ((Habit) -> Unit)? = null,
  onDeleteForeverRequest: ((Habit) -> Unit)? = null,
  viewModel: HabitCreationViewModel = viewModel(
    factory = HabitRadarViewModelFactory(LocalContext.current.appContainer)
  )
) {
  LaunchedEffect(existingHabit?.id, isDraftTemplate, today) {
    viewModel.resetForm(
      today = today,
      existingHabit = existingHabit,
      existingReminders = existingReminders,
      isDraftTemplate = isDraftTemplate
    )
  }

  val uiState by viewModel.uiState.collectAsState()

  HabitCreationFormContent(
    uiState = uiState,
    onNameChange = viewModel::updateName,
    onDescriptionChange = viewModel::updateDescription,
    onSelectIcon = viewModel::selectIcon,
    onSelectColor = viewModel::selectColor,
    onSelectTrackType = viewModel::selectTrackType,
    onTargetValueChange = viewModel::updateTargetValue,
    onUnitChange = viewModel::updateUnit,
    onSelectComparison = viewModel::selectComparison,
    onSelectFrequency = viewModel::selectFrequency,
    onSelectFrequencyPreset = viewModel::selectFrequencyPreset,
    onToggleDayOfWeek = viewModel::toggleDayOfWeek,
    onIncrementTimesPerWeek = viewModel::incrementTimesPerWeek,
    onDecrementTimesPerWeek = viewModel::decrementTimesPerWeek,
    onSetTimesPerWeek = viewModel::setTimesPerWeek,
    onToggleDayOfMonth = viewModel::toggleDayOfMonth,
    onSetRemindMeMaster = viewModel::setRemindMeMaster,
    onAddOrUpdateReminder = viewModel::addOrUpdateReminder,
    onRemoveReminder = viewModel::removeReminder,
    onGroupChange = viewModel::updateGroup,
    onSelectCategory = viewModel::selectCategory,
    onCategoryChange = viewModel::updateCategory,
    onTagDraftChange = viewModel::updateTagDraft,
    onAddTag = viewModel::addTag,
    onRemoveTag = viewModel::removeTag,
    onToggleTag = viewModel::toggleTag,
    onGoalStreakChange = viewModel::updateGoalStreak,
    onAskNoteChange = viewModel::setAskNote,
    onApplyTemplate = viewModel::applyTemplate,
    onCancel = onBack,
    onSave = {
      viewModel.saveHabit(onSuccess = onHabitSaved)
    },
    onPauseRequest = onPauseRequest,
    onArchiveRequest = onArchiveRequest,
    onDeleteForeverRequest = onDeleteForeverRequest
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitCreationFormContent(
  uiState: HabitCreationUiState,
  onNameChange: (String) -> Unit,
  onDescriptionChange: (String) -> Unit,
  onSelectIcon: (String) -> Unit,
  onSelectColor: (String) -> Unit,
  onSelectTrackType: (TrackType) -> Unit,
  onTargetValueChange: (String) -> Unit,
  onUnitChange: (String) -> Unit,
  onSelectComparison: (ComparisonType) -> Unit,
  onSelectFrequency: (RepeatType) -> Unit,
  onSelectFrequencyPreset: (FrequencyPreset) -> Unit,
  onToggleDayOfWeek: (Int) -> Unit,
  onIncrementTimesPerWeek: () -> Unit,
  onDecrementTimesPerWeek: () -> Unit,
  onSetTimesPerWeek: (Int) -> Unit,
  onToggleDayOfMonth: (Int) -> Unit,
  onSetRemindMeMaster: (Boolean) -> Unit,
  onAddOrUpdateReminder: (Reminder) -> Unit,
  onRemoveReminder: (String) -> Unit,
  onGroupChange: (String) -> Unit,
  onSelectCategory: (String) -> Unit = {},
  onCategoryChange: (String) -> Unit = {},
  onTagDraftChange: (String) -> Unit = {},
  onAddTag: (String) -> Unit = {},
  onRemoveTag: (String) -> Unit = {},
  onToggleTag: (String) -> Unit = {},
  onGoalStreakChange: (String) -> Unit,
  onAskNoteChange: (Boolean) -> Unit,
  onApplyTemplate: (HabitTemplate) -> Unit,
  onCancel: () -> Unit,
  onSave: () -> Unit,
  onPauseRequest: ((Habit) -> Unit)? = null,
  onArchiveRequest: ((Habit) -> Unit)? = null,
  onDeleteForeverRequest: ((Habit) -> Unit)? = null
) {
  val colors = LocalHabitRadarColors.current
  val tint = habitTint(uiState.colorKey, colors.isDark)

  var showIconPicker by remember { mutableStateOf(false) }
  var showTemplateGallery by remember { mutableStateOf(false) }
  var showMoreSection by remember { mutableStateOf(false) }
  var editingReminder by remember { mutableStateOf<Reminder?>(null) }
  var showNewReminderSheet by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .imePadding()
      .testTag("habit_creation_screen")
  ) {
    // Top Header Bar: Cancel · Title & Offline Status · Save pill
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
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClick = onCancel)
          .padding(horizontal = 10.dp, vertical = 8.dp)
          .testTag("cancel_habit_button")
      )

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = if (uiState.isEditMode) "EDIT HABIT" else "NEW HABIT",
          style = MaterialTheme.typography.labelSmall,
          color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.testTag("offline_ready_badge")
        ) {
          Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = "Offline first local storage",
            tint = colors.accent,
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Saved locally · Offline ready",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            color = colors.textSecondary
          )
        }
      }

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(if (uiState.isValid) colors.accent else colors.surface2)
          .clickable(enabled = !uiState.isSaving) { onSave() }
          .padding(horizontal = 20.dp, vertical = 10.dp)
          .testTag("save_habit_button")
      ) {
        Text(
          text = if (uiState.isSaving) "Saving…" else "Save",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (uiState.isValid) colors.onAccent else colors.textSecondary
        )
      }
    }

    // Validation Error Banner
    if (uiState.errorMessage != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0x26EE9A9A))
          .padding(horizontal = 16.dp, vertical = 10.dp)
          .testTag("habit_form_error")
      ) {
        Text(
          text = uiState.errorMessage,
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFFEE9A9A)
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
      // 0. Browse ideas starter pill (New habit only)
      if (!uiState.isEditMode) {
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
            text = "Browse starter templates",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // 1. Icon tile with radial glow + Change icon pill
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(120.dp)
          .clip(RoundedCornerShape(32.dp))
          .background(
            Brush.radialGradient(
              colors = listOf(
                tint.baseColor.copy(alpha = 0.34f),
                tint.iconTileBackground
              )
            )
          )
          .clickable { showIconPicker = true }
          .testTag("habit_icon_picker_trigger")
      ) {
        HabitIconView(
          iconKey = uiState.iconKey,
          tint = Color.White,
          modifier = Modifier.size(54.dp),
          emojiSize = 42.sp
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(colors.surface2)
          .clickable { showIconPicker = true }
          .padding(horizontal = 16.dp, vertical = 7.dp)
      ) {
        Text(
          text = "Change icon",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = colors.accent
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 2. Habit Name Field ("Name it plainly") + Quick Suggestions
      BasicTextField(
        value = uiState.name,
        onValueChange = onNameChange,
        textStyle = TextStyle(
          fontFamily = InstrumentSerifFamily,
          fontSize = 32.sp,
          color = colors.textPrimary,
          textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { innerTextField ->
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            if (uiState.name.isEmpty()) {
              Text(
                text = "Name your habit…",
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

      Spacer(modifier = Modifier.height(8.dp))

      // Quick name chips when name is blank
      if (uiState.name.isBlank()) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
        ) {
          QUICK_NAME_SUGGESTIONS.forEach { suggestion ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface2)
                .clickable { onNameChange(suggestion) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("name_suggestion_${suggestion.lowercase().replace(' ', '_')}")
            ) {
              Text(
                text = suggestion,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.sp,
                color = colors.textSecondary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 3. Description field
      BasicTextField(
        value = uiState.description,
        onValueChange = onDescriptionChange,
        textStyle = TextStyle(
          fontFamily = PlusJakartaSansFamily,
          fontSize = 15.sp,
          color = colors.textSecondary,
          textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { inner ->
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (uiState.description.isEmpty()) {
              Text(
                text = "Add optional note or motivation",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 15.sp,
                color = colors.textSecondary.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
              )
            }
            inner()
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("habit_description_input")
      )

      Spacer(modifier = Modifier.height(20.dp))
      CardHairline()
      Spacer(modifier = Modifier.height(18.dp))

      // 3.5. CATEGORY & TAGS SECTION (Organize habits by Fitness, Mindfulness, Learning, etc. for analytics)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("habit_category_tags_section")
      ) {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          SectionLabel("CATEGORY & TAGS")
          if (uiState.category.isNotBlank()) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accentContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("selected_category_badge")
            ) {
              Text(
                text = uiState.category,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
              )
            }
          }
        }

        Text(
          text = "Organize by area (Fitness, Mindfulness, Learning) for analytics filtering",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 12.sp,
          color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preset Category Chips (Fitness, Mindfulness, Learning, Health, Sleep, Productivity, Finance, Creativity)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_category_chips_row")
        ) {
          HabitCategoryCatalog.PRESET_CATEGORIES.forEach { cat ->
            val isSelected = uiState.category.equals(cat, ignoreCase = true)
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) colors.accent else colors.surface2)
                .clickable { onSelectCategory(cat) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("habit_category_chip_${cat.lowercase()}")
            ) {
              Text(
                text = cat,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.onAccent else colors.textPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Category Input
        OutlinedTextField(
          value = uiState.category,
          onValueChange = onCategoryChange,
          label = { Text("Category / Area (e.g., Fitness, Mindfulness, Learning)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_category_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tags Sub-section
        Text(
          text = "Tags (${uiState.tags.size} active)",
          fontFamily = PlusJakartaSansFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Active removable tags row
        if (uiState.tags.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 10.dp)
              .testTag("habit_active_tags_row")
          ) {
            uiState.tags.forEach { tag ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .background(colors.accentContainer)
                  .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                  .clickable { onRemoveTag(tag) }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
                  .testTag("habit_selected_tag_${tag.lowercase().replace(' ', '_')}")
              ) {
                Text(
                  text = "#$tag  ×",
                  fontFamily = PlusJakartaSansFamily,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = colors.accent
                )
              }
            }
          }
        }

        // Suggested tags based on selected category
        val suggestedTags = remember(uiState.category) {
          HabitCategoryCatalog.suggestedTagsForCategory(uiState.category)
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 10.dp)
        ) {
          suggestedTags.forEach { suggested ->
            val isTagged = uiState.tags.any { it.equals(suggested, ignoreCase = true) }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isTagged) colors.accentContainer else colors.surface2)
                .clickable { onToggleTag(suggested) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("habit_suggested_tag_${suggested.lowercase().replace(' ', '_')}")
            ) {
              Text(
                text = if (isTagged) "✓ #$suggested" else "+ #$suggested",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.sp,
                fontWeight = if (isTagged) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isTagged) colors.accent else colors.textSecondary
              )
            }
          }
        }

        // Custom tag input + Add Tag button
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = uiState.tagDraftInput,
            onValueChange = onTagDraftChange,
            label = { Text("Add custom tag (e.g., Cardio, DeepFocus)") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("habit_tag_input")
          )
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .height(52.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(if (uiState.tagDraftInput.isNotBlank()) colors.accent else colors.surface2)
              .clickable(enabled = uiState.tagDraftInput.isNotBlank()) {
                onAddTag(uiState.tagDraftInput)
              }
              .padding(horizontal = 16.dp)
              .testTag("add_habit_tag_button")
          ) {
            Text(
              text = "+ Tag",
              fontFamily = PlusJakartaSansFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = if (uiState.tagDraftInput.isNotBlank()) colors.onAccent else colors.textSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))
      CardHairline()
      Spacer(modifier = Modifier.height(18.dp))

      // 4. FREQUENCY / SCHEDULE SECTION
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          SectionLabel("FREQUENCY")
          Text(
            text = uiState.frequencySummary,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent,
            modifier = Modifier.testTag("frequency_summary_text")
          )
        }

        // Quick Frequency Preset Pills
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 14.dp)
        ) {
          FrequencyPreset.entries.forEach { preset ->
            val isSelected = uiState.activePreset == preset
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) colors.accentContainer else colors.surface2)
                .then(
                  if (isSelected) {
                    Modifier.border(1.dp, colors.accent, RoundedCornerShape(18.dp))
                  } else {
                    Modifier
                  }
                )
                .clickable { onSelectFrequencyPreset(preset) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("frequency_preset_${preset.name.lowercase()}")
            ) {
              Text(
                text = preset.label,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) colors.accent else colors.textPrimary
              )
            }
          }
        }

        // Segmented Control: Daily | Weekly | Monthly
        HabitRadarSegmentedControl(
          options = RepeatType.entries,
          selected = uiState.repeatType,
          labelMapper = {
            when (it) {
              RepeatType.DAILY -> "Daily"
              RepeatType.WEEKLY -> "Weekly"
              RepeatType.MONTHLY -> "Monthly"
            }
          },
          onSelect = onSelectFrequency,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("frequency_repeat_type_selector")
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState.repeatType) {
          RepeatType.DAILY -> {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Scheduled days of the week",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
              )
              Text(
                text = "${uiState.daysOfWeek.size}/7 days",
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.sp,
                color = colors.textSecondary
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              (1..7).forEach { dow ->
                val isSelected = dow in uiState.daysOfWeek
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) colors.accent else colors.surface2)
                    .clickable { onToggleDayOfWeek(dow) }
                    .testTag("frequency_day_$dow")
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
                Column {
                  Text(
                    text = "${uiState.timesPerPeriod} times per week",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.testTag("frequency_weekly_count")
                  )
                  Text(
                    text = "Complete on any days of the week",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape)
                      .background(colors.surface2)
                      .clickable { onDecrementTimesPerWeek() }
                      .testTag("frequency_weekly_decrement")
                  ) {
                    Text("−", fontSize = 20.sp, color = colors.textPrimary)
                  }
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape)
                      .background(colors.accent)
                      .clickable { onIncrementTimesPerWeek() }
                      .testTag("frequency_weekly_increment")
                  ) {
                    Text("+", fontSize = 20.sp, color = colors.onAccent)
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                (1..7).forEach { count ->
                  val isSel = uiState.timesPerPeriod == count
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .weight(1f)
                      .height(38.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (isSel) colors.accentContainer else colors.surface2)
                      .clickable { onSetTimesPerWeek(count) }
                      .testTag("frequency_weekly_option_$count")
                  ) {
                    Text(
                      text = "${count}x",
                      fontFamily = PlusJakartaSansFamily,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isSel) colors.accent else colors.textSecondary
                    )
                  }
                }
              }
            }
          }

          RepeatType.MONTHLY -> {
            Text(
              text = "Days of month (${uiState.daysOfMonth.size} selected)",
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
                val sel = day in uiState.daysOfMonth
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) colors.accent else colors.surface2)
                    .clickable { onToggleDayOfMonth(day) }
                    .testTag("frequency_month_day_$day")
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

      // 5. HABIT COLOR (2 rows x 8 swatches)
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
                  val isSelected = uiState.colorKey.equals(hex, ignoreCase = true)
                  val c = parseHexColor(hex)
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(52.dp)
                      .then(
                        if (isSelected) {
                          Modifier.border(2.dp, Color(0xFF8FBDF0), RoundedCornerShape(16.dp))
                        } else {
                          Modifier
                        }
                      )
                      .padding(4.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(c)
                      .clickable { onSelectColor(hex) }
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

      // 6. TRACK segmented: Task | Amount | Time
      Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("TRACK")
        HabitRadarSegmentedControl(
          options = TrackType.entries,
          selected = uiState.trackType,
          labelMapper = {
            when (it) {
              TrackType.TASK -> "Task"
              TrackType.AMOUNT -> "Amount"
              TrackType.TIME -> "Time"
            }
          },
          onSelect = onSelectTrackType,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("track_type_selector")
        )

        if (uiState.trackType != TrackType.TASK) {
          Spacer(modifier = Modifier.height(14.dp))
          HabitRadarCard {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              OutlinedTextField(
                value = uiState.targetValueInput,
                onValueChange = onTargetValueChange,
                label = {
                  Text(if (uiState.trackType == TrackType.AMOUNT) "Target amount" else "Target minutes")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("habit_target_value_input")
              )
              if (uiState.trackType == TrackType.AMOUNT) {
                OutlinedTextField(
                  value = uiState.unit,
                  onValueChange = onUnitChange,
                  label = { Text("Unit") },
                  singleLine = true,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("habit_unit_input")
                )
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HabitRadarSegmentedControl(
              options = ComparisonType.entries,
              selected = uiState.comparison,
              labelMapper = {
                when (it) {
                  ComparisonType.AT_LEAST -> "At least"
                  ComparisonType.AT_MOST -> "At most"
                  ComparisonType.EXACTLY -> "Exactly"
                }
              },
              onSelect = onSelectComparison,
              height = 44.dp,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))
      CardHairline()
      Spacer(modifier = Modifier.height(18.dp))

      // 7. REMINDERS section
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
              checked = uiState.remindMeMaster,
              onCheckedChange = onSetRemindMeMaster
            )
          }

          if (uiState.remindMeMaster) {
            Spacer(modifier = Modifier.height(12.dp))
            uiState.reminders.forEach { r ->
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
                    text = if (r.daysOfWeek.size == 7) {
                      "Everyday"
                    } else {
                      r.daysOfWeek.sorted().joinToString(", ") { ScheduleMatcher.dowShortLabel(it) }
                    },
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                  )
                }
                IconButton(onClick = { onRemoveReminder(r.id) }) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete reminder",
                    tint = colors.textSecondary
                  )
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.reminders.size < 5) {
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
              listOf("Morning", "Afternoon", "Evening").forEach { preset ->
                val sel = uiState.groupName.equals(preset, ignoreCase = true)
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) colors.accentContainer else colors.surface2)
                    .clickable { onGroupChange(if (sel) "" else preset) }
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
              value = uiState.groupName,
              onValueChange = onGroupChange,
              label = { Text("Custom group (optional)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = uiState.goalStreakInput,
              onValueChange = onGoalStreakChange,
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
              HabitRadarSwitch(
                checked = uiState.askNote,
                onCheckedChange = onAskNoteChange
              )
            }
          }
        }
      }

      // Edit mode actions: Pause, Archive, Delete
      if (uiState.isEditMode && uiState.existingHabit != null) {
        val targetHabit = uiState.existingHabit
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
                .clickable { onPauseRequest(targetHabit) }
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
                .clickable { onArchiveRequest(targetHabit) }
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

  if (showIconPicker) {
    IconPickerBottomSheet(
      selectedKey = uiState.iconKey,
      accentColor = parseHexColor(uiState.colorKey),
      onSelect = {
        onSelectIcon(it)
        showIconPicker = false
      },
      onDismiss = { showIconPicker = false }
    )
  }

  if (showTemplateGallery) {
    TemplateGalleryBottomSheet(
      onPickTemplate = { tpl ->
        onApplyTemplate(tpl)
        showTemplateGallery = false
      },
      onDismiss = { showTemplateGallery = false }
    )
  }

  if (showNewReminderSheet || editingReminder != null) {
    ReminderEditorBottomSheet(
      initialReminder = editingReminder,
      habitId = uiState.existingHabit?.id ?: "",
      habitName = uiState.name.ifBlank { "this habit" },
      defaultDays = uiState.daysOfWeek,
      onDismiss = {
        showNewReminderSheet = false
        editingReminder = null
      },
      onSave = { saved ->
        onAddOrUpdateReminder(saved)
        showNewReminderSheet = false
        editingReminder = null
      },
      onDelete = { toDel ->
        onRemoveReminder(toDel.id)
        editingReminder = null
      }
    )
  }

  if (showDeleteDialog && uiState.existingHabit != null) {
    val existing = uiState.existingHabit
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete ${existing.name}?") },
      text = {
        Text("This permanently deletes the habit and all its history. Archive it instead?")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onArchiveRequest?.invoke(existing)
          }
        ) {
          Text("Archive", color = colors.accent, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onDeleteForeverRequest?.invoke(existing)
          }
        ) {
          Text("Delete forever", color = Color(0xFFEE9A9A))
        }
      }
    )
  }
}
