package com.droidnova.fliptomute.ui.screens.call_state_test

import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.telephony.CellularCallMonitorError
import com.droidnova.fliptomute.telephony.CellularCallMonitorState
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.telephony.FakeCellularCallMonitor
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
class CallStateTestViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startIsIdempotentAndStopClearsCurrentState() = runTest {
        val monitor = FakeCellularCallMonitor()
        val viewModel = createViewModel(monitor)
        collect(viewModel)
        viewModel.startListening()
        viewModel.startListening()
        assertEquals(1, monitor.startCount)
        assertTrue(viewModel.uiState.value.isListening)

        monitor.emit(CellularCallMonitorState.Listening(CellularCallState.RINGING, 1))
        viewModel.stopListening()
        assertEquals(1, monitor.stopCount)
        assertFalse(viewModel.uiState.value.isListening)
        assertEquals(CellularCallState.UNKNOWN, viewModel.uiState.value.currentCallState)
    }

    @Test
    fun monitorAvailabilityPermissionAndErrorsMap() = runTest {
        val monitor = FakeCellularCallMonitor()
        val setup = FakeSetupAccessRepository(grantedAccess())
        val viewModel = CallStateTestViewModel(monitor, setup)
        collect(viewModel)
        monitor.emit(CellularCallMonitorState.PermissionRequired)
        assertFalse(viewModel.uiState.value.hasPhonePermission)
        monitor.emit(CellularCallMonitorState.TelephonyUnavailable)
        assertFalse(viewModel.uiState.value.isTelephonyAvailable)
        monitor.emit(CellularCallMonitorState.Error(CellularCallMonitorError.REGISTRATION_FAILED))
        assertEquals(CellularCallMonitorError.REGISTRATION_FAILED, viewModel.uiState.value.error)
    }

    @Test
    fun historyKeepsDistinctRecentTransitionsAndRingingAfterIdle() = runTest {
        val monitor = FakeCellularCallMonitor()
        val viewModel = createViewModel(monitor)
        collect(viewModel)
        viewModel.startListening()
        listOf(
            CellularCallState.IDLE,
            CellularCallState.RINGING,
            CellularCallState.RINGING,
            CellularCallState.ACTIVE,
            CellularCallState.IDLE,
            CellularCallState.ACTIVE,
            CellularCallState.IDLE,
        ).forEach { monitor.emit(CellularCallMonitorState.Listening(it, 2)) }

        assertEquals(5, viewModel.uiState.value.recentTransitions.size)
        assertTrue(CellularCallState.RINGING in viewModel.uiState.value.recentTransitions)
        assertEquals(CellularCallState.IDLE, viewModel.uiState.value.recentTransitions.first())

        viewModel.stopListening()
        viewModel.startListening()
        assertTrue(viewModel.uiState.value.recentTransitions.isEmpty())
    }

    private fun createViewModel(monitor: FakeCellularCallMonitor) =
        CallStateTestViewModel(monitor, FakeSetupAccessRepository(grantedAccess()))

    private suspend fun kotlinx.coroutines.test.TestScope.collect(viewModel: CallStateTestViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun grantedAccess() = SetupAccessState(phoneStateStatus = SetupAccessStatus.GRANTED)
}
