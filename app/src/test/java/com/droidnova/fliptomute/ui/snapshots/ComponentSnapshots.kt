package com.droidnova.fliptomute.ui.snapshots

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.ui.components.ActionSelector
import com.droidnova.fliptomute.ui.components.AttentionCard
import com.droidnova.fliptomute.ui.components.FlipChoice
import com.droidnova.fliptomute.ui.components.FlipPowerControl
import com.droidnova.fliptomute.ui.components.PowerControlState
import com.droidnova.fliptomute.ui.components.SettingsGroup
import com.droidnova.fliptomute.ui.components.StatsCard
import com.droidnova.fliptomute.ui.components.SwitchRow
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.utils.AppTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the design-system components to PNG files under app/build/snapshots, so the look can be
 * checked without a phone. It asserts only that each one draws. Outfit is a downloadable font and
 * does not load here, so headings use the platform font in these images.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class ComponentSnapshots {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun lightBlue() = snapshot("components_light_blue") { Gallery() }

    @Test fun darkBlue() = snapshot("components_dark_blue", dark = true) { Gallery() }

    @Test fun lightTeal() = snapshot("components_light_teal", theme = AppTheme.TEAL) { Gallery() }

    @Test fun darkSunset() = snapshot("components_dark_sunset", dark = true, theme = AppTheme.SUNSET) { Gallery() }

    @Composable
    private fun Gallery() {
        Column(Modifier.width(400.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlipPowerControl(PowerControlState.ON, {}, size = 120.dp)
                FlipPowerControl(PowerControlState.OFF, {}, size = 120.dp)
                FlipPowerControl(PowerControlState.PAUSED, {}, size = 120.dp)
            }
            ActionSelector(FlipChoice.SILENCE, {})
            AttentionCard("Android stopped Flip to Mute", body = "Tap to turn it back on.", actionLabel = "Turn back on", onAction = {})
            StatsCard(silencedThisMonth = 14, lastFlip = "today, 2:10 PM")
            SettingsGroup("Flip behaviour") {
                SwitchRow(Icons.Default.Vibration, "Buzz when a flip is detected", true, {})
                SwitchRow(Icons.Default.Layers, "Only when lying flat", false, {}, summary = "Ignore flips from your hand")
            }
        }
    }

    private fun snapshot(
        name: String,
        dark: Boolean = false,
        theme: AppTheme = AppTheme.BLUE,
        content: @Composable () -> Unit,
    ) {
        // Paused clock: the endless pulse on the "on" control would otherwise keep the UI from settling
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlipToMuteTheme(appTheme = theme, darkTheme = dark) {
                // Same as MainActivity: a readable default text colour at the root
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                ) {
                    Column(Modifier.background(MaterialTheme.colorScheme.background)) { content() }
                }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val view = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/snapshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
