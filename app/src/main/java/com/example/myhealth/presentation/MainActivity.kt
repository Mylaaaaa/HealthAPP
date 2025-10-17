// file: app/src/main/java/com/example/myhealth/presentation/MainActivity.kt
package com.example.myhealth.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.myhealth.presentation.loginregister.RootApp
import com.example.myhealth.presentation.theme.HealthConnectTheme
import com.example.myhealth.presentation.theme.ThemeMode
import com.example.myhealth.presentation.theme.ThemeViewModel

/**
 * Single source of truth for theming.
 * We read ThemeViewModel here and wrap the ENTIRE app with HealthConnectTheme.
 * That way, Login/Register (Auth flow) and the signed-in app both follow Settings.
 */
class MainActivity : ComponentActivity() {

    // Activity-scoped ThemeViewModel
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val healthConnectManager = (application as BaseApplication).healthConnectManager

        setContent {
            // Observe current theme mode from DataStore via ThemeViewModel
            val themeMode: ThemeMode by themeViewModel.themeMode.collectAsState()

            // Decide darkTheme flag based on the selected mode
            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Dark   -> true
                ThemeMode.Light  -> false
            }

            // IMPORTANT: Only this one theme wrapper in the whole app
            HealthConnectTheme(darkTheme = darkTheme) {
                RootApp(
                    healthConnectManager = healthConnectManager,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}
