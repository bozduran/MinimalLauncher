package com.example.minimallauncher.data

import java.text.Collator

/**
 * Turns raw [LaunchableEntry] values into the launcher's app list.
 *
 * Pure, so every policy decision below is unit-tested rather than requiring a device
 * with two profiles:
 *
 *  - disabled activities are dropped, so the launcher cannot offer an app the system
 *    will refuse to start;
 *  - the launcher excludes itself;
 *  - one entry survives per **(package, profile)** pair. A package that publishes
 *    several launcher activities shows once (the duplicate-entry bug), while the same
 *    package installed in a work profile *and* personally still shows twice, because
 *    those are genuinely different apps;
 *  - the survivor is chosen deterministically so the drawer's `LazyColumn` key does
 *    not move between reloads.
 */
object LaunchableMapper {

    fun map(
        entries: List<LaunchableEntry>,
        collator: Collator,
        selfPackage: String,
        onSkip: (Throwable, String) -> Unit = { _, _ -> },
    ): List<AppInfo> = entries
        .asSequence()
        .filter { it.enabled }
        .filter { it.packageName != selfPackage }
        .mapNotNull { entry ->
            try {
                AppInfo(
                    label = entry.label,
                    packageName = entry.packageName,
                    activityName = entry.activityName,
                    profileKind = entry.profileKind,
                )
            } catch (error: Exception) {
                // A single unreadable entry must not blank the whole list, but the
                // skip is reported rather than silently discarded.
                onSkip(error, entry.packageName)
                null
            }
        }
        .sortedWith(compareBy({ it.packageName }, { it.activityName }))
        .distinctBy { it.packageName to it.profileKind }
        .sortedWith(compareBy(collator) { it.label })
        .toList()
}

/**
 * [AppRepository] over a [LaunchableSource].
 *
 * Keeping the source injectable is what allows the profile-aware path to be tested
 * with fakes; the previous implementation called `PackageManager` directly and could
 * only be exercised on a device.
 */
class SourceBackedAppRepository(
    private val source: LaunchableSource,
    private val selfPackage: String,
    private val logger: AppLogger = NoOpAppLogger,
    private val collator: () -> Collator = { Collator.getInstance(java.util.Locale.getDefault()) },
) : AppRepository {

    override suspend fun loadApps(): List<AppInfo> =
        LaunchableMapper.map(
            entries = source.entries(),
            collator = collator(),
            selfPackage = selfPackage,
            onSkip = { error, packageName ->
                logger.record(TAG, error, "skipping unreadable package: $packageName")
            },
        )

    private companion object {
        const val TAG = "app-enumeration"
    }
}
