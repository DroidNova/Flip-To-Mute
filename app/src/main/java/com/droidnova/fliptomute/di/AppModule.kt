package com.droidnova.fliptomute.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.droidnova.fliptomute.audio.AndroidFlipFeedback
import com.droidnova.fliptomute.audio.AndroidIncomingCallVibrationController
import com.droidnova.fliptomute.audio.AndroidRingerModeController
import com.droidnova.fliptomute.audio.AndroidVibrationCapabilityRepository
import com.droidnova.fliptomute.audio.FlipFeedback
import com.droidnova.fliptomute.audio.IncomingCallVibrationControllerFactory
import com.droidnova.fliptomute.audio.RingerModeController
import com.droidnova.fliptomute.audio.RingerModeControllerFactory
import com.droidnova.fliptomute.audio.VibrationCapabilityRepository
import com.droidnova.fliptomute.boot.BootMonitoringCoordinator
import com.droidnova.fliptomute.boot.DefaultBootMonitoringCoordinator
import com.droidnova.fliptomute.data.analytics.AnalyticsLogger
import com.droidnova.fliptomute.data.analytics.Funnel
import com.droidnova.fliptomute.data.analytics.firebaseAnalyticsLogger
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.DataStoreAppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.appDataStore
import com.droidnova.fliptomute.data.recovery.DataStoreRingerRecoveryRepository
import com.droidnova.fliptomute.data.recovery.RingerRecoveryRepository
import com.droidnova.fliptomute.data.reliability.AndroidBatteryOptimizationStatus
import com.droidnova.fliptomute.data.reliability.BatteryOptimizationStatus
import com.droidnova.fliptomute.data.setup.AndroidSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.deviceadmin.AndroidDeviceAdminCapabilityRepository
import com.droidnova.fliptomute.deviceadmin.DeviceAdminCapabilityRepository
import com.droidnova.fliptomute.notification.InterruptionAlertController
import com.droidnova.fliptomute.notification.MonitoringNotificationManager
import com.droidnova.fliptomute.notification.PausedNotificationController
import com.droidnova.fliptomute.quicksettings.AndroidQuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.screenlock.AndroidScreenLockController
import com.droidnova.fliptomute.screenlock.AndroidScreenStateRepository
import com.droidnova.fliptomute.screenlock.ScreenLockController
import com.droidnova.fliptomute.screenlock.ScreenStateRepository
import com.droidnova.fliptomute.sensor.AndroidDeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.AndroidProximityMonitor
import com.droidnova.fliptomute.sensor.AndroidProximitySensorCapability
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitorFactory
import com.droidnova.fliptomute.sensor.ProximityMonitorFactory
import com.droidnova.fliptomute.sensor.ProximitySensorCapability
import com.droidnova.fliptomute.service.AndroidMonitoringServiceController
import com.droidnova.fliptomute.service.AppRecoveryManager
import com.droidnova.fliptomute.service.DefaultAppRecoveryManager
import com.droidnova.fliptomute.service.InMemoryMonitoringStateRepository
import com.droidnova.fliptomute.service.MonitoringHealthCheck
import com.droidnova.fliptomute.service.MonitoringServiceController
import com.droidnova.fliptomute.service.MonitoringStateRepository
import com.droidnova.fliptomute.telephony.AndroidCellularCallMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorFactory
import com.droidnova.fliptomute.utils.MonitoringLog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The app's object graph, as Secret Calculator's AppModule (architecture A1). It builds exactly
 * what the old hand-written AppContainer built: the same implementations, one shared instance where
 * the container had one, and a fresh monitor or ringer controller wherever a factory made one.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Storage ---

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.appDataStore

    @Provides @Singleton
    fun provideAppPreferencesRepository(dataStore: DataStore<Preferences>): AppPreferencesRepository =
        DataStoreAppPreferencesRepository(dataStore)

    @Provides @Singleton
    fun provideRingerRecoveryRepository(dataStore: DataStore<Preferences>): RingerRecoveryRepository =
        DataStoreRingerRecoveryRepository(dataStore)

    // --- Device state ---

    @Provides @Singleton
    fun provideSetupAccessRepository(@ApplicationContext context: Context): SetupAccessRepository =
        AndroidSetupAccessRepository(context)

    @Provides @Singleton
    fun provideDeviceAdminCapabilityRepository(@ApplicationContext context: Context): DeviceAdminCapabilityRepository =
        AndroidDeviceAdminCapabilityRepository(context)

    @Provides @Singleton
    fun provideScreenLockController(@ApplicationContext context: Context): ScreenLockController =
        AndroidScreenLockController(context)

    @Provides @Singleton
    fun provideScreenStateRepository(@ApplicationContext context: Context): ScreenStateRepository =
        AndroidScreenStateRepository(context)

    @Provides @Singleton
    fun provideProximitySensorCapability(@ApplicationContext context: Context): ProximitySensorCapability =
        AndroidProximitySensorCapability(context)

    @Provides @Singleton
    fun provideVibrationCapabilityRepository(@ApplicationContext context: Context): VibrationCapabilityRepository =
        AndroidVibrationCapabilityRepository(context)

    @Provides @Singleton
    fun provideBatteryOptimizationStatus(@ApplicationContext context: Context): BatteryOptimizationStatus =
        AndroidBatteryOptimizationStatus(context)

    @Provides @Singleton
    fun provideFlipFeedback(@ApplicationContext context: Context): FlipFeedback = AndroidFlipFeedback(context)

    // --- Factories: every create() returns a new instance, as before ---

    @Provides @Singleton
    fun provideDeviceOrientationMonitorFactory(@ApplicationContext context: Context) =
        DeviceOrientationMonitorFactory { AndroidDeviceOrientationMonitor(context) }

    @Provides @Singleton
    fun provideProximityMonitorFactory(@ApplicationContext context: Context) =
        ProximityMonitorFactory { AndroidProximityMonitor(context) }

    @Provides @Singleton
    fun provideCellularCallMonitorFactory(@ApplicationContext context: Context) =
        CellularCallMonitorFactory { AndroidCellularCallMonitor(context) }

    @Provides @Singleton
    fun provideRingerModeControllerFactory(
        @ApplicationContext context: Context,
        recoveryRepository: RingerRecoveryRepository,
    ) = RingerModeControllerFactory { AndroidRingerModeController(context, recoveryRepository) }

    @Provides @Singleton
    fun provideIncomingCallVibrationControllerFactory(
        @ApplicationContext context: Context,
        capability: VibrationCapabilityRepository,
    ) = IncomingCallVibrationControllerFactory { AndroidIncomingCallVibrationController(context, capability) }

    /** Unscoped: each view model that asks gets its own monitor, like factory.create() did. */
    @Provides
    fun provideDeviceOrientationMonitor(factory: DeviceOrientationMonitorFactory): DeviceOrientationMonitor =
        factory.create()

    @Provides
    fun provideCellularCallMonitor(factory: CellularCallMonitorFactory): CellularCallMonitor = factory.create()

    @Provides
    fun provideRingerModeController(factory: RingerModeControllerFactory): RingerModeController = factory.create()

    // --- Monitoring ---

    /** One per process: the service, tile, receivers and screens must all see the same state. */
    @Provides @Singleton
    fun provideMonitoringStateRepository(): MonitoringStateRepository = InMemoryMonitoringStateRepository()

    @Provides @Singleton
    fun provideMonitoringServiceController(@ApplicationContext context: Context): MonitoringServiceController =
        AndroidMonitoringServiceController(context)

    @Provides @Singleton
    fun provideQuickSettingsTileUpdateRequester(@ApplicationContext context: Context): QuickSettingsTileUpdateRequester =
        AndroidQuickSettingsTileUpdateRequester(context)

    @Provides @Singleton
    fun provideMonitoringNotificationManager(@ApplicationContext context: Context) =
        MonitoringNotificationManager(context)

    @Provides
    fun providePausedNotificationController(manager: MonitoringNotificationManager): PausedNotificationController = manager

    @Provides
    fun provideInterruptionAlertController(manager: MonitoringNotificationManager): InterruptionAlertController = manager

    @Provides @Singleton
    fun provideAppRecoveryManager(
        preferences: AppPreferencesRepository,
        state: MonitoringStateRepository,
        ringerFactory: RingerModeControllerFactory,
        controller: MonitoringServiceController,
        setup: SetupAccessRepository,
        tiles: QuickSettingsTileUpdateRequester,
        pausedNotifications: PausedNotificationController,
    ): AppRecoveryManager = DefaultAppRecoveryManager(
        preferences, state, ringerFactory.create(), controller, setup, tiles, pausedNotifications,
    )

    @Provides @Singleton
    fun provideBootMonitoringCoordinator(
        @ApplicationContext context: Context,
        preferences: AppPreferencesRepository,
        setup: SetupAccessRepository,
        state: MonitoringStateRepository,
        controller: MonitoringServiceController,
        pausedNotifications: PausedNotificationController,
        tiles: QuickSettingsTileUpdateRequester,
        ringerFactory: RingerModeControllerFactory,
        alerts: InterruptionAlertController,
        analytics: AnalyticsLogger,
    ): BootMonitoringCoordinator = DefaultBootMonitoringCoordinator(
        preferences, setup, state, controller, pausedNotifications, tiles, ringerFactory.create(),
        log = { message -> MonitoringLog.d(context, message) },
        alertController = alerts,
        analytics = analytics,
    )

    @Provides @Singleton
    fun provideMonitoringHealthCheck(
        preferences: AppPreferencesRepository,
        setup: SetupAccessRepository,
        state: MonitoringStateRepository,
        controller: MonitoringServiceController,
        alerts: InterruptionAlertController,
        analytics: AnalyticsLogger,
    ) = MonitoringHealthCheck(preferences, setup, state, controller, alerts, analytics)

    // --- Analytics ---

    @Provides @Singleton
    fun provideAnalyticsLogger(@ApplicationContext context: Context): AnalyticsLogger = firebaseAnalyticsLogger(context)

    @Provides @Singleton
    fun provideFunnel(@ApplicationContext context: Context, logger: AnalyticsLogger) = Funnel(context, logger)
}
