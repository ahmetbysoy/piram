package com.example.domain.engine.burst

import com.example.core.util.MathUtils
import com.example.domain.model.BurstCluster
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import java.util.concurrent.ConcurrentLinkedDeque

class BurstDetector(
    private val windowMs: Long = 1500L,
    private val minOrderCount: Int = 4,
    private val minVolumeSpike: Double = 0.5
) {
    private val recentOrders = ConcurrentLinkedDeque<Order>()
    private val activeBursts = ConcurrentLinkedDeque<BurstCluster>()

    fun processOrder(order: Order): BurstCluster? {
        val now = order.timestamp
        recentOrders.addLast(order)
        cleanupOld(now)

        // Filter recent orders within window with the same side
        val windowOrders = recentOrders.filter { it.timestamp >= now - windowMs }
        val sameSideOrders = windowOrders.filter { it.side == order.side }

        if (sameSideOrders.size >= minOrderCount) {
            val totalVol = sameSideOrders.sumOf { it.volume }
            val totalVal = sameSideOrders.sumOf { it.value }
            val avgPrice = if (totalVol > 0) sameSideOrders.sumOf { it.price * it.volume } / totalVol else order.price
            val durationSec = MathUtils.clamp((now - sameSideOrders.first().timestamp) / 1000.0, 0.05, 5.0)
            val velocity = totalVol / durationSec

            // Calculate intensity score
            val intensity = (sameSideOrders.size * 1.5) + (velocity / (minVolumeSpike + 0.01))

            if (totalVol >= minVolumeSpike) {
                val burst = BurstCluster(
                    id = MathUtils.generateUniqueId(),
                    side = order.side,
                    totalValue = totalVal,
                    totalVolume = totalVol,
                    orderCount = sameSideOrders.size,
                    startTime = sameSideOrders.first().timestamp,
                    endTime = now,
                    avgPrice = avgPrice,
                    intensityScore = intensity,
                    exchange = order.exchange
                )
                activeBursts.addLast(burst)
                while (activeBursts.size > 15) {
                    activeBursts.pollFirst()
                }
                return burst
            }
        }
        return null
    }

    private fun cleanupOld(currentTime: Long) {
        val cutoff = currentTime - (windowMs * 3)
        while (recentOrders.isNotEmpty() && recentOrders.peekFirst()?.timestamp ?: Long.MAX_VALUE < cutoff) {
            recentOrders.pollFirst()
        }
    }

    fun getActiveBursts(): List<BurstCluster> {
        val now = System.currentTimeMillis()
        return activeBursts.filter { now - it.endTime <= 10_000L }.toList()
    }

    fun clear() {
        recentOrders.clear()
        activeBursts.clear()
    }
}
