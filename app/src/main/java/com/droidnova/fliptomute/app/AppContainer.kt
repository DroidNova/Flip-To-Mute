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

interface AppContainer {
    val appPreferencesRepository: AppPreferencesRepository
    val setupAccessRepository: SetupAccessRepository
    val deviceOrientationMonitorFactory: DeviceOrientationMonitorFactory
    val cellularCallMonitorFactory: CellularCallMonitorFactory
    val ringerModeControllerFactory: RingerModeControllerFactory
}

class DefaultAppContainer(context: Context) : AppContainer {
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
        AndroidRingerModeController(context.applicationContext)
    }
}
