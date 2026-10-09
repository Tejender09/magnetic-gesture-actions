package com.example.magneticsecretactions.action

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.media.AudioManager
import android.view.KeyEvent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.example.magneticsecretactions.pattern.MagneticPattern

sealed interface MagneticAction {
    data class OpenUrl(val url: String) : MagneticAction
    data object ToggleFlashlight : MagneticAction
    data class OpenApp(val packageName: String) : MagneticAction
    data class MediaPlayPause(val unused: Unit = Unit) : MagneticAction
    data class Notification(val title: String, val message: String) : MagneticAction
}

class ActionEngine(private val context: Context) {
    private var flashlightOn = false

    fun execute(action: MagneticAction): Result<Unit> = runCatching {
        when (action) {
            is MagneticAction.OpenUrl -> context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(action.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            MagneticAction.ToggleFlashlight -> toggleFlashlight()
            is MagneticAction.OpenApp -> openApp(action.packageName)
            is MagneticAction.MediaPlayPause -> mediaPlayPause()
            is MagneticAction.Notification -> notify(action)
        }
    }

    fun defaultActionFor(pattern: MagneticPattern): MagneticAction =
        MagneticAction.Notification("Magnetic action", pattern.label)

    private fun toggleFlashlight() {
        check(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            "Camera permission is required to control the flashlight"
        }

        val manager = context.getSystemService(CameraManager::class.java)
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: error("No flashlight-capable camera is available")
        flashlightOn = !flashlightOn
        manager.setTorchMode(cameraId, flashlightOn)
    }

    private fun openApp(packageName: String) {
        require(packageName.isNotBlank()) { "No app package configured" }
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: error("No launchable app found for $packageName")
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun mediaPlayPause() {
        val audio = context.getSystemService(AudioManager::class.java)
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
    }

    private fun notify(action: MagneticAction.Notification) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel("magnetic_actions", "Magnetic actions", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        manager.notify(
            action.hashCode(),
            NotificationCompat.Builder(context, "magnetic_actions")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(action.title)
                .setContentText(action.message)
                .setAutoCancel(true)
                .build()
        )
    }
}
