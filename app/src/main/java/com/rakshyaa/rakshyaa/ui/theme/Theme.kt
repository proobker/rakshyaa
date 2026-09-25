package com.rakshyaa.rakshyaa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SafetyPrimary = Color(0xFF00695C)
val SafetySecondary = Color(0xFF0288D1)
val Warning = Color(0xFFEF6C00)
val Success = Color(0xFF2E7D32)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2F0E2),
    onPrimaryContainer = Color(0xFF002F28),
    secondary = Color(0xFF49665D),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F9F6),
    onBackground = Color(0xFF182D27),
    surface = Color(0xFFF6F9F6),
    onSurface = Color(0xFF182D27),
    onSurfaceVariant = Color(0xFF486158),
    surfaceContainer = Color(0xFFEAF1EC),
    surfaceContainerHighest = Color(0xFFDCE7DF),
    outlineVariant = Color(0xFFBFCFC5),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF85D5BE),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFFC2F0E2),
    secondary = Color(0xFFB0CCC0),
    onSecondary = Color(0xFF1A1B1E),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF101C18),
    onBackground = Color(0xFFE0EBE3),
    surface = Color(0xFF101C18),
    onSurface = Color(0xFFE0EBE3),
    onSurfaceVariant = Color(0xFFB6CBC0),
    surfaceContainer = Color(0xFF1C2B24),
    surfaceContainerHighest = Color(0xFF2E4036),
    outlineVariant = Color(0xFF40584B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF69002D)
)

@Composable
fun RakshyaaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
