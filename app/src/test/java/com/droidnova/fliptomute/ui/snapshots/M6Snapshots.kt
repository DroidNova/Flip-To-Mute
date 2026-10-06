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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.data.reliability.PhoneBrand
import com.droidnova.fliptomute.ui.screens.about.AboutScreen
import com.droidnova.fliptomute.ui.screens.about.PreviewAboutActions
import com.droidnova.fliptomute.ui.screens.check_setup.CheckSetupScreen
import com.droidnova.fliptomute.ui.screens.check_setup.CheckSetupUiState
import com.droidnova.fliptomute.ui.screens.check_setup.CheckStep
import com.droidnova.fliptomute.ui.screens.check_setup.PreviewCheckSetupActions
import com.droidnova.fliptomute.ui.screens.check_setup.StepResult
import com.droidnova.fliptomute.ui.screens.keep_running.KeepRunningScreen
import com.droidnova.fliptomute.ui.screens.keep_running.KeepRunningUiState
import com.droidnova.fliptomute.ui.screens.settings.PreviewSettingsActions
import com.droidnova.fliptomute.ui.screens.settings.SettingsScreen
import com.droidnova.fliptomute.ui.screens.settings.SettingsUiState
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.utils.about_utils.OtherAppItem
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The M6 screens, saved as PNGs under app/build/snapshots for a visual check (M6-12). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h1400dp-xhdpi")
class M6Snapshots {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun settings() = snapshot("settings") { SettingsScreen(SettingsUiState(batteryRestricted = true), PreviewSettingsActions) }

    @Test fun settingsDark() = snapshot("settings_dark", dark = true) { SettingsScreen(SettingsUiState(), PreviewSettingsActions) }

    @Test fun keepRunning() = snapshot("keep_running") {
        KeepRunningScreen(KeepRunningUiState(batteryRestricted = true, brand = PhoneBrand.XIAOMI), {}, {}, {}, {})
    }

    @Test fun checkSetup() = snapshot("check_setup") {
        val results = mapOf(
            CheckStep.ACCESS to StepResult.PASS,
            CheckStep.BATTERY to StepResult.WARNING,
            CheckStep.FLIP_SENSOR to StepResult.RUNNING,
            CheckStep.SOUND to StepResult.NOT_CHECKED,
            CheckStep.CALLS to StepResult.NOT_CHECKED,
        )
        CheckSetupScreen(CheckSetupUiState(results), PreviewCheckSetupActions)
    }

    @Test fun about() = snapshot("about") {
        val apps = listOf(
            OtherAppItem(R.string.other_app_bvr_title, R.string.other_app_bvr_description, "bvr", R.drawable.ic_bvr),
            OtherAppItem(R.string.other_app_secret_calculator_title, R.string.other_app_secret_calculator_description, "calc", R.drawable.ic_calculator),
        )
        AboutScreen("2.0.0", apps, PreviewAboutActions)
    }

    private fun snapshot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlipToMuteTheme(darkTheme = dark) {
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
