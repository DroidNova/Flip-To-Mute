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
import com.droidnova.fliptomute.data.stats.FlipRecord
import com.droidnova.fliptomute.data.stats.FlipStats
import com.droidnova.fliptomute.ui.screens.activity.ActivityScreen
import com.droidnova.fliptomute.ui.screens.activity.ActivityUiState
import com.droidnova.fliptomute.ui.screens.activity.activityUiState
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.utils.AppTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The activity screen, saved as PNGs under app/build/snapshots for a visual check. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h1200dp-xhdpi")
class ActivitySnapshots {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = System.currentTimeMillis()
    private val records = listOf(1L, 5L, 26L, 30L, 31L, 75L, 120L).mapIndexed { index, hoursAgo ->
        FlipRecord(now - hoursAgo * 3_600_000L, if (index == 2) FlipAction.VIBRATE else FlipAction.SILENT)
    }

    @Test fun week() = snapshot("activity", activityUiState(records, FlipStats(thisMonth = 14, lastMonth = 9, total = 40), now))

    @Test fun weekDark() = snapshot("activity_dark", activityUiState(records.take(2), FlipStats(total = 2), now), dark = true)

    @Test fun empty() = snapshot("activity_empty", ActivityUiState())

    // The earned themes (future features F36), so their colours can be checked by eye
    @Test fun forest() = snapshot("theme_forest", activityUiState(records, FlipStats(thisMonth = 9, lastMonth = 9, total = 12), now), theme = AppTheme.FOREST)

    @Test fun roseDark() = snapshot("theme_rose_dark", activityUiState(records, FlipStats(thisMonth = 4, lastMonth = 9, total = 30), now), dark = true, theme = AppTheme.ROSE)

    @Test fun midnightDark() = snapshot("theme_midnight_dark", activityUiState(records, FlipStats(total = 60), now), dark = true, theme = AppTheme.MIDNIGHT)

    @Test fun midnightLight() = snapshot("theme_midnight_light", activityUiState(records, FlipStats(total = 60), now), theme = AppTheme.MIDNIGHT)

    private fun snapshot(name: String, state: ActivityUiState, dark: Boolean = false, theme: AppTheme = AppTheme.BLUE) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FlipToMuteTheme(appTheme = theme, darkTheme = dark) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                        ActivityScreen(state, onBack = {})
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
