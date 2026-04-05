package com.plausiden.app

import android.app.Application
import android.util.Log

/**
 * Application class for PlausiDen.
 * Initializes the notification channel and schedules background work on startup.
 */
class PlausiDenApp : Application() {

    companion object {
        private const val TAG = "PlausiDenApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PlausiDen application starting")

        // Create notification channel for foreground service
        NotificationHelper.createNotificationChannel(this)

        // Schedule periodic work to keep service alive
        ScheduleWorker.enqueuePeriodicWork(this)
    }
}
