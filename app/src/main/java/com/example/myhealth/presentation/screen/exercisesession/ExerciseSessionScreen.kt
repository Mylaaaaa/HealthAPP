package com.example.myhealth.presentation.screen.exercisesession
import com.example.myhealth.presentation.screen.exercisesession.planaccess.PlanTasksStore

import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import com.example.myhealth.data.ExerciseSession

/**
 * Exercise hub with four tabs:
 * - Plan: weekly plan + start guided workout (Day bottom sheet -> Start guided)
 * - Workout: today's sessions
 * - Courses: curated routines
 * - Stats: weekly summary
 *
 * Guided and Summary are shown as fullscreen overlays above the tabs.
 *
 * CHANGES (non-destructive):
 * - Replaced top TabRow with a BottomNavigation bar (as per your sketch).
 * - Kept the original TabRow block in comments so nothing is lost.
 * - When saving Summary, mirror Guided completion into PlanTasksStore so
 *   the Workout page progress ring / "Completed" updates even if only one item is done.
 */
@Composable
fun ExerciseSessionScreen(
    modifier: Modifier = Modifier,
    // Backward-compat params kept to avoid breaking call sites
    permissions: Set<String> = emptySet(),
    permissionsGranted: Boolean = false,
    uiState: Any? = null,
    onError: (Throwable) -> Unit = {},
    onPermissionsResult: (Boolean) -> Unit = {},
    // New params actually used by the UI
    sessionsList: List<ExerciseSession> = emptyList(),
    backgroundReadAvailable: Boolean = permissions.isNotEmpty(),
    backgroundReadGranted: Boolean = permissionsGranted,
    onPermissionsLaunch: (Set<String>) -> Unit = {},
    onInsertClick: () -> Unit = {},
    onDetailsClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val progressStore = remember { PlanProgressStore(appContext) }
    // Used to reflect Guided completion on Workout page (progress ring & "Completed")
    val planStore = remember { PlanTasksStore(appContext) }
    val activeStore = remember { ActiveDayProgressStore(appContext) } // persistence for partial guided progress

    // Keep your tab enum, but we'll render bottom navigation instead of TabRow
    var selectedTab by rememberSaveable { mutableStateOf(ExerciseTab.Workout) }

    // Guided & Summary overlays
    val guidedVm = remember { GuidedWorkoutViewModel() }
    var showGuided by rememberSaveable { mutableStateOf(false) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    var previewSummary by remember { mutableStateOf<WorkoutSummaryData?>(null) }

    Box(Modifier.fillMaxSize()) {

        // ---------- BASE CONTENT WITH BOTTOM NAV ----------
        Scaffold(
            bottomBar = {
                // BottomNavigation mirrors your former tabs
                BottomNavigation {
                    ExerciseTab.values().forEach { tab ->
                        BottomNavigationItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding)) {

                /* ---------- ORIGINAL TOP TABROW (kept for reference; no longer used) ----------
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    ExerciseTab.values().forEachIndexed { i, tab ->
                        Tab(
                            selected = i == selectedTab.ordinal,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.title) },
                            icon = { Icon(tab.icon, contentDescription = tab.title) }
                        )
                    }
                }
                ------------------------------------------------------------------------------ */

                when (selectedTab) {
                    ExerciseTab.Plan -> ExercisePlanScreen(
                        modifier = Modifier.padding(16.dp),
                        // When user taps “Start guided” for a day
                        onStartDay = { day ->
                            guidedVm.startFromPlan(day)
                            showGuided = true
                        }
                    )
                    ExerciseTab.Workout -> WorkoutPage(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        sessionsList = sessionsList,
                        backgroundReadAvailable = backgroundReadAvailable,
                        backgroundReadGranted = backgroundReadGranted,
                        onRequestBgRead = {
                            onPermissionsLaunch(
                                setOf(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
                            )
                        },
                        onInsertClick = onInsertClick,
                        onDetailsClick = onDetailsClick,
                        onDeleteClick = onDeleteClick
                    )
                    ExerciseTab.Courses -> ExerciseCoursesScreen(Modifier.padding(16.dp))
                    ExerciseTab.Stats -> ExerciseStatsScreen(
                        modifier = Modifier.padding(16.dp),
                        sessions = sessionsList
                    )
                }
            }
        }

        // ---------- FULLSCREEN OVERLAYS ----------

        // Guided (Play) overlay
        if (showGuided && guidedVm.activeWorkout != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                GuidedWorkoutScreen(
                    vm = guidedVm,
                    onExitConfirm = {
                        // user discards: leave Guided
                        guidedVm.clear()
                        showGuided = false
                        previewSummary = null
                    },
                    onFinishRequest = {
                        // open Summary preview
                        val aw = guidedVm.activeWorkout!!
                        val done = aw.items.count { it.status == ItemStatus.DONE }
                        val completion = if (aw.items.isEmpty()) 0f else done.toFloat() / aw.items.size

                        previewSummary = WorkoutSummaryData(
                            title = aw.dayTitle,
                            totalMs = guidedVm.totalElapsedMs,
                            items = aw.items.toList(),
                            completionRate = completion,
                            overallRpe = 7,
                            notes = null
                        )
                        // Hide Guided while Summary is on
                        showGuided = false
                        showSummary = true
                    }
                )
            }
        }

        // Summary overlay
        if (showSummary && previewSummary != null) {
            WorkoutSummaryScreen(
                preview = previewSummary!!,
                onSave = { rpe, notes ->
                    // Persist finished workout to history
                    val saved = guidedVm.finish(overallRpe = rpe, notes = notes)
                    progressStore.markDone(
                        LocalDate.now(),
                        saved?.title ?: previewSummary!!.title
                    )
                    // Clear partial progress for this day
                    activeStore.clear(
                        LocalDate.now(),
                        saved?.title ?: previewSummary!!.title
                    )

                    // === Mirror Guided completion back into today's PlanTasksStore ===
                    // Reason: Workout page progress ring & "Completed" read from PlanTasksStore.
                    // We set per-item completion so even partial completion is reflected immediately.
                    val today = LocalDate.now()
                    val currentTasks = planStore.getTasks(today)
                    if (currentTasks.isNotEmpty()) {
                        // Keep quick-added (synthetic) tasks intact; update only planned items by index order.
                        val (planned, synthetic) = currentTasks.partition { it.type != "synthetic" }
                        val finished = (saved ?: previewSummary!!).items
                        val updatedPlanned = planned.mapIndexed { index, t ->
                            val st = finished.getOrNull(index)?.status
                            // If you only want DONE to count: (st == ItemStatus.DONE)
                            val doneFlag = (st == ItemStatus.DONE)
                            t.copy(completed = doneFlag)
                        }
                        planStore.setTasks(
                            date = today,
                            dayTitle = planStore.getDayTitle(today),
                            tasks = updatedPlanned + synthetic
                        )
                    }
                    // === end mirror ===

                    // Close summary and go to Plan (saved)
                    previewSummary = null
                    showSummary = false
                    guidedVm.clear()
                    selectedTab = ExerciseTab.Plan
                },
                onClose = {
                    // Back to Guided without losing state
                    previewSummary = null
                    showSummary = false
                    showGuided = true
                }
            )
        }
    }
}
// === Restore missing WorkoutPage wrapper ===
@Composable
fun WorkoutPage(
    modifier: Modifier = Modifier,
    sessionsList: List<ExerciseSession>,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    onRequestBgRead: () -> Unit,
    onInsertClick: () -> Unit,
    onDetailsClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    WorkoutDashboardM2(
        modifier = modifier,
        sessionsList = sessionsList,
        backgroundReadAvailable = backgroundReadAvailable,
        backgroundReadGranted = backgroundReadGranted,
        onRequestBgRead = onRequestBgRead,
        onInsertClick = onInsertClick,
        onDetailsClick = onDetailsClick,
        onDeleteClick = onDeleteClick
    )
}

// ===== Sub-components (Material 2) =====
// (All your original helper composables & enums are kept below.)

@Composable
private fun HeroSectionM2(
    done: Int,
    goal: Int,
    progress: Float,
    streakDays: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp),
        elevation = 2.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress with number
            Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = 1f,
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
                )
                CircularProgressIndicator(
                    progress = progress,
                    strokeWidth = 10.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$done/$goal", style = MaterialTheme.typography.h6)
                    Text("sessions", style = MaterialTheme.typography.caption)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                MetricRowM2(label = "Streak", value = "$streakDays days")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.95f)
                )
                Text(
                    text = "${(progress * 100).toInt()}% of daily goal",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricRowM2(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.caption
        )
        Text(
            value,
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@Composable
private fun QuickActionsRowM2(onQuickAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onQuickAdd) { Text("Add walk 30m") }
        OutlinedButton(onClick = onQuickAdd) { Text("Add run 25m") }
        OutlinedButton(onClick = onQuickAdd) { Text("Add strength 35m") }
    }
}

@Composable
private fun RecommendationCardM2(onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Based on last week", style = MaterialTheme.typography.caption)
                Text(
                    "Recommended: Easy Walk · 30 minutes",
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    "Gentle cardio to maintain your streak.",
                    style = MaterialTheme.typography.body2
                )
            }
            Button(onClick = onAdd) { Text("Add") }
        }
    }
}

private enum class ExerciseTab(
    val title: String,
    val icon: ImageVector
) {
    Plan("Plan", Icons.Default.Rule),
    Workout("Workout", Icons.Default.FitnessCenter),
    Courses("Courses", Icons.Default.ListAlt),
    Stats("Stats", Icons.Default.BarChart)
}

@Composable
private fun BackgroundReadRequest(
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    onRequestBgRead: () -> Unit
) {
    if (!backgroundReadAvailable) return
    AnimatedVisibility(visible = !backgroundReadGranted) {
        Card(
            backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.08f),
            elevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Allow background read", style = MaterialTheme.typography.subtitle2)
                    Text(
                        "Enable background reads to keep your sessions list up-to-date.",
                        style = MaterialTheme.typography.body2
                    )
                }
                OutlinedButton(onClick = onRequestBgRead) { Text("Grant") }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun EmptyState() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.03f))
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text("No sessions yet.", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
            Text(
                "Tap the button above to insert a sample workout.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
