package com.example.domain

import com.example.domain.model.SymbolMeta

/**
 * Sembol kayıt defteri (piramit `precision.ts` + `seedSymbols.ts` portu).
 *
 * - exchangeInfo gelene kadar tohum liste devrede (arama her zaman çalışır).
 * - `resolve` / `search` rank skorlu arama yapar.
 * - `tickDecimals` fiyat biçimleme için ondalık hane sayısını verir.
 *
 * Saf Kotlin (Android bağımlılığı yok) — JUnit ile test edilebilir.
 */
class SymbolRegistry {

    private val map = LinkedHashMap<String, SymbolMeta>()
    private var list: List<SymbolMeta> = emptyList()

    var loaded: Boolean = false
        private set

    init {
        applySeed()
    }

    fun get(symbol: String): SymbolMeta? = map[symbol.uppercase().trim()]

    fun symbols(): List<SymbolMeta> = list

    /** Fiyat biçimleme için ondalık hane; bilinmiyorsa null (otomatik biçime dön). */
    fun tickDecimals(symbol: String): Int? = get(symbol)?.tickDecimals()

    fun ingest(metas: List<SymbolMeta>) {
        if (metas.isEmpty()) return
        map.clear()
        list = metas
        for (m in metas) map[m.symbol] = m
        list = list.sortedBy { it.symbol }
        loaded = true
    }

    /** Sorguyu geçerli bir sembole çevirir; bulamazsa null. */
    fun resolve(query: String): String? {
        val q = query.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (q.isEmpty()) return null
        if (map.containsKey(q)) return q
        if (map.containsKey("${q}USDT")) return "${q}USDT"
        if (map.containsKey("${q}USDC")) return "${q}USDC"
        val hit = list.find { it.symbol.startsWith(q) || it.base == q }
        if (hit != null) return hit.symbol
        return if (q.endsWith("USDT") || q.endsWith("USDC")) q else "${q}USDT"
    }

    /** Rank skorlu arama; boş sorguda popülerler döner. */
    fun search(query: String, limit: Int = 12): List<SymbolMeta> {
        val q = query.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (q.isEmpty()) {
            return list.filter { Regex("^(BTC|ETH|SOL|BNB|XRP|DOGE|PEPE)").containsMatchIn(it.symbol) }
                .take(limit)
        }
        return list
            .map { it to rank(it, q) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun rank(s: SymbolMeta, q: String): Int = when {
        s.symbol == q || s.base == q -> 100
        s.symbol.startsWith(q) -> 80
        s.base.startsWith(q) -> 70
        s.symbol.contains(q) -> 40
        else -> 0
    }

    private fun applySeed() {
        map.clear()
        list = SEED_SYMBOLS
        for (s in SEED_SYMBOLS) map[s.symbol] = s
        loaded = false
    }

    companion object {
        /** CORS / ağ yoksa bile arama çalışsın — popüler spot çiftleri + tick. */
        val SEED_SYMBOLS: List<SymbolMeta> = listOf(
            SymbolMeta("BTCUSDT", "BTC", "0.01", "0.00001"),
            SymbolMeta("ETHUSDT", "ETH", "0.01", "0.0001"),
            SymbolMeta("BNBUSDT", "BNB", "0.01", "0.001"),
            SymbolMeta("SOLUSDT", "SOL", "0.01", "0.001"),
            SymbolMeta("XRPUSDT", "XRP", "0.0001", "0.1"),
            SymbolMeta("DOGEUSDT", "DOGE", "0.00001", "1"),
            SymbolMeta("ADAUSDT", "ADA", "0.0001", "1"),
            SymbolMeta("AVAXUSDT", "AVAX", "0.01", "0.001"),
            SymbolMeta("LINKUSDT", "LINK", "0.01", "0.001"),
            SymbolMeta("DOTUSDT", "DOT", "0.001", "0.1"),
            SymbolMeta("NEARUSDT", "NEAR", "0.001", "0.1"),
            SymbolMeta("SUIUSDT", "SUI", "0.0001", "0.1"),
            SymbolMeta("PEPEUSDT", "PEPE", "0.00000001", "1"),
            SymbolMeta("WIFUSDT", "WIF", "0.0001", "0.1"),
            SymbolMeta("AAVEUSDT", "AAVE", "0.01", "0.001"),
            SymbolMeta("LTCUSDT", "LTC", "0.01", "0.001"),
            SymbolMeta("APTUSDT", "APT", "0.0001", "0.1"),
            SymbolMeta("ARBUSDT", "ARB", "0.0001", "0.1"),
            SymbolMeta("OPUSDT", "OP", "0.0001", "0.1"),
            SymbolMeta("TIAUSDT", "TIA", "0.0001", "0.1")
        )
    }
}
