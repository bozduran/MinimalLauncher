package com.example.minimallauncher.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.minimallauncher.data.AndroidLauncherGateway
import com.example.minimallauncher.data.DataStoreSettingsRepository
import com.example.minimallauncher.data.PackageChangeSource
import com.example.minimallauncher.data.PackageManagerAppRepository
import com.example.minimallauncher.data.dataStore

/**
 * Supplies production dependencies to [LauncherViewModel].
 *
 * Not a DI framework — at this size an explicit factory is enough, and it keeps
 * the ViewModel constructor free of `Context` so tests can build one directly.
 */
class LauncherViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return LauncherViewModel(
            appRepo = PackageManagerAppRepository(application),
            settingsRepo = DataStoreSettingsRepository(application.dataStore),
            appChangeSource = PackageChangeSource(application),
            gateway = AndroidLauncherGateway(application),
        ) as T
    }
}
