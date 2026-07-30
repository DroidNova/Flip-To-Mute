package com.droidnova.fliptomute.ui.screens.about

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.AppTopBar

/** Placeholder destination for the product About content. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(stringResource(R.string.about_title), onBack) },
    ) { _ -> }
}
