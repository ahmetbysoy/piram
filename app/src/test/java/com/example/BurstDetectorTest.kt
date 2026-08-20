package com.example

import com.example.domain.engine.burst.BurstDetector
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BurstDetectorTest {

    @Test
    fun testBurstDetectionOnRapidOrderFlow() {
        val detector = BurstDetector(windowMs = 1500L, minOrderCount = 3, minVolumeSpike = 0.2)
        val now = System.currentTimeMillis()

        detector.processOrder(Order("1", OrderSide.BUY, 0.5, 65000.0, now))
        detector.processOrder(Order("2", OrderSide.BUY, 0.8, 65005.0, now + 10))
        val cluster = detector.processOrder(Order("3", OrderSide.BUY, 1.2, 65010.0, now + 20))

        assertNotNull(cluster)
        assertTrue(cluster!!.orderCount >= 3)
        assertTrue(cluster.totalVolume >= 2.5)
    }
}
