package com.example.minimallauncher.data

import android.provider.AlarmClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SystemScreenTargets] and [ScreenTargetResolver] (I18N-3).
 *
 * The resolver is the fix for the hardcoded `com.google.android.calendar` package:
 * on a device without Google Calendar both the package lookup and the content-URI
 * fallback failed, so tapping the date did nothing at all.
 */
class SystemScreenTargetsTest {

    // ── the constants must match the platform's ─────────────────────────────

    @Test
    fun `the alarms actions match the platform constants`() {
        // These are compile-time String constants, so comparing them needs no
        // Android runtime — and it catches a typo that would otherwise silently make
        // the clock strategy unresolvable.
        assertEquals(AlarmClock.ACTION_SHOW_ALARMS, SystemScreenTargets.ACTION_SHOW_ALARMS)
        assertEquals(AlarmClock.ACTION_SET_ALARM, SystemScreenTargets.ACTION_SET_ALARM)
    }

    @Test
    fun `the calendar category matches the platform constant`() {
        assertEquals(
            android.content.Intent.CATEGORY_APP_CALENDAR,
            SystemScreenTargets.CATEGORY_APP_CALENDAR,
        )
    }

    @Test
    fun `the main and view actions match the platform constants`() {
        assertEquals(android.content.Intent.ACTION_MAIN, SystemScreenTargets.ACTION_MAIN)
        assertEquals(android.content.Intent.ACTION_VIEW, SystemScreenTargets.ACTION_VIEW)
    }

    // ── no hardcoded third-party packages ───────────────────────────────────

    @Test
    fun `no strategy names a third-party package`() {
        val all = SystemScreenTargets.CALENDAR + SystemScreenTargets.CLOCK
        val withPackage = all.filter { target ->
            when (target) {
                is ScreenTarget.Category -> target.category.contains("com.google")
                is ScreenTarget.View -> target.uri.contains("com.google")
                is ScreenTarget.Action -> target.action.contains("com.google")
            }
        }

        assertTrue("strategies must not hardcode a vendor package: $withPackage", withPackage.isEmpty())
    }

    // ── strategy order ──────────────────────────────────────────────────────

    @Test
    fun `the calendar prefers resolving the platform category before the content uri`() {
        val first = SystemScreenTargets.CALENDAR.first()

        assertEquals(
            ScreenTarget.Category(
                SystemScreenTargets.ACTION_MAIN,
                SystemScreenTargets.CATEGORY_APP_CALENDAR,
            ),
            first,
        )
    }

    @Test
    fun `the clock prefers showing alarms before setting one`() {
        assertEquals(
            listOf(
                ScreenTarget.Action(SystemScreenTargets.ACTION_SHOW_ALARMS),
                ScreenTarget.Action(SystemScreenTargets.ACTION_SET_ALARM),
            ),
            SystemScreenTargets.CLOCK,
        )
    }

    @Test
    fun `both strategies have a fallback`() {
        assertTrue("a single-strategy list has nothing to fall back to", SystemScreenTargets.CALENDAR.size > 1)
        assertTrue(SystemScreenTargets.CLOCK.size > 1)
    }

    // ── resolver ────────────────────────────────────────────────────────────

    @Test
    fun `the resolver picks the first candidate the platform accepts`() {
        val chosen = ScreenTargetResolver.firstResolvable(SystemScreenTargets.CALENDAR) { true }

        assertEquals(SystemScreenTargets.CALENDAR.first(), chosen)
    }

    @Test
    fun `the resolver skips candidates the platform rejects`() {
        val chosen = ScreenTargetResolver.firstResolvable(SystemScreenTargets.CALENDAR) { target ->
            target is ScreenTarget.View
        }

        assertTrue(chosen is ScreenTarget.View)
    }

    @Test
    fun `the resolver returns null when nothing can be handled`() {
        // This is the case that must surface a failure rather than a dead tap.
        assertNull(ScreenTargetResolver.firstResolvable(SystemScreenTargets.CLOCK) { false })
    }

    @Test
    fun `the resolver respects the declared order`() {
        val onlyLast = ScreenTargetResolver.firstResolvable(SystemScreenTargets.CLOCK) { target ->
            target == SystemScreenTargets.CLOCK.last()
        }

        assertEquals(SystemScreenTargets.CLOCK.last(), onlyLast)
    }

    @Test
    fun `the resolver handles an empty candidate list`() {
        assertNull(ScreenTargetResolver.firstResolvable(emptyList<ScreenTarget>()) { true })
    }
}
