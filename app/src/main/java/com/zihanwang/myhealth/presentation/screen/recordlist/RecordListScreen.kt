package com.zihanwang.myhealth.presentation.screen.recordlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.zihanwang.myhealth.R
import com.zihanwang.myhealth.formatDisplayTimeStartEnd
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.UUID
import kotlin.random.Random

/**
 * Record list that inherits the app theme (system-following light/dark).
 *
 * Changes vs original:
 * - Removed the internal HealthConnectTheme wrapper so this screen follows the app theme.
 * - Wrapped content in a Surface with MaterialTheme.colors.background.
 * - Use MaterialTheme color tokens only (no hard-coded colors).
 */
@Composable
fun RecordListScreen(
    uid: String,
    permissions: Set<String>,
    permissionsGranted: Boolean,
    recordType: RecordType,
    seriesRecordsType: SeriesRecordsType,
    recordList: List<Record>,
    uiState: RecordListScreenViewModel.UiState,
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {}
) {
    // Keep track of last error id to avoid re-emitting same snackbar on recompositions
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    LaunchedEffect(uiState) {
        // Trigger initial load once permissions flow completes
        if (uiState is RecordListScreenViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }
        // Emit error once per unique error id
        if (uiState is RecordListScreenViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    if (uiState != RecordListScreenViewModel.UiState.Uninitialized) {
        // IMPORTANT: no nested theme here; inherit the app-level theme.
        Surface(color = MaterialTheme.colors.background) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!permissionsGranted) {
                    item {
                        Button(onClick = { onPermissionsLaunch(permissions) }) {
                            Text(text = stringResource(R.string.permissions_button_label))
                        }
                    }
                } else {
                    // Header: record type + uid
                    item {
                        Text(
                            text = recordType.clazz.simpleName ?: "Record",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.primary,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        Text(
                            text = uid,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                    // Sub-header: series
                    item {
                        Text(
                            text = "${seriesRecordsType.clazz.simpleName} list",
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onBackground
                        )
                    }

                    // Render list items per series type
                    when (seriesRecordsType) {
                        SeriesRecordsType.STEPS -> {
                            for (record in recordList.map { it as StepsRecord }) {
                                renderData(
                                    record,
                                    record.startTime,
                                    record.endTime,
                                    "Count: ${record.count}"
                                )
                            }
                        }
                        SeriesRecordsType.DISTANCE -> {
                            for (record in recordList.map { it as DistanceRecord }) {
                                renderData(
                                    record,
                                    record.startTime,
                                    record.endTime,
                                    "Distance: ${record.distance}"
                                )
                            }
                        }
                        SeriesRecordsType.CALORIES -> {
                            for (record in recordList.map { it as TotalCaloriesBurnedRecord }) {
                                renderData(
                                    record,
                                    record.startTime,
                                    record.endTime,
                                    "Energy: ${record.energy}"
                                )
                            }
                        }
                        SeriesRecordsType.HEARTRATE -> {
                            for (record in recordList.map { it as HeartRateRecord }) {
                                val samples = record.samples.joinToString(", ") { it.beatsPerMinute.toString() }
                                renderData(
                                    record,
                                    record.startTime,
                                    record.endTime,
                                    "Heartbeat Samples: $samples"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Helper used in preview to build fake steps records. */
private fun buildStepsSeries(
    sessionStartTime: ZonedDateTime,
    sessionEndTime: ZonedDateTime
) = StepsRecord(
    metadata = Metadata.manualEntry(),
    startTime = sessionStartTime.toInstant(),
    startZoneOffset = sessionStartTime.offset,
    endTime = sessionEndTime.toInstant(),
    endZoneOffset = sessionEndTime.offset,
    count = Random.nextInt(9000).toLong() + 1000,
)

/**
 * Renders a single list row for a Health Connect Record.
 * Uses theme text styles/colors only.
 */
fun LazyListScope.renderData(
    record: Record,
    startTime: Instant,
    endTime: Instant,
    data: String
) {
    item {
        Text(
            text = record.metadata.id,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
        )
        Text(
            text = formatDisplayTimeStartEnd(startTime, null, endTime, null),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground
        )
        Text(
            text = data,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground
        )
    }
}

/**
 * Preview uses a simple MaterialTheme.
 * In the real app this screen will inherit the app theme from the root.
 */
@Preview(showBackground = true)
@Composable
fun RecordListScreenPreview() {
    MaterialTheme {
        val uid = UUID.randomUUID().toString()
        RecordListScreen(
            uid = uid,
            permissions = emptySet(),
            permissionsGranted = true,
            recordType = RecordType.EXERCISE_SESSION,
            seriesRecordsType = SeriesRecordsType.STEPS,
            recordList = listOf(
                buildStepsSeries(now().minusMinutes(180), now().minusMinutes(120)),
                buildStepsSeries(now().minusMinutes(60), now())
            ),
            uiState = RecordListScreenViewModel.UiState.Done,
        )
    }
}
