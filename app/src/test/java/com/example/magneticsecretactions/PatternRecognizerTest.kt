package com.example.magneticsecretactions

import com.example.magneticsecretactions.pattern.MagneticPattern
import com.example.magneticsecretactions.pattern.PatternRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatternRecognizerTest {
    @Test fun recognizesDoubleTapAfterTimeout() {
        val recognizer = PatternRecognizer()
        assertNull(recognizer.tap(100))
        assertNull(recognizer.tap(500))
        assertEquals(MagneticPattern.DOUBLE, recognizer.flush(1_300))
    }

    @Test fun rejectsTooFastSecondTap() {
        val recognizer = PatternRecognizer()
        recognizer.tap(100)
        recognizer.tap(200)
        assertEquals(MagneticPattern.SINGLE, recognizer.flush(900))
    }
}
