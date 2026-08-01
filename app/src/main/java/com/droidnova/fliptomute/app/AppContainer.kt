package com.droidnova.fliptomute.app

import android.content.Context
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.DataStoreAppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.appDataStore
import com.droidnova.fliptomute.data.recovery.DataStoreRingerRecoveryRepository
import com.droidnova.fliptomute.data.recovery.RingerRecoveryRepository
import com.droidnova.fliptomute.data.setup.AndroidSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.sensor.AndroidDeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitorFactory
import com.droidnova.fliptomute.telephony.AndroidCellularCallMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorFactory
import com.droidnova.fliptomute.audio.AndroidRingerModeController
import com.droidnova.fliptomute.audio.RingerModeControllerFactory
import com.droidnova.fliptomute.audio.AndroidIncomingCallVibrationController
import com.droidnova.fliptomute.audio.AndroidVibrationCapabilityRepository
import com.droidnova.fliptomute.audio.IncomingCallVibrationControllerFactory
import com.droidnova.fliptomute.audio.VibrationCapabilityRepository
import com.droidnova.fliptomute.service.AndroidMonitoringServiceController
import com.droidnova.fliptomute.service.InMemoryMonitoringStateRepository
import com.droidnova.fliptomute.service.MonitoringServiceController
import com.droidnova.fliptomute.service.MonitoringStateRepository
import com.droidnova.fliptomute.service.AppRecoveryManager
import com.droidnova.fliptomute.service.DefaultAppRecoveryManager
import com.droidnova.fliptomute.quicksettings.AndroidQuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.notification.MonitoringNotificationManager
import com.droidnova.fliptomute.notification.PausedNotificationController

interface AppContainer {
    val appPreferencesRepository: AppPreferencesRepository
    val setupAccessRepository: SetupAccessRepository
    val deviceOrientationMonitorFactory: DeviceOrientationMonitorFactory
    val cellularCallMonitorFactory: CellularCallMonitorFactory
    val ringerModeControllerFactory: RingerModeControllerFactory
    val vibrationCapabilityRepository: VibrationCapabilityRepository
    val incomingCallVibrationControllerFactory: IncomingCallVibrationControllerFactory
    val monitoringStateRepository: MonitoringStateRepository
    val monitoringServiceController: MonitoringServiceController
    val ringerRecoveryRepository: RingerRecoveryRepository
    val appRecoveryManager: AppRecoveryManager
    val quickSettingsTileUpdateRequester: QuickSettingsTileUpdateRequester
    val pausedNotificationController: PausedNotificationController
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationContext = context.applicationContext
    private val dataStore = applicationContext.appDataStore
    override val appPreferencesRepository: AppPreferencesRepository = DataStoreAppPreferencesRepository(dataStore)
    override val ringerRecoveryRepository: RingerRecoveryRepository = DataStoreRingerRecoveryRepository(dataStore)
    override val setupAccessRepository: SetupAccessRepository =
        AndroidSetupAccessRepository(context.applicationContext)
    override val deviceOrientationMonitorFactory = DeviceOrientationMonitorFactory {
        AndroidDeviceOrientationMonitor(context.applicationContext)
    }
    override val cellularCallMonitorFactory = CellularCallMonitorFactory {
        AndroidCellularCallMonitor(context.applicationContext)
    }
    override val ringerModeControllerFactory = RingerModeControllerFactory {
        AndroidRingerModeController(applicationContext, ringerRecoveryRepository)
    }
    override val vibrationCapabilityRepository = AndroidVibrationCapabilityRepository(applicationContext)
    override val incomingCallVibrationControllerFactory = IncomingCallVibrationControllerFactory {
        AndroidIncomingCallVibrationController(applicationContext, vibrationCapabilityRepository)
    }
    override val monitoringStateRepository: MonitoringStateRepository = InMemoryMonitoringStateRepository()
    override val monitoringServiceController: MonitoringServiceController =
        AndroidMonitoringServiceController(applicationContext)
    override val quickSettingsTileUpdateRequester: QuickSettingsTileUpdateRequester =
        AndroidQuickSettingsTileUpdateRequester(applicationContext)
    override val pausedNotificationController: PausedNotificationController =
        MonitoringNotificationManager(applicationContext)
    override val appRecoveryManager: AppRecoveryManager by lazy {
        DefaultAppRecoveryManager(
            appPreferencesRepository,
            monitoringStateRepository,
            ringerModeControllerFactory.create(),
            quickSettingsTileUpdateRequester,
            pausedNotificationController,
        )
    }
}
