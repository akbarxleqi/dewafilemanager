package com.dewa.filemanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val MTDarkColorScheme = darkColorScheme(
    primary = MTPrimary,
    onPrimary = MTOnBackground,
    secondary = MTSurface,
    onSecondary = MTOnSurface,
    background = MTBackground,
    onBackground = MTOnBackground,
    surface = MTSurface,
    onSurface = MTOnSurface,
    error = MTError,
    onError = MTOnBackground
)

private val MTLightColorScheme = lightColorScheme(
    primary = MTPrimary,
    onPrimary = MTOnBackground,
    secondary = MTSurfaceLight,
    onSecondary = MTOnSurfaceLight,
    background = MTBackgroundLight,
    onBackground = MTOnBackgroundLight,
    surface = MTSurfaceLight,
    onSurface = MTOnSurfaceLight,
    error = MTError,
    onError = MTOnBackgroundLight
)

@Composable
fun DewaManagerTheme(
    darkTheme: Boolean = true, // Force dark theme for MT Style
    dynamicColor: Boolean = false, // Disable dynamic color to maintain MT look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MTDarkColorScheme else MTLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}