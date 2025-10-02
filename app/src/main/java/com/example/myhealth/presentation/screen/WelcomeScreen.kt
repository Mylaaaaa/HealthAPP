package com.example.myhealth.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
 * Shows navigation cards for all main features.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.home),
            style = MaterialTheme.typography.h6
        )

        Spacer(Modifier.height(16.dp))

        // List of entries shown on Home
        val entries = listOf(
            Screen.Dashboard,
            Screen.ExerciseSessions,
            Screen.SleepSessions,
            Screen.Nutrition,
            Screen.Mind,
            Screen.Reports,
            Screen.InputReadings,
            Screen.SettingsScreen
        ).filter { it.hasMenuItem }

        // 2-column grid of navigation cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries) { s ->
                HomeNavCard(
                    title = stringResource(id = s.titleId),
                    onClick = { navController.navigate(s.route) }
                )
            }
        }
    }
}

/**
 * Individual navigation card for the Home screen.
 */
@Composable
private fun HomeNavCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        elevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1
            )
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
