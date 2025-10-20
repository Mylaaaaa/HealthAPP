@file:Suppress("unused")

package com.zihanwang.myhealth.presentation.screen.sleepsession

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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
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
import com.zihanwang.myhealth.data.SleepSessionData
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sqrt

/* ============================================================================
 * Sleep Sessions Screen (Compose / Material)
 * - Overview: if Today/Yesterday are empty -> inject fake data + show analysis & tips
 * - Log: expandable rows with stage timeline
 * - Stats: last 7 days stacked bars; weekday letters aligned with bars
 *
 * IMPORTANT: This project uses the 3-parameter Stage constructor:
 *   SleepSessionRecord.Stage(
 *       startTime = …, endTime = …, stage = …)
 * ========================================================================== */

@Composable
fun SleepSessionScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    sessionsList: List<SleepSessionData>,
    uiState: SleepSessionViewModel.UiState,
    onInsertClick: () -> Unit,
    onError: (Throwable?) -> Unit,
    onPermissionsResult: () -> Unit,
    onPermissionsLaunch: (Set<String>) -> Unit
) {
    val lastErrorId = remember { mutableStateOf(UUID.randomUUID()) }
    LaunchedEffect(uiState) {
        if (uiState is SleepSessionViewModel.UiState.Uninitialized) onPermissionsResult()
        if (uiState is SleepSessionViewModel.UiState.Error && lastErrorId.value != uiState.uuid) {
            onError(uiState.exception)
            lastErrorId.value = uiState.uuid
        }
    }

    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }

    // UI-only augmentation (no writes): always ensure Today / Yesterday + last 7 days exist
    val sessions = remember(sessionsList) { augmentWithFakes3Param(sessionsList, zone, today) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        NavItem("Overview", Icons.Filled.Home),
        NavItem("Log", Icons.AutoMirrored.Filled.List),
        NavItem("Stats", Icons.Filled.BarChart)
    )

    Scaffold(
        bottomBar = {
            BottomNavigation(backgroundColor = MaterialTheme.colors.primary, contentColor = Color.White) {
                tabs.forEachIndexed { i, item ->
                    BottomNavigationItem(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
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
            when (selectedTab) {
                0 -> OverviewTab(sessions)
                1 -> LogTab(sessions)
                2 -> StatsTab(sessions)
            }
        }
    }
}

/* =============================== Overview ================================== */

@Composable
private fun OverviewTab(sessions: List<SleepSessionData>) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val yesterday = today.minusDays(1)

    val todaySession = sessionForDate(sessions, today, zone)
    val yesterdaySession = sessionForDate(sessions, yesterday, zone)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OverviewCard(title = "Today", s = todaySession, zone = zone, all = sessions)
        OverviewCard(title = "Yesterday", s = yesterdaySession, zone = zone, all = sessions)
    }
}

@Composable
private fun OverviewCard(title: String, s: SleepSessionData?, zone: ZoneId, all: List<SleepSessionData>) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            if (s == null) {
                Text("No sleep recorded")
                return@Column
            }
            val m = metrics(s)
            val tf = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)

            Text("${formatHhMm(m.totalMinutes)}  •  Efficiency ${m.efficiencyPercent}%")
            Text("Bedtime ${tf.format(s.startTime)}  ·  Wake-up ${tf.format(s.endTime)}")

            StageBreakdown(deep = m.deepMin, light = m.lightMin, rem = m.remMin, awake = m.awakeMin)

            // Analysis
            val avg = all.averageMinutes()
            val delta = m.totalMinutes - avg
            val deltaTxt = when {
                delta > 15 -> "↑ $delta min vs recent avg"
                delta < -15 -> "↓ ${-delta} min vs recent avg"
                else -> "close to your recent avg"
            }
            Text("Analysis", fontWeight = FontWeight.SemiBold)
            Text("• Duration: ${formatHhMm(m.totalMinutes)} ($deltaTxt; target 7–9h).")
            val pctDeep = if (m.totalMinutes > 0) (m.deepMin * 100 / m.totalMinutes) else 0
            val pctRem = if (m.totalMinutes > 0) (m.remMin * 100 / m.totalMinutes) else 0
            Text("• Deep ${pctDeep}%, REM ${pctRem}% (typical Deep 13–23%, REM 20–25%).")
            Text("• Awake ${formatHhMm(m.awakeMin)}; efficiency ${m.efficiencyPercent}%.")

            // Tips
            val tips = buildList {
                if (m.totalMinutes < 7 * 60) add("Aim for ≥ 7 hours tonight.")
                if (m.efficiencyPercent < 85) add("Cut screen time 30 min before bed; keep room cool and dark.")
                if (m.bedtimeOffsetStdMin != null && m.bedtimeOffsetStdMin > 45) add("Keep bedtime within a 45-minute window.")
                if (isEmpty()) add("Nice work—keep your schedule consistent!")
            }
            Text("Tips", fontWeight = FontWeight.SemiBold)
            tips.forEach { Text("• $it") }
        }
    }
}

/* ================================= Log ===================================== */

@Composable
private fun LogTab(sessions: List<SleepSessionData>) {
    val tf = remember { DateTimeFormatter.ofPattern("HH:mm") }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(sessions, key = { it.uid }) { s ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatHhMm((s.duration ?: Duration.ZERO).toMinutes().toInt()), style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                            val startLocal = s.startTime.atZone(ZoneId.systemDefault()).format(tf)
                            val endLocal = s.endTime.atZone(ZoneId.systemDefault()).format(tf)
                            Text("$startLocal – $endLocal", color = Color.Gray)
                        }
                        Text(if (expanded) "Hide" else "Details", color = MaterialTheme.colors.primary)
                    }
                    AnimatedVisibility(expanded) {
                        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            Divider()
                            Spacer(Modifier.height(8.dp))
                            Text("Stages", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            StageTimelineBar(stages = s.stages, height = 10.dp)
                            Spacer(Modifier.height(8.dp))
                            StageLegend()
                        }
                    }
                }
            }
        }
    }
}

/* ================================= Stats =================================== */

@Composable
private fun StatsTab(sessions: List<SleepSessionData>) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val last7 = remember(sessions) { aggregateLast7(sessions, zone, today) }
    val labels = last7.map { it.date.dayOfWeek.name.first().toString() }

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Past 7 days", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            WeeklyBars(days = last7, barWidth = 18.dp, height = 140.dp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                labels.forEach { Text(it, style = MaterialTheme.typography.caption) }
            }
            Spacer(Modifier.height(12.dp))
            LegendBlock()
        }
    }

    Spacer(Modifier.height(12.dp))
    WeeklyAnalysisCard(sessions, zone)
}

/* ============================== Fake data (3-param Stage) =================== */

private fun augmentWithFakes3Param(original: List<SleepSessionData>, zone: ZoneId, today: LocalDate): List<SleepSessionData> {
    val startDay = today.minusDays(6)
    val byWake = original.groupBy { it.endTime.atZone(zone).toLocalDate() }.toMutableMap()

    fun ensure(date: LocalDate) {
        if (byWake[date].isNullOrEmpty()) {
            byWake[date] = (byWake[date] ?: emptyList()) + makeFakeFor3Param(date, zone)
        }
    }
    ensure(today)
    ensure(today.minusDays(1))
    for (i in 0..6) ensure(startDay.plusDays(i.toLong()))

    return byWake.values.flatten().sortedBy { it.endTime }
}

private fun makeFakeFor3Param(date: LocalDate, zone: ZoneId): SleepSessionData {
    val seed = date.dayOfYear
    val wake = date.atTime(7, (seed % 37)).atZone(zone).toInstant()
    val totalMin = 420 + (seed % 80) // 7h~8h20m
    val start = wake.minusSeconds(totalMin * 60L)

    val deep = 80 + (seed * 7 % 30)
    val rem = 70 + (seed * 5 % 25)
    val awake = 10 + (seed * 3 % 15)
    val light = (totalMin - deep - rem - awake).coerceAtLeast(30)

    val stages = mutableListOf<SleepSessionRecord.Stage>()
    var cursor = start
    fun add(kind: Int, minutes: Int) {
        val end = cursor.plusSeconds(minutes * 60L)
        // 3-parameter constructor to match your project’s HC version
        stages += SleepSessionRecord.Stage(
            startTime = cursor,
            endTime = end,
            stage = kind
        )
        cursor = end
    }
    add(SleepSessionRecord.STAGE_TYPE_AWAKE, awake / 2)
    add(SleepSessionRecord.STAGE_TYPE_LIGHT, light / 2)
    add(SleepSessionRecord.STAGE_TYPE_DEEP, deep)
    add(SleepSessionRecord.STAGE_TYPE_REM, rem)
    add(SleepSessionRecord.STAGE_TYPE_LIGHT, light - light / 2)
    add(SleepSessionRecord.STAGE_TYPE_AWAKE, (awake - awake / 2).coerceAtLeast(1))

    return SleepSessionData(
        uid = "fake-$date",
        startTime = start,
        endTime = wake,
        duration = Duration.ofMinutes(totalMin.toLong()),
        stages = stages,
        notes = "Synthetic session"
    )
}

/* ======================= Aggregation & visualization ======================= */

private data class DayAgg(val date: LocalDate, val total: Int, val deep: Int, val light: Int, val rem: Int, val awake: Int)

private fun aggregateLast7(sessions: List<SleepSessionData>, zone: ZoneId, today: LocalDate): List<DayAgg> {
    val startDay = today.minusDays(6)
    val map = mutableMapOf<LocalDate, MutableList<SleepSessionData>>()
    sessions.forEach { s ->
        val d = s.endTime.atZone(zone).toLocalDate()
        if (!d.isBefore(startDay) && !d.isAfter(today)) map.getOrPut(d) { mutableListOf() }.add(s)
    }
    fun mins(a: Instant, b: Instant) = Duration.between(a, b).toMinutes().toInt().coerceAtLeast(0)

    return (0..6).map { i ->
        val d = startDay.plusDays(i.toLong())
        var deep = 0; var light = 0; var rem = 0; var awake = 0; var total = 0
        for (s in map[d].orEmpty()) {
            total += (s.duration ?: Duration.between(s.startTime, s.endTime)).toMinutes().toInt().coerceAtLeast(0)
            s.stages.forEach { st ->
                val m = mins(st.startTime, st.endTime)
                when (st.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deep += m
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> light += m
                    SleepSessionRecord.STAGE_TYPE_REM -> rem += m
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> awake += m
                }
            }
        }
        DayAgg(d, total, deep, light, rem, awake)
    }
}

/* ================================= Charts & UI bits ======================== */

@Composable
private fun WeeklyBars(days: List<DayAgg>, barWidth: Dp, height: Dp) {
    val bw = with(LocalDensity.current) { barWidth.toPx() }
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val n = days.size.coerceAtLeast(1)
        val seg = w / n
        val top = h * 0.05f
        val trackH = h * 0.90f
        val bottom = top + trackH
        val maxMin = (days.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)

        days.forEachIndexed { i, d ->
            val cx = seg * i + seg / 2f
            val left = cx - bw / 2f

            // Background track
            drawRect(color = Color(0xFFE5E7EB), topLeft = Offset(left, top), size = Size(bw, trackH))

            var curTop = bottom
            fun part(color: Color, minutes: Int) {
                if (minutes <= 0) return
                val ph = (minutes.toFloat() / maxMin) * trackH
                val t = curTop - ph
                drawRect(color = color, topLeft = Offset(left, t), size = Size(bw, ph))
                curTop = t
            }
            part(Color(0xFF26A69A), d.deep)   // Deep
            part(Color(0xFF42A5F5), d.light)  // Light
            part(Color(0xFF7E57C2), d.rem)    // REM
            part(Color(0xFFF6A13E), d.awake)  // Awake
        }
    }
}

@Composable
private fun StageTimelineBar(stages: List<SleepSessionRecord.Stage>, height: Dp) {
    val total = stages.sumOf { Duration.between(it.startTime, it.endTime).toMinutes().toInt().coerceAtLeast(1) }.coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(height).background(Color(0x14000000), CircleShape).padding(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { i, s ->
            val m = Duration.between(s.startTime, s.endTime).toMinutes().toInt().coerceAtLeast(1)
            val weight = m / total.toFloat()
            Box(
                Modifier.fillMaxHeight().weight(weight).background(
                    when (s.stage) {
                        SleepSessionRecord.STAGE_TYPE_DEEP -> Color(0xFF26A69A)
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> Color(0xFF42A5F5)
                        SleepSessionRecord.STAGE_TYPE_REM -> Color(0xFF7E57C2)
                        SleepSessionRecord.STAGE_TYPE_AWAKE -> Color(0xFFF6A13E)
                        else -> Color.LightGray
                    },
                    CircleShape
                )
            )
            if (i != stages.lastIndex) Spacer(Modifier.width(2.dp))
        }
    }
}

@Composable
private fun StageLegend() {
    @Composable fun Item(text: String, color: Color) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Text(text, style = MaterialTheme.typography.caption)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Item("Deep sleep", Color(0xFF26A69A))
        Item("Light sleep", Color(0xFF42A5F5))
        Item("REM", Color(0xFF7E57C2))
        Item("Awake", Color(0xFFF6A13E))
        Item("Background track (not a stage)", Color(0x14000000))
    }
}

@Composable private fun LegendBlock() { StageLegend() }

/* ================================ Analysis ================================= */

@Composable
private fun WeeklyAnalysisCard(sessions: List<SleepSessionData>, zone: ZoneId) {
    val totalMin = sessions.sumOf { (it.duration ?: Duration.between(it.startTime, it.endTime)).toMinutes().toInt().coerceAtLeast(0) }
    val count = sessions.size
    val avg = if (count > 0) totalMin / count else 0
    val debt = (7 * 8 * 60 - totalMin).coerceAtLeast(0)

    val bedMins = sessions.map {
        val t = it.startTime.atZone(zone).toLocalTime(); t.hour * 60 + t.minute
    }
    val std = bedMins.stdDevRounded()

    val earliest = sessions.minByOrNull { it.startTime }?.startTime?.atZone(zone)?.toLocalTime()
    val latest = sessions.maxByOrNull { it.startTime }?.startTime?.atZone(zone)?.toLocalTime()

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Weekly analysis", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
            Text("Average duration: ${formatHhMm(avg)}")
            Text("Sessions counted: $count")
            Text("Sleep debt: ${formatHhMm(debt)} (vs 8h/day)")
            Text("Bedtime consistency (std dev): $std min")
            if (earliest != null && latest != null) {
                val tf = DateTimeFormatter.ofPattern("HH:mm")
                Text("Earliest / latest bedtime: ${earliest.format(tf)} – ${latest.format(tf)}")
            }
        }
    }
}

/* =============================== Small helpers ============================= */

private data class NavItem(val label: String, val icon: ImageVector)

private fun formatHhMm(totalMin: Int): String {
    val h = totalMin / 60
    val m = totalMin % 60
    return "${h}h${m}m"
}

private fun sessionForDate(list: List<SleepSessionData>, date: LocalDate, zone: ZoneId): SleepSessionData? =
    list.filter { it.endTime.atZone(zone).toLocalDate() == date }.maxByOrNull { it.endTime }

private data class DayMetrics(
    val totalMinutes: Int,
    val asleepMin: Int,
    val awakeMin: Int,
    val deepMin: Int,
    val lightMin: Int,
    val remMin: Int,
    val efficiencyPercent: Int,
    val bedtimeOffsetStdMin: Int? = null
)

private fun metrics(s: SleepSessionData): DayMetrics {
    val by = s.stages.groupBy { it.stage }.mapValues { (_, v) ->
        v.sumOf { Duration.between(it.startTime, it.endTime).toMinutes().toInt().coerceAtLeast(0) }
    }
    val deep = by[SleepSessionRecord.STAGE_TYPE_DEEP] ?: 0
    val light = by[SleepSessionRecord.STAGE_TYPE_LIGHT] ?: 0
    val rem = by[SleepSessionRecord.STAGE_TYPE_REM] ?: 0
    val awake = by[SleepSessionRecord.STAGE_TYPE_AWAKE] ?: 0
    val total = (s.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(deep + light + rem + awake)
    val asleep = deep + light + rem
    val eff = if (total > 0) ((asleep * 100f) / total).roundToInt() else 0
    return DayMetrics(total, asleep, awake, deep, light, rem, eff)
}

private fun List<SleepSessionData>.averageMinutes(): Int =
    if (isEmpty()) 0 else map { (it.duration ?: Duration.ZERO).toMinutes().toInt().coerceAtLeast(0) }.average().roundToInt()

private fun List<Int>.stdDevRounded(): Int {
    if (isEmpty()) return 0
    val avg = average()
    val sum = sumOf { (it - avg) * (it - avg) }
    return sqrt(sum / size).roundToInt()
}

@Composable
private fun StageBreakdown(deep: Int, light: Int, rem: Int, awake: Int) {
    @Composable
    fun RowItem(name: String, minutes: Int, color: Color) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(name)
            }
            Text(formatHhMm(minutes), color = Color.Gray)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RowItem("Deep sleep", deep, Color(0xFF26A69A))
        RowItem("Light sleep", light, Color(0xFF42A5F5))
        RowItem("REM", rem, Color(0xFF7E57C2))
        RowItem("Awake", awake, Color(0xFFF6A13E))
    }
}
