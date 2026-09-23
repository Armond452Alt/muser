package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = NeonGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003822),
    onSecondaryContainer = Color(0xFF6CF8B8),
    tertiary = PlayStationBlue,
    onTertiary = Color.White,
    background = ObsidianDark,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianCardBorder,
    error = AlertRed,
    onError = Color.White
)

private val LightColorScheme = DarkColorScheme // Gaming console audio app is optimized for dark OLED theme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to gaming dark theme
    dynamicColor: Boolean = false, // Keep high contrast gaming accents
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
