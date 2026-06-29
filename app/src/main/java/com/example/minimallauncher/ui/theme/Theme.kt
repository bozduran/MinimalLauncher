package com.example.minimallauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.luminance

@Composable
fun MinimalLauncherTheme(
    palette: LauncherPalette,
    content: @Composable () -> Unit,
) {
    val isDark = palette.bg.luminance() < 0.5f

    val scheme = if (isDark) {
        darkColorScheme(
            background = palette.bg,
            surface = palette.surface,
            surfaceVariant = palette.surface2,
            onBackground = palette.text,
            onSurface = palette.text,
            primary = palette.accent,
            onPrimary = palette.bg,
            secondary = palette.accentSoft,
            outline = palette.border,
        )
    } else {
        lightColorScheme(
            background = palette.bg,
            surface = palette.surface,
            surfaceVariant = palette.surface2,
            onBackground = palette.text,
            onSurface = palette.text,
            primary = palette.accent,
            onPrimary = palette.bg,
            secondary = palette.accentSoft,
            outline = palette.border,
        )
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content,
        )
    }
}