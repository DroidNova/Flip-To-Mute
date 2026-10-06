package com.droidnova.fliptomute.sensor

/**
 * How readily a flip is accepted (future features F7). Normal is exactly the detection every
 * version so far has shipped with; Quick and Careful move the hold time and the angle around it.
 */
enum class FlipSensitivity(val value: String) {
    QUICK("quick"),
    NORMAL("normal"),
    CAREFUL("careful"),
    ;

    fun detectionConfiguration(): FaceDownDetectionConfiguration = when (this) {
        QUICK -> FaceDownDetectionConfiguration(
            faceDownEnterThreshold = -0.70f,
            faceDownExitThreshold = -0.50f,
            minimumStableDurationMillis = 200L,
        )
        NORMAL -> FaceDownDetectionConfiguration()
        CAREFUL -> FaceDownDetectionConfiguration(
            faceDownEnterThreshold = -0.85f,
            faceDownExitThreshold = -0.65f,
            minimumStableDurationMillis = 800L,
        )
    }

    companion object {
        fun fromValue(value: String?): FlipSensitivity = entries.firstOrNull { it.value == value } ?: NORMAL
    }
}
