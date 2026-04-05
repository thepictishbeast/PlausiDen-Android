package com.plausiden.app

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Manages user profile configuration for PlausiDen.
 *
 * Persists all settings to SharedPreferences: intensity level, enabled categories,
 * active hours, service state, and artifact count.
 */
class ProfileManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "plausiden_profile"
        private const val KEY_INTENSITY = "intensity"
        private const val KEY_CATEGORY_PREFIX = "category_"
        private const val KEY_ACTIVE_HOURS = "active_hours"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_ARTIFACT_COUNT = "artifact_count"
        private const val KEY_SCHEDULE_START_HOUR = "schedule_start_hour"

        const val INTENSITY_LOW = "low"
        const val INTENSITY_MEDIUM = "medium"
        const val INTENSITY_HIGH = "high"
        const val INTENSITY_MAXIMUM = "maximum"

        private val DEFAULT_CATEGORIES = setOf(
            "browser", "filesystem", "contacts", "media", "clipboard"
        )
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Intensity ---

    fun getIntensity(): String =
        prefs.getString(KEY_INTENSITY, INTENSITY_MEDIUM) ?: INTENSITY_MEDIUM

    fun setIntensity(intensity: String) {
        prefs.edit().putString(KEY_INTENSITY, intensity).apply()
    }

    // --- Categories ---

    fun isCategoryEnabled(category: String): Boolean =
        prefs.getBoolean("$KEY_CATEGORY_PREFIX$category", category in DEFAULT_CATEGORIES)

    fun setCategoryEnabled(category: String, enabled: Boolean) {
        prefs.edit().putBoolean("$KEY_CATEGORY_PREFIX$category", enabled).apply()
    }

    fun getEnabledCategories(): List<String> =
        DEFAULT_CATEGORIES.filter { isCategoryEnabled(it) }

    // --- Schedule ---

    fun getActiveHours(): Int = prefs.getInt(KEY_ACTIVE_HOURS, 24)

    fun setActiveHours(hours: Int) {
        prefs.edit().putInt(KEY_ACTIVE_HOURS, hours.coerceIn(1, 24)).apply()
    }

    fun getScheduleStartHour(): Int = prefs.getInt(KEY_SCHEDULE_START_HOUR, 0)

    fun setScheduleStartHour(hour: Int) {
        prefs.edit().putInt(KEY_SCHEDULE_START_HOUR, hour.coerceIn(0, 23)).apply()
    }

    /**
     * Check if the current time falls within the configured active hours window.
     */
    fun isWithinActiveHours(): Boolean {
        val activeHours = getActiveHours()
        if (activeHours >= 24) return true // Always active

        val startHour = getScheduleStartHour()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val endHour = (startHour + activeHours) % 24

        return if (endHour > startHour) {
            currentHour in startHour until endHour
        } else {
            // Wraps around midnight
            currentHour >= startHour || currentHour < endHour
        }
    }

    // --- Service State ---

    fun isServiceEnabled(): Boolean = prefs.getBoolean(KEY_SERVICE_ENABLED, false)

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    // --- Artifact Count ---

    fun getTotalArtifactsGenerated(): Long = prefs.getLong(KEY_ARTIFACT_COUNT, 0L)

    fun incrementArtifactCount(count: Long) {
        val current = getTotalArtifactsGenerated()
        prefs.edit().putLong(KEY_ARTIFACT_COUNT, current + count).apply()
    }

    /**
     * Export the current profile as a JSON string for the Rust engine.
     */
    fun toProfileJson(): String {
        val categories = getEnabledCategories().joinToString(",") { "\"$it\"" }
        return """
        {
            "intensity": "${getIntensity()}",
            "categories": [$categories],
            "active_hours": ${getActiveHours()},
            "schedule_start_hour": ${getScheduleStartHour()}
        }
        """.trimIndent()
    }
}
