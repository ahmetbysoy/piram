package com.example.domain.engine

import com.example.domain.model.OrderSide

/**
 * Rolling 60-second buy/sell volume window used for the "1M FLOW" metric.
 *
 * Keeps an in-memory deque of (timestamp, side, volume) samples, pruned on every
 * access, so the buy/sell split reflects only the most recent minute of activity.
 * The clock is injectable for deterministic unit tests.
 */
class OneMinuteVolumeTracker(
    private val clock: () -> Long = System::currentTimeMillis
) {

    private data class Sample(
        val timestamp: Long,
        val side: OrderSide,
        val volume: Double
    )

    private val windowMs: Long = 60_000L
    private val samples = ArrayDeque<Sample>()

    @Synchronized
    fun record(side: OrderSide, volume: Double, timestamp: Long = clock()) {
        if (volume <= 0) return
        samples.addLast(Sample(timestamp, side, volume))
        prune(timestamp)
    }

    @Synchronized
    fun buyVolume(now: Long = clock()): Double {
        prune(now)
        return samples.sumOf { if (it.side == OrderSide.BUY) it.volume else 0.0 }
    }

    @Synchronized
    fun sellVolume(now: Long = clock()): Double {
        prune(now)
        return samples.sumOf { if (it.side == OrderSide.SELL) it.volume else 0.0 }
    }

    @Synchronized
    fun clear() {
        samples.clear()
    }

    private fun prune(now: Long) {
        val cutoff = now - windowMs
        while (samples.isNotEmpty() && samples.first().timestamp < cutoff) {
            samples.removeFirst()
        }
    }
}
