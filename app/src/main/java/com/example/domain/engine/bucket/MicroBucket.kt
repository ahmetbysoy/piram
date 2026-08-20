package com.example.domain.engine.bucket

import com.example.core.util.MathUtils
import com.example.domain.model.LayerAggregate
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import kotlin.math.max

class MicroBucket(
    val index: Int,
    val minVolume: Double,
    val maxVolume: Double,
    val label: String
) {
    var currentVolume: Double = 0.0
        private set
    var buyVolume: Double = 0.0
        private set
    var sellVolume: Double = 0.0
        private set
    var orderCount: Int = 0
        private set
    var displayVolume: Double = 0.0
        private set
    var lastUpdated: Long = System.currentTimeMillis()
        private set

    fun addOrder(order: Order) {
        currentVolume += order.volume
        if (order.side == OrderSide.BUY) {
            buyVolume += order.volume
        } else {
            sellVolume += order.volume
        }
        orderCount++
        lastUpdated = System.currentTimeMillis()
    }

    fun decay(decayRate: Float, dtSeconds: Float) {
        currentVolume = MathUtils.exponentialDecayDouble(currentVolume, 0.0, decayRate.toDouble(), dtSeconds.toDouble())
        buyVolume = MathUtils.exponentialDecayDouble(buyVolume, 0.0, decayRate.toDouble(), dtSeconds.toDouble())
        sellVolume = MathUtils.exponentialDecayDouble(sellVolume, 0.0, decayRate.toDouble(), dtSeconds.toDouble())
    }

    fun updateDisplay(smoothingFactor: Float = 0.18f) {
        displayVolume = MathUtils.lerpDouble(displayVolume, currentVolume, smoothingFactor.toDouble())
    }

    fun toAggregate(isWhaleTier: Boolean): LayerAggregate {
        val total = buyVolume + sellVolume
        val ratio = if (total > 0) (buyVolume / total).toFloat() else 0.5f
        return LayerAggregate(
            layerIndex = index,
            minVolume = minVolume,
            maxVolume = maxVolume,
            currentVolume = currentVolume,
            displayVolume = displayVolume,
            buyVolume = buyVolume,
            sellVolume = sellVolume,
            orderCount = orderCount,
            buyRatio = ratio,
            isWhaleTier = isWhaleTier,
            label = label
        )
    }

    fun reset() {
        currentVolume = 0.0
        buyVolume = 0.0
        sellVolume = 0.0
        orderCount = 0
        displayVolume = 0.0
    }
}
