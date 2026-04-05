package com.plausiden.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Broadcast receiver that restarts the PollutionService after device boot.
 *
 * Only starts the service if the user had previously enabled it.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "Boot completed, checking if service should restart")

        val profileManager = ProfileManager(context)
        if (profileManager.isServiceEnabled()) {
            Log.i(TAG, "Service was enabled, restarting")
            val serviceIntent = Intent(context, PollutionService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)

            // Re-enqueue periodic work
            ScheduleWorker.enqueuePeriodicWork(context)
        } else {
            Log.d(TAG, "Service was not enabled, not restarting")
        }
    }
}
