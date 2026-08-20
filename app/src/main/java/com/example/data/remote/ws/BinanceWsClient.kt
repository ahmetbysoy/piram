package com.example.data.remote.ws

import com.example.core.util.MathUtils
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

class BinanceWsClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : ExchangeWebSocketClient {

    override val exchangeName = "Binance"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val reconnectPolicy = WsReconnectPolicy()
    private var activeTradeWs: WebSocket? = null
    private var activeDepthWs: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun trades(symbol: String): Flow<Order> = callbackFlow {
        val cleanSymbol = symbol.lowercase().replace("-", "").replace("/", "")
        var attempt = 0
        var isClosing = false

        fun connectWs() {
            if (!isActive || isClosing) return

            _connectionState.value = ConnectionState.Connecting
            val url = "wss://stream.binance.com:9443/ws/${cleanSymbol}@aggTrade"
            val request = Request.Builder().url(url).build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    attempt = 0
                    _connectionState.value = ConnectionState.Connected
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val price = json.optString("p", "0").toDoubleOrNull() ?: 0.0
                        val volume = json.optString("q", "0").toDoubleOrNull() ?: 0.0
                        val isBuyerMaker = json.optBoolean("m", false)
                        val tradeTime = json.optLong("T", System.currentTimeMillis())
                        val tradeId = json.optLong("a", System.currentTimeMillis()).toString()

                        if (price > 0 && volume > 0) {
                            val order = Order(
                                id = tradeId,
                                side = if (isBuyerMaker) OrderSide.SELL else OrderSide.BUY,
                                volume = volume,
                                price = price,
                                timestamp = tradeTime,
                                value = price * volume,
                                exchange = exchangeName
                            )
                            trySend(order)
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

            reconnectPolicy.cleanClose(activeTradeWs)
            activeTradeWs = client.newWebSocket(request, listener)
        }

        connectWs()

        awaitClose {
            isClosing = true
            reconnectPolicy.cleanClose(activeTradeWs)
            activeTradeWs = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    override fun depth(symbol: String): Flow<Depth> = callbackFlow {
        val cleanSymbol = symbol.lowercase().replace("-", "").replace("/", "")
        var isClosing = false

        fun connectDepthWs() {
            if (!isActive || isClosing) return
            val url = "wss://stream.binance.com:9443/ws/${cleanSymbol}@depth20@100ms"
            val request = Request.Builder().url(url).build()

            val listener = object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val bidsArray = json.optJSONArray("bids")
                        val asksArray = json.optJSONArray("asks")

                        val bids = ArrayList<DepthLevel>()
                        if (bidsArray != null) {
                            for (i in 0 until bidsArray.length()) {
                                val item = bidsArray.getJSONArray(i)
                                val p = item.getString(0).toDoubleOrNull() ?: 0.0
                                val v = item.getString(1).toDoubleOrNull() ?: 0.0
                                if (p > 0 && v > 0) bids.add(DepthLevel(p, v))
                            }
                        }

                        val asks = ArrayList<DepthLevel>()
                        if (asksArray != null) {
                            for (i in 0 until asksArray.length()) {
                                val item = asksArray.getJSONArray(i)
                                val p = item.getString(0).toDoubleOrNull() ?: 0.0
                                val v = item.getString(1).toDoubleOrNull() ?: 0.0
                                if (p > 0 && v > 0) asks.add(DepthLevel(p, v))
                            }
                        }

                        if (bids.isNotEmpty() && asks.isNotEmpty()) {
                            val depth = Depth(
                                bids = bids,
                                asks = asks,
                                exchange = exchangeName,
                                timestamp = System.currentTimeMillis()
                            )
                            trySend(depth)
                        }
                    } catch (_: Exception) {}
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
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
        reconnectPolicy.cleanClose(activeTradeWs)
        reconnectPolicy.cleanClose(activeDepthWs)
        activeTradeWs = null
        activeDepthWs = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
