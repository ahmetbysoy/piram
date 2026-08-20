package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Binance aggTrade DTO
// {"e":"aggTrade","E":1625000000000,"s":"BTCUSDT","a":12345,"p":"34500.00","q":"0.5","f":1,"l":2,"T":1625000000000,"m":true,"M":true}
@JsonClass(generateAdapter = true)
data class BinanceAggTradeDto(
    @Json(name = "e") val eventType: String? = null,
    @Json(name = "s") val symbol: String? = null,
    @Json(name = "a") val tradeId: Long? = null,
    @Json(name = "p") val price: String? = null,
    @Json(name = "q") val quantity: String? = null,
    @Json(name = "T") val tradeTime: Long? = null,
    @Json(name = "m") val isBuyerMaker: Boolean? = null
)

// Binance Depth DTO
@JsonClass(generateAdapter = true)
data class BinanceDepthDto(
    @Json(name = "lastUpdateId") val lastUpdateId: Long? = null,
    @Json(name = "bids") val bids: List<List<String>>? = null,
    @Json(name = "asks") val asks: List<List<String>>? = null
)

// Bybit Trade DTO
@JsonClass(generateAdapter = true)
data class BybitTradeResponse(
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "ts") val timestamp: Long? = null,
    @Json(name = "data") val data: List<BybitTradeItem>? = null
)

@JsonClass(generateAdapter = true)
data class BybitTradeItem(
    @Json(name = "T") val timestamp: Long? = null,
    @Json(name = "s") val symbol: String? = null,
    @Json(name = "S") val side: String? = null, // "Buy" or "Sell"
    @Json(name = "v") val volume: String? = null,
    @Json(name = "p") val price: String? = null,
    @Json(name = "i") val tradeId: String? = null
)

// OKX Trade DTO
@JsonClass(generateAdapter = true)
data class OkxTradeResponse(
    @Json(name = "arg") val arg: Map<String, String>? = null,
    @Json(name = "data") val data: List<OkxTradeItem>? = null
)

@JsonClass(generateAdapter = true)
data class OkxTradeItem(
    @Json(name = "instId") val instId: String? = null,
    @Json(name = "tradeId") val tradeId: String? = null,
    @Json(name = "px") val price: String? = null,
    @Json(name = "sz") val size: String? = null,
    @Json(name = "side") val side: String? = null, // "buy" or "sell"
    @Json(name = "ts") val timestamp: String? = null
)

// KuCoin Ticker / Trade DTO
@JsonClass(generateAdapter = true)
data class KuCoinWsResponse(
    @Json(name = "type") val type: String? = null,
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "subject") val subject: String? = null,
    @Json(name = "data") val data: Map<String, Any>? = null
)
