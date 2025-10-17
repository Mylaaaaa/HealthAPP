package com.example.myhealth.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val LightColorPalette = lightColors(
    primary = HealthConnectBlue,
    primaryVariant = HealthConnectBlue,      // You can provide a darker blue if you have one
    secondary = HealthConnectGreen,
    secondaryVariant = HealthConnectGreen,

    background = Color(0xFFF8FAFC),          // very light gray-blue for a softer white
    surface = Color(0xFFFFFFFF),             // cards / sheets
    error = Color(0xFFB00020),

    onPrimary = Color.White,                 // text/icons on primary
    onSecondary = Color.White,               // text/icons on secondary
    onBackground = Color(0xFF111418),        // main text on background
    onSurface = Color(0xFF1F2328),           // text on cards
    onError = Color.White
)

/* ----------------------------- Dark palette ------------------------------ */
/**
 * Dark theme color tokens.
 * Keep backgrounds dark and raise contrast with onBackground/onSurface.
 * Primary/secondary are slightly lighter tints so they remain vivid on dark.
 */
private val DarkColorPalette = darkColors(
    primary = Color(0xFF90CAF9),             // lightened blue for dark mode (Readable on dark)
    primaryVariant = Color(0xFF64B5F6),
    secondary = Color(0xFF80DEEA),           // lightened green/cyan accent
    secondaryVariant = Color(0xFF4DD0E1),

    background = Color(0xFF0F1115),          // near-black with a hint of blue/gray
    surface = Color(0xFF171A1F),             // card background slightly lighter than background
    error = Color(0xFFCF6679),

    onPrimary = Color.Black,                 // dark text/icons on light-ish primary
    onSecondary = Color.Black,
    onBackground = Color(0xFFE6E8EC),        // light text on dark background
    onSurface = Color(0xFFE6E8EC),
    onError = Color.Black
)

/* -------------------------- Theme entry function ------------------------- */
/**
 * App theme entry.
 *
 * @param darkTheme If true, forces dark theme. Default follows system setting.
 *                  In the future, you can pass a user preference here (SYSTEM / LIGHT / DARK).
 */
@Composable
fun HealthConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Pick the active palette
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    // Keep status/navigation bars readable and consistent with the theme background
    val systemUi = rememberSystemUiController()
    val useDarkIcons = !darkTheme // dark icons on light backgrounds; light icons on dark backgrounds
    SideEffect {
        // Use background for bars to avoid a heavy top stripe and to improve contrast with icons
        systemUi.setStatusBarColor(colors.background, darkIcons = useDarkIcons)
        systemUi.setNavigationBarColor(colors.background, darkIcons = useDarkIcons)
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
