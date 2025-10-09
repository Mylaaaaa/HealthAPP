package com.example.myhealth.presentation.screen.exercisesession

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.myhealth.presentation.screen.exercisesession.planaccess.PlanTasksStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Courses screen (build-safe, no top bar here).
 * - No internal top app bar to avoid duplicated headers with parent screen.
 * - Buttons show Toast by default so you can verify tap works immediately.
 * - Progress auto-syncs from PlanTasksStore (i.e., when Workout marks all planned tasks done).
 */
data class CourseLite(
    val id: String,
    val title: String,
    val totalWeeks: Int,
    val daysPerWeek: Int
)

data class WeekStats(
    val weekIndex: Int,
    val totalWeeks: Int,
    val weekDone: Int,
    val weekTarget: Int,
    val totalDone: Int,
    val totalTarget: Int
)

private class CourseProgressStore(private val context: Context) {
    private val sp: SharedPreferences
        get() = context.getSharedPreferences("courses_progress", Context.MODE_PRIVATE)

    fun ensureSeed(course: CourseLite) {
        if (!sp.contains("active_course")) sp.edit { putString("active_course", course.id) }
        if (!sp.contains("start_date")) {
            val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            sp.edit { putString("start_date", monday.toString()) }
        }
    }

    private fun startMonday(): LocalDate {
        val raw = sp.getString("start_date", null)
        return if (raw.isNullOrBlank()) {
            val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            sp.edit { putString("start_date", monday.toString()) }
            monday
        } else LocalDate.parse(raw)
    }

    fun currentWeekIndex(course: CourseLite): Int {
        val weeks = ((LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toEpochDay() - startMonday().toEpochDay()) / 7).toInt()
        return weeks.coerceIn(0, course.totalWeeks - 1)
    }

    fun weekStats(course: CourseLite, weekIndex: Int): WeekStats {
        val done = sp.getStringSet("done_days", emptySet()) ?: emptySet()
        val totalDone = done.size
        val weekDone = (totalDone % course.daysPerWeek).coerceAtLeast(0)
        return WeekStats(
            weekIndex = weekIndex,
            totalWeeks = course.totalWeeks,
            weekDone = weekDone,
            weekTarget = course.daysPerWeek,
            totalDone = totalDone,
            totalTarget = course.totalWeeks * course.daysPerWeek
        )
    }

    /** Called whenever PlanTasksStore changes to mirror course progress. */
    fun syncFromPlanTasksStore(date: LocalDate = LocalDate.now()) {
        val plan = PlanTasksStore(context.applicationContext)
        val tasks = plan.getTasks(date)
        val planned = tasks.filter { it.type != "synthetic" }
        if (planned.isEmpty()) return
        val allDone = planned.all { it.completed }
        val set = sp.getStringSet("done_days", mutableSetOf())!!.toMutableSet()
        if (allDone) set.add(date.toString()) else set.remove(date.toString())
        sp.edit { putStringSet("done_days", set) }
    }
}

/**
 * Parent should host its own TopAppBar. This composable draws only the body.
 */
@Composable
fun ExerciseCoursesScreen(
    modifier: Modifier = Modifier,
    // Optional callbacks. Defaults show Toast so taps are visible without navigation wiring.
    onContinue: () -> Unit = {},
    onSwitchCourse: () -> Unit = {},
    onPreviewCourse: (CourseLite) -> Unit = {},
    onJoinCourse: (CourseLite) -> Unit = {}
) {
    val ctx = LocalContext.current
    val toast = remember { { msg: String -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() } }

    // Active course (simple single-course model for now)
    val activeCourse = remember {
        CourseLite(
            id = "fat_loss_4w",
            title = "4-week Fat Loss Journey",
            totalWeeks = 4,
            daysPerWeek = 5
        )
    }
    val progress = remember { CourseProgressStore(ctx.applicationContext) }

    // Receive updates from PlanTasksStore → reflect into course progress
    var version by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { progress.ensureSeed(activeCourse) }
    DisposableEffect(Unit) {
        val planStore = PlanTasksStore(ctx.applicationContext)
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            progress.syncFromPlanTasksStore(LocalDate.now())
            version++
        }
        planStore.addOnChangeListener(l)
        onDispose { planStore.removeOnChangeListener(l) }
    }

    // Recompute stats on every sync tick
    val weekIndex = remember(version) { progress.currentWeekIndex(activeCourse) }
    val stats = remember(version) { progress.weekStats(activeCourse, weekIndex) }

    // Discover list (more items as you asked)
    val discoverList = remember {
        listOf(
            CourseLite("fat_loss_4w", "4-week Fat Loss Journey", 4, 5),
            CourseLite("lean_strength_6w", "Lean Strength • 6 weeks", 6, 4),
            CourseLite("endurance_8w", "8-week Endurance Base", 8, 4),
            CourseLite("mobility_3w", "Mobility Reset • 3 weeks", 3, 3),
            CourseLite("core_30d", "Core Builder • 30 days", 4, 7) // 4w × 7d
        )
    }

    // Body without top bar (so no duplicate header)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("Courses", style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold)
        }

        // Active course summary card
        item {
            ActiveCourseCard(
                course = activeCourse,
                stats = stats,
                onContinue = {
                    toast("Continue tapped")
                    onContinue()
                },
                onSwitch = {
                    toast("Switch tapped")
                    onSwitchCourse()
                }
            )
        }

        item { Text("My courses", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold) }

        // Mini card for the same active course (kept by your design)
        item {
            MyCourseMiniCard(
                course = activeCourse,
                stats = stats,
                isActive = true,
                onGo = {
                    toast("Go tapped")
                    onContinue()
                }
            )
        }

        item { Text("Discover", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold) }

        // More Discover items
        items(discoverList) { c ->
            DiscoverCourseCard(
                course = c,
                onPreview = {
                    toast("Preview • ${c.title}")
                    onPreviewCourse(c)
                },
                onJoin = {
                    toast("Join • ${c.title}")
                    onJoinCourse(c)
                }
            )
        }
    }
}

@Composable
private fun ActiveCourseCard(
    course: CourseLite,
    stats: WeekStats,
    onContinue: () -> Unit,
    onSwitch: () -> Unit
) {
    Card(elevation = 4.dp, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colors.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(course.title, fontWeight = FontWeight.SemiBold)
                    Text("Week ${stats.weekIndex + 1} of ${stats.totalWeeks}", style = MaterialTheme.typography.caption)
                }
                OutlinedButton(onClick = onSwitch) { Text("Switch") }
            }

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = if (stats.totalTarget == 0) 0f else stats.totalDone / stats.totalTarget.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("${stats.totalDone}/${stats.totalTarget} days", style = MaterialTheme.typography.caption)

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colors.primary)
                Spacer(Modifier.width(6.dp))
                Text("This week: ${stats.weekDone}/${stats.weekTarget}", style = MaterialTheme.typography.body2)
            }

            Spacer(Modifier.height(8.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = onContinue) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Continue")
            }
        }
    }
}

@Composable
private fun MyCourseMiniCard(
    course: CourseLite,
    stats: WeekStats,
    isActive: Boolean,
    onGo: () -> Unit
) {
    Card(elevation = 2.dp, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(course.title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = if (stats.totalTarget == 0) 0f else stats.totalDone / stats.totalTarget.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(enabled = isActive, onClick = { /* no-op */ }) { Text(if (isActive) "Active" else "Inactive") }
                Button(onClick = onGo) { Text("Go") }
            }
        }
    }
}

@Composable
private fun DiscoverCourseCard(
    course: CourseLite,
    onPreview: () -> Unit,
    onJoin: () -> Unit
) {
    Card(elevation = 3.dp, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colors.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(course.title, fontWeight = FontWeight.SemiBold)
                    Text("${course.totalWeeks} weeks · ${course.daysPerWeek} days/wk", style = MaterialTheme.typography.caption)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                // Simple copy per course id just to make cards feel different
                when (course.id) {
                    "lean_strength_6w" -> "Progressive strength + short cardio. Build lean muscles with 4 sessions/week."
                    "endurance_8w" -> "Aerobic base, long Z2 days, one tempo. Ideal for runners and cyclists."
                    "mobility_3w" -> "Daily mobility flows for joints & posture. Short and restorative."
                    "core_30d" -> "Everyday core micro-workouts to reinforce stability."
                    else -> "Balanced cardio + strength with mobility finisher. Great for beginners."
                },
                style = MaterialTheme.typography.body2
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreview) { Text("Preview") }
                Button(onClick = onJoin) { Text("Join") }
            }
        }
    }
}
