package com.example.minimallauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimallauncher.data.AppChangeSource
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppRepository
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
) : ViewModel() {

    private val allAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = allAppsFlow.asStateFlow()

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
    }

    fun refresh() {
        viewModelScope.launch {
            val apps = withContext(ioDispatcher) { appRepo.loadApps() }
            allAppsFlow.value = apps
        }
    }

    fun setQuery(q: String) {
        query.value = q
    }

    fun onHomePressed() {
        query.value = ""
        _goHome.tryEmit(Unit)
    }

    fun toggleFavorite(pkg: String) = viewModelScope.launch { settingsRepo.toggleFavorite(pkg) }
    fun toggleHidden(pkg: String) = viewModelScope.launch { settingsRepo.toggleHidden(pkg) }
    fun setUse24h(v: Boolean) = viewModelScope.launch { settingsRepo.setUse24h(v) }
    fun setTheme(key: String) = viewModelScope.launch { settingsRepo.setTheme(key) }
}
