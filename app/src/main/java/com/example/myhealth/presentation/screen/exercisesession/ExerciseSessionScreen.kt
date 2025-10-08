package com.example.myhealth.presentation.screen.exercisesession

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myhealth.data.ExerciseSession
import com.example.myhealth.presentation.component.ExerciseSessionRow
import androidx.health.connect.client.permission.HealthPermission

/**
 * Exercise hub with four tabs:
 * - Plan: goal/profile + generated weekly plan
 * - Workout: today's sessions (insert / details / delete)
 * - Courses: curated routines
 * - Stats: weekly summary
 *
 * NOTE:
 * The outer screen (host) already provides a TopAppBar.
 * This file intentionally does NOT add another TopAppBar to avoid a double header.
 */
@Composable
fun ExerciseSessionScreen(
    modifier: Modifier = Modifier,

    // ---- Backward-compat names (safe defaults) ----
    permissions: Set<String> = emptySet(),
    permissionsGranted: Boolean = false,
    uiState: Any? = null,
    onError: (Throwable) -> Unit = {},
    onPermissionsResult: (Boolean) -> Unit = {},

    // ---- New names actual UI uses ----
    sessionsList: List<ExerciseSession> = emptyList(),
    backgroundReadAvailable: Boolean = permissions.isNotEmpty(),
    backgroundReadGranted: Boolean = permissionsGranted,
    onPermissionsLaunch: (Set<String>) -> Unit = {},

    onInsertClick: () -> Unit = {},
    onDetailsClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(ExerciseTab.Workout) }

    Scaffold { padding ->
        Column(Modifier.padding(padding)) {

            // Tabs
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ExerciseTab.values().forEachIndexed { index, tab ->
                    Tab(
                        selected = index == selectedTab.ordinal,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) }
                    )
                }
            }

            // Pages
            when (selectedTab) {
                ExerciseTab.Plan -> ExercisePlanScreen(
                    modifier = Modifier.padding(16.dp),
                    onStartDay = {
                        // For now: simply switch to Workout tab.
                        // Later you can put the guided execution into a shared ViewModel here.
                        selectedTab = ExerciseTab.Workout
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
}

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
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
        modifier = modifier
    )
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
                    Text("Allow background read", fontWeight = FontWeight.SemiBold)
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

        SectionHeader("Today's sessions", Modifier.fillMaxWidth().padding(vertical = 6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 4.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (sessionsList.isEmpty()) {
                item { EmptyState() }
            } else {
                items(sessionsList, key = { it.id }) { s ->
                    val appInfo = s.sourceAppInfo
                    com.example.myhealth.presentation.component.ExerciseSessionRow(
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
private fun EmptyState() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.03f))
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
