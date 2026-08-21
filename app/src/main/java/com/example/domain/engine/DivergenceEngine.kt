package com.example.domain.engine

import com.example.domain.model.LayerAggregate
import kotlin.math.abs
import kotlin.math.tanh

enum class DivergenceKind { TOPLAMA, BOSALTMA, YOK }

data class DivergenceSignal(
    val kind: DivergenceKind,
    val score: Double,
    val yazi: String
)

/**
 * Toplama / boşaltma anlatısı (piramit'ten port):
 * tepe (büyük) katman neti vs taban (küçük) katman neti + fiyat değişimi (tanh).
 * OI yalnızca dipnot; yoksa cümleye eklenmez — "yalan yok".
 */
object DivergenceEngine {

    fun evaluate(
        layers: List<LayerAggregate>,
        priceChangePct: Double,
        oiDelta: Double? = null,
        numLayers: Int = SignalConfig.DEFAULT_LAYERS,
        minNotional: Double = SignalConfig.DIV_MIN_NOTIONAL,
        pxTau: Double = SignalConfig.PX_TAU,
        divScore: Double = SignalConfig.DIV_SCORE
    ): DivergenceSignal {
        if (layers.isEmpty()) return DivergenceSignal(DivergenceKind.YOK, 0.0, "")

        // Katman sayısına göre türetilir (sabit indeks yok):
        // top = üst 2 katman (whale + shark), bottom = alt ~%40 (retail).
        val topFrom = (numLayers - 2).coerceAtLeast(1)
        val bottomTo = (numLayers * 2 / 5).coerceAtLeast(1)

        val top = layers.filter { it.layerIndex >= topFrom }
        val bot = layers.filter { it.layerIndex < bottomTo }

        val topNet = top.sumOf { it.buyNotional - it.sellNotional }
        val botNet = bot.sumOf { it.buyNotional - it.sellNotional }
        val topAbs = top.sumOf { it.buyNotional + it.sellNotional }
        val botAbs = bot.sumOf { it.buyNotional + it.sellNotional }

        if (topAbs + botAbs < minNotional) {
            return DivergenceSignal(DivergenceKind.YOK, 0.0, "")
        }

        val topRatio = if (topAbs > 0) topNet / topAbs else 0.0
        val botRatio = if (botAbs > 0) botNet / botAbs else 0.0
        val px = tanh(priceChangePct / pxTau)

        val bosaltma = maxOf(0.0, px) * maxOf(0.0, -topRatio) * maxOf(0.0, botRatio)
        val toplama = maxOf(0.0, -px) * maxOf(0.0, topRatio) * maxOf(0.0, -botRatio)

        return when {
            bosaltma >= divScore && bosaltma >= toplama -> DivergenceSignal(
                DivergenceKind.BOSALTMA,
                bosaltma,
                "Küçükler kovalıyor, büyükler SATIŞ — boşaltma." + oiNote(oiDelta, DivergenceKind.BOSALTMA)
            )
            toplama >= divScore -> DivergenceSignal(
                DivergenceKind.TOPLAMA,
                toplama,
                "Büyükler ALIŞ yazıyor, küçükler SATIŞ — toplama." + oiNote(oiDelta, DivergenceKind.TOPLAMA)
            )
            else -> DivergenceSignal(DivergenceKind.YOK, maxOf(bosaltma, toplama), "")
        }
    }

    private fun oiNote(oiDelta: Double?, kind: DivergenceKind): String {
        if (oiDelta == null || abs(oiDelta) < 1e-8) return ""
        return if (kind == DivergenceKind.TOPLAMA) {
            if (oiDelta > 0) " OI şişiyor — yeni long kokusu." else " OI iniyor — short kapanışı olabilir."
        } else {
            if (oiDelta > 0) " OI şişiyor — yeni short açılıyor olabilir." else " OI iniyor — long'lar kapanıyor."
        }
    }
}
