package com.example.data.remote.ws

import com.example.domain.model.ConnectionState
import com.example.domain.model.MiniTickerParser
import com.example.domain.model.MiniTickerRow
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
import java.util.concurrent.TimeUnit

/**
 * Binance USD-M futures `!miniTicker@arr` akışı — tüm sembollerin 24s özeti
 * (radar ekranı için). Yaklaşık saniyede bir tam liste yayınlanır.
 */
class BinanceMiniTickerClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val reconnectPolicy = WsReconnectPolicy()
    private var activeWs: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun miniTickers(): Flow<List<MiniTickerRow>> = callbackFlow {
        var isClosing = false
        var attempt = 0

        fun connectWs() {
            if (!isActive || isClosing) return

            _connectionState.value = ConnectionState.Connecting
            val request = Request.Builder()
                .url("wss://fstream.binance.com/market/ws/!miniTicker@arr")
                .build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    attempt = 0
                    _connectionState.value = ConnectionState.Connected
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val rows = MiniTickerParser.parseArray(text)
                    if (rows.isNotEmpty()) trySend(rows)
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

    fun disconnect() {
        reconnectPolicy.cleanClose(activeWs)
        activeWs = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
