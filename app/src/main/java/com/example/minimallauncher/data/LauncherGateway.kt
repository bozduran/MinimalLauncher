package com.example.minimallauncher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings

/**
 * The launcher's outgoing actions: opening an app and the system screens it
 * offers from its menus.
 *
 * Every method returns a [Result] instead of throwing, so the caller decides how
 * a failure is surfaced. Previously these were `object AppLauncher` functions
 * called straight from composables with a `Context`, where failures were either
 * discarded or swallowed by a bare `runCatching` — untestable, unlogged, and
 * invisible to the user.
 */
interface LauncherGateway {

    fun launch(app: AppInfo): Result<Unit>

    fun openAppInfo(packageName: String): Result<Unit>

    fun uninstall(packageName: String): Result<Unit>

    fun openHomeSettings(): Result<Unit>

    fun openClock(): Result<Unit>

    fun openCalendar(): Result<Unit>
}

/**
 * [LauncherGateway] backed by `Activity.startActivity`.
 *
 * The launcher is always foreground when these run, so background-activity-start
 * restrictions do not apply.
 */
class AndroidLauncherGateway(private val context: Context) : LauncherGateway {

    override fun launch(app: AppInfo): Result<Unit> {
        // Copy the framework-owned intent before adding flags rather than mutating
        // the instance PackageManager handed back.
        val existing = context.packageManager.getLaunchIntentForPackage(app.packageName)
        val intent = if (existing != null) {
            Intent(existing).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return start(intent)
    }

    override fun openAppInfo(packageName: String): Result<Unit> = start(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    override fun uninstall(packageName: String): Result<Unit> = start(
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    override fun openHomeSettings(): Result<Unit> {
        // ACTION_HOME_SETTINGS is absent on some OEM builds; fall back to the
        // generic settings screen before reporting a failure.
        val homeSettings = start(
            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        if (homeSettings.isSuccess) return homeSettings
        return start(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun openClock(): Result<Unit> = start(
        Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    override fun openCalendar(): Result<Unit> {
        val launch = context.packageManager.getLaunchIntentForPackage(CALENDAR_PACKAGE)
        val intent = if (launch != null) {
            Intent(launch).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(CALENDAR_CONTENT_URI))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return start(intent)
    }

    private fun start(intent: Intent): Result<Unit> = runCatching { context.startActivity(intent) }

    private companion object {
        // TODO(I18N-3): resolve the calendar by capability instead of package name.
        const val CALENDAR_PACKAGE = "com.google.android.calendar"
        const val CALENDAR_CONTENT_URI = "content://com.android.calendar/time"
    }
}
