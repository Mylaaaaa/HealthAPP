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
 * Additions (non-destructive):
 * - "Recommend exercise" row with 3 cards; tapping one appends a synthetic task to Today's plan.
 * - Today's plan rows use an action button instead of checkbox:
 *      • Tap → confirm mark completed (or confirm cancel if already completed).
 * - Synthetic tasks (recommended/quick-add) are deletable; built-in plan tasks are not.
 * - All writes go through PlanTasksStore -> both Plan & Workout screens auto-refresh.
 */
@Composable
fun WorkoutDashboardM2(
    modifier: Modifier = Modifier,
    sessionsList: List<ExerciseSession>,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    onRequestBgRead: () -> Unit,
    onInsertClick: () -> Unit,
    onDetailsClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    val app = LocalContext.current.applicationContext
    val today = remember { LocalDate.now() }

    val tasksStore = remember { PlanTasksStore(app) }
    val completedStore = remember { CompletedSessionsStore(app) } // kept for possible use

    // Local state mirrors today's tasks; we always refresh from the store after any write.
    var plannedTasks by remember { mutableStateOf(tasksStore.getTasks(today)) }

    // Hero progress is computed on the fly from plannedTasks
    val plannedCount = plannedTasks.size
    val completedCount = plannedTasks.count { it.completed }
    val progress = if (plannedCount == 0) 0f else completedCount.toFloat() / plannedCount

    // We don't assume fields of ExerciseSession; just render raw list if provided
    val todaySessions = sessionsList

    // Confirmation dialog states
    var toToggle by remember { mutableStateOf<PlanTasksStore.PlanTask?>(null) }
    var toggleToCompleted by remember { mutableStateOf(true) }
    var toDelete by remember { mutableStateOf<PlanTasksStore.PlanTask?>(null) }

    // Listen to prefs → auto refresh when any page calls setTasks()
    DisposableEffect(today) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            plannedTasks = tasksStore.getTasks(today)
        }
        tasksStore.addOnChangeListener(listener)
        onDispose { tasksStore.removeOnChangeListener(listener) }
    }

    val df = remember { DateTimeFormatter.ofPattern("EEE, dd MMM") }

    AnimatedVisibility(visible = !backgroundReadGranted && backgroundReadAvailable) {
        BackgroundReadRequest(
            backgroundReadAvailable = backgroundReadAvailable,
            backgroundReadGranted = backgroundReadGranted,
            onRequestBgRead = onRequestBgRead
        )
    }

    Column(modifier.fillMaxSize()) {
        HeroSectionM2(
            bigNumber = plannedCount,
            planned = plannedCount,
            completed = completedCount,
            progress = progress
        )

        Spacer(Modifier.height(8.dp))

        // ---------- Recommend exercise ----------
        SectionHeaderM2(
            icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null) },
            title = "Recommend exercise",
            subtitle = df.format(today)
        )
        RecommendRow(
            onPick = { title, minutes ->
                // Append a synthetic (deletable) task
                val list = tasksStore.getTasks(today)
                val newTask = PlanTasksStore.PlanTask(
                    taskId = "synth-${System.currentTimeMillis()}",
                    title = title,
                    type = "synthetic",     // mark as synthetic so we can allow deletion
                    completed = false,
                    target = minutes
                )
                tasksStore.setTasks(
                    date = today,
                    dayTitle = tasksStore.getDayTitle(today),
                    tasks = list + newTask
                )
                plannedTasks = tasksStore.getTasks(today) // local refresh
            }
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ---------- Today's plan ----------
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
                        // Ask for confirmation (button changed to confirm dialog)
                        toToggle = p
                        toggleToCompleted = wantCompleted
                    },
                    onDeleteRequest = {
                        // Only allow delete for synthetic tasks (UI 已限制，这里双保险)
                        if (p.type == "synthetic") {
                            toDelete = p
                        }
                    }
                )
                // Whole row click → open details
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(0.dp)
                        .clickable { /* you can open a details dialog here if needed */ }
                )
                Spacer(Modifier.height(8.dp))
            }

            // ---------- Recent Sessions ----------
            if (todaySessions.isNotEmpty()) {
                item {
                    SectionHeaderM2(
                        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
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
            text = {
                Text("This will remove \"${t.title}\" from today's plan. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    // Only remove if synthetic
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

/* --------------------------------------------------------------------------------- */
/* Helper UI blocks — kept minimal; you can keep your originals if you prefer.      */
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
private fun HeroSectionM2(
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

/** Three horizontal recommendation cards. */
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
            modifier = Modifier.weight(1f).height(72.dp),
            title = "Easy walk",
            minutes = 30,
            onPick = onPick
        )
        SuggestionCard(
            modifier = Modifier.weight(1f).height(72.dp),
            title = "Light run",
            minutes = 25,
            onPick = onPick
        )
        SuggestionCard(
            modifier = Modifier.weight(1f).height(72.dp),
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

/** Generic session row: no assumptions about ExerciseSession fields. */
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
                    Icon(Icons.Default.Info, contentDescription = "Details")
                }
                IconButton(onClick = { onDeleteClick(session.hashCode().toString()) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

/**
 * Action row for plan tasks.
 * - Button toggles completion with confirm dialog (instead of a checkbox).
 * - Delete icon only shown for synthetic tasks.
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

            // Primary action button replaces checkbox
            if (!task.completed) {
                OutlinedButton(onClick = { onToggleRequest(true) }) { Text("Mark done") }
            } else {
                OutlinedButton(onClick = { onToggleRequest(false) }) { Text("Unmark") }
            }

            Spacer(Modifier.width(6.dp))

            // Delete only for synthetic (recommended/quick-add) tasks
            if (task.type == "synthetic") {
                IconButton(onClick = onDeleteRequest) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}
