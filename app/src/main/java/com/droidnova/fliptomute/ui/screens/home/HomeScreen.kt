package com.droidnova.fliptomute.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.data.setup.SetupAccessType
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.components.FlipActionOption
import com.droidnova.fliptomute.ui.components.InformationCard
import com.droidnova.fliptomute.ui.components.SectionHeader
import com.droidnova.fliptomute.ui.components.SetupItem
import com.droidnova.fliptomute.ui.components.StatusCard
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.ui.util.RefreshOnResume

@Composable
fun HomeRoute(
    onSettingsClick: () -> Unit,
    onPermissionsClick: () -> Unit,
    onSensorTestClick: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    RefreshOnResume(viewModel::refreshAccessState)
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    HomeScreen(
        state = state,
        onFlipActionSelected = viewModel::onFlipActionSelected,
        onSetupItemClick = { onPermissionsClick() },
        onSettingsClick = onSettingsClick,
        onSensorTestClick = onSensorTestClick,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onFlipActionSelected: (FlipAction) -> Unit,
    onSetupItemClick: (SetupAccessType) -> Unit,
    onSettingsClick: () -> Unit,
    onSensorTestClick: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppTopBar(title = stringResource(R.string.app_name)) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings_content_description))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StatusCard(state.monitoringStatus, state.isMonitoringEnabled) }
            item { SectionHeader(stringResource(R.string.flip_action_section)) }
            item {
                FlipActionOption(
                    title = stringResource(R.string.silent_title),
                    description = stringResource(R.string.silent_description),
                    icon = Icons.Default.VolumeOff,
                    selected = state.selectedFlipAction == FlipAction.SILENT,
                    onClick = { onFlipActionSelected(FlipAction.SILENT) },
                )
            }
            item {
                FlipActionOption(
                    title = stringResource(R.string.vibrate_title),
                    description = stringResource(R.string.vibrate_description),
                    icon = Icons.Default.Vibration,
                    selected = state.selectedFlipAction == FlipAction.VIBRATE,
                    onClick = { onFlipActionSelected(FlipAction.VIBRATE) },
                )
            }
            item { SectionHeader(stringResource(R.string.required_setup)) }
            items(state.setupItems.size, key = { state.setupItems[it].type }) { index ->
                val item = state.setupItems[index]
                SetupItem(item, onClick = { onSetupItemClick(item.type) })
            }
            item { SensorTestCard(onSensorTestClick) }
            item { HowItWorks() }
            item {
                InformationCard(
                    text = stringResource(R.string.privacy_description),
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.privacy_title),
                )
            }
            item {
                InformationCard(
                    text = stringResource(R.string.monitoring_notification_description),
                    icon = Icons.Default.Info,
                )
            }
        }
    }
}

@Composable
private fun SensorTestCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.ScreenRotation, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.test_flip_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.test_flip_description), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun HowItWorks() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.how_it_works))
        listOf(R.string.how_step_one, R.string.how_step_two, R.string.how_step_three).forEachIndexed { i, text ->
            Text(
                text = stringResource(R.string.numbered_step, i + 1, stringResource(text)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    FlipToMuteTheme(dynamicColor = false) {
        HomeScreen(HomeUiState(), {}, {}, {}, {})
    }
}
