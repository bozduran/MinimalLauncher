package com.example.minimallauncher.data

import java.text.Collator

/**
 * Pure ordering/collapsing rules for the launcher's app list.
 *
 * Extracted from the `PackageManager` query so the identity rules — the source of
 * the duplicate-entry and mis-keyed-favorite bugs — are unit-testable without a
 * device.
 */
object AppListOrdering {

    /**
     * Collapses a raw resolve list to **one entry per package**, ordered by label
     * using [collator].
     *
     * An app that publishes several `MAIN`/`LAUNCHER` activities (vendor aliases,
     * per-product variants) would otherwise appear multiple times. The surviving
     * activity is chosen deterministically — the lowest activity name for that
     * package — so the same entry wins across repeated loads and the drawer's
     * `LazyColumn` key stays stable.
     */
    fun collapseAndSort(apps: List<AppInfo>, collator: Collator): List<AppInfo> =
        apps.sortedWith(compareBy({ it.packageName }, { it.activityName }))
            .distinctBy { it.packageName }
            .sortedWith(compareBy(collator) { it.label })
}
