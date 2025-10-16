// NutritionRootScreen.kt
// Purpose: Inner bottom navigation for Nutrition module with two tabs (Overview | State).
// NOTE: We DO NOT rename your existing NutritionScreen. The Overview tab calls your current NutritionScreen().

package com.example.myhealth.presentation.screen.nutrition

import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// Routes used only for inner navigation inside the Nutrition module
const val ROUTE_NUTRITION_OVERVIEW: String = "nutrition/overview"
const val ROUTE_NUTRITION_STATE: String = "nutrition/state"

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun NutritionRootScreen() {
    // Inner NavController specific to the Nutrition module
    val innerNav = rememberNavController()
    val tabs = listOf(
        TabItem(ROUTE_NUTRITION_OVERVIEW, "Overview", Icons.Filled.List),
        TabItem(ROUTE_NUTRITION_STATE, "State", Icons.Filled.Dashboard)
    )

    Scaffold(
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by innerNav.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                tabs.forEach { tab ->
                    BottomNavigationItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            innerNav.navigate(tab.route) {
                                // Keep a single instance per tab and restore its state
                                popUpTo(innerNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    )
    { innerPadding ->
        // Create the AndroidViewModel once and share to both tabs
        val app = LocalContext.current.applicationContext as Application
        val sharedVm: NutritionViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )
        NavHost(
            navController = innerNav,
            startDestination = ROUTE_NUTRITION_OVERVIEW,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_NUTRITION_OVERVIEW) {
                com.example.myhealth.presentation.screen.nutrition.NutritionScreen() // if NutritionScreen doesn't accept a vm param
                // If your NutritionScreen CAN accept a vm param, prefer passing sharedVm:
                // NutritionScreen(vm = sharedVm)
            }
            composable(ROUTE_NUTRITION_STATE) {
                com.example.myhealth.presentation.screen.nutrition.NutritionStateScreen(vm = sharedVm)
            }
        }
    }
}
