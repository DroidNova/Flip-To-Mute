package com.droidnova.fliptomute.ui.screens.permissions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.TemporaryScreen

@Composable
fun PermissionsScreen(modifier: Modifier = Modifier) {
    TemporaryScreen(
        titleResource = R.string.permissions_title,
        modifier = modifier,
    )
}
