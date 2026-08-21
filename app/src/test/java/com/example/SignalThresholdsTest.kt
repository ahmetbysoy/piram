package com.example

import com.example.domain.engine.strategy.SignalThresholds
import com.example.domain.model.SignalType
import org.junit.Assert.assertEquals
import org.junit.Test

class SignalThresholdsTest {

    @Test
    fun `varsayilan esikler`() {
        assertEquals(SignalType.STRONG_BUY, SignalThresholds.signalFor(0.5))
        assertEquals(SignalType.BUY, SignalThresholds.signalFor(0.2))
        assertEquals(SignalType.NEUTRAL, SignalThresholds.signalFor(0.0))
        assertEquals(SignalType.SELL, SignalThresholds.signalFor(-0.2))
        assertEquals(SignalType.STRONG_SELL, SignalThresholds.signalFor(-0.5))
    }

    @Test
    fun `ozel esikler`() {
        assertEquals(SignalType.BUY, SignalThresholds.signalFor(0.3, strong = 0.6, weak = 0.2))
        assertEquals(SignalType.STRONG_BUY, SignalThresholds.signalFor(0.7, strong = 0.6, weak = 0.2))
        assertEquals(SignalType.NEUTRAL, SignalThresholds.signalFor(0.1, strong = 0.6, weak = 0.2))
    }

    @Test
    fun `guven formulu ve sinir`() {
        assertEquals(0.55, SignalThresholds.confidenceFor(0.0), 1e-9)
        assertEquals(0.75, SignalThresholds.confidenceFor(0.5, base = 0.55, scale = 0.4), 1e-9)
        // 0.55 + 1.0*0.4 = 0.95 (cap 1.0'a takilmaz)
        assertEquals(0.95, SignalThresholds.confidenceFor(1.0, base = 0.55, scale = 0.4), 1e-9)
        // cap: 0.5 + 2.0*0.45 = 1.4 -> 1.0
        assertEquals(1.0, SignalThresholds.confidenceFor(2.0, base = 0.5, scale = 0.45), 1e-9)
    }
}
