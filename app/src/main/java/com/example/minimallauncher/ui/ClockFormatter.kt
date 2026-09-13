package com.example.minimallauncher.ui

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Clock and date formatting for the home screen.
 *
 * Extracted from the composable so the 12/24-hour behaviour and the locale rules
 * can be unit-tested, and so the [DateTimeFormatter]s are compiled once per
 * instance instead of on every recomposition (the clock recomposes every minute).
 */
class ClockFormatter(private val locale: Locale = Locale.getDefault()) {

    private val pattern24 = DateTimeFormatter.ofPattern("HH:mm", locale)

    /**
     * 12-hour pattern without an AM/PM marker.
     *
     * A deliberate design choice, not an oversight: the original UI rendered
     * `h:mm` alone, and the user's explicit 24h/12h preference wins over the
     * locale's own convention (a Greek phone set to 12h still shows no marker).
     */
    private val pattern12 = DateTimeFormatter.ofPattern("h:mm", locale)

    private val patternDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)

    fun time(now: LocalDateTime, use24h: Boolean): String =
        now.format(if (use24h) pattern24 else pattern12)

    /**
     * The date with its first letter capitalised, so it reads naturally in locales
     * (Greek among them) whose month and weekday names are not capitalised by
     * `EEEE, d MMMM` on their own.
     */
    fun date(now: LocalDateTime): String =
        now.format(patternDate).replaceFirstChar { it.titlecase(locale) }
}
