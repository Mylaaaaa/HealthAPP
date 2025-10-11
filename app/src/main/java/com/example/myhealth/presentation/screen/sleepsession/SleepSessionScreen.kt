@file:Suppress("unused")

package com.example.myhealth.presentation.screen.sleepsession
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.automirrored.filled.List
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
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.math.sqrt
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
    var selected by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        NavItem("Overview", Icons.Filled.Home),
        NavItem("Log", Icons.AutoMirrored.Filled.List),
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
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val yesterday = today.minusDays(1)

    val todaySession = sessionForDate(sessions, today, zone)
    val yesterdaySession = sessionForDate(sessions, yesterday, zone)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        OverviewCardFor("Today", todaySession, zone)

        OverviewCardFor("Yesterday", yesterdaySession, zone)
    }
}

@Composable
private fun OverviewCardFor(title: String, s: SleepSessionData?, zone: ZoneId) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            if (s == null) {
                Text("No sleep recorded")
                return@Column
            }

            val m = metrics(s)
            val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)

            Text(
                "${formatHhMm(Duration.ofMinutes(m.totalMinutes.toLong()))}  •  " +
                        "Efficiency ${m.efficiencyPercent}%"
            )
            Text("Bedtime ${fmt.format(s.startTime)}  ·  Wake-up ${fmt.format(s.endTime)}")

            // Stage details
            StageBreakdownRows(
                deep = m.deepMin,
                light = m.lightMin,
                rem = m.remMin,
                awake = m.awakeMin
            )

            // Suggestion block (simple rules)
            SuggestionBlock(m)
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
                            val m = metrics(s)
                            Text("Notes: ${s.notes ?: "—"}")
                            Spacer(Modifier.height(10.dp))

                            Text("Stages", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))

                            StageTimelineBarSimple(s.stages, height = 10.dp)
                            Spacer(Modifier.height(8.dp))
                            StageLegend()

                            Spacer(Modifier.height(10.dp))
                            StageBreakdownRows(
                                deep = m.deepMin,
                                light = m.lightMin,
                                rem = m.remMin,
                                awake = m.awakeMin
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
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val last7 = sessions.take(7)

    // Weekly numbers
    val weekMinAsleep = last7.sumOf { metrics(it).asleepMin }
    val targetWeek = 8 * 60 * 7
    val sleepBalance = weekMinAsleep - targetWeek        // negative => debt
    val avgMin = averageMinutes(last7)
    val consistencyStd = bedtimeStdDevMinutes(last7, zone)
    val (earliestBed, latestBed) = bedtimeExtremes(last7, zone)

    // Mini bars for last 7 sessions (left = older)
    val bars: List<Int> = last7.map { (it.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(0) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Stage distribution & duration buckets（保留前版两卡）
        StageDistributionCard(sessions)
        DurationBucketsCard(sessions)

        // —— Weekly insights ——
        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weekly analysis", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Text("Average duration: ${formatHhMm(Duration.ofMinutes(avgMin.toLong()))}")
                Text("Sessions counted: ${last7.size}")

                val balanceHours = (sleepBalance / 60.0)
                val balanceText = if (sleepBalance >= 0)
                    "Sleep surplus: +${"%.1f".format(balanceHours)} h (vs 8h/day)"
                else
                    "Sleep debt: ${"%.1f".format(-balanceHours)} h (vs 8h/day)"
                Text(balanceText)

                Text("Bedtime consistency (std dev): ${consistencyStd} min")
                if (earliestBed != null && latestBed != null) {
                    Text("Earliest / latest bedtime: $earliestBed – $latestBed")
                }

                Spacer(Modifier.height(8.dp))
                MiniBarsRow(values = bars, maxMinutes = 9 * 60) // scale to 9h
                Text(
                    "Last 7 sessions (left older → right newer)",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        }
    }
}

/* ------------------------------ Reused cards ------------------------------- */

@Composable
private fun StageDistributionCard(sessions: List<SleepSessionData>) {
    val totalMin = sessions.sumOf { (it.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(0) }
        .coerceAtLeast(1)
    val byStage: Map<Int, Int> = remember(sessions) {
        sessions.flatMap { it.stages }.groupBy { it.stage }.mapValues { (_, list) ->
            list.sumOf {
                Duration.between(it.startTime, it.endTime).toMinutes().toInt().coerceAtLeast(0)
            }
        }
    }
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
            StageLegend()
        }
    }
}

@Composable
private fun DurationBucketsCard(sessions: List<SleepSessionData>) {
    val totalSessions = sessions.size.coerceAtLeast(1)
    val lt6 = sessions.count { ((it.duration ?: Duration.ZERO).toHours() < 6) }
    val btw = sessions.count { val h = (it.duration ?: Duration.ZERO).toHours(); h in 6..8 }
    val gt8 = sessions.count { ((it.duration ?: Duration.ZERO).toHours() > 8) }
    Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Duration buckets", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            LabeledProgress("Under 6h",  (lt6 * 100f / totalSessions).roundToInt(), MaterialTheme.colors.error)
            LabeledProgress("6–8h",      (btw * 100f / totalSessions).roundToInt(), MaterialTheme.colors.primary)
            LabeledProgress("Over 8h",   (gt8 * 100f / totalSessions).roundToInt(), Color(0xFF26A69A))
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
private fun StageBreakdownRows(deep: Int, light: Int, rem: Int, awake: Int) {
    @Composable
    fun Line(name: String, minutes: Int, color: Color) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(name)
            }
            Text(formatHhMm(Duration.ofMinutes(minutes.toLong())), color = Color.Gray)
        }
    }

    Line("Deep sleep", deep, stageColor(SleepSessionRecord.STAGE_TYPE_DEEP))
    Line("Light sleep", light, stageColor(SleepSessionRecord.STAGE_TYPE_LIGHT))
    Line("REM",        rem,  stageColor(SleepSessionRecord.STAGE_TYPE_REM))
    Line("Awake",      awake,stageColor(SleepSessionRecord.STAGE_TYPE_AWAKE))
}

/** Suggestion rules based on duration & efficiency & consistency. */
@Composable
private fun SuggestionBlock(m: DayMetrics) {
    val tips = mutableListOf<String>()
    if (m.totalMinutes < 7 * 60) tips += "Aim for at least 7 hours of sleep tonight."
    if (m.efficiencyPercent < 85) tips += "Reduce awakenings: keep the room dark and cool, and avoid screens 30 minutes before bed."
    if (m.bedtimeOffsetStdMin != null && m.bedtimeOffsetStdMin > 45)
        tips += "Try to keep bedtime within a 45-minute window for better consistency."

    if (tips.isEmpty()) tips += "Great job! Keep a consistent schedule to maintain quality."

    Spacer(Modifier.height(6.dp))
    Text("Tips", fontWeight = FontWeight.SemiBold)
    tips.forEach { Text("• $it") }
}

/**
 * Simple horizontal stage bar (no Canvas).
 * Gray track is a background only (not a stage).
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

/** 7-small bars row without Canvas. */
@Composable
private fun MiniBarsRow(values: List<Int>, maxMinutes: Int) {
    val maxV = maxMinutes.coerceAtLeast(values.maxOrNull() ?: 1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { v ->
            val frac = (v.coerceAtLeast(0) / maxV.toFloat()).coerceIn(0f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(Color(0x14000000), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(frac)
                        .background(MaterialTheme.colors.primary, RoundedCornerShape(6.dp))
                )
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

/** Pick the latest session whose end date equals the given date in user's zone. */
private fun sessionForDate(all: List<SleepSessionData>, date: LocalDate, zone: ZoneId): SleepSessionData? =
    all.filter { ZonedDateTime.ofInstant(it.endTime, zone).toLocalDate() == date }
        .maxByOrNull { it.endTime }

/** Metrics for one session (minutes + efficiency). */
private data class DayMetrics(
    val totalMinutes: Int,
    val asleepMin: Int,
    val awakeMin: Int,
    val deepMin: Int,
    val lightMin: Int,
    val remMin: Int,
    val efficiencyPercent: Int,
    val bedtimeOffsetStdMin: Int? = null // filled only when used in weekly context
)

private fun metrics(s: SleepSessionData): DayMetrics {
    val minsByStage = s.stages.groupBy { it.stage }.mapValues { (_, list) ->
        list.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes().toInt().coerceAtLeast(0)
        }
    }
    val deep = minsByStage[SleepSessionRecord.STAGE_TYPE_DEEP] ?: 0
    val light = minsByStage[SleepSessionRecord.STAGE_TYPE_LIGHT] ?: 0
    val rem = minsByStage[SleepSessionRecord.STAGE_TYPE_REM] ?: 0
    val awake = minsByStage[SleepSessionRecord.STAGE_TYPE_AWAKE] ?: 0

    val total = (s.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(deep + light + rem + awake)
    val asleep = deep + light + rem
    val eff = if (total > 0) ((asleep * 100f) / total).roundToInt() else 0

    return DayMetrics(
        totalMinutes = total,
        asleepMin = asleep,
        awakeMin = awake,
        deepMin = deep,
        lightMin = light,
        remMin = rem,
        efficiencyPercent = eff
    )
}

/** Std dev (minutes) of bedtimes in last N sessions. */
private fun bedtimeStdDevMinutes(list: List<SleepSessionData>, zone: ZoneId): Int {
    if (list.size < 2) return 0
    val mins = list.map {
        val z = ZonedDateTime.ofInstant(it.startTime, zone)
        z.hour * 60 + z.minute
    }
    val mean = mins.average()
    val variance = mins.map { (it - mean) * (it - mean) }.average()
    return sqrt(variance).roundToInt()
}

private fun bedtimeExtremes(list: List<SleepSessionData>, zone: ZoneId): Pair<String?, String?> {
    if (list.isEmpty()) return null to null
    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    val sorted = list.map { it.startTime }.sorted()
    return fmt.format(sorted.first()) to fmt.format(sorted.last())
}
