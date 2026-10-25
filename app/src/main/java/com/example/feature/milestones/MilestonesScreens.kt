package com.example.feature.milestones

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.model.Entry
import com.example.core.domain.model.Goal
import com.example.core.domain.model.GoalStatus
import com.example.core.domain.model.Habit
import com.example.core.domain.model.HabitPause
import com.example.core.domain.model.HabitStepMetric
import com.example.core.domain.model.Step
import com.example.core.domain.model.StepType
import com.example.core.domain.usecase.GoalStepEvaluator
import com.example.core.domain.usecase.PaceStatus
import com.example.core.ui.components.CardHairline
import com.example.core.ui.components.HabitCheckCircle
import com.example.core.ui.components.HabitIconView
import com.example.core.ui.components.HabitRadarCard
import com.example.core.ui.components.HabitRadarSegmentedControl
import com.example.core.ui.components.HabitRadarSwitch
import com.example.core.ui.components.MomentumRingView
import com.example.core.ui.components.SectionLabel
import com.example.core.ui.components.SerifHeadlineText
import com.example.core.ui.components.SubPageTopBar
import com.example.core.ui.theme.HabitPaletteRow1
import com.example.core.ui.theme.HabitPaletteRow2
import com.example.core.ui.theme.InstrumentSerifFamily
import com.example.core.ui.theme.LocalHabitRadarColors
import com.example.core.ui.theme.PlusJakartaSansFamily
import com.example.core.ui.theme.habitTint
import com.example.core.ui.theme.parseHexColor
import com.example.feature.habit.IconPickerBottomSheet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

// 5.15 A. MILESTONES LIST SCREEN
@Composable
fun MilestonesListScreen(
  goals: List<Goal>,
  stepsByGoal: Map<String, List<Step>>,
  habitsMap: Map<String, Habit>,
  entriesByHabit: Map<String, List<Entry>>,
  pausesByHabit: Map<String, List<HabitPause>>,
  today: LocalDate,
  onBack: () -> Unit,
  onOpenNewGoal: (Goal?, List<Step>?) -> Unit,
  onOpenGoalDetail: (String) -> Unit,
  onToggleManualStep: (Step) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var showDoneTab by remember { mutableStateOf(false) }
  val expandedGoals = remember { mutableStateListOf<String>() }

  val summaries = remember(goals, stepsByGoal, habitsMap, entriesByHabit, pausesByHabit, today) {
    goals.map { g ->
      GoalStepEvaluator.computeGoalSummary(
        goal = g,
        steps = stepsByGoal[g.id].orEmpty(),
        habitsMap = habitsMap,
        entriesByHabit = entriesByHabit,
        pausesByHabit = pausesByHabit,
        today = today
      )
    }
  }

  val activeSummaries = summaries.filter { it.goal.status == GoalStatus.ACTIVE }
  val doneSummaries = summaries.filter { it.goal.status == GoalStatus.COMPLETED }
  val displayedList = if (showDoneTab) doneSummaries else activeSummaries

  val totalActiveSteps = activeSummaries.sumOf { it.totalStepsCount }
  val doneActiveSteps = activeSummaries.sumOf { it.completedStepsCount }
  val overallActiveRatio = if (totalActiveSteps > 0) (doneActiveSteps.toFloat() / totalActiveSteps.toFloat()).coerceIn(0f, 1f) else 0f

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(
      title = "Milestones",
      onBack = onBack
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .padding(end = 8.dp)
          .size(42.dp)
          .clip(CircleShape)
          .background(colors.accent)
          .clickable { onOpenNewGoal(null, null) }
          .testTag("new_goal_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = "New Goal", tint = colors.onAccent)
      }
    }

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      item {
        SerifHeadlineText(rawText = "Big goals, *small steps.*", fontSizeSp = 34)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "${activeSummaries.size} goals · $doneActiveSteps of $totalActiveSteps steps done",
          fontSize = 14.sp,
          color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.surface2)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(overallActiveRatio)
              .fillMaxHeight()
              .clip(RoundedCornerShape(3.dp))
              .background(colors.accent)
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
        HabitRadarSegmentedControl(
          options = listOf(false, true),
          selected = showDoneTab,
          labelMapper = { if (it) "Done (${doneSummaries.size})" else "Active (${activeSummaries.size})" },
          onSelect = { showDoneTab = it },
          height = 46.dp,
          modifier = Modifier.fillMaxWidth()
        )
      }

      if (displayedList.isEmpty()) {
        item {
          HabitRadarCard {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
              Text(
                text = if (showDoneTab) "No completed goals yet." else "No goals yet. Pick something big and cut it into small steps.",
                fontSize = 15.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
              )
              if (!showDoneTab) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.accent)
                    .clickable { onOpenNewGoal(null, null) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                  Text("+ New goal", color = colors.onAccent, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(14.dp))
                // 3 example starter chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf(
                    Triple("Read 12 books", "book", listOf("Book 1", "Book 2", "Book 3", "Book 4")),
                    Triple("Memorize a chapter", "sunrise", listOf("Verses 1–10", "Verses 11–20", "Verses 21–30", "Full review")),
                    Triple("Save for a laptop", "laptop", listOf("Save first 25%", "Save 50%", "Save 75%", "Reach 100% target"))
                  ).forEach { (title, icon, stepTitles) ->
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface2)
                        .clickable {
                          val gId = UUID.randomUUID().toString()
                          val sampleGoal = Goal(
                            id = gId,
                            title = title,
                            iconKey = icon,
                            colorKey = "#D6BEFA",
                            startDate = today,
                            targetDate = today.plusMonths(3)
                          )
                          val sampleSteps = stepTitles.mapIndexed { idx, sTitle ->
                            Step(id = UUID.randomUUID().toString(), goalId = gId, title = sTitle, sortOrder = idx)
                          }
                          onOpenNewGoal(sampleGoal, sampleSteps)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                      Text(text = "Example: $title", fontSize = 13.sp, color = colors.accent)
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        items(displayedList, key = { it.goal.id }) { summary ->
          val goal = summary.goal
          val tint = habitTint(goal.colorKey, colors.isDark)
          val isExpanded = goal.id in expandedGoals
          val dueChipText = when {
            goal.targetDate == null -> "No date"
            summary.daysRemaining != null && summary.daysRemaining < 0 -> "Overdue ${-summary.daysRemaining} days"
            summary.daysRemaining == 0L -> "Due today"
            else -> "${summary.daysRemaining} days left"
          }
          val isOverdue = summary.daysRemaining != null && summary.daysRemaining < 0

          HabitRadarCard(
            backgroundColor = tint.cardBackground,
            onClick = { onOpenGoalDetail(goal.id) }
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(52.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(tint.iconTileBackground)
              ) {
                HabitIconView(iconKey = goal.iconKey, tint = tint.baseColor)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = goal.title,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = colors.textPrimary
                )
                Text(
                  text = "${summary.completedStepsCount} of ${summary.totalStepsCount} steps",
                  fontSize = 13.sp,
                  color = colors.textSecondary
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (isOverdue) Color(0x33EE9A9A) else tint.subtleElementBg)
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = dueChipText,
                  fontSize = 11.sp,
                  color = if (isOverdue) Color(0xFFEE9A9A) else tint.baseColor
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar + %
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(colors.surface2)
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth((summary.overallPercent / 100f).coerceIn(0.04f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(tint.baseColor)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "${summary.overallPercent}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tint.baseColor
              )
            }

            // Next step row
            if (summary.nextStep != null) {
              val ns = summary.nextStep
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(tint.subtleElementBg)
                  .padding(horizontal = 12.dp, vertical = 8.dp)
              ) {
                Text(
                  text = "Next: ${ns.title}",
                  fontSize = 14.sp,
                  color = colors.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                if (ns.type == StepType.MANUAL) {
                  HabitCheckCircle(
                    isDone = ns.done,
                    isSkipped = false,
                    colorHex = goal.colorKey,
                    size = 30.dp,
                    onClick = { onToggleManualStep(ns) }
                  )
                } else {
                  Text(
                    text = summary.stepProgressLabelMap[ns.id] ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tint.baseColor
                  )
                }
                IconButton(
                  onClick = {
                    if (isExpanded) expandedGoals.remove(goal.id) else expandedGoals.add(goal.id)
                  },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand steps",
                    tint = colors.textSecondary
                  )
                }
              }
            }

            if (isExpanded) {
              val nextThree = summary.steps.filter { !it.done }.take(3)
              Spacer(modifier = Modifier.height(8.dp))
              nextThree.forEach { st ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 6.dp)
                ) {
                  Text(st.title, fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.weight(1f))
                  if (st.type == StepType.MANUAL) {
                    HabitCheckCircle(
                      isDone = st.done,
                      isSkipped = false,
                      colorHex = goal.colorKey,
                      size = 26.dp,
                      onClick = { onToggleManualStep(st) }
                    )
                  } else {
                    Text(summary.stepProgressLabelMap[st.id] ?: "", fontSize = 11.sp, color = tint.baseColor)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

// 5.15 D. GOAL DETAIL SCREEN
@Composable
fun GoalDetailScreen(
  goal: Goal,
  steps: List<Step>,
  habitsMap: Map<String, Habit>,
  entriesByHabit: Map<String, List<Entry>>,
  pausesByHabit: Map<String, List<HabitPause>>,
  today: LocalDate,
  onBack: () -> Unit,
  onEditGoal: () -> Unit,
  onToggleManualStep: (Step) -> Unit,
  onUpdateAmountStep: (Step, Double) -> Unit,
  onSkipStepForNow: (Step) -> Unit,
  onSaveStep: (Step) -> Unit,
  onDeleteStep: (Step) -> Unit,
  onMarkGoalStatus: (GoalStatus) -> Unit,
  onDeleteGoal: () -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val tint = habitTint(goal.colorKey, colors.isDark)

  val summary = remember(goal, steps, habitsMap, entriesByHabit, pausesByHabit, today) {
    GoalStepEvaluator.computeGoalSummary(
      goal = goal,
      steps = steps,
      habitsMap = habitsMap,
      entriesByHabit = entriesByHabit,
      pausesByHabit = pausesByHabit,
      today = today
    )
  }

  var showOverflow by remember { mutableStateOf(false) }
  var editingStep by remember { mutableStateOf<Step?>(null) }
  var showNewStepSheet by remember { mutableStateOf(false) }
  var amountStepperTarget by remember { mutableStateOf<Step?>(null) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.bg)
  ) {
    SubPageTopBar(
      title = goal.title,
      onBack = onBack
    ) {
      IconButton(onClick = onEditGoal) {
        Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = colors.textPrimary)
      }
      Box {
        IconButton(onClick = { showOverflow = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.textPrimary)
        }
        DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
          if (goal.status == GoalStatus.ACTIVE) {
            DropdownMenuItem(
              text = { Text("Mark goal complete") },
              onClick = { showOverflow = false; onMarkGoalStatus(GoalStatus.COMPLETED) }
            )
          } else {
            DropdownMenuItem(
              text = { Text("Reopen goal") },
              onClick = { showOverflow = false; onMarkGoalStatus(GoalStatus.ACTIVE) }
            )
          }
          DropdownMenuItem(
            text = { Text("Delete goal", color = Color(0xFFEE9A9A)) },
            onClick = { showOverflow = false; onDeleteGoal() }
          )
        }
      }
    }

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. Hero card
      item {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(tint.iconTileBackground)
            ) {
              HabitIconView(iconKey = goal.iconKey, tint = tint.baseColor, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(goal.title, fontFamily = InstrumentSerifFamily, fontSize = 28.sp, color = colors.textPrimary)
              if (!goal.description.isNullOrBlank()) {
                Text(goal.description, fontSize = 13.sp, color = colors.textSecondary)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${summary.completedStepsCount} of ${summary.totalStepsCount} steps · ${goal.targetDate?.let { "Finish by ${it.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}" } ?: "No deadline"}",
                fontSize = 12.sp,
                color = colors.textSecondary
              )
            }
            MomentumRingView(score = summary.overallPercent, color = tint.baseColor, size = 68.dp)
          }
        }
      }

      // 2. Next step card
      if (summary.nextStep != null) {
        val ns = summary.nextStep
        item {
          HabitRadarCard(backgroundColor = tint.cardBackground) {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              SectionLabel("NEXT STEP")
              Text(
                text = "Skip for now",
                fontSize = 12.sp,
                color = tint.baseColor,
                modifier = Modifier.clickable { onSkipStepForNow(ns) }
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(ns.title, fontFamily = InstrumentSerifFamily, fontSize = 24.sp, color = colors.textPrimary)
                Text(
                  text = summary.stepProgressLabelMap[ns.id] ?: "",
                  fontSize = 13.sp,
                  color = tint.baseColor
                )
              }
              if (ns.type == StepType.MANUAL) {
                HabitCheckCircle(
                  isDone = ns.done,
                  isSkipped = false,
                  colorHex = goal.colorKey,
                  size = 54.dp,
                  onClick = { onToggleManualStep(ns) }
                )
              } else if (ns.type == StepType.AMOUNT) {
                HabitCheckCircle(
                  isDone = ns.done,
                  isSkipped = false,
                  colorHex = goal.colorKey,
                  size = 54.dp,
                  progressRatio = summary.stepProgressMap[ns.id],
                  progressLabel = "${(ns.amountCurrent ?: 0.0).roundToInt()}",
                  onClick = { amountStepperTarget = ns }
                )
              }
            }
          }
        }
      }

      // 3. Steps timeline with connected vertical line
      item {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          SectionLabel("STEPS TIMELINE")
          Column {
            summary.steps.forEachIndexed { idx, step ->
              val isLast = idx == summary.steps.lastIndex
              Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { editingStep = step }
                  .padding(vertical = 8.dp)
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.width(42.dp)
                ) {
                  HabitCheckCircle(
                    isDone = step.done,
                    isSkipped = false,
                    colorHex = goal.colorKey,
                    size = 34.dp,
                    progressRatio = if (step.type != StepType.MANUAL) summary.stepProgressMap[step.id] else null,
                    onClick = {
                      when (step.type) {
                        StepType.MANUAL -> onToggleManualStep(step)
                        StepType.AMOUNT -> amountStepperTarget = step
                        StepType.HABIT -> { /* Automatic from linked habit */ }
                      }
                    }
                  )
                  if (!isLast) {
                    Canvas(modifier = Modifier.width(2.dp).height(28.dp)) {
                      drawLine(
                        color = tint.baseColor.copy(alpha = 0.35f),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 2.dp.toPx()
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = step.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (step.done) colors.textSecondary else colors.textPrimary,
                    textDecoration = if (step.done) TextDecoration.LineThrough else TextDecoration.None
                  )
                  val subText = buildList {
                    add(summary.stepProgressLabelMap[step.id] ?: "")
                    if (step.type == StepType.HABIT) add("Completes automatically")
                    if (step.dueDate != null) add("Due ${step.dueDate.format(DateTimeFormatter.ofPattern("d MMM"))}")
                  }.filter { it.isNotBlank() }.joinToString(" · ")
                  Text(subText, fontSize = 12.sp, color = colors.textSecondary)
                }
                IconButton(onClick = { onDeleteStep(step) }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete step", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .clip(RoundedCornerShape(22.dp))
              .background(tint.subtleElementBg)
              .clickable { showNewStepSheet = true }
          ) {
            Text("+ Add a small step", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = tint.baseColor)
          }
        }
      }

      // 4. Pace & prediction
      item {
        HabitRadarCard(backgroundColor = tint.cardBackground) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            SectionLabel("PACE & PREDICTION")
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(tint.subtleElementBg)
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(summary.paceStatus.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint.baseColor)
            }
          }
          if (summary.completedStepsCount < 2) {
            Text("Finish a few steps to see a prediction.", fontSize = 14.sp, color = colors.textSecondary)
          } else {
            Text(
              text = "Projected finish: ${summary.projectedFinishDate?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: "-"} · Overdue steps: ${summary.overdueStepsCount}",
              fontSize = 14.sp,
              color = colors.textPrimary
            )
          }
        }
      }

      // 6. Activity log
      val completedSteps = summary.steps.filter { it.done }
      if (completedSteps.isNotEmpty()) {
        item {
          HabitRadarCard(backgroundColor = tint.cardBackground) {
            SectionLabel("ACTIVITY LOG")
            completedSteps.forEach { cs ->
              val dateStr = cs.doneAt?.atZone(ZoneId.systemDefault())?.toLocalDate()
                ?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: "Done"
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
              ) {
                Text(cs.title, fontSize = 14.sp, color = colors.textPrimary)
                Text(dateStr, fontSize = 12.sp, color = tint.baseColor)
              }
            }
          }
        }
      }
    }
  }

  if (showNewStepSheet || editingStep != null) {
    StepEditorBottomSheet(
      initialStep = editingStep,
      goalId = goal.id,
      nextSortOrder = steps.size,
      availableHabits = habitsMap.values.toList(),
      onDismiss = {
        showNewStepSheet = false
        editingStep = null
      },
      onSave = { saved ->
        onSaveStep(saved)
        showNewStepSheet = false
        editingStep = null
      },
      onDelete = { toDel ->
        onDeleteStep(toDel)
        editingStep = null
      }
    )
  }

  if (amountStepperTarget != null) {
    val st = amountStepperTarget!!
    StepAmountUpdateSheet(
      step = st,
      onDismiss = { amountStepperTarget = null },
      onSaveValue = { v ->
        onUpdateAmountStep(st, v)
        amountStepperTarget = null
      }
    )
  }
}

// 5.15 B. NEW / EDIT GOAL SHEET + "BREAK IT DOWN" HELPER
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditGoalBottomSheet(
  existingGoal: Goal?,
  existingSteps: List<Step>,
  availableHabits: List<Habit>,
  prelinkedHabit: Habit? = null,
  today: LocalDate,
  onDismiss: () -> Unit,
  onSave: (Goal, List<Step>) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  val goalId = remember { existingGoal?.id ?: UUID.randomUUID().toString() }

  var title by remember {
    mutableStateOf(
      existingGoal?.title ?: prelinkedHabit?.let { "${it.name} streak milestone" } ?: ""
    )
  }
  var description by remember { mutableStateOf(existingGoal?.description ?: "") }
  var iconKey by remember { mutableStateOf(existingGoal?.iconKey ?: prelinkedHabit?.iconKey ?: "flag") }
  var colorKey by remember { mutableStateOf(existingGoal?.colorKey ?: prelinkedHabit?.colorKey ?: "#D6BEFA") }
  var targetDate by remember { mutableStateOf(existingGoal?.targetDate ?: today.plusMonths(1)) }

  val stepsList = remember {
    mutableStateListOf<Step>().apply {
      if (existingSteps.isNotEmpty()) {
        addAll(existingSteps)
      } else if (prelinkedHabit != null) {
        add(
          Step(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            title = "Reach 14-day streak on ${prelinkedHabit.name}",
            type = StepType.HABIT,
            linkedHabitId = prelinkedHabit.id,
            habitMetric = HabitStepMetric.STREAK,
            habitTarget = 14,
            sortOrder = 0
          )
        )
      } else {
        add(Step(id = UUID.randomUUID().toString(), goalId = goalId, title = "Step 1", sortOrder = 0))
      }
    }
  }

  var newStepDraft by remember { mutableStateOf("") }
  var remindWeekly by remember { mutableStateOf(existingGoal?.remindWeekly ?: false) }
  var remindStepDue by remember { mutableStateOf(existingGoal?.remindStepDue ?: false) }
  var showIconPicker by remember { mutableStateOf(false) }
  var showBreakItDownSheet by remember { mutableStateOf(false) }

  val isValid = title.trim().isNotEmpty() && stepsList.isNotEmpty()
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
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Text(
          text = "Cancel",
          color = colors.textSecondary,
          modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp)
        )
        Text(
          text = if (existingGoal != null) "EDIT GOAL" else "NEW GOAL",
          style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
          color = colors.textSecondary
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isValid) colors.accent else colors.surface2)
            .clickable(enabled = isValid) {
              val savedGoal = Goal(
                id = goalId,
                title = title.trim(),
                description = description.trim().ifBlank { null },
                iconKey = iconKey,
                colorKey = colorKey,
                startDate = existingGoal?.startDate ?: today,
                targetDate = targetDate,
                status = existingGoal?.status ?: GoalStatus.ACTIVE,
                remindWeekly = remindWeekly,
                remindStepDue = remindStepDue
              )
              onSave(savedGoal, stepsList.toList())
            }
            .padding(horizontal = 20.dp, vertical = 9.dp)
            .testTag("save_goal_button")
        ) {
          Text("Save", color = if (isValid) colors.onAccent else colors.textSecondary, fontWeight = FontWeight.Bold)
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(110.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(tint.iconTileBackground)
            .clickable { showIconPicker = true }
        ) {
          HabitIconView(iconKey = iconKey, tint = tint.baseColor, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Change icon",
          fontSize = 13.sp,
          color = colors.accent,
          modifier = Modifier.clickable { showIconPicker = true }
        )

        Spacer(modifier = Modifier.height(18.dp))

        BasicTextField(
          value = title,
          onValueChange = { if (it.length <= 50) title = it },
          textStyle = TextStyle(
            fontFamily = InstrumentSerifFamily,
            fontSize = 30.sp,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
          ),
          cursorBrush = SolidColor(colors.accent),
          decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
              if (title.isEmpty()) {
                Text(
                  text = "Name the big goal",
                  fontFamily = InstrumentSerifFamily,
                  fontSize = 30.sp,
                  color = colors.textSecondary.copy(alpha = 0.6f)
                )
              }
              inner()
            }
          },
          modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // GOAL COLOR
        Column(modifier = Modifier.fillMaxWidth()) {
          SectionLabel("GOAL COLOR")
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              (HabitPaletteRow1 + HabitPaletteRow2).forEach { hex ->
                val sel = colorKey.equals(hex, ignoreCase = true)
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(parseHexColor(hex))
                    .clickable { colorKey = hex }
                ) {
                  if (sel) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF101114))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // FINISH BY quick chips
        Column(modifier = Modifier.fillMaxWidth()) {
          SectionLabel("FINISH BY")
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              "1 week" to today.plusWeeks(1),
              "1 month" to today.plusMonths(1),
              "3 months" to today.plusMonths(3),
              "1 year" to today.plusYears(1)
            ).forEach { (label, dt) ->
              val sel = targetDate == dt
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .background(if (sel) colors.accent else colors.surface2)
                  .clickable { targetDate = dt }
                  .padding(horizontal = 14.dp, vertical = 8.dp)
              ) {
                Text(label, fontSize = 13.sp, color = if (sel) colors.onAccent else colors.textPrimary, fontWeight = FontWeight.Medium)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // STEPS + "Break it down" offline helper
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            SectionLabel("STEPS (${stepsList.size})")
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colors.accentContainer)
                .clickable { showBreakItDownSheet = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("break_it_down_button")
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Break it down", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          stepsList.forEachIndexed { idx, step ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
              Text("${idx + 1}.", fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.width(24.dp))
              Text(step.title, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
              IconButton(
                onClick = { if (stepsList.size > 1) stepsList.removeAt(idx) },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Remove step", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = newStepDraft,
              onValueChange = { newStepDraft = it },
              label = { Text("+ Add a small step") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.accent)
                .clickable {
                  if (newStepDraft.isNotBlank()) {
                    stepsList.add(
                      Step(
                        id = UUID.randomUUID().toString(),
                        goalId = goalId,
                        title = newStepDraft.trim(),
                        sortOrder = stepsList.size
                      )
                    )
                    newStepDraft = ""
                  }
                }
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add step", tint = colors.onAccent)
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // REMINDERS
        Column(modifier = Modifier.fillMaxWidth()) {
          SectionLabel("REMINDERS")
          HabitRadarCard {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Weekly check-in (Sun 18:00)", color = colors.textPrimary, fontSize = 15.sp)
              HabitRadarSwitch(checked = remindWeekly, onCheckedChange = { remindWeekly = it })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Step due dates (09:00)", color = colors.textPrimary, fontSize = 15.sp)
              HabitRadarSwitch(checked = remindStepDue, onCheckedChange = { remindStepDue = it })
            }
          }
        }

        Spacer(modifier = Modifier.height(36.dp))
      }
    }
  }

  if (showIconPicker) {
    IconPickerBottomSheet(
      selectedKey = iconKey,
      accentColor = parseHexColor(colorKey),
      onSelect = { iconKey = it; showIconPicker = false },
      onDismiss = { showIconPicker = false }
    )
  }

  if (showBreakItDownSheet) {
    BreakItDownHelperSheet(
      goalId = goalId,
      startDate = today,
      targetDate = targetDate,
      onDismiss = { showBreakItDownSheet = false },
      onGeneratedSteps = { gen ->
        stepsList.clear()
        stepsList.addAll(gen)
        showBreakItDownSheet = false
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakItDownHelperSheet(
  goalId: String,
  startDate: LocalDate,
  targetDate: LocalDate?,
  onDismiss: () -> Unit,
  onGeneratedSteps: (List<Step>) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var byAmountMode by remember { mutableStateOf(false) }
  var stepCount by remember { mutableIntStateOf(6) }
  var pattern by remember { mutableStateOf("Step {n}") }
  var totalAmountStr by remember { mutableStateOf("300") }
  var perStepStr by remember { mutableStateOf("30") }
  var unitStr by remember { mutableStateOf("pages") }
  var spreadDueDates by remember { mutableStateOf(true) }

  ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.bg) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
      Text(
        text = "Break it down",
        fontFamily = InstrumentSerifFamily,
        fontSize = 28.sp,
        color = colors.textPrimary
      )
      Spacer(modifier = Modifier.height(10.dp))
      HabitRadarSegmentedControl(
        options = listOf(false, true),
        selected = byAmountMode,
        labelMapper = { if (it) "By amount" else "By count" },
        onSelect = { byAmountMode = it },
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(14.dp))

      if (!byAmountMode) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("How many steps? $stepCount", color = colors.textPrimary, fontSize = 16.sp)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.surface2)
                .clickable { if (stepCount > 2) stepCount-- }
            ) { Text("−", color = colors.textPrimary) }
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.accent)
                .clickable { if (stepCount < 30) stepCount++ }
            ) { Text("+", color = colors.onAccent) }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = pattern,
          onValueChange = { pattern = it },
          label = { Text("Name pattern (use {n})") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = totalAmountStr,
            onValueChange = { totalAmountStr = it.filter { c -> c.isDigit() } },
            label = { Text("Total") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = perStepStr,
            onValueChange = { perStepStr = it.filter { c -> c.isDigit() } },
            label = { Text("Per step") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = unitStr,
            onValueChange = { unitStr = it },
            label = { Text("Unit") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Spread due dates evenly", color = colors.textPrimary, fontSize = 15.sp)
        HabitRadarSwitch(checked = spreadDueDates, onCheckedChange = { spreadDueDates = it })
      }

      Spacer(modifier = Modifier.height(18.dp))
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .clip(RoundedCornerShape(25.dp))
          .background(colors.accent)
          .clickable {
            val totalDays = targetDate?.let { max(7L, java.time.temporal.ChronoUnit.DAYS.between(startDate, it)) } ?: 30L
            val generated = if (!byAmountMode) {
              (1..stepCount).map { n ->
                val due = if (spreadDueDates) startDate.plusDays((totalDays * n) / stepCount) else null
                Step(
                  id = UUID.randomUUID().toString(),
                  goalId = goalId,
                  title = pattern.replace("{n}", n.toString()),
                  dueDate = due,
                  sortOrder = n - 1,
                  type = StepType.MANUAL
                )
              }
            } else {
              val tot = (totalAmountStr.toIntOrNull() ?: 300).coerceAtLeast(10)
              val chunk = (perStepStr.toIntOrNull() ?: 30).coerceAtLeast(1)
              val count = ((tot + chunk - 1) / chunk).coerceIn(1, 30)
              (0 until count).map { idx ->
                val low = idx * chunk + 1
                val high = minOf(tot, (idx + 1) * chunk)
                val due = if (spreadDueDates) startDate.plusDays((totalDays * (idx + 1)) / count) else null
                Step(
                  id = UUID.randomUUID().toString(),
                  goalId = goalId,
                  title = "${unitStr.replaceFirstChar { it.uppercase() }} $low–$high",
                  dueDate = due,
                  sortOrder = idx,
                  type = StepType.AMOUNT,
                  amountTarget = (high - low + 1).toDouble(),
                  amountCurrent = 0.0,
                  amountUnit = unitStr
                )
              }
            }
            onGeneratedSteps(generated)
          }
      ) {
        Text("Generate steps", color = colors.onAccent, fontWeight = FontWeight.Bold)
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

// 5.15 C. STEP EDITOR BOTTOM SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepEditorBottomSheet(
  initialStep: Step?,
  goalId: String,
  nextSortOrder: Int,
  availableHabits: List<Habit>,
  onDismiss: () -> Unit,
  onSave: (Step) -> Unit,
  onDelete: ((Step) -> Unit)? = null
) {
  val colors = LocalHabitRadarColors.current
  var title by remember { mutableStateOf(initialStep?.title ?: "") }
  var note by remember { mutableStateOf(initialStep?.note ?: "") }
  var type by remember { mutableStateOf(initialStep?.type ?: StepType.MANUAL) }
  var linkedHabitId by remember { mutableStateOf(initialStep?.linkedHabitId ?: availableHabits.firstOrNull()?.id) }
  var habitMetric by remember { mutableStateOf(initialStep?.habitMetric ?: HabitStepMetric.STREAK) }
  var habitTargetStr by remember { mutableStateOf((initialStep?.habitTarget ?: 14).toString()) }
  var amountTargetStr by remember { mutableStateOf((initialStep?.amountTarget ?: 100.0).roundToInt().toString()) }
  var amountCurrentStr by remember { mutableStateOf((initialStep?.amountCurrent ?: 0.0).roundToInt().toString()) }
  var amountUnit by remember { mutableStateOf(initialStep?.amountUnit ?: "pages") }

  ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.bg) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Cancel", color = colors.textSecondary, modifier = Modifier.clickable(onClick = onDismiss))
        Text(if (initialStep != null) "EDIT STEP" else "NEW STEP", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (title.isNotBlank()) colors.accent else colors.surface2)
            .clickable(enabled = title.isNotBlank()) {
              val amtTarget = amountTargetStr.toDoubleOrNull() ?: 100.0
              val amtCurr = amountCurrentStr.toDoubleOrNull() ?: 0.0
              onSave(
                Step(
                  id = initialStep?.id ?: UUID.randomUUID().toString(),
                  goalId = goalId,
                  title = title.trim(),
                  note = note.trim().ifBlank { null },
                  dueDate = initialStep?.dueDate,
                  sortOrder = initialStep?.sortOrder ?: nextSortOrder,
                  type = type,
                  linkedHabitId = if (type == StepType.HABIT) linkedHabitId else null,
                  habitMetric = if (type == StepType.HABIT) habitMetric else null,
                  habitTarget = if (type == StepType.HABIT) habitTargetStr.toIntOrNull() ?: 14 else null,
                  habitWindowDays = 30,
                  amountTarget = if (type == StepType.AMOUNT) amtTarget else null,
                  amountCurrent = if (type == StepType.AMOUNT) amtCurr else null,
                  amountUnit = if (type == StepType.AMOUNT) amountUnit else null,
                  done = if (type == StepType.AMOUNT) amtCurr >= amtTarget else (initialStep?.done ?: false),
                  doneAt = initialStep?.doneAt
                )
              )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Text("Save", color = colors.onAccent, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Step title") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Note (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(14.dp))

      SectionLabel("STEP TYPE")
      HabitRadarSegmentedControl(
        options = StepType.entries,
        selected = type,
        labelMapper = {
          when (it) {
            StepType.MANUAL -> "Manual"
            StepType.HABIT -> "Habit"
            StepType.AMOUNT -> "Amount"
          }
        },
        onSelect = { type = it },
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))
      when (type) {
        StepType.MANUAL -> {
          Text("Completed by tapping its check circle.", fontSize = 13.sp, color = colors.textSecondary)
        }
        StepType.HABIT -> {
          Text("Choose habit:", fontSize = 13.sp, color = colors.textSecondary)
          Spacer(modifier = Modifier.height(6.dp))
          availableHabits.forEach { h ->
            val sel = h.id == linkedHabitId
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (sel) colors.accentContainer else colors.surface2)
                .clickable { linkedHabitId = h.id }
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(h.name, color = if (sel) colors.accent else colors.textPrimary)
              if (sel) Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent)
            }
            Spacer(modifier = Modifier.height(4.dp))
          }
          Spacer(modifier = Modifier.height(8.dp))
          HabitRadarSegmentedControl(
            options = HabitStepMetric.entries,
            selected = habitMetric,
            labelMapper = {
              when (it) {
                HabitStepMetric.STREAK -> "Streak N"
                HabitStepMetric.TOTAL_CHECKINS -> "Total N"
                HabitStepMetric.RATE -> "Rate ≥ N%"
              }
            },
            onSelect = { habitMetric = it },
            height = 40.dp,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = habitTargetStr,
            onValueChange = { habitTargetStr = it.filter { c -> c.isDigit() } },
            label = { Text("Target number (N)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
        StepType.AMOUNT -> {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = amountCurrentStr,
              onValueChange = { amountCurrentStr = it.filter { c -> c.isDigit() } },
              label = { Text("Current") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = amountTargetStr,
              onValueChange = { amountTargetStr = it.filter { c -> c.isDigit() } },
              label = { Text("Target") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = amountUnit,
              onValueChange = { amountUnit = it },
              label = { Text("Unit") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      if (initialStep != null && onDelete != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(Color(0x26EE9A9A))
            .clickable { onDelete(initialStep) }
        ) {
          Text("Delete step", color = Color(0xFFEE9A9A), fontWeight = FontWeight.SemiBold)
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepAmountUpdateSheet(
  step: Step,
  onDismiss: () -> Unit,
  onSaveValue: (Double) -> Unit
) {
  val colors = LocalHabitRadarColors.current
  var valStr by remember { mutableStateOf((step.amountCurrent ?: 0.0).roundToInt().toString()) }

  ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.bg) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
      Text(step.title, fontFamily = InstrumentSerifFamily, fontSize = 26.sp, color = colors.textPrimary)
      Text("Target: ${(step.amountTarget ?: 100.0).roundToInt()} ${step.amountUnit ?: ""}", fontSize = 13.sp, color = colors.textSecondary)
      Spacer(modifier = Modifier.height(14.dp))
      OutlinedTextField(
        value = valStr,
        onValueChange = { valStr = it.filter { c -> c.isDigit() || c == '.' } },
        label = { Text("Current amount") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(14.dp))
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(colors.accent)
          .clickable { onSaveValue(valStr.toDoubleOrNull() ?: 0.0) }
      ) {
        Text("Update progress", color = colors.onAccent, fontWeight = FontWeight.Bold)
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
