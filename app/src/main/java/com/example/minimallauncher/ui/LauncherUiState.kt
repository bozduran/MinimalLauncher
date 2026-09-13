package com.example.minimallauncher.ui

import androidx.compose.runtime.Immutable
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY

/**
 * The state of the launcher's app list.
 *
 * The previous model had no loading concept at all: `allApps` started as an empty
 * list, so "nothing configured" and "not loaded yet" were the same value and the
 * "no favorites yet" onboarding copy appeared on every cold start.
 */
@Immutable
sealed interface AppListState {

    /** No attempt has finished yet. */
    data object Loading : AppListState

    /** A usable list. [apps] is a read-only copy; nothing can mutate it in place. */
    data class Ready(val apps: List<AppInfo>) : AppListState

    /**
     * The first load failed and there is nothing to show.
     *
     * A failure while a list is already on screen keeps [Ready] (so the user does
     * not lose their apps and no empty state flashes) and reports through
     * [LauncherUiState.loadFailure] instead.
     */
    data class Error(val cause: Throwable) : AppListState
}

/**
 * One immutable snapshot of everything the launcher UI renders.
 *
 * Screens collect this single flow. Previously each of nine separate `StateFlow`s
 * was collected independently across three screens, which meant no atomic snapshot
 * (a favorites list could be derived from one app list and an older favorites list)
 * and nowhere to express loading or error state.
 *
 * Every collection is a read-only copy produced by the ViewModel, so no caller can
 * mutate state in place — that is the guarantee behind `@Immutable`.
 *
 * One-shot events are deliberately *not* part of this type: `goHome` is a
 * `SharedFlow` because "scroll to page 0" is an event, not state, and replaying it
 * after a configuration change would be wrong.
 */
@Immutable
data class LauncherUiState(
    val appList: AppListState = AppListState.Loading,
    val query: String = "",
    val favorites: List<AppInfo> = emptyList(),
    val drawerApps: List<AppInfo> = emptyList(),
    val favoritePackages: Set<String> = emptySet(),
    val hiddenPackages: Set<String> = emptySet(),
    val use24h: Boolean = true,
    val themeKey: String = DEFAULT_THEME_KEY,
    val settingsError: Throwable? = null,
    val actionFailure: ActionFailure? = null,
    /** A failed refresh while a usable list is still on screen. */
    val loadFailure: Throwable? = null,
) {

    /** The loaded apps, or empty if there is nothing to show yet. */
    val apps: List<AppInfo> get() = (appList as? AppListState.Ready)?.apps ?: emptyList()

    val isLoadingApps: Boolean get() = appList is AppListState.Loading

    /** A first-load failure, or a refresh failure when a list is still shown. */
    val appListError: Throwable?
        get() = (appList as? AppListState.Error)?.cause ?: loadFailure
}
