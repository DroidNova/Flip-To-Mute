package com.droidnova.fliptomute.audio

enum class VibrationAvailability { AVAILABLE, NO_VIBRATOR, TEMPORARILY_BLOCKED, UNAVAILABLE }

enum class IncomingCallVibrationFailure {
    NO_VIBRATOR, BLOCKED_BY_SYSTEM_POLICY, VIBRATOR_SERVICE_UNAVAILABLE, START_FAILED, UNKNOWN,
}

sealed interface IncomingCallVibrationResult {
    data object Started : IncomingCallVibrationResult
    data class Failed(val reason: IncomingCallVibrationFailure) : IncomingCallVibrationResult
}

interface IncomingCallVibrationController {
    fun getAvailability(): VibrationAvailability
    fun start(): IncomingCallVibrationResult
    fun stop()
}

fun interface IncomingCallVibrationControllerFactory {
    fun create(): IncomingCallVibrationController
}
