package com.droidnova.fliptomute.ui.snapshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.data.stats.FlipStats
import com.droidnova.fliptomute.ui.screens.about.AboutScreen
import com.droidnova.fliptomute.ui.screens.about.PreviewAboutActions
import com.droidnova.fliptomute.ui.screens.check_setup.CheckSetupScreen
import com.droidnova.fliptomute.ui.screens.check_setup.CheckSetupUiState
import com.droidnova.fliptomute.ui.screens.check_setup.PreviewCheckSetupActions
import com.droidnova.fliptomute.ui.screens.home.HomeCard
import com.droidnova.fliptomute.ui.screens.home.HomeScreen
import com.droidnova.fliptomute.ui.screens.home.HomeStatus
import com.droidnova.fliptomute.ui.screens.home.HomeUiState
import com.droidnova.fliptomute.ui.screens.home.PreviewHomeActions
import com.droidnova.fliptomute.ui.screens.settings.PreviewSettingsActions
import com.droidnova.fliptomute.ui.screens.settings.SettingsScreen
import com.droidnova.fliptomute.ui.screens.settings.SettingsUiState
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.utils.about_utils.OtherAppItem
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The accessibility and layout passes (M7-05, M7-06, design spec sections 7 and 8): 200% font, a
 * 320 dp phone, landscape and a tablet, saved as PNGs, plus checks on touch targets and the switch.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveSnapshots {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val homeOn = HomeUiState(
        status = HomeStatus.ON,
        cards = listOf(HomeCard.BatteryWarning, HomeCard.Stats(FlipStats(thisMonth = 14, total = 40, lastFlipAt = null))),
    )
    private val apps = listOf(
        OtherAppItem(R.string.other_app_bvr_title, R.string.other_app_bvr_description, "bvr", R.drawable.ic_bvr),
    )

    @Test @Config(qualifiers = "w360dp-h1600dp-xhdpi", fontScale = 2.0f)
    fun homeAtDoubleFont() {
        snapshot("a11y_home_font200") { HomeScreen(homeOn, PreviewHomeActions) }
        // The main control is a switch that reports its state (design spec 7)
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertIsOn()
        assertTouchTargets()
    }

    @Test @Config(qualifiers = "w360dp-h3000dp-xhdpi", fontScale = 2.0f)
    fun settingsAtDoubleFont() {
        snapshot("a11y_settings_font200") { SettingsScreen(SettingsUiState(), PreviewSettingsActions) }
        assertTouchTargets()
    }

    @Test @Config(qualifiers = "w360dp-h1400dp-xhdpi", fontScale = 2.0f)
    fun checkSetupAtDoubleFont() {
        snapshot("a11y_check_setup_font200") { CheckSetupScreen(CheckSetupUiState(), PreviewCheckSetupActions) }
        assertTouchTargets()
    }

    @Test @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun homeOnSmallPhone() = snapshot("layout_home_320dp") { HomeScreen(homeOn, PreviewHomeActions) }

    @Test @Config(qualifiers = "w320dp-h900dp-mdpi")
    fun aboutOnSmallPhone() = snapshot("layout_about_320dp") { AboutScreen("2.0.0", apps, PreviewAboutActions) }

    @Test @Config(qualifiers = "w800dp-h360dp-land-mdpi")
    fun homeLandscape() = snapshot("layout_home_landscape") { HomeScreen(homeOn, PreviewHomeActions) }

    @Test @Config(qualifiers = "w900dp-h1200dp-mdpi")
    fun settingsTablet() = snapshot("layout_settings_tablet") { SettingsScreen(SettingsUiState(), PreviewSettingsActions) }

    @Test @Config(qualifiers = "w900dp-h1200dp-mdpi")
    fun homeTablet() = snapshot("layout_home_tablet") { HomeScreen(homeOn, PreviewHomeActions) }

    /** Every clickable element offers at least a 48 dp square to touch (design spec 7). */
    private fun assertTouchTargets() {
        val minPx = 48f * compose.activity.resources.displayMetrics.density - 1f
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            val bounds = node.touchBoundsInRoot
            assertTrue(
                "Touch target ${bounds} under 48 dp: ${node.config}",
                bounds.width >= minPx && bounds.height >= minPx,
            )
        }
    }

    private fun snapshot(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlipToMuteTheme {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Box(Modifier.background(MaterialTheme.colorScheme.background)) { content() }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        val view = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(File("build/snapshots").apply { mkdirs() }, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
