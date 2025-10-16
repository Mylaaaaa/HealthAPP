package com.example.myhealth.presentation.screen.mind

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.Calendar

/**
 * MindOverviewScreen — Plan B (richer but focused)
 *
 * What this screen contains:
 *  - White TopAppBar: back + "Mindfulness" + calendar icon (date picker).
 *  - "Today" summary with a small "Remaining" pill and a soft motivation line.
 *  - Quick Start row: a single one-tap 3-min breathing (auto-start).
 *  - Mood check-in row.
 *  - Recent moods: last 3 calendar days (today / yesterday / 2 days ago).
 *      NOTE: we reliably show today's mood; the other two fallback to "—"
 *      if historical mood lookup is not available in the current VM API.
 *  - Guided sessions: opens a config dialog (choose 3/5/10 min), then timer.
 *  - 7-day trend is NOT shown here (moved to State).
 *
 * Navigation contract (unchanged):
 *  - onOpenSession(title, mins, date, tag, autoStart)
 *      -> MindRootScreen already passes these to your timer route.
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

    // VM reactive states
    val selectedDate by vm.today.collectAsState()
    val todayMinutes by vm.todayMinutes.collectAsState()
    val weekly by vm.weeklyMinutes.collectAsState()
    val streak by vm.streakDays.collectAsState()
    val reminder by vm.reminderEnabled.collectAsState()
    val moodToday by vm.lastMood.collectAsState() // mood for selected day (today by default)

    // Dialog state for Guided sessions
    var dialog by remember { mutableStateOf<GuidedConfigDialogState?>(null) }

    // Open system DatePicker and update VM date
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

    // Small helpers
    val dailyGoal = 10
    val remaining = (dailyGoal - todayMinutes).coerceAtLeast(0)
    val motivation = when {
        remaining == 0 -> "Goal reached — great job!"
        remaining <= 2 -> "Only $remaining min to hit today’s goal."
        else -> "You’re on day $streak — keep it up!"
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
            // --- Today summary -------------------------------------------------
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Ring(
                            progress = safeRatio(todayMinutes.toFloat(), dailyGoal.toFloat()),
                            size = 64.dp,
                            stroke = 8.dp,
                            tint = MaterialTheme.colors.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Today", style = MaterialTheme.typography.subtitle1)
                            Text("$todayMinutes / $dailyGoal min", color = Color.Gray)
                            Spacer(Modifier.height(6.dp))
                            // soft motivation line
                            Text(motivation, color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f))
                        }
                        // remaining pill
                        if (remaining > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colors.primary.copy(alpha = 0.10f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Remaining $remaining", color = MaterialTheme.colors.primary)
                            }
                        }
                    }
                }
            }

            // --- Quick Start: one-tap 3-min breathing (auto-start) -------------
            Card(elevation = 4.dp) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Quick start", style = MaterialTheme.typography.subtitle1, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            onOpenSession("Box Breathing", 3, selectedDate, "breathing", /*autoStart=*/true)
                        }
                    ) { Text("3 min") }
                }
            }

            // --- Quick actions: Mood (Planner removed) -------------------------
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Breathing",
                    subtitle = "1–3 min",
                    icon = Icons.Filled.Psychology,
                    tint = MaterialTheme.colors.primary,
                    onClick = {
                        onOpenSession("Box Breathing", 3, selectedDate, "breathing", true)
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Mood",
                    subtitle = moodToday?.label ?: "check-in",
                    icon = Icons.Filled.Psychology,
                    tint = Color(0xFF26C6DA),
                    onClick = { vm.checkInMood(Mood.GOOD) },
                    modifier = Modifier.weight(1f)
                )
            }

            // --- Mood check-in (inline row) -----------------------------------
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("Mood check-in", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in enumValues<Mood>()) {
                            val selected = (moodToday == m)
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
                                    color = if (selected) m.tint
                                    else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // --- Recent moods (today / yesterday / 2 days ago) -----------------
            // NOTE: if VM doesn't expose historical mood lookup yet, we show today's mood
            // and fallback "—" for the previous two days. It still fills space meaningfully.
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Recent moods", style = MaterialTheme.typography.subtitle1)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val days = listOf(0L, 1L, 2L)
                        days.forEach { back ->
                            val date = selectedDate.minusDays(back)
                            val label = weekdayShort(date)
                            val moodGlyph = if (back == 0L) (moodToday?.glyph ?: "—") else "—"
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colors.primary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) { Text(moodGlyph) }
                                Spacer(Modifier.height(4.dp))
                                Text(label, color = Color.Gray)
                            }
                        }
                    }
                    Text(
                        "Tip: logging mood daily improves insights.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // --- Guided sessions (open config dialog, then timer) --------------
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
                                    dialog = GuidedConfigDialogState(
                                        title = session.title,
                                        minutes = session.mins,
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

    // --- Guided config dialog -----------------------------------------------
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

/* ------------------------------ Dialog & helpers ------------------------------ */

private data class GuidedConfigDialogState(
    val title: String,
    val minutes: Int,
    val tag: String
)

@Composable
private fun GuidedConfigDialog(
    state: GuidedConfigDialogState,
    onDismiss: () -> Unit,
    onStart: (mins: Int, tag: String) -> Unit
) {
    var mins by remember { mutableIntStateOf(state.minutes.coerceIn(listOf(3, 5, 10))) }
    val tag = state.tag

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        confirmButton = { Button(onClick = { onStart(mins, tag) }) { Text("Start") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DurationChip(current: Int, value: Int, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = if (current == value) ButtonDefaults.outlinedBorder else null
    ) { Text("$value min") }
}

// Helper: ensure a default if the provided minutes is not in [3,5,10]
private fun Int.coerceIn(choices: List<Int>) = if (this in choices) this else choices.first()

// Helper: localized short weekday label (e.g., Mon, Tue)
private fun weekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
