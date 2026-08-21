package com.example.domain.engine

import com.example.domain.model.Order

/**
 * #15 absorptionIndex — yüksek hacim + düşük fiyat hareketi = absorpsiyon.
 * Büyük notional giriyor ama fiyat kıpırdamıyorsa bir taraf emri emiyor demektir
 * (LiquidityHunt için mikro-sinyal katmanı). 0..1 arası skor.
 */
object AbsorptionIndex {

    fun compute(trades: List<Order>, prices: List<Double>): Double {
        if (trades.size < 10 || prices.size < 10) return 0.0
        val notional = trades.takeLast(20).sumOf { it.value }
        val recent = prices.takeLast(20)
        val hi = recent.maxOrNull() ?: return 0.0
        val lo = recent.minOrNull() ?: return 0.0
        if (hi <= 0) return 0.0
        val rangePct = (hi - lo) / hi * 100.0
        if (rangePct <= 0) return 0.0
        // notional (100K USDT başına) / fiyat aralığı % → dar aralık + yüksek hacim = yüksek skor
        return ((notional / 100_000.0) / rangePct).coerceIn(0.0, 1.0)
    }
}
