package com.zihanwang.myhealth.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val statBlock: Color,
    val cardBg: Color,
    val fabContainer: Color,
    val fabContent: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        statBlock = Color.Magenta,
        cardBg = Color.White,
        fabContainer = Color.Blue,
        fabContent = Color.White
    )
}
