package com.example.data.remote.ws

import com.example.domain.model.ConnectionState
import com.example.domain.model.Depth
import com.example.domain.model.DepthLevel
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class KrakenWsClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
) : ExchangeWebSocketClient {

    override val exchangeName = "Kraken"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val reconnectPolicy = WsReconnectPolicy()
    private var activeWs: WebSocket? = null
    private var activeDepthWs: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun trades(symbol: String): Flow<Order> = callbackFlow {
        // Kraken expects e.g. "XBT/USD" or "BTC/USDT"
        val clean = symbol.uppercase().replace("-", "")
        val krakenPair = when (clean) {
            "BTCUSDT" -> "XBT/USDT"
            "BTCUSD" -> "XBT/USD"
            "ETHUSDT" -> "ETH/USDT"
            "SOLUSDT" -> "SOL/USDT"
            else -> if (clean.endsWith("USDT")) "${clean.substring(0, clean.length - 4)}/USDT" else clean
        }

        var isClosing = false
        var attempt = 0

        fun connectWs() {
            if (!isActive || isClosing) return

            _connectionState.value = ConnectionState.Connecting
            val request = Request.Builder().url("wss://ws.kraken.com").build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    attempt = 0
                    _connectionState.value = ConnectionState.Connected
                    val subMsg = JSONObject().apply {
                        put("event", "subscribe")
                        put("pair", org.json.JSONArray().apply { put(krakenPair) })
                        put("subscription", JSONObject().apply { put("name", "trade") })
                    }
                    webSocket.send(subMsg.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        if (text.startsWith("[")) {
                            val arr = org.json.JSONArray(text)
                            if (arr.length() >= 4 && arr.getString(2) == "trade") {
                                val tradesArr = arr.getJSONArray(1)
                                for (i in 0 until tradesArr.length()) {
                                    val item = tradesArr.getJSONArray(i)
                                    val price = item.getString(0).toDoubleOrNull() ?: 0.0
                                    val volume = item.getString(1).toDoubleOrNull() ?: 0.0
                                    val ts = (item.getString(2).toDoubleOrNull() ?: (System.currentTimeMillis() / 1000.0)) * 1000.0
                                    val sideStr = item.getString(3) // "b" or "s"

                                    if (price > 0 && volume > 0) {
                                        val order = Order(
                                            id = "kraken_${ts.toLong()}_$i",
                                            side = if (sideStr == "b") OrderSide.BUY else OrderSide.SELL,
                                            volume = volume,
                                            price = price,
                                            timestamp = ts.toLong(),
                                            value = price * volume,
                                            exchange = exchangeName
                                        )
                                        trySend(order)
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (isClosing) return
                    attempt++
                    val delayMs = reconnectPolicy.calculateDelay(attempt)
                    _connectionState.value = ConnectionState.Reconnecting(attempt, delayMs)
                    scope.launch {
                        delay(delayMs)
                        if (isActive && !isClosing) connectWs()
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (!isClosing) {
                        attempt++
                        val delayMs = reconnectPolicy.calculateDelay(attempt)
                        _connectionState.value = ConnectionState.Reconnecting(attempt, delayMs)
                        scope.launch {
                            delay(delayMs)
                            if (isActive && !isClosing) connectWs()
                        }
                    }
                }
            }

            reconnectPolicy.cleanClose(activeWs)
            activeWs = client.newWebSocket(request, listener)
        }

        connectWs()

        awaitClose {
            isClosing = true
            reconnectPolicy.cleanClose(activeWs)
            activeWs = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    override fun depth(symbol: String): Flow<Depth> = callbackFlow {
        // Kraken expects e.g. "XBT/USDT"
        val clean = symbol.uppercase().replace("-", "")
        val krakenPair = when (clean) {
            "BTCUSDT" -> "XBT/USDT"
            "BTCUSD" -> "XBT/USD"
            "ETHUSDT" -> "ETH/USDT"
            "SOLUSDT" -> "SOL/USDT"
            else -> if (clean.endsWith("USDT")) "${clean.substring(0, clean.length - 4)}/USDT" else clean
        }

        var isClosing = false

        fun connectDepthWs() {
            if (!isActive || isClosing) return
            val request = Request.Builder().url("wss://ws.kraken.com/v2").build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val subMsg = JSONObject().apply {
                        put("method", "subscribe")
                        put("params", JSONObject().apply {
                            put("channel", "book")
                            put("symbol", org.json.JSONArray().apply { put(krakenPair) })
                            put("depth", 25)
                        })
                    }
                    webSocket.send(subMsg.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val dataArr = json.optJSONArray("data")
                        if (dataArr != null && dataArr.length() > 0) {
                            val item = dataArr.getJSONObject(0)
                            val bids = parseKrakenLevels(item.optJSONArray("bids"))
                            val asks = parseKrakenLevels(item.optJSONArray("asks"))
                            if (bids.isNotEmpty() && asks.isNotEmpty()) {
                                trySend(Depth(bids, asks, exchangeName, System.currentTimeMillis()))
                            }
                        }
                    } catch (_: Exception) {}
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (isClosing) return
                    scope.launch {
                        delay(3000)
                        if (isActive && !isClosing) connectDepthWs()
                    }
                }
            }

            reconnectPolicy.cleanClose(activeDepthWs)
            activeDepthWs = client.newWebSocket(request, listener)
        }

        connectDepthWs()

        awaitClose {
            isClosing = true
            reconnectPolicy.cleanClose(activeDepthWs)
            activeDepthWs = null
        }
    }

    override fun disconnect() {
        reconnectPolicy.cleanClose(activeWs)
        reconnectPolicy.cleanClose(activeDepthWs)
        activeWs = null
        activeDepthWs = null
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun parseKrakenLevels(arr: org.json.JSONArray?): List<DepthLevel> {
        if (arr == null) return emptyList()
        val levels = ArrayList<DepthLevel>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val p = obj.optString("price", "0").toDoubleOrNull() ?: 0.0
            val v = obj.optString("qty", "0").toDoubleOrNull() ?: 0.0
            if (p > 0 && v > 0) levels.add(DepthLevel(p, v))
        }
        return levels
    }
}
