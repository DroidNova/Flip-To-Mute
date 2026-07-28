package com.droidnova.fliptomute.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.droidnova.fliptomute.util.MonitoringLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidCellularCallMonitor(context: Context) : CellularCallMonitor {
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager
    private val baseTelephonyManager = applicationContext.getSystemService(TelephonyManager::class.java)
    private val subscriptionManager = applicationContext.getSystemService(SubscriptionManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val registrations = mutableListOf<Registration>()
    private val subscriptionStates = mutableMapOf<Int, CellularCallState>()
    private val mutableState = MutableStateFlow<CellularCallMonitorState>(CellularCallMonitorState.Stopped)
    override val state: StateFlow<CellularCallMonitorState> = mutableState.asStateFlow()
    override val isTelephonyAvailable: Boolean
        get() = TelephonyAvailabilityPolicy.canAttemptRegistration(
            telephonyManagerAvailable = baseTelephonyManager != null,
            hasBaseFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
            hasCallingFeature = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CALLING),
        )
    private var isStarting = false
    private var isListening = false

    override fun start() {
        if (isStarting || isListening) return
        isStarting = true
        runOnMainThread(::startOnMainThread)
    }

    override fun stop() {
        isStarting = false
        runOnMainThread {
            clearRegistrations()
            mutableState.value = CellularCallMonitorState.Stopped
        }
    }

    private fun startOnMainThread() {
        if (!isStarting || isListening) return
        val hasBaseFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        val hasCallingFeature = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CALLING)
        debugLog("FEATURE_TELEPHONY: $hasBaseFeature")
        debugLog("FEATURE_TELEPHONY_CALLING: $hasCallingFeature")
        debugLog("TelephonyManager available: ${baseTelephonyManager != null}")
        if (baseTelephonyManager == null) {
            isStarting = false
            mutableState.value = CellularCallMonitorState.TelephonyUnavailable
            return
        }
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            isStarting = false
            mutableState.value = CellularCallMonitorState.PermissionRequired
            return
        }
        val baseManager = baseTelephonyManager
        val managers = activeTelephonyManagers(baseManager)
        debugLog("Active subscription count: ${managers.size}")
        clearRegistrations()
        isStarting = true
        var permissionFailure = false
        var attemptCount = 0
        var unsupportedCount = 0
        managers.forEachIndexed { token, manager ->
            debugLog("Attempting subscription-specific registration")
            attemptCount++
            try {
                register(manager, token)
            } catch (error: SecurityException) {
                permissionFailure = true
                logRegistrationFailure(error)
            } catch (error: UnsupportedOperationException) {
                unsupportedCount++
                logRegistrationFailure(error)
            } catch (error: RuntimeException) {
                logRegistrationFailure(error)
            }
        }
        debugLog("Subscription callback registration success count: ${registrations.size}")
        if (registrations.isEmpty()) {
            debugLog("Attempting default TelephonyManager fallback")
            attemptCount++
            try {
                register(baseManager, managers.size)
                debugLog("Default TelephonyManager registration succeeded")
            } catch (error: SecurityException) {
                permissionFailure = true
                logDefaultRegistrationFailure(error)
            } catch (error: UnsupportedOperationException) {
                unsupportedCount++
                logDefaultRegistrationFailure(error)
            } catch (error: RuntimeException) {
                logDefaultRegistrationFailure(error)
            }
        }
        debugLog("Final successful registration count: ${registrations.size}")
        if (registrations.isNotEmpty()) {
            isListening = true
            isStarting = false
            updateAggregateState()
            debugLog("Successful telephony registration count: ${registrations.size}")
        } else when (TelephonyRegistrationResult.resolve(attemptCount, unsupportedCount, permissionFailure)) {
            TelephonyRegistrationFailure.PERMISSION_REQUIRED -> {
                clearRegistrations()
                mutableState.value = CellularCallMonitorState.PermissionRequired
                debugLog("Telephony registration failed: PermissionRequired")
            }
            TelephonyRegistrationFailure.UNSUPPORTED -> {
                clearRegistrations()
                mutableState.value = CellularCallMonitorState.TelephonyUnavailable
                debugLog("Telephony registration failed: TelephonyUnavailable")
            }
            TelephonyRegistrationFailure.REGISTRATION_FAILED -> {
                registrationError(CellularCallMonitorError.REGISTRATION_FAILED)
                debugLog("Telephony registration failed: REGISTRATION_FAILED")
            }
        }
    }

    private fun activeTelephonyManagers(baseManager: TelephonyManager): List<TelephonyManager> {
        val subscriptionIds = try {
            subscriptionManager?.activeSubscriptionInfoList
                ?.asSequence()
                ?.map { it.subscriptionId }
                ?.filter { subscriptionId -> SubscriptionManager.isValidSubscriptionId(subscriptionId) }
                ?.distinct()
                ?.toList()
                .orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: UnsupportedOperationException) {
            emptyList()
        }
        return subscriptionIds.mapNotNull {
            try {
                baseManager.createForSubscriptionId(it)
            } catch (error: RuntimeException) {
                logRegistrationFailure(error)
                null
            }
        }
    }

    private fun register(manager: TelephonyManager, token: Int) {
        val onChanged: (Int) -> Unit = { androidState -> onCallStateChanged(token, androidState) }
        val registration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerModern(manager, onChanged)
        } else {
            LegacyRegistration(LegacyCallStateRegistration(manager, onChanged)).also { it.register() }
        }
        registrations += registration
        subscriptionStates.putIfAbsent(token, CellularCallState.UNKNOWN)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerModern(manager: TelephonyManager, onChanged: (Int) -> Unit): Registration {
        val callback = ModernCallStateCallback(onChanged)
        manager.registerTelephonyCallback(applicationContext.mainExecutor, callback)
        return ModernRegistration(manager, callback)
    }

    private fun onCallStateChanged(token: Int, androidState: Int) {
        if (!isListening && !isStarting) return
        subscriptionStates[token] = CellularCallStateMapper.fromAndroidState(androidState)
        updateAggregateState()
    }

    private fun updateAggregateState() {
        val aggregate = CellularCallStateAggregator.aggregate(subscriptionStates.values)
        mutableState.value = CellularCallMonitorState.Listening(aggregate, registrations.size)
        debugLog("Call monitor emitted state: Listening, ${aggregate.name}")
    }

    private fun registrationError(error: CellularCallMonitorError) {
        clearRegistrations()
        mutableState.value = CellularCallMonitorState.Error(error)
    }

    private fun clearRegistrations() {
        registrations.forEach { registration ->
            try {
                registration.unregister()
            } catch (_: SecurityException) {
                // Registration bookkeeping is still cleared when permission was revoked.
            } catch (_: IllegalStateException) {
                // The telephony service may already have discarded the registration.
            } catch (_: UnsupportedOperationException) {
                // The device stopped supporting the registration while it was active.
            }
        }
        registrations.clear()
        subscriptionStates.clear()
        isListening = false
        isStarting = false
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun debugLog(message: String) {
        MonitoringLog.d(applicationContext, message)
    }

    private fun logRegistrationFailure(error: RuntimeException) =
        MonitoringLog.failure(applicationContext, "Telephony registration attempt failed", error)

    private fun logDefaultRegistrationFailure(error: RuntimeException) =
        MonitoringLog.failure(applicationContext, "Default TelephonyManager registration failed", error)

    private interface Registration {
        fun unregister()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private class ModernRegistration(
        private val manager: TelephonyManager,
        private val callback: ModernCallStateCallback,
    ) : Registration {
        override fun unregister() = manager.unregisterTelephonyCallback(callback)
    }

    private class LegacyRegistration(
        private val registration: LegacyCallStateRegistration,
    ) : Registration {
        fun register() = registration.register()
        override fun unregister() = registration.unregister()
    }

}
