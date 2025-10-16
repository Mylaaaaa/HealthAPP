package com.example.myhealth.presentation.screen.mind

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack   // ← import back icon
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings   // ← import settings icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MindOverviewScreen(
    onOpenState: () -> Unit,         // navigate to MindStateScreen
    onOpenTimeSettings: () -> Unit,  // open system time picker
    onBack: () -> Unit,              // popBackStack
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Keep only Settings icon (open time picker) — removed Timeline. :contentReference[oaicite:1]{index=1}
                    IconButton(onClick = onOpenTimeSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Set reminder time")
                    }
                }
            )
        },
        backgroundColor = Color.White
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today card (kept from your original structure) :contentReference[oaicite:2]{index=2}
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

            // Quick actions (kept) :contentReference[oaicite:3]{index=3}
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Breathing",
                    subtitle = "1–3 min",
                    icon = Icons.Filled.Psychology,
                    tint = MaterialTheme.colors.primary,
                    onClick = { vm.addSession(3, "breathing") },
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
                    subtitle = "schedule",
                    icon = Icons.Filled.Event,
                    tint = Color(0xFF7C4DFF),
                    onClick = onOpenTimeSettings, // reuse time picker
                    modifier = Modifier.weight(1f)
                )
            }

            // Mood inline picker (your original inline UI; no external MoodPicker) :contentReference[oaicite:4]{index=4}
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

            // 7-day trend (Details navigates to State) :contentReference[oaicite:5]{index=5}
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("7-day trend", style = MaterialTheme.typography.subtitle1)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onOpenState) {
                            Text("Details")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    BarMiniChart(values = weekly.map { it.toFloat() })
                }
            }

            // Guided sessions (each starts then go to State) :contentReference[oaicite:6]{index=6}
            val sessions = listOf(
                MindSession("s1", "Box Breathing", 3, "focus", Color(0xFF26C6DA)),
                MindSession("s2", "Body Scan", 5, "relax", Color(0xFF7C4DFF)),
                MindSession("s3", "Morning Calm", 4, "energy", Color(0xFFFF7043)),
                MindSession("s4", "Sleep Wind-down", 6, "sleep", Color(0xFF66BB6A))
            )
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("Guided sessions", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sessions) { s ->
                            GuidedChip(s = s, onStart = {
                                vm.addSession(s.mins, s.tag)
                                onOpenState()
                            })
                        }
                    }
                }
            }
        }
    }
}
