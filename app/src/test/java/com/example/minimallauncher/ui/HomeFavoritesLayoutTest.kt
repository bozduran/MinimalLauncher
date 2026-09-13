package com.example.minimallauncher.ui

import androidx.compose.ui.unit.dp
import com.example.minimallauncher.data.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HomeFavoritesLayout] — the fix for favorites being clipped off
 * both edges of the home screen (see CODE_AUDIT.md F4).
 */
class HomeFavoritesLayoutTest {

    private fun favorites(count: Int): List<AppInfo> =
        (1..count).map { AppInfo(label = "App $it", packageName = "pkg$it", activityName = "a") }

    private val tallScreen = 900.dp
    private val smallScreen = 640.dp
    private val tinyScreen = 200.dp

    @Test
    fun `capacity grows with the available height`() {
        assertTrue(HomeFavoritesLayout.capacity(tallScreen) > HomeFavoritesLayout.capacity(smallScreen))
    }

    @Test
    fun `a screen shorter than the fixed chrome fits no rows`() {
        assertEquals(0, HomeFavoritesLayout.capacity(tinyScreen))
    }

    @Test
    fun `all favorites are shown when they fit`() {
        assertEquals(favorites(3), HomeFavoritesLayout.visible(favorites(3), tallScreen))
        assertEquals(0, HomeFavoritesLayout.overflowCount(favorites(3), tallScreen))
    }

    @Test
    fun `an empty list stays empty`() {
        assertEquals(emptyList<AppInfo>(), HomeFavoritesLayout.visible(emptyList(), tallScreen))
        assertEquals(0, HomeFavoritesLayout.overflowCount(emptyList(), tallScreen))
    }

    @Test
    fun `overflow reserves a row for the more indicator`() {
        val capacity = HomeFavoritesLayout.capacity(smallScreen)
        val visible = HomeFavoritesLayout.visible(favorites(capacity + 2), smallScreen)

        assertEquals(capacity - 1, visible.size)
        assertTrue("the overflow row must be able to render", visible.size < capacity)
    }

    @Test
    fun `exactly capacity favorites need no overflow row`() {
        val capacity = HomeFavoritesLayout.capacity(smallScreen)

        assertEquals(capacity, HomeFavoritesLayout.visible(favorites(capacity), smallScreen).size)
        assertEquals(0, HomeFavoritesLayout.overflowCount(favorites(capacity), smallScreen))
    }

    @Test
    fun `visible rows are a prefix of the saved favorite order`() {
        val all = favorites(20)
        val visible = HomeFavoritesLayout.visible(all, smallScreen)

        assertEquals(all.take(visible.size), visible)
    }

    @Test
    fun `no favorite is ever lost - visible plus overflow is the whole list`() {
        val all = favorites(30)
        val visible = HomeFavoritesLayout.visible(all, smallScreen)
        val overflow = HomeFavoritesLayout.overflowCount(all, smallScreen)

        assertEquals(all.size, visible.size + overflow)
        assertEquals(all, visible + all.drop(visible.size))
    }

    @Test
    fun `thirty favorites on a small screen remain reachable`() {
        val all = favorites(30)
        val visible = HomeFavoritesLayout.visible(all, smallScreen)

        assertTrue("at least one favorite must be shown", visible.isNotEmpty())
        assertTrue("the rest must be reachable through the overflow row", HomeFavoritesLayout.overflowCount(all, smallScreen) > 0)
    }

    @Test
    fun `the visible rows always fit the available height`() {
        for (height in listOf(320.dp, 480.dp, 640.dp, 800.dp, 1000.dp)) {
            val visible = HomeFavoritesLayout.visible(favorites(30), height)
            val needed = HomeFavoritesLayout.FixedChromeHeight +
                HomeFavoritesLayout.FavoriteRowHeight * visible.size

            assertTrue(
                "at height $height the rendered rows must fit (needed $needed)",
                needed <= height || visible.isEmpty(),
            )
        }
    }
}
