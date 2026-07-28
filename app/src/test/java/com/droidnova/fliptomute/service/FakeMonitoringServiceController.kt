package com.droidnova.fliptomute.service

class FakeMonitoringServiceController(
    var startResult: MonitoringCommandResult = MonitoringCommandResult.Accepted,
) : MonitoringServiceController {
    var startCount = 0
    var stopCount = 0
    override fun startMonitoring(): MonitoringCommandResult { startCount++; return startResult }
    override fun stopMonitoring() { stopCount++ }
}
