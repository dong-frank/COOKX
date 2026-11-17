package com.cookx.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF34634C), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBDC), onPrimaryContainer = Color(0xFF173D2C),
    secondary = Color(0xFF926042), secondaryContainer = Color(0xFFDCEBDC),
    onSecondaryContainer = Color(0xFF173D2C),
    background = Color(0xFFFAF8F2), surface = Color(0xFFFAF8F2),
    surfaceContainer = Color(0xFFF1EEE5), surfaceContainerLow = Color(0xFFF5F2EA),
)
private val Dark = darkColorScheme(primary = Color(0xFFA3D1AF), secondary = Color(0xFFEAB895))
@Composable fun COOKXTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = Typography, content = content)
}
