package com.example.minimallauncher.data

import android.content.Context
import android.content.Intent
import java.text.Collator
import java.util.Locale

class AppRepository(private val context: Context) {

    /** All launchable apps (excluding this launcher), sorted by label using a locale collator. */
    fun loadApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val myPackage = context.packageName
        val collator = Collator.getInstance(Locale.getDefault())

        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { ri ->
                val activity = ri.activityInfo ?: return@mapNotNull null
                if (activity.packageName == myPackage) return@mapNotNull null
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = activity.packageName,
                    activityName = activity.name,
                )
            }
            .distinctBy { it.key }
            .sortedWith(compareBy(collator) { it.label })
    }
}
