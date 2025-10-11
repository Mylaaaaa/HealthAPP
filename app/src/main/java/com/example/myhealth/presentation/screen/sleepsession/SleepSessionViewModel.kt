package com.example.myhealth.presentation.screen.sleepsession

import android.os.RemoteException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.data.SleepSessionData
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class SleepSessionViewModel(
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class)
    )

    var permissionsGranted by mutableStateOf(false)
        private set

    var sessionsList by mutableStateOf(listOf<SleepSessionData>())
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    fun initialLoad() {
        viewModelScope.launch {
            uiState = UiState.Uninitialized
            tryWithPermissionsCheck {
                sessionsList = healthConnectManager.readSleepSessions()
            }
        }
    }

    fun generateSleepData() {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                healthConnectManager.deleteAllSleepData()
                healthConnectManager.generateSleepData()
                sessionsList = healthConnectManager.readSleepSessions()

                if (sessionsList.isEmpty()) {
                    uiState = UiState.Error(IllegalStateException("No sleep sessions found"))
                }
            }
        }
    }

    /**
     * Wraps a Health Connect suspend call with permission checking and error handling.
     */
    private suspend fun tryWithPermissionsCheck(block: suspend () -> Unit) {
        permissionsGranted = healthConnectManager.hasAllPermissions(permissions)
        uiState = try {
            if (permissionsGranted) {
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
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}

class SleepSessionViewModelFactory(
    private val healthConnectManager: HealthConnectManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SleepSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SleepSessionViewModel(healthConnectManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
