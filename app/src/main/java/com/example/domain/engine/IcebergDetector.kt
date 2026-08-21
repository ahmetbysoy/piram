package com.example.domain.engine

import com.example.domain.model.Depth
import com.example.domain.model.DepthLevel

/**
 * #14 icebergDetector — aynı fiyat seviyesinde defalarca dolup yeniden dolan emirler
 * (iceberg / gizli büyük emir) tespiti. Saf, injectable değil ama state'li ve test edilebilir
 * (deterministik depth dizisiyle).
 *
 * Mantık: bir seviye zirvesinin altına düşer (dropRatio) ve tekrar zirveye yakın dolarsa
 * (recoverRatio) "refill" sayılır; [refillThreshold] tekrar → iceberg.
 */
class IcebergDetector(
    private val refillThreshold: Int = 3,
    private val dropRatio: Double = 0.5,
    private val recoverRatio: Double = 0.8
) {
    data class IcebergHit(val side: String, val price: Double)

    private class Track {
        var peak = 0.0
        var refilling = false
        var refills = 0
    }

    private val bids = HashMap<Double, Track>()
    private val asks = HashMap<Double, Track>()

    fun detect(depth: Depth?): List<IcebergHit> {
        if (depth == null) return emptyList()
        val out = ArrayList<IcebergHit>()
        scan(depth.bids.take(10), bids, "BID", out)
        scan(depth.asks.take(10), asks, "ASK", out)
        return out
    }

    private fun scan(
        levels: List<DepthLevel>,
        map: HashMap<Double, Track>,
        side: String,
        out: MutableList<IcebergHit>
    ) {
        for (lvl in levels) {
            val t = map.getOrPut(lvl.price) { Track() }
            if (!t.refilling && t.peak > 0 && lvl.volume < t.peak * dropRatio) {
                t.refilling = true
            }
            if (t.refilling && lvl.volume >= t.peak * recoverRatio) {
                t.refills++
                t.refilling = false
            }
            if (lvl.volume > t.peak) t.peak = lvl.volume
            if (t.refills >= refillThreshold) out.add(IcebergHit(side, lvl.price))
        }
    }

    fun reset() {
        bids.clear()
        asks.clear()
    }
}
