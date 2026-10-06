package com.droidnova.fliptomute.ui.screens.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.droidnova.fliptomute.audio.FlipFeedback
import com.droidnova.fliptomute.data.analytics.Funnel
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus.GRANTED
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.FakeDeviceOrientationMonitor
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * M4-08: the whole first run through the real screen and view model, with fakes for Android. Also
 * saves each step as a PNG under app/build/snapshots for a visual check without a phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class OnboardingFlowComposeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val setup = FakeSetupAccessRepository()
    private val preferences = FakeAppPreferencesRepository()
    private val sensor = FakeDeviceOrientationMonitor()
    private lateinit var viewModel: OnboardingViewModel

    @Before fun setUp() {
        context.getSharedPreferences("funnel", Context.MODE_PRIVATE).edit().clear().commit()
        viewModel = OnboardingViewModel(
            SavedStateHandle(), setup, preferences, sensor, FlipFeedback { }, Funnel(context) { _, _ -> },
        )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlipToMuteTheme {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        OnboardingScreen(state, actions)
                    }
                }
            }
        }
        settle()
    }

    @Test fun firstRun_welcomeToAccessToTryItToHome() {
        compose.onNodeWithText("Silence calls with a flip").assertIsDisplayed()
        snapshot("onboarding_1_welcome")
        compose.onNodeWithText("Get started").performClick()
        settle()

        compose.onNodeWithText("Three quick steps").assertIsDisplayed()
        compose.onNodeWithText("Allow phone access").assertIsDisplayed()
        snapshot("onboarding_2_access")

        // The user grants everything in system screens and comes back
        setup.stateOnRefresh = SetupAccessState(GRANTED, GRANTED, GRANTED)
        viewModel.onVisibilityChanged(true)
        settle()
        compose.onNodeWithText("Try it now").assertIsDisplayed()
        snapshot("onboarding_3_try_it")

        sensor.emit(DeviceOrientation.FACE_DOWN)
        settle()
        compose.onNodeWithText("It works").assertIsDisplayed()
        snapshot("onboarding_4_success")
        compose.onNodeWithText("Turn on Flip to Mute").performClick()
        settle()
        assertEquals(OnboardingExit.OpenHome(enableMonitoring = true), viewModel.exit.value)
    }

    @Test fun accessStep_namesTheNextMissingAccess() {
        compose.onNodeWithText("Get started").performClick()
        settle()
        setup.stateOnRefresh = SetupAccessState(phoneStateStatus = GRANTED)
        viewModel.onVisibilityChanged(true)
        settle()
        compose.onNodeWithText("Allow notifications").assertIsDisplayed()
        compose.onNodeWithText("Do this later").performClick()
        settle()
        assertEquals(OnboardingExit.OpenHome(enableMonitoring = false), viewModel.exit.value)
    }

    private val actions = object : OnboardingActions {
        override fun getStarted() = viewModel.onGetStarted()
        override fun primaryAccess() { viewModel.onPrimaryAccess(false, false, true) }
        override fun confirmSoundHint() = viewModel.dismissSoundHint()
        override fun dismissSoundHint() = viewModel.dismissSoundHint()
        override fun later() = viewModel.later()
        override fun finish() = viewModel.finish()
        override fun back() = viewModel.back()
        override fun messageShown() = viewModel.messageShown()
    }

    private fun settle() {
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
    }

    private fun snapshot(name: String) {
        val view = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/snapshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
