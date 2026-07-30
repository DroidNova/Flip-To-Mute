package com.droidnova.fliptomute.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class AndroidDeviceOrientationMonitor(
    context: Context,
    private val configuration: FaceDownDetectionConfiguration = FaceDownDetectionConfiguration(),
) : DeviceOrientationMonitor, SensorEventListener {
    private val sensorManager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val selectedSensor = gravitySensor ?: accelerometer
    private val sensorSource = when (selectedSensor?.type) {
        Sensor.TYPE_GRAVITY -> OrientationSensorSource.GRAVITY
        Sensor.TYPE_ACCELEROMETER -> OrientationSensorSource.ACCELEROMETER
        else -> null
    }
    private val classifier = DeviceOrientationClassifier(configuration)
    private val accelerometerFilter = AccelerometerGravityFilter(configuration.accelerometerFilterAlpha)
    private val flatStabilityDetector = FlatSurfaceStabilityDetector(configuration)
    private val mutableState = MutableStateFlow(initialState())
    override val state: StateFlow<FaceDownDetectionState> = mutableState.asStateFlow()
    override val isSensorAvailable: Boolean get() = selectedSensor != null
    private var isStarted = false

    override fun start() {
        if (isStarted) return
        val sensor = selectedSensor
        val source = sensorSource
        if (sensorManager == null || sensor == null || source == null) {
            mutableState.value = FaceDownDetectionState.SensorUnavailable
            return
        }
        resetProcessing()
        isStarted = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        mutableState.value = if (isStarted) {
            FaceDownDetectionState.Detecting(DeviceOrientation.UNKNOWN, source, 0f, 0f, 0f)
        } else {
            FaceDownDetectionState.Error
        }
    }

    override fun stop() {
        if (isStarted) sensorManager?.unregisterListener(this)
        isStarted = false
        resetProcessing()
        mutableState.value = initialState()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isStarted || event.sensor != selectedSensor || event.values.size < VECTOR_SIZE) return
        var x = event.values[0]
        var y = event.values[1]
        var z = event.values[2]
        if (sensorSource == OrientationSensorSource.ACCELEROMETER) {
            accelerometerFilter.update(x, y, z)
            x = accelerometerFilter.x
            y = accelerometerFilter.y
            z = accelerometerFilter.z
        }
        val source = sensorSource ?: return
        mutableState.value = FaceDownDetectionState.Detecting(
            orientation = classifier.processSample(x, y, z, event.timestamp),
            sensorSource = source,
            gravityX = x,
            gravityY = y,
            gravityZ = z,
            isFlatAndStable = flatStabilityDetector.processSample(x, y, z, event.timestamp),
            isNearlyHorizontal = isNearlyHorizontal(x, y, z),
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun resetProcessing() {
        classifier.reset()
        accelerometerFilter.reset()
        flatStabilityDetector.reset()
    }

    private fun initialState(): FaceDownDetectionState = if (isSensorAvailable) {
        FaceDownDetectionState.Idle(true, sensorSource)
    } else {
        FaceDownDetectionState.SensorUnavailable
    }

    private fun isNearlyHorizontal(x: Float, y: Float, z: Float): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z)
        return magnitude.isFinite() && magnitude > 0f &&
            abs(z / magnitude) >= configuration.flatOrientationThreshold
    }

    private companion object {
        const val VECTOR_SIZE = 3
    }
}
