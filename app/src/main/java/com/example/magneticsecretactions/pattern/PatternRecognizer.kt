package com.example.magneticsecretactions.pattern

enum class MagneticPattern(val label: String, val taps: Int) {
    SINGLE("Single tap", 1), DOUBLE("Double tap", 2), TRIPLE("Triple tap", 3)
}

class PatternRecognizer(
    private val interTapMinMs: Long = 150,
    private val patternTimeoutMs: Long = PATTERN_TIMEOUT_MS
) {
    private val taps = mutableListOf<Long>()

    fun tap(atMs: Long): MagneticPattern? {
        val previous = taps.lastOrNull()
        if (previous != null && atMs - previous < interTapMinMs) return null
        if (previous != null && atMs - previous > patternTimeoutMs) taps.clear()
        taps += atMs
        return null
    }

    fun flush(atMs: Long): MagneticPattern? {
        if (taps.isEmpty() || atMs - taps.last() < patternTimeoutMs) return null
        val result = when (taps.size) {
            1 -> MagneticPattern.SINGLE
            2 -> MagneticPattern.DOUBLE
            3 -> MagneticPattern.TRIPLE
            else -> null
        }
        taps.clear()
        return result
    }

    fun reset() {
        taps.clear()
    }

    companion object {
        const val PATTERN_TIMEOUT_MS = 700L
    }
}
