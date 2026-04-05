package com.plausiden.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

/**
 * Foreground service that runs the Rust engine in the background.
 *
 * Displays a persistent notification with innocuous text: "Device optimization running"
 * Calls NativeBridge to generate and inject artifacts at the configured intensity.
 */
class PollutionService : Service() {

    companion object {
        private const val TAG = "PollutionService"
        private const val NOTIFICATION_ID = 1

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var profileManager: ProfileManager
    private var pollutionJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PollutionService created")
        profileManager = ProfileManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "PollutionService starting")

        // Start as foreground service with innocuous notification
        val notification = NotificationHelper.buildForegroundNotification(this)
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true

        // Start artifact generation loop
        startPollutionLoop()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "PollutionService stopping")
        isRunning = false
        pollutionJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Main loop that periodically generates artifacts via the Rust engine.
     * Interval and artifact count depend on the configured intensity level.
     */
    private fun startPollutionLoop() {
        pollutionJob?.cancel()
        pollutionJob = serviceScope.launch {
            while (isActive) {
                try {
                    val intensity = profileManager.getIntensity()
                    val intervalMs = getIntervalForIntensity(intensity)
                    val batchSize = getBatchSizeForIntensity(intensity)

                    // Check which categories are enabled
                    val enabledCategories = listOf(
                        "browser", "filesystem", "contacts", "media", "clipboard"
                    ).filter { profileManager.isCategoryEnabled(it) }

                    // Generate artifacts for each enabled category
                    for (category in enabledCategories) {
                        if (!isActive) break
                        try {
                            val riskLevel = mapIntensityToRisk(intensity)
                            val result = NativeBridge.generateArtifacts(
                                category, batchSize, riskLevel
                            )
                            Log.d(TAG, "Generated artifacts for $category: $result")
                            profileManager.incrementArtifactCount(batchSize.toLong())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error generating artifacts for $category", e)
                        }
                    }

                    delay(intervalMs)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error in pollution loop", e)
                    delay(60_000) // Back off on error
                }
            }
        }
    }

    private fun getIntervalForIntensity(intensity: String): Long = when (intensity) {
        ProfileManager.INTENSITY_LOW -> 300_000L      // 5 minutes
        ProfileManager.INTENSITY_MEDIUM -> 120_000L   // 2 minutes
        ProfileManager.INTENSITY_HIGH -> 30_000L      // 30 seconds
        ProfileManager.INTENSITY_MAXIMUM -> 10_000L   // 10 seconds
        else -> 120_000L
    }

    private fun getBatchSizeForIntensity(intensity: String): Int = when (intensity) {
        ProfileManager.INTENSITY_LOW -> 1
        ProfileManager.INTENSITY_MEDIUM -> 3
        ProfileManager.INTENSITY_HIGH -> 5
        ProfileManager.INTENSITY_MAXIMUM -> 10
        else -> 3
    }

    private fun mapIntensityToRisk(intensity: String): String = when (intensity) {
        ProfileManager.INTENSITY_LOW -> "low"
        ProfileManager.INTENSITY_MEDIUM -> "medium"
        ProfileManager.INTENSITY_HIGH -> "high"
        ProfileManager.INTENSITY_MAXIMUM -> "maximum"
        else -> "medium"
    }
}
