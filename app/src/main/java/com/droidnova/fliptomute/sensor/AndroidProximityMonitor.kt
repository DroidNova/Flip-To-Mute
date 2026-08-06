package com.droidnova.fliptomute.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidProximityMonitor(context: Context) : ProximityMonitor, SensorEventListener {
    private val sensorManager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val classifier = ProximityValueClassifier()
    private val mutableState = MutableStateFlow<ProximityMonitorState>(ProximityMonitorState.Stopped)
    override val state: StateFlow<ProximityMonitorState> = mutableState.asStateFlow()
    override val isSensorAvailable: Boolean get() = sensor != null
    private var started = false

    override fun start() {
        if (started) return
        val proximitySensor = sensor ?: run {
            mutableState.value = ProximityMonitorState.SensorUnavailable
            return
        }
        mutableState.value = ProximityMonitorState.WaitingForReading
        started = try {
            sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL) == true
        } catch (_: RuntimeException) {
            false
        }
        if (!started) mutableState.value = ProximityMonitorState.Error
    }

    override fun stop() {
        if (started) sensorManager?.unregisterListener(this)
        started = false
        mutableState.value = ProximityMonitorState.Stopped
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!started || event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val classified = classifier.classify(event.values.firstOrNull() ?: Float.NaN, event.sensor.maximumRange)
        if (classified == ProximityState.UNKNOWN) return
        val next = ProximityMonitorState.Listening(classified)
        if (mutableState.value != next) mutableState.value = next
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

class AndroidProximitySensorCapability(context: Context) : ProximitySensorCapability {
    override val isAvailable = context.applicationContext.getSystemService(SensorManager::class.java)
        ?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
}
