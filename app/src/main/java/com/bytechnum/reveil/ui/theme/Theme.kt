package com.bytechnum.reveil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueContainerLight,
    onPrimaryContainer = BrandBlueDark,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    error = AlertRed,
)

private val darkColors = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = BrandBlueContainerDark,
    primaryContainer = BrandBlueContainerDark,
    onPrimaryContainer = BrandBlueContainerLight,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    error = AlertRedLight,
)

@Composable
fun ReveilTheme(isDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColors else lightColors,
        typography = ReveilTypography,
        content = content,
    )
}
