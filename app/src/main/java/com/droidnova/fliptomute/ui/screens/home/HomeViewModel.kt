package com.droidnova.fliptomute.ui.screens.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun selectFlipAction(action: FlipAction) {
        _uiState.update { state -> state.copy(selectedFlipAction = action) }
    }

    fun onSetupItemClick(type: SetupItemType) = when (type) {
        SetupItemType.PHONE,
        SetupItemType.SOUND_CONTROL,
        SetupItemType.NOTIFICATIONS,
        -> Unit // Permission and settings actions are intentionally deferred.
    }
}
