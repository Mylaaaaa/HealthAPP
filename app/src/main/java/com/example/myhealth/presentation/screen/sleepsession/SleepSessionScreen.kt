package com.example.myhealth.presentation.screen.sleepsession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.SleepSessionRecord
import com.example.myhealth.R
import com.example.myhealth.data.SleepSessionData
import com.example.myhealth.presentation.theme.HealthConnectTheme
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun SleepSessionScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    sessionsList: List<SleepSessionData>,
    uiState: SleepSessionViewModel.UiState,
    onInsertClick: () -> Unit = {},
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {}
) {
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    // Handle initialization and error updates
    LaunchedEffect(uiState) {
        if (uiState is SleepSessionViewModel.UiState.Uninitialized) onPermissionsResult()
        if (uiState is SleepSessionViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    if (uiState != SleepSessionViewModel.UiState.Uninitialized) {
        val expandedUid = rememberSaveable { mutableStateOf<String?>(null) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!permissionsGranted) {
                item {
                    Button(onClick = { onPermissionsLaunch(permissions) }) {
                        Text(text = stringResource(R.string.permissions_button_label))
                    }
                }
            } else {
                item {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(8.dp),
                        onClick = onInsertClick
                    ) {
                        Text(stringResource(id = R.string.generate_sleep_data))
                    }
                }

                items(sessionsList, key = { it.uid }) { session ->
                    SleepSessionItem(
                        session = session,
                        expanded = expandedUid.value == session.uid,
                        onToggle = {
                            expandedUid.value =
                                if (expandedUid.value == session.uid) null else session.uid
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepSessionItem(
    session: SleepSessionData,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onToggle() },
        elevation = 3.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = Color(0xFF00796B),
                    modifier = Modifier.size(40.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = formatDuration(session.duration),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Display time in local timezone
                    val startLocal = session.startTime
                        .atZone(ZoneId.systemDefault())
                        .format(timeFmt)
                    val endLocal = session.endTime
                        .atZone(ZoneId.systemDefault())
                        .format(timeFmt)
                    Text(
                        text = "$startLocal - $endLocal",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Divider()
                    Spacer(Modifier.height(8.dp))
                    Text("Notes: ${session.notes ?: "No notes"}", fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Sleep stages:", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))

                    session.stages.take(8).forEach { s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val localTime = s.startTime
                                .atZone(ZoneId.systemDefault())
                                .format(timeFmt)
                            Text(text = localTime, fontSize = 13.sp)

                            Surface(
                                color = stageColor(s.stage),
                                shape = RoundedCornerShape(10.dp),
                                elevation = 0.dp
                            ) {
                                Text(
                                    text = stageLabel(s.stage),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(duration: Duration?): String {
    val d = duration ?: Duration.ZERO
    val hours = d.toHours()
    val minutes = d.toMinutesPart()
    return "${hours}h${minutes}m"
}

private fun stageLabel(stage: Int): String = when (stage) {
    SleepSessionRecord.STAGE_TYPE_AWAKE -> "Awake"
    SleepSessionRecord.STAGE_TYPE_LIGHT -> "Light"
    SleepSessionRecord.STAGE_TYPE_DEEP  -> "Deep"
    SleepSessionRecord.STAGE_TYPE_REM   -> "REM"
    else -> "Unknown"
}

@Composable
private fun stageColor(stage: Int) = when (stage) {
    SleepSessionRecord.STAGE_TYPE_AWAKE -> MaterialTheme.colors.secondary
    SleepSessionRecord.STAGE_TYPE_LIGHT -> MaterialTheme.colors.primary.copy(alpha = 0.65f)
    SleepSessionRecord.STAGE_TYPE_DEEP  -> MaterialTheme.colors.primary
    SleepSessionRecord.STAGE_TYPE_REM   -> MaterialTheme.colors.error.copy(alpha = 0.85f)
    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
}

private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Preview
@Composable
fun SleepSessionScreenPreview() {
    HealthConnectTheme {
        val end2 = ZonedDateTime.now()
        val start2 = end2.minusHours(5)
        val end1 = end2.minusDays(1)
        val start1 = end1.minusHours(5)
        SleepSessionScreen(
            permissions = setOf(),
            permissionsGranted = true,
            sessionsList = listOf(
                SleepSessionData(
                    uid = "123",
                    title = "My sleep",
                    notes = "Slept well",
                    startTime = start1.toInstant(),
                    startZoneOffset = start1.offset,
                    endTime = end1.toInstant(),
                    endZoneOffset = end1.offset,
                    duration = Duration.between(start1, end1),
                    stages = listOf(
                        SleepSessionRecord.Stage(
                            stage = SleepSessionRecord.STAGE_TYPE_DEEP,
                            startTime = start1.toInstant(),
                            endTime = end1.toInstant()
                        )
                    )
                ),
                SleepSessionData(
                    uid = "124",
                    title = "My sleep",
                    notes = "Slept great",
                    startTime = start2.toInstant(),
                    startZoneOffset = start2.offset,
                    endTime = end2.toInstant(),
                    endZoneOffset = end2.offset,
                    duration = Duration.between(start2, end2),
                    stages = listOf(
                        SleepSessionRecord.Stage(
                            stage = SleepSessionRecord.STAGE_TYPE_REM,
                            startTime = start2.toInstant(),
                            endTime = end2.toInstant()
                        )
                    )
                )
            ),
            uiState = SleepSessionViewModel.UiState.Done
        )
    }
}
