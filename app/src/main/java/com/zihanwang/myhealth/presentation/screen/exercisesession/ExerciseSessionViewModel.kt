package com.zihanwang.myhealth.presentation.screen.exercisesession

import android.os.RemoteException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zihanwang.myhealth.data.ExerciseSession
import com.zihanwang.myhealth.data.HealthConnectManager
import com.zihanwang.myhealth.data.dateTimeWithOffsetOrDefault
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

class ExerciseSessionViewModel(private val healthConnectManager: HealthConnectManager) :
    ViewModel() {
    private val healthConnectCompatibleApps = healthConnectManager.healthConnectCompatibleApps

    val permissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(SpeedRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class)
    )

    var permissionsGranted = mutableStateOf(false)
        private set

    var backgroundReadAvailable = mutableStateOf(false)
        private set

    var backgroundReadGranted = mutableStateOf(false)
        private set

    var sessionsList: MutableState<List<ExerciseSession>> = mutableStateOf(listOf())
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    fun initialLoad() {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                readExerciseSessions()
            }
        }
    }

    fun insertExerciseSession() {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
                val latestStartOfSession = ZonedDateTime.now().minusMinutes(30)
                val offset = Random.nextDouble()

                // Generate random start time between the start of the day and (now - 30mins).
                val startOfSession = startOfDay.plusSeconds(
                    (Duration.between(startOfDay, latestStartOfSession).seconds * offset).toLong()
                )
                val endOfSession = startOfSession.plusMinutes(30)

                healthConnectManager.writeExerciseSession(startOfSession, endOfSession)
                readExerciseSessions()
            }
        }
    }

    fun deleteExerciseSession(uid: String) {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                healthConnectManager.deleteExerciseSession(uid)
                readExerciseSessions()
            }
        }
    }

    private suspend fun readExerciseSessions() {
        val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val now = Instant.now()

        sessionsList.value = healthConnectManager
            .readExerciseSessions(startOfDay.toInstant(), now)
            .map { record ->
                val packageName = record.metadata.dataOrigin.packageName
                ExerciseSession(
                    startTime = dateTimeWithOffsetOrDefault(record.startTime, record.startZoneOffset),
                    endTime   = dateTimeWithOffsetOrDefault(record.endTime,   record.endZoneOffset),   // ← 对
                    id = record.metadata.id,
                    sourceAppInfo = healthConnectCompatibleApps[packageName],
                    title = record.title
                )
            }
    }

    /**
     * Provides permission check and error handling for Health Connect suspend function calls.
     *
     * Permissions are checked prior to execution of [block], and if all permissions aren't granted
     * the [block] won't be executed, and [permissionsGranted] will be set to false, which will
     * result in the UI showing the permissions button.
     *
     * Where an error is caught, of the type Health Connect is known to throw, [uiState] is set to
     * [UiState.Error], which results in the snackbar being used to show the error message.
     */
    private suspend fun tryWithPermissionsCheck(block: suspend () -> Unit) {
        permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)
        backgroundReadAvailable.value = healthConnectManager.isFeatureAvailable(
            HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
        )
        backgroundReadGranted.value = healthConnectManager.hasAllPermissions(
            setOf(PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
        )
        uiState = try {
            if (permissionsGranted.value) {
                block()
            }
            UiState.Done
        } catch (remoteException: RemoteException) {
            UiState.Error(remoteException)
        } catch (securityException: SecurityException) {
            UiState.Error(securityException)
        } catch (ioException: IOException) {
            UiState.Error(ioException)
        } catch (illegalStateException: IllegalStateException) {
            UiState.Error(illegalStateException)
        }
    }

    sealed class UiState {
        object Uninitialized : UiState()
        object Done : UiState()

        // A random UUID is used in each Error object to allow errors to be uniquely identified,
        // and recomposition won't result in multiple snackbars.
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }

    private val _workoutItems = kotlinx.coroutines.flow.MutableStateFlow<List<ExerciseItemUi>>(emptyList())
    val workoutItems: kotlinx.coroutines.flow.StateFlow<List<ExerciseItemUi>> = _workoutItems

    init {
        val now = java.time.LocalDate.now()
        _workoutItems.value = listOf(
            ExerciseItemUi(id = java.util.UUID.randomUUID().toString(), date = now, duration = java.time.Duration.ofMinutes(30), isCompleted = false),
            ExerciseItemUi(id = java.util.UUID.randomUUID().toString(), date = now.minusDays(1), duration = java.time.Duration.ofMinutes(45), isCompleted = true),
        )
    }

    fun addWorkout(date: java.time.LocalDate, minutes: Int, completed: Boolean) {
        val list = _workoutItems.value.toMutableList()
        list.add(0, ExerciseItemUi(java.util.UUID.randomUUID().toString(), date, java.time.Duration.ofMinutes(minutes.toLong()), completed))
        _workoutItems.value = list
    }

    fun editWorkout(id: String, date: java.time.LocalDate, minutes: Int, completed: Boolean) {
        val list = _workoutItems.value.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        if (i != -1) {
            list[i] = list[i].copy(date = date, duration = java.time.Duration.ofMinutes(minutes.toLong()), isCompleted = completed)
            _workoutItems.value = list
        }
    }

    fun deleteWorkout(id: String) {
        _workoutItems.value = _workoutItems.value.filterNot { it.id == id }
    }

    fun toggleWorkoutCompleted(id: String) {
        val list = _workoutItems.value.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        if (i != -1) {
            list[i] = list[i].copy(isCompleted = !list[i].isCompleted)
            _workoutItems.value = list
        }
    }

}

class ExerciseSessionViewModelFactory(
    private val healthConnectManager: HealthConnectManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExerciseSessionViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

