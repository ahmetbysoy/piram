package com.example

import com.example.domain.engine.OneMinuteVolumeTracker
import com.example.domain.model.OrderSide
import org.junit.Assert.assertEquals
import org.junit.Test

class OneMinuteVolumeTrackerTest {

    @Test
    fun `sums buy and sell volumes within the window`() {
        var now = 1_000_000L
        val tracker = OneMinuteVolumeTracker(clock = { now })
        tracker.record(OrderSide.BUY, 1.0, now)
        tracker.record(OrderSide.BUY, 2.0, now)
        tracker.record(OrderSide.SELL, 5.0, now)

        assertEquals(3.0, tracker.buyVolume(now), 1e-9)
        assertEquals(5.0, tracker.sellVolume(now), 1e-9)
    }

    @Test
    fun `expires samples older than 60 seconds`() {
        var now = 0L
        val tracker = OneMinuteVolumeTracker(clock = { now })
        tracker.record(OrderSide.BUY, 4.0, 0L)

        now = 61_000L
        assertEquals(0.0, tracker.buyVolume(now), 1e-9)
    }

    @Test
    fun `keeps samples just inside the window boundary`() {
        var now = 0L
        val tracker = OneMinuteVolumeTracker(clock = { now })
        tracker.record(OrderSide.SELL, 7.0, 0L)

        now = 59_999L
        assertEquals(7.0, tracker.sellVolume(now), 1e-9)
    }

    @Test
    fun `clear resets all state`() {
        val tracker = OneMinuteVolumeTracker(clock = { 0L })
        tracker.record(OrderSide.SELL, 9.0, 0L)
        tracker.record(OrderSide.BUY, 2.0, 0L)
        tracker.clear()
        assertEquals(0.0, tracker.buyVolume(), 1e-9)
        assertEquals(0.0, tracker.sellVolume(), 1e-9)
    }
}
