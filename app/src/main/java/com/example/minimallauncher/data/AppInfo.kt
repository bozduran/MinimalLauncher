package com.example.minimallauncher.data

/**
 * A launchable app entry shown by the launcher.
 *
 * Identity is the **package name**: favorites and hidden settings are stored per
 * package, so anything that treats an app as an activity (the previous
 * `packageName/activityName` key) would show one app several times and make a
 * single favorite action apply to all of its launcher activities.
 *
 * [activityName] is kept only as the launch detail.
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    /**
     * Normalised label used for search matching.
     *
     * Computed once when the entry is built (on the IO dispatcher, while the
     * package list is being read) instead of re-running NFC/NFD normalisation over
     * every installed app on every keystroke.
     */
    val searchKey: String = TextNormalizer.normalize(label),
)
