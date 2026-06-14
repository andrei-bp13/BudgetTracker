package com.pardonmyspending.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

    MaterialTheme(
        colorScheme = colors,
        typography  = BudgetTypography,
        content     = content
    )
}
