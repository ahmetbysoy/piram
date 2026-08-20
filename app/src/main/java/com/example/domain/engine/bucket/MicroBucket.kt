package com.example.domain.engine.bucket

import com.example.core.util.MathUtils
import com.example.domain.model.LayerAggregate
import com.example.domain.model.Order
import com.example.domain.model.OrderSide

/**
 * Tek notional (USDT) katmanı. Sipariş **adeti** değil, `Order.value` (fiyat × adet) toplanır.
 */
class MicroBucket(
    val index: Int,
    val minNotional: Double,
    val maxNotional: Double,
    val label: String
) {
    var currentNotional: Double = 0.0
        private set
    var buyNotional: Double = 0.0
        private set
    var sellNotional: Double = 0.0
        private set
    var orderCount: Int = 0
        private set
    var displayNotional: Double = 0.0
        private set
    var lastUpdated: Long = System.currentTimeMillis()
        private set

    fun addOrder(order: Order) {
        val value = order.value
        currentNotional += value
        if (order.side == OrderSide.BUY) {
            buyNotional += value
        } else {
            sellNotional += value
        }
        orderCount++
        lastUpdated = System.currentTimeMillis()
    }

    fun decay(decayRate: Float, dtSeconds: Float) {
        currentNotional = MathUtils.exponentialDecayDouble(currentNotional, 0.0, decayRate.toDouble(), dtSeconds.toDouble())
        buyNotional = MathUtils.exponentialDecayDouble(buyNotional, 0.0, decayRate.toDouble(), dtSeconds.toDouble())
        sellNotional = MathUtils.exponentialDecayDouble(sellNotional, 0.0, decayRate.toDouble(), dtSeconds.toDouble())
    }

    fun updateDisplay(smoothingFactor: Float = 0.18f) {
        displayNotional = MathUtils.lerpDouble(displayNotional, currentNotional, smoothingFactor.toDouble())
    }

    fun toAggregate(isWhaleTier: Boolean): LayerAggregate {
        val total = buyNotional + sellNotional
        val ratio = if (total > 0) (buyNotional / total).toFloat() else 0.5f
        return LayerAggregate(
            layerIndex = index,
            minNotional = minNotional,
            maxNotional = maxNotional,
            notional = currentNotional,
            displayNotional = displayNotional,
            buyNotional = buyNotional,
            sellNotional = sellNotional,
            orderCount = orderCount,
            buyRatio = ratio,
            isWhaleTier = isWhaleTier,
            label = label
        )
    }

    fun reset() {
        currentNotional = 0.0
        buyNotional = 0.0
        sellNotional = 0.0
        orderCount = 0
        displayNotional = 0.0
    }
}
