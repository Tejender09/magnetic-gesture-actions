package com.example.magneticsecretactions.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SensorInfo(
    val name: String, val vendor: String, val type: Int, val stringType: String?,
    val version: Int, val range: Float, val resolution: Float, val power: Float, val minDelayUs: Int
)


class SensorRepository(context: Context) : SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val _samples = MutableStateFlow<MagneticSample?>(null)
    val samples: StateFlow<MagneticSample?> = _samples
    val inventory: List<SensorInfo> = manager.getSensorList(Sensor.TYPE_ALL).map {
        SensorInfo(it.name, it.vendor, it.type, it.stringType, it.version, it.maximumRange, it.resolution, it.power, it.minDelay)
    }
    val magnetometer: Sensor? = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    fun start(): Boolean = magnetometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } ?: false
    fun stop() = manager.unregisterListener(this)
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD && event.values.size >= 3) {
            _samples.value = MagneticSample(event.values[0], event.values[1], event.values[2], System.currentTimeMillis())
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

