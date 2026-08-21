package com.example.domain.engine

/**
 * #13 generateTradeStory — son dakikayı tek cümlelik Türkçe "kanka özeti"ne çevirir.
 * DivergenceEngine'in anlatısının genellemesi. Saf Kotlin, test edilebilir.
 */
object StoryGenerator {

    fun generate(
        whaleNotional: Double,
        retailNotional: Double,
        ofi: Double,          // -1..+1
        burstCount: Int,
        currentPrice: Double,
        vwap: Double
    ): String {
        val total = whaleNotional + retailNotional
        val whalePct = if (total > 0) whaleNotional / total * 100.0 else 0.0
        val ofiPct = ofi * 100.0

        val sb = StringBuilder("Kanka özet: ")
        // Akış + yüzde başta (en kritik bilgi) — tek satırda kesilse bile kaybolmaz
        sb.append(
            when {
                ofiPct > 5.0 -> "+${"%.0f".format(ofiPct)}% ALIŞ yönlü"
                ofiPct < -5.0 -> "${"%.0f".format(ofiPct)}% SATIŞ yönlü"
                else -> "dengeli akış"
            }
        )
        sb.append(", ")
        sb.append(
            if (whalePct >= 50.0) "kurumsal ağırlıkta (${"%.0f".format(whalePct)}%)"
            else "perakende ağırlıkta (${"%.0f".format(100.0 - whalePct)}%)"
        )
        if (burstCount > 0) sb.append(", $burstCount salvo aktif")
        sb.append(
            when {
                vwap > 0 && currentPrice >= vwap -> " — fiyat VWAP üstünde."
                vwap > 0 -> " — fiyat VWAP altında."
                else -> "."
            }
        )
        return sb.toString()
    }
}
