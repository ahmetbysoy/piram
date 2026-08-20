package com.example.data.remote.ws

import okhttp3.WebSocket
import java.util.Random
import kotlin.math.min
import kotlin.math.pow

class WsReconnectPolicy(
    private val baseDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 30_000L,
    private val multiplier: Double = 1.8
) {
    private val random = Random()

    fun calculateDelay(attempt: Int): Long {
        val exponential = (baseDelayMs * multiplier.pow(attempt.toDouble())).toLong()
        val capped = min(exponential, maxDelayMs)
        val jitter = random.nextInt((capped * 0.2).toInt().coerceAtLeast(100))
        return capped + jitter
    }

    fun cleanClose(webSocket: WebSocket?) {
        try {
            webSocket?.close(1000, "Normal Closure")
        } catch (_: Exception) {
            try {
                webSocket?.cancel()
            } catch (_: Exception) {}
        }
    }
}
