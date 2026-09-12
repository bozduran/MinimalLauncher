package com.example.minimallauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wiring tests for [AppChangeActions].
 *
 * The actual delivery of a broadcast needs an instrumented test; what can be
 * pinned here is *which* changes the launcher listens for, so a refactor cannot
 * silently drop one and leave the app list stale.
 */
class AppChangeActionsTest {

    @Test
    fun `every package change that alters the app list is observed`() {
        assertEquals(
            listOf(
                "android.intent.action.PACKAGE_ADDED",
                "android.intent.action.PACKAGE_REMOVED",
                "android.intent.action.PACKAGE_CHANGED",
                "android.intent.action.PACKAGE_REPLACED",
            ),
            AppChangeActions.PACKAGE_ACTIONS,
        )
    }

    @Test
    fun `a locale change re-sorts the app list`() {
        // AppRepository orders apps with Collator.getInstance(Locale.getDefault()),
        // so a language change must trigger a reload or the order stays stale.
        assertEquals("android.intent.action.LOCALE_CHANGED", AppChangeActions.LOCALE_ACTION)
    }

    @Test
    fun `the locale action is not a package action`() {
        assertFalse(AppChangeActions.LOCALE_ACTION in AppChangeActions.PACKAGE_ACTIONS)
    }

    @Test
    fun `package actions are all distinct`() {
        assertEquals(
            AppChangeActions.PACKAGE_ACTIONS.size,
            AppChangeActions.PACKAGE_ACTIONS.toSet().size,
        )
    }

    @Test
    fun `package actions are non-empty`() {
        assertTrue(AppChangeActions.PACKAGE_ACTIONS.isNotEmpty())
    }
}
