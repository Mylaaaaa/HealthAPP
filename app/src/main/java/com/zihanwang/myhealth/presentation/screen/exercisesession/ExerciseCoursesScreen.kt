package com.zihanwang.myhealth.presentation.screen.exercisesession

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

/* --------------------------------------------------------------------------
 * Lightweight models & storage – kept in this file so you don't touch others.
 * Marked `internal` to avoid "private in file" access errors from this file.
 * In a larger app, move them into your data layer and delete this section.
 * -------------------------------------------------------------------------- */

internal data class CourseLite(
    val id: String,
    val title: String,
    val weeks: Int,
    val daysPerWeek: Int
)

internal class CoursesRepository {
    // Simple in-memory catalog
    internal val all = listOf(
        CourseLite("fatloss_4w", "4-week Fat Loss Journey", 4, 5),
        CourseLite("lean_6w", "Lean Strength", 6, 4),
        CourseLite("hiit_3w", "HIIT Booster", 3, 3),
    )

    fun myCourses(): List<CourseLite> = listOf(all[0])           // pretend user has joined the first
    fun discover(): List<CourseLite> = all.drop(0)               // the rest are discoverable
    fun findById(id: String?): CourseLite? = all.firstOrNull { it.id == id }
}

internal class CoursePrefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("course_prefs", Context.MODE_PRIVATE)

    fun getActiveCourseId(): String? = sp.getString("active_course", null)
    fun setActiveCourseId(id: String) { sp.edit().putString("active_course", id).apply() }

    // Progress for top card visuals
    fun getWeekIndex(id: String?): Int = if (id == null) 0 else sp.getInt("wkIdx_$id", 0)
    fun getWeekDone(id: String?): Int = if (id == null) 0 else sp.getInt("wkDone_$id", 0)

    // Utility so Workout “mark all complete” can write back if you need later:
    fun setWeekDone(id: String, done: Int) { sp.edit().putInt("wkDone_$id", done).apply() }
}

/* ==========================================================================
 * MAIN, callback-driven implementation (reusable).
 *  - Keep this as the single source of truth for the screen UI.
 *  - Other overloads delegate to this to keep compatibility.
 * ========================================================================== */

@Composable
fun ExerciseCoursesScreen(
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    onBack: () -> Unit = {},
    onContinue: (courseId: String) -> Unit = {},
    onSwitch: (courseId: String) -> Unit = {},
    onJoin: (courseId: String) -> Unit = {},
    onPreview: (courseId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { CoursePrefs(context) }
    val repo = remember { CoursesRepository() }

    val scaffold = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    // Active course id is kept in prefs; reflect into Compose state
    var activeId by remember { mutableStateOf(prefs.getActiveCourseId()) }
    val activeCourse = remember(activeId) { repo.findById(activeId) }
    val myCourses = remember { repo.myCourses() }
    val discover = remember { repo.discover() }

    // Switch dialog
    var showSwitchDialog by remember { mutableStateOf(false) }
    var pendingSwitchId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        scaffoldState = scaffold,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Exercise sessions") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Active course card
            item {
                Text("Courses", style = MaterialTheme.typography.h6, modifier = Modifier.padding(bottom = 8.dp))
                ActiveCourseCard(
                    course = activeCourse,
                    prefs = prefs,
                    onContinue = { activeCourse?.id?.let(onContinue) },
                    onSwitch = {
                        // Find a candidate from "My courses" different from current active
                        pendingSwitchId = myCourses.firstOrNull { it.id != activeCourse?.id }?.id
                        if (pendingSwitchId != null) showSwitchDialog = true
                    }
                )
            }

            // My courses
            if (myCourses.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colors.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("My courses", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
                items(myCourses) { c ->
                    MyCourseItem(
                        course = c,
                        isActive = c.id == activeId,
                        onGo = {
                            // Make it the active one, then trigger Continue flow
                            prefs.setActiveCourseId(c.id)
                            activeId = c.id
                            onContinue(c.id)
                        }
                    )
                }
            }

            // Discover section
            if (discover.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, null, tint = MaterialTheme.colors.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Discover", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
                items(discover) { c ->
                    DiscoverItem(
                        course = c,
                        onPreview = { onPreview(c.id) },
                        onJoin = {
                            prefs.setActiveCourseId(c.id)
                            activeId = c.id
                            onJoin(c.id)
                            scope.launch { scaffold.snackbarHostState.showSnackbar("Joined: ${c.title}") }
                        }
                    )
                }
            }
        }
    }

    // Confirm dialog for switching active course
    if (showSwitchDialog && pendingSwitchId != null) {
        AlertDialog(
            onDismissRequest = { showSwitchDialog = false; pendingSwitchId = null },
            title = { Text("Switch active course?") },
            text = { Text("Your active course will be changed.") },
            confirmButton = {
                TextButton(onClick = {
                    val newId = pendingSwitchId!!
                    prefs.setActiveCourseId(newId)
                    // Update local state so UI reflects immediately
                    activeId = newId
                    onSwitch(newId)
                    showSwitchDialog = false
                    pendingSwitchId = null
                }) { Text("Switch") }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchDialog = false; pendingSwitchId = null }) { Text("Cancel") }
            }
        )
    }
}

/* ==========================================================================
 * Backward-compatible overloads – DO NOT delete.
 * These ensure you don't need to touch other files that already call the
 * screen with different parameters.
 * ========================================================================== */

/**
 * Old call sites that only passed a Modifier (or nothing) will compile here.
 * We keep the top bar OFF so the parent screen's app bar doesn't duplicate.
 */
@Composable
fun ExerciseCoursesScreen(modifier: Modifier = Modifier) {
    ExerciseCoursesScreen(
        modifier = modifier,
        showTopBar = false,      // avoid double top bars
        onBack = {},
        onContinue = {},         // keep as no-op to be safe
        onSwitch = {},
        onJoin = {},
        onPreview = {}
    )
}

/**
 * Call sites that pass a NavController will compile here.
 * Map actions to your real routes if they differ.
 */
@Composable
fun ExerciseCoursesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    ExerciseCoursesScreen(
        modifier = modifier,
        showTopBar = false,                       // parent top bar takes over
        onBack = { navController.popBackStack() },
        onContinue = { /* e.g. */ navController.navigate("workout") },
        onSwitch = { /* optional toast/log */ },
        onJoin = { /* after joining, go to workout */ navController.navigate("workout") },
        onPreview = { courseId -> navController.navigate("course_preview/$courseId") }
    )
}

/* ==========================================================================
 * UI pieces
 * ========================================================================== */

@Composable
private fun ActiveCourseCard(
    course: CourseLite?,
    prefs: CoursePrefs,
    onContinue: () -> Unit,
    onSwitch: () -> Unit
) {
    Card(elevation = 4.dp, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colors.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(course?.title ?: "No active course", fontWeight = FontWeight.SemiBold)
                    val wIdx = prefs.getWeekIndex(course?.id)
                    if (course != null) {
                        Text(
                            "Week ${wIdx + 1} of ${course.weeks}",
                            style = MaterialTheme.typography.body2,
                            color = Color.Gray
                        )
                    }
                }
                OutlinedButton(enabled = course != null, onClick = onSwitch) { Text("Switch") }
            }

            if (course != null) {
                val done = prefs.getWeekDone(course.id)
                val perWeek = course.daysPerWeek
                LinearProgressIndicator(
                    progress = (done / perWeek.toFloat()).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF2E7D32)) }
                    Spacer(Modifier.width(8.dp))
                    Text("This week: $done/${course.daysPerWeek}", style = MaterialTheme.typography.body2)
                }
            }

            Button(enabled = course != null, onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Continue")
            }
        }
    }
}

@Composable
private fun MyCourseItem(
    course: CourseLite,
    isActive: Boolean,
    onGo: () -> Unit
) {
    Card(elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(course.title, fontWeight = FontWeight.SemiBold)
                Text("Week 1/${course.weeks}", style = MaterialTheme.typography.caption, color = Color.Gray)
            }
            OutlinedButton(enabled = isActive, onClick = { /* already active */ }) {
                Text(if (isActive) "Active" else "—")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onGo) { Text("Go") }
        }
    }
}

@Composable
private fun DiscoverItem(
    course: CourseLite,
    onPreview: () -> Unit,
    onJoin: () -> Unit
) {
    Card(elevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colors.primary) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${course.title} · ${course.weeks} weeks", fontWeight = FontWeight.SemiBold)
                    Text(
                        "${course.weeks} weeks · ${course.daysPerWeek} days/wk",
                        style = MaterialTheme.typography.caption, color = Color.Gray
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f)) { Text("Preview") }
                Button(onClick = onJoin, modifier = Modifier.weight(1f)) { Text("Join") }
            }
        }
    }
}
