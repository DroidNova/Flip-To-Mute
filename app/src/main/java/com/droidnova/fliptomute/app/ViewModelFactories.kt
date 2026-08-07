package com.droidnova.fliptomute.app

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.droidnova.fliptomute.ui.screens.home.HomeViewModel
import com.droidnova.fliptomute.ui.screens.permissions.PermissionsViewModel
import com.droidnova.fliptomute.ui.screens.settings.SettingsViewModel
import com.droidnova.fliptomute.ui.screens.sensor_test.SensorTestViewModel
import com.droidnova.fliptomute.ui.screens.call_state_test.CallStateTestViewModel
import com.droidnova.fliptomute.ui.screens.sound_control_test.SoundControlTestViewModel

class ViewModelFactories(container: AppContainer) {
    val home: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                container.appPreferencesRepository,
                container.setupAccessRepository,
                container.monitoringStateRepository,
                container.monitoringServiceController,
                container.appRecoveryManager,
            )
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
            SettingsViewModel(
                container.appPreferencesRepository,
                container.setupAccessRepository,
                container.proximitySensorCapability,
                container.deviceAdminCapabilityRepository,
            )
        }
    }

    val sensorTest: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SensorTestViewModel(container.deviceOrientationMonitorFactory.create())
        }
    }

    val callStateTest: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            CallStateTestViewModel(
                container.cellularCallMonitorFactory.create(),
                container.setupAccessRepository,
            )
        }
    }

    val soundControlTest: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SoundControlTestViewModel(
                container.appPreferencesRepository,
                container.setupAccessRepository,
                container.ringerModeControllerFactory.create(),
            )
        }
    }
}
