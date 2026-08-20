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

class KuCoinWsClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
) : ExchangeWebSocketClient {

    override val exchangeName = "KuCoin"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val reconnectPolicy = WsReconnectPolicy()
    private var activeWs: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun trades(symbol: String): Flow<Order> = callbackFlow {
        val clean = symbol.uppercase().replace("/", "")
        val kuCoinSymbol = if (!clean.contains("-") && clean.endsWith("USDT")) {
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

            // KuCoin Public Direct Push endpoint
            val request = Request.Builder().url("wss://ws-api-spot.kucoin.com/?connectId=${System.currentTimeMillis()}").build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    attempt = 0
                    _connectionState.value = ConnectionState.Connected
                    val subMsg = JSONObject().apply {
                        put("id", System.currentTimeMillis())
                        put("type", "subscribe")
                        put("topic", "/market/match:$kuCoinSymbol")
                        put("privateChannel", false)
                        put("response", true)
                    }
                    webSocket.send(subMsg.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val type = json.optString("type")
                        if (type == "message") {
                            val dataObj = json.optJSONObject("data")
                            if (dataObj != null) {
                                val price = dataObj.optString("price", "0").toDoubleOrNull() ?: 0.0
                                val size = dataObj.optString("size", "0").toDoubleOrNull() ?: 0.0
                                val side = dataObj.optString("side", "buy")
                                val ts = dataObj.optLong("time", System.currentTimeMillis() * 1_000_000) / 1_000_000
                                val tradeId = dataObj.optString("tradeId", ts.toString())

                                if (price > 0 && size > 0) {
                                    val order = Order(
                                        id = "kucoin_$tradeId",
                                        side = if (side.equals("buy", ignoreCase = true)) OrderSide.BUY else OrderSide.SELL,
                                        volume = size,
                                        price = price,
                                        timestamp = ts,
                                        value = price * size,
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
