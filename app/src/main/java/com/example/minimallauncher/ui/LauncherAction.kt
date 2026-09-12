package com.example.minimallauncher.ui

import androidx.annotation.StringRes
import com.example.minimallauncher.R

/** The outgoing actions the launcher can attempt on the user's behalf. */
enum class LauncherAction {
    OpenApp,
    OpenAppInfo,
    Uninstall,
    OpenHomeSettings,
    OpenClock,
    OpenCalendar,
}

/**
 * An outgoing action that could not be performed.
 *
 * A dead intent used to be indistinguishable from a working one: `openAppInfo`,
 * `uninstall`, `openClock` and `openCalendar` swallowed every failure with a bare
 * `runCatching`, so tapping a menu row simply did nothing, with no message and no
 * log. This type makes the failure observable and reportable.
 */
data class ActionFailure(
    val action: LauncherAction,
    val cause: Throwable,
    /** What the action was about: an app label, or a package name. */
    val subject: String? = null,
)

/**
 * The message to show for a [ActionFailure].
 *
 * Kept separate from the ViewModel so there is no `Context` in state, and pure so
 * every action's message can be asserted in a unit test.
 */
@StringRes
fun ActionFailure.messageResId(): Int = when (action) {
    LauncherAction.OpenApp -> R.string.error_open_app
    LauncherAction.OpenAppInfo -> R.string.error_app_info
    LauncherAction.Uninstall -> R.string.error_uninstall
    LauncherAction.OpenHomeSettings -> R.string.error_home_settings
    LauncherAction.OpenClock -> R.string.error_clock
    LauncherAction.OpenCalendar -> R.string.error_calendar
}
