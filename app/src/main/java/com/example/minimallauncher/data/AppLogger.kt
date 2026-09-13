package com.example.minimallauncher.data

import android.util.Log

/**
 * Minimal failure seam.
 *
 * The launcher previously discarded failures inside bare `runCatching {}` blocks,
 * which made "nothing happened" bugs undiagnosable. Handled failures are recorded
 * here instead, so they can be asserted in tests and, later, forwarded to a crash
 * or analytics backend without touching the call sites.
 */
interface AppLogger {

    /**
     * @param tag what was being attempted, e.g. `"app-list-load"`.
     * @param throwable the failure, when there is one.
     * @param message optional human-readable context.
     */
    fun record(tag: String, throwable: Throwable? = null, message: String? = null)
}

/** [AppLogger] that writes to logcat. Used in production builds. */
class LogcatAppLogger : AppLogger {

    override fun record(tag: String, throwable: Throwable?, message: String?) {
        if (throwable != null) {
            Log.e(TAG_PREFIX + tag, message ?: tag, throwable)
        } else {
            Log.w(TAG_PREFIX + tag, message ?: tag)
        }
    }

    private companion object {
        const val TAG_PREFIX = "MinimalLauncher/"
    }
}

/** [AppLogger] that discards everything. Useful as a default where logging is irrelevant. */
object NoOpAppLogger : AppLogger {
    override fun record(tag: String, throwable: Throwable?, message: String?) = Unit
}
