package com.droidnova.fliptomute.ui.screens.keep_running

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.data.analytics.AnalyticsEvents
import com.droidnova.fliptomute.data.analytics.AnalyticsLogger
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.reliability.BatteryOptimizationStatus
import com.droidnova.fliptomute.data.reliability.PhoneBrand
import com.droidnova.fliptomute.ui.components.GradientBanner
import com.droidnova.fliptomute.ui.components.IconBadge
import com.droidnova.fliptomute.ui.components.NovaCard
import com.droidnova.fliptomute.ui.components.NovaTopBar
import com.droidnova.fliptomute.ui.components.SectionLabel
import com.droidnova.fliptomute.ui.components.SettingsGroup
import com.droidnova.fliptomute.ui.components.SwitchRow
import com.droidnova.fliptomute.ui.theme.stateColors
import com.droidnova.fliptomute.ui.util.RefreshOnResume
import com.droidnova.fliptomute.utils.openAppDetailsSettings
import com.droidnova.fliptomute.utils.openBatteryOptimizationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KeepRunningUiState(
    /** True when Android may stop Flip to Mute; null when the phone cannot tell. */
    val batteryRestricted: Boolean? = null,
    val brand: PhoneBrand = PhoneBrand.OTHER,
    val startAfterRestart: Boolean = true,
)

/** "Keep it running" (M6-02, audit R4, design spec 4.7). */
@HiltViewModel
class KeepRunningViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val batteryOptimizationStatus: BatteryOptimizationStatus,
    private val analytics: AnalyticsLogger,
) : ViewModel() {
    private val restricted = MutableStateFlow(batteryOptimizationStatus.isIgnoringBatteryOptimizations()?.not())
    private val brand = PhoneBrand.from(Build.MANUFACTURER)

    val uiState: StateFlow<KeepRunningUiState> = combine(preferencesRepository.preferences, restricted) { preferences, r ->
        KeepRunningUiState(batteryRestricted = r, brand = brand, startAfterRestart = preferences.startAfterPhoneRestart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), KeepRunningUiState(brand = brand))

    init {
        log("shown")
    }

    /** Back from system settings: the user may have changed the battery setting. */
    fun refresh() {
        val wasRestricted = restricted.value
        restricted.value = batteryOptimizationStatus.isIgnoringBatteryOptimizations()?.not()
        if (wasRestricted == true && restricted.value == false) log("exempted")
    }

    fun onOpenBatterySettings() = log("opened_settings")

    fun setStartAfterRestart(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setStartAfterPhoneRestart(enabled) }
    }

    private fun log(action: String) {
        analytics.log(AnalyticsEvents.BATTERY_GUIDANCE, mapOf(AnalyticsEvents.PARAM_ACTION to action))
    }
}

@Composable
fun KeepRunningRoute(onBack: () -> Unit, viewModel: KeepRunningViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    KeepRunningScreen(
        state = state,
        onBack = onBack,
        onOpenBatterySettings = {
            viewModel.onOpenBatterySettings()
            context.openBatteryOptimizationSettings()
        },
        onOpenAppInfo = { context.openAppDetailsSettings() },
        onStartAfterRestartChanged = viewModel::setStartAfterRestart,
    )
}

@Composable
fun KeepRunningScreen(
    state: KeepRunningUiState,
    onBack: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onStartAfterRestartChanged: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = { NovaTopBar(stringResource(R.string.settings_group_keep_running), onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val restricted = state.batteryRestricted == true
                val color = if (restricted) stateColors.paused else stateColors.on
                NovaCard(color = color.container) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            if (restricted) Icons.Filled.BatteryAlert else Icons.Filled.BatteryChargingFull,
                            tint = color.accent,
                            container = color.accent.copy(alpha = 0.14f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(
                                R.string.keep_running_status,
                                stringResource(
                                    when (state.batteryRestricted) {
                                        true -> R.string.battery_restricted
                                        false -> R.string.battery_not_restricted
                                        null -> R.string.battery_unknown
                                    },
                                ),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = color.onContainer,
                        )
                    }
                }
                Text(
                    stringResource(R.string.keep_running_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GradientBanner(
                    icon = Icons.Filled.BatteryChargingFull,
                    title = stringResource(R.string.keep_running_open_battery),
                    subtitle = stringResource(R.string.keep_running_open_battery_sub),
                    onClick = onOpenBatterySettings,
                )
                SectionLabel(stringResource(R.string.keep_running_steps_title), Modifier.padding(start = 6.dp, top = 8.dp))
                NovaCard {
                    stringArrayResource(state.brand.stepsRes).forEachIndexed { index, step ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(24.dp),
                            )
                            Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenAppInfo,
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.keep_running_app_info))
                    }
                }
                SettingsGroup(stringResource(R.string.settings_group_keep_running)) {
                    SwitchRow(
                        Icons.Filled.RestartAlt,
                        stringResource(R.string.start_after_phone_restart),
                        state.startAfterRestart,
                        onStartAfterRestartChanged,
                        summary = stringResource(R.string.start_after_phone_restart_description),
                    )
                }
            }
        }
    }
}

