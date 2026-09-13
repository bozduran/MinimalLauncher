package com.example.minimallauncher.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrast audit across every palette in [AppThemes] (A11Y-3).
 *
 * The launcher ships 38 hand-tuned/derived palettes and each one decides whether
 * text is readable, so this runs as a unit test rather than a manual review.
 *
 * **Status of the audit (measured at the time of writing):**
 *
 * | pairing      | min   | max   | palettes below WCAG AA (4.5:1) |
 * |--------------|-------|-------|-------------------------------|
 * | text / bg    | 4.99  | 21.00 | 0                             |
 * | text2 / bg   | 2.44  |  8.15 | 25                            |
 * | text3 / bg   | 1.65  |  5.84 | 37                            |
 * | accent / bg  | 2.21  | 10.45 | 12                            |
 *
 * Primary text is compliant everywhere. Secondary/tertiary text and the accent
 * are **not**, and fixing that means changing the palette derivation (visual
 * design), which is out of scope for this backlog — it is recorded here as an
 * open finding rather than silently loosened into a passing threshold.
 *
 * The "regression floor" tests below therefore assert only that contrast never
 * gets *worse* than the measured values; the characterisation tests pin the size
 * of the remaining gap so it cannot be forgotten.
 */
class PaletteContrastTest {

    private companion object {
        /** WCAG AA for normal-size text. */
        const val AA_NORMAL_TEXT = 4.5

        /** WCAG AA for large text and non-text UI components. */
        const val AA_LARGE_TEXT = 3.0

        // Measured minima — regression floors, NOT accessibility targets.
        const val MEASURED_MIN_TEXT2 = 2.4
        const val MEASURED_MIN_TEXT3 = 1.6
        const val MEASURED_MIN_ACCENT = 2.2
    }

    // ── WCAG 2.1 relative luminance and contrast ratio ───────────────────────

    private fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun relativeLuminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun below(threshold: Double, select: (LauncherPalette) -> Color): List<String> =
        AppThemes
            .filter { contrastRatio(select(it.palette), it.palette.bg) < threshold }
            .map { it.key }

    /** Guards the sanity of the contrast maths itself before trusting any result. */
    @Test
    fun `contrast maths matches known values`() {
        assertEquals(21.0, contrastRatio(Color.White, Color.Black), 0.01)
        assertEquals(1.0, contrastRatio(Color.White, Color.White), 0.01)
    }

    @Test
    fun `every palette has a distinct key and a full palette`() {
        assertEquals(38, AppThemes.size)
        assertEquals(AppThemes.size, AppThemes.map { it.key }.toSet().size)
    }

    // ── requirements that hold today ────────────────────────────────────────

    @Test
    fun `primary text meets WCAG AA on every palette background`() {
        val failing = below(AA_NORMAL_TEXT) { it.text }

        assertTrue(
            "primary text must be readable in every theme; failing: ${failing.joinToString()}",
            failing.isEmpty(),
        )
    }

    // ── regression floors (measured, not targets) ───────────────────────────

    @Test
    fun `secondary text contrast does not regress below the measured floor`() {
        val failing = below(MEASURED_MIN_TEXT2) { it.text2 }

        assertTrue("text2 contrast regressed in: ${failing.joinToString()}", failing.isEmpty())
    }

    @Test
    fun `tertiary text contrast does not regress below the measured floor`() {
        val failing = below(MEASURED_MIN_TEXT3) { it.text3 }

        assertTrue("text3 contrast regressed in: ${failing.joinToString()}", failing.isEmpty())
    }

    @Test
    fun `accent contrast does not regress below the measured floor`() {
        val failing = below(MEASURED_MIN_ACCENT) { it.accent }

        assertTrue("accent contrast regressed in: ${failing.joinToString()}", failing.isEmpty())
    }

    // ── characterisation: the remaining accessibility gap ───────────────────

    @Test
    fun `the number of palettes failing AA for secondary text is recorded`() {
        val failing = below(AA_NORMAL_TEXT) { it.text2 }

        assertEquals(
            "text2 AA compliance changed. If this count dropped, the palette " +
                "derivation improved — update this expectation and the A11Y-3 status " +
                "in USER_STORIES.md.",
            25,
            failing.size,
        )
    }

    @Test
    fun `the number of palettes failing AA for tertiary text is recorded`() {
        val failing = below(AA_NORMAL_TEXT) { it.text3 }

        assertEquals(
            "text3 AA compliance changed. See the note on the secondary-text test.",
            37,
            failing.size,
        )
    }

    @Test
    fun `the number of palettes failing AA-large for the accent is recorded`() {
        val failing = below(AA_LARGE_TEXT) { it.accent }

        assertEquals(
            "accent AA-large compliance changed. See the note on the secondary-text test.",
            3,
            failing.size,
        )
        assertEquals(
            "accent used for the favorite star / switch track must stay above AA-large",
            listOf("ayu-light", "rose-pine-dawn", "everforest-light"),
            failing,
        )
    }
}
