package com.example.minimallauncher.ui.theme

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [LaunchTheme] — the launch window that is painted before the app's
 * first frame (see USER_STORIES.md PERF-4).
 *
 * Getting this wrong is very visible: the window background and the system-bar icon
 * colour are chosen from it, so a wrong answer shows as a dark flash with unreadable
 * icons on every press of Home.
 */
class LaunchThemeTest {

    @Test
    fun `a dark palette gets the dark launch theme`() {
        assertEquals(LaunchTheme.Dark, LaunchTheme.forBackground(paletteFor("warm-dark").bg))
        assertEquals(LaunchTheme.Dark, LaunchTheme.forBackground(paletteFor("dracula").bg))
        assertEquals(LaunchTheme.Dark, LaunchTheme.forBackground(paletteFor("nord").bg))
    }

    @Test
    fun `a light palette gets the light launch theme`() {
        assertEquals(LaunchTheme.Light, LaunchTheme.forBackground(paletteFor("paper").bg))
        assertEquals(LaunchTheme.Light, LaunchTheme.forBackground(paletteFor("github-light").bg))
        assertEquals(LaunchTheme.Light, LaunchTheme.forBackground(paletteFor("solarized-light").bg))
    }

    @Test
    fun `every selectable palette resolves to a matching launch theme`() {
        // Iterating AppThemes rather than hardcoding names means a new palette is
        // covered automatically.
        val mismatched = AppThemes.filter { option ->
            val expected = if (option.palette.bg.luminance() < LaunchTheme.DARK_BELOW_LUMINANCE) {
                LaunchTheme.Dark
            } else {
                LaunchTheme.Light
            }
            LaunchTheme.forThemeKey(option.key) != expected
        }.map { it.key }

        assertTrue("launch theme disagrees with the palette for: $mismatched", mismatched.isEmpty())
    }

    @Test
    fun `all 38 palettes are covered`() {
        assertEquals(38, AppThemes.size)
    }

    @Test
    fun `an unknown theme key falls back to the default palette's launch theme`() {
        val expected = LaunchTheme.forThemeKey(DEFAULT_THEME_KEY)

        assertEquals(expected, LaunchTheme.forThemeKey(null))
        assertEquals(expected, LaunchTheme.forThemeKey("no-such-theme"))
    }

    @Test
    fun `the default palette is dark`() {
        // The manifest theme is dark, so a first launch with no saved choice must not
        // flash a light window.
        assertEquals(LaunchTheme.Dark, LaunchTheme.forThemeKey(null))
    }

    @Test
    fun `the light and dark launch themes are distinct`() {
        assertTrue(LaunchTheme.Light != LaunchTheme.Dark)
        assertEquals(2, LaunchTheme.entries.size)
    }
}
