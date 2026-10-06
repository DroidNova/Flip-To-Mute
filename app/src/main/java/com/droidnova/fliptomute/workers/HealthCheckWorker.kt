package com.droidnova.fliptomute.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.droidnova.fliptomute.app.FlipToMuteApplication
import com.droidnova.fliptomute.util.MonitoringLog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/**
 * Runs [com.droidnova.fliptomute.service.MonitoringHealthCheck] about every six hours. Built like
 * Secret Calculator's TrashCleanupWorker. It reaches its dependencies through the app container
 * for now; M2 moves this to a Hilt entry point, as in Secret Calculator.
 */
class HealthCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as FlipToMuteApplication).container
        return try {
            val result = container.monitoringHealthCheck.run()
            MonitoringLog.d(applicationContext, "Health check: ${result.name}")
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // A failed check is not worth retrying sooner; the next period runs anyway
            MonitoringLog.failure(applicationContext, "Health check failed", error)
            Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "monitoring_health_check"
        private const val INTERVAL_HOURS = 6L

        /** Safe to call on every app start: the unique name keeps a single schedule. Never throws. */
        fun schedule(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<HealthCheckWorker>(INTERVAL_HOURS, TimeUnit.HOURS).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            } catch (error: IllegalStateException) {
                // WorkManager not initialised (for example in JVM tests); app start must not fail
                MonitoringLog.failure(context, "Health check scheduling failed", error)
            }
        }
    }
}
