package com.example.data.remote.ws

import com.example.domain.model.ConnectionState
import com.example.domain.model.Depth
import com.example.domain.model.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ExchangeWebSocketClient {
    val exchangeName: String
    val connectionState: StateFlow<ConnectionState>
    fun trades(symbol: String): Flow<Order>
    fun depth(symbol: String): Flow<Depth>
    fun disconnect()
}
