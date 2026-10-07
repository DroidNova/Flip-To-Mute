package com.droidnova.fliptomute.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.APP_ICON_MASK_FRACTION
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val HOLD_MS = 450L
private const val LETTER_STEP_MS = 28

/** The longest the intro waits for [SplashIntro]'s `ready` after its entrance: the user never waits on an ad. */
internal const val MAX_READY_WAIT_MS = 2_500L
private val IconSize = 112.dp

/**
 * The system splash draws the icon's canvas at 288 dp inside a 192 dp circle; the intro starts at
 * that size and shrinks to [IconSize], so the handoff on the first frame is seamless.
 */
private const val SYSTEM_SPLASH_CIRCLE_DP = 192f
private val SYSTEM_SPLASH_SCALE = SYSTEM_SPLASH_CIRCLE_DP / IconSize.value

/**
 * The first thing a cold start shows, built like Notification History's intro: it takes over from
 * the system splash (same icon, same place) on the very first frame, the icon settles into its
 * rounded tile and tips over like a phone being turned face down while ripples spread behind it,
 * then the app's name rises in letter by letter.
 *
 * The heavy app UI is only built once that entrance has played ([onEntranceDone]), so building it
 * cannot stutter the animation. If the app is not [ready] by then (the launch ads are still
 * loading), the ripples keep pulsing for at most [MAX_READY_WAIT_MS], so the screen never looks
 * frozen and the user never waits long for an ad. Then the intro fades and lifts away and calls
 * [onFinished].
 */
@Composable
fun SplashIntro(ready: Boolean, onEntranceDone: () -> Unit, onFinished: () -> Unit) {
    val isReady by rememberUpdatedState(ready)
    val name = stringResource(R.string.app_name)
    // Starts at the size and round shape of the system splash icon, so the handoff is seamless
    val iconScale = remember { Animatable(SYSTEM_SPLASH_SCALE) }
    val iconMorph = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) }
    val ripple = remember { Animatable(0f) }
    val letters = remember(name) { name.map { Animatable(0f) } }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { iconMorph.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
            launch {
                iconScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            }
            launch { ripple.animateTo(1f, tween(1300, easing = LinearEasing)) }
            launch {
                delay(260)
                // A phone turned face down: a big tip, then smaller and smaller either way, then still
                flip.animateTo(
                    0f,
                    keyframes {
                        durationMillis = 720
                        0f at 0
                        -38f at 110
                        22f at 240
                        -12f at 360
                        6f at 470
                        -2f at 580
                        0f at 720
                    },
                )
            }
            val lettersDone = async {
                delay(330)
                letters.forEachIndexed { index, letter ->
                    launch {
                        delay((index * LETTER_STEP_MS).toLong())
                        letter.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                    }
                }
                delay((letters.size * LETTER_STEP_MS).toLong())
            }
            lettersDone.await()
        }
        // Build the app underneath now; give it two frames to compose and lay out
        onEntranceDone()
        withFrameNanos { }
        withFrameNanos { }
        delay(HOLD_MS)
        // Still loading the launch ads: keep the ripples going instead of freezing, but not for long
        if (!isReady) {
            coroutineScope {
                val pulse = launch {
                    while (true) {
                        ripple.snapTo(0f)
                        ripple.animateTo(1f, tween(1300, easing = LinearEasing))
                    }
                }
                withTimeoutOrNull(MAX_READY_WAIT_MS) { snapshotFlow { isReady }.first { it } }
                pulse.cancel()
            }
        }
        exit.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        onFinished()
    }

    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                scaleX = 1f + exit.value * 0.06f
                scaleY = 1f + exit.value * 0.06f
            }
            .background(colors.background)
            // A soft glow in the theme's colour behind the icon
            .background(
                Brush.radialGradient(
                    listOf(colors.primary.copy(alpha = 0.16f), colors.background.copy(alpha = 0f)),
                    radius = 900f,
                ),
            )
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        // The icon sits at the exact centre, where the system splash drew it; the name hangs below
        Box(contentAlignment = Alignment.Center) {
            Box(contentAlignment = Alignment.Center) {
                // Three rings spreading out from the icon, one after another
                val rippleColor = colors.primary
                Canvas(Modifier.size(260.dp)) {
                    repeat(3) { i ->
                        val t = ((ripple.value - i * 0.22f) / 0.78f).coerceIn(0f, 1f)
                        if (t > 0f) {
                            val radius = IconSize.toPx() / 2f + t * (size.minDimension / 2f - IconSize.toPx() / 2f)
                            drawCircle(
                                color = rippleColor.copy(alpha = (1f - t) * 0.35f),
                                radius = radius,
                                style = Stroke(width = (1f - t) * 6.dp.toPx() + 1f),
                            )
                        }
                    }
                }
                LauncherIcon(
                    morph = iconMorph.value,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                        // Tips over its vertical axis, as a phone turned face down on a table
                        rotationY = flip.value
                        cameraDistance = 14f * density
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.offset(y = IconSize / 2 + 44.dp),
            ) {
                name.forEachIndexed { index, char ->
                    val progress = letters[index]
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onBackground,
                        modifier = Modifier.graphicsLayer {
                            alpha = progress.value.coerceIn(0f, 1f)
                            translationY = (1f - progress.value) * 18.dp.toPx()
                        },
                    )
                }
            }
        }
    }
}

/**
 * The launcher icon's artwork (its adaptive foreground) cropped to the 72 dp mask, so it looks
 * exactly like the icon on the home screen; the same crop as `AppIcon`.
 */
@Composable
private fun LauncherIcon(morph: Float, modifier: Modifier = Modifier) {
    // morph 0: the system splash's circle, flat; morph 1: the launcher's rounded tile, raised
    val shape = RoundedCornerShape(percent = (50 - 27 * morph).roundToInt())
    Box(
        modifier = modifier
            .size(IconSize)
            .shadow(16.dp * morph, shape, clip = false)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            // The whole 108 dp canvas, scaled so its masked 72 dp middle fills the box edge to edge
            modifier = Modifier.requiredSize(IconSize / APP_ICON_MASK_FRACTION),
        )
    }
}
