package com.droidnova.fliptomute.ui.screens.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.data.setup.SetupAccessType
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.components.SetupItem
import com.droidnova.fliptomute.ui.components.toSetupItems
import com.droidnova.fliptomute.ui.util.RefreshOnResume
import com.droidnova.fliptomute.util.findActivity
import com.droidnova.fliptomute.util.openAppDetailsSettings
import com.droidnova.fliptomute.util.openAppNotificationSettings
import com.droidnova.fliptomute.util.openNotificationPolicySettings
import com.droidnova.fliptomute.util.SettingsLaunchResult
import kotlinx.coroutines.launch

private enum class ExplanationDialog { PHONE, NOTIFICATIONS, SOUND }

@Composable
fun PermissionsRoute(
    onBack: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: PermissionsViewModel = viewModel(factory = viewModelFactory)
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<ExplanationDialog?>(null) }

    fun reportSettingsResult(result: SettingsLaunchResult) {
        if (result == SettingsLaunchResult.UNAVAILABLE) scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.settings_unavailable))
        }
    }

    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refreshAccessState()
        if (!granted) scope.launch {
            val activity = context.findActivity()
            val permanentlyDenied = viewModel.permissionAction(
                RuntimeSetupPermission.PHONE,
                granted = false,
                shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_PHONE_STATE,
                ),
            ) == RuntimePermissionAction.OPEN_SETTINGS
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.phone_access_denied),
                actionLabel = if (permanentlyDenied) context.getString(R.string.open_app_settings) else null,
            )
            if (permanentlyDenied && result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                reportSettingsResult(context.openAppDetailsSettings())
            }
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refreshAccessState()
        if (!granted) scope.launch {
            val activity = context.findActivity()
            val permanentlyDenied = viewModel.permissionAction(
                RuntimeSetupPermission.NOTIFICATIONS,
                granted = false,
                shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
            ) == RuntimePermissionAction.OPEN_SETTINGS
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.notification_access_denied),
                actionLabel = if (permanentlyDenied) context.getString(R.string.open_settings_action) else null,
            )
            if (permanentlyDenied && result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                reportSettingsResult(context.openAppNotificationSettings())
            }
        }
    }

    RefreshOnResume(viewModel::refreshAccessState)
    PermissionsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAccessClick = { type ->
            when (type) {
                SetupAccessType.PHONE_STATE -> {
                    val activity = context.findActivity()
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE,
                    ) == PackageManager.PERMISSION_GRANTED
                    val rationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.READ_PHONE_STATE,
                        )
                    when (viewModel.permissionAction(RuntimeSetupPermission.PHONE, granted, rationale)) {
                        RuntimePermissionAction.REFRESH -> viewModel.refreshAccessState()
                        RuntimePermissionAction.SHOW_EXPLANATION -> dialog = ExplanationDialog.PHONE
                        RuntimePermissionAction.OPEN_SETTINGS ->
                            reportSettingsResult(context.openAppDetailsSettings())
                    }
                }
                SetupAccessType.NOTIFICATIONS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        val activity = context.findActivity()
                        val rationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        )
                        when (viewModel.permissionAction(
                            RuntimeSetupPermission.NOTIFICATIONS,
                            granted = false,
                            shouldShowRationale = rationale,
                        )) {
                            RuntimePermissionAction.SHOW_EXPLANATION -> dialog = ExplanationDialog.NOTIFICATIONS
                            RuntimePermissionAction.OPEN_SETTINGS ->
                                reportSettingsResult(context.openAppNotificationSettings())
                            RuntimePermissionAction.REFRESH -> viewModel.refreshAccessState()
                        }
                    } else {
                        reportSettingsResult(context.openAppNotificationSettings())
                    }
                }
                SetupAccessType.SOUND_CONTROL -> dialog = ExplanationDialog.SOUND
            }
        },
        onFinish = {
            viewModel.onSetupFinished(onBack)
        },
    )

    dialog?.let { shownDialog ->
        AccessExplanationDialog(
            type = shownDialog,
            onDismiss = { dialog = null },
            onContinue = {
                dialog = null
                when (shownDialog) {
                    ExplanationDialog.PHONE -> {
                        viewModel.markPermissionRequested(RuntimeSetupPermission.PHONE)
                        phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    }
                    ExplanationDialog.NOTIFICATIONS -> {
                        viewModel.markPermissionRequested(RuntimeSetupPermission.NOTIFICATIONS)
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    ExplanationDialog.SOUND -> reportSettingsResult(context.openNotificationPolicySettings())
                }
            },
        )
    }
}

@Composable
private fun PermissionsScreen(
    state: PermissionsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAccessClick: (SetupAccessType) -> Unit,
    onFinish: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AppTopBar(stringResource(R.string.app_setup_title), onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Text(stringResource(R.string.app_setup_description)) }
            items(state.accessState.toSetupItems(), key = { it.type }) { item ->
                val notificationSettingsRequired = item.type == SetupAccessType.NOTIFICATIONS &&
                    item.status == SetupAccessStatus.NOT_GRANTED &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            LocalContext.current,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED)
                SetupItem(
                    item = item,
                    onClick = { onAccessClick(item.type) },
                    actionLabel = if (notificationSettingsRequired) {
                        stringResource(R.string.open_settings_action)
                    } else null,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canFinishSetup,
                    ) { Text(stringResource(R.string.finish_setup)) }
                    Text(
                        stringResource(
                            if (state.isSetupComplete) R.string.setup_complete_hint else R.string.finish_setup_hint,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessExplanationDialog(
    type: ExplanationDialog,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val title = when (type) {
        ExplanationDialog.PHONE -> R.string.phone_access_dialog_title
        ExplanationDialog.NOTIFICATIONS -> R.string.notification_access_dialog_title
        ExplanationDialog.SOUND -> R.string.sound_access_dialog_title
    }
    val message = when (type) {
        ExplanationDialog.PHONE -> R.string.phone_access_dialog_message
        ExplanationDialog.NOTIFICATIONS -> R.string.notification_access_dialog_message
        ExplanationDialog.SOUND -> R.string.sound_access_dialog_message
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(message)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(if (type == ExplanationDialog.SOUND) R.string.open_settings_action else R.string.continue_action))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now_action)) } },
    )
}
