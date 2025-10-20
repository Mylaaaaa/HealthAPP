@file:Suppress("unused")

package com.zihanwang.myhealth.presentation.screen.exercisesession

import androidx.compose.runtime.*
import com.zihanwang.myhealth.data.HealthConnectManager
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.time.LocalDate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zihanwang.myhealth.data.ExerciseSession
import kotlin.math.min

/* ============================================================
 *  Host: loads real sessions from Health Connect for this week,
 *  then feeds your existing ExerciseStatsScreen UI.
 *  NOTE: this keeps your original UI & logic untouched.
 * ============================================================ */
@Composable
fun ExerciseStatsScreenHost(
    manager: HealthConnectManager,
    modifier: Modifier = Modifier
) {
    var sessions by remember { mutableStateOf<List<ExerciseSession>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Current week: Monday 00:00 -> Sunday 23:59:59.999
        val now = ZonedDateTime.now()
        val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toLocalDate()
            .atStartOfDay(now.zone)
        val weekEnd = weekStart.plusDays(7).minusNanos(1)

        // Read ExerciseSessionRecord from Health Connect
        val recs = manager.readExerciseSessions(
            start = weekStart.toInstant(),
            end   = weekEnd.toInstant()
        )

        // Map HC records -> your domain model (ZonedDateTime)
        sessions = recs.map { r ->
            ExerciseSession(
                id = r.metadata.id,
                title = r.title ?: "Session",
                startTime = r.startTime.atZone(now.zone),               // ZonedDateTime
                endTime   = r.endTime.atZone(now.zone),                 // ZonedDateTime
                sourceAppInfo = null
            )
        }
    }

    ExerciseStatsScreen(
        modifier = modifier,
        sessions = sessions
    )
}

/* ============================================================
 *  Your ORIGINAL stats dashboard UI (kept intact)
 * ============================================================ */
@Composable
fun ExerciseStatsScreen(
    modifier: Modifier = Modifier,
    sessions: List<ExerciseSession>
) {
    // Build UI state from real sessions
    val state = remember(sessions) { buildStatsUiStateFrom(sessions) }

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
    }
}

/* ---------------- UI state ---------------- */

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

/* ---------------- Building blocks ---------------- */

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
        Box(Modifier.padding(8.dp), contentAlignment = Alignment.CenterStart, content = content)
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
            Text("$completed / $goal sessions completed", style = MaterialTheme.typography.subtitle1)
            Spacer(Modifier.height(10.dp))
            LinearProgress(progress = progress, trackColor = track, barColor = bar, height = 10.dp, corner = 8.dp)
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
        LinearProgress(progress = (percent / 100f).coerceIn(0f, 1f), trackColor = track, barColor = bar, height = 8.dp, corner = 6.dp)
    }
}

/** Rounded-corner horizontal progress bar (M2 safe). */
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

/** Minimal bar chart with Canvas (no 3rd-party lib, works on M2). */
@Composable
private fun WeeklyBarChart(
    values: List<Int>,
    labels: List<String>,
    barWidth: Dp = 10.dp,
    height: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    // Colors follow the current theme
    val colors = MaterialTheme.colors
    val trackColor = colors.onSurface.copy(alpha = 0.12f)   // background rail
    val barColor = colors.primary                           // filled part
    val labelColor = colors.onSurface.copy(alpha = 0.60f)   // text under bars

    val bars = min(values.size, labels.size)
    if (bars == 0) return

    Column(modifier = modifier) {
        // --- Bars ---
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val barPx = barWidth.toPx()
            val maxV = (values.maxOrNull() ?: 0).coerceAtLeast(1)
            val gap = ((size.width - bars * barPx) / (bars + 1)).coerceAtLeast(0f)
            val chartHeightPx = size.height * 0.85f // leave room for rounded caps

            repeat(bars) { i ->
                val x = gap + i * (barPx + gap)
                val v = values[i].coerceAtLeast(0)
                val h = (v / maxV.toFloat()) * chartHeightPx

                // background "rail"
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

        // --- Labels under bars ---
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
 *    REAL aggregation from your ExerciseSession (kept)
 * ============================================================ */
private fun buildStatsUiStateFrom(sessions: List<ExerciseSession>): StatsUiState {
    if (sessions.isEmpty()) {
        // Graceful fallback when no data
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

    // Current week range Mon..Sun
    val weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusDays(6)

    data class Sess(val date: LocalDate, val minutes: Int, val title: String)

    val norm = sessions.mapNotNull { s ->
        val startZdt = runCatching { s.startTime }.getOrNull()   // ZonedDateTime?
        val endZdt   = runCatching { s.endTime }.getOrNull()     // ZonedDateTime?
        if (startZdt == null || endZdt == null) return@mapNotNull null

        val minutes = kotlin.math.max(0, java.time.Duration.between(startZdt, endZdt).toMinutes().toInt())
        val date = startZdt.toLocalDate()
        Sess(date = date, minutes = minutes, title = runCatching { s.title }.getOrNull().orEmpty())
    }

    // Weekly minutes (Mon..Sun)
    val weeklyMinutes = IntArray(7) { 0 }
    norm.forEach { ss ->
        if (!ss.date.isBefore(weekStart) && !ss.date.isAfter(weekEnd)) {
            val idx = dayIndexMon0Sun6(ss.date.dayOfWeek)
            if (idx in 0..6) weeklyMinutes[idx] += ss.minutes
        }
    }

    val weekCompleted = weeklyMinutes.count { it > 0 }
    val weekGoal = 7

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
        weekGoal = weekGoal,
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
 * Preview cannot access your app's HealthConnectManager,
 * so we preview the pure UI with sample sessions.
 * -------------------------------------------------- */
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun Preview_ExerciseStatsScreen() {
    // Sample data for preview only
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.ZonedDateTime.now(zone)
    val sample = listOf(
        ExerciseSession(
            id = "p1",
            title = "Run",
            startTime = today.minusDays(1),
            endTime = today.minusDays(1).plusMinutes(45),
            sourceAppInfo = null
        ),
        ExerciseSession(
            id = "p2",
            title = "Walk",
            startTime = today.minusDays(2),
            endTime = today.minusDays(2).plusMinutes(30),
            sourceAppInfo = null
        ),
        ExerciseSession(
            id = "p3",
            title = "Yoga",
            startTime = today.minusDays(3),
            endTime = today.minusDays(3).plusMinutes(60),
            sourceAppInfo = null
        )
    )

    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            ExerciseStatsScreen(modifier = Modifier.fillMaxSize(), sessions = sample)
        }
    }
}
