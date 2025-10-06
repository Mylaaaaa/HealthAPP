package com.example.myhealth.presentation.screen.exercisesession

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Personalized plan page. Stores a lightweight profile locally and generates a weekly plan.
 * No network; safe to keep entirely in Compose with SharedPreferences.
 */
@Composable
fun ExercisePlanScreen(
    modifier: Modifier = Modifier,
    onAdjustPlan: () -> Unit = {}
) {
    val prefs = rememberExercisePrefs()
    var hasProfile by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableStateOf(0) }
    var profile by rememberSaveable(stateSaver = UserProfileSaver) { mutableStateOf(UserProfile()) }

    // Load saved profile (if any)
    LaunchedEffect(Unit) {
        prefs.loadProfileOrNull()?.let {
            profile = it
            hasProfile = true
        }
    }

    val plan by remember(profile, hasProfile) {
        mutableStateOf(if (hasProfile) generatePlan(profile) else null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (!hasProfile) {
            // Minimal 3-step setup for better UX on first visit
            SimpleWizard(
                step = step,
                profile = profile,
                onProfileChange = { profile = it },
                onPrev = { step = (step - 1).coerceAtLeast(0) },
                onNext = { step += 1 },
                onFinish = {
                    hasProfile = true
                    prefs.saveProfile(profile)
                    onAdjustPlan()
                }
            )
        } else {
            // Plan card
            Card(
                elevation = 4.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your weekly plan", style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(8.dp))
                    Text("Goal: ${profile.goal.name}")
                    Spacer(Modifier.height(6.dp))
                    Text("Schedule: ${profile.daysPerWeek} days/week · ${profile.minutesPerSession} min/session")
                    Spacer(Modifier.height(6.dp))
                    Text("Suggested HR: ${plan!!.suggestedHRZone}")
                    Spacer(Modifier.height(6.dp))
                    Text("Estimated weekly calorie burn: ${plan!!.weeklyKcalTarget} kcal")
                    Spacer(Modifier.height(10.dp))
                    Text("Split:", fontWeight = FontWeight.SemiBold)
                    plan!!.split.forEach { Text("• $it") }
                    if (plan!!.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Notes:", fontWeight = FontWeight.SemiBold)
                        Text(plan!!.notes)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                onClick = {
                    // Reset profile to re-run the wizard
                    prefs.clearProfile()
                    hasProfile = false
                    step = 0
                }
            ) { Text("Adjust plan") }
        }
    }
}

/* ---------- Minimal wizard (3 steps) ---------- */

@Composable
private fun SimpleWizard(
    step: Int,
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    when (step) {
        0 -> StepGoal(profile, onProfileChange) { onNext() }
        1 -> StepBasic(profile, onProfileChange, onPrev) { onNext() }
        2 -> StepSchedule(profile, onProfileChange, onPrev) { onFinish() }
    }
}

@Composable private fun StepGoal(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onNext: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), elevation = 4.dp) {
        Column(Modifier.padding(16.dp)) {
            Text("Your goal", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalChip("Lose weight", profile.goal == Goal.LoseWeight) { onProfileChange(profile.copy(goal = Goal.LoseWeight)) }
                GoalChip("Gain muscle", profile.goal == Goal.GainMuscle) { onProfileChange(profile.copy(goal = Goal.GainMuscle)) }
                GoalChip("Maintain", profile.goal == Goal.Maintain) { onProfileChange(profile.copy(goal = Goal.Maintain)) }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
        }
    }
}

@Composable private fun StepBasic(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    var height by rememberSaveable { mutableStateOf(profile.heightCm.toString()) }
    var weight by rememberSaveable { mutableStateOf(profile.weightKg.toString()) }
    var target by rememberSaveable { mutableStateOf(profile.targetKg.toString()) }

    Card(Modifier.fillMaxWidth(), elevation = 4.dp) {
        Column(Modifier.padding(16.dp)) {
            Text("Basic metrics", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(height, { height = it.filter(Char::isDigit) }, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(weight, { weight = it.filter(Char::isDigit) }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(target, { target = it.filter(Char::isDigit) }, label = { Text("Target (kg)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = {
                        onProfileChange(
                            profile.copy(
                                heightCm = height.toIntOrNull() ?: profile.heightCm,
                                weightKg = weight.toIntOrNull() ?: profile.weightKg,
                                targetKg = target.toIntOrNull() ?: profile.targetKg
                            )
                        )
                        onNext()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Next") }
            }
        }
    }
}

@Composable private fun StepSchedule(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onPrev: () -> Unit,
    onFinish: () -> Unit
) {
    var days by rememberSaveable { mutableStateOf(profile.daysPerWeek.toFloat()) }
    var minutes by rememberSaveable { mutableStateOf(profile.minutesPerSession.toFloat()) }

    Card(Modifier.fillMaxWidth(), elevation = 4.dp) {
        Column(Modifier.padding(16.dp)) {
            Text("Schedule", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(8.dp))
            Text("Days per week: ${days.toInt()}")
            Slider(value = days, onValueChange = { days = it }, valueRange = 1f..7f, steps = 5)
            Spacer(Modifier.height(8.dp))
            Text("Minutes per session: ${minutes.toInt()}")
            Slider(value = minutes, onValueChange = { minutes = it }, valueRange = 20f..90f, steps = 6)

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = {
                        onProfileChange(profile.copy(
                            daysPerWeek = days.toInt(),
                            minutesPerSession = minutes.toInt()
                        ))
                        onFinish()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Finish") }
            }
        }
    }
}

/* ---------- Tiny persistence + plan generator (shared with this page only) ---------- */

private enum class Goal { LoseWeight, GainMuscle, Maintain }

private data class UserProfile(
    val goal: Goal = Goal.LoseWeight,
    val heightCm: Int = 170,
    val weightKg: Int = 60,
    val targetKg: Int = 55,
    val daysPerWeek: Int = 3,
    val minutesPerSession: Int = 40
)

private val UserProfileSaver = listSaver<UserProfile, Any?>(
    save = {
        listOf(it.goal.name, it.heightCm, it.weightKg, it.targetKg, it.daysPerWeek, it.minutesPerSession)
    },
    restore = { l ->
        UserProfile(
            goal = Goal.valueOf(l[0] as String),
            heightCm = l[1] as Int,
            weightKg = l[2] as Int,
            targetKg = l[3] as Int,
            daysPerWeek = l[4] as Int,
            minutesPerSession = l[5] as Int
        )
    }
)

@Composable private fun rememberExercisePrefs(): ExercisePrefs {
    val ctx = LocalContext.current
    return remember(ctx) { ExercisePrefs(ctx) }
}

private class ExercisePrefs(private val context: Context) {
    private val sp get() = context.getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE)

    fun saveProfile(profile: UserProfile) {
        sp.edit()
            .putBoolean("has_profile", true)
            .putString("profile", encode(profile))
            .apply()
    }

    fun loadProfileOrNull(): UserProfile? {
        if (!sp.getBoolean("has_profile", false)) return null
        return sp.getString("profile", null)?.let { runCatching { decode(it) }.getOrNull() }
    }

    fun clearProfile() {
        sp.edit().clear().apply()
    }
}

private fun encode(p: UserProfile): String =
    listOf(p.goal.name, p.heightCm, p.weightKg, p.targetKg, p.daysPerWeek, p.minutesPerSession).joinToString("|")

private fun decode(s: String): UserProfile {
    val a = s.split("|")
    return UserProfile(
        goal = Goal.valueOf(a[0]),
        heightCm = a[1].toInt(),
        weightKg = a[2].toInt(),
        targetKg = a[3].toInt(),
        daysPerWeek = a[4].toInt(),
        minutesPerSession = a[5].toInt()
    )
}

private data class ExercisePlan(
    val weeklyKcalTarget: Int,
    val suggestedHRZone: String,
    val split: List<String>,
    val notes: String
)

private fun generatePlan(p: UserProfile): ExercisePlan {
    val delta = (p.weightKg - p.targetKg)
    val base = when (p.goal) {
        Goal.LoseWeight -> 1500
        Goal.GainMuscle -> 900
        Goal.Maintain -> 700
    }
    val scheduleBias = (p.daysPerWeek * p.minutesPerSession / 10)
    val weekly = (base + scheduleBias + (delta * 15)).coerceAtLeast(400)

    val hr = when (p.goal) {
        Goal.LoseWeight -> "Zone 2 (RPE 4–5) + HIIT once"
        Goal.GainMuscle -> "Strength focus + short cardio"
        Goal.Maintain -> "Balanced cardio/strength"
    }
    val split = when (p.goal) {
        Goal.LoseWeight -> listOf("HIIT ×1", "Zone2 ×${(p.daysPerWeek - 1).coerceAtLeast(1)}", "Full-body circuit ×1")
        Goal.GainMuscle -> listOf("Full-body ×3", "Zone2 ×1")
        Goal.Maintain -> listOf("Mixed cardio ×2", "Compounds ×2", "Mobility ×1")
    }
    val notes = "Each session ~${p.minutesPerSession} min. Rest 1 day between strength days."

    return ExercisePlan(weeklyKcalTarget = weekly, suggestedHRZone = hr, split = split.take(p.daysPerWeek), notes = notes)
}

/** Small reusable chip used in the wizard (copied local to keep deps minimal) */
@Composable private fun GoalChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.surface,
        shape = MaterialTheme.shapes.small,
        border = ButtonDefaults.outlinedBorder,
        modifier = Modifier.height(36.dp).padding(end = 4.dp)
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) { Text(if (selected) "✓ $text" else text) }
    }
}
