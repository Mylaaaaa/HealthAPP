package com.example.myhealth.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.myhealth.presentation.loginregister.RootApp
import com.example.myhealth.presentation.theme.HealthConnectTheme
import com.example.myhealth.presentation.theme.ThemeViewModel

class MainActivity : ComponentActivity() {

    // Provide ThemeViewModel at the Activity scope
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val healthConnectManager = (application as BaseApplication).healthConnectManager

        setContent {
            // HealthConnectTheme is still used in RootApp/HealthConnectApp;
            // here we just supply the ViewModel down the tree.
            HealthConnectTheme {
                RootApp(
                    healthConnectManager = healthConnectManager,
                    themeViewModel = themeViewModel  // <-- pass down
                )
            }
        }
    }
}
