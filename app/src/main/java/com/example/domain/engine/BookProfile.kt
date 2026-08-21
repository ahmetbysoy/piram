package com.example.domain.engine

import com.example.domain.model.Depth

/**
 * Konsolide order book profil özeti (heatmap / dengesizlik için).
 * Saf Kotlin — test edilebilir.
 */
data class BookProfile(
    val bidTotal: Double,     // ilk N seviye toplam bid hacmi
    val askTotal: Double,     // ilk N seviye toplam ask hacmi
    val imbalance: Double,    // -1..+1 (pozitif = alıcı ağırlıklı)
    val bestBid: Double,
    val bestAsk: Double,
    val spread: Double,
    val bidWallPrice: Double?, // en kalın bid seviyesi (duvar)
    val askWallPrice: Double?  // en kalın ask seviyesi (duvar)
)

object BookProfile {

    fun compute(depth: Depth?, levels: Int = 10): BookProfile? {
        if (depth == null || depth.bids.isEmpty() || depth.asks.isEmpty()) return null

        val bids = depth.bids.take(levels)
        val asks = depth.asks.take(levels)

        val bidTotal = bids.sumOf { it.volume }
        val askTotal = asks.sumOf { it.volume }
        val imbalance = if (bidTotal + askTotal > 0) {
            (bidTotal - askTotal) / (bidTotal + askTotal)
        } else {
            0.0
        }

        val bidWall = bids.maxByOrNull { it.volume }
        val askWall = asks.maxByOrNull { it.volume }

        return BookProfile(
            bidTotal = bidTotal,
            askTotal = askTotal,
            imbalance = imbalance,
            bestBid = depth.bids.first().price,
            bestAsk = depth.asks.first().price,
            spread = depth.spread,
            bidWallPrice = bidWall?.price,
            askWallPrice = askWall?.price
        )
    }
}
