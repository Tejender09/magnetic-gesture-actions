package com.example.magneticsecretactions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.magneticsecretactions.action.ActionEngine
import com.example.magneticsecretactions.action.MagneticAction
import com.example.magneticsecretactions.monitor.MonitoringService
import com.example.magneticsecretactions.pattern.MagneticPattern
import com.example.magneticsecretactions.pattern.PatternRecognizer
import com.example.magneticsecretactions.sensor.MagneticDetector
import com.example.magneticsecretactions.sensor.MagneticSample
import com.example.magneticsecretactions.sensor.MagneticState
import com.example.magneticsecretactions.sensor.SensorRepository
import com.example.magneticsecretactions.settings.ActionChoice
import com.example.magneticsecretactions.settings.AppSettings
import com.example.magneticsecretactions.ui.AppTab
import com.example.magneticsecretactions.ui.FloatingBottomNavBar
import com.example.magneticsecretactions.ui.GestureActivityItem
import com.example.magneticsecretactions.ui.GesturesScreen
import com.example.magneticsecretactions.ui.HomeScreen
import com.example.magneticsecretactions.ui.MagRingTheme
import com.example.magneticsecretactions.ui.SettingsScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private var hasCameraPermission by mutableStateOf(false)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val sensors = SensorRepository(this)
        val settings = AppSettings(this)

        setContent {
            MagRingTheme {
                MainAppScreen(
                    sensors = sensors,
                    settings = settings,
                    hasCameraPermission = hasCameraPermission,
                    onRequestCameraPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }
}

@Composable
private fun MainAppScreen(
    sensors: SensorRepository,
    settings: AppSettings,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val actionEngine = remember { ActionEngine(context) }
    val recognizer = remember { PatternRecognizer(patternTimeoutMs = settings.patternTimeoutMs) }
    var patternJob by remember { mutableStateOf<Job?>(null) }

    var currentTab by remember { mutableStateOf(AppTab.FEED) }
    var monitoring by remember { mutableStateOf(false) }

    val sample by sensors.samples.collectAsState()
    val detector = remember {
        MagneticDetector(settings.sensitivity, cooldownMs = 500L).apply {
            settings.baseline?.let { restoreCalibration(it, settings.threshold) }
        }
    }

    var detectorState by remember { mutableStateOf(detector.state) }
    var baseline by remember { mutableStateOf(settings.baseline) }
    var delta by remember { mutableStateOf(0f) }
    var samples by remember { mutableStateOf(emptyList<MagneticSample>()) }
    var selectedPattern by remember { mutableStateOf(MagneticPattern.SINGLE) }
    var settingsRefresh by remember { mutableStateOf(0) }

    val activities = remember { mutableStateListOf<GestureActivityItem>() }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    fun executeActionForChoice(choice: ActionChoice) {
        val action = when (choice) {
            ActionChoice.FLASHLIGHT -> {
                if (!hasCameraPermission) onRequestCameraPermission()
                MagneticAction.ToggleFlashlight
            }
            ActionChoice.NOTIFICATION -> MagneticAction.Notification(
                "Magnetic secret triggered", "Gesture action executed"
            )
            ActionChoice.OPEN_URL -> MagneticAction.OpenUrl(settings.url)
            ActionChoice.OPEN_APP -> MagneticAction.OpenApp(settings.appPackage)
            ActionChoice.MEDIA_PLAY_PAUSE -> MagneticAction.MediaPlayPause()
        }
        actionEngine.execute(action)
    }

    LaunchedEffect(sample) {
        sample?.let { s ->
            samples = (samples + s).takeLast(200)
            val event = detector.accept(s)
            detectorState = detector.state
            baseline = detector.baseline
            delta = detector.baseline?.let { b -> abs(s.magnitude - b) } ?: 0f

            // Real-time foreground pattern recognition
            event?.let { ev ->
                if (ev.state == MagneticState.MAGNET_REMOVED) {
                    recognizer.tap(ev.sample.timeMs)
                    patternJob?.cancel()
                    patternJob = coroutineScope.launch {
                        delay(settings.patternTimeoutMs + 50L)
                        recognizer.flush(ev.sample.timeMs + settings.patternTimeoutMs + 50L)?.let { pattern ->
                            val chosenAction = settings.actionFor(pattern.name)
                            executeActionForChoice(chosenAction)
                            activities.add(
                                0,
                                GestureActivityItem(
                                    id = System.currentTimeMillis(),
                                    patternName = pattern.label,
                                    actionName = chosenAction.name.replace("_", " "),
                                    timeLabel = timeFormat.format(Date()),
                                    deltaU = delta
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun toggleMonitoring() {
        if (monitoring) {
            sensors.stop()
            context.stopService(Intent(context, MonitoringService::class.java))
        } else {
            sensors.start()
            context.startForegroundService(Intent(context, MonitoringService::class.java))
        }
        monitoring = !monitoring
    }

    fun calibrate() {
        if (samples.isNotEmpty()) {
            detector.calibrate(samples)
            settings.baseline = detector.baseline
            settings.threshold = detector.thresholdDelta
            baseline = detector.baseline
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        Crossfade(
            targetState = currentTab,
            animationSpec = tween(280),
            label = "tab_crossfade"
        ) { tab ->
            when (tab) {
                AppTab.FEED -> HomeScreen(
                    monitoring = monitoring,
                    onToggleMonitoring = ::toggleMonitoring,
                    sample = sample,
                    detectorState = detectorState,
                    baseline = baseline,
                    delta = delta,
                    thresholdDelta = detector.thresholdDelta,
                    samplesCount = samples.size,
                    sensorInfo = sensors.inventory.firstOrNull { it.stringType?.contains("magnetic", ignoreCase = true) == true },
                    recentActivities = activities,
                    onCalibrate = ::calibrate
                )
                AppTab.GESTURES -> {
                    val currentAction = remember(selectedPattern, settingsRefresh) {
                        settings.actionFor(selectedPattern.name)
                    }
                    GesturesScreen(
                        selectedPattern = selectedPattern,
                        onPatternSelected = { selectedPattern = it },
                        currentAction = currentAction,
                        onActionSelected = { action ->
                            settings.setActionFor(selectedPattern.name, action)
                            settings.action = action
                            settingsRefresh++
                            if (action == ActionChoice.FLASHLIGHT && !hasCameraPermission) {
                                onRequestCameraPermission()
                            }
                        },
                        onTestAction = { action ->
                            executeActionForChoice(action)
                            activities.add(
                                0,
                                GestureActivityItem(
                                    id = System.currentTimeMillis(),
                                    patternName = "${selectedPattern.label} (Test)",
                                    actionName = action.name.replace("_", " "),
                                    timeLabel = timeFormat.format(Date()),
                                    deltaU = delta
                                )
                            )
                        },
                        samplesCount = samples.size,
                        onCalibrate = ::calibrate
                    )
                }
                AppTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    hasCameraPermission = hasCameraPermission,
                    onRequestCameraPermission = onRequestCameraPermission,
                    onSensitivityChanged = { detector.sensitivity = it }
                )
            }
        }

        // Floating Pill Bottom Navigation Bar
        FloatingBottomNavBar(
            currentTab = currentTab,
            onTabSelected = { currentTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(WindowInsets.navigationBars.asPaddingValues())
        )
    }
}
