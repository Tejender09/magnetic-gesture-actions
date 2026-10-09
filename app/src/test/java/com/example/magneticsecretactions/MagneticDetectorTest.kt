package com.example.magneticsecretactions

import com.example.magneticsecretactions.sensor.MagneticDetector
import com.example.magneticsecretactions.sensor.MagneticSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MagneticDetectorTest {
    @Test fun magnitudeIsEuclideanNorm() {
        assertEquals(13f, MagneticSample(3f, 4f, 12f, 1).magnitude, 0.001f)
    }

    @Test fun eventRequiresApproachAndReturn() {
        val detector = MagneticDetector(cooldownMs = 0)
        detector.accept(MagneticSample(0f, 0f, 50f, 0))
        detector.thresholdDelta = 10f
        detector.accept(MagneticSample(0f, 0f, 80f, 100))
        detector.accept(MagneticSample(0f, 0f, 80f, 300))
        assertNotNull(detector.accept(MagneticSample(0f, 0f, 50f, 500)))
    }
}
