package com.droidnova.fliptomute.app

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.droidnova.fliptomute.ui.screens.home.HomeViewModel
import com.droidnova.fliptomute.ui.screens.permissions.PermissionsViewModel
import com.droidnova.fliptomute.ui.screens.settings.SettingsViewModel

class ViewModelFactories(container: AppContainer) {
    val home: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HomeViewModel(container.appPreferencesRepository, container.setupAccessRepository)
        }
    }

    val permissions: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            PermissionsViewModel(
                container.setupAccessRepository,
                container.appPreferencesRepository,
            )
        }
    }

    val settings: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SettingsViewModel(container.appPreferencesRepository, container.setupAccessRepository)
        }
    }
}
