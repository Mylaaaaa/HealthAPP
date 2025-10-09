package com.example.myhealth.presentation.screen.exercisesession

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myhealth.presentation.screen.exercisesession.planaccess.CompletedSessionsStore
import com.example.myhealth.presentation.screen.exercisesession.planaccess.PlanTasksStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Use the same session type as in ExerciseSessionScreen
import com.example.myhealth.data.ExerciseSession

/**
 * Workout dashboard (Material 2).
 *
 * Additions (non-destructive):
 * - "Recommend exercise" row with 3 cards; tapping one appends a synthetic task to Today's plan.
 * - Today's plan rows are checkable; clicking a row opens a details dialog with "Mark as completed".
 * - Any completion change writes to PlanTasksStore and immediately refreshes the hero progress.
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

    var plannedTasks by remember { mutableStateOf(tasksStore.getTasks(today)) }
    val plannedCount = plannedTasks.size
    val completedCount = plannedTasks.count { it.completed }
    val progress = if (plannedCount == 0) 0f else completedCount.toFloat() / plannedCount

    // Don't assume fields on ExerciseSession; show raw list
    val todaySessions = sessionsList

    // Dialog state
    var detailTask by remember { mutableStateOf<PlanTasksStore.PlanTask?>(null) }

    // Listen to prefs → auto refresh progress when setTasks() is called anywhere
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
                val list = tasksStore.getTasks(today)
                val newTask = PlanTasksStore.PlanTask(
                    taskId = "synth-${System.currentTimeMillis()}",
                    title = title,
                    type = "synthetic", // so our partition(filter) logic keeps working
                    completed = false,
                    target = minutes
                )
                tasksStore.setTasks(
                    date = today,
                    dayTitle = tasksStore.getDayTitle(today),
                    tasks = list + newTask
                )
                plannedTasks = tasksStore.getTasks(today) // local instant refresh
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
                PlannedTaskRowCheckable(
                    task = p,
                    onToggle = { checked ->
                        val listNow = tasksStore.getTasks(today)
                        val newList = listNow.map { if (it.taskId == p.taskId) it.copy(completed = checked) else it }
                        tasksStore.setTasks(
                            date = today,
                            dayTitle = tasksStore.getDayTitle(today),
                            tasks = newList
                        )
                        plannedTasks = newList
                    }
                )
                // Click anywhere below the checkbox row to open details
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(0.dp)
                        .clickable { detailTask = p }
                )
                Spacer(Modifier.height(8.dp))
            }

            item { Divider(modifier = Modifier.alpha(0.08f)) }
            item { Spacer(Modifier.height(8.dp)) }

            // ---------- Recent Sessions ----------
            if (todaySessions.isEmpty()) {
                item { EmptyStateCard() }
            } else {
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
                        onDeleteClick  = { onDeleteClick(index.toString()) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ---------- Details dialog for a plan task ----------
    detailTask?.let { t ->
        PlanTaskDetailDialog(
            task = t,
            onDismiss = { detailTask = null },
            onMarkCompleted = {
                val listNow = tasksStore.getTasks(today)
                val newList = listNow.map { if (it.taskId == t.taskId) it.copy(completed = true) else it }
                tasksStore.setTasks(
                    date = today,
                    dayTitle = tasksStore.getDayTitle(today),
                    tasks = newList
                )
                plannedTasks = newList
                detailTask = null
            }
        )
    }
}

/* ---------------- Helper UI blocks (kept minimal) ---------------- */

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

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.03f)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("No sessions recorded today", style = MaterialTheme.typography.subtitle1)
            Text(
                "Start a guided plan or quick-add a record to see progress here.",
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
        }
    }
}

/** Three horizontal recommendation cards. */
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
        // NOTE: put weight() here (inside Row scope), not inside SuggestionCard
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
    modifier: Modifier = Modifier,   // <-- add modifier param
    title: String,
    minutes: Int,
    onPick: (title: String, minutes: Int) -> Unit
) {
    Card(
        modifier = modifier            // <-- use the modifier passed from Row scope
            .clickable { onPick(title, minutes) },
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

/** Checkable row for plan tasks; additive (does not replace your original row). */
@Composable
private fun PlannedTaskRowCheckable(
    task: PlanTasksStore.PlanTask,
    onToggle: (Boolean) -> Unit
) {
    Card(
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.03f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.completed, onCheckedChange = onToggle)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.subtitle2)
                task.target?.let { mins ->
                    Text("$mins min", style = MaterialTheme.typography.caption, color = Color.Gray)
                }
            }
        }
    }
}

/** Simple detail dialog for a plan task, includes "Mark as completed". */
@Composable
private fun PlanTaskDetailDialog(
    task: PlanTasksStore.PlanTask,
    onDismiss: () -> Unit,
    onMarkCompleted: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column {
                Text("Details of this exercise plan.", style = MaterialTheme.typography.body2)
                task.target?.let { Text("Target: $it min", style = MaterialTheme.typography.caption) }
                val statusText = if (task.completed) "Already completed" else "Not completed yet"
                Text(statusText, style = MaterialTheme.typography.caption, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = onMarkCompleted) { Text("Mark as completed") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
