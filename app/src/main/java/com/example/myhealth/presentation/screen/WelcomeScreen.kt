package com.example.myhealth.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myhealth.presentation.navigation.Screen

/**
 * Home screen of the app.
 * Shows the welcome message and provides quick navigation entries.
 */
@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome text section
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Welcome to My Health!",
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Use the menu or quick entries below to explore different features of My Health.",
            style = MaterialTheme.typography.body2
        )

        Spacer(Modifier.height(16.dp))

        // Quick navigation cards
        Text(
            text = "Quick entries",
            style = MaterialTheme.typography.overline,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )

        val entries = listOf(
            Screen.Dashboard,
            Screen.ExerciseSessions,
            Screen.SleepSessions,
            Screen.InputReadings,
            Screen.Reports,
            Screen.Nutrition,
            Screen.Mind,
            Screen.SettingsScreen
        ).filter { it.hasMenuItem }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries.size) { idx ->
                val s = entries[idx]
                HomeNavCard(
                    title = s.name.replaceFirstChar { it.uppercase() },
                    subtitle = s.route,
                    onClick = {
                        navController.navigate(s.route) {
                            navController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) { saveState = true }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

/**
 * Single card item for navigation entry.
 */
@Composable
private fun HomeNavCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.subtitle1)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
