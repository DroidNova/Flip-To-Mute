package com.droidnova.fliptomute.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Secret Calculator's design kit (VaultDesign.kt there), copied with the same sizes, shapes and motion
 * (architecture A7). Shared with the developer's other apps (Speedometer): softly
 * tinted rounded cards, small uppercase section labels, and compact one-line rows led by a round
 * icon badge. Colours come from the theme, so every colour theme follows automatically.
 */

val NovaCardShape: Shape = RoundedCornerShape(22.dp)
val NovaTileShape: Shape = RoundedCornerShape(14.dp)

/** Card fill: the theme's accent, faintly, over the raised surface. */
@Composable
fun novaCardColor(): Color =
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainer)

/** Lighter fill for tiles and icon badges sitting on a card. */
@Composable
fun novaTileColor(): Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)

/** Shrinks a little while pressed, so taps feel physical. */
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** A tinted, rounded card. Clickable when [onClick] is given (with a soft press scale). */
@Composable
fun NovaCard(
    modifier: Modifier = Modifier,
    color: Color = novaCardColor(),
    contentPadding: PaddingValues = PaddingValues(14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressScale(interaction, 0.98f) else Modifier)
            .clip(NovaCardShape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = ripple(), onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Small uppercase heading above a group. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A round badge holding an icon, the lead of every row. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = novaTileColor(),
    size: Dp = 38.dp,
) {
    Surface(shape = CircleShape, color = container, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.53f))
        }
    }
}

/** A labelled card holding one group of rows. */
@Composable
fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        SectionLabel(title, Modifier.padding(start = 6.dp, top = 12.dp, bottom = 6.dp))
        NovaCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.animateContentSize(),
                content = content,
            )
        }
    }
}

/**
 * One setting on one line: icon badge, title, an optional one-line value, then [trailing] (a
 * switch, a value) or a chevron when it opens something. [onClick] null makes it a plain label.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    summary: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint = if (destructive) colors.error else colors.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (destructive) colors.error else colors.onSurface,
            )
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }
}

/** A row whose whole surface is the switch (also for TalkBack and keyboards). */
@Composable
fun SwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    SettingsRow(
        icon = icon,
        title = title,
        summary = summary,
        onClick = null,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}

/** A selectable pill: optional color dot, label, a check when selected, a lock when [locked]. */
@Composable
fun ChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    locked: Boolean = false,
    leadingIcon: ImageVector? = null,
    /** Hides the selected check (for pills whose selection is shown by color alone). */
    showCheck: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        if (selected) colors.primaryContainer.copy(alpha = 0.45f) else colors.surface.copy(alpha = 0.6f),
        label = "pillColor",
    )
    Row(
        modifier = modifier
            // A 48 dp touch area around the smaller pill (design spec 7)
            .minimumInteractiveComponentSize()
            .pressScale(interaction)
            .clip(RoundedCornerShape(50))
            .background(container)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant), RoundedCornerShape(50))
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = if (selected) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.primary else colors.onSurface, maxLines = 1)
        when {
            selected && showCheck -> Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
            locked -> Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
    }
}

/** A bold gradient card used for the one highlighted action on a screen (Premium). */
@Composable
fun GradientBanner(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.98f)
            .clip(NovaCardShape)
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint = colors.onPrimary, container = colors.onPrimary.copy(alpha = 0.18f), size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onPrimary.copy(alpha = 0.85f))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onPrimary)
    }
}

/**
 * Fades and lifts content in when it first appears, [index] steps later than the first item, so
 * grids and lists arrive one tile after another. Runs once per composition of the item.
 */
fun Modifier.appearIn(index: Int, stepMillis: Int = 35): Modifier = composed {
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            androidx.compose.animation.core.tween(
                durationMillis = 320,
                delayMillis = (index * stepMillis).coerceAtMost(400),
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
        )
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

/** One action in a bottom sheet: icon badge and label, red when [destructive]. */
@Composable
fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit, destructive: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            icon,
            tint = if (destructive) colors.error else colors.primary,
            container = if (destructive) colors.errorContainer.copy(alpha = 0.4f) else novaCardColor(),
            size = 40.dp,
        )
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (destructive) colors.error else colors.onSurface)
    }
}

/** A round action with its label underneath; several sit side by side in a sheet. */
@Composable
fun ActionTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, destructive: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressScale(interaction)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconBadge(
            icon,
            tint = if (destructive) colors.error else colors.primary,
            container = if (destructive) colors.errorContainer.copy(alpha = 0.4f) else novaCardColor(),
            size = 52.dp,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (destructive) colors.error else colors.onSurface, maxLines = 1)
    }
}

/** The top of a bottom sheet: icon badge, title and one short line. */
@Composable
fun SheetHeader(icon: ImageVector, title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, container = novaCardColor(), size = 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Shape for text fields inside sheets and dialogs. */
val NovaFieldShape: Shape = RoundedCornerShape(14.dp)

/** A slow up-and-down drift, for a hero icon that should feel alive. */
fun Modifier.floating(distance: Dp = 6.dp, periodMillis: Int = 2200): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "floating")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatingOffset",
    )
    graphicsLayer { translationY = offset * distance.toPx() }
}
