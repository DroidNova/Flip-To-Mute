package com.droidnova.fliptomute.screenlock

interface ScreenLockController {
    fun lockScreen(): ScreenLockResult
}

sealed interface ScreenLockResult {
    data object Locked : ScreenLockResult
    data object AdminInactive : ScreenLockResult
    data object ScreenNotInteractive : ScreenLockResult
    data object AlreadyLocked : ScreenLockResult
    data object Unsupported : ScreenLockResult
    data object Failed : ScreenLockResult
}
