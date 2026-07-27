package com.droidnova.fliptomute.ui.screens.sensor_test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.TemporaryScreen

@Composable
fun SensorTestScreen(modifier: Modifier = Modifier) {
    TemporaryScreen(
        titleResource = R.string.sensor_test_title,
        modifier = modifier,
    )
}
