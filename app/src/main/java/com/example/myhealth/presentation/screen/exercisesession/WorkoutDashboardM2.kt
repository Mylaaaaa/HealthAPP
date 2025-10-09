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
import com.example.myhealth.presentation.screen.exercisesession.planaccess.PlanTasksStore
import java.time.LocalDate

/**
 * Workout dashboard (Material 2)
 *
 * Rules:
 * - Big number in the ring = today's logged sessions (planned + extras)
 * - Progress bar           = completed planned tasks / planned tasks (planned-only progress)
 * - The list shows only today's sessions
 *
 * NOTE: start/end are ZonedDateTime in your model and are passed as ZonedDateTime
 *       to ExerciseSessionRow to match its signature. No Long/epoch conversion here.
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
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    // 1) Read planned tasks for today (written by Plan screen)
    val tasksStore = remember { PlanTasksStore(context.applicationContext) }
    val todayTasks by remember { mutableStateOf(tasksStore.getTasks(today)) }
    val plannedCount = todayTasks.size

    // 2) How many planned are completed:
    //    If today's Plan title is marked done in PlanProgressStore -> all planned considered done.
    val progressStore = remember { PlanProgressStore(context.applicationContext) }
    val todayPlanTitle = remember { tasksStore.getDayTitle(today) }
    val completedPlannedCount = remember(plannedCount, todayPlanTitle) {
        if (plannedCount == 0) 0
        else if (todayPlanTitle != null && progressStore.isDone(today, todayPlanTitle)) plannedCount
        else 0
    }

    // 3) Today's sessions (planned + extras). Keep ZonedDateTime end-to-end.
    val todaySessions = remember(sessionsList) { sessionsList.filter { it.isSameDate(today) } }
    val loggedCount = todaySessions.size

    // 4) Planned progress (planned-only)
    val progress = if (plannedCount == 0) 0f else (completedPlannedCount / plannedCount.toFloat())

    Column(modifier.fillMaxSize()) {

        // --- HERO: big number = loggedCount; progress = completed/planned ---
        HeroSectionM2(
            bigNumber = loggedCount,
            planned = plannedCount,
            completed = completedPlannedCount,
            progress = progress
        )

        // --- Background read permission banner (unchanged) ---
        BackgroundReadRequest(
            backgroundReadAvailable = backgroundReadAvailable,
            backgroundReadGranted = backgroundReadGranted,
            onRequestBgRead = onRequestBgRead
        )

        // --- Quick actions (extras only; do not affect progress) ---
        QuickActionsRowM2(onQuickAdd = onInsertClick)

        // --- Recommendation card (unchanged) ---
        RecommendationCardM2(onAdd = onInsertClick)

        // --- Title ---
        Text(
            if (plannedCount > 0) "Today's sessions ($plannedCount planned)" else "Today's sessions",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            style = MaterialTheme.typography.subtitle1
        )

        // --- List: only today's sessions (ZonedDateTime → ZonedDateTime) ---
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
                        start = s.startTime,    // ZonedDateTime expected by your row
                        end = s.endTime,        // ZonedDateTime expected by your row
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

/* ====================== Sub-components (styling unchanged) ====================== */

@Composable
private fun HeroSectionM2(
    bigNumber: Int,   // today's logged sessions (planned + extras)
    planned: Int,     // number of planned tasks today
    completed: Int,   // number of completed planned tasks
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
            // Circular big number
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
            // Use AutoMirrored icon to avoid the deprecation warning.
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

/* ====================== Helpers ====================== */

/** True if the session belongs to the given calendar date (local timezone). */
private fun ExerciseSession.isSameDate(date: LocalDate): Boolean {
    return try {
        // startTime is ZonedDateTime in your model
        this.startTime.toLocalDate() == date
    } catch (_: Throwable) {
        false
    }
}
