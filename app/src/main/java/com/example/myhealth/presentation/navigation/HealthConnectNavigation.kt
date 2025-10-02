/*
 * Navigation host for MyHealth (Home as start destination).
 * Minimal changes from your original:
 * 1) startDestination -> Screen.Home.route
 * 2) Add composable(Screen.Home.route) { WelcomeScreen(navController) }
 * Everything else stays the same.
 */
package com.example.myhealth.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.presentation.screen.SettingsScreen
import com.example.myhealth.presentation.screen.WelcomeScreen
import com.example.myhealth.presentation.screen.changes.DifferentialChangesScreen
import com.example.myhealth.presentation.screen.changes.DifferentialChangesViewModel
import com.example.myhealth.presentation.screen.changes.DifferentialChangesViewModelFactory
import com.example.myhealth.presentation.screen.exercisesession.ExerciseSessionScreen
import com.example.myhealth.presentation.screen.exercisesession.ExerciseSessionViewModel
import com.example.myhealth.presentation.screen.exercisesession.ExerciseSessionViewModelFactory
import com.example.myhealth.presentation.screen.exercisesessiondetail.ExerciseSessionDetailScreen
import com.example.myhealth.presentation.screen.exercisesessiondetail.ExerciseSessionDetailViewModel
import com.example.myhealth.presentation.screen.exercisesessiondetail.ExerciseSessionDetailViewModelFactory
import com.example.myhealth.presentation.screen.inputreadings.InputReadingsScreen
import com.example.myhealth.presentation.screen.inputreadings.InputReadingsViewModel
import com.example.myhealth.presentation.screen.inputreadings.InputReadingsViewModelFactory
import com.example.myhealth.presentation.screen.privacypolicy.PrivacyPolicyScreen
import com.example.myhealth.presentation.screen.recordlist.RecordListScreen
import com.example.myhealth.presentation.screen.recordlist.RecordListScreenViewModel
import com.example.myhealth.presentation.screen.recordlist.RecordListViewModelFactory
import com.example.myhealth.presentation.screen.recordlist.RecordType
import com.example.myhealth.presentation.screen.recordlist.SeriesRecordsType
import com.example.myhealth.presentation.screen.sleepsession.SleepSessionScreen
import com.example.myhealth.presentation.screen.sleepsession.SleepSessionViewModel
import com.example.myhealth.presentation.screen.sleepsession.SleepSessionViewModelFactory
import com.example.myhealth.presentation.screen.dashboard.DashboardScreen
import com.example.myhealth.presentation.screen.nutrition.NutritionScreen
import com.example.myhealth.presentation.screen.mind.MindScreen
import com.example.myhealth.presentation.screen.reports.ReportsScreen
import com.example.myhealth.showExceptionSnackbar
import kotlinx.coroutines.launch

/**
 * Provides the navigation in the app.
 * Uses Home as the start destination, rendering WelcomeScreen(navController) as the Home UI.
 */
@Composable
fun HealthConnectNavigation(
    navController: NavHostController,
    healthConnectManager: HealthConnectManager,
    scaffoldState: ScaffoldState
) {
    val scope = rememberCoroutineScope()

    // ✅ Start from Home
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        // Home page: reuse your WelcomeScreen as the Home UI
        composable(Screen.Home.route) {
            com.example.myhealth.presentation.home.HomeHost(navController)
        }

        composable(
            route = Screen.PrivacyPolicy.route,
            deepLinks = listOf(
                navDeepLink {
                    action = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
                }
            )
        ) {
            PrivacyPolicyScreen()
        }

        composable(Screen.SettingsScreen.route){
            SettingsScreen { scope.launch { healthConnectManager.revokeAllPermissions() } }
        }

        composable(Screen.ExerciseSessions.route) {
            val viewModel: ExerciseSessionViewModel = viewModel(
                factory = ExerciseSessionViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val sessionsList by viewModel.sessionsList
            val permissions = viewModel.permissions
            val backgroundReadAvailable by viewModel.backgroundReadAvailable
            val backgroundReadGranted by viewModel.backgroundReadGranted
            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }

            ExerciseSessionScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                backgroundReadAvailable = backgroundReadAvailable,
                backgroundReadGranted = backgroundReadGranted,
                sessionsList = sessionsList,
                uiState = viewModel.uiState,
                onInsertClick = { viewModel.insertExerciseSession() },
                onDetailsClick = { uid ->
                    navController.navigate(Screen.ExerciseSessionDetail.route + "/" + uid)
                },
                onDeleteClick = { uid -> viewModel.deleteExerciseSession(uid) },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        composable(Screen.ExerciseSessionDetail.route + "/{$UID_NAV_ARGUMENT}") {
            val uid = it.arguments?.getString(UID_NAV_ARGUMENT)!!
            val viewModel: ExerciseSessionDetailViewModel = viewModel(
                factory = ExerciseSessionDetailViewModelFactory(
                    uid = uid,
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val sessionMetrics by viewModel.sessionMetrics
            val permissions = viewModel.permissions
            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }

            ExerciseSessionDetailScreen(
                permissions = permissions,
                permissionsGranted = permissionsGranted,
                sessionMetrics = sessionMetrics,
                uiState = viewModel.uiState,
                onDetailsClick = { recordType, recordId, seriesRecordsType ->
                    navController.navigate(
                        Screen.RecordListScreen.route + "/" +
                                recordType + "/" + recordId + "/" + seriesRecordsType
                    )
                },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        composable(
            Screen.RecordListScreen.route + "/{$RECORD_TYPE}" +
                    "/{$UID_NAV_ARGUMENT}" + "/{$SERIES_RECORDS_TYPE}"
        ) {
            val uid = it.arguments?.getString(UID_NAV_ARGUMENT)!!
            val recordTypeString = it.arguments?.getString(RECORD_TYPE)!!
            val seriesRecordsTypeString = it.arguments?.getString(SERIES_RECORDS_TYPE)!!
            val viewModel: RecordListScreenViewModel = viewModel(
                factory = RecordListViewModelFactory(
                    uid = uid,
                    recordTypeString = recordTypeString,
                    seriesRecordsTypeString = seriesRecordsTypeString,
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val recordList = viewModel.recordList
            val permissions = viewModel.permissions
            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }

            RecordListScreen(
                uid = uid,
                permissions = permissions,
                permissionsGranted = permissionsGranted,
                recordType = RecordType.valueOf(recordTypeString),
                seriesRecordsType = SeriesRecordsType.valueOf(seriesRecordsTypeString),
                recordList = recordList,
                uiState = viewModel.uiState,
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        composable(Screen.SleepSessions.route) {
            val viewModel: SleepSessionViewModel = viewModel(
                factory = SleepSessionViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val sessionsList by viewModel.sessionsList
            val permissions = viewModel.permissions
            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }

            SleepSessionScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                sessionsList = sessionsList,
                uiState = viewModel.uiState,
                onInsertClick = { viewModel.generateSleepData() },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        composable(Screen.InputReadings.route) {
            val viewModel: InputReadingsViewModel = viewModel(
                factory = InputReadingsViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val readingsList by viewModel.readingsList
            val permissions = viewModel.permissions
            val weeklyAvg by viewModel.weeklyAvg
            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }

            InputReadingsScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                uiState = viewModel.uiState,
                onInsertClick = { weightInput -> viewModel.inputReadings(weightInput) },
                weeklyAvg = weeklyAvg,
                onDeleteClick = { uid -> viewModel.deleteWeightInput(uid) },
                readingsList = readingsList,
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        composable(Screen.DifferentialChanges.route) {
            val viewModel: DifferentialChangesViewModel = viewModel(
                factory = DifferentialChangesViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val changesToken by viewModel.changesToken
            val permissionsGranted by viewModel.permissionsGranted
            val permissions = viewModel.permissions
            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }

            DifferentialChangesScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                changesEnabled = changesToken != null,
                onChangesEnable = { enabled -> viewModel.enableOrDisableChanges(enabled) },
                changes = viewModel.changes,
                changesToken = changesToken,
                onGetChanges = { viewModel.getChanges() },
                uiState = viewModel.uiState,
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = { viewModel.initialLoad() }
            ) { values ->
                permissionsLauncher.launch(values)
            }
        }


        composable(Screen.Dashboard.route) { DashboardScreen() }
        composable(Screen.Nutrition.route) { NutritionScreen() }
        composable(Screen.Mind.route)      { MindScreen() }
        composable(Screen.Reports.route)   { ReportsScreen() }
    }
}
