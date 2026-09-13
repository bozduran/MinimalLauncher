package com.example.minimallauncher.data

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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

    override fun openClock(): Result<Unit> = openFirstResolvable(SystemScreenTargets.CLOCK, "clock")

    override fun openCalendar(): Result<Unit> =
        openFirstResolvable(SystemScreenTargets.CALENDAR, "calendar")

    /**
     * Opens the first strategy the platform can actually handle.
     *
     * Resolving by capability rather than package name is what makes this work on a
     * device with a non-Google calendar; when nothing can handle any strategy the
     * caller gets a failure to report (STAB-5) instead of a tap that does nothing.
     */
    private fun openFirstResolvable(candidates: List<ScreenTarget>, what: String): Result<Unit> {
        val target = ScreenTargetResolver.firstResolvable(candidates, ::canResolve)
            ?: return Result.failure(
                ActivityNotFoundException("no activity available to open the $what"),
            )
        return start(intentFor(target))
    }

    private fun canResolve(target: ScreenTarget): Boolean =
        context.packageManager.resolveActivity(intentFor(target), 0) != null

    private fun intentFor(target: ScreenTarget): Intent = when (target) {
        is ScreenTarget.Category -> Intent(target.action).apply {
            addCategory(target.category)
        }
        is ScreenTarget.View -> Intent(target.action, Uri.parse(target.uri))
        is ScreenTarget.Action -> Intent(target.action)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun start(intent: Intent): Result<Unit> = runCatching { context.startActivity(intent) }
}
