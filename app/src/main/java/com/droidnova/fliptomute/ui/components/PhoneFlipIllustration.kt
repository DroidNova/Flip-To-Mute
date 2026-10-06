package com.droidnova.fliptomute.ui.components

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import kotlinx.coroutines.delay

/**
 * A phone that turns face down (design spec 3.4 and 9). Face up shows a ringing screen; face down
 * shows the back with a muted bell. The turn is a 3D flip around the horizontal axis, 400 ms, or an
 * instant change when the system has animations turned off.
 */
@Composable
fun PhoneFlipIllustration(
    faceDown: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    val reduceMotion = rememberReduceMotion()
    val angle by animateFloatAsState(
        targetValue = if (faceDown) 180f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(FLIP_MILLIS),
        label = "phoneFlip",
    )
    Box(
        modifier = modifier
            .size(width = width, height = width * PHONE_ASPECT)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .graphicsLayer {
                rotationX = angle
                cameraDistance = 12f * density
            },
    ) {
        if (angle <= 90f) {
            PhoneFront(accent)
        } else {
            // Turned 180 degrees with the phone, so the back reads the right way up
            Box(Modifier.graphicsLayer { rotationX = 180f }) { PhoneBack(accent) }
        }
    }
}

/**
 * The phone flipping face down and back up in a loop, for the Welcome step. Shows it face down and
 * still when animations are off.
 */
@Composable
fun PhoneFlipDemo(modifier: Modifier = Modifier, width: Dp = 96.dp, contentDescription: String? = null) {
    val reduceMotion = rememberReduceMotion()
    var faceDown by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(DEMO_HOLD_MILLIS)
            faceDown = !faceDown
        }
    }
    PhoneFlipIllustration(faceDown, modifier, width, contentDescription = contentDescription)
}

@Composable
private fun PhoneFront(accent: Color) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .clip(PhoneShape)
            .background(colors.surfaceContainerHigh)
            .border(3.dp, colors.onSurface.copy(alpha = 0.7f), PhoneShape)
            .padding(6.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.16f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(width = 14.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(colors.onSurface.copy(alpha = 0.35f)),
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun PhoneBack(accent: Color) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .clip(PhoneShape)
            .background(colors.onSurface.copy(alpha = 0.82f))
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .size(12.dp)
                .clip(CircleShape)
                .background(colors.surface.copy(alpha = 0.5f)),
        )
        Icon(
            Icons.Filled.NotificationsOff,
            contentDescription = null,
            tint = accent.copy(alpha = 0.9f).compositeOverSurface(colors.surface),
            modifier = Modifier.align(Alignment.Center).size(24.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .height(2.dp)
                .fillMaxWidth(0.4f)
                .clip(CircleShape)
                .background(colors.surface.copy(alpha = 0.3f)),
        )
    }
}

private fun Color.compositeOverSurface(surface: Color): Color =
    androidx.compose.ui.graphics.lerp(this, surface, 0.15f)

/** True when the user turned animations off (developer options or accessibility). */
@Composable
fun rememberReduceMotion(): Boolean {
    if (LocalInspectionMode.current) return false
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

private val PhoneShape = RoundedCornerShape(14.dp)
private const val PHONE_ASPECT = 1.9f
private const val FLIP_MILLIS = 400
private const val DEMO_HOLD_MILLIS = 1_200L

@Preview(showBackground = true)
@Composable
private fun PhoneFaceUpPreview() {
    FlipToMuteTheme { PhoneFlipIllustration(faceDown = false, modifier = Modifier.padding(16.dp)) }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PhoneFaceDownDarkPreview() {
    FlipToMuteTheme(darkTheme = true) { PhoneFlipIllustration(faceDown = true, modifier = Modifier.padding(16.dp)) }
}
