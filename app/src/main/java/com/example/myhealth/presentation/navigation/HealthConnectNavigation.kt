package com.example.myhealth.presentation.navigation
import com.example.myhealth.presentation.home.HomeHost
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.presentation.screen.SettingsScreen
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
import com.example.myhealth.presentation.screen.mind.MindScreen
import com.example.myhealth.showExceptionSnackbar
import kotlinx.coroutines.launch
import android.app.TimePickerDialog
import android.content.Context
import com.example.myhealth.presentation.screen.mind.MindRootScreen
import java.util.*
import androidx.compose.ui.platform.LocalContext


fun showTimePicker(context: Context) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            // TODO: Handle selected time here
            println("Selected time: $selectedHour:$selectedMinute")
        },
        hour,
        minute,
        true
    ).show()
}


/**
 * App navigation graph.
 * Start destination is Home. All screens keep your original behavior.
 *
 * Note:
 * - Replaced all `by viewModel.xxx` usages with explicit `.value` to
 *   avoid the Kotlin property delegate error on this file.
 */
@Composable
fun HealthConnectNavigation(
    navController: NavHostController,
    healthConnectManager: HealthConnectManager,
    scaffoldState: ScaffoldState
) {
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        // Home
        composable(Screen.Home.route) {
            HomeHost(navController = navController)
        }

        // Privacy policy (deep link preserved)
        composable(
            route = Screen.PrivacyPolicy.route,
            deepLinks = listOf(
                navDeepLink { action = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" }
            )
        ) {
            PrivacyPolicyScreen()
        }

        // Settings
        composable(Screen.SettingsScreen.route) {
            SettingsScreen { scope.launch { healthConnectManager.revokeAllPermissions() } }
        }

        // Exercise Sessions
        composable(Screen.ExerciseSessions.route) {
            val viewModel: ExerciseSessionViewModel = viewModel(
                factory = ExerciseSessionViewModelFactory(healthConnectManager)
            )

            LaunchedEffect(Unit) { viewModel.initialLoad() }

            val permissionsGranted       = viewModel.permissionsGranted.value
            val sessionsList             = viewModel.sessionsList.value
            val permissions              = viewModel.permissions
            val backgroundReadAvailable  = viewModel.backgroundReadAvailable.value
            val backgroundReadGranted    = viewModel.backgroundReadGranted.value

            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }

            ExerciseSessionScreen(
                permissions = permissions,
                permissionsGranted = permissionsGranted,
                backgroundReadAvailable = backgroundReadAvailable,
                backgroundReadGranted = backgroundReadGranted,
                sessionsList = sessionsList,
                uiState = viewModel.uiState,
                onInsertClick = { viewModel.insertExerciseSession() },
                onDetailsClick = { uid ->
                    navController.navigate(Screen.ExerciseSessionDetail.route + "/$uid")
                },
                onDeleteClick = { uid -> viewModel.deleteExerciseSession(uid) },
                onError = { exception -> showExceptionSnackbar(scaffoldState, scope, exception) },
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        // Weight Records
        composable(Screen.WeightRecords.route) {
            val viewModel: InputReadingsViewModel = viewModel(
                factory = InputReadingsViewModelFactory(healthConnectManager)
            )

            val permissionsGranted = viewModel.permissionsGranted.value
            val readingsList       = viewModel.readingsList.value
            val permissions        = viewModel.permissions
            val weeklyAvg          = viewModel.weeklyAvg.value

            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }

            LaunchedEffect(Unit) { viewModel.initialLoad() }

            InputReadingsScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                uiState = viewModel.uiState,
                onInsertClick = { weightInput -> viewModel.inputReadings(weightInput) },
                weeklyAvg = weeklyAvg,
                onDeleteClick = { uid -> viewModel.deleteWeightInput(uid) },
                readingsList = readingsList,
                onError = { exception -> showExceptionSnackbar(scaffoldState, scope, exception) },
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        // Exercise Session Detail
        composable(Screen.ExerciseSessionDetail.route + "/{$UID_NAV_ARGUMENT}") {
            val uid = it.arguments?.getString(UID_NAV_ARGUMENT)!!

            val viewModel: ExerciseSessionDetailViewModel = viewModel(
                factory = ExerciseSessionDetailViewModelFactory(uid, healthConnectManager)
            )

            val permissionsGranted = viewModel.permissionsGranted.value
            val sessionMetrics     = viewModel.sessionMetrics.value
            val permissions        = viewModel.permissions

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
                onError = { exception -> showExceptionSnackbar(scaffoldState, scope, exception) },
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        // Record List (from Session Detail)
        composable(
            Screen.RecordListScreen.route + "/{$RECORD_TYPE}" +
                    "/{$UID_NAV_ARGUMENT}" + "/{$SERIES_RECORDS_TYPE}"
        ) {
            val uid = it.arguments?.getString(UID_NAV_ARGUMENT)!!
            val recordTypeString        = it.arguments?.getString(RECORD_TYPE)!!
            val seriesRecordsTypeString = it.arguments?.getString(SERIES_RECORDS_TYPE)!!

            val viewModel: RecordListScreenViewModel = viewModel(
                factory = RecordListViewModelFactory(
                    uid = uid,
                    recordTypeString = recordTypeString,
                    seriesRecordsTypeString = seriesRecordsTypeString,
                    healthConnectManager = healthConnectManager
                )
            )

            val permissionsGranted = viewModel.permissionsGranted.value
            val recordList         = viewModel.recordList
            val permissions        = viewModel.permissions

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

        // Sleep Sessions
        composable(Screen.SleepSessions.route) {
            val viewModel: SleepSessionViewModel = viewModel(
                factory = SleepSessionViewModelFactory(healthConnectManager)
            )

            val permissionsGranted = viewModel.permissionsGranted
            val sessionsList       = viewModel.sessionsList
            val permissions        = viewModel.permissions


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
                onError = { exception -> showExceptionSnackbar(scaffoldState, scope, exception) },
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        // Input Readings (second entry kept as in your file)
        composable(Screen.InputReadings.route) {
            val viewModel: InputReadingsViewModel = viewModel(
                factory = InputReadingsViewModelFactory(healthConnectManager)
            )

            val permissionsGranted = viewModel.permissionsGranted.value
            val readingsList       = viewModel.readingsList.value
            val permissions        = viewModel.permissions
            val weeklyAvg          = viewModel.weeklyAvg.value

            val onPermissionsResult = { viewModel.initialLoad() }
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()
                }

            LaunchedEffect(Unit) { viewModel.initialLoad() }

            InputReadingsScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                uiState = viewModel.uiState,
                onInsertClick = { weightInput -> viewModel.inputReadings(weightInput) },
                weeklyAvg = weeklyAvg,
                onDeleteClick = { uid -> viewModel.deleteWeightInput(uid) },
                readingsList = readingsList,
                onError = { exception -> showExceptionSnackbar(scaffoldState, scope, exception) },
                onPermissionsResult = { viewModel.initialLoad() },
                onPermissionsLaunch = { values -> permissionsLauncher.launch(values) }
            )
        }

        // Differential Changes
        composable(Screen.DifferentialChanges.route) {
            val viewModel: DifferentialChangesViewModel = viewModel(
                factory = DifferentialChangesViewModelFactory(healthConnectManager)
            )

            val changesToken       = viewModel.changesToken.value
            val permissionsGranted = viewModel.permissionsGranted.value
            val permissions        = viewModel.permissions

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
                onError = { exception -> showExceptionSnackbar(scaffoldState, scope, exception) },
                onPermissionsResult = { viewModel.initialLoad() }
            ) { values -> permissionsLauncher.launch(values) }
        }

        // Other tabs
        composable(Screen.Nutrition.route) {
            com.example.myhealth.presentation.screen.nutrition.NutritionRootScreen()
        }
        composable(Screen.Mind.route) {
            val context = LocalContext.current
            MindRootScreen(
                onBack = { navController.popBackStack() },
                onOpenTimeSettings = {
                    showTimePicker(context)
                }
            )


        }
    }
}
