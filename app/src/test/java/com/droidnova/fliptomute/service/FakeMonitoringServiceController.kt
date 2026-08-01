package com.droidnova.fliptomute.service

class FakeMonitoringServiceController(
    var startResult: MonitoringCommandResult = MonitoringCommandResult.Accepted,
) : MonitoringServiceController {
    var startCount = 0
    var stopCount = 0
    var pauseCount = 0
    var resumeCount = 0
    override fun startMonitoring(): MonitoringCommandResult { startCount++; return startResult }
    override fun pauseMonitoring(): MonitoringCommandResult { pauseCount++; return MonitoringCommandResult.Accepted }
    override fun resumeMonitoring(): MonitoringCommandResult { resumeCount++; return MonitoringCommandResult.Accepted }
    override fun stopMonitoring(): MonitoringCommandResult { stopCount++; return MonitoringCommandResult.Accepted }
}
