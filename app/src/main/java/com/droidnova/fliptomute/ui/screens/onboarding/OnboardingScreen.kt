package com.droidnova.fliptomute.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.ui.components.IconBadge
import com.droidnova.fliptomute.ui.components.NovaCard
import com.droidnova.fliptomute.ui.components.PhoneFlipDemo
import com.droidnova.fliptomute.ui.components.PhoneFlipIllustration
import com.droidnova.fliptomute.ui.components.SheetHeader
import com.droidnova.fliptomute.ui.components.appearIn
import com.droidnova.fliptomute.ui.components.novaCardColor
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.ui.theme.stateColors

/** Everything the first-run screen can ask for, as Secret Calculator's OnboardingActions. */
interface OnboardingActions {
    fun getStarted()
    fun primaryAccess()
    fun confirmSoundHint()
    fun dismissSoundHint()
    fun later()
    fun finish()
    fun back()
    fun messageShown()
}

@Composable
fun OnboardingScreen(state: OnboardingUiState, actions: OnboardingActions) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            actions.messageShown()
        }
    }
    // Back goes to the previous step; on the first one it leaves
    BackHandler(onBack = actions::back)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomButtons(state, actions) },
        containerColor = MaterialTheme.colorScheme.background,
        // The activity pads for the system bars once
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!state.accessOnly) StepIndicator(state.step)
            AnimatedContent(targetState = state.step, label = "step") { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.ACCESS -> AccessStep(state)
                    OnboardingStep.TRY_IT -> TryItStep(state.tryState)
                }
            }
        }
    }
    if (state.showSoundHint) SoundHintSheet(actions)
}

@Composable
private fun StepIndicator(step: OnboardingStep) {
    val total = OnboardingStep.entries.size
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OnboardingStep.entries.forEach { s ->
                Box(
                    Modifier
                        .height(4.dp)
                        .width(if (s == step) 28.dp else 12.dp)
                        .background(
                            if (s.ordinal <= step.ordinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
        Text(
            stringResource(R.string.onboarding_step_of, step.ordinal + 1, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BottomButtons(state: OnboardingUiState, actions: OnboardingActions) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state.step) {
            OnboardingStep.WELCOME -> PrimaryButton(stringResource(R.string.onboarding_get_started), actions::getStarted)
            OnboardingStep.ACCESS -> {
                val next = state.nextAccess
                if (next != null && !state.unsupported) {
                    PrimaryButton(stringResource(primaryLabel(next, state.nextNeedsSettings)), actions::primaryAccess)
                }
                TextButton(onClick = actions::later, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(if (state.accessOnly) R.string.not_now_action else R.string.access_later))
                }
            }
            OnboardingStep.TRY_IT -> if (state.tryState.flipped) {
                PrimaryButton(stringResource(R.string.try_turn_on), actions::finish)
            } else {
                TextButton(onClick = actions::finish, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun primaryLabel(step: AccessStep, needsSettings: Boolean): Int = when {
    needsSettings -> R.string.open_settings_action
    step == AccessStep.PHONE -> R.string.access_allow_phone
    step == AccessStep.NOTIFICATIONS -> R.string.access_allow_notifications
    else -> R.string.access_allow_sound
}

@Composable
private fun WelcomeStep() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            Modifier
                .padding(top = 16.dp)
                .size(180.dp)
                .background(novaCardColor(), RoundedCornerShape(48.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PhoneFlipDemo(width = 72.dp, contentDescription = stringResource(R.string.onboarding_phone_description))
        }
        Text(
            stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NovaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Lock)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.onboarding_privacy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AccessStep(state: OnboardingUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.onboarding_access_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(if (state.unsupported) R.string.access_unsupported else R.string.onboarding_access_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NovaCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AccessStepRow(
                    Icons.Filled.Phone, R.string.access_phone_title, R.string.access_phone_reason,
                    state.access.phoneStateStatus, current = state.nextAccess == AccessStep.PHONE, index = 0,
                )
                AccessStepRow(
                    Icons.Filled.NotificationsActive, R.string.access_notifications_title, R.string.access_notifications_reason,
                    state.access.notificationStatus, current = state.nextAccess == AccessStep.NOTIFICATIONS, index = 1,
                )
                AccessStepRow(
                    Icons.AutoMirrored.Filled.VolumeOff, R.string.access_sound_title, R.string.access_sound_reason,
                    state.access.soundControlStatus, current = state.nextAccess == AccessStep.SOUND, index = 2,
                )
            }
        }
    }
}

@Composable
private fun AccessStepRow(
    icon: ImageVector,
    title: Int,
    reason: Int,
    status: SetupAccessStatus,
    current: Boolean,
    index: Int,
) {
    val colors = MaterialTheme.colorScheme
    val done = status == SetupAccessStatus.GRANTED
    Row(
        Modifier
            .fillMaxWidth()
            .appearIn(index)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint = if (current) colors.primary else colors.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(stringResource(reason), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        val statusText = stringResource(if (done) R.string.allowed else R.string.not_allowed)
        Icon(
            if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = statusText,
            tint = if (done) stateColors.on.accent else if (current) colors.primary else colors.outline,
        )
    }
}

@Composable
private fun TryItStep(tryState: TryState) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val success = tryState.flipped
        Box(
            Modifier
                .padding(top = 16.dp)
                .size(180.dp)
                .background(if (success) stateColors.on.container else novaCardColor(), RoundedCornerShape(48.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (success) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = stateColors.on.accent, modifier = Modifier.size(72.dp))
            } else {
                PhoneFlipIllustration(
                    faceDown = tryState.faceDown,
                    width = 72.dp,
                    contentDescription = stringResource(R.string.onboarding_phone_description),
                )
            }
        }
        Text(
            stringResource(if (success) R.string.try_success_title else R.string.try_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(if (success) R.string.try_success_body else R.string.try_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (tryState.showHint && !success) {
            NovaCard {
                Text(
                    stringResource(R.string.try_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundHintSheet(actions: OnboardingActions) {
    ModalBottomSheet(
        onDismissRequest = actions::dismissSoundHint,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SheetHeader(Icons.AutoMirrored.Filled.VolumeOff, stringResource(R.string.sound_hint_title))
            Text(
                stringResource(R.string.sound_hint_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PrimaryButton(stringResource(R.string.continue_action), actions::confirmSoundHint)
            TextButton(onClick = actions::dismissSoundHint, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.not_now_action))
            }
        }
    }
}

internal object PreviewOnboardingActions : OnboardingActions {
    override fun getStarted() = Unit
    override fun primaryAccess() = Unit
    override fun confirmSoundHint() = Unit
    override fun dismissSoundHint() = Unit
    override fun later() = Unit
    override fun finish() = Unit
    override fun back() = Unit
    override fun messageShown() = Unit
}

@Preview(showBackground = true)
@Composable
private fun OnboardingWelcomePreview() {
    FlipToMuteTheme { OnboardingScreen(OnboardingUiState(), PreviewOnboardingActions) }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingAccessPreview() {
    FlipToMuteTheme {
        OnboardingScreen(
            OnboardingUiState(
                step = OnboardingStep.ACCESS,
                access = SetupAccessState(phoneStateStatus = SetupAccessStatus.GRANTED),
                nextAccess = AccessStep.NOTIFICATIONS,
            ),
            PreviewOnboardingActions,
        )
    }
}
