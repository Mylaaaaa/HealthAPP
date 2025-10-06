package com.example.myhealth.presentation.screen.exercisesession

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import java.time.DayOfWeek

/* ---------------------------------------------------------------------------
   Planner screen
   - First time: multi-step wizard (many questions)
   - After saved: plan overview (daily + weekly) with "Re-customize" button
   - Comments are in English for submission
   --------------------------------------------------------------------------- */

@Composable
fun ExercisePlanScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember(context) { ExercisePrefs(context) }

    // Load persisted profile (or a default)
    val persisted = prefs.loadProfileOrNull()
    var profile by rememberSaveable(stateSaver = ProfileSaver) {
        mutableStateOf(persisted ?: UserProfile())
    }
    // Separate boolean flag to control which UI to show
    var isSaved by rememberSaveable { mutableStateOf(persisted != null) }

    if (isSaved) {
        PlanOverview(
            profile = profile,
            onReCustomise = {
                // Go back to the wizard but keep current answers as draft
                isSaved = false
            }
        )
    } else {
        PlannerWizard(
            initial = profile,
            onSave = { newProfile ->
                prefs.saveProfile(newProfile)
                profile = newProfile
                isSaved = true
            }
        )
    }
}

/* ------------------------------ DATA MODEL ------------------------------- */

enum class Goal { LoseWeight, GainMuscle, Maintain, ImproveEndurance }
enum class Experience { Beginner, Intermediate, Advanced }
enum class Equipment { None, Minimal, FullGym }
enum class SessionLength { Short15, Standard30, Long45 }
enum class TimeOfDay { Morning, Afternoon, Evening }
enum class WorkoutType { Cardio, Strength, Mobility, HIIT, Core }

/** User settings captured by the wizard. */
data class UserProfile(
    val goal: Goal = Goal.LoseWeight,
    val experience: Experience = Experience.Beginner,
    val equipment: Equipment = Equipment.None,
    val daysPerWeek: Int = 3,
    val sessionLength: SessionLength = SessionLength.Standard30,
    val preferIndoor: Boolean = true,
    val hasInjury: Boolean = false,
    val preferredTypes: Set<WorkoutType> = setOf(WorkoutType.Cardio, WorkoutType.Core),
    val timeOfDay: TimeOfDay = TimeOfDay.Morning,
    val heightCm: Int? = null,
    val weightKg: Float? = null,
    val targetWeightKg: Float? = null,
    val availableDays: Set<DayOfWeek> = emptySet() // if empty, we auto-generate days
)

/* ------------------------------ PREFERENCES ------------------------------ */

private class ExercisePrefs(private val context: Context) {
    private val sp get() = context.getSharedPreferences("exercise_planner", Context.MODE_PRIVATE)

    fun saveProfile(p: UserProfile) {
        sp.edit {
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
                .toSet()
                .ifEmpty { setOf(WorkoutType.Cardio) },
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

    fun clear() { sp.edit { clear() } }
}

/* ------------------------------ WIZARD UI -------------------------------- */

@Composable
private fun PlannerWizard(
    initial: UserProfile,
    onSave: (UserProfile) -> Unit
) {
    var step by rememberSaveable { mutableStateOf(0) }
    var draft by rememberSaveable(stateSaver = ProfileSaver) { mutableStateOf(initial) }

    // Steps: 0..10
    val totalSteps = 11

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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
            8 -> BodyMetricsStep(
                height = draft.heightCm,
                weight = draft.weightKg,
                target = draft.targetWeightKg
            ) { h, w, t -> draft = draft.copy(heightCm = h, weightKg = w, targetWeightKg = t) }
            9 -> AvailabilityStep(
                selected = draft.availableDays
            ) { days -> draft = draft.copy(availableDays = days) }
            10 -> SummaryStep(draft)
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                enabled = step > 0,
                onClick = { step -= 1 }
            ) { Text("Back") }

            if (step < totalSteps - 1) {
                Button(onClick = { step += 1 }) { Text("Next") }
            } else {
                Button(onClick = { onSave(draft) }) { Text("Save my plan") }
            }
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

@Composable
private fun GoalStep(selected: Goal, onSelect: (Goal) -> Unit) {
    StepCard(title = "Your primary goal") {
        ChoiceRadio("Lose weight", selected == Goal.LoseWeight) { onSelect(Goal.LoseWeight) }
        ChoiceRadio("Gain muscle", selected == Goal.GainMuscle) { onSelect(Goal.GainMuscle) }
        ChoiceRadio("Maintain", selected == Goal.Maintain) { onSelect(Goal.Maintain) }
        ChoiceRadio("Improve endurance", selected == Goal.ImproveEndurance) { onSelect(Goal.ImproveEndurance) }
    }
}

@Composable
private fun ExperienceStep(selected: Experience, onSelect: (Experience) -> Unit) {
    StepCard(title = "Training experience") {
        ChoiceRadio("Beginner", selected == Experience.Beginner) { onSelect(Experience.Beginner) }
        ChoiceRadio("Intermediate", selected == Experience.Intermediate) { onSelect(Experience.Intermediate) }
        ChoiceRadio("Advanced", selected == Experience.Advanced) { onSelect(Experience.Advanced) }
    }
}

@Composable
private fun EquipmentStep(selected: Equipment, onSelect: (Equipment) -> Unit) {
    StepCard(title = "Equipment availability") {
        ChoiceRadio("No equipment", selected == Equipment.None) { onSelect(Equipment.None) }
        ChoiceRadio("Minimal (bands/dumbbells)", selected == Equipment.Minimal) { onSelect(Equipment.Minimal) }
        ChoiceRadio("Full gym", selected == Equipment.FullGym) { onSelect(Equipment.FullGym) }
    }
}

@Composable
private fun DaysPerWeekStep(value: Int, onChange: (Int) -> Unit) {
    StepCard(title = "How many days per week?") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            (2..6).forEach { d ->
                ChoiceChip(text = "$d", selected = value == d) { onChange(d) }
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun SessionLengthStep(selected: SessionLength, onSelect: (SessionLength) -> Unit) {
    StepCard(title = "Typical session length") {
        ChoiceChip("~15 min", selected == SessionLength.Short15) { onSelect(SessionLength.Short15) }
        Spacer(Modifier.width(8.dp))
        ChoiceChip("~30 min", selected == SessionLength.Standard30) { onSelect(SessionLength.Standard30) }
        Spacer(Modifier.width(8.dp))
        ChoiceChip("~45+ min", selected == SessionLength.Long45) { onSelect(SessionLength.Long45) }
    }
}

@Composable
private fun PreferredTypesStep(selected: Set<WorkoutType>, onChange: (Set<WorkoutType>) -> Unit) {
    StepCard(title = "Preferred workout types") {
        val all = listOf(WorkoutType.Cardio, WorkoutType.Strength, WorkoutType.Mobility, WorkoutType.HIIT, WorkoutType.Core)
        Row(Modifier.fillMaxWidth()) {
            all.forEach { t ->
                val s = selected.contains(t)
                ChoiceChip(text = t.name, selected = s) {
                    onChange(if (s) selected - t else selected + t)
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Tip: choose 2–3 types you enjoy the most.", style = MaterialTheme.typography.body2)
    }
}

@Composable
private fun EnvironmentStep(indoor: Boolean, injury: Boolean, onChange: (Boolean, Boolean) -> Unit) {
    StepCard(title = "Environment & limitations") {
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

@Composable
private fun TimeOfDayStep(selected: TimeOfDay, onSelect: (TimeOfDay) -> Unit) {
    StepCard(title = "Preferred time of day") {
        Row {
            ChoiceChip("Morning", selected == TimeOfDay.Morning) { onSelect(TimeOfDay.Morning) }
            Spacer(Modifier.width(8.dp))
            ChoiceChip("Afternoon", selected == TimeOfDay.Afternoon) { onSelect(TimeOfDay.Afternoon) }
            Spacer(Modifier.width(8.dp))
            ChoiceChip("Evening", selected == TimeOfDay.Evening) { onSelect(TimeOfDay.Evening) }
        }
    }
}

@Composable
private fun BodyMetricsStep(
    height: Int?,
    weight: Float?,
    target: Float?,
    onChange: (Int?, Float?, Float?) -> Unit
) {
    var h by rememberSaveable { mutableStateOf(height?.toString() ?: "") }
    var w by rememberSaveable { mutableStateOf(weight?.toString() ?: "") }
    var t by rememberSaveable { mutableStateOf(target?.toString() ?: "") }

    StepCard(title = "Body metrics (optional)") {
        LabeledField("Height (cm)", h) { h = it }
        Spacer(Modifier.height(8.dp))
        LabeledField("Weight (kg)", w) { w = it }
        Spacer(Modifier.height(8.dp))
        LabeledField("Target weight (kg)", t) { t = it }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            onChange(h.toIntOrNull(), w.toFloatOrNull(), t.toFloatOrNull())
        }) { Text("Apply") }
    }
}

@Composable
private fun AvailabilityStep(
    selected: Set<DayOfWeek>,
    onChange: (Set<DayOfWeek>) -> Unit
) {
    StepCard(title = "Which days are you available?") {
        val all = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )
        Column {
            all.chunked(4).forEach { row ->
                Row {
                    row.forEach { d ->
                        val s = d in selected
                        ChoiceChip(d.name.take(3), s) {
                            onChange(if (s) selected - d else selected + d)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Text("If none selected, days will be auto-distributed by frequency.", style = MaterialTheme.typography.body2)
    }
}

@Composable
private fun SummaryStep(draft: UserProfile) {
    StepCard(title = "Summary") {
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

/* ----------------------------- SAVED VIEW UI ----------------------------- */

@Composable
private fun PlanOverview(profile: UserProfile, onReCustomise: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your plan", style = MaterialTheme.typography.h6)
            TextButton(onClick = onReCustomise) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Re-customize")
            }
        }

        Spacer(Modifier.height(8.dp))
        ProfileSummary(profile)

        Spacer(Modifier.height(12.dp))
        Text("Weekly plan", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))
        WeeklyPlanCards(generateWeeklyPlan(profile))

        Spacer(Modifier.height(12.dp))
        Text("Daily suggestions", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))
        DaySuggestionCard(generateDailySuggestion(profile))
    }
}

@Composable
private fun ProfileSummary(p: UserProfile) {
    Card(elevation = 2.dp) {
        Column(Modifier.padding(14.dp)) {
            Text("Goal: ${p.goal.name}")
            Text("Experience: ${p.experience.name}")
            Text("Equipment: ${p.equipment.name}")
            Text("Days/week: ${p.daysPerWeek}")
            Text("Session length: ${p.sessionLength.name}")
            Text("Preferred: ${p.preferredTypes.joinToString()}")
            Text("Environment: ${if (p.preferIndoor) "Indoor" else "Outdoor"}")
            Text("Injury: ${if (p.hasInjury) "Yes" else "No"}")
            Text("Time of day: ${p.timeOfDay.name}")
            if (p.heightCm != null || p.weightKg != null || p.targetWeightKg != null) {
                Text("Body: ${p.heightCm ?: "-"} cm / ${p.weightKg ?: "-"} kg → target ${p.targetWeightKg ?: "-"} kg")
            }
            val avail = p.availableDays.joinToString { it.name.take(3) }
            Text("Available days: ${if (avail.isBlank()) "Auto" else avail}")
        }
    }
}

/* ----------------------------- PLAN GENERATION --------------------------- */

data class PlanDay(val title: String, val items: List<String>)
data class ExercisePlan(val days: List<PlanDay>)

@Composable
private fun WeeklyPlanCards(plan: ExercisePlan) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plan.days.forEach { day ->
            Card(elevation = 2.dp) {
                Column(Modifier.padding(14.dp)) {
                    Text(day.title, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    day.items.forEach { Text("• $it", style = MaterialTheme.typography.body2) }
                }
            }
        }
    }
}

@Composable
private fun DaySuggestionCard(lines: List<String>) {
    Card(elevation = 2.dp) {
        Column(Modifier.padding(14.dp)) {
            lines.forEach { Text("• $it", style = MaterialTheme.typography.body2) }
        }
    }
}

private fun generateWeeklyPlan(p: UserProfile): ExercisePlan {
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

    // Injury-safe cardio override
    val safeCardio = if (p.hasInjury) "Low-impact cardio (bike/elliptical) · ${
        when (p.sessionLength) {
            SessionLength.Short15 -> "15 min"
            SessionLength.Standard30 -> "30 min"
            SessionLength.Long45 -> "45 min"
        }
    }" else baseCardio

    // Weekday schedule
    val schedule: List<DayOfWeek> = if (p.availableDays.isNotEmpty()) {
        p.availableDays.toList().sortedBy { it.value }
    } else {
        when (p.daysPerWeek) {
            2 -> listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)
            3 -> listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            4 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
            5 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
            6 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
            else -> listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        }
    }.take(p.daysPerWeek)

    val dayLabel = { d: DayOfWeek -> d.name.take(3).replaceFirstChar { it.uppercase() } }

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
        PlanDay("${dayLabel(d)} – ${labelForGoal(p.goal)} focus", items)
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

@Composable
private fun StepCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))
    Spacer(Modifier.height(8.dp))
    Card(elevation = 2.dp) { Column(Modifier.padding(14.dp), content = content) }
}

@Composable
private fun ChoiceRadio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.15f) else MaterialTheme.colors.surface,
        contentColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
        shape = MaterialTheme.shapes.small,
        elevation = if (selected) 4.dp else 1.dp,
        modifier = Modifier.clickable { onClick() }
    ) { Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.caption)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Saver for UserProfile (receiver is SaverScope, matches Compose API). */
private val ProfileSaver = listSaver<UserProfile, String>(
    save = { p ->
        listOf(
            p.goal.name,
            p.experience.name,
            p.equipment.name,
            p.daysPerWeek.toString(),
            p.sessionLength.name,
            p.preferIndoor.toString(),
            p.hasInjury.toString(),
            p.preferredTypes.joinToString(",") { it.name },
            p.timeOfDay.name,
            p.heightCm?.toString() ?: "",
            p.weightKg?.toString() ?: "",
            p.targetWeightKg?.toString() ?: "",
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
            preferredTypes = l[7].split(",").filter { it.isNotBlank() }
                .mapNotNull { runCatching { WorkoutType.valueOf(it) }.getOrNull() }.toSet()
                .ifEmpty { setOf(WorkoutType.Cardio) },
            timeOfDay = runCatching { TimeOfDay.valueOf(l[8]) }.getOrDefault(TimeOfDay.Morning),
            heightCm = l[9].toIntOrNull(),
            weightKg = l[10].toFloatOrNull(),
            targetWeightKg = l[11].toFloatOrNull(),
            availableDays = l.getOrNull(12)?.split(",")?.filter { it.isNotBlank() }?.mapNotNull {
                runCatching { DayOfWeek.valueOf(it) }.getOrNull()
            }?.toSet() ?: emptySet()
        )
    }
)
