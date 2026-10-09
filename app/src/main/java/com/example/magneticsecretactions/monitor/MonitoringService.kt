package com.example.magneticsecretactions.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.magneticsecretactions.action.ActionEngine
import com.example.magneticsecretactions.action.MagneticAction
import com.example.magneticsecretactions.pattern.PatternRecognizer
import com.example.magneticsecretactions.settings.ActionChoice
import com.example.magneticsecretactions.settings.AppSettings
import com.example.magneticsecretactions.sensor.MagneticDetector
import com.example.magneticsecretactions.sensor.SensorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MonitoringService : LifecycleService() {
    private lateinit var sensors: SensorRepository
    private lateinit var settings: AppSettings
    private lateinit var actionEngine: ActionEngine
    private val detector = MagneticDetector(cooldownMs = 500L)
    private lateinit var recognizer: PatternRecognizer
    private var patternJob: Job? = null
    override fun onCreate() {
        super.onCreate()
        sensors = SensorRepository(this)
        settings = AppSettings(this)
        recognizer = PatternRecognizer(patternTimeoutMs = settings.patternTimeoutMs)
        actionEngine = ActionEngine(this)
        detector.sensitivity = settings.sensitivity
        settings.baseline?.let { detector.restoreCalibration(it, settings.threshold) }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Magnetic monitoring", NotificationManager.IMPORTANCE_LOW))
        startForeground(ID, Notification.Builder(this, CHANNEL)
            .setContentTitle("Magnetic monitoring active")
            .setContentText("Monitoring locally for magnetic patterns")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true).build())
        if (!sensors.start()) {
            stopSelf()
            return
        }
        lifecycleScope.launch {
            sensors.samples.collect { sample ->
                sample ?: return@collect
                detector.accept(sample)?.let { event ->
                    if (event.state.name == "MAGNET_REMOVED") {
                        recognizer.tap(event.sample.timeMs)
                        patternJob?.cancel()
                        patternJob = launch {
                            delay(settings.patternTimeoutMs + 50L)
                            recognizer.flush(event.sample.timeMs + settings.patternTimeoutMs + 50L)?.let { pattern ->
                                val action = when (settings.actionFor(pattern.name)) {
                                    ActionChoice.FLASHLIGHT -> MagneticAction.ToggleFlashlight
                                    ActionChoice.NOTIFICATION -> MagneticAction.Notification(
                                        "Magnetic ring action", pattern.label
                                    )
                                    ActionChoice.OPEN_URL -> MagneticAction.OpenUrl(settings.url)
                                    ActionChoice.OPEN_APP -> MagneticAction.OpenApp(settings.appPackage)
                                    ActionChoice.MEDIA_PLAY_PAUSE -> MagneticAction.MediaPlayPause()
                                }
                                actionEngine.execute(action)
                                    .onFailure { error ->
                                        publishActionResult("${pattern.label} failed: ${error.message}")
                                    }
                                    .onSuccess {
                                        publishActionResult("${pattern.label} executed")
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        if (::sensors.isInitialized) sensors.stop()
        super.onDestroy()
    }
    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
    private fun publishActionResult(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            RESULT_ID,
            Notification.Builder(this, CHANNEL)
                .setContentTitle("Magnetic ring action")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setAutoCancel(true)
                .build()
        )
    }
    companion object {
        private const val CHANNEL = "magnetic_monitoring"
        private const val ID = 1001
        private const val RESULT_ID = 1002
    }
}
