package com.example.domain.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Reconnecting(val attempt: Int, val delayMs: Long) : ConnectionState()
    data class Error(val cause: String) : ConnectionState()
}

data class ExchangeStatus(
    val exchange: String,
    val state: ConnectionState,
    val pingMs: Long = 0,
    val messageCount: Long = 0,
    val lastMessageTime: Long = 0L,
    val isEnabled: Boolean = true
)
