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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.util.*

/**
 * MindOverviewScreen
 *
 * - White TopAppBar (no blue secondary appbar): back arrow + "Mindfulness" + calendar icon.
 * - REMOVED the inline date quick bar (Yesterday/Today/Tomorrow) per your request.
 * - RESTORED "Planner" quick card.
 * - 7-day trend shows chart ONLY (removed "Details").
 * - Guided sessions open your timer page via onOpenSession(title, mins).
 */
@Composable
fun MindOverviewScreen(
    onBack: () -> Unit,
    onOpenSession: (String, Int) -> Unit = { _, _ -> },   // open timer screen
    vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val ctx = LocalContext.current

    // VM-backed reactive states. VM defaults date to today; calendar changes it.
    val selectedDate by vm.today.collectAsState()
    val todayMinutes by vm.todayMinutes.collectAsState()
    val weekly by vm.weeklyMinutes.collectAsState()
    val streak by vm.streakDays.collectAsState()
    val reminder by vm.reminderEnabled.collectAsState()
    val mood by vm.lastMood.collectAsState()

    // Calendar picker (kept in the top-right action)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFFF7043))
                            Spacer(Modifier.width(6.dp))
                            Text("Streak: $streak days")
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Reminders", style = MaterialTheme.typography.caption)
                        Switch(checked = reminder, onCheckedChange = vm::setReminder)
                    }
                }
            }

            // Quick actions (Breathing / Mood / Planner restored)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Breathing",
                    subtitle = "1–3 min",
                    icon = Icons.Filled.Psychology,
                    tint = MaterialTheme.colors.primary,
                    onClick = { onOpenSession("Box Breathing", 3) },
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
                QuickActionCard(
                    title = "Planner",               // restored
                    subtitle = "schedule",
                    icon = Icons.Filled.Event,
                    tint = Color(0xFF7C4DFF),
                    onClick = ::openDatePicker,      // reuse calendar
                    modifier = Modifier.weight(1f)
                )
            }

            // Mood picker (inline)
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
                                    color = if (selected) m.tint
                                    else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 7-day trend (Details removed)
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("7-day trend", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    BarMiniChart(values = weekly.map { it.toFloat() })
                }
            }

            // Guided sessions → open timer
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
                                onStart = { session -> onOpenSession(session.title, session.mins) }
                            )
                        }
                    }
                }
            }
        }
    }
}
