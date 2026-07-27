package com.droidnova.fliptomute.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.components.SectionHeader
import com.droidnova.fliptomute.ui.components.SettingsItem
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(stringResource(R.string.settings_title), onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader(stringResource(R.string.behaviour_section)) }
            item { SettingsItem(stringResource(R.string.selected_flip_action), stringResource(R.string.silent_title)) }
            item {
                SettingsItem(
                    stringResource(R.string.detection_feedback),
                    stringResource(R.string.detection_feedback_description),
                )
            }
            item { SectionHeader(stringResource(R.string.monitoring_section)) }
            item {
                SettingsItem(
                    stringResource(R.string.notifications_title),
                    stringResource(R.string.monitoring_notification_description),
                )
            }
            item { SectionHeader(stringResource(R.string.about_section)) }
            item { SettingsItem(stringResource(R.string.privacy_policy), stringResource(R.string.privacy_policy_unavailable)) }
            item { SettingsItem(stringResource(R.string.app_version), stringResource(R.string.app_version_value)) }
            item { SettingsItem(stringResource(R.string.about_app), stringResource(R.string.about_app_description)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    FlipToMuteTheme(dynamicColor = false) { SettingsScreen {} }
}
