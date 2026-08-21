package com.example.domain.model

/** Sinyal günlüğü satırı (UI/model). */
data class JournalRow(
    val kind: String,   // TOPLAMA / BOSALTMA
    val price: Double,
    val at: Long,
    val later5: Double? = null,
    val later15: Double? = null,
    val later60: Double? = null
)

/**
 * İsabet oranı: later15 dolu satırlarda sinyal yönü tutmuş mu?
 * toplama → fiyat yükselmeli; bosaltma → fiyat düşmeli. (n, ok) döner.
 */
fun journalHitRate(rows: List<JournalRow>): Pair<Int, Int> {
    var n = 0
    var ok = 0
    for (r in rows) {
        val later = r.later15 ?: continue
        n++
        val up = later >= r.price
        if (r.kind == "TOPLAMA" && up) ok++
        if (r.kind == "BOSALTMA" && !up) ok++
    }
    return n to ok
}
