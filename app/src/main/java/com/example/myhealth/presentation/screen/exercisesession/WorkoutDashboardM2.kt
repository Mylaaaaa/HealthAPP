@file:OptIn(ExperimentalMaterialApi::class)

package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ---- Your domain model ----
// Adjust field names here if your ExerciseSession is slightly different.
data class ExerciseSession(
    val id: String,
    val type: String,
    val minutes: Int,
    val calories: Int,
    val intensity: String,
    val startTime: String
)

/**
 * Dashboard-styled Workout page (Material 2),
 * drop-in for your existing WorkoutPage body.
 *
 * Keep your WorkoutPage(...) signature unchanged in ExerciseSessionScreen.kt,
 * and call this inside it.
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
    // Local UI state (goal can come from ViewModel later)
    var dailyGoal by remember { mutableStateOf(45) }

    val done = sessionsList.sumOf { it.minutes }
    val progress = (done / dailyGoal.toFloat()).coerceIn(0f, 1f)

    // Add-session sheet control (uses Material 2 BottomSheet)
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    var showSheet by remember { mutableStateOf(false) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetElevation = 8.dp,
        sheetContent = {
            AddSessionSheetM2(
                onAdd = { minutes, type, intensity ->
                    // Delegate to your add flow
                    onInsertClick()
                    // After the insert flow returns (e.g. via ViewModel), the list will refresh upstream
                    showSheet = false
                },
                onClose = { showSheet = false }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Workout · Today", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    actions = {
                        IconButton(onClick = { /* open settings for goals, etc. */ }) {
                            Icon(Icons.Outlined.Settings, contentDescription = null)
                        }
                        IconButton(onClick = { /* overflow */ }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = null)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showSheet = true }) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null)
                }
            }
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // HERO / summary
                HeroSectionM2(
                    progress = progress,
                    done = done,
                    goal = dailyGoal,
                    calories = sessionsList.sumOf { it.calories },
                    streak = 4 // demo value; wire to your real streak later
                )

                // Background read permissions hint (optional)
                if (backgroundReadAvailable && !backgroundReadGranted) {
                    PermissionBannerM2(onRequest = onRequestBgRead)
                }

                // Quick actions (templates)
                QuickActionsRowM2(
                    onQuickAdd = { template ->
                        // If you have a specific insert-with-template flow, call it here.
                        // Otherwise, just open the sheet for manual add:
                        showSheet = true
                    }
                )

                // Recommendation card
                RecommendationCardM2(onAdd = {
                    // One-tap recommended add. Plug into your insert logic:
                    onInsertClick()
                })

                // List or empty state
                if (sessionsList.isEmpty()) {
                    EmptyStateM2(
                        onQuickStart = { onInsertClick() },
                        onBrowseTemplates = { showSheet = true }
                    )
                } else {
                    SessionListM2(
                        sessions = sessionsList,
                        onDelete = onDeleteClick,
                        onEdit = onDetailsClick
                    )
                }
            }
        }

        // Presentation of sheet
        LaunchedEffect(showSheet) {
            if (showSheet) sheetState.show() else sheetState.hide()
        }
    }
}

// ---- UI Parts (Material 2) ----

@Composable
private fun HeroSectionM2(
    progress: Float,
    done: Int,
    goal: Int,
    calories: Int,
    streak: Int
) {
    Card(Modifier.fillMaxWidth().padding(16.dp), elevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress + number
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = 1f,
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
                )
                CircularProgressIndicator(progress = progress, strokeWidth = 10.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$done/$goal", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("min", style = MaterialTheme.typography.caption)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                MetricRowM2("Streak", "$streak days")
                MetricRowM2("Calories", "$calories kcal")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                )
                Text(
                    text = "${(progress * 100).roundToInt()}% of daily goal",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable private fun MetricRowM2(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.caption, modifier = Modifier.width(72.dp))
        Text(value, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PermissionBannerM2(onRequest: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.08f),
        elevation = 0.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Background read not granted", style = MaterialTheme.typography.subtitle2)
                Text("Allow background read to show auto-logged sessions.", style = MaterialTheme.typography.caption)
            }
            OutlinedButton(onClick = onRequest) { Text("Allow") }
        }
    }
}

@Composable
private fun QuickActionsRowM2(onQuickAdd: (Template) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionChipM2("Walk 30m") { onQuickAdd(Template.Walk30) }
        ActionChipM2("Run 25m") { onQuickAdd(Template.Run25) }
        ActionChipM2("Strength 35m") { onQuickAdd(Template.Strength35) }
        ActionChipM2("Yoga 20m") { onQuickAdd(Template.Yoga20) }
    }
}

@Composable
private fun ActionChipM2(text: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        border = ButtonDefaults.outlinedBorder,
        elevation = 0.dp,
        onClick = onClick
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.body2)
    }
}

@Composable
private fun RecommendationCardM2(onAdd: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Based on last week", style = MaterialTheme.typography.caption)
                Text("Recommended: Easy Walk · 30 minutes", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Text("Gentle cardio to maintain your streak.", style = MaterialTheme.typography.body2)
            }
            Button(onClick = onAdd) { Text("Add") }
        }
    }
}

@Composable
private fun SessionListM2(
    sessions: List<ExerciseSession>,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        item {
            Surface(elevation = 1.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Today", style = MaterialTheme.typography.subtitle1)
                }
            }
        }
        items(items = sessions, key = { it.id }) { s ->
            val dismissState = rememberDismissState(
                confirmStateChange = { value ->
                    if (value == DismissValue.DismissedToEnd || value == DismissValue.DismissedToStart) {
                        onDelete(s.id); true
                    } else false
                }
            )
            SwipeToDismiss(
                state = dismissState,
                directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
                background = { DismissBackgroundM2(dismissState) },
                dismissContent = { SessionCardM2(s, onEdit = { onEdit(s.id) }) }
            )
        }
    }
}

@Composable
private fun DismissBackgroundM2(state: DismissState) {
    val bg = if (state.targetValue != DismissValue.Default)
        MaterialTheme.colors.error.copy(alpha = 0.15f)
    else
        MaterialTheme.colors.onSurface.copy(alpha = 0.04f)

    Box(
        Modifier.fillMaxWidth().height(84.dp).background(bg).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = null)
    }
}

@Composable
private fun SessionCardM2(session: ExerciseSession, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), elevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${session.type} · ${session.minutes} min", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Text("${session.startTime}  ·  ${session.calories} kcal  ·  ${session.intensity}", style = MaterialTheme.typography.caption)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = null) }
        }
    }
}

@Composable
private fun EmptyStateM2(
    onQuickStart: () -> Unit,
    onBrowseTemplates: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No sessions yet", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Quick start with a 20-minute walk or browse templates.", style = MaterialTheme.typography.body2)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onQuickStart) { Text("Quick Start: Walk 20m") }
        TextButton(onClick = onBrowseTemplates) { Text("Browse templates") }
    }
}

// ---- Bottom sheet (M2) ----

@Composable
private fun AddSessionSheetM2(
    onAdd: (minutes: Int, type: String, intensity: String) -> Unit,
    onClose: () -> Unit
) {
    var minutes by remember { mutableStateOf(30) }
    var type by remember { mutableStateOf("Walk") }
    var intensity by remember { mutableStateOf("Moderate") }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("New session", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(12.dp))

        // Type row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Walk", "Run", "Strength", "Yoga").forEach { t ->
                OutlinedButton(onClick = { type = t }) { Text(t) }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Intensity row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Easy", "Moderate", "Hard").forEach { i ->
                OutlinedButton(onClick = { intensity = i }) { Text(i) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Minutes", modifier = Modifier.width(80.dp))
            OutlinedButton(onClick = { minutes = (minutes - 5).coerceAtLeast(5) }) { Text("-") }
            Text("  $minutes  ", fontSize = 18.sp)
            OutlinedButton(onClick = { minutes += 5 }) { Text("+") }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onAdd(minutes, type, intensity) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add session") }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

// ---- Simple template enum (optional) ----
private enum class Template { Walk30, Run25, Strength35, Yoga20 }
