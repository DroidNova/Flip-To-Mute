package com.droidnova.fliptomute.app

import android.content.Context
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.DataStoreAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.AndroidSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.sensor.AndroidDeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitorFactory
import com.droidnova.fliptomute.telephony.AndroidCellularCallMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorFactory
import com.droidnova.fliptomute.audio.AndroidRingerModeController
import com.droidnova.fliptomute.audio.RingerModeControllerFactory
import com.droidnova.fliptomute.service.AndroidMonitoringServiceController
import com.droidnova.fliptomute.service.InMemoryMonitoringStateRepository
import com.droidnova.fliptomute.service.MonitoringServiceController
import com.droidnova.fliptomute.service.MonitoringStateRepository

interface AppContainer {
    val appPreferencesRepository: AppPreferencesRepository
    val setupAccessRepository: SetupAccessRepository
    val deviceOrientationMonitorFactory: DeviceOrientationMonitorFactory
    val cellularCallMonitorFactory: CellularCallMonitorFactory
    val ringerModeControllerFactory: RingerModeControllerFactory
    val monitoringStateRepository: MonitoringStateRepository
    val monitoringServiceController: MonitoringServiceController
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationContext = context.applicationContext
    override val appPreferencesRepository: AppPreferencesRepository =
        DataStoreAppPreferencesRepository(context.applicationContext)
    override val setupAccessRepository: SetupAccessRepository =
        AndroidSetupAccessRepository(context.applicationContext)
    override val deviceOrientationMonitorFactory = DeviceOrientationMonitorFactory {
        AndroidDeviceOrientationMonitor(context.applicationContext)
    }
    override val cellularCallMonitorFactory = CellularCallMonitorFactory {
        AndroidCellularCallMonitor(context.applicationContext)
    }
    override val ringerModeControllerFactory = RingerModeControllerFactory {
        AndroidRingerModeController(applicationContext)
    }
    override val monitoringStateRepository: MonitoringStateRepository = InMemoryMonitoringStateRepository()
    override val monitoringServiceController: MonitoringServiceController =
        AndroidMonitoringServiceController(applicationContext)
}
