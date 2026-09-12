package com.example.minimallauncher.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.minimallauncher.data.AndroidLauncherGateway
import com.example.minimallauncher.data.DataStoreSettingsRepository
import com.example.minimallauncher.data.LogcatAppLogger
import com.example.minimallauncher.data.PackageChangeSource
import com.example.minimallauncher.data.PackageManagerAppRepository
import com.example.minimallauncher.data.dataStore
import com.example.minimallauncher.ui.theme.LaunchThemeStore

/**
 * Supplies production dependencies to [LauncherViewModel].
 *
 * Not a DI framework — at this size an explicit factory is enough, and it keeps
 * the ViewModel constructor free of `Context` so tests can build one directly.
 *
 * The [createSavedStateHandle] overload is the one `by viewModels()` uses, which is
 * what gives the ViewModel a handle backed by the Activity's saved state so the
 * search query survives process death.
 */
class LauncherViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        newInstance(modelClass, extras.createSavedStateHandle())

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        newInstance(modelClass, SavedStateHandle())

    @Suppress("UNCHECKED_CAST")
    private fun <T : ViewModel> newInstance(
        modelClass: Class<T>,
        savedState: SavedStateHandle,
    ): T {
        require(modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        // One logger instance so app-enumeration skips and ViewModel failures share
        // a tag prefix.
        val logger = LogcatAppLogger()
        val launchThemeStore = LaunchThemeStore(application)
        return LauncherViewModel(
            appRepo = PackageManagerAppRepository(application, logger),
            settingsRepo = DataStoreSettingsRepository(
                dataStore = application.dataStore,
                launchThemeMirror = launchThemeStore::saveThemeKey,
            ),
            appChangeSource = PackageChangeSource(application),
            gateway = AndroidLauncherGateway(application),
            savedState = savedState,
            logger = logger,
        ) as T
    }
}
