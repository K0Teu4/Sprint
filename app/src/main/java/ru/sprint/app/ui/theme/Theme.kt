package ru.sprint.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightBackground = Color(0xFFF6F7F9)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFEEF0F4)
private val LightText = Color(0xFF171A1F)
private val LightMuted = Color(0xFF6E7580)
private val LightOutline = Color(0xFFDDE1E7)

private val DarkBackground = Color(0xFF101216)
private val DarkSurface = Color(0xFF191C22)
private val DarkSurfaceVariant = Color(0xFF242831)
private val DarkText = Color(0xFFF3F4F6)
private val DarkMuted = Color(0xFFA8AFBA)
private val DarkOutline = Color(0xFF343943)

@Composable
fun SprintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.GREEN,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = accent.dark,
            onPrimary = Color(0xFF101216),
            primaryContainer = accent.dark.copy(alpha = 0.20f),
            onPrimaryContainer = DarkText,
            secondary = accent.dark,
            secondaryContainer = accent.dark.copy(alpha = 0.14f),
            onSecondaryContainer = DarkText,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            onBackground = DarkText,
            onSurface = DarkText,
            onSurfaceVariant = DarkMuted,
            outline = DarkOutline,
            error = Color(0xFFFF6B6B)
        )
    } else {
        lightColorScheme(
            primary = accent.light,
            onPrimary = Color.White,
            primaryContainer = accent.light.copy(alpha = 0.12f),
            onPrimaryContainer = LightText,
            secondary = accent.light,
            secondaryContainer = accent.light.copy(alpha = 0.10f),
            onSecondaryContainer = LightText,
            background = LightBackground,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            onBackground = LightText,
            onSurface = LightText,
            onSurfaceVariant = LightMuted,
            outline = LightOutline,
            error = Color(0xFFD64545)
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = SprintTypography,
        shapes = SprintShapes,
        content = content
    )
}
