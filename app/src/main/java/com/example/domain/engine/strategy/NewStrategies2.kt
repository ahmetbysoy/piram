package com.example.domain.engine.strategy

import com.example.domain.model.MarketSnapshot
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import kotlin.math.abs

/**
 * #17 WickRejectionStrategy — son pencerenin high/low/open/close'undan gerçek
 * üst/alt wick oranı hesaplar. Üst wick baskın (yukarı red) → SELL, alt wick
 * baskın (aşağı red) → BUY. PriceActionStrategy'nin eksik bıraktığı wick mantığı.
 */
class WickRejectionStrategy : Strategy {
    override val id = "wick_rejection"
    override val name = "Wick Rejection"
    override val description = "Gerçek üst/alt wick oranı ile yön reddi"
    override val category = StrategyCategory.TREND

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 10) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Wick örnekleniyor")
        }
        val window = prices.takeLast(10)
        val open = window.first()
        val close = window.last()
        val high = window.maxOrNull() ?: close
        val low = window.minOrNull() ?: close

        val range = high - low
        if (range <= 0) return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Sıfır aralık")

        val upperWick = high - maxOf(open, close)
        val lowerWick = minOf(open, close) - low
        val upperRatio = upperWick / range
        val lowerRatio = lowerWick / range

        val score = when {
            lowerRatio > 0.35 && lowerRatio > upperRatio -> 0.6   // aşağı red → BUY
            upperRatio > 0.35 && upperRatio > lowerRatio -> -0.6  // yukarı red → SELL
            else -> 0.0
        }
        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)
        val reason = "Wick: üst ${"%.0f".format(upperRatio * 100)}% alt ${"%.0f".format(lowerRatio * 100)}%"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, 0.5, 0.45), score, reason,
            mapOf("upperWick" to upperRatio, "lowerWick" to lowerRatio))
    }
}

/**
 * #19 OIDivergenceStrategy — saf OI-fiyat ayrımı:
 * fiyat↑ OI↑ = yeni long (BUY) · fiyat↑ OI↓ = short cover (zayıf) ·
 * fiyat↓ OI↑ = yeni short (SELL) · fiyat↓ OI↓ = long cover (zayıf).
 */
class OIDivergenceStrategy : Strategy {
    override val id = "oi_divergence"
    override val name = "OI Price Divergence"
    override val description = "OI + fiyat yönünden yeni pozisyon vs kapanış ayrımı"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val oiDelta = data.oiDelta
        if (oiDelta == null || abs(oiDelta) < 1e-8) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "OI Δ yok")
        }
        val prices = data.recentPrices
        if (prices.size < 5) return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Fiyat örnekleniyor")
        val changePct = (prices.last() - prices.first()) / prices.first() * 100.0

        val score = when {
            changePct > 0.05 && oiDelta > 0 -> 0.55   // yeni long
            changePct > 0.05 && oiDelta < 0 -> 0.2    // short cover (zayıf)
            changePct < -0.05 && oiDelta > 0 -> -0.55 // yeni short
            changePct < -0.05 && oiDelta < 0 -> -0.2  // long cover (zayıf)
            else -> 0.0
        }
        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)
        val reason = "Fiyat ${"%.3f".format(changePct)}% · OI Δ ${"%.0f".format(oiDelta)}"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, 0.5, 0.45), score, reason,
            mapOf("changePct" to changePct, "oiDelta" to oiDelta))
    }
}

/**
 * #20 TapeReadingSpeedStrategy — trade büyüklük dağılımının hızlanması:
 * son N trade'in ikinci yarısının ortalama notional'ı ilk yarısından büyükse
 * büyük oyuncular giriyor demektir (tape hızlanması). Yön flow ile birleşir.
 */
class TapeReadingSpeedStrategy : Strategy {
    override val id = "tape_reading_speed"
    override val name = "Tape Reading Speed"
    override val description = "Küçük emirden büyük emire geçiş ivmesi (tape hızlanması)"
    override val category = StrategyCategory.MICROSTRUCTURE

    override fun execute(data: MarketSnapshot): StrategyResult {
        val trades = data.trades
        if (trades.size < 20) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Tape örnekleniyor")
        }
        val recent = trades.takeLast(20)
        val half = recent.size / 2
        val firstAvg = recent.take(half).map { it.value }.average()
        val secondAvg = recent.takeLast(half).map { it.value }.average()
        if (firstAvg <= 0) return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Hacim yok")

        val accel = (secondAvg - firstAvg) / firstAvg   // ivme
        val flow = data.orderFlowImbalance

        val score = when {
            accel > 0.4 && flow > 0 -> 0.6    // büyük alıcılar hızlanıyor
            accel > 0.4 && flow < 0 -> -0.6   // büyük satıcılar hızlanıyor
            accel > 0.15 -> (flow * 0.5).coerceIn(-0.3, 0.3)
            else -> 0.0
        }
        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)
        val reason = "Tape ivme ${"%.0f".format(accel * 100)}% (${"%.0f".format(firstAvg)}→${"%.0f".format(secondAvg)} USDT)"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, 0.5, 0.45), score, reason,
            mapOf("accel" to accel))
    }
}

/**
 * #21 VwapReversionStrategy — fiyatın VWAP'tan aşırı sapmasını geçmiş sapma
 * dağılımıyla (z-score) ölçüp geri çekilme skoru üretir.
 */
class VwapReversionStrategy : Strategy {
    override val id = "vwap_reversion"
    override val name = "VWAP Reversion"
    override val description = "VWAP'tan aşırı sapma + geçmiş dağılım ile geri dönüş"
    override val category = StrategyCategory.MOMENTUM

    override fun execute(data: MarketSnapshot): StrategyResult {
        val vwap = data.vwap
        val price = data.currentPrice
        if (vwap <= 0 || price <= 0) return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "VWAP yok")
        val dev = (price - vwap) / vwap * 100.0

        // geçmiş sapma dağılımı (basit: son N fiyatın VWAP'a oranı)
        val prices = data.recentPrices
        if (prices.size < 20) return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Dağılım örnekleniyor")
        val devs = prices.map { (it - vwap) / vwap * 100.0 }
        val mean = devs.sum() / devs.size
        val variance = devs.map { (it - mean) * (it - mean) }.sum() / devs.size
        val std = kotlin.math.sqrt(variance)
        if (std <= 0) return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Sıfır sapma")
        val z = dev / std

        val score = when {
            z > 1.8 -> -0.6   // aşırı yukarı sapma → geri dönüş SELL
            z < -1.8 -> 0.6   // aşırı aşağı sapma → geri dönüş BUY
            else -> 0.0
        }
        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)
        val reason = "VWAP sapma ${"%.3f".format(dev)}% (z=${"%.2f".format(z)})"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, 0.5, 0.45), score, reason,
            mapOf("dev" to dev, "z" to z))
    }
}

/**
 * #22 FibonacciConfluenceStrategy — son swing high/low'dan fib retracement
 * seviyeleri (0.236/0.382/0.5/0.618/0.786); fiyat bir seviyeye yakınsa
 * + son fiyat momentumuyla birleşik skor.
 */
class FibonacciConfluenceStrategy : Strategy {
    override val id = "fibonacci_confluence"
    override val name = "Fibonacci Confluence"
    override val description = "Swing fib seviyeleriyle fiyat çakışması"
    override val category = StrategyCategory.TREND

    override fun execute(data: MarketSnapshot): StrategyResult {
        val prices = data.recentPrices
        if (prices.size < 30) {
            return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Swing örnekleniyor")
        }
        val window = prices.takeLast(30)
        val high = window.maxOrNull() ?: return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Swing yok")
        val low = window.minOrNull() ?: return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Swing yok")
        val range = high - low
        if (range <= 0) return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Sıfır aralık")

        val price = data.currentPrice
        // yükseliş swing'inde retracement seviyeleri
        val levels = listOf(0.236, 0.382, 0.5, 0.618, 0.786).map { high - range * it }
        val nearest = levels.minByOrNull { abs(it - price) } ?: return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Seviye yok")
        val distPct = abs(price - nearest) / range * 100.0
        if (distPct > 1.0) return StrategyResult(id, name, SignalType.NEUTRAL, 0.5, 0.0, "Fib seviyesi uzağında")

        // son momentum yönü
        val momentum = prices.last() - prices[prices.size - 6]
        val score = when {
            momentum > 0 -> 0.5   // fib desteğinden yukarı dönüş
            momentum < 0 -> -0.5  // fib direncinden aşağı
            else -> 0.0
        }
        val signal = SignalThresholds.signalFor(score, strong = 0.5, weak = 0.15)
        val reason = "Fib ${"%.2f".format(nearest)} (${"%.1f".format(distPct)}% yakınlık)"
        return StrategyResult(id, name, signal, SignalThresholds.confidenceFor(score, 0.5, 0.45), score, reason,
            mapOf("fibLevel" to nearest, "distPct" to distPct))
    }
}
