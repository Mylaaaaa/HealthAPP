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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlinx.coroutines.launch // for showing Snackbar in a coroutine

/**
 * Timer screen that saves to the provided date.
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

    LaunchedEffect(running, secondsLeft) {
        if (running && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    val mm = secondsLeft / 60
    val ss = secondsLeft % 60
    val timeText = "%02d:%02d".format(mm, ss)
    val scaffold = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    Scaffold(
        // Inject scaffold state so we can show Snackbars
        scaffoldState = scaffold,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // a11y: describe the back action for screen readers
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = MaterialTheme.colors.surface, contentColor = MaterialTheme.colors.onSurface, elevation = 0.dp
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Guided Timer", style = MaterialTheme.typography.h6)
            Text(timeText, style = MaterialTheme.typography.h3, color = MaterialTheme.colors.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { running = !running }) {
                    Text(if (running) "Pause" else if (secondsLeft == minutes * 60) "Start" else "Resume")
                }
                OutlinedButton(onClick = { running = false; secondsLeft = minutes * 60 }) { Text("Reset") }
            }

            Button(
                onClick = {
                    val date = runCatching { LocalDate.parse(dateIso) }.getOrElse { LocalDate.now() }
                    vm.setDate(date)

                    // Save the ACTUAL practiced minutes instead of the target minutes.
                    // This avoids over-reporting if the user ends early.
                    val actualMinutes = ((minutes * 60 - secondsLeft).coerceAtLeast(0)) / 60
                    val toSave = actualMinutes.coerceAtLeast(1) // minimum 1 minute

                    // Show user feedback with Snackbars
                    scope.launch {
                        runCatching { vm.addSession(toSave, tag) }
                            .onSuccess { scaffold.snackbarHostState.showSnackbar("Saved") }
                            .onFailure { scaffold.snackbarHostState.showSnackbar("Save failed") }
                        onBack()
                    }
                },
                enabled = !running || secondsLeft == 0
            ) { Text("Finish") }
        }
    }
}
