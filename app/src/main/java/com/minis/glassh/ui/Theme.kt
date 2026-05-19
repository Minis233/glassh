package com.minis.glassh.ui

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A4DFF),
    onPrimary = Color.White,
    secondary = Color(0xFF3A8DFF),
    background = Color(0xFFF8F4FF),
    onBackground = Color(0xFF1B1530),
    surface = Color(0xFFF8F4FF),
    onSurface = Color(0xFF1B1530),
    surfaceVariant = Color(0xFFEDE7FA),
    onSurfaceVariant = Color(0xFF453E5C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB6A2FF),
    onPrimary = Color(0xFF1B0F4A),
    secondary = Color(0xFF89C2FF),
    background = Color(0xFF0E0B1A),
    onBackground = Color(0xFFF0EBFF),
    surface = Color(0xFF0E0B1A),
    onSurface = Color(0xFFF0EBFF),
    surfaceVariant = Color(0xFF24203A),
    onSurfaceVariant = Color(0xFFC8C0E5),
)

@Composable
fun GlasshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
