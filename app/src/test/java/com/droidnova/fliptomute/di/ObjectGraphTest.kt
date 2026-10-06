package com.droidnova.fliptomute.di

import com.droidnova.fliptomute.audio.RingerModeController
import com.droidnova.fliptomute.boot.BootMonitoringCoordinator
import com.droidnova.fliptomute.notification.InterruptionAlertController
import com.droidnova.fliptomute.notification.MonitoringNotificationManager
import com.droidnova.fliptomute.notification.PausedNotificationController
import com.droidnova.fliptomute.service.AppRecoveryManager
import com.droidnova.fliptomute.service.MonitoringStateRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * M2-05: the Hilt graph keeps the sharing the old AppContainer had. Above all, the service, tile,
 * receivers and screens must see one monitoring state, or the tile and Home would disagree.
 */
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ObjectGraphTest {

    @get:Rule val hilt = HiltAndroidRule(this)

    @Inject lateinit var stateForService: MonitoringStateRepository
    @Inject lateinit var stateForTile: MonitoringStateRepository
    @Inject lateinit var notificationManager: MonitoringNotificationManager
    @Inject lateinit var pausedNotifications: PausedNotificationController
    @Inject lateinit var alerts: InterruptionAlertController
    @Inject lateinit var recoveryOne: AppRecoveryManager
    @Inject lateinit var recoveryTwo: AppRecoveryManager
    @Inject lateinit var ringerOne: RingerModeController
    @Inject lateinit var ringerTwo: RingerModeController
    @Inject lateinit var bootCoordinator: BootMonitoringCoordinator

    @Before fun setUp() = hilt.inject()

    @Test fun monitoringState_isOnePerProcess() = assertSame(stateForService, stateForTile)

    @Test fun pausedNotificationAndStoppedAlert_shareOneManager() {
        assertSame(notificationManager, pausedNotifications)
        assertSame(notificationManager, alerts)
    }

    @Test fun recoveryManager_isASingleton() = assertSame(recoveryOne, recoveryTwo)

    @Test fun ringerControllers_areFreshPerUser_asTheFactoryMadeThem() = assertNotSame(ringerOne, ringerTwo)

    @Test fun backgroundEntryPoint_seesTheSameObjects() {
        val entryPoint = dagger.hilt.EntryPoints.get(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            BackgroundEntryPoint::class.java,
        )
        assertSame(bootCoordinator, entryPoint.bootMonitoringCoordinator())
    }
}
