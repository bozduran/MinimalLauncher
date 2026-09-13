package com.example.minimallauncher.data

/**
 * A way of reaching a system screen, described independently of `Intent`
 * construction.
 *
 * This exists so the *fallback order* is unit-testable. `Intent` cannot be
 * constructed in a JVM test, so a gateway that built intents inline could only be
 * verified by installing the app on a device without Google Calendar.
 */
sealed interface ScreenTarget {

    /** `ACTION_MAIN` plus a category, e.g. `CATEGORY_APP_CALENDAR`. */
    data class Category(val action: String, val category: String) : ScreenTarget

    /** `ACTION_VIEW` on a content URI, e.g. the calendar provider. */
    data class View(val action: String, val uri: String) : ScreenTarget

    /** A bare action, e.g. `AlarmClock.ACTION_SHOW_ALARMS`. */
    data class Action(val action: String) : ScreenTarget
}

/**
 * The ordered strategies for opening the clock and calendar.
 *
 * Deliberately free of hardcoded third-party package names: the previous
 * implementation tried `com.google.android.calendar` and then a
 * `content://com.android.calendar/time` URI, so on any device without Google
 * Calendar both failed and the user saw nothing happen.
 */
object SystemScreenTargets {

    const val ACTION_MAIN = "android.intent.action.MAIN"
    const val ACTION_VIEW = "android.intent.action.VIEW"
    const val CATEGORY_APP_CALENDAR = "android.intent.category.APP_CALENDAR"

    /**
     * Value of `AlarmClock.ACTION_SHOW_ALARMS`.
     *
     * Spelled out rather than importing the platform constant so this file stays
     * free of `android.*` and remains usable from a JVM test; a test asserts it
     * matches the real `AlarmClock` constant.
     */
    const val ACTION_SHOW_ALARMS = "android.intent.action.SHOW_ALARMS"

    /** Value of `AlarmClock.ACTION_SET_ALARM`; the fallback when a device has no alarms screen. */
    const val ACTION_SET_ALARM = "android.intent.action.SET_ALARM"

    /** The platform calendar provider's "today" view. */
    const val CALENDAR_CONTENT_URI = "content://com.android.calendar/time"

    /**
     * Calendar strategies, best first.
     *
     * `CATEGORY_APP_CALENDAR` is resolved by the platform against whatever calendar
     * the user actually has, which is the point: capability, not package name.
     */
    val CALENDAR: List<ScreenTarget> = listOf(
        ScreenTarget.Category(ACTION_MAIN, CATEGORY_APP_CALENDAR),
        ScreenTarget.View(ACTION_VIEW, CALENDAR_CONTENT_URI),
    )

    /** Clock strategies, best first. */
    val CLOCK: List<ScreenTarget> = listOf(
        ScreenTarget.Action(ACTION_SHOW_ALARMS),
        ScreenTarget.Action(ACTION_SET_ALARM),
    )
}

/** Chooses the first candidate a platform can actually handle. */
object ScreenTargetResolver {

    /**
     * The first target in [candidates] that [canResolve] accepts, or null when none
     * can be handled — in which case the caller reports a failure rather than
     * disappearing silently.
     */
    fun <T> firstResolvable(candidates: List<T>, canResolve: (T) -> Boolean): T? =
        candidates.firstOrNull(canResolve)
}
