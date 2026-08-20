package com.example.domain

import com.example.domain.engine.bucket.MicroBucketManager
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroBucketManagerTest {

    @Test
    fun testLogarithmicLayerDistribution() {
        val manager = MicroBucketManager(numLayers = 8, minVolume = 0.001, maxVolume = 25.0)

        val smallOrder = Order(
            id = "1",
            side = OrderSide.BUY,
            volume = 0.0005,
            price = 60000.0,
            timestamp = System.currentTimeMillis()
        )
        val processedSmall = manager.processOrder(smallOrder)
        assertEquals(0, processedSmall.layerIndex)
        assertEquals(false, processedSmall.isWhale)

        val largeOrder = Order(
            id = "2",
            side = OrderSide.SELL,
            volume = 30.0,
            price = 60000.0,
            timestamp = System.currentTimeMillis()
        )
        val processedLarge = manager.processOrder(largeOrder)
        assertEquals(7, processedLarge.layerIndex)
        assertEquals(true, processedLarge.isWhale)
    }

    @Test
    fun testVolumeDecayAndAggregation() {
        val manager = MicroBucketManager(numLayers = 8, minVolume = 0.001, maxVolume = 25.0)

        val order = Order(
            id = "10",
            side = OrderSide.BUY,
            volume = 5.0,
            price = 65000.0,
            timestamp = System.currentTimeMillis()
        )
        val processed = manager.processOrder(order)

        val beforeDecay = manager.getAggregatedLayers()
        val targetLayer = beforeDecay.find { it.layerIndex == processed.layerIndex }
        assertTrue((targetLayer?.currentVolume ?: 0.0) > 0.0)

        // Apply decay
        manager.decayAll(decayRate = 0.5f, dtSeconds = 1.0f)
        val afterDecay = manager.getAggregatedLayers()
        val targetLayerAfter = afterDecay.find { it.layerIndex == processed.layerIndex }
        assertTrue((targetLayerAfter?.currentVolume ?: 0.0) < (targetLayer?.currentVolume ?: 0.0))
    }
}
