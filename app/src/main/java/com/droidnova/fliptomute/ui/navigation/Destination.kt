package com.droidnova.fliptomute.ui.navigation

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Permissions : Destination("permissions")
    data object Settings : Destination("settings")
    data object SensorTest : Destination("sensor_test")
    data object CallStateTest : Destination("call_state_test")
    data object SoundControlTest : Destination("sound_control_test")
}
