package com.example.myhealth.presentation.screen.mind

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

/**
 * MindSessionTimerScreen
 *
 * A guided mindfulness session screen with a countdown timer.
 * - title: practice name (e.g., "Box Breathing")
 * - minutes: duration in minutes
 * - Provides Start, Pause, Reset, and Finish buttons.
 * - When finished, saves session to database via ViewModel and returns.
 */
@Composable
fun MindSessionTimerScreen(
    title: String,
    minutes: Int,
    onBack: () -> Unit,
    vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    var running by remember { mutableStateOf(false) }       // Timer running state
    var secondsLeft by remember { mutableStateOf(minutes * 60) }

    // Countdown logic
    LaunchedEffect(running, secondsLeft) {
        if (running && secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }

    // Convert remaining time to MM:SS format
    val mm = secondsLeft / 60
    val ss = secondsLeft % 60
    val timeText = "%02d:%02d".format(mm, ss)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(24.dp)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Guided Timer", style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold))
            Text(timeText, style = MaterialTheme.typography.h3, color = MaterialTheme.colors.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { running = !running }) {
                    Text(if (running) "Pause" else "Start")
                }
                OutlinedButton(onClick = {
                    running = false
                    secondsLeft = minutes * 60
                }) { Text("Reset") }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    running = false
                    vm.addSession(minutes, title.lowercase())
                    onBack()
                },
                enabled = !running || secondsLeft == 0
            ) {
                Text("Finish")
            }
        }
    }
}
