package com.example.minimallauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimallauncher.data.AppChangeSource
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppLogger
import com.example.minimallauncher.data.AppRepository
import com.example.minimallauncher.data.LogcatAppLogger
import com.example.minimallauncher.data.SettingsRepository
import com.example.minimallauncher.data.TextNormalizer
import com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the launcher's app/favorite/search state.
 *
 * Dependencies arrive through the constructor (see [LauncherViewModelFactory]) so
 * every behaviour here can be exercised in a JVM unit test with fakes and a
 * `TestDispatcher` — no Robolectric, no device.
 */
class LauncherViewModel(
    private val appRepo: AppRepository,
    private val settingsRepo: SettingsRepository,
    appChangeSource: AppChangeSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logger: AppLogger = LogcatAppLogger(),
) : ViewModel() {

    private val allAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = allAppsFlow.asStateFlow()

    /**
     * Last app-list load failure, or null when the list is current.
     *
     * Surfaced instead of thrown: an uncaught exception inside `viewModelScope`
     * (which only has a `SupervisorJob`, not an exception handler) would kill the
     * process, and this app *is* the device's home screen.
     */
    private val _appListError = MutableStateFlow<Throwable?>(null)
    val appListError: StateFlow<Throwable?> = _appListError.asStateFlow()

    /**
     * Non-null while stored settings could not be read (defaults are in use) or a
     * settings write failed. Dismissible by the user.
     */
    private val _settingsError = MutableStateFlow<Throwable?>(null)
    val settingsError: StateFlow<Throwable?> = _settingsError.asStateFlow()

    val query = MutableStateFlow("")

    // Emits when the user presses Home so the UI scrolls back to page 0.
    private val _goHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val goHome = _goHome

    private val favoritePkgs = settingsRepo.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val hiddenPkgs = settingsRepo.hidden
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val use24h: StateFlow<Boolean> = settingsRepo.use24h
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val themeKey: StateFlow<String> = settingsRepo.themeKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_THEME_KEY)

    val favoriteSet: StateFlow<Set<String>> = favoritePkgs
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val hiddenSet: StateFlow<Set<String>> = hiddenPkgs

    /** Apps shown on the home screen, in the saved favorite order. */
    val favorites: StateFlow<List<AppInfo>> =
        combine(allAppsFlow, favoritePkgs) { apps, favs ->
            val byPkg = apps.associateBy { it.packageName }
            favs.mapNotNull { byPkg[it] }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Apps shown in the drawer: not hidden, filtered by the (Greek-aware) query. */
    val drawerApps: StateFlow<List<AppInfo>> =
        combine(allAppsFlow, hiddenPkgs, query) { apps, hidden, q ->
            apps.asSequence()
                .filter { it.packageName !in hidden }
                .filter { TextNormalizer.matches(it.label, q) }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refresh()

        // Registration lifetime follows collection (see PackageChangeSource).
        viewModelScope.launch {
            appChangeSource.changes.collect { refresh() }
        }

        // Surface a settings store that cannot be read, instead of presenting
        // defaults as if they were the user's configuration.
        viewModelScope.launch {
            settingsRepo.readError.collect { error ->
                if (error != null) _settingsError.value = error
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val apps = withContext(ioDispatcher) { appRepo.loadApps() }
                allAppsFlow.value = apps
                _appListError.value = null
            } catch (error: Exception) {
                // Keep the last known good list on screen and report the failure.
                logger.record(TAG_APP_LIST, error, "app list load failed")
                _appListError.value = error
            }
        }
    }

    /** Clears the app-list error without retrying. */
    fun dismissAppListError() {
        _appListError.value = null
    }

    /** Retries the app list load after a failure. */
    fun retryLoad() = refresh()

    /** Clears the settings warning. */
    fun dismissSettingsError() {
        _settingsError.value = null
    }

    fun setQuery(q: String) {
        query.value = q
    }

    fun onHomePressed() {
        query.value = ""
        _goHome.tryEmit(Unit)
    }

    fun toggleFavorite(pkg: String) = settingsWrite { settingsRepo.toggleFavorite(pkg) }
    fun toggleHidden(pkg: String) = settingsWrite { settingsRepo.toggleHidden(pkg) }
    fun setUse24h(v: Boolean) = settingsWrite { settingsRepo.setUse24h(v) }
    fun setTheme(key: String) = settingsWrite { settingsRepo.setTheme(key) }

    /**
     * Runs a settings mutation, turning a storage failure into user-visible state
     * instead of an uncaught exception. Because the persisted flows are the single
     * source of truth there is no optimistic copy to roll back: a failed write
     * simply never appears, and the observable state still shows the last value
     * that was actually stored.
     */
    private fun settingsWrite(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (error: Exception) {
                logger.record(TAG_SETTINGS, error, "settings write failed")
                _settingsError.value = error
            }
        }
    }

    private companion object {
        const val TAG_APP_LIST = "app-list-load"
        const val TAG_SETTINGS = "settings-write"
    }
}
