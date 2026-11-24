package com.m3u.smartphone.ui.material.model

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.m3u.core.foundation.architecture.preferences.ThemeStyle
import com.m3u.smartphone.ui.material.ktx.createAppColorScheme
import androidx.compose.ui.graphics.Color

val LocalThemeStyle = staticCompositionLocalOf { ThemeStyle.MATERIAL }

@Composable
@SuppressLint("RestrictedApi")
fun Theme(
    argb: Int,
    themeStyle: Int,
    useDynamicColors: Boolean,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    typography: Typography,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val supportsDynamicTheming = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val baseColorScheme = if (useDynamicColors && supportsDynamicTheming) {
        if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        remember(useDarkTheme, argb, themeStyle) {
            createAppColorScheme(argb, useDarkTheme, themeStyle)
        }
    }
    // Pure black (AMOLED) in dark mode. Cards stay slightly lighter so they remain visible.
    val colorScheme = if (useDarkTheme) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF111111),
            surfaceContainerHigh = Color(0xFF181818),
            surfaceContainerHighest = Color(0xFF202020),
        )
    } else {
        baseColorScheme
    }
    val resolvedStyle = themeStyle.takeIf { it == ThemeStyle.WARM_EDITORIAL }
        ?: ThemeStyle.MATERIAL

    CompositionLocalProvider(
        LocalThemeStyle provides resolvedStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
