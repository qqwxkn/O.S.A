package com.example.asa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val YellowColorScheme = darkColorScheme(
    primary = OsaYellow,
    onPrimary = OsaOnYellow,
    primaryContainer = OsaYellowDim,
    onPrimaryContainer = Color.White,
    outline = OsaYellow,
    background = OsaBlack,
    onBackground = Color.White,
    surface = OsaSurface,
    onSurface = Color.White,
    surfaceVariant = OsaSurface2,
    onSurfaceVariant = Color.White
)

@Composable
fun ASATheme(
    darkTheme: Boolean = false,
    yellowTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        yellowTheme -> YellowColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
