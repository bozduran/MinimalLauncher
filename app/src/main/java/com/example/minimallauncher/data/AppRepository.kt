package com.example.minimallauncher.data

/**
 * Source of the launchable apps shown by the launcher.
 *
 * Implementations may perform blocking `PackageManager` work, so callers are
 * responsible for dispatching off the main thread. Keeping that decision in the
 * ViewModel (rather than inside the implementation) lets tests inject a
 * `TestDispatcher` and observe the code that actually runs in production.
 */
interface AppRepository {

    /** All launchable apps, excluding this launcher, sorted by label for the active locale. */
    suspend fun loadApps(): List<AppInfo>
}
