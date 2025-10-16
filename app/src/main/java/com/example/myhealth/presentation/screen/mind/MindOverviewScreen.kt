package com.example.myhealth.presentation.screen.mind
import androidx.compose.runtime.mutableIntStateOf
import android.app.Application
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.util.*

/**
 * MindOverviewScreen (Plan B)
 *
 * - White TopAppBar: back + "Mindfulness" + calendar icon (date picker).
 * - Quick actions: Breathing (one-tap 3 min, auto-start), Mood (check-in).  // Planner removed.
 * - Guided sessions: tapping shows a config dialog (description + duration 3/5/10 + Start).
 * - 7-day trend has NO "Details" button.
 * - When starting a session, we also pass the currently selected date to the timer screen.
 */
@Composable
fun MindOverviewScreen(
    onBack: () -> Unit,
    onOpenSession: (title: String, mins: Int, date: LocalDate, tag: String, autoStart: Boolean) -> Unit = { _, _, _, _, _ -> },
    vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val ctx = LocalContext.current

    // ViewModel-driven reactive states
    val selectedDate by vm.today.collectAsState()
    val todayMinutes by vm.todayMinutes.collectAsState()
    val weekly by vm.weeklyMinutes.collectAsState()
    val streak by vm.streakDays.collectAsState()
    val reminder by vm.reminderEnabled.collectAsState()
    val mood by vm.lastMood.collectAsState()

    // Simple config dialog state for Guided session
    var dialog by remember { mutableStateOf<GuidedConfigDialogState?>(null) }

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate.year)
            set(Calendar.MONTH, selectedDate.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, selectedDate.dayOfMonth)
        }
        DatePickerDialog(
            ctx,
            { _, y, m, d -> vm.setDate(LocalDate.of(y, m + 1, d)) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mindfulness") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::openDatePicker) {
                        Icon(Icons.Filled.Event, contentDescription = "Select date")
                    }
                },
                backgroundColor = Color.White,
                elevation = 0.dp
            )
        },
        backgroundColor = Color.White
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today summary
            Card(elevation = 4.dp) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Ring(
                        progress = safeRatio(todayMinutes.toFloat(), 10f),
                        size = 64.dp,
                        stroke = 8.dp,
                        tint = MaterialTheme.colors.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Today", style = MaterialTheme.typography.subtitle1)
                        Text("$todayMinutes / 10 min", color = Color.Gray)
                        Spacer(Modifier.height(6.dp))
                        Text("Streak: $streak days")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Reminders", style = MaterialTheme.typography.caption)
                        Switch(checked = reminder, onCheckedChange = vm::setReminder)
                    }
                }
            }

            // Quick actions (Planner removed)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // One-tap breathing: go straight to timer, auto start, default 3 min
                QuickActionCard(
                    title = "Breathing",
                    subtitle = "1–3 min",
                    icon = Icons.Filled.Psychology,
                    tint = MaterialTheme.colors.primary,
                    onClick = {
                        onOpenSession("Box Breathing", 3, selectedDate, "breathing", /*autoStart=*/true)
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Mood",
                    subtitle = mood?.label ?: "check-in",
                    icon = Icons.Filled.Psychology,
                    tint = Color(0xFF26C6DA),
                    onClick = { vm.checkInMood(Mood.GOOD) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Mood picker
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("Mood check-in", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in enumValues<Mood>()) {
                            val selected = (mood == m)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) m.tint.copy(alpha = 0.14f)
                                        else MaterialTheme.colors.surface
                                    )
                                    .clickable { vm.checkInMood(m) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(m.glyph)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    m.label,
                                    color = if (selected) m.tint else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 7-day trend (no Details)
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("7-day trend", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    BarMiniChart(values = weekly.map { it.toFloat() })
                }
            }

            // Guided sessions: opens a config dialog instead of going straight to timer
            val sessions = listOf(
                MindSession("s1", "Box Breathing", 3, "focus", Color(0xFF26C6DA)),
                MindSession("s2", "Body Scan", 5, "relax", Color(0xFF7C4DFF))
            )
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("Guided sessions", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sessions) { s ->
                            GuidedChip(
                                s = s,
                                onStart = { session ->
                                    // Open config dialog: user can pick duration then start
                                    dialog = GuidedConfigDialogState(
                                        title = session.title,
                                        // default minutes based on item
                                        minutes = session.mins,
                                        // normalized tags for analytics
                                        tag = if (session.title.contains("scan", ignoreCase = true)) "bodyscan" else "breathing"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Configuration Dialog for Guided sessions
    dialog?.let { d ->
        GuidedConfigDialog(
            state = d,
            onDismiss = { dialog = null },
            onStart = { chosenMin, chosenTag ->
                onOpenSession(d.title, chosenMin, selectedDate, chosenTag, /*autoStart=*/false)
                dialog = null
            }
        )
    }
}

/** Simple state holder for the guided config dialog. */
private data class GuidedConfigDialogState(
    val title: String,
    val minutes: Int,
    val tag: String
)

/** A plain AlertDialog that lets user pick duration and confirm start. */
@Composable
private fun GuidedConfigDialog(
    state: GuidedConfigDialogState,
    onDismiss: () -> Unit,
    onStart: (mins: Int, tag: String) -> Unit
) {
    // use Int-optimized state holder (IDE 的建议)
    var mins by remember { mutableIntStateOf(state.minutes.coerceIn(listOf(3, 5, 10))) }
    val tag = state.tag

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {   // ✅ 修复：verticalArrangement
                Text(
                    "A focused, guided practice. Choose your duration:",
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DurationChip(current = mins, value = 3) { mins = 3 }
                    DurationChip(current = mins, value = 5) { mins = 5 }
                    DurationChip(current = mins, value = 10) { mins = 10 }
                }
                Text("Tip: shorter sessions are great for busy days.")
            }
        },
        confirmButton = {
            Button(onClick = { onStart(mins, tag) }) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DurationChip(current: Int, value: Int, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = if (current == value) ButtonDefaults.outlinedBorder else null
    ) {
        Text("$value min")
    }
}


// helper to avoid lint error if list doesn't contain value
private fun Int.coerceIn(choices: List<Int>) = if (this in choices) this else choices.first()
