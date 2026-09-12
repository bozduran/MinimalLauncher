package com.example.minimallauncher.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Whether the launch window should be light or dark.
 *
 * The OS draws `windowBackground` before the app's first frame, so it cannot come
 * from DataStore (which is asynchronous). The previous theme hardcoded the dark
 * background and `windowLightStatusBar=false`, so a user on one of the 19 light
 * palettes saw a dark flash and unreadable status-bar icons on every press of Home.
 */
enum class LaunchTheme {
    Dark,
    Light,
    ;

    /**
     * Which launch window suits [palette].
     *
     * Pure so every palette can be checked in a unit test rather than by installing
     * all 38 of them.
     */
    companion object {

        /** Matches the boundary [MinimalLauncherTheme] uses to choose a Material scheme. */
        const val DARK_BELOW_LUMINANCE = 0.5f

        fun forBackground(background: Color): LaunchTheme =
            if (background.luminance() < DARK_BELOW_LUMINANCE) Dark else Light

        fun forThemeKey(key: String?): LaunchTheme =
            forBackground(paletteFor(key ?: DEFAULT_THEME_KEY).bg)
    }
}

/**
 * A synchronously-readable mirror of the selected theme key.
 *
 * DataStore is the source of truth for settings, but it cannot be read on the main
 * thread during `onCreate`. This small `SharedPreferences` mirror exists only so the
 * launch window can be given the right background before the first frame; it is
 * written whenever the theme changes and is never read for anything else.
 */
class LaunchThemeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The last selected theme key, or null if the user has never chosen one. */
    fun savedThemeKey(): String? = prefs.getString(KEY_THEME, null)

    fun saveThemeKey(key: String) {
        prefs.edit().putString(KEY_THEME, key).apply()
    }

    /** The launch window to use before DataStore has been read. */
    fun launchTheme(): LaunchTheme = LaunchTheme.forThemeKey(savedThemeKey())

    private companion object {
        const val PREFS_NAME = "launcher_launch_theme"
        const val KEY_THEME = "theme_key"
    }
}
