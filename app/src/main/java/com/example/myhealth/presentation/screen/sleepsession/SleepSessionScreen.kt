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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
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
    // One-shot error handler
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
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
    val latest = sessions.maxByOrNull { it.endTime }
    val last7 = sessions.take(7)
    val avgMin7 = averageMinutes(last7)
    val typicalBed = typicalClock(last7) { it.startTime }
    val typicalWake = typicalClock(last7) { it.endTime }
    val sessionsCount = last7.size

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Today", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                if (latest != null) {
                    val d = latest.duration ?: Duration.ZERO
                    Text("${formatHhMm(d)}  •  Stages: ${latest.stages.size}")
                } else {
                    Text("No sleep recorded")
                }
            }
        }

        Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("This week", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Average duration: ${formatHhMm(Duration.ofMinutes(avgMin7.toLong()))}")
                Text("Sessions: $sessionsCount")
            }
        }

        Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Consistency", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Typical bedtime: $typicalBed")
                Text("Typical wake-up: $typicalWake")
            }
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
                            Spacer(Modifier.height(10.dp))

                            Text("Stages", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))

                            // Simple timeline bar (no Canvas)
                            StageTimelineBarSimple(s.stages, height = 10.dp)

                            // Color legend (includes gray meaning)
                            Spacer(Modifier.height(8.dp))
                            StageLegend()

                            // Textual breakdown
                            Spacer(Modifier.height(10.dp))
                            StageBreakdownText(
                                stages = s.stages,
                                totalMinutes = (s.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(1)
                            )
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
    val totalMin = sessions.sumOf { (it.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(0) }
        .coerceAtLeast(1)

    // Aggregate minutes by stage
    val byStage: Map<Int, Int> = remember(sessions) {
        sessions.flatMap { it.stages }.groupBy { it.stage }.mapValues { (_, list) ->
            list.sumOf {
                Duration.between(it.startTime, it.endTime).toMinutes().toInt().coerceAtLeast(0)
            }
        }
    }

    // Duration buckets
    val buckets = remember(sessions) {
        val lt6 = sessions.count { ((it.duration ?: Duration.ZERO).toHours() < 6) }
        val btw = sessions.count { val h = (it.duration ?: Duration.ZERO).toHours(); h in 6..8 }
        val gt8 = sessions.count { ((it.duration ?: Duration.ZERO).toHours() > 8) }
        Triple(lt6, btw, gt8)
    }
    val totalSessions = sessions.size.coerceAtLeast(1)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stage distribution", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                listOf(
                    SleepSessionRecord.STAGE_TYPE_DEEP,
                    SleepSessionRecord.STAGE_TYPE_LIGHT,
                    SleepSessionRecord.STAGE_TYPE_REM,
                    SleepSessionRecord.STAGE_TYPE_AWAKE
                ).forEach { st ->
                    val mins = byStage[st] ?: 0
                    val pct = ((mins.toFloat() / totalMin) * 100f).roundToInt()
                    LabeledProgress(label = stageLabel(st), percent = pct, color = stageColor(st))
                }
                Spacer(Modifier.height(6.dp))
                StageLegend() // explain colors + gray track
            }
        }

        Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Duration buckets", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                val (lt6, btw, gt8) = buckets
                LabeledProgress("Under 6h",  (lt6 * 100f / totalSessions).roundToInt(), MaterialTheme.colors.error)
                LabeledProgress("6–8h",      (btw * 100f / totalSessions).roundToInt(), MaterialTheme.colors.primary)
                LabeledProgress("Over 8h",   (gt8 * 100f / totalSessions).roundToInt(), Color(0xFF26A69A))
            }
        }
    }
}

/* ------------------------------ Small pieces ------------------------------- */

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
private fun LabeledProgress(label: String, percent: Int, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("$percent%", color = Color.Gray)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0x14000000), CircleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction = (percent.coerceIn(0, 100) / 100f))
                    .height(8.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
private fun StatPill(title: String, value: String) {
    Surface(shape = CircleShape, elevation = 2.dp) {
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
 * Simple horizontal stage bar (no Canvas).
 * Each stage becomes a weighted Box with a fixed color.
 * Gray track (background) is only the timeline track, NOT a sleep stage.
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
            .background(Color(0x14000000), shape = CircleShape) // <-- gray track = background only
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

/** Legend for stage colors + gray background explanation. */
@Composable
private fun StageLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LegendRow("Deep sleep",  stageColor(SleepSessionRecord.STAGE_TYPE_DEEP))
        LegendRow("Light sleep", stageColor(SleepSessionRecord.STAGE_TYPE_LIGHT))
        LegendRow("REM",         stageColor(SleepSessionRecord.STAGE_TYPE_REM))
        LegendRow("Awake",       stageColor(SleepSessionRecord.STAGE_TYPE_AWAKE))
        LegendRow("Background track (not a stage)", Color(0x14000000))
    }
}

@Composable
private fun LegendRow(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(text, color = if (text.startsWith("Background")) Color.Gray else LocalContentColor.current)
    }
}

/** Textual breakdown for stages: label + hh:mm + percent */
@Composable
private fun StageBreakdownText(stages: List<SleepSessionRecord.Stage>, totalMinutes: Int) {
    val grouped = remember(stages) {
        stages.groupBy { it.stage }.mapValues { (_, list) ->
            list.sumOf {
                Duration.between(it.startTime, it.endTime).toMinutes().toInt().coerceAtLeast(0)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(
            SleepSessionRecord.STAGE_TYPE_DEEP,
            SleepSessionRecord.STAGE_TYPE_LIGHT,
            SleepSessionRecord.STAGE_TYPE_REM,
            SleepSessionRecord.STAGE_TYPE_AWAKE
        ).forEach { st ->
            val mins = grouped[st] ?: 0
            if (mins > 0) {
                val pct = ((mins.toFloat() / totalMinutes) * 100f).roundToInt().coerceIn(0, 100)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(stageColor(st), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(stageLabel(st))
                    }
                    Text("${formatHhMm(Duration.ofMinutes(mins.toLong()))}  (${pct}%)", color = Color.Gray)
                }
            }
        }
    }
}

/* --------------------------------- helpers ---------------------------------- */

private fun stageColor(stage: Int): Color = when (stage) {
    SleepSessionRecord.STAGE_TYPE_AWAKE -> Color(0xFFF6A13E)
    SleepSessionRecord.STAGE_TYPE_LIGHT -> Color(0xFF42A5F5)
    SleepSessionRecord.STAGE_TYPE_DEEP  -> Color(0xFF26A69A)
    SleepSessionRecord.STAGE_TYPE_REM   -> Color(0xFF7E57C2)
    else -> Color.LightGray
}

private fun stageLabel(stage: Int): String = when (stage) {
    SleepSessionRecord.STAGE_TYPE_AWAKE -> "Awake"
    SleepSessionRecord.STAGE_TYPE_LIGHT -> "Light sleep"
    SleepSessionRecord.STAGE_TYPE_DEEP  -> "Deep sleep"
    SleepSessionRecord.STAGE_TYPE_REM   -> "REM"
    else -> "Unknown"
}

private fun formatHhMm(d: Duration): String {
    val totalMin = d.toMinutes().toInt().coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return "${h}h${m}m"
}

private fun averageMinutes(list: List<SleepSessionData>): Int =
    if (list.isEmpty()) 0
    else list.map { (it.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(0) }
        .average().roundToInt()

/** Typical clock time (avg minutes-of-day) for a field (start or end). */
private fun typicalClock(
    list: List<SleepSessionData>,
    pick: (SleepSessionData) -> java.time.Instant
): String {
    if (list.isEmpty()) return "—"
    val zone = ZoneId.systemDefault()
    val minutes = list.map {
        val zdt = ZonedDateTime.ofInstant(pick(it), zone)
        zdt.hour * 60 + zdt.minute
    }
    val avg = minutes.average().roundToInt()
    val h = (avg / 60) % 24
    val m = avg % 60
    return "%02d:%02d".format(h, m)
}
