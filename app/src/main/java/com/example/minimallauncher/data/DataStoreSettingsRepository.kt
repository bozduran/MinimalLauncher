package com.example.minimallauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    override val favorites: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[Keys.FAVORITES]?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    override val hidden: Flow<Set<String>> = dataStore.data.map { it[Keys.HIDDEN] ?: emptySet() }

    override val use24h: Flow<Boolean> = dataStore.data.map { it[Keys.USE_24H] ?: true }

    override val themeKey: Flow<String> =
        dataStore.data.map { it[Keys.THEME] ?: DEFAULT_THEME_KEY }

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
