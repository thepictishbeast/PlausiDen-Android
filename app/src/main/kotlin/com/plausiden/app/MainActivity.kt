package com.plausiden.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

/**
 * Main activity for PlausiDen.
 *
 * Material Design 3 UI with:
 * - Status card showing running/paused state and total artifacts generated
 * - Intensity selector (Low/Medium/High/Maximum)
 * - Category toggles (browser, filesystem, contacts, etc.)
 * - Schedule configuration (active hours)
 * - Start/Stop button
 */
class MainActivity : AppCompatActivity() {

    private lateinit var profileManager: ProfileManager

    // UI elements
    private lateinit var statusText: TextView
    private lateinit var artifactCountText: TextView
    private lateinit var intensityGroup: RadioGroup
    private lateinit var toggleBrowser: MaterialSwitch
    private lateinit var toggleFilesystem: MaterialSwitch
    private lateinit var toggleContacts: MaterialSwitch
    private lateinit var toggleMedia: MaterialSwitch
    private lateinit var toggleClipboard: MaterialSwitch
    private lateinit var scheduleSlider: Slider
    private lateinit var scheduleLabel: TextView
    private lateinit var startStopButton: Button

    private var isServiceRunning = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startPollutionService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        profileManager = ProfileManager(this)
        bindViews()
        loadProfile()
        updateStatusDisplay()
    }

    override fun onResume() {
        super.onResume()
        updateStatusDisplay()
    }

    private fun bindViews() {
        statusText = findViewById(R.id.status_text)
        artifactCountText = findViewById(R.id.artifact_count_text)
        intensityGroup = findViewById(R.id.intensity_group)
        toggleBrowser = findViewById(R.id.toggle_browser)
        toggleFilesystem = findViewById(R.id.toggle_filesystem)
        toggleContacts = findViewById(R.id.toggle_contacts)
        toggleMedia = findViewById(R.id.toggle_media)
        toggleClipboard = findViewById(R.id.toggle_clipboard)
        scheduleSlider = findViewById(R.id.schedule_slider)
        scheduleLabel = findViewById(R.id.schedule_label)
        startStopButton = findViewById(R.id.start_stop_button)

        // Intensity selector
        intensityGroup.setOnCheckedChangeListener { _, checkedId ->
            val intensity = when (checkedId) {
                R.id.radio_low -> ProfileManager.INTENSITY_LOW
                R.id.radio_medium -> ProfileManager.INTENSITY_MEDIUM
                R.id.radio_high -> ProfileManager.INTENSITY_HIGH
                R.id.radio_maximum -> ProfileManager.INTENSITY_MAXIMUM
                else -> ProfileManager.INTENSITY_MEDIUM
            }
            profileManager.setIntensity(intensity)
        }

        // Category toggles — save on change
        toggleBrowser.setOnCheckedChangeListener { _, checked ->
            profileManager.setCategoryEnabled("browser", checked)
        }
        toggleFilesystem.setOnCheckedChangeListener { _, checked ->
            profileManager.setCategoryEnabled("filesystem", checked)
        }
        toggleContacts.setOnCheckedChangeListener { _, checked ->
            profileManager.setCategoryEnabled("contacts", checked)
        }
        toggleMedia.setOnCheckedChangeListener { _, checked ->
            profileManager.setCategoryEnabled("media", checked)
        }
        toggleClipboard.setOnCheckedChangeListener { _, checked ->
            profileManager.setCategoryEnabled("clipboard", checked)
        }

        // Schedule slider (active hours: 1-24)
        scheduleSlider.addOnChangeListener { _, value, _ ->
            val hours = value.toInt()
            scheduleLabel.text = getString(R.string.schedule_hours_label, hours)
            profileManager.setActiveHours(hours)
        }

        // Start/Stop button
        startStopButton.setOnClickListener {
            if (isServiceRunning) {
                stopPollutionService()
            } else {
                requestStartService()
            }
        }
    }

    private fun loadProfile() {
        // Intensity
        val intensity = profileManager.getIntensity()
        val radioId = when (intensity) {
            ProfileManager.INTENSITY_LOW -> R.id.radio_low
            ProfileManager.INTENSITY_MEDIUM -> R.id.radio_medium
            ProfileManager.INTENSITY_HIGH -> R.id.radio_high
            ProfileManager.INTENSITY_MAXIMUM -> R.id.radio_maximum
            else -> R.id.radio_medium
        }
        intensityGroup.check(radioId)

        // Categories
        toggleBrowser.isChecked = profileManager.isCategoryEnabled("browser")
        toggleFilesystem.isChecked = profileManager.isCategoryEnabled("filesystem")
        toggleContacts.isChecked = profileManager.isCategoryEnabled("contacts")
        toggleMedia.isChecked = profileManager.isCategoryEnabled("media")
        toggleClipboard.isChecked = profileManager.isCategoryEnabled("clipboard")

        // Schedule
        val hours = profileManager.getActiveHours()
        scheduleSlider.value = hours.toFloat()
        scheduleLabel.text = getString(R.string.schedule_hours_label, hours)
    }

    private fun updateStatusDisplay() {
        isServiceRunning = PollutionService.isRunning
        statusText.text = if (isServiceRunning) {
            getString(R.string.status_running)
        } else {
            getString(R.string.status_paused)
        }

        val count = profileManager.getTotalArtifactsGenerated()
        artifactCountText.text = getString(R.string.artifact_count, count)

        startStopButton.text = if (isServiceRunning) {
            getString(R.string.stop_button)
        } else {
            getString(R.string.start_button)
        }
    }

    private fun requestStartService() {
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startPollutionService()
    }

    private fun startPollutionService() {
        val intent = Intent(this, PollutionService::class.java)
        ContextCompat.startForegroundService(this, intent)
        isServiceRunning = true
        updateStatusDisplay()
    }

    private fun stopPollutionService() {
        val intent = Intent(this, PollutionService::class.java)
        stopService(intent)
        isServiceRunning = false
        updateStatusDisplay()
    }
}
