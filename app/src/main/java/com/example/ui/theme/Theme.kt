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
    primary = PharmaCyanAccent,
    onPrimary = Color.White,
    primaryContainer = PharmaBlueDark,
    onPrimaryContainer = PharmaBlueLight,
    secondary = PharmaTeal,
    onSecondary = Color.White,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Slate700,
    onSurface = Slate50,
    onBackground = Slate50,
    error = PharmaError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PharmaBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PharmaBlueLight,
    onPrimaryContainer = PharmaBlueDark,
    secondary = PharmaCyanAccent,
    onSecondary = Color.White,
    tertiary = PharmaTeal,
    background = Slate50,
    surface = Color.White,
    surfaceVariant = Slate100,
    onSurface = Slate900,
    onBackground = Slate900,
    outline = Slate300,
    outlineVariant = Slate200,
    error = PharmaError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded pharmaceutical look
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
