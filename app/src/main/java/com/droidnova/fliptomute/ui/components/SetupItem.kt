package com.droidnova.fliptomute.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.screens.home.SetupItemType
import com.droidnova.fliptomute.ui.screens.home.SetupItemUiModel

@Composable
fun SetupItem(item: SetupItemUiModel, onClick: () -> Unit) {
    val resources = setupResources(item.type)
    SetupChecklistItem(
        title = stringResource(resources.title),
        description = stringResource(resources.description),
        status = stringResource(if (item.isAllowed) R.string.allowed else R.string.not_allowed),
        actionLabel = stringResource(resources.action),
        onClick = onClick,
    )
}

private data class SetupResources(val title: Int, val description: Int, val action: Int)

private fun setupResources(type: SetupItemType) = when (type) {
    SetupItemType.PHONE -> SetupResources(
        R.string.phone_access_title,
        R.string.phone_access_description,
        R.string.allow_action,
    )
    SetupItemType.SOUND_CONTROL -> SetupResources(
        R.string.sound_access_title,
        R.string.sound_access_description,
        R.string.open_settings_action,
    )
    SetupItemType.NOTIFICATIONS -> SetupResources(
        R.string.notifications_title,
        R.string.notifications_description,
        R.string.allow_action,
    )
}
