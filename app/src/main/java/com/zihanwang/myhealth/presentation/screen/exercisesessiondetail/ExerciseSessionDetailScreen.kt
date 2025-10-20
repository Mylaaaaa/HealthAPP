package com.zihanwang.myhealth.presentation.screen.exercisesessiondetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import com.zihanwang.myhealth.data.ExerciseSessionData
import com.zihanwang.myhealth.data.formatTime
import com.zihanwang.myhealth.presentation.component.ExerciseSessionDetailsMinMaxAvg
import com.zihanwang.myhealth.presentation.screen.recordlist.RecordType
import com.zihanwang.myhealth.presentation.screen.recordlist.SeriesRecordsType
import com.zihanwang.myhealth.presentation.theme.HealthConnectTheme
import com.zihanwang.myhealth.presentation.screen.exercisesession.planaccess.CompletedSessionsStore
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import com.zihanwang.myhealth.R
import androidx.compose.ui.res.stringResource

@Composable
fun ExerciseSessionDetailScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    sessionMetrics: ExerciseSessionData,
    uiState: ExerciseSessionDetailViewModel.UiState,
    onDetailsClick: (String, String, String) -> Unit = { _, _, _ -> },
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {}
) {
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    LaunchedEffect(uiState) {
        if (uiState is ExerciseSessionDetailViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }
        if (uiState is ExerciseSessionDetailViewModel.UiState.Error &&
            errorId.value != uiState.uuid
        ) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    if (uiState != ExerciseSessionDetailViewModel.UiState.Uninitialized) {
        val context = LocalContext.current
        val today = remember { LocalDate.now() }
        val completedStore = remember { CompletedSessionsStore(context.applicationContext) }

        // Persisted completion flag for this session id (scoped to "today" like before)
        var isCompleted by rememberSaveable(sessionMetrics.uid) {
            mutableStateOf(completedStore.isCompleted(today, sessionMetrics.uid))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!permissionsGranted) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onPermissionsLaunch(permissions) }) {
                        Text(text = stringResource(R.string.permissions_button_label))
                    }
                }
            } else {
                // ---------- Header ----------
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.exercise_session_detail),
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.primary,
                    )
                    Text(
                        text = "id: ${sessionMetrics.uid}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // ---------- Modern "Complete" button (replaces checkbox) ----------
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                val newValue = !isCompleted
                                isCompleted = newValue
                                // Keep using your store so dashboard progress stays in sync
                                completedStore.setCompleted(today, sessionMetrics.uid, newValue)
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isCompleted)
                                    Color(0xFF4CAF50) /* green */
                                else
                                    MaterialTheme.colors.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(48.dp)
                        ) {
                            Text(
                                if (isCompleted) "Completed ✓" else "Mark as Completed",
                                color = Color.White,
                                style = MaterialTheme.typography.button
                            )
                        }
                    }
                    Divider()
                    Spacer(Modifier.height(8.dp))
                }

                // ---------- Duration ----------
                item {
                    SessionBlock(label = stringResource(R.string.total_active_duration)) {
                        val active = sessionMetrics.totalActiveTime ?: Duration.ZERO
                        Text(active.formatTime(), style = MaterialTheme.typography.h6)
                    }
                }

                // ---------- Steps ----------
                item {
                    SessionBlock(label = stringResource(R.string.total_steps)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                sessionMetrics.totalSteps?.toString() ?: "0",
                                style = MaterialTheme.typography.h6
                            )
                            RecordsIconButton(sessionMetrics.uid, SeriesRecordsType.STEPS, onDetailsClick)
                        }
                    }
                }

                // ---------- Distance ----------
                item {
                    SessionBlock(label = stringResource(R.string.total_distance)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                sessionMetrics.totalDistance?.toString() ?: "0.0",
                                style = MaterialTheme.typography.h6
                            )
                            RecordsIconButton(sessionMetrics.uid, SeriesRecordsType.DISTANCE, onDetailsClick)
                        }
                    }
                }

                // ---------- Calories ----------
                item {
                    SessionBlock(label = stringResource(R.string.total_energy)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                sessionMetrics.totalEnergyBurned?.inCalories.toString(),
                                style = MaterialTheme.typography.h6
                            )
                            RecordsIconButton(sessionMetrics.uid, SeriesRecordsType.CALORIES, onDetailsClick)
                        }
                    }
                }

                // ---------- Heart rate ----------
                item {
                    SessionBlock(label = stringResource(R.string.hr_stats)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExerciseSessionDetailsMinMaxAvg(
                                sessionMetrics.minHeartRate?.toString()
                                    ?: stringResource(id = R.string.not_available_abbrev),
                                sessionMetrics.maxHeartRate?.toString()
                                    ?: stringResource(id = R.string.not_available_abbrev),
                                sessionMetrics.avgHeartRate?.toString()
                                    ?: stringResource(id = R.string.not_available_abbrev)
                            )
                            RecordsIconButton(sessionMetrics.uid, SeriesRecordsType.HEARTRATE, onDetailsClick)
                        }
                    }
                }
            }
        }
    }
}

/** Small block with label, content and divider. */
@Composable
private fun SessionBlock(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        content()
        Divider()
        Spacer(Modifier.height(12.dp))
    }
}

/** Chevron button that navigates to the time-series details. */
@Composable
fun RecordsIconButton(
    uid: String,
    seriesRecordsType: SeriesRecordsType,
    onDetailsClick: (String, String, String) -> Unit = { _, _, _ -> }
) {
    IconButton(
        onClick = {
            onDetailsClick(
                RecordType.EXERCISE_SESSION.toString(),
                uid,
                seriesRecordsType.toString()
            )
        },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.details_button)
        )
    }
}

@Preview
@Composable
fun ExerciseSessionDetailPreview() {
    HealthConnectTheme {
        val uid = UUID.randomUUID().toString()
        val sample = ExerciseSessionData(
            uid = uid,
            totalActiveTime = Duration.ofMinutes(75),
            totalSteps = 5152,
            totalDistance = Length.meters(11923.4),
            totalEnergyBurned = Energy.calories(1131.2),
            minHeartRate = 55,
            maxHeartRate = 103,
            avgHeartRate = 77,
        )
        ExerciseSessionDetailScreen(
            permissions = setOf(),
            permissionsGranted = true,
            sessionMetrics = sample,
            uiState = ExerciseSessionDetailViewModel.UiState.Done
        )
    }
}
