package com.zihanwang.myhealth.presentation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zihanwang.myhealth.data.HealthConnectManager
import com.zihanwang.myhealth.presentation.navigation.Drawer
import com.zihanwang.myhealth.presentation.navigation.HealthConnectNavigation
import com.zihanwang.myhealth.presentation.navigation.Screen
import com.zihanwang.myhealth.presentation.theme.ThemeViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.zihanwang.myhealth.R

/**
 * DO NOT wrap HealthConnectTheme here.
 * MainActivity already wraps the whole app with the selected theme.
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HealthConnectApp(
    healthConnectManager: HealthConnectManager,
    themeViewModel: ThemeViewModel // keep param (Settings uses it), but don't wrap theme here
) {
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val availability by healthConnectManager.availability
    val context = LocalContext.current

    BackHandler(enabled = true) {
        when {
            scaffoldState.drawerState.isOpen -> scope.launch { scaffoldState.drawerState.close() }
            navController.previousBackStackEntry != null -> navController.popBackStack()
            else -> (context as? Activity)?.let { ActivityCompat.finishAfterTransition(it) }
        }
    }

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
                    if (currentRoute?.startsWith(Screen.Home.route) == true) {
                        IconButton(
                            onClick = {
                                if (availability == SDK_AVAILABLE) {
                                    scope.launch { scaffoldState.drawerState.open() }
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
        drawerContent = {
            if (availability == SDK_AVAILABLE) {
                Drawer(
                    scope = scope,
                    scaffoldState = scaffoldState,
                    navController = navController
                )
            }
        },
        snackbarHost = { SnackbarHost(it) { data -> Snackbar(snackbarData = data) } }
    ) {
        HealthConnectNavigation(
            healthConnectManager = healthConnectManager,
            navController = navController,
            scaffoldState = scaffoldState,
            themeViewModel = themeViewModel
        )
    }
}
