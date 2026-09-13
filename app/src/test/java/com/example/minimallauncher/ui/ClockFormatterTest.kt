package com.example.minimallauncher.ui

import java.time.LocalDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ClockFormatter] — the 12/24-hour switch and the locale rules
 * behind the home screen's clock and date (see USER_STORIES.md ARCH-4).
 */
class ClockFormatterTest {

    private val english = ClockFormatter(Locale.ENGLISH)
    private val greek = ClockFormatter(Locale.forLanguageTag("el"))

    private fun at(hour: Int, minute: Int, day: Int = 2, month: Int = 1, year: Int = 2024) =
        LocalDateTime.of(year, month, day, hour, minute)

    // ── time: 24-hour ───────────────────────────────────────────────────────

    @Test
    fun `24-hour time is zero padded`() {
        assertEquals("09:05", english.time(at(9, 5), use24h = true))
        assertEquals("00:00", english.time(at(0, 0), use24h = true))
    }

    @Test
    fun `24-hour time does not wrap in the afternoon`() {
        assertEquals("14:30", english.time(at(14, 30), use24h = true))
        assertEquals("23:59", english.time(at(23, 59), use24h = true))
    }

    // ── time: 12-hour ───────────────────────────────────────────────────────

    @Test
    fun `12-hour time is not zero padded`() {
        assertEquals("9:05", english.time(at(9, 5), use24h = false))
    }

    @Test
    fun `12-hour time converts the afternoon`() {
        assertEquals("2:30", english.time(at(14, 30), use24h = false))
    }

    @Test
    fun `midnight renders as twelve in 12-hour mode`() {
        // The classic off-by-one: 00:xx must be 12:xx, not 0:xx.
        assertEquals("12:00", english.time(at(0, 0), use24h = false))
        assertEquals("12:30", english.time(at(0, 30), use24h = false))
    }

    @Test
    fun `noon renders as twelve in 12-hour mode`() {
        assertEquals("12:00", english.time(at(12, 0), use24h = false))
    }

    @Test
    fun `the 12-hour pattern has no am-pm marker by design`() {
        val morning = english.time(at(9, 0), use24h = false)
        val evening = english.time(at(21, 0), use24h = false)

        assertTrue("no AM/PM marker: '$morning'", morning.none { it.isLetter() })
        assertTrue("no AM/PM marker: '$evening'", evening.none { it.isLetter() })
    }

    @Test
    fun `the user preference wins over the locale`() {
        // A Greek device set to 12-hour mode still shows 12-hour time.
        assertEquals("2:30", greek.time(at(14, 30), use24h = false))
        assertEquals("14:30", greek.time(at(14, 30), use24h = true))
    }

    // ── date ────────────────────────────────────────────────────────────────

    @Test
    fun `the date is locale specific`() {
        assertEquals("Tuesday, 2 January", english.date(at(9, 0)))
        assertEquals("Τρίτη, 2 Ιανουαρίου", greek.date(at(9, 0)))
    }

    @Test
    fun `the date capitalises its first letter`() {
        // Russian weekday and month names are lowercase in CLDR data, which is what
        // the titlecase step exists for.
        val russian = ClockFormatter(Locale.forLanguageTag("ru"))
        val formatted = russian.date(at(9, 0))

        assertEquals("Вторник, 2 января", formatted)
        assertTrue("must start uppercase", formatted.first().isUpperCase())
    }

    @Test
    fun `an already capitalised locale is unchanged by titlecasing`() {
        assertEquals("Tuesday, 2 January", english.date(at(9, 0)))
    }

    @Test
    fun `leap day formats correctly`() {
        assertEquals("Thursday, 29 February", english.date(at(9, 0, day = 29, month = 2)))
    }

    @Test
    fun `year boundaries format correctly`() {
        assertEquals("Wednesday, 1 January", english.date(at(9, 0, day = 1, month = 1, year = 2025)))
        assertEquals("Tuesday, 31 December", english.date(at(9, 0, day = 31, month = 12)))
    }

    @Test
    fun `formatting is deterministic and side-effect free`() {
        val first = english.time(at(14, 30), use24h = true)

        repeat(5) { assertEquals(first, english.time(at(14, 30), use24h = true)) }
    }

    @Test
    fun `a formatter is reusable across values`() {
        // Guards against state leaking between calls now that the formatters are
        // compiled once per instance rather than per recomposition.
        assertEquals("09:05", english.time(at(9, 5), use24h = true))
        assertEquals("Tuesday, 2 January", english.date(at(9, 5)))
        assertEquals("9:05", english.time(at(9, 5), use24h = false))
    }
}
