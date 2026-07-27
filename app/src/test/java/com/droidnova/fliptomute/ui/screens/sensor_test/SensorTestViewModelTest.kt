package com.droidnova.fliptomute.ui.screens.sensor_test

import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.FakeDeviceOrientationMonitor
import com.droidnova.fliptomute.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorTestViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startIsIdempotentAndStopClearsState() = runTest {
        val monitor = FakeDeviceOrientationMonitor()
        val viewModel = SensorTestViewModel(monitor)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.startTest()
        viewModel.startTest()
        assertEquals(1, monitor.startCount)
        assertTrue(viewModel.uiState.value.isTesting)
        monitor.emit(DeviceOrientation.FACE_DOWN)
        viewModel.stopTest()
        assertEquals(1, monitor.stopCount)
        assertFalse(viewModel.uiState.value.isTesting)
        assertEquals(DeviceOrientation.UNKNOWN, viewModel.uiState.value.orientation)
    }

    @Test
    fun unavailableSensorDoesNotStart() {
        val monitor = FakeDeviceOrientationMonitor(isSensorAvailable = false)
        val viewModel = SensorTestViewModel(monitor)
        viewModel.startTest()
        assertEquals(0, monitor.startCount)
        assertFalse(viewModel.uiState.value.isSensorAvailable)
    }

    @Test
    fun detectorStatesMapToUi() = runTest {
        val monitor = FakeDeviceOrientationMonitor()
        val viewModel = SensorTestViewModel(monitor)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.startTest()

        listOf(DeviceOrientation.FACE_UP, DeviceOrientation.MOVING, DeviceOrientation.FACE_DOWN).forEach {
            monitor.emit(it)
            assertEquals(it, viewModel.uiState.value.orientation)
        }
        monitor.emitError()
        assertTrue(viewModel.uiState.value.hasError)
    }
}
