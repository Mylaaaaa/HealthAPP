package com.example.myhealth.presentation.screen.mind

import androidx.compose.foundation.layout.padding   // ← add padding import
import androidx.compose.ui.Modifier               // ← add Modifier import
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private const val ROUTE_MIND_OVERVIEW = "mind_overview"
private const val ROUTE_MIND_STATE = "mind_state"

/**
 * MindRootScreen
 * - Keeps your two tabs (Overview/State). :contentReference[oaicite:7]{index=7}
 * - Delegates TopAppBar to the Overview screen (so it shows back/title/settings there).
 * - Uses white background and correct imports (fixes unresolved 'padding').
 */
@Composable
fun MindRootScreen(
    onBack: () -> Unit,
    onOpenTimeSettings: () -> Unit
) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()

    Scaffold(
        backgroundColor = Color.White,
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    selected = current?.destination?.route == ROUTE_MIND_OVERVIEW,
                    onClick = {
                        nav.navigate(ROUTE_MIND_OVERVIEW) {
                            launchSingleTop = true
                            popUpTo(ROUTE_MIND_OVERVIEW) { inclusive = false }
                        }
                    },
                    icon = { Icon(Icons.Filled.Psychology, contentDescription = null) },
                    label = { Text("Overview") }
                )
                BottomNavigationItem(
                    selected = current?.destination?.route == ROUTE_MIND_STATE,
                    onClick = { nav.navigate(ROUTE_MIND_STATE) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Timeline, contentDescription = null) },
                    label = { Text("State") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = ROUTE_MIND_OVERVIEW,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_MIND_OVERVIEW) {
                // Pass callbacks required by the updated Overview
                MindOverviewScreen(
                    onOpenState = { nav.navigate(ROUTE_MIND_STATE) },
                    onOpenTimeSettings = onOpenTimeSettings,
                    onBack = onBack
                )
            }
            composable(ROUTE_MIND_STATE) { MindStateScreen() }
        }
    }
}
