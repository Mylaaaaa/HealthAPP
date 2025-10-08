// app/src/main/java/com/example/myhealth/presentation/screen/exercisesession/ExercisePlanScreen.kt
package com.example.myhealth.presentation.screen.exercisesession


import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/* ---------------------------------------------------------------------------
   Planner screen
   - First time: multi-step wizard (rich questionnaire)
   - After saved: overview (chips + weekly plan cards)
   - Tap a day card -> show Day Plan detail (BottomSheet) with actions.
   - All comments are in English for submission.
   --------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExercisePlanScreen(
    modifier: Modifier = Modifier,
    // Fired when user taps "Start guided" on a day plan (parent can navigate).
    onStartDay: (PlanDay) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember(context) { ExercisePrefs(context) }
    // NEW: tiny store to persist “done today” for each plan-day
    val progressStore = remember { PlanProgressStore(context.applicationContext) }
    val activeStore = remember { ActiveDayProgressStore(context.applicationContext) }
    // Load persisted profile or a default
    val persisted = prefs.loadProfileOrNull()
    var profile by rememberSaveable(stateSaver = ProfileSaver) { mutableStateOf(persisted ?: UserProfile()) }
    var isSaved by rememberSaveable { mutableStateOf(persisted != null) }

    // BottomSheet state for Day Plan detail
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = Hidden, skipHalfExpanded = true)
    var sheetDay by remember { mutableStateOf<PlanDay?>(null) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetContent = {
            DayPlanSheet(
                day = sheetDay,
                isDoneToday = sheetDay?.let { progressStore.isDone(LocalDate.now(), it.title) } ?: false,
                onStart = { day ->
                    // if user chooses "Redo", drop today's Done flag first
                    if (progressStore.isDone(LocalDate.now(), day.title)) {
                        progressStore.clear(LocalDate.now(), day.title)
                        activeStore.clear(LocalDate.now(), day.title)
                    }
                    onStartDay(day)
                    scope.launch { sheetState.hide() }
                },
                onQuickDone = { day ->
                    // Quick mark puts Done back
                    progressStore.markDone(LocalDate.now(), day.title)
                    Toast.makeText(context, "Marked done: ${day.title}", Toast.LENGTH_SHORT).show()
                    scope.launch { sheetState.hide() }
                }
            )

        }
    ) {
        if (isSaved) {
            PlanOverview(
                profile = profile,
                onReCustomise = { isSaved = false }, // back to wizard with current answers
                onReset = {                      // clear storage and go to wizard
                    prefs.clear()
                    profile = UserProfile()
                    isSaved = false
                },
                // When a day card is clicked, open the bottom sheet
                onDayClick = { day ->
                    sheetDay = day
                    scope.launch { sheetState.show() }
                },
                // NEW: tell list which days are done today (to show Done chip)
                isDoneToday = { day -> progressStore.isDone(LocalDate.now(), day.title) }
            )
        } else {
            PlannerWizard(
                initial = profile,
                onSave = { p -> prefs.saveProfile(p); profile = p; isSaved = true }
            )
        }
    }
}

/* ------------------------------ DATA MODEL ------------------------------- */

enum class Goal { LoseWeight, GainMuscle, Maintain, ImproveEndurance }
enum class Experience { Beginner, Intermediate, Advanced }
enum class Equipment { None, Minimal, FullGym }
enum class SessionLength { Short15, Standard30, Long45 }
enum class TimeOfDay { Morning, Afternoon, Evening }
enum class WorkoutType { Cardio, Strength, Mobility, HIIT, Core }

data class UserProfile(
    val goal: Goal = Goal.LoseWeight,
    val experience: Experience = Experience.Beginner,
    val equipment: Equipment = Equipment.None,
    val daysPerWeek: Int = 3, // 1..7 supported
    val sessionLength: SessionLength = SessionLength.Standard30,
    val preferIndoor: Boolean = true,
    val hasInjury: Boolean = false,
    val preferredTypes: Set<WorkoutType> = setOf(WorkoutType.Cardio, WorkoutType.Core),
    val timeOfDay: TimeOfDay = TimeOfDay.Morning,
    val heightCm: Int? = null,
    val weightKg: Float? = null,
    val targetWeightKg: Float? = null,
    val availableDays: Set<DayOfWeek> = emptySet() // if empty, we auto-distribute
)

/* ------------------------------ PREFERENCES ------------------------------ */

private class ExercisePrefs(private val context: Context) {
    private val sp get() = context.getSharedPreferences("exercise_planner", Context.MODE_PRIVATE)

    fun saveProfile(p: UserProfile) = sp.edit {
        putString("goal", p.goal.name)
        putString("experience", p.experience.name)
        putString("equipment", p.equipment.name)
        putInt("daysPerWeek", p.daysPerWeek)
        putString("sessionLength", p.sessionLength.name)
        putBoolean("preferIndoor", p.preferIndoor)
        putBoolean("hasInjury", p.hasInjury)
        putString("preferredTypes", p.preferredTypes.joinToString(",") { it.name })
        putString("timeOfDay", p.timeOfDay.name)
        putString("heightCm", p.heightCm?.toString() ?: "")
        putString("weightKg", p.weightKg?.toString() ?: "")
        putString("targetWeightKg", p.targetWeightKg?.toString() ?: "")
        putString("availableDays", p.availableDays.joinToString(",") { it.name })
    }

    fun loadProfileOrNull(): UserProfile? {
        if (!sp.contains("goal")) return null
        val parseInt = { s: String? -> s?.toIntOrNull() }
        val parseFloat = { s: String? -> s?.toFloatOrNull() }
        return UserProfile(
            goal = runCatching { Goal.valueOf(sp.getString("goal", Goal.LoseWeight.name)!!) }.getOrDefault(Goal.LoseWeight),
            experience = runCatching { Experience.valueOf(sp.getString("experience", Experience.Beginner.name)!!) }.getOrDefault(Experience.Beginner),
            equipment = runCatching { Equipment.valueOf(sp.getString("equipment", Equipment.None.name)!!) }.getOrDefault(Equipment.None),
            daysPerWeek = sp.getInt("daysPerWeek", 3),
            sessionLength = runCatching { SessionLength.valueOf(sp.getString("sessionLength", SessionLength.Standard30.name)!!) }.getOrDefault(SessionLength.Standard30),
            preferIndoor = sp.getBoolean("preferIndoor", true),
            hasInjury = sp.getBoolean("hasInjury", false),
            preferredTypes = sp.getString("preferredTypes", "Cardio,Core")!!
                .split(",").filter { it.isNotBlank() }
                .mapNotNull { runCatching { WorkoutType.valueOf(it) }.getOrNull() }
                .toSet().ifEmpty { setOf(WorkoutType.Cardio) },
            timeOfDay = runCatching { TimeOfDay.valueOf(sp.getString("timeOfDay", TimeOfDay.Morning.name)!!) }.getOrDefault(TimeOfDay.Morning),
            heightCm = parseInt(sp.getString("heightCm", "")),
            weightKg = parseFloat(sp.getString("weightKg", "")),
            targetWeightKg = parseFloat(sp.getString("targetWeightKg", "")),
            availableDays = sp.getString("availableDays", "")!!
                .split(",").filter { it.isNotBlank() }
                .mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                .toSet()
        )
    }

    fun clear() = sp.edit { clear() }
}

/* ------------------------------ WIZARD UI -------------------------------- */

@Composable
private fun PlannerWizard(initial: UserProfile, onSave: (UserProfile) -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    var draft by rememberSaveable(stateSaver = ProfileSaver) { mutableStateOf(initial) }
    val totalSteps = 11 // 0..10

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Tell us about you", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = (step + 1) / totalSteps.toFloat(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        when (step) {
            0 -> GoalStep(draft.goal) { draft = draft.copy(goal = it) }
            1 -> ExperienceStep(draft.experience) { draft = draft.copy(experience = it) }
            2 -> EquipmentStep(draft.equipment) { draft = draft.copy(equipment = it) }
            3 -> DaysPerWeekStep(draft.daysPerWeek) { draft = draft.copy(daysPerWeek = it) }
            4 -> SessionLengthStep(draft.sessionLength) { draft = draft.copy(sessionLength = it) }
            5 -> PreferredTypesStep(draft.preferredTypes) { draft = draft.copy(preferredTypes = it) }
            6 -> EnvironmentStep(draft.preferIndoor, draft.hasInjury) { indoor, injury ->
                draft = draft.copy(preferIndoor = indoor, hasInjury = injury)
            }
            7 -> TimeOfDayStep(draft.timeOfDay) { draft = draft.copy(timeOfDay = it) }
            8 -> BodyMetricsStep(draft.heightCm, draft.weightKg, draft.targetWeightKg) { h, w, t ->
                draft = draft.copy(heightCm = h, weightKg = w, targetWeightKg = t)
            }
            9 -> AvailabilityStep(draft.availableDays) { days -> draft = draft.copy(availableDays = days) }
            10 -> SummaryStep(draft)
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(enabled = step > 0, onClick = { step -= 1 }) { Text("Back") }
            if (step < totalSteps - 1) Button(onClick = { step += 1 }) { Text("Next") }
            else Button(onClick = { onSave(draft) }) { Text("Save my plan") }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "You can re-customize later from the plan screen.",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

/* ----------------------------- WIZARD STEPS ------------------------------ */

@Composable private fun GoalStep(selected: Goal, onSelect: (Goal) -> Unit) {
    StepCard("Your primary goal") {
        ChoiceRadio("Lose weight", selected == Goal.LoseWeight) { onSelect(Goal.LoseWeight) }
        ChoiceRadio("Gain muscle", selected == Goal.GainMuscle) { onSelect(Goal.GainMuscle) }
        ChoiceRadio("Maintain", selected == Goal.Maintain) { onSelect(Goal.Maintain) }
        ChoiceRadio("Improve endurance", selected == Goal.ImproveEndurance) { onSelect(Goal.ImproveEndurance) }
    }
}
@Composable private fun ExperienceStep(selected: Experience, onSelect: (Experience) -> Unit) {
    StepCard("Training experience") {
        ChoiceRadio("Beginner", selected == Experience.Beginner) { onSelect(Experience.Beginner) }
        ChoiceRadio("Intermediate", selected == Experience.Intermediate) { onSelect(Experience.Intermediate) }
        ChoiceRadio("Advanced", selected == Experience.Advanced) { onSelect(Experience.Advanced) }
    }
}
@Composable private fun EquipmentStep(selected: Equipment, onSelect: (Equipment) -> Unit) {
    StepCard("Equipment availability") {
        ChoiceRadio("No equipment", selected == Equipment.None) { onSelect(Equipment.None) }
        ChoiceRadio("Minimal (bands/dumbbells)", selected == Equipment.Minimal) { onSelect(Equipment.Minimal) }
        ChoiceRadio("Full gym", selected == Equipment.FullGym) { onSelect(Equipment.FullGym) }
    }
}
@Composable private fun DaysPerWeekStep(value: Int, onChange: (Int) -> Unit) {
    StepCard("How many days per week?") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            (1..7).forEach { d ->
                ChoiceChip(text = "$d", selected = value == d) { onChange(d) }
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("You can also mark specific available days later.", style = MaterialTheme.typography.body2)
    }
}
@Composable private fun SessionLengthStep(selected: SessionLength, onSelect: (SessionLength) -> Unit) {
    StepCard("Typical session length") {
        Row {
            ChoiceChip("~15 min", selected == SessionLength.Short15) { onSelect(SessionLength.Short15) }
            Spacer(Modifier.width(8.dp))
            ChoiceChip("~30 min", selected == SessionLength.Standard30) { onSelect(SessionLength.Standard30) }
            Spacer(Modifier.width(8.dp))
            ChoiceChip("~45+ min", selected == SessionLength.Long45) { onSelect(SessionLength.Long45) }
        }
    }
}
@Composable private fun PreferredTypesStep(selected: Set<WorkoutType>, onChange: (Set<WorkoutType>) -> Unit) {
    StepCard("Preferred workout types") {
        val all = listOf(WorkoutType.Cardio, WorkoutType.Strength, WorkoutType.Mobility, WorkoutType.HIIT, WorkoutType.Core)
        Row(Modifier.fillMaxWidth()) {
            all.forEach { t ->
                val s = t in selected
                ChoiceChip(t.name, s) { onChange(if (s) selected - t else selected + t) }
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Tip: choose 2–3 types you enjoy the most.", style = MaterialTheme.typography.body2)
    }
}
@Composable private fun EnvironmentStep(indoor: Boolean, injury: Boolean, onChange: (Boolean, Boolean) -> Unit) {
    StepCard("Environment & limitations") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChoiceChip("Indoor", indoor) { onChange(true, injury) }
            Spacer(Modifier.width(8.dp))
            ChoiceChip("Outdoor", !indoor) { onChange(false, injury) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = injury, onCheckedChange = { onChange(indoor, it) })
            Spacer(Modifier.width(8.dp))
            Text("I currently have an injury or limitation")
        }
    }
}
@Composable private fun TimeOfDayStep(selected: TimeOfDay, onSelect: (TimeOfDay) -> Unit) {
    StepCard("Preferred time of day") {
        Row {
            ChoiceChip("Morning", selected == TimeOfDay.Morning) { onSelect(TimeOfDay.Morning) }
            Spacer(Modifier.width(8.dp))
            ChoiceChip("Afternoon", selected == TimeOfDay.Afternoon) { onSelect(TimeOfDay.Afternoon) }
            Spacer(Modifier.width(8.dp))
            ChoiceChip("Evening", selected == TimeOfDay.Evening) { onSelect(TimeOfDay.Evening) }
        }
    }
}
@Composable private fun BodyMetricsStep(height: Int?, weight: Float?, target: Float?, onChange: (Int?, Float?, Float?) -> Unit) {
    var h by rememberSaveable { mutableStateOf(height?.toString() ?: "") }
    var w by rememberSaveable { mutableStateOf(weight?.toString() ?: "") }
    var t by rememberSaveable { mutableStateOf(target?.toString() ?: "") }
    StepCard("Body metrics (optional)") {
        LabeledField("Height (cm)", h) { h = it }
        Spacer(Modifier.height(8.dp))
        LabeledField("Weight (kg)", w) { w = it }
        Spacer(Modifier.height(8.dp))
        LabeledField("Target weight (kg)", t) { t = it }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { onChange(h.toIntOrNull(), w.toFloatOrNull(), t.toFloatOrNull()) }) { Text("Apply") }
    }
}
@Composable private fun AvailabilityStep(selected: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    StepCard("Which days are you available?") {
        val all = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )
        Column {
            all.chunked(4).forEach { row ->
                Row {
                    row.forEach { d ->
                        val s = d in selected
                        ChoiceChip(d.name.take(3), s) { onChange(if (s) selected - d else selected + d) }
                        Spacer(Modifier.width(8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Text("If none selected, days will be auto-distributed.", style = MaterialTheme.typography.body2)
    }
}
@Composable private fun SummaryStep(draft: UserProfile) {
    StepCard("Summary") {
        Text("Goal: ${draft.goal}")
        Text("Experience: ${draft.experience}")
        Text("Equipment: ${draft.equipment}")
        Text("Days/week: ${draft.daysPerWeek}")
        Text("Session length: ${draft.sessionLength}")
        Text("Preferred types: ${draft.preferredTypes.joinToString()}")
        Text("Indoor: ${draft.preferIndoor}")
        Text("Injury: ${draft.hasInjury}")
        Text("Time of day: ${draft.timeOfDay}")
        Text("Height: ${draft.heightCm ?: "-"} cm, Weight: ${draft.weightKg ?: "-"} kg, Target: ${draft.targetWeightKg ?: "-"} kg")
        Text("Available days: ${draft.availableDays.joinToString { it.name.take(3) }.ifBlank { "Auto" }}")
    }
}

/* ----------------------------- SAVED VIEW (PRETTY) ----------------------- */

@Composable
private fun PlanOverview(
    profile: UserProfile,
    onReCustomise: () -> Unit,
    onReset: () -> Unit,
    onDayClick: (PlanDay) -> Unit,
    // NEW: injected checker for “done today”
    isDoneToday: (PlanDay) -> Boolean
) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {

        // Header with "Re-customize"
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your plan", style = MaterialTheme.typography.h6)

            // Confirmation dialog
            var showConfirmDialog by remember { mutableStateOf(false) }
            Row {
                TextButton(onClick = { showConfirmDialog = true }) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Re-customize")
                }
            }
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("Re-customize Plan") },
                    text = { Text("Do you want to re-customize your plan? Your current settings will be replaced once you save the new one.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirmDialog = false
                            onReCustomise()
                        }) { Text("Yes, re-customize") }
                    },
                    dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") } }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        // Profile summary card with chips
        Card(elevation = 3.dp, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoalBadge(profile.goal)
                    Spacer(Modifier.width(12.dp))
                    Text(titleForGoal(profile.goal), style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))
                }
                Spacer(Modifier.height(10.dp))
                FlowChips(
                    listOf(
                        "Exp: ${profile.experience.name}",
                        "Equip: ${profile.equipment.name}",
                        "${profile.daysPerWeek} days/wk",
                        when (profile.sessionLength) {
                            SessionLength.Short15 -> "~15 min"
                            SessionLength.Standard30 -> "~30 min"
                            SessionLength.Long45 -> "~45+ min"
                        },
                        profile.timeOfDay.name,
                        if (profile.preferIndoor) "Indoor" else "Outdoor",
                        if (profile.hasInjury) "Injury aware" else "No injury"
                    ) + profile.preferredTypes.map { it.name }
                )
                if (profile.heightCm != null || profile.weightKg != null || profile.targetWeightKg != null) {
                    Spacer(Modifier.height(8.dp)); Divider(); Spacer(Modifier.height(8.dp))
                    Text(
                        "Body: ${profile.heightCm ?: "-"} cm / ${profile.weightKg ?: "-"} kg → target ${profile.targetWeightKg ?: "-"} kg",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(icon = Icons.Default.Today, title = "Weekly plan")
        Spacer(Modifier.height(8.dp))

        // Generate and render weekly plan (cards are clickable)
        val plan = remember(profile) { generateWeeklyPlan(profile, computeSchedule(profile)) }
        WeeklyPlanPretty(
            plan = plan,
            isDoneToday = isDoneToday, // NEW
            onDayClick = onDayClick
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle(icon = Icons.Default.CheckCircle, title = "Daily suggestions")
        Spacer(Modifier.height(8.dp))
        DailySuggestionPretty(generateDailySuggestion(profile))
    }
}

/* ---------- Pretty components ---------- */

@Composable private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colors.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))
    }
}
@Composable private fun GoalBadge(goal: Goal) {
    val color = when (goal) {
        Goal.LoseWeight -> MaterialTheme.colors.primary.copy(alpha = 0.15f)
        Goal.GainMuscle -> Color(0xFFEDC2FF).copy(alpha = 0.45f)
        Goal.Maintain -> Color(0xFFB2DFDB)
        Goal.ImproveEndurance -> Color(0xFFBBDEFB)
    }
    Box(Modifier.size(34.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Icon(
            when (goal) {
                Goal.LoseWeight -> Icons.Default.FavoriteBorder
                Goal.GainMuscle -> Icons.Default.FitnessCenter
                Goal.Maintain -> Icons.Default.CheckCircle
                Goal.ImproveEndurance -> Icons.Default.DirectionsRun
            },
            contentDescription = null
        )
    }
}
@Composable private fun FlowChips(labels: List<String>) {
    // Simple wrap without extra dependencies
    Column {
        var line = mutableListOf<String>()
        labels.forEachIndexed { i, s ->
            line += s
            val isBreak = (i == labels.lastIndex) || line.joinToString(" • ").length > 32
            if (isBreak) {
                Row { line.forEach { InfoChip(it); Spacer(Modifier.width(6.dp)) } }
                Spacer(Modifier.height(6.dp)); line = mutableListOf()
            }
        }
    }
}
@Composable private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
        contentColor = MaterialTheme.colors.onSurface,
        shape = RoundedCornerShape(50),
        elevation = 0.dp
    ) { Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.caption) }
}
@Composable private fun WeeklyPlanPretty(
    plan: ExercisePlan,
    isDoneToday: (PlanDay) -> Boolean, // NEW
    onDayClick: (PlanDay) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plan.days.forEach { day ->
            Card(
                elevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.clickable { onDayClick(day) }
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    DayBadge(day.title.take(3))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(day.title, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        day.items.forEach { Text("• $it", style = MaterialTheme.typography.body2) }
                    }
                    if (isDoneToday(day)) DoneChip() // NEW
                }
            }
        }
    }
}
@Composable private fun DayBadge(threeLetter: String) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colors.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) { Text(threeLetter.uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary) }
}
@Composable private fun DailySuggestionPretty(lines: List<String>) {
    Card(elevation = 2.dp, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.forEachIndexed { idx, line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (idx) { 0 -> Icons.Default.LocalFireDepartment; 1 -> Icons.Default.TaskAlt; else -> Icons.Default.SelfImprovement },
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(line, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

/* ----------------------------- PLAN GENERATION --------------------------- */

data class PlanDay(val title: String, val items: List<String>)
data class ExercisePlan(val days: List<PlanDay>)

/** Compute which days of week to schedule for, honoring availability and daysPerWeek (1..7). */
private fun computeSchedule(p: UserProfile): List<DayOfWeek> {
    val all = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )
    val target = p.daysPerWeek.coerceIn(1, 7)

    val base = if (p.availableDays.isNotEmpty()) {
        p.availableDays.toList().sortedBy { it.value }
    } else {
        when (target) {
            1 -> listOf(DayOfWeek.WEDNESDAY)
            2 -> listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)
            3 -> listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            4 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
            5 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
            6 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
            else -> all // 7
        }
    }
    val result = mutableListOf<DayOfWeek>()
    result += base
    if (result.size < target) {
        val rest = all.filter { it !in result }
        result += rest
    }
    return result.take(target)
}

private fun generateWeeklyPlan(p: UserProfile, schedule: List<DayOfWeek>): ExercisePlan {
    val baseCardio = when (p.sessionLength) {
        SessionLength.Short15 -> "Zone-2 walk/jog · 15 min"
        SessionLength.Standard30 -> "Zone-2 cardio · 30 min"
        SessionLength.Long45 -> "Zone-2 cardio · 45 min"
    }
    val hiit = if (WorkoutType.HIIT in p.preferredTypes)
        when (p.sessionLength) {
            SessionLength.Short15 -> "HIIT · 6×30s on/30s off"
            SessionLength.Standard30 -> "HIIT · 10×30s on/30s off"
            SessionLength.Long45 -> "HIIT · 12×40s on/20s off"
        } else null

    val strength = when (p.equipment) {
        Equipment.None -> "Bodyweight strength circuit"
        Equipment.Minimal -> "DB/Kettlebell strength circuit"
        Equipment.FullGym -> "Full-gym strength split"
    } + when (p.experience) {
        Experience.Beginner -> " · 3 sets each"
        Experience.Intermediate -> " · 4 sets each"
        Experience.Advanced -> " · 5 sets each"
    }

    val mobility = "Mobility & stretching · 10–15 min"
    val core = "Core stability · 8–12 min"

    val safeCardio = if (p.hasInjury) "Low-impact cardio (bike/elliptical) · " + when (p.sessionLength) {
        SessionLength.Short15 -> "15 min"; SessionLength.Standard30 -> "30 min"; SessionLength.Long45 -> "45 min"
    } else baseCardio

    val dayLabel = { d: DayOfWeek -> d.name.take(3) }

    val days = schedule.mapIndexed { idx, d ->
        val items = buildList {
            val strengthFirst = (p.goal == Goal.GainMuscle) || (WorkoutType.Strength in p.preferredTypes && idx % 2 == 0)
            val cardioFirst = (p.goal != Goal.GainMuscle) || (WorkoutType.Cardio in p.preferredTypes && idx % 2 == 1)
            if (strengthFirst) add(strength)
            if (cardioFirst) add(safeCardio)
            if (hiit != null && (p.goal == Goal.LoseWeight || p.goal == Goal.ImproveEndurance) && idx % 3 == 2) add(hiit)
            if (WorkoutType.Mobility in p.preferredTypes) add(mobility)
            if (WorkoutType.Core in p.preferredTypes) add(core)
        }
        PlanDay("${dayLabel(d).uppercase()} – ${labelForGoal(p.goal)} focus", items)
    }

    return ExercisePlan(days)
}

private fun generateDailySuggestion(p: UserProfile): List<String> {
    val warmup = "Warm-up · 5 min (dynamic)"
    val finisher = if (WorkoutType.Mobility in p.preferredTypes) "Cool-down · mobility 5–10 min" else "Cool-down · light stretch 5 min"
    val main = when {
        p.goal == Goal.GainMuscle && WorkoutType.Strength in p.preferredTypes -> "Main: strength compound lifts"
        p.goal == Goal.LoseWeight && WorkoutType.HIIT in p.preferredTypes -> "Main: HIIT intervals"
        WorkoutType.Cardio in p.preferredTypes -> "Main: steady cardio"
        else -> "Main: mixed circuit"
    }
    return listOf(warmup, main, finisher)
}

private fun labelForGoal(g: Goal) = when (g) {
    Goal.LoseWeight -> "fat-loss"
    Goal.GainMuscle -> "muscle"
    Goal.Maintain -> "balanced"
    Goal.ImproveEndurance -> "endurance"
}

/* -------------------------- UI HELPERS / SAVERS -------------------------- */

@Composable private fun StepCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))
    Spacer(Modifier.height(8.dp))
    Card(elevation = 2.dp, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp), content = content) }
}
@Composable private fun ChoiceRadio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick); Spacer(Modifier.width(8.dp)); Text(label)
    }
}
@Composable private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.15f) else MaterialTheme.colors.onSurface.copy(alpha = 0.06f),
        contentColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
        shape = RoundedCornerShape(50), elevation = 0.dp,
        modifier = Modifier.clickable { onClick() }
    ) { Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.caption) }
}
@Composable private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column { Text(label, style = MaterialTheme.typography.caption); OutlinedTextField(value, onValueChange = onChange, singleLine = true, modifier = Modifier.fillMaxWidth()) }
}

/** Saver for UserProfile (receiver is SaverScope, matches Compose API). */
private val ProfileSaver = listSaver<UserProfile, String>(
    save = { p ->
        listOf(
            p.goal.name, p.experience.name, p.equipment.name, p.daysPerWeek.toString(), p.sessionLength.name,
            p.preferIndoor.toString(), p.hasInjury.toString(),
            p.preferredTypes.joinToString(",") { it.name }, p.timeOfDay.name,
            p.heightCm?.toString() ?: "", p.weightKg?.toString() ?: "", p.targetWeightKg?.toString() ?: "",
            p.availableDays.joinToString(",") { it.name }
        )
    },
    restore = { l ->
        UserProfile(
            goal = runCatching { Goal.valueOf(l[0]) }.getOrDefault(Goal.LoseWeight),
            experience = runCatching { Experience.valueOf(l[1]) }.getOrDefault(Experience.Beginner),
            equipment = runCatching { Equipment.valueOf(l[2]) }.getOrDefault(Equipment.None),
            daysPerWeek = l[3].toIntOrNull() ?: 3,
            sessionLength = runCatching { SessionLength.valueOf(l[4]) }.getOrDefault(SessionLength.Standard30),
            preferIndoor = l[5].toBooleanStrictOrNull() ?: true,
            hasInjury = l[6].toBooleanStrictOrNull() ?: false,
            preferredTypes = l[7].split(",").filter { it.isNotBlank() }.mapNotNull { runCatching { WorkoutType.valueOf(it) }.getOrNull() }.toSet()
                .ifEmpty { setOf(WorkoutType.Cardio) },
            timeOfDay = runCatching { TimeOfDay.valueOf(l[8]) }.getOrDefault(TimeOfDay.Morning),
            heightCm = l[9].toIntOrNull(),
            weightKg = l[10].toFloatOrNull(),
            targetWeightKg = l[11].toFloatOrNull(),
            availableDays = l.getOrNull(12)?.split(",")?.filter { it.isNotBlank() }?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }?.toSet()
                ?: emptySet()
        )
    }
)

/* --------- Titles --------- */
private fun titleForGoal(goal: Goal) = when (goal) {
    Goal.LoseWeight -> "Fat-loss program"
    Goal.GainMuscle -> "Muscle program"
    Goal.Maintain -> "Balanced program"
    Goal.ImproveEndurance -> "Endurance program"
}

/* --------------------------- BottomSheet content ------------------------- */

@Composable
private fun DayPlanSheet(
    day: PlanDay?,
    isDoneToday: Boolean,
    onStart: (PlanDay) -> Unit,
    onQuickDone: (PlanDay) -> Unit
) {
    if (day == null) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No day selected")
        }
        return
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Today, contentDescription = null, tint = MaterialTheme.colors.primary)
            Spacer(Modifier.width(8.dp))
            Text(day.title, style = MaterialTheme.typography.h6)
        }

        // Today's Action List
        Card(elevation = 1.dp, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                day.items.forEach { Text("• $it", style = MaterialTheme.typography.body2) }
            }
        }

        // -- These are the two steps: When today's tasks are completed, provide a prompt and change the button text to "Redo guided" -- //
        if (isDoneToday) {
            Text(
                "Completed today — tap Redo to start a new run.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Main button: If today's tasks have been completed, the text will display "Redo guided"; the logic remains as onStart(day)
            Button(modifier = Modifier.weight(1f), onClick = { onStart(day) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isDoneToday) "Redo guided" else "Start guided")
            }

            // Quick marking completion: If completed today, disable and change the text description
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onQuickDone(day) },
                enabled = !isDoneToday
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isDoneToday) "Already done today" else "Mark done quickly")
            }
        }
    }
}


/* ------------------------------ Small UI bits ---------------------------- */

@Composable
private fun DoneChip() {
    Surface(
        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
        contentColor = Color(0xFF2E7D32),
        shape = RoundedCornerShape(50)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Done", style = MaterialTheme.typography.caption)
        }
    }
}

