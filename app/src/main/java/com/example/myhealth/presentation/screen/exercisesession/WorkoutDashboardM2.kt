package com.example.myhealth.presentation.screen.exercisesession

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

/**
 * Workout dashboard (Material 2).
 *
 * - Big number: today's logged sessions (planned + extra)
 * - Planned: count of today's plan tasks from PlanTasksStore
 * - Completed: number of planned tasks marked completed (via CompletedSessionsStore)
 * - Deleting a "Quick Add" session will also consume ONE synthetic plan item -> planned -1
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

    // Stores
    val tasksStore = remember { PlanTasksStore(app) }
    val completedStore = remember { CompletedSessionsStore(app) }

    // Derive today's sessions from incoming list
    var todaySessions by remember { mutableStateOf(sessionsList.filter { it.isSameDate(today) }) }
    LaunchedEffect(sessionsList) {
        todaySessions = sessionsList.filter { it.isSameDate(today) }
    }

    // Planned count (kept as state so we can refresh after add/delete)
    var plannedCount by remember { mutableStateOf(tasksStore.count(today)) }

    // Completed count (recompute whenever the set of today's sessions changes)
    var completedCount by remember { mutableStateOf(0) }
    LaunchedEffect(todaySessions) {
        completedCount = completedStore.countCompleted(today, todaySessions.map { it.id })
    }

    val progress = when (plannedCount) {
        0 -> 0f
        else -> (minOf(completedCount, plannedCount) / plannedCount.toFloat())
    }.coerceIn(0f, 1f)

    Column(modifier.fillMaxSize()) {

        // ---- HERO ----
        HeroSectionM2(
            bigNumber = todaySessions.size,
            planned = plannedCount,
            completed = completedCount,
            progress = progress
        )

        // ---- Permission banner ----
        BackgroundReadRequest(
            backgroundReadAvailable = backgroundReadAvailable,
            backgroundReadGranted = backgroundReadGranted,
            onRequestBgRead = onRequestBgRead
        )

        // ---- Quick Add (each will: add a synthetic plan item, then insert a sample session) ----
        QuickActionsRowM2(
            onQuickAddWalk = {
                tasksStore.addSyntheticTask(today, title = "Walk 30m", target = 30)
                plannedCount = tasksStore.count(today)     // refresh planned immediately
                onInsertClick()                             // your existing insert logic
            },
            onQuickAddRun = {
                tasksStore.addSyntheticTask(today, title = "Run 25m", target = 25)
                plannedCount = tasksStore.count(today)
                onInsertClick()
            },
            onQuickAddStrength = {
                tasksStore.addSyntheticTask(today, title = "Strength 35m", target = 35)
                plannedCount = tasksStore.count(today)
                onInsertClick()
            }
        )

        // ---- Recommendation card (same behavior as Quick Add) ----
        RecommendationCardM2(
            onAdd = {
                tasksStore.addSyntheticTask(today, title = "Easy Walk 30m", target = 30)
                plannedCount = tasksStore.count(today)
                onInsertClick()
            }
        )

        // ---- Title ----
        Text(
            if (plannedCount > 0) "Today's sessions ($plannedCount planned)" else "Today's sessions",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            style = MaterialTheme.typography.subtitle1
        )

        // ---- List ----
        if (todaySessions.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
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
                            // 1) Consume ONE synthetic plan item for today (if any)
                            val consumed = tasksStore.consumeOneSyntheticTask(today)
                            if (consumed) {
                                plannedCount = tasksStore.count(today) // refresh planned
                            }
                            // 2) Proceed with actual session deletion (your ViewModel / DB)
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
    bigNumber: Int,   // logged sessions today (planned + extras)
    planned: Int,     // planned tasks for today
    completed: Int,   // completed planned tasks
    progress: Float   // completed / planned
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
            // Circular indicator with big number
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
                    text = when (planned) {
                        0 -> "No plan today"
                        else -> "${(progress * 100).toInt()}% of today's plan"
                    },
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
        Text(label, modifier = Modifier.width(82.dp), style = MaterialTheme.typography.caption)
        Text(value, style = MaterialTheme.typography.subtitle1)
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

/* ---------------- Helpers ---------------- */

/** True if the session belongs to the given calendar date (local timezone). */
private fun ExerciseSession.isSameDate(date: LocalDate): Boolean =
    runCatching { this.startTime.toLocalDate() == date }.getOrDefault(false)
