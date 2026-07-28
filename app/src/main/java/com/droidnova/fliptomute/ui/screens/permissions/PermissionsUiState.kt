package com.droidnova.fliptomute.ui.screens.permissions

import com.droidnova.fliptomute.data.setup.SetupAccessState

data class PermissionsUiState(
    val accessState: SetupAccessState = SetupAccessState(),
) {
    val canFinishSetup: Boolean get() = accessState.isSetupComplete
    val isSetupComplete: Boolean get() = accessState.isSetupComplete
}
