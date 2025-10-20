package com.zihanwang.myhealth.presentation.screen.mind

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

/**
 * Minimal guided session screen with countdown timer.
 */
@Composable
fun MindSessionScreen(
    title: String,
    minutes: Int,
    onBack: () -> Unit,
    vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    var running by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(minutes * 60) }

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
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                elevation = 0.dp,
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Guided timer",
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(timeText, style = MaterialTheme.typography.h3, color = MaterialTheme.colors.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { running = !running }) {
                    Text(if (running) "Pause" else "Start")
                }
                OutlinedButton(
                    onClick = { running = false; secondsLeft = minutes * 60 }
                ) { Text("Reset") }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    vm.addSession(minutes, title.lowercase())
                    onBack()
                },
                enabled = secondsLeft == 0 || !running
            ) { Text("Finish") }
        }
    }
}
