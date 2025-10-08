package com.example.myhealth.presentation.screen.exercisesession

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
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import com.example.myhealth.data.ExerciseSession
import com.example.myhealth.presentation.component.ExerciseSessionRow

/**
 * Exercise hub with four tabs:
 * - Plan: weekly plan + start guided workout (Day bottom sheet -> Start guided)
 * - Workout: today's sessions
 * - Courses: curated routines
 * - Stats: weekly summary
 *
 * Guided and Summary are shown as fullscreen overlays above the tabs.
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
    onDeleteClick: (String) -> Unit = {}
) {
    val appContext = LocalContext.current.applicationContext
    val progressStore = remember { PlanProgressStore(appContext) }
    val activeStore = remember { ActiveDayProgressStore(appContext) } // persistence for partial guided progress

    var selectedTab by rememberSaveable { mutableStateOf(ExerciseTab.Workout) }

    // Guided & Summary overlays
    val guidedVm = remember { GuidedWorkoutViewModel() }
    var showGuided by rememberSaveable { mutableStateOf(false) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    var previewSummary by remember { mutableStateOf<WorkoutSummaryData?>(null) }

    Box(Modifier.fillMaxSize()) {

        // ---------- BASE TABS ----------
        Scaffold { padding ->
            Column(Modifier.padding(padding)) {
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
                    // Persist finished workout
                    val saved = guidedVm.finish(overallRpe = rpe, notes = notes)
                    progressStore.markDone(
                        LocalDate.now(),
                        saved?.title ?: previewSummary!!.title
                    )
                    // Clear partial progress (new run will start clean)
                    activeStore.clear(
                        LocalDate.now(),
                        saved?.title ?: previewSummary!!.title
                    )

                    // Close summary and go to Plan (saved)
                    previewSummary = null
                    showSummary = false
                    guidedVm.clear()
                    selectedTab = ExerciseTab.Plan
                },
                onClose = {
                    // ⬅️ This is the key change you asked for:
                    // When user hits X on Summary, return to the previous screen (Guided/Play).
                    previewSummary = null
                    showSummary = false
                    // DO NOT clear the VM; keep current guided state so user resumes seamlessly
                    showGuided = true
                }
            )
        }
    }
}

// -------------------- ENUM & SUB COMPONENTS --------------------

private enum class ExerciseTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Plan("Plan", Icons.Default.Rule),
    Workout("Workout", Icons.Default.FitnessCenter),
    Courses("Courses", Icons.Default.ListAlt),
    Stats("Stats", Icons.Default.BarChart)
}

@Composable
private fun WorkoutPage(
    modifier: Modifier = Modifier,
    sessionsList: List<ExerciseSession>,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    onRequestBgRead: () -> Unit,
    onInsertClick: () -> Unit,
    onDetailsClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Column(modifier.fillMaxSize()) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onInsertClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add a sample session")
            }
        }

        BackgroundReadRequest(
            backgroundReadAvailable = backgroundReadAvailable,
            backgroundReadGranted = backgroundReadGranted,
            onRequestBgRead = onRequestBgRead
        )

        Text("Today's sessions", Modifier.fillMaxWidth().padding(vertical = 6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (sessionsList.isEmpty()) {
                item { EmptyState() }
            } else {
                items(sessionsList, key = { it.id }) { s ->
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
                    Spacer(Modifier.height(8.dp))
                    Divider(modifier = Modifier.alpha(0.1f))
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
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
