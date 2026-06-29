package com.example.minimallauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.minimallauncher.ui.theme.DEFAULT_THEME_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "launcher_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        // Ordered list of favorite package names, newline-separated.
        val FAVORITES = stringPreferencesKey("favorites")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val USE_24H = booleanPreferencesKey("use_24h")
        val THEME = stringPreferencesKey("theme")
    }

    val favorites: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FAVORITES]?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val hidden: Flow<Set<String>> = context.dataStore.data.map { it[Keys.HIDDEN] ?: emptySet() }

    val use24h: Flow<Boolean> = context.dataStore.data.map { it[Keys.USE_24H] ?: true }

    val themeKey: Flow<String> =
        context.dataStore.data.map { it[Keys.THEME] ?: DEFAULT_THEME_KEY }

    suspend fun toggleFavorite(pkg: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES]
                ?.split("\n")?.filter { it.isNotBlank() }?.toMutableList()
                ?: mutableListOf()
            if (!current.remove(pkg)) current.add(pkg)
            prefs[Keys.FAVORITES] = current.joinToString("\n")
        }
    }

    suspend fun toggleHidden(pkg: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.HIDDEN] ?: emptySet()).toMutableSet()
            if (!current.add(pkg)) current.remove(pkg)
            prefs[Keys.HIDDEN] = current
        }
    }

    suspend fun setUse24h(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_24H] = value }
    }

    suspend fun setTheme(key: String) {
        context.dataStore.edit { it[Keys.THEME] = key }
    }
}