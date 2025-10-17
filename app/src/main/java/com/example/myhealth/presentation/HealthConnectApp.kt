package com.example.myhealth.presentation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myhealth.R
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.presentation.navigation.Drawer
import com.example.myhealth.presentation.navigation.HealthConnectNavigation
import com.example.myhealth.presentation.navigation.Screen
import com.example.myhealth.presentation.theme.HealthConnectTheme
import com.example.myhealth.presentation.theme.ThemeMode
import com.example.myhealth.presentation.theme.ThemeViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme


const val TAG = "Health Connect sample"

private fun isTopLevel(route: String?): Boolean {
    return route?.startsWith(Screen.Home.route) == true
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HealthConnectApp(
    healthConnectManager: HealthConnectManager,
    themeViewModel: ThemeViewModel
) {
    // Observe current theme mode
    val themeMode by themeViewModel.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    HealthConnectTheme(darkTheme = darkTheme) {

        val scaffoldState = rememberScaffoldState()
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val availability by healthConnectManager.availability
        val context = LocalContext.current

        /* ---------------- 系统返回键逻辑 ---------------- */
        BackHandler(enabled = true) {
            when {
                scaffoldState.drawerState.isOpen -> {
                    scope.launch { scaffoldState.drawerState.close() }
                }
                navController.previousBackStackEntry != null -> {
                    navController.popBackStack()
                }
                else -> {
                    (context as? Activity)?.let {
                        ActivityCompat.finishAfterTransition(it)
                    }
                }
            }
        }

        /* ---------------- Scaffold 外层框架 ---------------- */
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = {
                        val titleId = when (currentRoute) {
                            Screen.ExerciseSessions.route -> Screen.ExerciseSessions.titleId
                            Screen.SleepSessions.route -> Screen.SleepSessions.titleId
                            Screen.InputReadings.route -> Screen.InputReadings.titleId
                            Screen.DifferentialChanges.route -> Screen.DifferentialChanges.titleId
                            Screen.WeightRecords.route -> Screen.WeightRecords.titleId
                            Screen.Nutrition.route -> Screen.Nutrition.titleId
                            Screen.Mind.route -> Screen.Mind.titleId
                            else -> R.string.app_name
                        }
                        Text(stringResource(titleId))
                    },
                    navigationIcon = {
                        if (isTopLevel(currentRoute)) {
                            IconButton(
                                onClick = {
                                    if (availability == SDK_AVAILABLE) {
                                        scope.launch {
                                            scaffoldState.drawerState.open()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = stringResource(id = R.string.menu)
                                )
                            }
                        } else {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    }
                )
            },

            /* ---------------- Drawer 侧边栏 ---------------- */
            drawerContent = {
                if (availability == SDK_AVAILABLE) {
                    Drawer(
                        scope = scope,
                        scaffoldState = scaffoldState,
                        navController = navController
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(it) { data -> Snackbar(snackbarData = data) }
            }
        ) {

            HealthConnectNavigation(
                healthConnectManager = healthConnectManager,
                navController = navController,
                scaffoldState = scaffoldState,
                themeViewModel = themeViewModel
            )
        }
    }
}
