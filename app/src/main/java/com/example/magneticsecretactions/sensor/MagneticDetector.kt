package com.example.magneticsecretactions.sensor

import kotlin.math.sqrt

data class MagneticSample(val x: Float, val y: Float, val z: Float, val timeMs: Long) {
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
}

enum class MagneticState { IDLE, MAGNET_APPROACHING, MAGNET_PRESENT, MAGNET_REMOVED }

data class DetectionEvent(val sample: MagneticSample, val state: MagneticState, val confidence: Float)

class MagneticDetector(
    sensitivity: Float = 1f,
    private val debounceMs: Long = 180L,
    private val cooldownMs: Long = 500L
) {
    var sensitivity: Float = sensitivity.coerceIn(0.25f, 4f)
    var baseline: Float? = null
        private set
    var thresholdDelta: Float = 30f
    var state: MagneticState = MagneticState.IDLE
        private set
    private var lastEventMs = Long.MIN_VALUE
    private var lastSampleMs = Long.MIN_VALUE

    fun reset() {
        baseline = null
        state = MagneticState.IDLE
        lastEventMs = Long.MIN_VALUE
        lastSampleMs = Long.MIN_VALUE
    }

    fun accept(sample: MagneticSample): DetectionEvent? {
        if (sample.timeMs <= lastSampleMs) return null
        lastSampleMs = sample.timeMs
        val currentBaseline = baseline
        if (currentBaseline == null) {
            baseline = sample.magnitude
            return null
        }
        val delta = kotlin.math.abs(sample.magnitude - currentBaseline)
        val activeDelta = thresholdDelta / sensitivity
        val nearBaseline = delta < activeDelta * 0.45f
        if (state == MagneticState.IDLE && nearBaseline) {
            baseline = currentBaseline * 0.98f + sample.magnitude * 0.02f
        }
        val now = sample.timeMs
        if (lastEventMs != Long.MIN_VALUE && now - lastEventMs < cooldownMs) return null
        when (state) {
            MagneticState.IDLE -> if (delta >= activeDelta) {
                state = MagneticState.MAGNET_APPROACHING
                return DetectionEvent(sample, state, (delta / activeDelta).coerceIn(0f, 1f))
            }
            MagneticState.MAGNET_APPROACHING -> if (delta >= activeDelta) {
                state = MagneticState.MAGNET_PRESENT
            } else if (nearBaseline) {
                state = MagneticState.IDLE
            }
            MagneticState.MAGNET_PRESENT -> if (nearBaseline) {
                state = MagneticState.MAGNET_REMOVED
                lastEventMs = now
                return DetectionEvent(sample, state, 1f)
            }
            MagneticState.MAGNET_REMOVED -> {
                state = MagneticState.IDLE
            }
        }
        return null
    }

    fun calibrate(samples: List<MagneticSample>) {
        require(samples.isNotEmpty()) { "At least one sample is required" }
        baseline = samples.map { it.magnitude }.average().toFloat()
        val deviations = samples.map { kotlin.math.abs(it.magnitude - baseline!!) }
        thresholdDelta = (deviations.sorted()[deviations.size * 9 / 10] * 4f).coerceIn(10f, 300f)
    }

    fun restoreCalibration(savedBaseline: Float, savedThreshold: Float) {
        baseline = savedBaseline
        thresholdDelta = savedThreshold.coerceIn(5f, 300f)
        state = MagneticState.IDLE
    }
}
