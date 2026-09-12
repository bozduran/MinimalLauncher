package com.example.minimallauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TextNormalizer] — the launcher's Greek/Latin search folding.
 *
 * Pure JVM: no Robolectric, no device. See USER_STORIES.md QA-1.
 */
class TextNormalizerTest {

    // ── normalize(): Greek ──────────────────────────────────────────────────

    @Test
    fun `normalize strips Greek tonos and lowercases`() {
        assertEquals("αθηνα", TextNormalizer.normalize("Αθήνα"))
    }

    @Test
    fun `normalize handles Greek dialytika`() {
        // ϊ (U+03CA) decomposes to ι + combining diaeresis, which is stripped.
        assertEquals("γαια", TextNormalizer.normalize("γαϊα"))
    }

    @Test
    fun `normalize folds Greek final sigma`() {
        assertEquals("οδυσσευσ", TextNormalizer.normalize("Οδυσσεύς"))
    }

    @Test
    fun `final sigma and medial sigma normalise identically`() {
        assertEquals(
            TextNormalizer.normalize("ΟΔΥΣΣΕΥΣ"),
            TextNormalizer.normalize("Οδυσσεύς"),
        )
    }

    // ── normalize(): Latin ──────────────────────────────────────────────────

    @Test
    fun `normalize strips Latin accents`() {
        assertEquals("cafe", TextNormalizer.normalize("Café"))
    }

    @Test
    fun `normalize lowercases plain ASCII`() {
        assertEquals("chrome", TextNormalizer.normalize("Chrome"))
    }

    // ── normalize(): degenerate input ───────────────────────────────────────

    @Test
    fun `normalize of empty string is empty and does not throw`() {
        assertEquals("", TextNormalizer.normalize(""))
    }

    @Test
    fun `normalize preserves digits and punctuation`() {
        assertEquals("7-11", TextNormalizer.normalize("7-11"))
    }

    @Test
    fun `normalize is idempotent`() {
        val once = TextNormalizer.normalize("Αθήνα Café Οδυσσεύς")
        assertEquals(once, TextNormalizer.normalize(once))
    }

    // ── Locale independence (Turkish dotless i) ─────────────────────────────

    @Test
    fun `lowercasing is locale independent - ASCII I becomes dotted i`() {
        // Kotlin's String.lowercase() uses Locale.ROOT, so the Turkish
        // locale-specific mapping (I -> ı) must NOT apply. This is deliberate:
        // the search key must not change with the device locale.
        assertEquals("i", TextNormalizer.normalize("I"))
    }

    @Test
    fun `dotless i is not folded onto ASCII i`() {
        // Documented limitation: 'ı' (U+0131) has no combining mark to strip and
        // is a distinct letter, so it does not match 'i'. Pinned so the behaviour
        // is intentional rather than accidental.
        assertFalse(TextNormalizer.matches("I", "ı"))
    }

    @Test
    fun `Turkish dotted capital I folds onto ASCII i`() {
        // 'İ' (U+0130) lowercases to i + combining dot above under Locale.ROOT,
        // and the combining mark is then stripped.
        assertEquals("istanbul", TextNormalizer.normalize("İstanbul"))
    }

    // ── matches(): blank query contract ─────────────────────────────────────

    @Test
    fun `blank query matches everything`() {
        assertTrue(TextNormalizer.matches("Chrome", ""))
        assertTrue(TextNormalizer.matches("Chrome", "   "))
        assertTrue(TextNormalizer.matches("", ""))
    }

    // ── matches(): case and accent symmetry ─────────────────────────────────

    @Test
    fun `matching is accent insensitive in both directions`() {
        assertTrue(TextNormalizer.matches("Café", "cafe"))
        assertTrue(TextNormalizer.matches("Cafe", "café"))
        assertTrue(TextNormalizer.matches("Αθήνα", "αθηνα"))
        assertTrue(TextNormalizer.matches("Αθηνα", "αθήνα"))
    }

    @Test
    fun `matching is case insensitive for Greek and Latin`() {
        assertTrue(TextNormalizer.matches("Αθήνα", "ΑΘΗΝΑ"))
        assertTrue(TextNormalizer.matches("CHROME", "chrome"))
        assertTrue(TextNormalizer.matches("chrome", "CHROME"))
    }

    @Test
    fun `matching finds substrings anywhere in the label`() {
        assertTrue(TextNormalizer.matches("Google Maps", "maps"))
        assertTrue(TextNormalizer.matches("Google Maps", "goo"))
        assertTrue(TextNormalizer.matches("Google Maps", "e ma"))
    }

    @Test
    fun `matching rejects absent queries`() {
        assertFalse(TextNormalizer.matches("Chrome", "firefox"))
        assertFalse(TextNormalizer.matches("", "chrome"))
    }

    @Test
    fun `matching folds the final sigma in the query`() {
        assertTrue(TextNormalizer.matches("Οδυσσεύς", "οδυσσευσ"))
        assertTrue(TextNormalizer.matches("Οδυσσεύς", "ΟΔΥΣΣΕΥΣ"))
    }
}
