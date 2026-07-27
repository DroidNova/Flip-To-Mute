package com.droidnova.fliptomute.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.stringResource
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.data.setup.SetupAccessType
import com.droidnova.fliptomute.ui.screens.home.SetupItemUiModel

@Composable
fun SetupItem(item: SetupItemUiModel, onClick: () -> Unit, actionLabel: String? = null) {
    val resources = setupResources(item.type)
    SetupChecklistItem(
        title = stringResource(resources.title),
        description = if (
            item.type == SetupAccessType.PHONE_STATE && item.status == SetupAccessStatus.NOT_SUPPORTED
        ) {
            stringResource(R.string.phone_not_supported_description)
        } else {
            stringResource(resources.description)
        },
        status = stringResource(
            when (item.status) {
                SetupAccessStatus.GRANTED -> R.string.allowed
                SetupAccessStatus.NOT_GRANTED -> R.string.not_allowed
                SetupAccessStatus.NOT_SUPPORTED -> R.string.not_supported
            },
        ),
        actionLabel = actionLabel ?: stringResource(resources.action),
        onClick = onClick,
        showAction = item.status == SetupAccessStatus.NOT_GRANTED,
        statusIcon = if (item.status == SetupAccessStatus.GRANTED) {
            Icons.Default.CheckCircle
        } else {
            Icons.Default.Warning
        },
        statusGranted = item.status == SetupAccessStatus.GRANTED,
    )
}

private data class SetupResources(val title: Int, val description: Int, val action: Int)

private fun setupResources(type: SetupAccessType) = when (type) {
    SetupAccessType.PHONE_STATE -> SetupResources(
        R.string.phone_access_title,
        R.string.phone_access_description,
        R.string.allow_action,
    )
    SetupAccessType.SOUND_CONTROL -> SetupResources(
        R.string.sound_access_title,
        R.string.sound_access_description,
        R.string.open_settings_action,
    )
    SetupAccessType.NOTIFICATIONS -> SetupResources(
        R.string.notifications_title,
        R.string.notifications_description,
        R.string.allow_action,
    )
}
