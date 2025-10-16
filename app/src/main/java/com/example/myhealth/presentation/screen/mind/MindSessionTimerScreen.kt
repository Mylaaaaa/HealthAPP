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
import java.time.LocalDate

/**
 * MindSessionTimerScreen
 *
 * - title: practice name ("Box Breathing", etc.)
 * - minutes: planned duration (3/5/10)
 * - dateIso: ISO date string (yyyy-MM-dd) to save into
 * - tag: normalized tag ("breathing", "bodyscan", ...)
 * - autoStart: if true, the timer starts automatically (used by Quick action)
 */
@Composable
fun MindSessionTimerScreen(
    title: String,
    minutes: Int,
    dateIso: String,
    tag: String,
    autoStart: Boolean,
    onBack: () -> Unit,
    vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    var running by remember { mutableStateOf(autoStart) }
    var secondsLeft by remember { mutableStateOf(minutes * 60) }

    // Countdown effect
    LaunchedEffect(running, secondsLeft) {
        if (running && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Guided Timer", style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold))
            Text(timeText, style = MaterialTheme.typography.h3, color = MaterialTheme.colors.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { running = !running }) {
                    Text(if (running) "Pause" else if (secondsLeft == minutes * 60) "Start" else "Resume")
                }
                OutlinedButton(onClick = {
                    running = false
                    secondsLeft = minutes * 60
                }) { Text("Reset") }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    // Ensure we save to the date that was selected in overview
                    val date = runCatching { LocalDate.parse(dateIso) }.getOrElse { LocalDate.now() }
                    vm.setDate(date)
                    vm.addSession(minutes, tag)
                    onBack()
                },
                enabled = !running || secondsLeft == 0
            ) {
                Text("Finish")
            }
        }
    }
}
