package com.droidnova.fliptomute.data.setup

enum class SetupAccessType { PHONE_STATE, SOUND_CONTROL, NOTIFICATIONS }

enum class SetupAccessStatus { GRANTED, NOT_GRANTED, NOT_SUPPORTED }

data class SetupAccessState(
    val phoneStateStatus: SetupAccessStatus = SetupAccessStatus.NOT_GRANTED,
    val soundControlStatus: SetupAccessStatus = SetupAccessStatus.NOT_GRANTED,
    val notificationStatus: SetupAccessStatus = SetupAccessStatus.NOT_GRANTED,
) {
    val isSetupComplete: Boolean
        get() = phoneStateStatus == SetupAccessStatus.GRANTED &&
            soundControlStatus == SetupAccessStatus.GRANTED &&
            notificationStatus == SetupAccessStatus.GRANTED

    fun statusFor(type: SetupAccessType): SetupAccessStatus = when (type) {
        SetupAccessType.PHONE_STATE -> phoneStateStatus
        SetupAccessType.SOUND_CONTROL -> soundControlStatus
        SetupAccessType.NOTIFICATIONS -> notificationStatus
    }
}
