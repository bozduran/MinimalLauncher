package com.example.minimallauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Process
import android.os.UserManager

/**
 * Which profile a launchable app belongs to.
 *
 * Kept as a small enum rather than a raw user id so it can travel through the pure
 * mapping layer and be unit-tested without the Android user machinery.
 */
enum class ProfileKind {
    /** The profile the launcher itself runs in. */
    Current,

    /**
     * Any other profile: a work profile, a secondary user, a private space.
     *
     * Deliberately not split into "work" and "other". Android exposes no public API
     * to tell them apart for another user — `UserManager.isManagedProfile(int)` and
     * `UserManager.getUserInfo(int)` are `@SystemApi`, and `UserHandle`'s identifier
     * accessor is newer than this app's minSdk. Reporting every non-current profile
     * honestly is better than guessing one is a work profile from a heuristic.
     */
    Other,
}

/**
 * One launchable activity as the platform reports it, before any launcher-specific
 * policy is applied.
 *
 * This is the seam that makes profile handling testable: the platform-specific
 * enumeration lives in a [LaunchableSource], and everything the launcher decides
 * afterwards is a pure function over these values.
 */
data class LaunchableEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val profileKind: ProfileKind = ProfileKind.Current,
    val enabled: Boolean = true,
)

/**
 * A source of launchable activities.
 *
 * Two implementations exist because neither is sufficient alone: `LauncherApps` is
 * the API intended for launchers and is profile-aware, while `PackageManager` is the
 * only option when `LauncherApps` is unavailable or returns nothing.
 */
interface LaunchableSource {

    fun entries(): List<LaunchableEntry>
}

/**
 * [LaunchableSource] backed by `LauncherApps`.
 *
 * This is the API a launcher is supposed to use: it reports enabled state and sees
 * every profile in the caller's profile group, which `PackageManager` does not — the
 * reason work-profile apps were previously invisible.
 */
class LauncherAppsLaunchableSource(private val context: Context) : LaunchableSource {

    override fun entries(): List<LaunchableEntry> {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
            ?: return emptyList()
        val userManager = context.getSystemService(UserManager::class.java) ?: return emptyList()
        val currentUser = Process.myUserHandle()

        return try {
            userManager.userProfiles.flatMap { user ->
                val profileKind =
                    if (user == currentUser) ProfileKind.Current else ProfileKind.Other
                launcherApps.getActivityList(null, user).map { activity ->
                    LaunchableEntry(
                        label = activity.label?.toString().orEmpty(),
                        packageName = activity.componentName.packageName,
                        activityName = activity.componentName.className,
                        profileKind = profileKind,
                        enabled = activity.applicationInfo?.enabled ?: true,
                    )
                }
            }
        } catch (error: Exception) {
            // Let the caller decide: it has a fallback and a logger.
            throw LaunchableSourceException("LauncherApps enumeration failed", error)
        }
    }
}

/** [LaunchableSource] backed by `PackageManager.queryIntentActivities`. */
class PackageManagerLaunchableSource(private val context: Context) : LaunchableSource {

    override fun entries(): List<LaunchableEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
            toEntry(resolveInfo, pm)
        }
    }

    private fun toEntry(resolveInfo: ResolveInfo, pm: PackageManager): LaunchableEntry? {
        val activity = resolveInfo.activityInfo ?: return null
        return LaunchableEntry(
            label = resolveInfo.loadLabel(pm).toString(),
            packageName = activity.packageName,
            activityName = activity.name,
            profileKind = ProfileKind.Current,
            enabled = activity.enabled,
        )
    }
}

/**
 * Tries [primary] and falls back to [secondary].
 *
 * The fallback is a deliberate, recorded decision rather than a silent empty list:
 * if `LauncherApps` is unavailable or reports nothing, the launcher must still show
 * apps, and the reason must be diagnosable.
 */
class FallbackLaunchableSource(
    private val primary: LaunchableSource,
    private val secondary: LaunchableSource,
    private val logger: AppLogger,
) : LaunchableSource {

    override fun entries(): List<LaunchableEntry> {
        val primaryEntries = try {
            primary.entries()
        } catch (error: Exception) {
            logger.record(TAG, error, "primary app enumeration failed; using the fallback")
            return secondary.entries()
        }

        if (primaryEntries.isEmpty()) {
            logger.record(TAG, null, "primary app enumeration returned nothing; using the fallback")
            return secondary.entries()
        }
        return primaryEntries
    }

    private companion object {
        const val TAG = "app-enumeration"
    }
}

/** Raised when a source cannot enumerate at all, as opposed to returning nothing. */
class LaunchableSourceException(message: String, cause: Throwable) : Exception(message, cause)
