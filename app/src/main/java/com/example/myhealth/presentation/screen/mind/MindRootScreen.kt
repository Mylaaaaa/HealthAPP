package com.example.myhealth.presentation.screen.mind

import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private const val ROUTE_MIND_OVERVIEW = "mind_overview"
private const val ROUTE_MIND_STATE = "mind_state"

@Composable
fun MindRootScreen() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()

    Scaffold(
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
            composable(ROUTE_MIND_OVERVIEW) { MindOverviewScreen() }
            composable(ROUTE_MIND_STATE) { MindStateScreen() }
        }
    }
}
