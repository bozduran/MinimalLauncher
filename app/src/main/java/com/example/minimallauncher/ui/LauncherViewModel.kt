package com.example.minimallauncher.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppRepository
import com.example.minimallauncher.data.SettingsRepository
import com.example.minimallauncher.data.TextNormalizer
import com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY
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

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val appRepo = AppRepository(app)
    private val settingsRepo = SettingsRepository(app)

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

    // Refresh the app list live whenever a package is added/removed/changed.
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    init {
        refresh()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            getApplication(),
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { getApplication<Application>().unregisterReceiver(packageReceiver) }
    }

    fun refresh() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { appRepo.loadApps() }
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