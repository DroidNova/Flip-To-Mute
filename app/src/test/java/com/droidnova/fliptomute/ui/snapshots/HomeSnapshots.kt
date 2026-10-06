package com.droidnova.fliptomute.ui.snapshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.droidnova.fliptomute.data.stats.FlipStats
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.ui.common.HomeHint
import com.droidnova.fliptomute.ui.components.FlipChoice
import com.droidnova.fliptomute.ui.screens.home.HomeCard
import com.droidnova.fliptomute.ui.screens.home.HomeScreen
import com.droidnova.fliptomute.ui.screens.home.HomeStatus
import com.droidnova.fliptomute.ui.screens.home.HomeUiState
import com.droidnova.fliptomute.ui.screens.home.PreviewHomeActions
import com.droidnova.fliptomute.ui.screens.home.attentionFor
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Home in its main states, saved as PNGs under app/build/snapshots for a visual check. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class HomeSnapshots {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val stats = FlipStats(thisMonth = 14, total = 40, lastFlipAt = System.currentTimeMillis() - 3_600_000L)

    @Test fun on() = snapshot(
        "home_on",
        HomeUiState(
            status = HomeStatus.ON,
            monitoringState = MonitoringRuntimeState.Active,
            cards = listOf(HomeCard.Stats(stats), HomeCard.Discovery(HomeHint.FLIP_TO_LOCK)),
        ),
    )

    @Test fun onDark() = snapshot(
        "home_on_dark",
        HomeUiState(status = HomeStatus.ON, flipChoice = FlipChoice.VIBRATE, cards = listOf(HomeCard.BatteryWarning, HomeCard.Stats(stats))),
        dark = true,
    )

    @Test fun off() = snapshot("home_off", HomeUiState(status = HomeStatus.OFF))

    @Test fun pausedUntil() = snapshot(
        "home_paused_until",
        HomeUiState(status = HomeStatus.PAUSED, pausedUntilEpochMs = 1_700_000_000_000L, cards = listOf(HomeCard.Stats(stats))),
    )

    @Test fun attention() = snapshot(
        "home_attention",
        HomeUiState(
            status = HomeStatus.ATTENTION,
            cards = listOf(HomeCard.NeedsAttention(attentionFor(MonitoringRuntimeState.Error(MonitoringFailure.SERVICE_START_NOT_ALLOWED)))),
        ),
    )

    @Test fun setupNeeded() = snapshot("home_setup_needed", HomeUiState(status = HomeStatus.SETUP_NEEDED))

    private fun snapshot(name: String, state: HomeUiState, dark: Boolean = false) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlipToMuteTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                        HomeScreen(state, PreviewHomeActions)
                    }
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
