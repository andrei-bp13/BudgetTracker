package com.budgettracker.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AccentOption(val name: String, val seed: Color)

val AccentColors = listOf(
    AccentOption("Indigo",    Color(0xFF3F51B5)),
    AccentOption("Purple",    Color(0xFF9C27B0)),
    AccentOption("Pink",      Color(0xFFE91E63)),
    AccentOption("Red",       Color(0xFFF44336)),
    AccentOption("Orange",    Color(0xFFFF5722)),
    AccentOption("Amber",     Color(0xFFFFC107)),
    AccentOption("Green",     Color(0xFF4CAF50)),
    AccentOption("Teal",      Color(0xFF009688)),
    AccentOption("Blue",      Color(0xFF2196F3)),
    AccentOption("Brown",     Color(0xFF795548)),
)

fun accentColorScheme(seed: Color, dark: Boolean): ColorScheme {
    // Derive a simple M3-style scheme from the seed color
    val primary = seed
    val onPrimary = Color.White
    val primaryContainer = seed.copy(alpha = 0.2f).compositeOver(Color.White)
    val onPrimaryContainer = seed

    return if (dark) darkColorScheme(
        primary          = seed,
        onPrimary        = Color.Black,
        primaryContainer = seed.copy(alpha = 0.3f).compositeOver(Color(0xFF1C1B1F)),
        onPrimaryContainer = seed.copy(alpha = 0.9f),
    ) else lightColorScheme(
        primary          = seed,
        onPrimary        = Color.White,
        primaryContainer = seed.copy(alpha = 0.15f).compositeOver(Color.White),
        onPrimaryContainer = seed,
    )
}

private fun Color.compositeOver(background: Color): Color {
    val a = this.alpha
    return Color(
        red   = this.red   * a + background.red   * (1f - a),
        green = this.green * a + background.green * (1f - a),
        blue  = this.blue  * a + background.blue  * (1f - a),
        alpha = 1f
    )
}
