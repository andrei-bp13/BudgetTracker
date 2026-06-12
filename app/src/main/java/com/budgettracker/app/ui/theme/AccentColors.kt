package com.budgettracker.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AccentOption(val name: String, val seed: Color)

val AccentColors = listOf(
    AccentOption("Blue",    Color(0xFF4361EE)),  // vivid blue
    AccentOption("Violet",  Color(0xFF8338EC)),  // violet
    AccentOption("Pink",    Color(0xFFF72585)),  // hot pink
    AccentOption("Coral",   Color(0xFFE63946)),  // coral red
    AccentOption("Orange",  Color(0xFFFB8500)),  // orange
    AccentOption("Amber",   Color(0xFFFFB703)),  // golden amber
    AccentOption("Emerald", Color(0xFF06D6A0)),  // emerald
    AccentOption("Teal",    Color(0xFF118AB2)),  // teal blue
    AccentOption("Sky",     Color(0xFF4CC9F0)),  // sky blue
    AccentOption("Custom",  Color(0xFF4361EE)),  // placeholder; actual color stored in prefs
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
