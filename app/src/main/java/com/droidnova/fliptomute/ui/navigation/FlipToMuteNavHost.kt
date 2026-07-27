package com.droidnova.fliptomute.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.droidnova.fliptomute.ui.screens.home.HomeRoute
import com.droidnova.fliptomute.ui.screens.permissions.PermissionsScreen
import com.droidnova.fliptomute.ui.screens.sensor_test.SensorTestScreen
import com.droidnova.fliptomute.ui.screens.settings.SettingsScreen

@Composable
fun FlipToMuteNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Destination.Home.route) {
            HomeRoute(
                onSettingsClick = { navController.navigateTo(Destination.Settings) },
                onPermissionsClick = { navController.navigateTo(Destination.Permissions) },
                onSensorTestClick = { navController.navigateTo(Destination.SensorTest) },
            )
        }
        composable(Destination.Permissions.route) { PermissionsScreen { navController.navigateUp() } }
        composable(Destination.Settings.route) { SettingsScreen { navController.navigateUp() } }
        composable(Destination.SensorTest.route) { SensorTestScreen { navController.navigateUp() } }
    }
}

private fun NavHostController.navigateTo(destination: Destination) {
    navigate(destination.route) { launchSingleTop = true }
}
