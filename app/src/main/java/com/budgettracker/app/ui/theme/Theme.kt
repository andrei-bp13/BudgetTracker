package com.budgettracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun BudgetTrackerTheme(
    themeMode: String = "system",
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "dark"  -> true
        "light" -> false
        else    -> isSystemInDarkTheme()
    }

    val seed   = AccentColors.getOrElse(accentIndex) { AccentColors[0] }.seed
    val colors = accentColorScheme(seed, dark)

    MaterialTheme(
        colorScheme = colors,
        typography  = BudgetTypography,
        content     = content
    )
}
