package com.plausiden.app

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager PeriodicWorkRequest that ensures the PollutionService stays running.
 *
 * Handles doze mode, app standby, and battery optimization by periodically
 * checking and restarting the foreground service if necessary.
 */
class ScheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "ScheduleWorker"
        private const val WORK_NAME = "plausiden_schedule_worker"

        /**
         * Enqueue a periodic work request to keep the pollution service alive.
         * Uses ExistingPeriodicWorkPolicy.KEEP to avoid duplicates.
         */
        fun enqueuePeriodicWork(context: Context) {
            val profileManager = ProfileManager(context)
            if (!profileManager.isServiceEnabled()) {
                Log.d(TAG, "Service not enabled, skipping work enqueue")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)  // Run even on low battery
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)     // Run even when device is active
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(
                15, TimeUnit.MINUTES  // Minimum interval for periodic work
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Log.i(TAG, "Periodic work enqueued")
        }

        /**
         * Cancel the periodic work request.
         */
        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Periodic work cancelled")
        }
    }

    override fun doWork(): Result {
        Log.d(TAG, "ScheduleWorker executing")

        val profileManager = ProfileManager(applicationContext)

        // Check if service should be running
        if (!profileManager.isServiceEnabled()) {
            Log.d(TAG, "Service disabled, not restarting")
            return Result.success()
        }

        // Check if within active hours
        if (!profileManager.isWithinActiveHours()) {
            Log.d(TAG, "Outside active hours, not restarting")
            return Result.success()
        }

        // Restart service if not running
        if (!PollutionService.isRunning) {
            Log.i(TAG, "Service not running, restarting")
            val intent = Intent(applicationContext, PollutionService::class.java)
            ContextCompat.startForegroundService(applicationContext, intent)
        } else {
            Log.d(TAG, "Service already running")
        }

        return Result.success()
    }
}
