package com.example.myhealth.presentation.screen.mind

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * MindOverviewScreen (updated)
 * - Top bar: back + "Mindfulness" + only Settings (line-chart removed).
 * - Body no big "Mindfulness" title; full white background.
 * - "7-day trend": removed "Details" button; kept the chart card.
 * - "Guided practice": clicking an item navigates to a dedicated session/timer screen.
 */
@Composable
fun MindOverviewScreen(
    onOpenSession: (title: String, mins: Int) -> Unit, // navigate to guided session/timer
    onOpenSettings: () -> Unit,                         // navigate to date settings page
    onBack: () -> Unit,                                 // popBackStack()
    vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val todayMinutes by vm.todayMinutes.collectAsState()
    val weekly by vm.weeklyMinutes.collectAsState()
    val streak by vm.streakDays.collectAsState()
    val reminder by vm.reminderEnabled.collectAsState()
    val mood by vm.lastMood.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = Color.White,
                elevation = 0.dp,
                title = { Text("Mindfulness") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Only keep Settings (open date-settings page); removed line-chart. :contentReference[oaicite:2]{index=2}
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Reminder/Date settings")
                    }
                }
            )
        },
        backgroundColor = Color.White
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today
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
                        Text(
                            "$todayMinutes / 10 min",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                null,
                                tint = Color(0xFFFF7043)
                            )
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

            // Quick actions
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
                    title = "Planner",
                    subtitle = "date",
                    icon = Icons.Filled.Event,
                    tint = Color(0xFF7C4DFF),
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                )
            }

            // Mood check-in (inline)
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

            // 7-day trend (no "Details" button now). :contentReference[oaicite:3]{index=3}
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("7-day trend", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    BarMiniChart(values = weekly.map { it.toFloat() })
                }
            }

            // Guided practice (renamed; navigates to timer screen)
            val sessions = listOf(
                MindSession("s1", "Box Breathing", 3, "focus", Color(0xFF26C6DA)),
                MindSession("s2", "Body Scan", 5, "relax", Color(0xFF7C4DFF)),
                MindSession("s3", "Morning Calm", 4, "energy", Color(0xFFFF7043)),
                MindSession("s4", "Sleep Wind-down", 6, "sleep", Color(0xFF66BB6A))
            )
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("Guided practice", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sessions) { s ->
                            GuidedChip(s = s, onStart = {
                                // Navigate to the dedicated guided session (timer) screen
                                onOpenSession(s.title, s.mins)
                            })
                        }
                    }
                }
            }
        }
    }
}
