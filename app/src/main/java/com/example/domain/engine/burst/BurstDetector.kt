package com.example.domain.engine.burst

import com.example.core.util.MathUtils
import com.example.domain.model.BurstCluster
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Kısa salvo / burst algılayıcı.
 *
 * Performans notu (kod analizi P1): önceki sürüm her `processOrder` çağrısında
 * tüm deque'yi iki kez `filter` ediyordu (O(n)). Artık alış/satış için ayrı
 * iki pencere tutulur ve kayan toplamlar (running totals) kullanılır —
 * her trade O(1) amortized, yüksek TPS'de CPU yakmaz.
 */
class BurstDetector(
    private val windowMs: Long = 1500L,
    private val minOrderCount: Int = 4,
    private val minVolumeSpike: Double = 0.5
) {
    /** Aynı yöndeki emirlerin zaman sıralı penceresi + kayan toplamlar. */
    private class SideWindow {
        val orders = ArrayDeque<Order>()
        var totalVol = 0.0
        var totalVal = 0.0
        var priceVolSum = 0.0

        fun add(o: Order) {
            orders.addLast(o)
            totalVol += o.volume
            totalVal += o.value
            priceVolSum += o.price * o.volume
        }

        fun evictOlderThan(cutoff: Long) {
            while (orders.isNotEmpty() && orders.first().timestamp < cutoff) {
                val o = orders.removeFirst()
                totalVol -= o.volume
                totalVal -= o.value
                priceVolSum -= o.price * o.volume
            }
        }

        fun clear() {
            orders.clear()
            totalVol = 0.0
            totalVal = 0.0
            priceVolSum = 0.0
        }
    }

    private val buyWindow = SideWindow()
    private val sellWindow = SideWindow()
    private val activeBursts = ConcurrentLinkedDeque<BurstCluster>()

    @Synchronized
    fun processOrder(order: Order): BurstCluster? {
        val now = order.timestamp
        val cutoff = now - windowMs

        val w = if (order.side == OrderSide.BUY) buyWindow else sellWindow
        w.add(order)
        w.evictOlderThan(cutoff)

        if (w.orders.size >= minOrderCount && w.totalVol >= minVolumeSpike) {
            val first = w.orders.first()
            val avgPrice = if (w.totalVol > 0) w.priceVolSum / w.totalVol else order.price
            val durationSec = MathUtils.clamp((now - first.timestamp) / 1000.0, 0.05, 5.0)
            val velocity = w.totalVol / durationSec
            val intensity = (w.orders.size * 1.5) + (velocity / (minVolumeSpike + 0.01))

            val burst = BurstCluster(
                id = MathUtils.generateUniqueId(),
                side = order.side,
                totalValue = w.totalVal,
                totalVolume = w.totalVol,
                orderCount = w.orders.size,
                startTime = first.timestamp,
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
        return null
    }

    fun getActiveBursts(): List<BurstCluster> {
        val now = System.currentTimeMillis()
        return activeBursts.filter { now - it.endTime <= 10_000L }.toList()
    }

    @Synchronized
    fun clear() {
        buyWindow.clear()
        sellWindow.clear()
        activeBursts.clear()
    }
}
