package com.droidnova.fliptomute.sensor

class AccelerometerGravityFilter(private val alpha: Float = 0.8f) {
    var x: Float = 0f
        private set
    var y: Float = 0f
        private set
    var z: Float = 0f
        private set
    var isInitialized: Boolean = false
        private set

    init {
        require(alpha in 0f..1f)
    }

    fun update(rawX: Float, rawY: Float, rawZ: Float) {
        if (!isInitialized) {
            x = rawX
            y = rawY
            z = rawZ
            isInitialized = true
            return
        }
        x = alpha * x + (1f - alpha) * rawX
        y = alpha * y + (1f - alpha) * rawY
        z = alpha * z + (1f - alpha) * rawZ
    }

    fun reset() {
        x = 0f
        y = 0f
        z = 0f
        isInitialized = false
    }
}
