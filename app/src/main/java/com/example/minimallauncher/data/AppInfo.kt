package com.example.minimallauncher.data

/**
 * A launchable app entry shown by the launcher.
 *
 * Identity is the **package name**: favorites and hidden settings are stored per
 * package, so anything that treated an app as an activity (the previous
 * `packageName/activityName` key) would show one app several times and make a single
 * favorite action apply to all of its launcher activities.
 *
 * [activityName] is kept only as the launch detail, and [profileKind] records which
 * profile the app came from so the drawer can mark work-profile apps.
 *
 * Note: favorites and hidden settings are still keyed by package name alone, so an
 * app installed both personally and in a work profile shares one favorite/hidden
 * state. Distinguishing them needs a settings-key migration, which is recorded as an
 * open item in USER_STORIES.md (PLAT-1).
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    /**
     * Normalised label used for search matching.
     *
     * Computed once when the entry is built (on the IO dispatcher, while the package
     * list is being read) instead of re-running NFC/NFD normalisation over every
     * installed app on every keystroke.
     */
    val searchKey: String = TextNormalizer.normalize(label),
    val profileKind: ProfileKind = ProfileKind.Current,
)
