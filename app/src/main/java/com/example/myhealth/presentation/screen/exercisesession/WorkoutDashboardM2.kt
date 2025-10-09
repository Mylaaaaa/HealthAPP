// app/src/main/java/com/example/myhealth/presentation/screen/exercisesession/WorkoutDashboardM2.kt
package com.example.myhealth.presentation.screen.exercisesession

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myhealth.data.ExerciseSession
import com.example.myhealth.presentation.component.ExerciseSessionRow
import com.example.myhealth.presentation.screen.exercisesession.planaccess.CompletedSessionsStore
import com.example.myhealth.presentation.screen.exercisesession.planaccess.PlanTasksStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Workout dashboard (Material 2).
 *
 * PLAN-DRIVEN:
 *  - Big number "sessions" == number of planned tasks for today.
 *  - "Planned" from PlanTasksStore; "Completed" from task.completed flag.
 *  - Listen for SharedPreferences changes so updates from Plan page reflect instantly.
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
    val ctx = LocalContext.current
    val app = ctx.applicationContext
    val today = remember { LocalDate.now() }

    val tasksStore = remember { PlanTasksStore(app) }
    val completedStore = remember { CompletedSessionsStore(app) } // kept for details page if used

    var plannedTasks by remember { mutableStateOf(tasksStore.getTasks(today)) }

    val plannedCount = plannedTasks.size
    val completedCount = plannedTasks.count { it.completed }
    val progress = if (plannedCount == 0) 0f else (completedCount.toFloat() / plannedCount)

    val todaySessions by remember(sessionsList, today) {
        mutableStateOf(sessionsList.filter { it.startTime.toLocalDate() == today })
    }

    val todayKey = remember(today) { "plan_tasks_${DateTimeFormatter.ISO_LOCAL_DATE.format(today)}" }
    DisposableEffect(todayKey) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == todayKey) {
                plannedTasks = tasksStore.getTasks(today)
            }
        }
        tasksStore.addOnChangeListener(listener)
        onDispose { tasksStore.removeOnChangeListener(listener) }
    }

    var quickAdds by rememberSaveable { mutableStateOf(0) }
    fun quickAdd(title: String, minutes: Int?) {
        tasksStore.addSyntheticTask(today, title = title, target = minutes)
        plannedTasks = tasksStore.getTasks(today)
        quickAdds++
        onInsertClick()
    }

    Column(modifier.fillMaxSize()) {
        HeroSectionM2(
            bigNumber = plannedCount,
            planned = plannedCount,
            completed = completedCount,
            progress = progress
        )

        BackgroundReadRequest(
            backgroundReadAvailable = backgroundReadAvailable,
            backgroundReadGranted = backgroundReadGranted,
            onRequestBgRead = onRequestBgRead
        )

        QuickActionsRowM2(
            onQuickAddWalk = { quickAdd("Walk 30m", 30) },
            onQuickAddRun = { quickAdd("Run 25m", 25) },
            onQuickAddStrength = { quickAdd("Strength 35m", 35) }
        )
        RecommendationCardM2(onAdd = { quickAdd("Easy Walk 30m", 30) })

        Text(
            text = if (plannedCount > 0) "Today's sessions ($plannedCount planned)" else "Today's sessions",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            style = MaterialTheme.typography.subtitle1
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (plannedTasks.isNotEmpty()) {
                item {
                    Text(
                        "Today's plan",
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                    )
                }
                items(plannedTasks, key = { it.taskId }) { p ->
                    PlannedTaskRow(title = p.title, minutes = p.target)
                    Spacer(Modifier.height(8.dp))
                }
                item { Divider(modifier = Modifier.alpha(0.08f)) }
                item { Spacer(Modifier.height(8.dp)) }
            }

            if (todaySessions.isEmpty()) {
                item { EmptyState() }
            } else {
                item {
                    Text(
                        "Logged today",
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                    )
                }
                items(todaySessions, key = { it.id }) { s ->
                    val appInfo = s.sourceAppInfo
                    ExerciseSessionRow(
                        start = s.startTime,
                        end = s.endTime,
                        uid = s.id,
                        name = s.title ?: "No title",
                        sourceAppName = appInfo?.appLabel ?: "Unknown app",
                        sourceAppIcon = appInfo?.icon,
                        onDeleteClick = { uid ->
                            val consumed = tasksStore.consumeOneSyntheticTask(today)
                            if (consumed) plannedTasks = tasksStore.getTasks(today)
                            onDeleteClick(uid)
                        },
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

/* ====================== Sub-components ====================== */

@Composable
private fun HeroSectionM2(
    bigNumber: Int,
    planned: Int,
    completed: Int,
    progress: Float
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
            Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = 1f,
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
                )
                CircularProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    strokeWidth = 10.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$bigNumber", style = MaterialTheme.typography.h6)
                    Text("sessions", style = MaterialTheme.typography.caption)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                MetricRowM2(label = "Planned", value = "$planned")
                MetricRowM2(label = "Completed", value = "$completed")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.95f)
                )
                Text(
                    text = if (planned == 0) "No plan today" else "${(progress * 100).toInt()}% of today's plan",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable private fun MetricRowM2(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(82.dp), style = MaterialTheme.typography.caption)
        Text(value, style = MaterialTheme.typography.subtitle1)
    }
}

@Composable
private fun PlannedTaskRow(title: String, minutes: Int?) {
    Card(elevation = 0.dp, backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.02f)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
                tint = MaterialTheme.colors.primary.copy(alpha = 0.75f)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.subtitle2)
                if (minutes != null) {
                    Text("$minutes min", style = MaterialTheme.typography.caption, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRowM2(
    onQuickAddWalk: () -> Unit,
    onQuickAddRun: () -> Unit,
    onQuickAddStrength: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onQuickAddWalk) { Text("Add walk 30m") }
        OutlinedButton(onClick = onQuickAddRun) { Text("Add run 25m") }
        OutlinedButton(onClick = onQuickAddStrength) { Text("Add strength 35m") }
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
                Text("Recommended: Easy Walk · 30 minutes", style = MaterialTheme.typography.subtitle1)
                Text("Gentle cardio to maintain your streak.", style = MaterialTheme.typography.body2)
            }
            Button(onClick = onAdd) { Text("Add") }
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
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = Color.Gray)
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
