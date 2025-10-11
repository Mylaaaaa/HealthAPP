package com.example.myhealth.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myhealth.presentation.loginregister.RootApp
import com.example.myhealth.presentation.theme.HealthConnectTheme

/**
 * The entry point into the sample.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val healthConnectManager = (application as BaseApplication).healthConnectManager

        setContent {
            HealthConnectTheme {
                RootApp(healthConnectManager = healthConnectManager)
            }
        }
    }
}
