@file:Suppress("unused")

package com.example.myhealth.presentation.screen.sleepsession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.SleepSessionRecord
import com.example.myhealth.data.SleepSessionData
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

            // Simple suggestion rules
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
/**
 * Weekly stacked bars (last 7 days) + key insights.
 * - One bar per day, stacked by sleep stages (Deep/Light/REM/Awake)
 * - Day is grouped by **wake day** (endTime's local date)
 * - Below the chart: average duration, session count, sleep debt (8h target),
 *   bedtime consistency (std dev), earliest/latest bedtime.
 */
@Composable
private fun SleepStatsTab(sessions: List<SleepSessionData>) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val last7 = remember(sessions) { aggregateLast7Days(sessions, zone, today) }

    val labels = last7.map { it.date.dayOfWeek.name.first().toString() }

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Past 7 days", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            WeeklyStackedBars(days = last7, barWidth = 18.dp, height = 140.dp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                labels.forEach { Text(it, style = MaterialTheme.typography.caption) }
            }
            Spacer(Modifier.height(12.dp))
            LegendColumn()
        }
    }

    Spacer(Modifier.height(12.dp))

    WeeklyAnalysisCard(sessions = sessions, zone = zone)
}

// ---------- Data aggregation ----------

private data class DayAgg(
    val date: LocalDate,
    val totalMin: Int,
    val deepMin: Int,
    val lightMin: Int,
    val remMin: Int,
    val awakeMin: Int
)

/** Group sessions by wake day (endTime local date) and keep only last 7 days. */
private fun aggregateLast7Days(
    sessions: List<SleepSessionData>,
    zone: ZoneId,
    today: LocalDate
): List<DayAgg> {
    val startDay = today.minusDays(6)
    val byDay = mutableMapOf<LocalDate, MutableList<SleepSessionData>>()
    sessions.forEach { s ->
        val day = s.endTime.atZone(zone).toLocalDate()
        if (!day.isBefore(startDay) && !day.isAfter(today)) {
            byDay.getOrPut(day) { mutableListOf() }.add(s)
        }
    }

    fun minutesBetween(a: Instant, b: Instant): Int =
        Duration.between(a, b).toMinutes().toInt().coerceAtLeast(0)

    val result = mutableListOf<DayAgg>()
    for (i in 0..6) {
        val d = startDay.plusDays(i.toLong())
        val list = byDay[d].orEmpty()
        var deep = 0; var light = 0; var rem = 0; var awake = 0; var total = 0
        list.forEach { s ->
            total += (s.duration ?: Duration.between(s.startTime, s.endTime)).toMinutes().toInt().coerceAtLeast(0)
            s.stages.forEach { st ->
                val m = minutesBetween(st.startTime, st.endTime)
                when (st.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP  -> deep += m
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> light += m
                    SleepSessionRecord.STAGE_TYPE_REM   -> rem += m
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> awake += m
                }
            }
        }
        result += DayAgg(d, total, deep, light, rem, awake)
    }
    return result
}

// ---------- Chart (Canvas) ----------

/**
 * Stacked bars with **no gaps between bars** (inter-bar gap = 0).
 * Bars are centered horizontally in the available width.
 */
// --- replace your WeeklyStackedBars with this one ---

@Composable
private fun WeeklyStackedBars(
    days: List<DayAgg>,
    barWidth: Dp,
    height: Dp
) {
    val bwPx = with(LocalDensity.current) { barWidth.toPx() }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val count = days.size.coerceAtLeast(1)

        val segment = w / count

        val barTop    = h * 0.05f
        val barHeight = h * 0.90f
        val barBottom = barTop + barHeight

        val maxMin = (days.maxOfOrNull { it.totalMin } ?: 0).coerceAtLeast(1)

        days.forEachIndexed { i, d ->
            val centerX = segment * i + segment / 2f
            val xLeft   = centerX - bwPx / 2f

            drawRect(
                color = Color(0xFFE5E7EB),
                topLeft = Offset(xLeft, barTop),
                size = Size(bwPx, barHeight)
            )

            var currentTop = barBottom
            fun drawPart(color: Color, minutes: Int) {
                if (minutes <= 0) return
                val ph  = (minutes.toFloat() / maxMin) * barHeight
                val top = currentTop - ph
                drawRect(
                    color = color,
                    topLeft = Offset(xLeft, top),
                    size = Size(bwPx, ph)
                )
                currentTop = top
            }

            drawPart(Color(0xFF26A69A), d.deepMin)   // Deep
            drawPart(Color(0xFF42A5F5), d.lightMin)  // Light
            drawPart(Color(0xFF7E57C2), d.remMin)    // REM
            drawPart(Color(0xFFF6A13E), d.awakeMin)  // Awake
        }
    }
}


@Composable
private fun LegendColumn() {
    @Composable fun RowItem(text: String, color: Color) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Text(text, style = MaterialTheme.typography.caption)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RowItem("Deep sleep",  Color(0xFF26A69A))
        RowItem("Light sleep", Color(0xFF42A5F5))
        RowItem("REM",         Color(0xFF7E57C2))
        RowItem("Awake",       Color(0xFFF6A13E))
        RowItem("Background track (not a stage)", Color(0x14000000))
    }
}

// ---------- Weekly analysis ----------

@Composable
private fun WeeklyAnalysisCard(sessions: List<SleepSessionData>, zone: ZoneId) {
    val totalMin = sessions.sumOf {
        (it.duration ?: Duration.between(it.startTime, it.endTime))
            .toMinutes().toInt().coerceAtLeast(0)
    }
    val count = sessions.size
    val avgMin = if (count > 0) totalMin / count else 0

    // Sleep debt vs 8h/day over last 7 days (non-negative)
    val days = 7
    val target = days * 8 * 60
    val debtMin = (target - totalMin).coerceAtLeast(0)

    // Bedtime consistency (std dev on bedtime minutes-of-day)
    val bedMinutes = sessions.map {
        val lt = it.startTime.atZone(zone).toLocalTime()
        lt.hour * 60 + lt.minute
    }
    val stdDevMin = bedMinutes.stdDevRounded()

    // Earliest / latest bedtime
    val earliest = sessions.minByOrNull { it.startTime }?.startTime?.atZone(zone)?.toLocalTime()
    val latest   = sessions.maxByOrNull { it.startTime }?.startTime?.atZone(zone)?.toLocalTime()

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Weekly analysis", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            Text("Average duration: ${formatHhMm(avgMin)}")
            Text("Sessions counted: $count")
            Text("Sleep debt: ${formatHhMm(debtMin)} (vs 8h/day)")
            Text("Bedtime consistency (std dev): ${stdDevMin} min")
            if (earliest != null && latest != null) {
                Text("Earliest / latest bedtime: ${earliest.formatHm()} – ${latest.formatHm()}")
            }
            // Mini sparkline
            Spacer(Modifier.height(6.dp))
            MiniSparkline(bedMinutes)
        }
    }
}

private fun List<Int>.stdDevRounded(): Int {
    if (isEmpty()) return 0
    val avg = this.average()
    val varSum = this.sumOf { ((it - avg) * (it - avg)) }
    val std = sqrt(varSum / this.size)
    return std.toInt()
}

private fun LocalTime.formatHm(): String =
    "%02d:%02d".format(this.hour, this.minute)

private fun formatHhMm(totalMin: Int): String {
    val h = (totalMin / 60).coerceAtLeast(0)
    val m = (totalMin % 60).coerceAtLeast(0)
    return "$h" + "h" + "$m" + "m"
}

// ---------- Small sparkline ----------

@Composable
private fun MiniSparkline(points: List<Int>) {
    if (points.isEmpty()) return
    val p = points.takeLast(14) // last 2 weeks if available
    Canvas(Modifier.fillMaxWidth().height(20.dp)) {
        val w = size.width
        val h = size.height
        val max = (p.maxOrNull() ?: 0).coerceAtLeast(1)
        val step = w / (p.size - 1).coerceAtLeast(1)
        for (i in 0 until p.size - 1) {
            val x1 = step * i
            val x2 = step * (i + 1)
            val y1 = h - (p[i] / max.toFloat()) * h
            val y2 = h - (p[i + 1] / max.toFloat()) * h
            drawLine(
                color = Color(0xFF3F51B5),
                start = Offset(x1, y1),
                end   = Offset(x2, y2),
                strokeWidth = 3f
            )
        }
    }
}

/* ------------------------------ Reused cards (optional) ------------------------------- */

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
    return "$h" + "h" + "$m" + "m"
}

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

private fun List<SleepSessionData>.averageMinutes(): Int =
    if (isEmpty()) 0
    else map { (it.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(0) }
        .average().roundToInt()

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
