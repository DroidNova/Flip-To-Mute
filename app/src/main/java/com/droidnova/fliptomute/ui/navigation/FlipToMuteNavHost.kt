package com.droidnova.fliptomute.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidnova.fliptomute.ui.screens.home.HomeScreen
import com.droidnova.fliptomute.ui.screens.permissions.PermissionsScreen
import com.droidnova.fliptomute.ui.screens.sensor_test.SensorTestScreen
import com.droidnova.fliptomute.ui.screens.settings.SettingsScreen

@Composable
fun FlipToMuteNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        composable(Destination.Home.route) { HomeScreen() }
        composable(Destination.Permissions.route) { PermissionsScreen() }
        composable(Destination.Settings.route) { SettingsScreen() }
        composable(Destination.SensorTest.route) { SensorTestScreen() }
    }
}
