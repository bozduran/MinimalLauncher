package com.example.minimallauncher.ui

/**
 * What Back should do.
 *
 * Extracted from `MainActivity` so the rules are unit-testable and, more
 * importantly, so the one case that would be catastrophic is explicit rather than
 * left to the platform default.
 */
enum class BackAction {

    /** Close the settings surface and return to the previous page. */
    CloseSettings,

    /** Leave the drawer and return to the home screen. */
    GoToHomePage,

    /**
     * Do nothing.
     *
     * On the home screen Back must never finish the Activity: this app is the
     * device's HOME activity, so finishing it would leave the user without a home
     * screen. Previously this case had no handler at all and was whatever the
     * platform default happened to be.
     */
    Stay,
}

/** The launcher's Back rules. */
object BackNavigation {

    fun decide(settingsOpen: Boolean, currentPage: Int): BackAction = when {
        settingsOpen -> BackAction.CloseSettings
        currentPage > 0 -> BackAction.GoToHomePage
        else -> BackAction.Stay
    }
}
