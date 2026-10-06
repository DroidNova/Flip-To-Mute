package com.droidnova.fliptomute.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.ui.theme.StateColor
import com.droidnova.fliptomute.ui.theme.stateColors

/** What the main control on Home shows (design spec 4.4). */
enum class PowerControlState { ON, OFF, PAUSED, BUSY, ATTENTION }

/**
 * The large round on and off control on Home (design spec 3.5 and 4.4). A Switch for TalkBack: it
 * announces "Flip to Mute, switch, on" and its state changes. The colour follows the state, and the
 * phone inside turns face down while Flip to Mute is on.
 */
@Composable
fun FlipPowerControl(
    state: PowerControlState,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    enabled: Boolean = state != PowerControlState.BUSY,
) {
    val colors = stateColors
    val stateColor = when (state) {
        PowerControlState.ON -> colors.on
        PowerControlState.PAUSED -> colors.paused
        PowerControlState.ATTENTION -> colors.attention
        PowerControlState.OFF, PowerControlState.BUSY -> colors.off
    }
    val container by animateColorAsState(stateColor.container, label = "powerContainer")
    val accent by animateColorAsState(stateColor.accent, label = "powerAccent")
    val checked = state == PowerControlState.ON
    val label = stringResource(R.string.app_name)
    val stateText = stringResource(
        when (state) {
            PowerControlState.ON -> R.string.power_state_on
            PowerControlState.PAUSED -> R.string.power_state_paused
            PowerControlState.BUSY -> R.string.power_state_busy
            PowerControlState.ATTENTION -> R.string.power_state_attention
            PowerControlState.OFF -> R.string.power_state_off
        },
    )
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (state == PowerControlState.ON && !rememberReduceMotion()) PulseRing(stateColor, size)
        Box(
            Modifier
                .size(size * 0.86f)
                .pressScale(interaction)
                .clip(CircleShape)
                .background(container)
                .border(3.dp, accent, CircleShape)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interaction,
                    indication = ripple(),
                    onValueChange = onToggle,
                )
                .semantics {
                    contentDescription = label
                    stateDescription = stateText
                },
            contentAlignment = Alignment.Center,
        ) {
            if (state == PowerControlState.BUSY) {
                CircularProgressIndicator(color = accent, modifier = Modifier.fillMaxSize().padding(10.dp))
            }
            PhoneFlipIllustration(faceDown = checked, width = size * 0.3f, accent = accent)
        }
    }
}

@Composable
private fun PulseRing(color: StateColor, size: Dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS, delayMillis = PULSE_PAUSE_MILLIS), RepeatMode.Restart),
        label = "pulseProgress",
    )
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                val scale = 0.86f + 0.14f * progress
                scaleX = scale
                scaleY = scale
                alpha = 1f - progress
            }
            .border(2.dp, color.accent, CircleShape),
    )
}

/** What a flip does to a ringing call (design spec 4.4, audit U4). */
enum class FlipChoice { SILENCE, VIBRATE }

/** One choice instead of two checkboxes: Silence or Vibrate, as two pills. */
@Composable
fun ActionSelector(selected: FlipChoice, onSelect: (FlipChoice) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoicePill(
            label = stringResource(R.string.action_silence),
            selected = selected == FlipChoice.SILENCE,
            onClick = { onSelect(FlipChoice.SILENCE) },
            leadingIcon = Icons.Filled.NotificationsOff,
            modifier = Modifier.weight(1f),
        )
        ChoicePill(
            label = stringResource(R.string.action_vibrate),
            selected = selected == FlipChoice.VIBRATE,
            onClick = { onSelect(FlipChoice.VIBRATE) },
            leadingIcon = Icons.Filled.Vibration,
            modifier = Modifier.weight(1f),
        )
    }
}

/** "Needs attention": what is wrong, in one line, and the one button that fixes it (audit U10). */
@Composable
fun AttentionCard(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val attention = stateColors.attention
    NovaCard(modifier = modifier, color = attention.container) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.WarningAmber, tint = attention.accent, container = attention.accent.copy(alpha = 0.14f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = attention.onContainer)
                if (body != null) {
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = attention.onContainer.copy(alpha = 0.8f))
                }
            }
        }
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(onClick = onAction, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                Text(actionLabel)
            }
        }
    }
}

/** Visible proof that Flip to Mute works (audit U11). [lastFlip] is already formatted, e.g. "today, 2:10 PM". */
@Composable
fun StatsCard(silencedThisMonth: Int, lastFlip: String?, modifier: Modifier = Modifier) {
    NovaCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Insights)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    pluralStringResource(R.plurals.stats_silenced_this_month, silencedThisMonth, silencedThisMonth),
                    style = MaterialTheme.typography.titleMedium,
                    // Cards are plain columns, not surfaces: always give text an explicit colour
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (lastFlip != null) {
                    Text(
                        stringResource(R.string.stats_last_flip, lastFlip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val PULSE_MILLIS = 1_200
private const val PULSE_PAUSE_MILLIS = 1_800

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun FlipComponentsPreview() {
    FlipToMuteTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlipPowerControl(PowerControlState.ON, {}, size = 120.dp)
                FlipPowerControl(PowerControlState.OFF, {}, size = 120.dp)
            }
            ActionSelector(FlipChoice.SILENCE, {})
            AttentionCard("Android stopped Flip to Mute", actionLabel = "Turn back on", onAction = {})
            StatsCard(silencedThisMonth = 14, lastFlip = "today, 2:10 PM")
        }
    }
}

@Preview(showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun FlipComponentsDarkLargeTextPreview() {
    FlipToMuteTheme(darkTheme = true) {
        Column(
            Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FlipPowerControl(PowerControlState.PAUSED, {}, size = 120.dp)
            ActionSelector(FlipChoice.VIBRATE, {})
            AttentionCard("Access was removed", body = "Flip to Mute needs sound control.", actionLabel = "Fix access", onAction = {})
            StatsCard(silencedThisMonth = 1, lastFlip = null)
        }
    }
}
