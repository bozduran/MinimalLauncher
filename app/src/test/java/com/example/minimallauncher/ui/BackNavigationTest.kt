package com.example.minimallauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [BackNavigation] (USER_STORIES.md PLAT-2).
 *
 * The important case is the last one: this app is the device's HOME activity, so
 * "Back on the home screen" must be an explicit no-op. If it ever resolved to
 * finishing the Activity the user would be left without a home screen.
 */
class BackNavigationTest {

    @Test
    fun `back closes settings before anything else`() {
        assertEquals(BackAction.CloseSettings, BackNavigation.decide(settingsOpen = true, currentPage = 0))
        assertEquals(BackAction.CloseSettings, BackNavigation.decide(settingsOpen = true, currentPage = 1))
    }

    @Test
    fun `back on the drawer returns to the home screen`() {
        assertEquals(BackAction.GoToHomePage, BackNavigation.decide(settingsOpen = false, currentPage = 1))
    }

    @Test
    fun `back on the home screen does nothing`() {
        assertEquals(BackAction.Stay, BackNavigation.decide(settingsOpen = false, currentPage = 0))
    }

    @Test
    fun `there is no action that finishes the launcher`() {
        // Guards the HOME-activity rule: every reachable combination must resolve to
        // one of the three handled actions, and none of them finishes the Activity.
        val allActions = (0..2).flatMap { page ->
            listOf(true, false).map { settings -> BackNavigation.decide(settings, page) }
        }

        assertEquals(
            setOf(BackAction.CloseSettings, BackAction.GoToHomePage, BackAction.Stay),
            allActions.toSet(),
        )
    }

    @Test
    fun `settings takes precedence over the page`() {
        // Settings is a full-screen surface: Back must close it rather than also
        // navigating the pager underneath.
        assertEquals(BackAction.CloseSettings, BackNavigation.decide(settingsOpen = true, currentPage = 2))
    }

    @Test
    fun `a negative or unknown page is treated as the home screen`() {
        assertEquals(BackAction.Stay, BackNavigation.decide(settingsOpen = false, currentPage = -1))
    }
}
