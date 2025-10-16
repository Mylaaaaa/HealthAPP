package com.example.myhealth.presentation.screen.mind

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val ROUTE_MIND_OVERVIEW = "mind_overview"
private const val ROUTE_MIND_STATE = "mind_state"
private const val ROUTE_MIND_SESSION = "mind_session?title={title}&mins={mins}"
private const val ROUTE_MIND_SETTINGS = "mind_settings"

/**
 * Root with 2 tabs + 2 extra destinations:
 * - Overview / State tabs stay the same.
 * - Added:
 *   • mind_session: dedicated guided session/timer screen
 *   • mind_settings: date settings screen (calendar + quick chips)
 */
@Composable
fun MindRootScreen(
    onBack: () -> Unit
) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()

    Scaffold(
        backgroundColor = Color.White,
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    selected = current?.destination?.route?.startsWith(ROUTE_MIND_OVERVIEW) == true,
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
                    selected = current?.destination?.route?.startsWith(ROUTE_MIND_STATE) == true,
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
            // Overview (now opens session/settings pages)
            composable(ROUTE_MIND_OVERVIEW) {
                MindOverviewScreen(
                    onOpenSession = { title, mins ->
                        val t = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
                        nav.navigate("mind_session?title=$t&mins=$mins")
                    },
                    onOpenSettings = { nav.navigate(ROUTE_MIND_SETTINGS) },
                    onBack = onBack
                )
            }

            // State tab (unchanged)
            composable(ROUTE_MIND_STATE) { MindStateScreen() }

            // Guided session / timer
            composable(
                route = ROUTE_MIND_SESSION,
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType; defaultValue = "Session" },
                    navArgument("mins") { type = NavType.IntType; defaultValue = 3 }
                )
            ) { backStack ->
                val title = backStack.arguments?.getString("title") ?: "Session"
                val mins = backStack.arguments?.getInt("mins") ?: 3
                MindSessionScreen(
                    title = title,
                    minutes = mins,
                    onBack = { nav.popBackStack() }
                )
            }

            // Date settings (calendar + quick choices)
            composable(ROUTE_MIND_SETTINGS) {
                MindReminderSettingsScreen(
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
