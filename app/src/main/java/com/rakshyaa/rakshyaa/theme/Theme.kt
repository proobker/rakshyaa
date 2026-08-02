package com.rakshyaa.rakshyaa.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val Typography = Typography(
    fontFamily = FontFamily.Default,
    // You can customize font sizes, weights, etc. here if needed
)

@Composable
fun RakshyaaTheme(
    /* Use darkColors parameter to switch between light and dark color schemes */
    darkTheme: Boolean = isSystemInDarkTheme(),
    /* content is the UI that will be displayed using this theme */
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFD0BCFF),
            primaryVariant = Color(0xFFB39DDB),
            onPrimary = Color(0xFF381E72),
            secondary = Color(0xFFCCC2DC),
            secondaryVariant = Color(0xFF8E879E),
            onSecondary = Color(0xFF1A1B1E),
            tertiary = Color(0xFFEFB8C8),
            tertiaryVariant = Color(0xFFD4B4C3),
            onTertiary = Color(0xFF331D28),
            background = Color(0xFF1C1B1F),
            onBackground = Color(0xFFE6E1E5),
            surface = Color(0xFF1C1B1F),
            onSurface = Color(0xFFE6E1E5),
            error = Color(0xFFCF6679),
            errorContainer = Color(0xFF93052A),
            onError = Color(0xFFFFFFFF),
            onErrorContainer = Color(0xFF69002D),
            // Custom semantic colors for safety app (adapted for dark theme)
            safetyPrimary = Color(0xFF54E6C7),
            safetySecondary = Color(0xFF64B5F6),
            warning = Color(0xFFFFB74D),
            success = Color(0xFF66BB6A)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6750A4),
            primaryVariant = Color(0xFF4F378B),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF625B71),
            secondaryVariant = Color(0xFF4A4458),
            onSecondary = Color(0xFFFFFFFF),
            tertiary = Color(0xFF7D5260),
            tertiaryVariant = Color(0xFF603D49),
            onTertiary = Color(0xFFFFFFFF),
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            error = Color(0xFFB3261E),
            errorContainer = Color(0xFFF9DEDC),
            onError = Color(0xFFFFFFFF),
            onErrorContainer = Color(0xFF410E0B),
            // Custom semantic colors for safety app
            safetyPrimary = Color(0xFF00695C),
            safetySecondary = Color(0xFF0288D1),
            warning = Color(0xFFEF6C00),
            success = Color(0xFF2E7D32)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}