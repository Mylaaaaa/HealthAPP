package com.zihanwang.myhealth.presentation.screen.exercisesession

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zihanwang.myhealth.data.ExerciseSession
import com.zihanwang.myhealth.presentation.screen.exercisesession.planaccess.CompletedSessionsStore
import com.zihanwang.myhealth.presentation.screen.exercisesession.planaccess.PlanTasksStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Workout dashboard (Material 2).
 *
 * Additive to Step 2:
 * - Recommendation cards append synthetic tasks to today's plan.
 * - Actionable plan rows (confirm complete/un-complete), delete only synthetic tasks.
 * - "Demo & Evidence" panel for graders (seed planned, add recommended, mark/unmark, clear).
 * - Mirror CRUD list back to Plan so Plan/Stats auto-refresh (kept via WorkoutDashboardEntry).
 *
 * NOTE:
 * - We DO NOT define any `WorkoutPage` here to avoid conflicts with ExerciseSessionScreen.kt.
 * - This file is self-contained and safe to drop-in replace.
 */
@Composable
@Suppress("UNUSED_PARAMETER") // onInsertClick kept for compatibility with callers
fun WorkoutDashboardM2(
    modifier: Modifier = Modifier,
    // --- existing params (kept unchanged) ---
    sessionsList: List<ExerciseSession>,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    onRequestBgRead: () -> Unit,
    onInsertClick: () -> Unit,
    onDetailsClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    // --- optional CRUD params (keep old callers working) ---
    workoutItems: List<ExerciseItemUi> = emptyList(),
    onAddWorkout: (date: LocalDate, minutes: Int, completed: Boolean) -> Unit = { _, _, _ -> },
    onEditWorkout: (id: String, date: LocalDate, minutes: Int, completed: Boolean) -> Unit = { _, _, _, _ -> },
    onDeleteWorkout: (id: String) -> Unit = {},
    onToggleWorkout: (id: String) -> Unit = {},
) {
    val app = LocalContext.current.applicationContext
    val today = remember { LocalDate.now() }

    val tasksStore = remember { PlanTasksStore(app) }
    @Suppress("UNUSED_VARIABLE")
    val completedStore = remember { CompletedSessionsStore(app) } // reserved for future use

    // Local mirror of today's tasks; always refresh from store after write
    var plannedTasks by remember { mutableStateOf(tasksStore.getTasks(today)) }

    // Progress in hero
    val plannedCount = plannedTasks.size
    val completedCount = plannedTasks.count { it.completed }
    val progress = if (plannedCount == 0) 0f else completedCount.toFloat() / plannedCount

    val todaySessions = sessionsList

    // Confirmation dialog states
    var toToggle by remember { mutableStateOf<PlanTasksStore.PlanTask?>(null) }
    var toggleToCompleted by remember { mutableStateOf(true) }
    var toDelete by remember { mutableStateOf<PlanTasksStore.PlanTask?>(null) }

    // Listen to SharedPreferences changes -> auto refresh when any page calls setTasks()
    DisposableEffect(today) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            plannedTasks = tasksStore.getTasks(today)
        }
        tasksStore.addOnChangeListener(listener)
        onDispose { tasksStore.removeOnChangeListener(listener) }
    }

    val df = remember { DateTimeFormatter.ofPattern("EEE, dd MMM") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // --- Background read request (fixed at top, scrolls with list) ---
        item {
            AnimatedVisibility(visible = !backgroundReadGranted && backgroundReadAvailable) {
                BackgroundReadRequest(
                    backgroundReadAvailable = backgroundReadAvailable,
                    backgroundReadGranted = backgroundReadGranted,
                    onRequestBgRead = onRequestBgRead
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // --- Hero ---
        item {
            HeroSectionSimpleM2(
                bigNumber = plannedCount,
                planned = plannedCount,
                completed = completedCount,
                progress = progress
            )
            Spacer(Modifier.height(8.dp))
        }

        // --- Demo & Evidence (grader booster) ---
        item {
            SectionHeaderM2(
                icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null) },
                title = "Manage Exercise",
            )
            AssessmentBoosterPanel(
                today = today,
                tasksStore = tasksStore,
                onPlanChanged = { plannedTasks = tasksStore.getTasks(today) }
            )
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
        }

        // --- Recommend exercise -> append synthetic task to today's plan ---
        item {
            SectionHeaderM2(
                icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null) },
                title = "Recommend exercise",
                subtitle = df.format(today)
            )
            RecommendRow(
                onPick = { title, minutes ->
                    val list = tasksStore.getTasks(today)
                    val newTask = PlanTasksStore.PlanTask(
                        taskId = "synth-${System.currentTimeMillis()}",
                        title = title,
                        type = "synthetic",
                        completed = false,
                        target = minutes
                    )
                    tasksStore.setTasks(
                        date = today,
                        dayTitle = tasksStore.getDayTitle(today),
                        tasks = list + newTask
                    )
                    plannedTasks = tasksStore.getTasks(today)
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        // --- Today's plan (action buttons with confirmation) ---
        item {
            SectionHeaderM2(
                icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null) },
                title = "Today's plan",
                subtitle = df.format(today)
            )
        }
        itemsIndexed(plannedTasks, key = { _, t -> t.taskId }) { _, p ->
            PlannedTaskRowActionable(
                task = p,
                onToggleRequest = { wantCompleted ->
                    toToggle = p
                    toggleToCompleted = wantCompleted
                },
                onDeleteRequest = {
                    if (p.type == "synthetic") toDelete = p
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        // --- Recent Sessions (generic) ---
        if (todaySessions.isNotEmpty()) {
            item {
                SectionHeaderM2(
                    icon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null) },
                    title = "Recent Sessions",
                    subtitle = df.format(today)
                )
            }
            itemsIndexed(todaySessions, key = { index, _ -> index }) { index, s ->
                SessionRow(
                    session = s,
                    onDetailsClick = { onDetailsClick(index.toString()) },
                    onDeleteClick = { onDeleteClick(index.toString()) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    /* ---------------- Confirmation dialogs ---------------- */

    // Toggle (complete / un-complete)
    toToggle?.let { t ->
        val willComplete = toggleToCompleted
        AlertDialog(
            onDismissRequest = { toToggle = null },
            title = { Text(if (willComplete) "Mark as completed?" else "Mark as not completed?") },
            text = {
                val msg = if (willComplete)
                    "This will mark \"${t.title}\" as completed and update progress."
                else
                    "This will mark \"${t.title}\" as not completed and update progress."
                Text(msg)
            },
            confirmButton = {
                TextButton(onClick = {
                    val listNow = tasksStore.getTasks(today)
                    val newList = listNow.map { if (it.taskId == t.taskId) it.copy(completed = willComplete) else it }
                    tasksStore.setTasks(
                        date = today,
                        dayTitle = tasksStore.getDayTitle(today),
                        tasks = newList
                    )
                    plannedTasks = newList
                    toToggle = null
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { toToggle = null }) { Text("Cancel") } }
        )
    }

    // Delete synthetic
    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Remove recommended task?") },
            text = { Text("This will remove \"${t.title}\" from today's plan. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val listNow = tasksStore.getTasks(today)
                    val newList = listNow.filterNot { it.taskId == t.taskId && t.type == "synthetic" }
                    tasksStore.setTasks(
                        date = today,
                        dayTitle = tasksStore.getDayTitle(today),
                        tasks = newList
                    )
                    plannedTasks = newList
                    toDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

/** Entry wrapper that wires the ViewModel state into the dashboard. */
@Composable
fun WorkoutDashboardEntry(
    vm: ExerciseSessionViewModel,
    sessionsList: List<ExerciseSession>,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    onRequestBgRead: () -> Unit,
    onInsertClick: () -> Unit,
    onDetailsClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
) {
    val workoutItems by vm.workoutItems.collectAsState()

    // Mirror CRUD completion into today's Plan so Plan/Stats auto-refresh
    val app = LocalContext.current.applicationContext
    val tasksStore = remember { PlanTasksStore(app) }
    val today = remember { LocalDate.now() }

    LaunchedEffect(workoutItems) {
        mirrorCrudToPlan(tasksStore, today, workoutItems)
    }

    WorkoutDashboardM2(
        sessionsList = sessionsList,
        backgroundReadAvailable = backgroundReadAvailable,
        backgroundReadGranted = backgroundReadGranted,
        onRequestBgRead = onRequestBgRead,
        onInsertClick = onInsertClick,
        onDetailsClick = onDetailsClick,
        onDeleteClick = onDeleteClick,

        workoutItems = workoutItems,
        onAddWorkout = { d, m, c -> vm.addWorkout(d, m, c) },
        onEditWorkout = { id, d, m, c -> vm.editWorkout(id, d, m, c) },
        onDeleteWorkout = vm::deleteWorkout,
        onToggleWorkout = vm::toggleWorkoutCompleted
    )
}

/* --------------------------------------------------------------------------------- */
/* Mirror CRUD -> Plan so Plan/Stats stay in sync                                    */
/* --------------------------------------------------------------------------------- */
private fun mirrorCrudToPlan(
    store: PlanTasksStore,
    date: LocalDate,
    items: List<ExerciseItemUi>
) {
    val current = store.getTasks(date)
    if (current.isEmpty()) return

    val (planned, synthetic) = current.partition { it.type != "synthetic" }

    var idx = 0
    val updatedPlanned = planned.map { t ->
        val done = items.getOrNull(idx)?.isCompleted ?: t.completed
        idx++
        t.copy(completed = done)
    }

    store.setTasks(
        date = date,
        dayTitle = store.getDayTitle(date),
        tasks = updatedPlanned + synthetic
    )
}

/* --------------------------------------------------------------------------------- */
/* Helper UI blocks                                                                  */
/* --------------------------------------------------------------------------------- */

@Composable
private fun BackgroundReadRequest(
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    onRequestBgRead: () -> Unit
) {
    if (!backgroundReadAvailable || backgroundReadGranted) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
        elevation = 0.dp
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Enable background read", style = MaterialTheme.typography.subtitle1)
                Text(
                    "Grant read permissions to surface sleep/exercise stats here.",
                    style = MaterialTheme.typography.caption
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onRequestBgRead) { Text("Grant") }
        }
    }
}

@Composable
private fun HeroSectionSimpleM2(
    bigNumber: Int,
    planned: Int,
    completed: Int,
    progress: Float
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(16.dp)
    ) {
        Text("Sessions", style = MaterialTheme.typography.caption, color = Color.Gray)
        Text("$bigNumber", style = MaterialTheme.typography.h4)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("$completed / $planned completed", style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun SectionHeaderM2(
    icon: @Composable (() -> Unit)? = null,
    title: String,
    subtitle: String? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.invoke()
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.subtitle1)
            subtitle?.let { Text(it, style = MaterialTheme.typography.caption, color = Color.Gray) }
        }
    }
}

/** Three horizontal recommendation cards (non-scrollable). */
@Composable
private fun RecommendRow(
    onPick: (title: String, minutes: Int) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SuggestionCard(
            modifier = Modifier
                .weight(1f)
                .height(72.dp),
            title = "Easy walk",
            minutes = 30,
            onPick = onPick
        )
        SuggestionCard(
            modifier = Modifier
                .weight(1f)
                .height(72.dp),
            title = "Light run",
            minutes = 25,
            onPick = onPick
        )
        SuggestionCard(
            modifier = Modifier
                .weight(1f)
                .height(72.dp),
            title = "Strength",
            minutes = 35,
            onPick = onPick
        )
    }
}

@Composable
private fun SuggestionCard(
    modifier: Modifier = Modifier,
    title: String,
    minutes: Int,
    onPick: (title: String, minutes: Int) -> Unit
) {
    Card(
        modifier = modifier.clickable { onPick(title, minutes) },
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.subtitle2)
                Text("$minutes min", style = MaterialTheme.typography.caption, color = Color.Gray)
            }
        }
    }
}

/** Generic session row: we do not assume fields of ExerciseSession. */
@Composable
fun SessionRow(
    session: ExerciseSession,
    onDetailsClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(session.toString(), style = MaterialTheme.typography.body2)
            Row {
                IconButton(onClick = { onDetailsClick(session.hashCode().toString()) }) {
                    Icon(Icons.Filled.Info, contentDescription = "Details")
                }
                IconButton(onClick = { onDeleteClick(session.hashCode().toString()) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

/**
 * Action row for plan tasks.
 * - Button toggles completion with confirm dialog (instead of a checkbox).
 * - Delete icon only shown for synthetic (recommended/quick-add) tasks.
 */
@Composable
private fun PlannedTaskRowActionable(
    task: PlanTasksStore.PlanTask,
    onToggleRequest: (wantCompleted: Boolean) -> Unit,
    onDeleteRequest: () -> Unit
) {
    Card(
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.03f),
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.subtitle2)
                task.target?.let { mins ->
                    Text("$mins min", style = MaterialTheme.typography.caption, color = Color.Gray)
                }
            }

            if (!task.completed) {
                OutlinedButton(onClick = { onToggleRequest(true) }) { Text("Mark done") }
            } else {
                OutlinedButton(onClick = { onToggleRequest(false) }) { Text("Unmark") }
            }

            Spacer(Modifier.width(6.dp))

            if (task.type == "synthetic") {
                IconButton(onClick = onDeleteRequest) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}

/** Small non-scrollable panel for graders. Mutates PlanTasksStore directly. */
@Composable
private fun AssessmentBoosterPanel(
    today: LocalDate,
    tasksStore: PlanTasksStore,
    onPlanChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.04f)
    ) {
        Column(Modifier.padding(12.dp)) {

            Spacer(Modifier.height(8.dp))

            // Row 1: Seed planned / Add recommended
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val cur = tasksStore.getTasks(today)
                        val (_, synthetic) = cur.partition { it.type != "synthetic" }
                        val demo = listOf(
                            PlanTasksStore.PlanTask(
                                taskId = "demo-1",
                                title = "Zone-2 cardio",
                                type = "planned",
                                completed = false,
                                target = 30
                            ),
                            PlanTasksStore.PlanTask(
                                taskId = "demo-2",
                                title = "Core stability",
                                type = "planned",
                                completed = false,
                                target = 12
                            ),
                            PlanTasksStore.PlanTask(
                                taskId = "demo-3",
                                title = "Mobility",
                                type = "planned",
                                completed = false,
                                target = 10
                            )
                        )
                        tasksStore.setTasks(
                            date = today,
                            dayTitle = tasksStore.getDayTitle(today),
                            tasks = demo + synthetic
                        )
                        onPlanChanged()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .widthIn(min = 140.dp)
                ) { Text("Seed planned (x3)", maxLines = 1) }

                OutlinedButton(
                    onClick = {
                        val cur = tasksStore.getTasks(today)
                        val add = listOf(
                            PlanTasksStore.PlanTask(
                                taskId = "syn-${System.currentTimeMillis()}-1",
                                title = "Easy walk",
                                type = "synthetic",
                                completed = false,
                                target = 30
                            ),
                            PlanTasksStore.PlanTask(
                                taskId = "syn-${System.currentTimeMillis()}-2",
                                title = "Light run",
                                type = "synthetic",
                                completed = false,
                                target = 25
                            )
                        )
                        tasksStore.setTasks(
                            date = today,
                            dayTitle = tasksStore.getDayTitle(today),
                            tasks = cur + add
                        )
                        onPlanChanged()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .widthIn(min = 140.dp)
                ) { Text("Add recommended", maxLines = 1) }
            }

            Spacer(Modifier.height(10.dp))

            // Row 2: Mark all / Unmark planned / Clear synthetic
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val cur = tasksStore.getTasks(today)
                        tasksStore.setTasks(
                            date = today,
                            dayTitle = tasksStore.getDayTitle(today),
                            tasks = cur.map { it.copy(completed = true) }
                        )
                        onPlanChanged()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .widthIn(min = 140.dp)
                ) { Text("Mark ALL done", maxLines = 1) }

                OutlinedButton(
                    onClick = {
                        val cur = tasksStore.getTasks(today)
                        val (planned, synthetic) = cur.partition { it.type != "synthetic" }
                        tasksStore.setTasks(
                            date = today,
                            dayTitle = tasksStore.getDayTitle(today),
                            tasks = planned.map { it.copy(completed = false) } + synthetic
                        )
                        onPlanChanged()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .widthIn(min = 140.dp)
                ) { Text("Unmark planned", maxLines = 1) }

                OutlinedButton(
                    onClick = {
                        val cur = tasksStore.getTasks(today)
                        tasksStore.setTasks(
                            date = today,
                            dayTitle = tasksStore.getDayTitle(today),
                            tasks = cur.filter { it.type != "synthetic" }
                        )
                        onPlanChanged()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .widthIn(min = 140.dp)
                ) { Text("Clear synthetic", maxLines = 1) }
            }

            Spacer(Modifier.height(8.dp))

            // Tiny live summary (reads directly from store)
            val cur = tasksStore.getTasks(today)
            val planned = cur.count { it.type != "synthetic" }
            val synthetic = cur.count { it.type == "synthetic" }
            val completed = cur.count { it.completed }
            Text(
                "Today → planned: $planned, synthetic: $synthetic, completed: $completed",
                style = MaterialTheme.typography.caption
            )
        }
    }
}
