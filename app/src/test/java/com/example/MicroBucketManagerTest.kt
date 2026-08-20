package com.example.domain

import com.example.domain.engine.bucket.MicroBucketManager
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroBucketManagerTest {

    @Test
    fun testLogarithmicNotionalDistribution() {
        val manager = MicroBucketManager(numLayers = 8, minNotional = 100.0, maxNotional = 1_000_000.0)

        // 0.0005 BTC @ 60000 = 30 USDT → en alt katman (toz)
        val smallOrder = Order(
            id = "1",
            side = OrderSide.BUY,
            volume = 0.0005,
            price = 60000.0,
            timestamp = System.currentTimeMillis()
        )
        val processedSmall = manager.processOrder(smallOrder)
        assertEquals(0, processedSmall.layerIndex)
        assertFalse(processedSmall.isWhale)

        // 30 BTC @ 60000 = 1.8M USDT → en üst katman (whale)
        val largeOrder = Order(
            id = "2",
            side = OrderSide.SELL,
            volume = 30.0,
            price = 60000.0,
            timestamp = System.currentTimeMillis()
        )
        val processedLarge = manager.processOrder(largeOrder)
        assertEquals(7, processedLarge.layerIndex)
        assertTrue(processedLarge.isWhale)
    }

    @Test
    fun testNotionalDecayAndAggregation() {
        val manager = MicroBucketManager(numLayers = 8, minNotional = 100.0, maxNotional = 1_000_000.0)

        // 5 BTC @ 65000 = 325K USDT
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
        assertTrue((targetLayer?.notional ?: 0.0) > 0.0)

        // Apply decay
        manager.decayAll(decayRate = 0.5f, dtSeconds = 1.0f)
        val afterDecay = manager.getAggregatedLayers()
        val targetLayerAfter = afterDecay.find { it.layerIndex == processed.layerIndex }
        assertTrue((targetLayerAfter?.notional ?: 0.0) < (targetLayer?.notional ?: 0.0))
    }
}
