package com.example.minimallauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.IOException

/** The application-wide preferences store. Wired into [DataStoreSettingsRepository] by the factory. */
val Context.dataStore by preferencesDataStore(name = "launcher_settings")

/**
 * [SettingsRepository] persisted in a [DataStore].
 *
 * Takes the `DataStore` rather than a `Context` so tests can supply one backed by
 * a temporary file, and so the storage layer has no Android framework dependency
 * beyond the store itself.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        // Ordered list of favorite package names, newline-separated.
        val FAVORITES = stringPreferencesKey("favorites")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val USE_24H = booleanPreferencesKey("use_24h")
        val THEME = stringPreferencesKey("theme")
    }

    private val _readError = MutableStateFlow<Throwable?>(null)

    /**
     * Single source of truth for every preference flow.
     *
     * Without the `catch`, an IOException from a corrupt or unreadable store
     * cancels the `stateIn` upstream in the ViewModel, and the exposed StateFlow
     * keeps its initial value forever: the user silently sees (and writes to)
     * defaults, with no crash and no indication that anything is wrong.
     */
    private val preferences: Flow<Preferences> = dataStore.data
        .onEach { _readError.value = null }
        .catch { error ->
            if (error is IOException) {
                _readError.value = error
                emit(emptyPreferences())
            } else {
                throw error
            }
        }

    override val readError: Flow<Throwable?> = _readError.asStateFlow()

    override val favorites: Flow<List<String>> = preferences.map { prefs ->
        prefs[Keys.FAVORITES]?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    override val hidden: Flow<Set<String>> = preferences.map { it[Keys.HIDDEN] ?: emptySet() }

    override val use24h: Flow<Boolean> = preferences.map { it[Keys.USE_24H] ?: true }

    override val themeKey: Flow<String> =
        preferences.map { it[Keys.THEME] ?: DEFAULT_THEME_KEY }

    override suspend fun toggleFavorite(pkg: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES]
                ?.split("\n")?.filter { it.isNotBlank() }?.toMutableList()
                ?: mutableListOf()
            if (!current.remove(pkg)) current.add(pkg)
            prefs[Keys.FAVORITES] = current.joinToString("\n")
        }
    }

    override suspend fun toggleHidden(pkg: String) {
        dataStore.edit { prefs ->
            val current = (prefs[Keys.HIDDEN] ?: emptySet()).toMutableSet()
            if (!current.add(pkg)) current.remove(pkg)
            prefs[Keys.HIDDEN] = current
        }
    }

    override suspend fun setUse24h(value: Boolean) {
        dataStore.edit { it[Keys.USE_24H] = value }
    }

    override suspend fun setTheme(key: String) {
        dataStore.edit { it[Keys.THEME] = key }
    }
}
