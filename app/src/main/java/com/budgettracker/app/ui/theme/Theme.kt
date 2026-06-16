package com.pardonmyspending.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun BudgetTrackerTheme(
    themeMode: String = "system",
    accentIndex: Int = 0,
    customAccentHex: String = "2196F3",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "dark"  -> true
        "light" -> false
        else    -> isSystemInDarkTheme()
    }

    val seed = if (accentIndex == 9) {
        runCatching { Color(0xFF000000L or customAccentHex.toLong(16)) }.getOrElse { Color(0xFF2196F3) }
    } else {
        AccentColors.getOrElse(accentIndex) { AccentColors[0] }.seed
    }

    val colors = accentColorScheme(seed, dark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars     = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography  = BudgetTypography,
        content     = content
    )
}
