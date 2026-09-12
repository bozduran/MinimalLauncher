package com.example.minimallauncher.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.data.AppInfo

/**
 * How many favorites fit on the home screen.
 *
 * The home screen is a fixed, centred, **non-scrollable** column and the only
 * vertical gesture belongs to the `VerticalPager` that switches to the drawer, so
 * the list cannot be made scrollable without the two fighting each other. With
 * `Arrangement.Center` and no scroll, once the content exceeded the viewport the
 * negative free space pushed it off **both** edges: the clock was clipped at the
 * top and the last favorites at the bottom, and neither was reachable.
 *
 * The fix is to cap the list and show an explicit overflow row that switches to
 * the drawer, where every app (including every favorite) is listed. This object is
 * pure and carries no Compose UI state, so the capacity arithmetic is unit-tested
 * rather than eyeballed on one device.
 */
object HomeFavoritesLayout {

    /** Clock + date + spacers + the "↑ apps" footer, measured from HomeScreen. */
    val FixedChromeHeight: Dp = 206.dp

    /** One favorite row: 23sp text plus 11dp vertical padding either side. */
    val FavoriteRowHeight: Dp = 50.dp

    /** How many rows fit in [availableHeight] above the fixed chrome. */
    fun capacity(availableHeight: Dp): Int {
        val usable = availableHeight - FixedChromeHeight
        if (usable < FavoriteRowHeight) return 0
        return (usable / FavoriteRowHeight).toInt()
    }

    /**
     * The favorites to render directly.
     *
     * When not everything fits, one row is reserved for the overflow indicator, so
     * "…and 4 more" is always visible and the remaining apps are one gesture away
     * in the drawer rather than clipped off-screen.
     */
    fun visible(favorites: List<AppInfo>, availableHeight: Dp): List<AppInfo> {
        val capacity = capacity(availableHeight)
        if (favorites.size <= capacity) return favorites
        return favorites.take((capacity - 1).coerceAtLeast(0))
    }

    /** How many favorites are not rendered directly; 0 means everything fits. */
    fun overflowCount(favorites: List<AppInfo>, availableHeight: Dp): Int =
        favorites.size - visible(favorites, availableHeight).size
}
