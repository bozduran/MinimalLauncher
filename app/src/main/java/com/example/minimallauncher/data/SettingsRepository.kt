package com.example.minimallauncher.data

import kotlinx.coroutines.flow.Flow

/**
 * Persisted launcher preferences: favorites (ordered), hidden packages, the
 * 12/24-hour clock preference and the selected theme key.
 *
 * Implementations are the single owner of the storage format — the ViewModel and
 * UI never see `Preferences` keys. This is what allows the DataStore-backed
 * implementation to be exercised in a plain JVM test over a temporary file.
 */
interface SettingsRepository {

    /** Ordered list of favorite package names. */
    val favorites: Flow<List<String>>

    /** Package names hidden from the drawer. */
    val hidden: Flow<Set<String>>

    /** True for a 24-hour clock. */
    val use24h: Flow<Boolean>

    /** Selected theme key; falls back to [com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY]. */
    val themeKey: Flow<String>

    /**
     * Non-null while the stored settings cannot be read and defaults are being
     * used. Sticky: it stays set until the store becomes readable again, so the UI
     * can warn the user rather than silently presenting defaults as their config.
     */
    val readError: Flow<Throwable?>

    /** Adds the package to favorites if absent, removes it if present. */
    suspend fun toggleFavorite(pkg: String)

    /** Hides the package if visible, unhides it if hidden. */
    suspend fun toggleHidden(pkg: String)

    suspend fun setUse24h(value: Boolean)

    suspend fun setTheme(key: String)

    /**
     * Drops favorites and hidden entries whose package is not in [installed].
     *
     * Uninstalling an app used to leave its favorite/hidden entries in the store
     * forever, so reinstalling the package silently resurrected the old choices.
     *
     * Callers must not pass an empty set: that would discard the user's entire
     * configuration on a transient empty enumeration.
     */
    suspend fun pruneMissing(installed: Set<String>)
}
