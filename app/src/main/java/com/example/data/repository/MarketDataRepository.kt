package com.example.data.repository

import com.example.data.local.db.AppDatabase
import com.example.data.local.db.TradeEntity
import com.example.data.remote.ws.BinanceLiquidationClient
import com.example.data.remote.ws.BinanceWsClient
import com.example.data.remote.ws.BybitWsClient
import com.example.data.remote.ws.ExchangeWebSocketClient
import com.example.data.remote.ws.KrakenWsClient
import com.example.data.remote.ws.KuCoinWsClient
import com.example.data.remote.ws.OkxWsClient
import com.example.domain.model.ConnectionState
import com.example.domain.model.Depth
import com.example.domain.model.ExchangeStatus
import com.example.domain.model.Liquidation
import com.example.domain.model.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

interface MarketDataRepository {
    val exchangeStatuses: StateFlow<List<ExchangeStatus>>
    fun subscribeTrades(symbol: String, enabledExchanges: Set<String>): Flow<Order>
    /**
     * Emits the latest depth snapshot of every enabled exchange as a list.
     * The consumer is responsible for aggregating (see [com.example.domain.engine.DepthAggregator]).
     */
    fun subscribeDepth(symbol: String, enabledExchanges: Set<String>): Flow<List<Depth>>
    fun subscribeLiquidations(): Flow<Liquidation>
    fun getRecentDbTrades(symbol: String, limit: Int = 100): Flow<List<Order>>
    fun disconnectAll()
}

class MarketDataRepositoryImpl(
    private val database: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : MarketDataRepository {

    private val binanceClient = BinanceWsClient()
    private val bybitClient = BybitWsClient()
    private val okxClient = OkxWsClient()
    private val krakenClient = KrakenWsClient()
    private val kuCoinClient = KuCoinWsClient()
    private val liquidationClient = BinanceLiquidationClient()

    private val clients = listOf(
        binanceClient,
        bybitClient,
        okxClient,
        krakenClient,
        kuCoinClient
    )

    private val _exchangeStatuses = MutableStateFlow<List<ExchangeStatus>>(
        clients.map { ExchangeStatus(it.exchangeName, ConnectionState.Disconnected) }
    )
    override val exchangeStatuses: StateFlow<List<ExchangeStatus>> = _exchangeStatuses.asStateFlow()

    init {
        // Monitor connection states
        clients.forEach { client ->
            scope.launch {
                client.connectionState.collect { state ->
                    _exchangeStatuses.value = _exchangeStatuses.value.map { status ->
                        if (status.exchange == client.exchangeName) {
                            status.copy(state = state)
                        } else {
                            status
                        }
                    }
                }
            }
        }
    }

    override fun subscribeTrades(symbol: String, enabledExchanges: Set<String>): Flow<Order> = channelFlow {
        val activeClients = clients.filter { enabledExchanges.contains(it.exchangeName) }
        val effectiveClients = if (activeClients.isEmpty()) listOf(binanceClient) else activeClients

        val flows = effectiveClients.map { client ->
            client.trades(symbol).map { order ->
                // Update stats
                _exchangeStatuses.value = _exchangeStatuses.value.map { status ->
                    if (status.exchange == client.exchangeName) {
                        status.copy(
                            messageCount = status.messageCount + 1,
                            lastMessageTime = System.currentTimeMillis()
                        )
                    } else {
                        status
                    }
                }

                // Async Room persistence
                scope.launch {
                    try {
                        database.tradeDao().insertTrade(TradeEntity.fromDomain(order, symbol))
                    } catch (_: Exception) {}
                }

                order
            }
        }

        flows.merge().collect { order ->
            send(order)
        }
    }

    override fun subscribeDepth(symbol: String, enabledExchanges: Set<String>): Flow<List<Depth>> = channelFlow {
        val activeClients = clients.filter { enabledExchanges.contains(it.exchangeName) }
        val effectiveClients = if (activeClients.isEmpty()) listOf(binanceClient) else activeClients

        // Keep the most recent book per venue; re-emit the full set on every update.
        val latest = ConcurrentHashMap<String, Depth>()

        val flows = effectiveClients.map { client ->
            client.depth(symbol).map { depth ->
                latest[client.exchangeName] = depth
                latest.values.toList()
            }
        }

        flows.merge().collect { depths ->
            send(depths)
        }
    }

    override fun subscribeLiquidations(): Flow<Liquidation> {
        return liquidationClient.liquidations()
    }

    override fun getRecentDbTrades(symbol: String, limit: Int): Flow<List<Order>> {
        return database.tradeDao().getRecentTrades(symbol, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun disconnectAll() {
        clients.forEach { it.disconnect() }
        liquidationClient.disconnect()
    }
}
