package com.example.domain.engine

import com.example.domain.model.Depth
import com.example.domain.model.DepthLevel

/**
 * Pure multi-venue order book aggregator.
 *
 * Merges per-venue depth snapshots into a single consolidated book:
 * bids are sorted best-first (descending price), asks best-first (ascending price),
 * each side capped at [MAX_LEVELS]. Venues with an empty side are ignored.
 *
 * This is what turns the five exchange depth streams into one synthetic
 * "aggregated" L2 book for the microstructure strategies and the UI.
 */
object DepthAggregator {

    const val MAX_LEVELS = 50
    const val AGGREGATED_LABEL = "AGGREGATED"

    fun aggregate(
        depths: List<Depth>,
        timestamp: Long = System.currentTimeMillis()
    ): Depth? {
        val valid = depths.filter { it.bids.isNotEmpty() && it.asks.isNotEmpty() }
        if (valid.isEmpty()) return null

        // Aynı fiyat seviyesindeki farklı borsa hacimleri birleştirilir (gerçek consolidated book).
        val bids = mergeLevels(valid.flatMap { it.bids })
            .sortedByDescending { it.price }
            .take(MAX_LEVELS)

        val asks = mergeLevels(valid.flatMap { it.asks })
            .sortedBy { it.price }
            .take(MAX_LEVELS)

        return Depth(
            bids = bids,
            asks = asks,
            exchange = AGGREGATED_LABEL,
            timestamp = timestamp
        )
    }

    fun bestBid(depth: Depth?): DepthLevel? = depth?.bids?.firstOrNull()

    fun bestAsk(depth: Depth?): DepthLevel? = depth?.asks?.firstOrNull()

    /**
     * Aynı fiyat seviyesindeki hacimleri toplar.
     * Fiyatlar string'den parse edildiği için borsalar arası hassas fark olabilir;
     * 8 ondalık haneye yuvarlanarak anahtar yapılır.
     */
    private fun mergeLevels(levels: List<DepthLevel>): List<DepthLevel> {
        val map = LinkedHashMap<Double, Double>()
        for (level in levels) {
            val key = Math.round(level.price * 1e8) / 1e8
            map[key] = (map[key] ?: 0.0) + level.volume
        }
        return map.map { (price, volume) -> DepthLevel(price, volume) }
    }
}
