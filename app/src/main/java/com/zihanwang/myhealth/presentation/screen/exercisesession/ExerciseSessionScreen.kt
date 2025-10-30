package com.zihanwang.myhealth.presentation.screen.exercisesession
import com.zihanwang.myhealth.presentation.screen.exercisesession.planaccess.PlanTasksStore

import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.zihanwang.myhealth.data.ExerciseSession
import com.zihanwang.myhealth.data.HealthConnectManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zihanwang.myhealth.presentation.screen.exercisesession.WorkoutDashboardEntry
import com.zihanwang.myhealth.presentation.screen.exercisesession.ExerciseSessionViewModel
import com.zihanwang.myhealth.presentation.screen.exercisesession.ExerciseSessionViewModelFactory
import androidx.compose.ui.text.font.FontWeight


@Composable
fun ExerciseSessionScreen(
    modifier: Modifier = Modifier,
    healthConnectManager: HealthConnectManager,
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
    /* -----------------------------------------------------------
     * Tab state MUST be declared before effects that depend on it.
     * ----------------------------------------------------------- */
    var selectedTab by rememberSaveable { mutableStateOf(ExerciseTab.Workout) }

    /* -----------------------------------------------------------
     * Remember the LocalDate the guided workout belongs to when the
     * user starts from the Plan tab (e.g., Wednesday while today is Saturday).
     * We derive the date from PlanDay.title = Mon/Tue/.../Sun.
     * ----------------------------------------------------------- */
    var targetPlanDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }

    /* -----------------------------------------------------------
     * Stats tab — real weekly data container.
     * We'll fill this only when user views the Stats page.
     * ----------------------------------------------------------- */
    var statsSessions by remember { mutableStateOf<List<ExerciseSession>>(emptyList()) }

    /* -----------------------------------------------------------
     * When Stats is selected, read this week's sessions
     * (Monday 00:00 -> next Monday 00:00, end exclusive) from HC.
     * Using end-exclusive avoids nanosecond edge cases on the boundary.
     * ----------------------------------------------------------- */
    LaunchedEffect(selectedTab) {
        if (selectedTab == ExerciseTab.Stats) {
            val now = java.time.ZonedDateTime.now()
            val weekStart = now
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay(now.zone)
            val weekEndExclusive = weekStart.plusDays(7) // end-exclusive

            runCatching {
                val recs = healthConnectManager.readExerciseSessions(
                    start = weekStart.toInstant(),
                    end   = weekEndExclusive.toInstant()
                )
                statsSessions = recs.map { r ->
                    ExerciseSession(
                        id = r.metadata.id,
                        title = r.title ?: "Session",
                        startTime = r.startTime.atZone(now.zone),
                        endTime   = r.endTime.atZone(now.zone),
                        sourceAppInfo = null
                    )
                }
            }.onFailure {
                // Do not crash the UI; just report error and keep last good data.
                onError(it)
            }
        }
    }

    val appContext = LocalContext.current.applicationContext
    val progressStore = remember { PlanProgressStore(appContext) }
    // Used to reflect Guided completion on Workout page (progress ring & "Completed")
    val planStore = remember { PlanTasksStore(appContext) }
    val activeStore = remember { ActiveDayProgressStore(appContext) } // persistence for partial guided progress

    // Guided & Summary overlays
    val guidedVm = remember { GuidedWorkoutViewModel() }
    var showGuided by rememberSaveable { mutableStateOf(false) }
    var showSummary by rememberSaveable { mutableStateOf(false) }
    var previewSummary by remember { mutableStateOf<WorkoutSummaryData?>(null) }

    Box(Modifier.fillMaxSize()) {

        // ---------- BASE CONTENT WITH BOTTOM NAV ----------
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
            bottomBar = {
                BottomNavigation(
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.onSurface,
                    elevation = 6.dp
                ) {
                    ExerciseTab.values().forEach { tab ->
                        val selected = selectedTab == tab
                        val iconTint = if (selected) Color(0xFF1976D2) else Color.Gray

                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.2f else 1f,
                            label = "tab_scale"
                        )

                        BottomNavigationItem(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = iconTint,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                )
                            },
                            label = {
                                Text(
                                    tab.title,
                                    color = iconTint,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            alwaysShowLabel = true,
                            selectedContentColor = iconTint,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }
            }

        ) { paddingValues ->

            // IMPORTANT: no verticalScroll here – avoid nesting with LazyColumn inside tabs.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Smooth fade between tabs
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = androidx.compose.animation.core.tween(300),
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {

                        ExerciseTab.Plan -> ExercisePlanScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .semantics { contentDescription = "Open Plan tab" },
                            onStartDay = { day, _ ->
                                // Map plan card title (Mon..Sun) to a real LocalDate in this week
                                targetPlanDate = resolveDateFromPlanTitle(day.title)
                                guidedVm.startFromPlan(day)
                                showGuided = true
                            }
                        )

                        // DO NOT wrap WorkoutPage with verticalScroll (it uses Lazy lists inside)
                        ExerciseTab.Workout -> WorkoutPage(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .semantics { contentDescription = "Open Workout tab" },
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

                        ExerciseTab.Courses -> ExerciseCoursesScreen(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .semantics { contentDescription = "Open Courses tab" }
                        )

                        ExerciseTab.Stats -> {
                            val zone = java.time.ZoneId.systemDefault()
                            val today = java.time.LocalDate.now(zone)
                            val weekStart = today.with(
                                java.time.temporal.TemporalAdjusters.previousOrSame(
                                    java.time.DayOfWeek.MONDAY
                                )
                            )

                            // Base = real sessions of this week
                            val base: List<ExerciseSession> =
                                if (statsSessions.isNotEmpty()) statsSessions else sessionsList

                            val baseByDate = base.groupBy { it.startTime.toLocalDate() }
                            val defaultPerTaskMinutes = 30

                            // Make synthetic sessions from completed Plan tasks on days without real sessions
                            val syntheticFromPlan: List<ExerciseSession> = (0..6).flatMap { d ->
                                val date = weekStart.plusDays(d.toLong())
                                if (!baseByDate[date].isNullOrEmpty()) return@flatMap emptyList()

                                val tasks = planStore.getTasks(date)
                                val completed = tasks.filter { it.completed }
                                val minutesFromTasks = completed.size * defaultPerTaskMinutes

                                if (minutesFromTasks > 0) {
                                    val start = date.atTime(12, 0).atZone(zone)
                                    val end = start.plusMinutes(minutesFromTasks.toLong())
                                    val title = (planStore.getDayTitle(date) ?: "").ifBlank { "Planned" }
                                    listOf(
                                        ExerciseSession(
                                            id = "plan-$date",
                                            title = "$title (planned)",
                                            startTime = start,
                                            endTime = end,
                                            sourceAppInfo = null
                                        )
                                    )
                                } else emptyList()
                            }

                            val merged = (base + syntheticFromPlan).sortedBy { it.startTime }

                            ExerciseStatsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .semantics { contentDescription = "Open Stats tab" },
                                sessions = merged
                            )
                        }
                    }
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
                        // We intentionally do not clear targetPlanDate here so the user can resume.
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

                    // --- Write back to the Plan day the user actually started from ---
                    val dateToCommit = targetPlanDate ?: LocalDate.now()

                    progressStore.markDone(
                        dateToCommit,
                        saved?.title ?: previewSummary!!.title
                    )
                    // Clear partial progress for this day
                    activeStore.clear(
                        dateToCommit,
                        saved?.title ?: previewSummary!!.title
                    )

                    // === Mirror Guided completion back into that day's PlanTasksStore ===
                    // Reason: Plan UI (progress ring & "Completed" badge) reads from PlanTasksStore.
                    val currentTasks = planStore.getTasks(dateToCommit)
                    if (currentTasks.isNotEmpty()) {
                        // Keep quick-added (synthetic) tasks intact; update only planned items by index order.
                        val (planned, synthetic) = currentTasks.partition { it.type != "synthetic" }

                        val finished = (saved ?: previewSummary!!).items
                        val allDone = finished.isNotEmpty() && finished.all { it.status == ItemStatus.DONE }

                        // If ALL guided items are DONE -> mark whole day done so "Completed" badge lights up
                        val updatedPlanned = if (allDone) {
                            planned.map { it.copy(completed = true) }
                        } else {
                            planned.mapIndexed { index, t ->
                                val st = finished.getOrNull(index)?.status
                                val doneFlag = (st == ItemStatus.DONE)
                                t.copy(completed = doneFlag)
                            }
                        }

                        val updatedSynthetic = if (allDone) {
                            synthetic.map { it.copy(completed = true) }
                        } else {
                            synthetic
                        }

                        planStore.setTasks(
                            date = dateToCommit,
                            dayTitle = planStore.getDayTitle(dateToCommit),
                            tasks = updatedPlanned + updatedSynthetic
                        )
                    }
                    // === end mirror ===

                    // Close summary and go to Plan (saved)
                    previewSummary = null
                    showSummary = false
                    guidedVm.clear()

                    // After commit, reset the captured date
                    targetPlanDate = null

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
    // Wrapper overload: auto-provide HealthConnectManager so old call sites keep working
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }

    WorkoutPage(
        modifier = modifier,
        sessionsList = sessionsList,
        backgroundReadAvailable = backgroundReadAvailable,
        backgroundReadGranted = backgroundReadGranted,
        onRequestBgRead = onRequestBgRead,
        onInsertClick = onInsertClick,
        onDetailsClick = onDetailsClick,
        onDeleteClick = onDeleteClick,
        healthConnectManager = healthConnectManager
    )
}

@Composable
fun WorkoutPage(
    modifier: Modifier = Modifier,
    sessionsList: List<ExerciseSession>,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    onRequestBgRead: () -> Unit,
    onInsertClick: () -> Unit,
    onDetailsClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    healthConnectManager: HealthConnectManager
) {
    // Use your ViewModel with HealthConnectManager via factory
    val vm: ExerciseSessionViewModel = viewModel(
        factory = ExerciseSessionViewModelFactory(healthConnectManager)
    )

    // Feed VM state + callbacks into your existing dashboard UI
    WorkoutDashboardEntry(
        vm = vm,
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
                Icon(Icons.Default.NotificationsActive, contentDescription = "Workout reminder") // a11y
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
            Icon(Icons.Default.DirectionsRun, contentDescription = "No sessions yet", tint = Color.Gray) // a11y
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
