package com.droidnova.fliptomute.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidnova.fliptomute.app.ViewModelFactories
import com.droidnova.fliptomute.ui.screens.home.HomeRoute
import com.droidnova.fliptomute.ui.screens.permissions.PermissionsRoute
import com.droidnova.fliptomute.ui.screens.sensor_test.SensorTestScreen
import com.droidnova.fliptomute.ui.screens.settings.SettingsRoute

@Composable
fun FlipToMuteNavHost(
    viewModelFactories: ViewModelFactories,
    modifier: Modifier = Modifier,
) {
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
                viewModelFactory = viewModelFactories.home,
            )
        }
        composable(Destination.Permissions.route) {
            PermissionsRoute(
                onBack = { navController.navigateUp() },
                viewModelFactory = viewModelFactories.permissions,
            )
        }
        composable(Destination.Settings.route) {
            SettingsRoute(
                onBack = { navController.navigateUp() },
                onOpenSetup = { navController.navigateTo(Destination.Permissions) },
                viewModelFactory = viewModelFactories.settings,
            )
        }
        composable(Destination.SensorTest.route) {
            SensorTestScreen(onBack = { navController.navigateUp() })
        }
    }
}

private fun NavHostController.navigateTo(destination: Destination) {
    navigate(destination.route) { launchSingleTop = true }
}
