package com.example.minimallauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import java.text.Collator
import java.util.Locale

/**
 * [AppRepository] backed by `PackageManager.queryIntentActivities`.
 *
 * The `<queries>` declaration in the manifest (MAIN/LAUNCHER) is what makes this
 * work under Android 11+ package-visibility rules.
 */
class PackageManagerAppRepository(private val context: Context) : AppRepository {

    /** All launchable apps (excluding this launcher), sorted by label using a locale collator. */
    override suspend fun loadApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val myPackage = context.packageName
        val collator = Collator.getInstance(Locale.getDefault())

        return pm.queryIntentActivities(intent, 0)
            // A single unreadable package (label lookup racing an uninstall, an OEM
            // PackageManager throwing where AOSP returns null) must not fail the
            // whole enumeration and leave the launcher with no app list at all.
            .mapNotNull { resolveInfo -> runCatching { toAppInfo(resolveInfo, pm, myPackage) }.getOrNull() }
            .let { AppListOrdering.collapseAndSort(it, collator) }
    }

    private fun toAppInfo(resolveInfo: ResolveInfo, pm: PackageManager, myPackage: String): AppInfo? {
        val activity = resolveInfo.activityInfo ?: return null
        if (activity.packageName == myPackage) return null
        return AppInfo(
            label = resolveInfo.loadLabel(pm).toString(),
            packageName = activity.packageName,
            activityName = activity.name,
        )
    }
}
