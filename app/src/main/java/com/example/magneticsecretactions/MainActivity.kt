package com.example.magneticsecretactions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.magneticsecretactions.monitor.MonitoringService
import com.example.magneticsecretactions.pattern.MagneticPattern
import com.example.magneticsecretactions.sensor.MagneticDetector
import com.example.magneticsecretactions.sensor.MagneticSample
import com.example.magneticsecretactions.sensor.SensorRepository
import com.example.magneticsecretactions.settings.ActionChoice
import com.example.magneticsecretactions.settings.AppSettings
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sensors = SensorRepository(this)
        val settings = AppSettings(this)
        setContent {
            MaterialTheme {
                ControlScreen(
                    sensors = sensors,
                    settings = settings,
                    requestCameraPermission = {
                        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlScreen(
    sensors: SensorRepository,
    settings: AppSettings,
    requestCameraPermission: () -> Unit
) {
    val context = LocalContext.current
    val sample by sensors.samples.collectAsState()
    val detector = remember { MagneticDetector(settings.sensitivity, cooldownMs = 500L) }
    var monitoring by remember { mutableStateOf(false) }
    var detectorState by remember { mutableStateOf(detector.state) }
    var baseline by remember { mutableStateOf(settings.baseline) }
    var delta by remember { mutableStateOf(0f) }
    var samples by remember { mutableStateOf(emptyList<MagneticSample>()) }
    var selectedPattern by remember { mutableStateOf(MagneticPattern.SINGLE) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(sample) {
        sample?.let {
            samples = (samples + it).takeLast(200)
            detector.accept(it)
            detectorState = detector.state
            baseline = detector.baseline
            delta = detector.baseline?.let { value -> abs(it.magnitude - value) } ?: 0f
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Magnetic Secret Actions", style = MaterialTheme.typography.headlineSmall)
        Text("Magnetic finger-ring controller", style = MaterialTheme.typography.bodyMedium)

        Section("Live sensor") {
            Text("Sensor: ${sensors.magnetometer?.name ?: "Magnetometer unavailable"}")
            Text("State: ${detectorState.name}")
            Text("Field: ${"%.1f".format(sample?.magnitude ?: 0f)} µT")
            Text("Baseline: ${"%.1f".format(baseline ?: 0f)} µT")
            Text("Change: ${"%.1f".format(delta)} µT / threshold ${"%.1f".format(detector.thresholdDelta)} µT")
            Text("Hall sensor candidates: ${sensors.hallCandidates.size}")
        }

        Section("Monitoring") {
            Button(onClick = {
                if (monitoring) {
                    sensors.stop()
                    context.stopService(Intent(context, MonitoringService::class.java))
                } else {
                    sensors.start()
                    context.startForegroundService(Intent(context, MonitoringService::class.java))
                }
                monitoring = !monitoring
            }) {
                Text(if (monitoring) "Stop monitoring" else "Start monitoring")
            }
            Text(if (monitoring) "Foreground monitoring is active." else "Monitoring is stopped.")
        }

        Section("Calibration") {
            Text("Keep the ring away from the phone before calibrating.")
            Button(
                onClick = {
                    if (samples.isNotEmpty()) {
                        detector.calibrate(samples)
                        settings.baseline = detector.baseline
                        settings.threshold = detector.thresholdDelta
                        baseline = detector.baseline
                    }
                },
                enabled = samples.isNotEmpty()
            ) {
                Text("Calibrate (${samples.size} samples)")
            }
        }

        Section("Pattern actions") {
            Text("Choose a gesture", style = MaterialTheme.typography.bodySmall)
            MagneticPattern.values().forEach { pattern ->
                Button(
                    onClick = { selectedPattern = pattern },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (pattern == selectedPattern) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = if (pattern == selectedPattern) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                ) {
                    Text(if (pattern == selectedPattern) "Selected • ${pattern.label}" else pattern.label)
                }
            }
            val selectedAction = remember(selectedPattern, refresh) {
                settings.actionFor(selectedPattern.name)
            }
            Text(
                "${selectedPattern.label} action: ${selectedAction.displayName()}",
                style = MaterialTheme.typography.titleSmall
            )
            Text("Choose what this gesture should do", style = MaterialTheme.typography.bodySmall)
            ActionChoice.values().forEach { action ->
                Button(
                    onClick = {
                        settings.setActionFor(selectedPattern.name, action)
                        settings.action = action
                        refresh++
                        if (action == ActionChoice.FLASHLIGHT) requestCameraPermission()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (action == selectedAction) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (action == selectedAction) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) {
                    Text(if (action == selectedAction) "Selected • ${action.displayName()}" else action.displayName())
                }
            }
        }

        SettingsSection(settings, onSensitivityChanged = { detector.sensitivity = it })
        Text("Configuration is saved automatically.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsSection(settings: AppSettings, onSensitivityChanged: (Float) -> Unit) {
    var sensitivity by remember { mutableStateOf(settings.sensitivity) }
    var timeout by remember { mutableStateOf(settings.patternTimeoutMs.toFloat()) }
    var url by remember { mutableStateOf(settings.url) }
    var appPackage by remember { mutableStateOf(settings.appPackage) }
    var appPickerOpen by remember { mutableStateOf(false) }
    val packageManager = LocalContext.current.packageManager
    val apps = remember(packageManager) { loadLaunchableApps(packageManager) }

    Section("Settings") {
        Text("Sensitivity: ${"%.2f".format(sensitivity)}")
        Slider(
            value = sensitivity,
            onValueChange = {
                sensitivity = it
                settings.sensitivity = it
                onSensitivityChanged(it)
            },
            valueRange = .25f..4f
        )
        Text("Pattern timeout: ${timeout.toInt()} ms")
        Slider(
            value = timeout,
            onValueChange = {
                timeout = it
                settings.patternTimeoutMs = it.toLong()
            },
            valueRange = 400f..1500f
        )
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                settings.url = it
            },
            label = { Text("URL action target") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = appPackage,
            onValueChange = {
                appPackage = it
                settings.appPackage = it
            },
            label = { Text("App package action target") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(onClick = { appPickerOpen = true }) {
            Text("Choose installed app")
        }
        if (appPackage.isNotBlank()) {
            Text(
                "Selected: ${apps.firstOrNull { it.packageName == appPackage }?.label ?: appPackage}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    if (appPickerOpen) {
        AlertDialog(
            onDismissRequest = { appPickerOpen = false },
            title = { Text("Choose an app") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    apps.forEach { app ->
                        TextButton(
                            onClick = {
                                appPackage = app.packageName
                                settings.appPackage = app.packageName
                                appPickerOpen = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${app.label}\n${app.packageName}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { appPickerOpen = false }) { Text("Cancel") }
            }
        )
    }
}

private data class LaunchableApp(val label: String, val packageName: String)

private fun loadLaunchableApps(packageManager: PackageManager): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .map { info ->
            LaunchableApp(
                label = info.loadLabel(packageManager).toString(),
                packageName = info.activityInfo.packageName
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun ActionChoice.displayName(): String = when (this) {
    ActionChoice.NOTIFICATION -> "Notification"
    ActionChoice.FLASHLIGHT -> "Flashlight"
    ActionChoice.OPEN_URL -> "Open URL"
    ActionChoice.OPEN_APP -> "Open app"
    ActionChoice.MEDIA_PLAY_PAUSE -> "Media"
}
