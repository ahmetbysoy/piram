package com.example.domain.engine

import com.example.domain.model.Order
import com.example.domain.model.OrderSide

/**
 * 1 saniyelik dilimli, notional (USDT) pencere defteri (piramit'ten port).
 * Katman bilmez; zaman dilimi + alış/satış notional + adet biriktirir.
 * Thread-safe (@Synchronized): trade stream (IO) ve motor loop (Default) aynı anda erişir.
 */
class WindowLedger(private val keepSeconds: Long = 3600L) {

    data class WindowSum(
        val buyNotional: Double,
        val sellNotional: Double,
        val count: Int
    ) {
        val net: Double get() = buyNotional - sellNotional
        val total: Double get() = buyNotional + sellNotional
    }

    private class Slice(val sec: Long) {
        var buyNotional: Double = 0.0
        var sellNotional: Double = 0.0
        var count: Int = 0
    }

    private val slices = ArrayDeque<Slice>()
    private var sessionBuy = 0.0
    private var sessionSell = 0.0
    private var sessionCount = 0
    private var lastPrice = 0.0
    private var sessionOpenPrice = 0.0

    @Synchronized
    fun reset() {
        slices.clear()
        sessionBuy = 0.0
        sessionSell = 0.0
        sessionCount = 0
        lastPrice = 0.0
        sessionOpenPrice = 0.0
    }

    @Synchronized
    fun ingest(order: Order) {
        val v = order.value
        if (!v.isFinite() || v <= 0) return
        val slice = sliceFor(order.timestamp / 1000)
        if (order.side == OrderSide.BUY) {
            slice.buyNotional += v
            sessionBuy += v
        } else {
            slice.sellNotional += v
            sessionSell += v
        }
        slice.count++
        sessionCount++
        lastPrice = order.price
        if (sessionOpenPrice == 0.0) sessionOpenPrice = order.price
    }

    @Synchronized
    fun pruneKeep(nowMs: Long) {
        val oldest = nowMs / 1000 - keepSeconds
        while (slices.isNotEmpty() && slices.first().sec < oldest) {
            slices.removeFirst()
        }
    }

    @Synchronized
    fun sumWindow(windowSec: Long, nowMs: Long): WindowSum {
        val nowSec = nowMs / 1000
        val from = nowSec - windowSec + 1
        var b = 0.0
        var s = 0.0
        var c = 0
        for (sl in slices) {
            if (sl.sec < from || sl.sec > nowSec) continue
            b += sl.buyNotional
            s += sl.sellNotional
            c += sl.count
        }
        return WindowSum(b, s, c)
    }

    @Synchronized
    fun sessionSum(): WindowSum = WindowSum(sessionBuy, sessionSell, sessionCount)

    @Synchronized
    fun lastPriceInfo(): Pair<Double, Double> = lastPrice to sessionOpenPrice

    @Synchronized
    fun sliceCount(): Int = slices.size

    private fun sliceFor(sec: Long): Slice {
        val last = slices.lastOrNull()
        if (last == null) {
            val created = Slice(sec)
            slices.addLast(created)
            return created
        }
        if (last.sec == sec) return last
        if (sec > last.sec) {
            val created = Slice(sec)
            slices.addLast(created)
            return created
        }
        // geç gelen (out-of-order) tick: geriye doğru doğru yeri bul
        for (i in slices.size - 2 downTo 0) {
            val sl = slices[i]
            if (sl.sec == sec) return sl
            if (sl.sec < sec) {
                val created = Slice(sec)
                slices.add(i + 1, created)
                return created
            }
        }
        val created = Slice(sec)
        slices.addFirst(created)
        return created
    }
}
