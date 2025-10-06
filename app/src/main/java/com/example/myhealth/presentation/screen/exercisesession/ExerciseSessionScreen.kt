package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Rule
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import com.example.myhealth.data.ExerciseSession
import com.example.myhealth.presentation.component.ExerciseSessionRow
import java.util.UUID

/**
 * Root of Exercise feature with a bottom navigation: Plan / Workout / Courses / Stats.
 * Signature matches the original screen so it can be dropped in without touching navigation.
 */
@Composable
fun ExerciseSessionScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    sessionsList: List<ExerciseSession>,
    uiState: ExerciseSessionViewModel.UiState,
    onInsertClick: () -> Unit = {},
    onDetailsClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    // Avoid re-notifying the same error after recomposition
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    LaunchedEffect(uiState) {
        if (uiState is ExerciseSessionViewModel.UiState.Uninitialized) {
            onPermissionsResult() // triggers viewModel.initialLoad()
        }
        if (uiState is ExerciseSessionViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }
    if (uiState == ExerciseSessionViewModel.UiState.Uninitialized) return

    // If runtime permissions are missing, gate the feature up-front
    if (!permissionsGranted) {
        PermissionGate(
            permissions = permissions,
            onPermissionsLaunch = onPermissionsLaunch
        )
        return
    }

    // 4 tabs entry
    val tabs = listOf(
        ExerciseTab.Plan, ExerciseTab.Workout, ExerciseTab.Courses, ExerciseTab.Stats
    )
    var current by rememberSaveable { mutableStateOf(ExerciseTab.Workout) }

    Scaffold(
        bottomBar = {
            BottomNavigation {
                tabs.forEach { tab ->
                    BottomNavigationItem(
                        selected = current == tab,
                        onClick = { current = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        when (current) {
            ExerciseTab.Plan -> ExercisePlanScreen(
                modifier = Modifier.padding(padding),
                onAdjustPlan = { /* handled inside screen via local prefs */ }
            )
            ExerciseTab.Workout -> WorkoutPage(
                modifier = Modifier.padding(padding),
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
                modifier = Modifier.padding(padding)
            )
            ExerciseTab.Stats -> ExerciseStatsScreen(
                modifier = Modifier.padding(padding),
                sessions = sessionsList
            )
        }
    }
}

/** Bottom tabs */
private enum class ExerciseTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Plan("Plan", Icons.Filled.Rule),
    Workout("Workout", Icons.Filled.FitnessCenter),
    Courses("Courses", Icons.Filled.ListAlt),
    Stats("Stats", Icons.Filled.BarChart)
}

/** Permission blocker shown when Health Connect permissions are missing */
@Composable
private fun PermissionGate(
    permissions: Set<String>,
    onPermissionsLaunch: (Set<String>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Exercise requires Health Connect permissions.", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onPermissionsLaunch(permissions) }) {
            Text("Grant permissions")
        }
    }
}

/** Workout page: add sample session, request background read, and show today's list */
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Action row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f).height(48.dp),
                    onClick = onInsertClick
                ) { Text("Add a sample session") }

                // Show background read CTA when available & not yet granted
                AnimatedVisibility(visible = !backgroundReadGranted) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).height(48.dp),
                        onClick = onRequestBgRead,
                        enabled = backgroundReadAvailable
                    ) {
                        Text(if (backgroundReadAvailable) "Enable background read" else "Not available")
                    }
                }
            }
        }

        // Title
        item {
            Text(
                "Today’s sessions",
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 6.dp)
            )
        }

        // Empty state
        if (sessionsList.isEmpty()) {
            item {
                Text(
                    "No sessions yet.",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
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
            }
        }
    }
}
