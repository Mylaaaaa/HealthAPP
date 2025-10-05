package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import com.example.myhealth.data.ExerciseSession
import com.example.myhealth.presentation.component.ExerciseSessionRow
import java.util.UUID
import kotlin.math.max


@Composable
fun ExerciseSessionScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    sessionsList: List<ExerciseSession>,
    uiState: ExerciseSessionViewModel.UiState,
    onInsertClick: () -> Unit = {},
    onDetailsClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {},
    onBackClick: () -> Unit = {}
) {

    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }


    LaunchedEffect(uiState) {
        if (uiState is ExerciseSessionViewModel.UiState.Uninitialized) onPermissionsResult()
        if (uiState is ExerciseSessionViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception); errorId.value = uiState.uuid
        }
    }
    if (uiState == ExerciseSessionViewModel.UiState.Uninitialized) return


    val prefs = rememberExercisePrefs()


    var hasProfile by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableStateOf(0) }


    var profile by rememberSaveable(stateSaver = UserProfileSaver) { mutableStateOf(UserProfile()) }

    // First attempt to read the saved configuration
    LaunchedEffect(Unit) {
        prefs.loadProfileOrNull()?.let {
            profile = it
            hasProfile = true
        }
    }

    // plan
    val plan by remember(profile, hasProfile) {
        mutableStateOf(if (hasProfile) generatePlan(profile) else null)
    }

    // Permission barrier
    if (!permissionsGranted) {
        PermissionGate(permissions = permissions, onPermissionsLaunch = onPermissionsLaunch)
        return
    }

    // Keep only the content area (the outer layer already has a top bar), avoiding double top bars.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (!hasProfile) {
            OnboardingWizard(
                step = step,
                profile = profile,
                onProfileChange = { profile = it },
                onPrev = { step = max(0, step - 1) },
                onNext = { step += 1 },
                onFinish = {
                    if (profile.heightCm > 0 && profile.weightKg > 0) {
                        hasProfile = true
                        prefs.saveProfile(profile)  // 完成时保存
                    }
                }
            )
        } else {
            PlanAndLogScreen(
                plan = plan!!,
                profile = profile,
                sessionsList = sessionsList,
                backgroundReadAvailable = backgroundReadAvailable,
                backgroundReadGranted = backgroundReadGranted,
                onRequestBgRead = {
                    onPermissionsLaunch(setOf(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND))
                },
                onInsertClick = onInsertClick,
                onDetailsClick = onDetailsClick,
                onDeleteClick = onDeleteClick,
                onAdjustPlan = {
                    // Clear the archives and return to the guide.
                    prefs.clearProfile()
                    hasProfile = false
                    step = 0
                }
            )
        }
    }
}

/* =================================================================================
 * Models & Saver
 * ================================================================================= */
private enum class Goal { LoseWeight, GainWeight, GainMuscle, Maintain }
private enum class Level { Beginner, Intermediate, Advanced }
private enum class Equipment { None, Bands, Dumbbells, Gym }

private data class UserProfile(
    val goal: Goal = Goal.LoseWeight,
    val heightCm: Int = 170,
    val weightKg: Int = 60,
    val targetKg: Int = 55,
    val level: Level = Level.Beginner,
    val equipment: Equipment = Equipment.None,
    val daysPerWeek: Int = 3,
    val minutesPerSession: Int = 40,
    val injuriesOrLimits: String = "",
    val likes: String = "",
    val dislikes: String = ""
)


private val UserProfileSaver = listSaver<UserProfile, Any?>(
    save = {
        listOf(
            it.goal.name,
            it.heightCm,
            it.weightKg,
            it.targetKg,
            it.level.name,
            it.equipment.name,
            it.daysPerWeek,
            it.minutesPerSession,
            it.injuriesOrLimits,
            it.likes,
            it.dislikes
        )
    },
    restore = { list ->
        UserProfile(
            goal = Goal.valueOf(list[0] as String),
            heightCm = list[1] as Int,
            weightKg = list[2] as Int,
            targetKg = list[3] as Int,
            level = Level.valueOf(list[4] as String),
            equipment = Equipment.valueOf(list[5] as String),
            daysPerWeek = list[6] as Int,
            minutesPerSession = list[7] as Int,
            injuriesOrLimits = list[8] as String,
            likes = list[9] as String,
            dislikes = list[10] as String
        )
    }
)


@Composable
private fun rememberExercisePrefs(): ExercisePrefs {
    val ctx = LocalContext.current
    return remember(ctx) { ExercisePrefs(ctx) }
}

private class ExercisePrefs(private val context: android.content.Context) {
    private val sp get() = context.getSharedPreferences("exercise_prefs", android.content.Context.MODE_PRIVATE)

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

private fun encode(p: UserProfile): String = listOf(
    p.goal.name, p.heightCm, p.weightKg, p.targetKg, p.level.name, p.equipment.name,
    p.daysPerWeek, p.minutesPerSession, p.injuriesOrLimits, p.likes, p.dislikes
).joinToString("|")

private fun decode(s: String): UserProfile {
    val a = s.split("|")
    return UserProfile(
        goal = Goal.valueOf(a[0]),
        heightCm = a[1].toInt(),
        weightKg = a[2].toInt(),
        targetKg = a[3].toInt(),
        level = Level.valueOf(a[4]),
        equipment = Equipment.valueOf(a[5]),
        daysPerWeek = a[6].toInt(),
        minutesPerSession = a[7].toInt(),
        injuriesOrLimits = a.getOrNull(8) ?: "",
        likes = a.getOrNull(9) ?: "",
        dislikes = a.getOrNull(10) ?: ""
    )
}

/* =================================================================================
 * Permission Gate
 * ================================================================================= */
@Composable
private fun PermissionGate(
    permissions: Set<String>,
    onPermissionsLaunch: (Set<String>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Exercise requires permissions", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onPermissionsLaunch(permissions) }) {
            Text("Grant permissions")
        }
    }
}

/* =================================================================================
 * Onboarding Wizard（6步）
 * ================================================================================= */
@Composable
private fun OnboardingWizard(
    step: Int,
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            StepIndicator(current = step, total = 6)
            Spacer(Modifier.height(8.dp))
        }

        when (step) {
            0 -> item { StepGoal(profile, onProfileChange, onNext) }
            1 -> item { StepBasic(profile, onProfileChange, onPrev, onNext) }
            2 -> item { StepTarget(profile, onProfileChange, onPrev, onNext) }
            3 -> item { StepLevelEquipment(profile, onProfileChange, onPrev, onNext) }
            4 -> item { StepFrequency(profile, onProfileChange, onPrev, onNext) }
            5 -> item { StepLimits(profile, onProfileChange, onPrev, onFinish) }
        }
    }
}

@Composable private fun StepIndicator(current: Int, total: Int) {
    Text("Step ${current + 1} of ${total + 1}", style = MaterialTheme.typography.subtitle1)
}

/* --- Step 0: Goal --- */
@Composable private fun StepGoal(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onNext: () -> Unit
) {
    CardBlock(title = "Your goal") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoalChip("Lose weight", profile.goal == Goal.LoseWeight) { onProfileChange(profile.copy(goal = Goal.LoseWeight)) }
            GoalChip("Gain weight", profile.goal == Goal.GainWeight) { onProfileChange(profile.copy(goal = Goal.GainWeight)) }
            GoalChip("Gain muscle", profile.goal == Goal.GainMuscle) { onProfileChange(profile.copy(goal = Goal.GainMuscle)) }
            GoalChip("Maintain", profile.goal == Goal.Maintain) { onProfileChange(profile.copy(goal = Goal.Maintain)) }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
    }
}

/* --- Step 1: Height / Weight --- */
@Composable private fun StepBasic(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    var h by rememberSaveable { mutableStateOf(profile.heightCm.toString()) }
    var w by rememberSaveable { mutableStateOf(profile.weightKg.toString()) }

    CardBlock(title = "Basic metrics") {
        OutlinedTextField(
            value = h, onValueChange = { v -> h = v.filter { it.isDigit() } },
            label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = w, onValueChange = { v -> w = v.filter { it.isDigit() } },
            label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = {
                    val hc = h.toIntOrNull() ?: profile.heightCm
                    val wc = w.toIntOrNull() ?: profile.weightKg
                    onProfileChange(profile.copy(heightCm = hc, weightKg = wc))
                    onNext()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Next") }
        }
    }
}

/* --- Step 2: Target weight --- */
@Composable private fun StepTarget(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    var t by rememberSaveable { mutableStateOf(profile.targetKg.toString()) }

    CardBlock(title = "Target weight") {
        OutlinedTextField(
            value = t, onValueChange = { v -> t = v.filter { it.isDigit() } },
            label = { Text("Target (kg)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = {
                    val tg = t.toIntOrNull() ?: profile.targetKg
                    onProfileChange(profile.copy(targetKg = tg))
                    onNext()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Next") }
        }
    }
}

/* --- Step 3: Level & Equipment --- */
@Composable private fun StepLevelEquipment(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    CardBlock(title = "Experience & equipment") {
        Text("Level")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectChip("Beginner", profile.level == Level.Beginner) { onProfileChange(profile.copy(level = Level.Beginner)) }
            SelectChip("Intermediate", profile.level == Level.Intermediate) { onProfileChange(profile.copy(level = Level.Intermediate)) }
            SelectChip("Advanced", profile.level == Level.Advanced) { onProfileChange(profile.copy(level = Level.Advanced)) }
        }
        Spacer(Modifier.height(12.dp))
        Text("Equipment")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Equipment.None, Equipment.Bands, Equipment.Dumbbells, Equipment.Gym).forEach { eq ->
                SelectChip(eq.name, profile.equipment == eq) {
                    onProfileChange(profile.copy(equipment = eq))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Next") }
        }
    }
}

/* --- Step 4: Frequency & duration --- */
@Composable private fun StepFrequency(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    var d by rememberSaveable { mutableStateOf(profile.daysPerWeek.toFloat()) }
    var m by rememberSaveable { mutableStateOf(profile.minutesPerSession.toFloat()) }

    CardBlock(title = "Schedule") {
        Text("Days per week: ${d.toInt()}")
        Slider(value = d, onValueChange = { d = it }, valueRange = 1f..7f, steps = 5)
        Spacer(Modifier.height(8.dp))
        Text("Minutes per session: ${m.toInt()}")
        Slider(value = m, onValueChange = { m = it }, valueRange = 20f..90f, steps = 6)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = {
                    onProfileChange(profile.copy(daysPerWeek = d.toInt(), minutesPerSession = m.toInt()))
                    onNext()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Next") }
        }
    }
}

/* --- Step 5: Injuries & limits --- */
@Composable private fun StepLimits(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onPrev: () -> Unit,
    onFinish: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf(profile.injuriesOrLimits) }
    CardBlock(title = "Injuries or limitations (optional)") {
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            label = { Text("e.g., knee pain, lower back") },
            modifier = Modifier.fillMaxWidth(), maxLines = 3
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = { onProfileChange(profile.copy(injuriesOrLimits = text)); onFinish() },
                modifier = Modifier.weight(1f)
            ) { Text("Finish") }
        }
    }
}

/* =================================================================================
 * Plan + Log screen
 * ================================================================================= */
@Composable
private fun PlanAndLogScreen(
    plan: ExercisePlan,
    profile: UserProfile,
    sessionsList: List<ExerciseSession>,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    onRequestBgRead: () -> Unit,
    onInsertClick: () -> Unit,
    onDetailsClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onAdjustPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 调整计划按钮（显眼且固定在列表上方）
        item {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(bottom = 6.dp),
                onClick = onAdjustPlan
            ) { Text("Adjust plan") }
        }

        item {
            CardBlock(title = "Your weekly plan", padding = 16.dp) {
                Text("Goal: ${profile.goal.name}")
                Spacer(Modifier.height(6.dp))
                Text("Schedule: ${profile.daysPerWeek} days/week · ${profile.minutesPerSession} min/session")
                Spacer(Modifier.height(6.dp))
                Text("Suggested HR: ${plan.suggestedHRZone}")
                Spacer(Modifier.height(6.dp))
                Text("Estimated weekly calorie burn: ${plan.weeklyKcalTarget} kcal")
                Spacer(Modifier.height(10.dp))
                Text("Split:", fontWeight = FontWeight.SemiBold)
                plan.split.forEach { Text("• $it") }
                if (plan.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Notes:", fontWeight = FontWeight.SemiBold)
                    Text(plan.notes)
                }
            }
        }

        item {
            CardBlock(title = "Tutorials") {
                TutorialItem("HIIT basics (12 min)", "40s fast / 20s easy × 12. Warm up & cool down included.")
                Divider(Modifier.padding(vertical = 8.dp))
                TutorialItem("Full-body form cues", "Squat/hinge/push/pull/core. Keep neutral spine. Breathe.")
                Divider(Modifier.padding(vertical = 8.dp))
                TutorialItem("Zone-2 guide", "Keep conversational pace (RPE 4–5). Track time-in-zone.")
            }
        }

        if (!backgroundReadGranted) {
            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(vertical = 4.dp),
                    onClick = onRequestBgRead,
                    enabled = backgroundReadAvailable,
                ) { Text(if (backgroundReadAvailable) "Request Background Read" else "Background Read Not Available") }
            }
        }

        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(vertical = 4.dp),
                onClick = onInsertClick
            ) { Text("Add a sample session") }
        }

        item {
            Text(
                "Today’s sessions",
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp)
            )
        }

        if (sessionsList.isEmpty()) {
            item {
                Text(
                    "No sessions yet.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            items(sessionsList) { s ->
                val appInfo = s.sourceAppInfo
                ExerciseSessionRow(
                    start = s.startTime,
                    end = s.endTime,
                    uid = s.id,
                    name = s.title ?: "No title",
                    sourceAppName = appInfo?.appLabel ?: "Unknown app",
                    sourceAppIcon = appInfo?.icon,
                    onDeleteClick = { uid -> onDeleteClick(uid) },
                    onDetailsClick = { uid -> onDetailsClick(uid) }
                )
            }
        }
    }
}

/* =================================================================================
 * UI bits
 * ================================================================================= */
@Composable private fun CardBlock(
    title: String,
    padding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(padding)) {
            Text(title, style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable private fun GoalChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.surface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            1.dp, if (selected) MaterialTheme.colors.primary
            else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .height(36.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(if (selected) "✓ $text" else text)
        }
    }
}

@Composable private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    GoalChip(text, selected, onClick)
}

@Composable private fun TutorialItem(title: String, body: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = body,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/* =================================================================================
 * Simple plan generator
 * ================================================================================= */
private data class ExercisePlan(
    val weeklyKcalTarget: Int,
    val suggestedHRZone: String,
    val split: List<String>,
    val notes: String
)

private fun generatePlan(p: UserProfile): ExercisePlan {
    val weightDelta = (p.weightKg - p.targetKg)
    val base = when (p.goal) {
        Goal.LoseWeight -> 1500
        Goal.GainWeight -> 600
        Goal.GainMuscle -> 900
        Goal.Maintain -> 700
    }
    val adjust = when (p.level) {
        Level.Beginner -> -100
        Level.Intermediate -> 0
        Level.Advanced -> 150
    }
    val equipmentBias = when (p.equipment) {
        Equipment.None -> 0
        Equipment.Bands -> 50
        Equipment.Dumbbells -> 120
        Equipment.Gym -> 200
    }
    val scheduleBias = (p.daysPerWeek * p.minutesPerSession / 10)
    val weeklyBurn = (base + adjust + equipmentBias + scheduleBias + (weightDelta * 15)).coerceAtLeast(400)

    val hr = when (p.goal) {
        Goal.LoseWeight -> "Zone 2 (RPE 4–5) + HIIT once"
        Goal.GainWeight -> "Moderate cardio, strength focus"
        Goal.GainMuscle -> "Strength focus, short cardio"
        Goal.Maintain -> "Balanced cardio/strength"
    }

    val split = when (p.goal) {
        Goal.LoseWeight -> listOf("HIIT ×1", "Zone2 ×${(p.daysPerWeek - 1).coerceAtLeast(1)}", "Full-body circuit ×1")
        Goal.GainWeight -> listOf("Full-body ×2", "Zone2 ×1", "Mobility ×1")
        Goal.GainMuscle -> when (p.level) {
            Level.Beginner -> listOf("Full-body ×3", "Zone2 ×1")
            else           -> listOf("Push ×1", "Pull ×1", "Legs ×1", "Zone2 ×1")
        }
        Goal.Maintain -> listOf("Mixed cardio ×2", "Compounds ×2", "Mobility ×1")
    }

    val notes = buildString {
        if (p.injuriesOrLimits.isNotBlank()) append("Limitations: ${p.injuriesOrLimits}. ")
        if (p.likes.isNotBlank()) append("Prefer: ${p.likes}. ")
        if (p.dislikes.isNotBlank()) append("Avoid: ${p.dislikes}. ")
        append("Each session ~${p.minutesPerSession} min. Rest 1 day between strength days.")
    }

    return ExercisePlan(
        weeklyKcalTarget = weeklyBurn,
        suggestedHRZone = hr,
        split = split.take(p.daysPerWeek.coerceAtLeast(1)),
        notes = notes
    )
}
