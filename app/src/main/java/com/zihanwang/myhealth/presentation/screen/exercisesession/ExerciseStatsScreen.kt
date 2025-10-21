@file:Suppress("unused")

package com.zihanwang.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zihanwang.myhealth.data.ExerciseSession
import com.zihanwang.myhealth.data.HealthConnectManager
import kotlin.math.min
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/* ============================================================
 *  Host composable:
 *  - Loads real sessions from Health Connect for the current week
 *  - Hosts your existing stats UI
 *  - Handles item click -> shows detail dialog
 *  NOTE: To actually get the dialog, your Nav destination must call
 *        ExerciseStatsScreenHost(...), not ExerciseStatsScreen(...).
 * ============================================================ */
@Composable
fun ExerciseStatsScreenHost(
    manager: HealthConnectManager,
    modifier: Modifier = Modifier
) {
    var sessions by remember { mutableStateOf<List<ExerciseSession>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var selected by remember { mutableStateOf<ExerciseSession?>(null) } // keeps the clicked session

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        try {
            val now = ZonedDateTime.now()
            val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate().atStartOfDay(now.zone)
            val weekEnd = weekStart.plusDays(7).minusNanos(1)

            val recs = manager.readExerciseSessions(
                start = weekStart.toInstant(),
                end = weekEnd.toInstant()
            )
            sessions = recs.map { r ->
                ExerciseSession(
                    id = r.metadata.id,
                    title = r.title ?: "Session",
                    startTime = r.startTime.atZone(now.zone),
                    endTime = r.endTime.atZone(now.zone),
                    sourceAppInfo = null
                )
            }
        } catch (t: Throwable) {
            error = t
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> Text(
            "Loading…",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.caption
        )
        error != null -> Text(
            "Failed to load data",
            modifier = Modifier.padding(16.dp),
            color = Color.Red,
            style = MaterialTheme.typography.caption
        )
        else -> {
            ExerciseStatsScreen(
                modifier = modifier,
                sessions = sessions,
                onOpenDetail = { sess -> selected = sess } // callback from row click
            )

            // Show detail as a dialog when an item is selected
            selected?.let { s ->
                androidx.compose.ui.window.Dialog(onDismissRequest = { selected = null }) {
                    Surface(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Session detail", style = MaterialTheme.typography.h6)
                                Text(
                                    "Close",
                                    color = MaterialTheme.colors.primary,
                                    modifier = Modifier.clickable { selected = null }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            // Reuse your existing detail screen
                            ExerciseDetailScreen(
                                session = s,
                                onClose = { selected = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================
 *  Your existing stats dashboard UI (kept intact)
 *  - Pure UI; it does not load data or open dialogs by itself.
 *  - onOpenDetail is provided by the Host above.
 * ============================================================ */
@Composable
fun ExerciseStatsScreen(
    modifier: Modifier = Modifier,
    sessions: List<ExerciseSession>,
    onOpenDetail: (ExerciseSession) -> Unit = {}   // keep your original API
) {
    // Build UI state from real sessions
    val state = remember(sessions) { buildStatsUiStateFrom(sessions) }

    // --- Built-in fallback dialog state ---
    // If the caller doesn't show a dialog, we will show one locally.
    var localOpen by remember { mutableStateOf<ExerciseSession?>(null) }

    // Wrap the external callback: call it first, then open our local dialog as a fallback.
    val openDetail: (ExerciseSession) -> Unit = { s ->
        onOpenDetail(s)     // let the caller handle it if they want
        localOpen = s       // still open locally so the user always sees something
    }

    // --- Empty state ---
    if (sessions.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "This week",
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))
            EmptyStateCard(
                title = "No activity this week",
                subtitle = "Start a session to track your progress."
            )
        }
        return
    }

    // --- Normal content when we do have sessions ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1) Week summary + progress bar
        Text(
            "This week",
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))
        SummaryProgressCard(
            completed = state.weekCompleted,
            goal = state.weekGoal
        )

        Spacer(Modifier.height(16.dp))

        // 2) Quick stats row (3 cards)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatCard(
                title = "Total time",
                value = "${state.totalMinutes}m",
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                title = "Avg / session",
                value = "${state.avgPerSessionMinutes}m",
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                title = "Longest streak",
                value = "${state.longestStreakDays} days",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // 3) Weekly trend (Mon..Sun)
        SectionTitle("Weekly activity")
        CardBox {
            WeeklyBarChart(
                values = state.weeklyMinutes,
                labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                barWidth = 18.dp,
                height = 140.dp
            )
        }

        Spacer(Modifier.height(20.dp))

        // 4) Activity breakdown
        SectionTitle("Activity breakdown")
        CardBox {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.activityBreakdown.forEach {
                    BreakdownRow(name = it.name, percent = it.percent)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 5) Recent sessions (tap to open detail)
        Spacer(Modifier.height(20.dp))
        SectionTitle("Recent sessions")
        CardBox {
            RecentSessionsList(
                sessions = sessions,
                onClick = openDetail         // <— use the wrapped callback
            )
        }
    }

    // --- Built-in fallback detail dialog (shows even if caller doesn't handle clicks) ---
    localOpen?.let { s ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { localOpen = null }) {
            androidx.compose.material.Surface(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Session detail", style = MaterialTheme.typography.h6)
                        Text(
                            "Close",
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.clickable { localOpen = null }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Reuse your existing detail screen
                    ExerciseDetailScreen(
                        session = s,
                        onClose = { localOpen = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSessionsList(
    sessions: List<ExerciseSession>,
    onClick: (ExerciseSession) -> Unit
) {
    val rows = remember(sessions) { sessions.sortedByDescending { it.startTime }.take(10) }
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        rows.forEach { s ->
            RecentSessionRow(s) { onClick(s) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RecentSessionRow(
    session: ExerciseSession,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colors
    val stroke = colors.onSurface.copy(alpha = 0.12f)
    val minutes = runCatching {
        java.time.Duration.between(session.startTime, session.endTime).toMinutes().toInt()
    }.getOrDefault(0).coerceAtLeast(0)

    Card(
        backgroundColor = colors.surface,
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // whole row is tappable
            .border(1.dp, stroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 4.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = session.title.orEmpty().ifBlank { "Session" },
                    style = MaterialTheme.typography.subtitle2
                )
                val day = session.startTime.dayOfWeek.name.first()
                Text(
                    text = "$day • ${minutes}m",
                    style = MaterialTheme.typography.caption,
                    color = colors.onSurface.copy(alpha = 0.6f)
                )
            }
            // simple chevron
            Text(">", color = colors.onSurface.copy(alpha = 0.6f))
        }
    }
}

/* ---------------- UI state & helpers ---------------- */

data class StatsUiState(
    val weekCompleted: Int,
    val weekGoal: Int,
    val totalMinutes: Int,
    val avgPerSessionMinutes: Int,
    val longestStreakDays: Int,
    val weeklyMinutes: List<Int>,            // size = 7 (Mon..Sun)
    val activityBreakdown: List<ActivityShare>
)

data class ActivityShare(val name: String, val percent: Int)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CardBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colors
    val stroke = colors.onSurface.copy(alpha = 0.12f)

    Card(
        backgroundColor = colors.surface,
        elevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, stroke, RoundedCornerShape(20.dp))
    ) {
        Box(
            Modifier.padding(8.dp),
            contentAlignment = Alignment.CenterStart,
            content = content
        )
    }
}

@Composable
private fun SummaryProgressCard(completed: Int, goal: Int) {
    val colors = MaterialTheme.colors
    val track = colors.onSurface.copy(alpha = 0.15f)
    val bar = colors.primary
    val progress = if (goal <= 0) 0f else (completed.toFloat() / goal).coerceIn(0f, 1f)

    CardBox {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("$completed / $goal days completed(per week)", style = MaterialTheme.typography.subtitle1)
            Spacer(Modifier.height(10.dp))
            LinearProgress(
                progress = progress,
                trackColor = track,
                barColor = bar,
                height = 10.dp,
                corner = 8.dp
            )
        }
    }
}

@Composable
private fun QuickStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colors
    val stroke = colors.onSurface.copy(alpha = 0.12f)
    val muted = colors.onSurface.copy(alpha = 0.6f)

    Card(
        backgroundColor = colors.surface,
        elevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.border(1.dp, stroke, RoundedCornerShape(20.dp))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth()) {
            Text(title, color = muted, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun BreakdownRow(name: String, percent: Int) {
    val colors = MaterialTheme.colors
    val muted = colors.onSurface.copy(alpha = 0.6f)
    val track = colors.onSurface.copy(alpha = 0.12f)
    val bar = colors.primary

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 15.sp)
            Text("$percent%", color = muted, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgress(
            progress = (percent / 100f).coerceIn(0f, 1f),
            trackColor = track,
            barColor = bar,
            height = 8.dp,
            corner = 6.dp
        )
    }
}

/** Rounded-corner horizontal progress bar (Material 2 friendly). */
@Composable
private fun LinearProgress(
    progress: Float,
    trackColor: Color,
    barColor: Color,
    height: Dp,
    corner: Dp
) {
    val shape = RoundedCornerShape(corner)
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(barColor)
        )
    }
}

/** Minimal bar chart using Canvas (no 3rd-party libs). */
@Composable
private fun WeeklyBarChart(
    values: List<Int>,
    labels: List<String>,
    barWidth: Dp = 10.dp,
    height: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colors
    val trackColor = colors.onSurface.copy(alpha = 0.12f)
    val barColor = colors.primary
    val labelColor = colors.onSurface.copy(alpha = 0.60f)

    val bars = min(values.size, labels.size)
    if (bars == 0) return

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val barPx = barWidth.toPx()
            val maxV = (values.maxOrNull() ?: 0).coerceAtLeast(1)
            val gap = ((size.width - bars * barPx) / (bars + 1)).coerceAtLeast(0f)
            val chartHeightPx = size.height * 0.85f

            repeat(bars) { i ->
                val x = gap + i * (barPx + gap)
                val v = values[i].coerceAtLeast(0)
                val h = (v / maxV.toFloat()) * chartHeightPx

                // rail
                drawLine(
                    color = trackColor,
                    start = Offset(x + barPx / 2, size.height),
                    end = Offset(x + barPx / 2, size.height - chartHeightPx),
                    strokeWidth = barPx,
                    cap = StrokeCap.Round
                )
                // value
                if (h > 0f) {
                    drawLine(
                        color = barColor,
                        start = Offset(x + barPx / 2, size.height),
                        end = Offset(x + barPx / 2, size.height - h),
                        strokeWidth = barPx,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(bars) { i ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = barWidth * 2)
                ) {
                    Text(labels[i], color = labelColor, fontSize = 13.sp, maxLines = 1)
                    val v = values[i]
                    if (v > 0) {
                        Text("${v}m", color = labelColor, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/* ============================================================
 *  Real aggregation from your ExerciseSession list
 * ============================================================ */
private fun buildStatsUiStateFrom(sessions: List<ExerciseSession>): StatsUiState {
    if (sessions.isEmpty()) {
        return StatsUiState(
            weekCompleted = 0,
            weekGoal = 7,
            totalMinutes = 0,
            avgPerSessionMinutes = 0,
            longestStreakDays = 0,
            weeklyMinutes = List(7) { 0 },
            activityBreakdown = emptyList()
        )
    }

    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)

    val weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusDays(6)

    data class Sess(val date: LocalDate, val minutes: Int, val title: String)

    val norm = sessions.mapNotNull { s ->
        val startZdt = runCatching { s.startTime }.getOrNull()
        val endZdt = runCatching { s.endTime }.getOrNull()
        if (startZdt == null || endZdt == null) return@mapNotNull null

        val minutes = kotlin.math.max(
            0,
            java.time.Duration.between(startZdt, endZdt).toMinutes().toInt()
        )
        val date = startZdt.toLocalDate()
        Sess(date = date, minutes = minutes, title = runCatching { s.title }.getOrNull().orEmpty())
    }

    val weeklyMinutes = IntArray(7) { 0 }
    norm.forEach { ss ->
        if (!ss.date.isBefore(weekStart) && !ss.date.isAfter(weekEnd)) {
            val idx = dayIndexMon0Sun6(ss.date.dayOfWeek)
            if (idx in 0..6) weeklyMinutes[idx] += ss.minutes
        }
    }

    val weekCompleted = weeklyMinutes.count { it > 0 }
    val totalMinutes = norm.sumOf { it.minutes }
    val avgPerSessionMinutes = if (norm.isNotEmpty()) totalMinutes / norm.size else 0

    val daysWithActivity: Set<LocalDate> = norm.filter { it.minutes > 0 }.map { it.date }.toSet()
    val longestStreakDays = longestStreak(daysWithActivity)

    val byCategory = norm.groupBy { guessCategory(it.title) }
    val totalForBreakdown = byCategory.values.sumOf { list -> list.sumOf { it.minutes } }.coerceAtLeast(1)
    val breakdown = byCategory.entries
        .sortedByDescending { it.value.sumOf { s -> s.minutes } }
        .map { (cat, list) ->
            val pct = (list.sumOf { it.minutes } * 100f / totalForBreakdown).toInt()
            ActivityShare(name = cat, percent = pct)
        }

    return StatsUiState(
        weekCompleted = weekCompleted,
        weekGoal = 7,
        totalMinutes = totalMinutes,
        avgPerSessionMinutes = avgPerSessionMinutes,
        longestStreakDays = longestStreakDays,
        weeklyMinutes = weeklyMinutes.toList(),
        activityBreakdown = breakdown
    )
}

// Map DayOfWeek -> index: Mon=0 .. Sun=6
private fun dayIndexMon0Sun6(d: java.time.DayOfWeek): Int =
    when (d) {
        java.time.DayOfWeek.MONDAY -> 0
        java.time.DayOfWeek.TUESDAY -> 1
        java.time.DayOfWeek.WEDNESDAY -> 2
        java.time.DayOfWeek.THURSDAY -> 3
        java.time.DayOfWeek.FRIDAY -> 4
        java.time.DayOfWeek.SATURDAY -> 5
        java.time.DayOfWeek.SUNDAY -> 6
    }

// Longest consecutive-day streak
private fun longestStreak(activeDays: Set<LocalDate>): Int {
    if (activeDays.isEmpty()) return 0
    val sorted = activeDays.sorted()
    var best = 1
    var cur = 1
    for (i in 1 until sorted.size) {
        val prev = sorted[i - 1]
        val curDay = sorted[i]
        if (java.time.temporal.ChronoUnit.DAYS.between(prev, curDay) == 1L) {
            cur += 1
            if (cur > best) best = cur
        } else {
            cur = 1
        }
    }
    return best
}

private fun guessCategory(title: String): String {
    val t = title.lowercase()
    return when {
        listOf("run", "jog").any { it in t } -> "Cardio"
        listOf("walk", "steps").any { it in t } -> "Cardio"
        listOf("ride", "cycle", "bike").any { it in t } -> "Cardio"
        listOf("yoga", "stretch").any { it in t } -> "Yoga"
        listOf("lift", "strength", "gym", "weights").any { it in t } -> "Strength"
        else -> "Other"
    }
}

/* ---------------- Preview (UI only) ----------------
 * Preview cannot open dialogs and does not connect to Health Connect.
 * It is only for design-time rendering.
 * -------------------------------------------------- */
@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Stats – empty state")
@Composable
private fun Preview_ExerciseStatsScreen() {
    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            ExerciseStatsScreen(
                modifier = Modifier.fillMaxSize(),
                sessions = emptyList()
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String
) {
    val colors = MaterialTheme.colors
    val stroke = colors.onSurface.copy(alpha = 0.12f)
    Card(
        backgroundColor = colors.surface,
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, stroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.FitnessCenter,
                contentDescription = "Exercise stats", // a11y
                tint = colors.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.caption,
                color = colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
