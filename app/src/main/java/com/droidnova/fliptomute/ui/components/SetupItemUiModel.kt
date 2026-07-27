package com.droidnova.fliptomute.ui.components

import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.data.setup.SetupAccessType

data class SetupItemUiModel(
    val type: SetupAccessType,
    val status: SetupAccessStatus,
)

fun SetupAccessState.toSetupItems(): List<SetupItemUiModel> = SetupAccessType.entries.map { type ->
    SetupItemUiModel(type = type, status = statusFor(type))
}
