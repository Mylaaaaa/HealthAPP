@file:Suppress("unused")

package com.example.myhealth.presentation.screen.sleepsession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.SleepSessionRecord
import com.example.myhealth.data.SleepSessionData
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/* =========================================================================================
 * Sleep sessions (Material 2) with a blue bottom bar (icons + labels).
 * - BottomNavigation (blue background = theme primary, white content)
 * - 3 tabs: Overview / Log / Stats
 * - No "Generate sleep data" button on the UI (kept parameter, not shown)
 * - Uses only sessionsList provided from ViewModel
 * ========================================================================================= */

@Composable
fun SleepSessionScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    sessionsList: List<SleepSessionData>,
    uiState: SleepSessionViewModel.UiState,
    onInsertClick: () -> Unit,                // kept for compatibility, but NOT shown
    onError: (Throwable?) -> Unit,
    onPermissionsResult: () -> Unit,
    onPermissionsLaunch: (Set<String>) -> Unit
) {
    // Error one-shot handler (same logic as before)
    val lastErrorId = remember { mutableStateOf(UUID.randomUUID()) }
    LaunchedEffect(uiState) {
        if (uiState is SleepSessionViewModel.UiState.Uninitialized) onPermissionsResult()
        if (uiState is SleepSessionViewModel.UiState.Error && lastErrorId.value != uiState.uuid) {
            onError(uiState.exception)
            lastErrorId.value = uiState.uuid
        }
    }

    // Bottom bar state
    var selected by remember { mutableStateOf(0) }
    val tabs = listOf(
        NavItem("Overview", Icons.Filled.Home),
        NavItem("Log", Icons.Filled.List),
        NavItem("Stats", Icons.Filled.BarChart)
    )

    Scaffold(
        bottomBar = {
            BottomNavigation(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White,
                elevation = 10.dp
            ) {
                tabs.forEachIndexed { index, item ->
                    BottomNavigationItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(item.icon, contentDescription = item.label, tint = Color.White) },
                        label = { Text(item.label, color = Color.White) },
                        selectedContentColor = Color.White,
                        unselectedContentColor = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    ) { inner ->
        // Main content
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // (Optional) permission button hidden by requirement; if you want it back, uncomment:
            // if (!permissionsGranted) {
            //     OutlinedButton(
            //         onClick = { onPermissionsLaunch(permissions) },
            //         modifier = Modifier.fillMaxWidth().height(44.dp)
            //     ) { Text("Grant permissions") }
            //     Spacer(Modifier.height(8.dp))
            // }

            when (selected) {
                0 -> SleepOverviewTab(sessionsList)
                1 -> SleepLogTab(sessionsList)
                2 -> SleepStatsTab(sessionsList)
            }
        }
    }
}

/* --------------------------------- Overview ---------------------------------- */

@Composable
private fun SleepOverviewTab(sessions: List<SleepSessionData>) {
    val today = sessions.maxByOrNull { it.endTime } // simple "latest" as today summary
    val totalWeekHours = sessions
        .take(7)
        .sumOf { (it.duration ?: Duration.ZERO).toHours().toInt().coerceAtLeast(0) }

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Today", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            if (today != null) {
                val d = today.duration ?: Duration.ZERO
                Text("${formatHhMm(d)}  •  Stages: ${today.stages.size}")
            } else {
                Text("No sleep recorded")
            }
            Spacer(Modifier.height(10.dp))
            Text("Weekly summary", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("$totalWeekHours h in last 7 sessions")
        }
    }
}

/* ----------------------------------- Log ------------------------------------- */

@Composable
private fun SleepLogTab(sessions: List<SleepSessionData>) {
    val fmtTime = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(sessions, key = { it.uid }) { s ->
            var expanded by remember { mutableStateOf(false) }

            Card(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = formatHhMm(s.duration ?: Duration.ZERO),
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold
                            )
                            val startLocal = s.startTime.atZone(ZoneId.systemDefault()).format(fmtTime)
                            val endLocal = s.endTime.atZone(ZoneId.systemDefault()).format(fmtTime)
                            Text("$startLocal – $endLocal", color = Color.Gray)
                        }
                        Text(
                            if (expanded) "Hide" else "Details",
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            Divider()
                            Spacer(Modifier.height(8.dp))
                            Text("Notes: ${s.notes ?: "—"}")
                            Spacer(Modifier.height(6.dp))
                            Text("Stages", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            // Simple stage bar without Canvas to avoid theme color issues
                            StageTimelineBarSimple(s.stages, height = 10.dp)
                        }
                    }
                }
            }
        }
    }
}

/* ----------------------------------- Stats ----------------------------------- */

@Composable
private fun SleepStatsTab(sessions: List<SleepSessionData>) {
    val total = sessions.size
    val avgMinutes = if (total > 0) {
        sessions.map { (it.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(0) }
            .average()
            .toInt()
    } else 0

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatPill("Avg duration", "${avgMinutes / 60}h${avgMinutes % 60}m")
            StatPill("Sessions", "$total")
        }
        // You can plug your chart here later; keep it simple & safe for now
        Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Tip", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Try to keep your bedtime and wake-up time consistent.")
            }
        }
    }
}

/* ------------------------------ Small pieces ------------------------------- */

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
private fun StatPill(title: String, value: String) {
    Surface(
        shape = CircleShape,
        elevation = 2.dp
    ) {
        Column(
            Modifier
                .widthIn(min = 120.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.caption, color = Color.Gray)
        }
    }
}

/**
 * A very safe stage timeline (no Canvas/MaterialTheme color copy chain).
 * Each stage becomes a weighted Box with a fixed color.
 */
@Composable
private fun StageTimelineBarSimple(stages: List<SleepSessionRecord.Stage>, height: Dp) {
    val totalMinutes = stages.sumOf {
        Duration.between(it.startTime, it.endTime).toMinutes().toInt().coerceAtLeast(1)
    }.coerceAtLeast(1)

    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0x14000000), shape = CircleShape)
            .padding(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { i, s ->
            val mins = Duration.between(s.startTime, s.endTime).toMinutes().toInt().coerceAtLeast(1)
            val weight = mins / totalMinutes.toFloat()
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(weight)
                    .background(stageColor(s.stage), shape = CircleShape)
            )
            if (i != stages.lastIndex) Spacer(Modifier.width(2.dp))
        }
    }
}

private fun stageColor(stage: Int): Color = when (stage) {
    SleepSessionRecord.STAGE_TYPE_AWAKE -> Color(0xFFF6A13E)
    SleepSessionRecord.STAGE_TYPE_LIGHT -> Color(0xFF42A5F5)
    SleepSessionRecord.STAGE_TYPE_DEEP  -> Color(0xFF26A69A)
    SleepSessionRecord.STAGE_TYPE_REM   -> Color(0xFF7E57C2)
    else -> Color.LightGray
}

private fun formatHhMm(d: Duration): String {
    val totalMin = d.toMinutes().toInt().coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return "${h}h${m}m"
}
