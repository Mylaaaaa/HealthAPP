package com.example.myhealth.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorPalette = darkColors(
    primary = HealthConnectGreen,  // 这里可替换为你想要的暗色主色
    secondary = HealthConnectBlue
)

private val LightColorPalette = lightColors(
    primary = HealthConnectBlue,   // 顶栏&按钮主色
    secondary = HealthConnectBlue
)

@Composable
fun HealthConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    // Ensure status bar & nav bar colors match the theme
    val systemUi = rememberSystemUiController()
    val useDarkIcons = !darkTheme
    SideEffect {
        systemUi.setStatusBarColor(colors.primary, darkIcons = useDarkIcons)
        systemUi.setNavigationBarColor(colors.background, darkIcons = useDarkIcons)
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
