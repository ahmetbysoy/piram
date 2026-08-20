package com.example

import com.example.domain.engine.WindowLedger
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowLedgerTest {

    private fun order(id: String, side: OrderSide, price: Double, volume: Double, ts: Long) =
        Order(id, side, volume, price, ts)

    @Test
    fun `pencere icinde alis satis notional toplar`() {
        val ledger = WindowLedger()
        val t0 = 1_000_000L
        ledger.ingest(order("1", OrderSide.BUY, 100.0, 1.0, t0))          // 100 USDT
        ledger.ingest(order("2", OrderSide.SELL, 100.0, 2.0, t0 + 1000))  // 200 USDT

        val sum = ledger.sumWindow(60, t0 + 2000)
        assertEquals(100.0, sum.buyNotional, 1e-9)
        assertEquals(200.0, sum.sellNotional, 1e-9)
        assertEquals(2, sum.count)
        assertEquals(-100.0, sum.net, 1e-9)
    }

    @Test
    fun `oturum toplami her seyi tutar`() {
        val ledger = WindowLedger()
        val t0 = 1_000_000L
        ledger.ingest(order("1", OrderSide.BUY, 10.0, 1.0, t0))
        ledger.ingest(order("2", OrderSide.BUY, 10.0, 1.0, t0 + 120_000))

        val s = ledger.sessionSum()
        assertEquals(20.0, s.buyNotional, 1e-9)
        assertEquals(2, s.count)
    }

    @Test
    fun `eski dilimleri budar`() {
        val ledger = WindowLedger(keepSeconds = 60)
        val t0 = 1_000_000L
        ledger.ingest(order("1", OrderSide.BUY, 10.0, 1.0, t0))

        ledger.pruneKeep(t0 + 120_000)
        assertEquals(0, ledger.sliceCount())
        assertEquals(0, ledger.sumWindow(3600, t0 + 120_000).count)
    }

    @Test
    fun `pencere disinda kalanlar sayilmaz`() {
        val ledger = WindowLedger()
        val t0 = 1_000_000L
        ledger.ingest(order("1", OrderSide.BUY, 10.0, 1.0, t0))
        ledger.ingest(order("2", OrderSide.BUY, 10.0, 1.0, t0 + 90_000)) // 90 sn sonra

        // 60 sn'lik pencere: sadece ikinci (90sn) trade sayılır
        val sum = ledger.sumWindow(60, t0 + 95_000)
        assertEquals(10.0, sum.buyNotional, 1e-9)
        assertEquals(1, sum.count)
    }

    @Test
    fun `net ve total hesaplari`() {
        val sum = WindowLedger.WindowSum(30.0, 10.0, 4)
        assertEquals(20.0, sum.net, 1e-9)
        assertEquals(40.0, sum.total, 1e-9)
    }
}
