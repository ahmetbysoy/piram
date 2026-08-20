package com.example.data.remote.ws

import com.example.domain.model.ConnectionState
import com.example.domain.model.Depth
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

class OkxWsClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
) : ExchangeWebSocketClient {

    override val exchangeName = "OKX"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val reconnectPolicy = WsReconnectPolicy()
    private var activeWs: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun trades(symbol: String): Flow<Order> = callbackFlow {
        // Format BTCUSDT -> BTC-USDT
        val clean = symbol.uppercase().replace("/", "")
        val okxInstId = if (!clean.contains("-") && clean.endsWith("USDT")) {
            val base = clean.substring(0, clean.length - 4)
            "$base-USDT"
        } else {
            clean
        }

        var isClosing = false
        var attempt = 0

        fun connectWs() {
            if (!isActive || isClosing) return

            _connectionState.value = ConnectionState.Connecting
            val request = Request.Builder().url("wss://ws.okx.com:8443/ws/v5/public").build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    attempt = 0
                    _connectionState.value = ConnectionState.Connected
                    val subMsg = JSONObject().apply {
                        put("op", "subscribe")
                        put("args", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("channel", "trades")
                                put("instId", okxInstId)
                            })
                        })
                    }
                    webSocket.send(subMsg.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val dataArr = json.optJSONArray("data")
                        if (dataArr != null) {
                            for (i in 0 until dataArr.length()) {
                                val item = dataArr.getJSONObject(i)
                                val price = item.optString("px", "0").toDoubleOrNull() ?: 0.0
                                val volume = item.optString("sz", "0").toDoubleOrNull() ?: 0.0
                                val sideStr = item.optString("side", "buy")
                                val ts = item.optString("ts", System.currentTimeMillis().toString()).toLongOrNull() ?: System.currentTimeMillis()
                                val tradeId = item.optString("tradeId", ts.toString())

                                if (price > 0 && volume > 0) {
                                    val order = Order(
                                        id = "okx_$tradeId",
                                        side = if (sideStr.equals("buy", ignoreCase = true)) OrderSide.BUY else OrderSide.SELL,
                                        volume = volume,
                                        price = price,
                                        timestamp = ts,
                                        value = price * volume,
                                        exchange = exchangeName
                                    )
                                    trySend(order)
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
        awaitClose {}
    }

    override fun disconnect() {
        reconnectPolicy.cleanClose(activeWs)
        activeWs = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
