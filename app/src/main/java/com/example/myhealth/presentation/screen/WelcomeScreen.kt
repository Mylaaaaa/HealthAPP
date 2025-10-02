package com.example.myhealth.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myhealth.R
import com.example.myhealth.presentation.navigation.Screen
import com.example.myhealth.presentation.theme.HealthConnectTheme

/**
 * Home screen (formerly WelcomeScreen).
 * Shows greeting + navigation grid + quick actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 👋 Greeting section
        Text(
            text = "👋 Welcome back, User!",
            style = MaterialTheme.typography.h6
        )

        Spacer(Modifier.height(16.dp))

        // 🔗 Quick actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                text = "🏃 Start Exercise",
                onClick = { navController.navigate(Screen.ExerciseSessions.route) },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                text = "⚖️ Record Weight",
                onClick = { navController.navigate(Screen.InputReadings.route) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // 📌 Navigation grid entries (with icons)
        val entries = listOf(
            Triple(Screen.Dashboard, Icons.Default.Dashboard, R.string.dashboard),
            Triple(Screen.ExerciseSessions, Icons.Default.FitnessCenter, R.string.exercise_sessions),
            Triple(Screen.SleepSessions, Icons.Default.Hotel, R.string.sleep_sessions),
            Triple(Screen.Nutrition, Icons.Default.LocalDining, R.string.nutrition),
            Triple(Screen.Mind, Icons.Default.SelfImprovement, R.string.mind),
            Triple(Screen.Reports, Icons.Default.Assessment, R.string.reports),
            Triple(Screen.InputReadings, Icons.Default.MonitorWeight, R.string.input_readings),
            Triple(Screen.SettingsScreen, Icons.Default.Settings, R.string.settings)
        )

        // 🧩 Grid layout
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries) { (screen, icon, titleId) ->
                HomeNavCard(
                    title = stringResource(id = titleId),
                    icon = icon,
                    onClick = { navController.navigate(screen.route) }
                )
            }
        }
    }
}

/**
 * Single quick action button.
 */
@Composable
private fun QuickActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(text)
    }
}

/**
 * Individual navigation card with icon + title.
 */
@Composable
private fun HomeNavCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        elevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colors.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.subtitle1)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    HealthConnectTheme {
        val navController = rememberNavController()
        WelcomeScreen(navController)
    }
}
