package com.example.myhealth.presentation
import com.example.myhealth.presentation.loginregister.RootApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * The entry point into the sample.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val healthConnectManager = (application as BaseApplication).healthConnectManager

        setContent { RootApp(healthConnectManager = healthConnectManager) }
    }
}
