package com.droidnova.fliptomute.ui.screens.onboarding

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.audio.FlipFeedback
import com.droidnova.fliptomute.data.analytics.Funnel
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus.GRANTED
import com.droidnova.fliptomute.data.setup.SetupAccessStatus.NOT_GRANTED
import com.droidnova.fliptomute.data.setup.SetupAccessStatus.NOT_SUPPORTED
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.FakeDeviceOrientationMonitor
import com.droidnova.fliptomute.testing.TrackedViewModels
import com.droidnova.fliptomute.utils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModels = TrackedViewModels()
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private var buzzes = 0

    @Before fun setUp() {
        context.getSharedPreferences("funnel", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun tearDown() = viewModels.clear()

    // --- Steps ---

    @Test fun startsAtWelcome_andGetStartedOpensAccess() {
        val f = fixture()
        assertEquals(OnboardingStep.WELCOME, f.vm.uiState.value.step)
        f.vm.onGetStarted()
        assertEquals(OnboardingStep.ACCESS, f.vm.uiState.value.step)
    }

    @Test fun backWalksStepsAndLeavesFromWelcome() {
        val f = fixture()
        f.vm.onGetStarted()
        f.vm.back()
        assertEquals(OnboardingStep.WELCOME, f.vm.uiState.value.step)
        f.vm.back()
        assertEquals(OnboardingExit.Close, f.vm.exit.value)
    }

    @Test fun later_marksDoneAndOpensHomeWithoutTurningOn() = runTest {
        val f = fixture()
        f.vm.onGetStarted()
        f.vm.later(); runCurrent()
        assertTrue(f.preferences.preferences.value.onboardingCompleted)
        assertEquals(OnboardingExit.OpenHome(enableMonitoring = false), f.vm.exit.value)
    }

    // --- Access ---

    @Test fun accessIsAskedInOrder_phoneNotificationsSound() {
        assertEquals(AccessStep.PHONE, OnboardingViewModel.nextAccess(SetupAccessState()))
        assertEquals(AccessStep.NOTIFICATIONS, OnboardingViewModel.nextAccess(access(phone = GRANTED)))
        assertEquals(AccessStep.SOUND, OnboardingViewModel.nextAccess(access(phone = GRANTED, notifications = GRANTED)))
        assertNull(OnboardingViewModel.nextAccess(access(GRANTED, GRANTED, GRANTED)))
    }

    @Test fun phone_isRequestedFirst_thenSentToSettingsAfterAPermanentNo() {
        val f = fixture()
        f.vm.onGetStarted()
        assertEquals(
            AccessCommand.RequestPermission(RuntimeSetupPermission.PHONE),
            f.vm.onPrimaryAccess(phoneRationale = false, notificationRationale = false, notificationRuntimeNeeded = true),
        )
        f.vm.onPermissionResult(RuntimeSetupPermission.PHONE, granted = false, permanentlyDenied = true)
        assertTrue(f.vm.uiState.value.nextNeedsSettings)
        assertEquals(R.string.phone_access_denied, f.vm.uiState.value.message)
        assertEquals(
            AccessCommand.OpenAppDetails,
            f.vm.onPrimaryAccess(phoneRationale = false, notificationRationale = false, notificationRuntimeNeeded = true),
        )
    }

    @Test fun notifications_useThePermissionOnAndroid13_andSettingsBefore() {
        val f = fixture(access(phone = GRANTED))
        assertEquals(
            AccessCommand.RequestPermission(RuntimeSetupPermission.NOTIFICATIONS),
            f.vm.onPrimaryAccess(false, false, notificationRuntimeNeeded = true),
        )
        assertEquals(AccessCommand.OpenNotificationSettings, f.vm.onPrimaryAccess(false, false, notificationRuntimeNeeded = false))
    }

    @Test fun soundControl_showsTheHintSheetFirst() {
        val f = fixture(access(phone = GRANTED, notifications = GRANTED))
        assertEquals(AccessCommand.None, f.vm.onPrimaryAccess(false, false, false))
        assertTrue(f.vm.uiState.value.showSoundHint)
        f.vm.dismissSoundHint()
        assertFalse(f.vm.uiState.value.showSoundHint)
    }

    @Test fun deviceWithoutCellularCalling_isMarkedUnsupported() {
        val f = fixture(access(phone = NOT_SUPPORTED))
        assertTrue(f.vm.uiState.value.unsupported)
    }

    @Test fun grantingTheLastAccess_movesToTryItAndStartsTheFunnel() = runTest {
        val f = fixture()
        f.vm.onGetStarted()
        f.setup.stateOnRefresh = access(GRANTED, GRANTED, GRANTED)
        f.vm.onVisibilityChanged(true); runCurrent()
        assertEquals(OnboardingStep.TRY_IT, f.vm.uiState.value.step)
        assertEquals(listOf(Funnel.SETUP_COMPLETE), events.map { it.first })
    }

    @Test fun withoutAMotionSensor_tryItIsSkippedAndHomeTurnsItOn() = runTest {
        val f = fixture(sensor = FakeDeviceOrientationMonitor(isSensorAvailable = false))
        f.vm.onGetStarted()
        f.setup.stateOnRefresh = access(GRANTED, GRANTED, GRANTED)
        f.vm.onVisibilityChanged(true); runCurrent()
        assertEquals(OnboardingExit.OpenHome(enableMonitoring = true), f.vm.exit.value)
        assertTrue(f.preferences.preferences.value.onboardingCompleted)
    }

    @Test fun accessOnlyMode_startsAtAccessAndClosesWhenDone() = runTest {
        val f = fixture(accessOnly = true, preferences = AppPreferences(onboardingCompleted = true))
        assertEquals(OnboardingStep.ACCESS, f.vm.uiState.value.step)
        f.setup.stateOnRefresh = access(GRANTED, GRANTED, GRANTED)
        f.vm.onVisibilityChanged(true); runCurrent()
        assertEquals(OnboardingExit.CloseAccess, f.vm.exit.value)
        // An earlier user is not counted as new
        assertTrue(events.isEmpty())
    }

    @Test fun accessOnlyMode_backAndNotNowClose() {
        val f = fixture(accessOnly = true)
        f.vm.back()
        assertEquals(OnboardingExit.CloseAccess, f.vm.exit.value)
    }

    // --- Try it ---

    @Test fun faceDown_buzzesOnceShowsSuccessAndStopsTheSensor() = runTest {
        val f = atTryIt()
        f.vm.onVisibilityChanged(true); runCurrent()
        assertEquals(1, f.sensor.activeRegistrations)
        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertTrue(f.vm.uiState.value.tryState.flipped)
        assertEquals(1, buzzes)
        assertEquals(0, f.sensor.activeRegistrations)
    }

    @Test fun sensorRunsOnlyWhileVisible() = runTest {
        val f = atTryIt()
        f.vm.onVisibilityChanged(false); runCurrent()
        assertEquals(0, f.sensor.activeRegistrations)
        f.vm.onVisibilityChanged(true); runCurrent()
        assertEquals(1, f.sensor.activeRegistrations)
    }

    @Test fun hintAppearsAfter20SecondsWithoutAFlip() = runTest {
        val f = atTryIt()
        f.vm.onVisibilityChanged(true); runCurrent()
        advanceTimeBy(OnboardingViewModel.HINT_DELAY_MILLIS - 1); runCurrent()
        assertFalse(f.vm.uiState.value.tryState.showHint)
        advanceTimeBy(2); runCurrent()
        assertTrue(f.vm.uiState.value.tryState.showHint)
    }

    @Test fun finishingAfterAFlip_turnsItOnAndRecordsTheTry() = runTest {
        val f = atTryIt()
        f.vm.onVisibilityChanged(true); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        f.vm.finish(); runCurrent()
        assertEquals(OnboardingExit.OpenHome(enableMonitoring = true), f.vm.exit.value)
        assertTrue(f.preferences.preferences.value.onboardingCompleted)
        val completion = events.single { it.first == Funnel.ONBOARDING_COMPLETE }
        assertEquals(Funnel.VALUE_DONE, completion.second[Funnel.PARAM_TRY_FLIP])
    }

    // --- Helpers ---

    private fun kotlinx.coroutines.test.TestScope.atTryIt(): Fixture {
        val f = fixture()
        f.vm.onGetStarted()
        f.setup.stateOnRefresh = access(GRANTED, GRANTED, GRANTED)
        f.vm.onVisibilityChanged(true); runCurrent()
        f.vm.onVisibilityChanged(false); runCurrent()
        return f
    }

    private fun access(
        phone: com.droidnova.fliptomute.data.setup.SetupAccessStatus = NOT_GRANTED,
        notifications: com.droidnova.fliptomute.data.setup.SetupAccessStatus = NOT_GRANTED,
        sound: com.droidnova.fliptomute.data.setup.SetupAccessStatus = NOT_GRANTED,
    ) = SetupAccessState(phoneStateStatus = phone, soundControlStatus = sound, notificationStatus = notifications)

    private fun fixture(
        access: SetupAccessState = SetupAccessState(),
        accessOnly: Boolean = false,
        preferences: AppPreferences = AppPreferences(),
        sensor: FakeDeviceOrientationMonitor = FakeDeviceOrientationMonitor(),
    ): Fixture {
        val setup = FakeSetupAccessRepository(access)
        val repository = FakeAppPreferencesRepository(preferences)
        val savedState = SavedStateHandle(mapOf(OnboardingViewModel.EXTRA_ACCESS_ONLY to accessOnly))
        val vm = viewModels.track(
            OnboardingViewModel(
                savedState, setup, repository, sensor,
                FlipFeedback { buzzes++ },
                Funnel(context) { event, params -> events += event to params },
            ),
        )
        return Fixture(vm, setup, repository, sensor)
    }

    private class Fixture(
        val vm: OnboardingViewModel,
        val setup: FakeSetupAccessRepository,
        val preferences: FakeAppPreferencesRepository,
        val sensor: FakeDeviceOrientationMonitor,
    )
}
