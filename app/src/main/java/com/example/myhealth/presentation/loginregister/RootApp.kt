package com.example.myhealth.presentation.loginregister

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.presentation.HealthConnectApp // 你原来的入口

@Composable
fun RootApp(healthConnectManager: HealthConnectManager) {
    var authed by rememberSaveable { mutableStateOf(false) }

    if (authed) {
        HealthConnectApp(healthConnectManager = healthConnectManager)
    } else {
        AuthNavHost(onAuthenticated = { authed = true })
    }
}
