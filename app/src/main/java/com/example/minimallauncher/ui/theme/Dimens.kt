package com.example.minimallauncher.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared spacing and sizing tokens.
 *
 * These replaced literals repeated across `HomeScreen`, `DrawerScreen`,
 * `SettingsScreen` and `Common`, so a layout change is one edit instead of a
 * multi-file search. Only values that play the same role in more than one place
 * are tokenised — genuinely one-off measurements stay inline as literals, which
 * keeps this a design vocabulary rather than a rename exercise.
 */
object Dimens {

    // ── horizontal insets ───────────────────────────────────────────────────

    /** Inset for screen content: home column, drawer rows. */
    val ScreenPadding = 30.dp

    /** Inset for settings and drawer rows, which run slightly wider than the home column. */
    val RowPadding = 28.dp

    /** Inset inside the app-menu dialog. */
    val MenuPadding = 22.dp

    // ── vertical rhythm ─────────────────────────────────────────────────────

    /** Tight gap, e.g. between the clock and the date. */
    val SpaceXs = 6.dp

    /** Gap between an indicator and its label, or between list sections. */
    val SpaceSm = 10.dp

    /** Gap before a trailing control. */
    val SpaceMd = 16.dp

    /** Gap above a section label. */
    val SpaceLg = 22.dp

    // ── row metrics ─────────────────────────────────────────────────────────

    /** Vertical padding of a tappable list row. */
    val RowVertical = 11.dp

    /** Vertical padding of a drawer list row. */
    val DrawerRowVertical = 13.dp

    /** Vertical padding of a dialog menu row. */
    val MenuRowVertical = 14.dp

    /** Vertical padding of the app-menu header. */
    val MenuHeaderVertical = 10.dp

    // ── component sizes ─────────────────────────────────────────────────────

    /** Minimum tappable size, per the Material accessibility guidance. */
    val MinTouchTarget = 48.dp

    /** Inset and height of the settings title bar. */
    val TitlePadding = 24.dp
    val TitleVertical = 18.dp

    /** Corner radius of tappable rows. */
    val RowCorner = 8.dp

    /** Corner radius of the app-menu dialog and theme swatches. */
    val PanelCorner = 16.dp
    val SwatchCorner = 10.dp
}
