package com.droidnova.fliptomute.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.TemporaryScreen

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    TemporaryScreen(
        titleResource = R.string.settings_title,
        modifier = modifier,
    )
}
