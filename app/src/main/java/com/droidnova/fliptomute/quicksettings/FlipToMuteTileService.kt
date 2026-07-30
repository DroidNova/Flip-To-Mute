package com.droidnova.fliptomute.quicksettings

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.droidnova.fliptomute.MainActivity
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.app.FlipToMuteApplication
import com.droidnova.fliptomute.service.MonitoringCommandResult
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FlipToMuteTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container by lazy { (application as FlipToMuteApplication).container }
    private val stateResolver = QuickSettingsTileStateResolver()
    private val clickResolver = QuickSettingsTileClickResolver()
    private var listeningJob: Job? = null
    private var commandPending = false

    override fun onTileAdded() {
        super.onTileAdded()
        container.setupAccessRepository.refresh()
        refreshTile()
    }
    override fun onStartListening() {
        super.onStartListening()
        container.setupAccessRepository.refresh()
        refreshTile()
        listeningJob?.cancel()
        listeningJob = scope.launch {
            container.monitoringStateRepository.state.collectLatest {
                commandPending = false
                refreshTile()
            }
        }
    }
    override fun onStopListening() { listeningJob?.cancel(); listeningJob = null; super.onStopListening() }
    override fun onTileRemoved() {
        listeningJob?.cancel()
        listeningJob = null
        super.onTileRemoved()
    }

    override fun onClick() {
        super.onClick()
        if (commandPending) return
        val setupComplete = container.setupAccessRepository.refreshAndGet().isSetupComplete
        when (clickResolver.resolve(container.monitoringStateRepository.state.value, setupComplete)) {
            QuickSettingsTileClickAction.StartMonitoring -> {
                commandPending = true
                updateTile(QuickSettingsTileStatus.STARTING)
                if (container.monitoringServiceController.startMonitoring() is MonitoringCommandResult.Rejected) {
                    commandPending = false
                    container.monitoringStateRepository.updateState(
                        MonitoringRuntimeState.Error(MonitoringFailure.SERVICE_START_NOT_ALLOWED),
                    )
                    container.quickSettingsTileUpdateRequester.requestUpdate()
                    refreshTile()
                }
            }
            QuickSettingsTileClickAction.StopMonitoring -> {
                commandPending = true
                updateTile(QuickSettingsTileStatus.STOPPING)
                container.monitoringServiceController.stopMonitoring()
            }
            QuickSettingsTileClickAction.OpenSetupAndEnable -> unlockAndRun { openSetupAndEnable() }
            QuickSettingsTileClickAction.Ignore -> refreshTile()
        }
    }

    override fun onDestroy() { listeningJob?.cancel(); scope.cancel(); super.onDestroy() }

    private fun refreshTile() {
        updateTile(
            stateResolver.resolve(
                container.monitoringStateRepository.state.value,
                container.setupAccessRepository.accessState.value.isSetupComplete,
            ),
        )
    }

    private fun updateTile(status: QuickSettingsTileStatus) {
        val tile = qsTile ?: return
        val subtitle = getString(status.subtitleResource())
        tile.label = getString(R.string.quick_settings_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_flip_to_mute)
        tile.state = when (status) {
            QuickSettingsTileStatus.ON -> Tile.STATE_ACTIVE
            QuickSettingsTileStatus.STARTING, QuickSettingsTileStatus.STOPPING -> Tile.STATE_UNAVAILABLE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle
            tile.contentDescription = "${tile.label}, $subtitle"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) tile.stateDescription = subtitle
        tile.updateTile()
    }

    private fun openSetupAndEnable() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivityLaunchRequestParser.OPEN_SETUP_AND_ENABLE_ACTION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val wrapper = PendingIntentActivityWrapper(
            this,
            REQUEST_CODE,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            false,
        )
        TileServiceCompat.startActivityAndCollapse(this, wrapper)
    }

    private fun QuickSettingsTileStatus.subtitleResource() = when (this) {
        QuickSettingsTileStatus.ON -> R.string.quick_settings_tile_on
        QuickSettingsTileStatus.OFF -> R.string.quick_settings_tile_off
        QuickSettingsTileStatus.STARTING -> R.string.quick_settings_tile_starting
        QuickSettingsTileStatus.STOPPING -> R.string.quick_settings_tile_stopping
        QuickSettingsTileStatus.SETUP_REQUIRED -> R.string.quick_settings_tile_setup_required
        QuickSettingsTileStatus.ERROR -> R.string.quick_settings_tile_start_failed
    }

    private companion object { const val REQUEST_CODE = 4102 }
}
