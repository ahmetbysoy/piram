package com.example.presentation.screens.pyramid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.MathUtils
import com.example.data.local.db.AppDatabase
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.data.repository.MarketDataRepository
import com.example.data.repository.MarketDataRepositoryImpl
import com.example.domain.engine.AdaptiveEdges
import com.example.domain.engine.DepthAggregator
import com.example.domain.engine.DivergenceEngine
import com.example.domain.engine.DivergenceKind
import com.example.domain.engine.OneMinuteVolumeTracker
import com.example.domain.engine.SignalConfig
import com.example.domain.engine.bucket.MicroBucketManager
import com.example.domain.engine.burst.BurstDetector
import com.example.domain.engine.strategy.StrategyEngine
import com.example.domain.engine.strategy.TechnicalIndicators
import com.example.domain.model.BurstCluster
import com.example.domain.model.ConsensusResult
import com.example.domain.model.Depth
import com.example.domain.model.ExchangeStatus
import com.example.domain.model.LayerAggregate
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import com.example.presentation.components.HapticController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

data class PyramidUiState(
    val symbol: String = "BTCUSDT",
    val currentPrice: Double = 0.0,
    val layers: List<LayerAggregate> = emptyList(),
    val selectedLayer: LayerAggregate? = null,
    val whaleOrders: List<Order> = emptyList(),
    val activeBursts: List<BurstCluster> = emptyList(),
    val consensus: ConsensusResult = ConsensusResult(
        overallSignal = SignalType.NEUTRAL,
        buyScore = 50.0,
        sellScore = 50.0,
        neutralScore = 100.0,
        confidence = 0.5,
        consensusStrength = 0.0,
        activeStrategiesCount = 20,
        bullishCount = 0,
        bearishCount = 0,
        neutralCount = 20
    ),
    val depth: Depth? = null,
    val venueDepths: List<Depth> = emptyList(),
    val venuePrices: Map<String, Double> = emptyMap(),
    val buyVolume1m: Double = 0.0,
    val sellVolume1m: Double = 0.0,
    val exchangeStatuses: List<ExchangeStatus> = emptyList(),
    val orderFlowImbalance: Double = 0.0,
    val vwap: Double = 0.0,
    val whaleNotional: Double = 0.0,
    val retailNotional: Double = 0.0,
    val divergenceYazi: String = "",
    val divergenceKind: DivergenceKind = DivergenceKind.YOK,
    val timeframe: String = "1M",
    val isHapticEnabled: Boolean = true
)

class PyramidViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository: MarketDataRepository = MarketDataRepositoryImpl(db)
    val preferencesRepository = UserPreferencesRepository(application)
    val strategyEngine = StrategyEngine()
    private val hapticController = HapticController(application)

    private val bucketManager = MicroBucketManager(
        numLayers = SignalConfig.DEFAULT_LAYERS,
        minNotional = SignalConfig.MIN_NOTIONAL,
        maxNotional = SignalConfig.MAX_NOTIONAL
    )
    private val burstDetector = BurstDetector(windowMs = 1500L, minOrderCount = 3, minVolumeSpike = 0.3)

    private val _uiState = MutableStateFlow(PyramidUiState())
    val uiState: StateFlow<PyramidUiState> = _uiState.asStateFlow()

    private val recentTrades = ConcurrentLinkedDeque<Order>()
    private val recentPrices = ConcurrentLinkedDeque<Double>()
    private val recentWhales = ConcurrentLinkedDeque<Order>()
    private val venuePrices = ConcurrentHashMap<String, Double>()
    private val minuteVolume = OneMinuteVolumeTracker()
    private val recentNotionals = ConcurrentLinkedDeque<Double>()
    private var sessionOpenPrice = 0.0
    private var lastAdaptRun = 0L
    private var adaptLo: Double = SignalConfig.MIN_NOTIONAL
    private var adaptHi: Double = SignalConfig.MAX_NOTIONAL

    private var tradeStreamJob: Job? = null
    private var depthStreamJob: Job? = null
    private var tickerAnimationJob: Job? = null
    private var currentSymbol = "BTCUSDT"
    private var decayFactor = 0.15f

    init {
        // Collect exchange statuses
        viewModelScope.launch {
            repository.exchangeStatuses.collect { statuses ->
                _uiState.value = _uiState.value.copy(exchangeStatuses = statuses)
            }
        }

        // Collect preferences
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collectLatest { prefs ->
                val symbolChanged = prefs.activeSymbol != currentSymbol
                currentSymbol = prefs.activeSymbol
                decayFactor = prefs.decayFactor
                _uiState.value = _uiState.value.copy(
                    symbol = prefs.activeSymbol,
                    timeframe = prefs.timeframe,
                    isHapticEnabled = prefs.hapticEnabled
                )

                if (symbolChanged || tradeStreamJob == null) {
                    startStreaming(prefs.activeSymbol, prefs.enabledExchanges)
                }
            }
        }

        startEngineLoop()
    }

    private fun startStreaming(symbol: String, enabledExchanges: Set<String>) {
        tradeStreamJob?.cancel()
        depthStreamJob?.cancel()

        bucketManager.reset()
        burstDetector.clear()
        recentTrades.clear()
        recentPrices.clear()
        recentWhales.clear()
        venuePrices.clear()
        minuteVolume.clear()
        recentNotionals.clear()
        sessionOpenPrice = 0.0
        lastAdaptRun = 0L

        // Stream real-time trades
        tradeStreamJob = viewModelScope.launch(Dispatchers.IO) {
            repository.subscribeTrades(symbol, enabledExchanges).collect { rawOrder ->
                val processedOrder = bucketManager.processOrder(rawOrder)

                recentTrades.addLast(processedOrder)
                while (recentTrades.size > 200) recentTrades.pollFirst()

                recentPrices.addLast(processedOrder.price)
                while (recentPrices.size > 100) recentPrices.pollFirst()

                recentNotionals.addLast(processedOrder.value)
                while (recentNotionals.size > 800) recentNotionals.pollFirst()
                if (sessionOpenPrice == 0.0) sessionOpenPrice = processedOrder.price

                // Cross-venue last price + rolling 1-minute volume flow
                venuePrices[processedOrder.exchange] = processedOrder.price
                minuteVolume.record(processedOrder.side, processedOrder.value)

                if (processedOrder.isWhale) {
                    recentWhales.addFirst(processedOrder)
                    while (recentWhales.size > 25) recentWhales.pollLast()
                    hapticController.triggerWhaleAlert(_uiState.value.isHapticEnabled)
                }

                val burst = burstDetector.processOrder(processedOrder)
                if (burst != null) {
                    hapticController.triggerBurstAlert(_uiState.value.isHapticEnabled)
                }
            }
        }

        // Stream depth from every enabled venue and aggregate into one book
        depthStreamJob = viewModelScope.launch(Dispatchers.IO) {
            repository.subscribeDepth(symbol, enabledExchanges).collect { depths ->
                val aggregated = DepthAggregator.aggregate(depths)
                _uiState.value = _uiState.value.copy(
                    depth = aggregated,
                    venueDepths = depths
                )
            }
        }
    }

    private fun startEngineLoop() {
        tickerAnimationJob?.cancel()
        tickerAnimationJob = viewModelScope.launch(Dispatchers.Default) {
            var lastStrategyRunTime = 0L

            while (isActive) {
                delay(SignalConfig.ENGINE_TICK_MS) // ~12 fps state update loop for smooth Canvas animation

                // Smooth exponential decay & display lerp
                bucketManager.decayAll(decayRate = decayFactor, dtSeconds = 0.08f)
                bucketManager.updateDisplay(smoothingFactor = SignalConfig.DISPLAY_SMOOTHING)

                val layers = bucketManager.getAggregatedLayers()
                val whaleVol = bucketManager.getWhaleNotional()
                val retailVol = bucketManager.getRetailNotional()
                val buyVolume1m = minuteVolume.buyVolume()
                val sellVolume1m = minuteVolume.sellVolume()
                val activeBursts = burstDetector.getActiveBursts()
                val tradeList = recentTrades.toList()
                val priceList = recentPrices.toList()
                val currentPrice = priceList.lastOrNull() ?: _uiState.value.currentPrice

                val ofi = TechnicalIndicators.orderFlowImbalance(tradeList)
                val vwap = TechnicalIndicators.vwap(tradeList)

                val now = System.currentTimeMillis()
                var consensus = _uiState.value.consensus

                // Adaptif eşik: coin'in notional dağılımına göre katman aralıklarını yenile (histerezisli)
                if (recentNotionals.size >= SignalConfig.ADAPT_MIN_TRADES && now - lastAdaptRun >= 5000L) {
                    lastAdaptRun = now
                    AdaptiveEdges.adaptiveRange(recentNotionals.toList())?.let { (lo, hi) ->
                        val loChange = kotlin.math.abs(lo - adaptLo) / adaptLo
                        val hiChange = kotlin.math.abs(hi - adaptHi) / adaptHi
                        if (loChange > SignalConfig.HYSTERESIS || hiChange > SignalConfig.HYSTERESIS) {
                            adaptLo = lo
                            adaptHi = hi
                            bucketManager.reconfigureThresholds(lo, hi)
                        }
                    }
                }

                // Toplama / boşaltma anlatısı (büyükler vs küçükler + fiyat)
                val changePct = if (sessionOpenPrice > 0) (currentPrice - sessionOpenPrice) / sessionOpenPrice * 100.0 else 0.0
                val divergence = DivergenceEngine.evaluate(layers, changePct, oiDelta = null)

                // Run 20 Strategies every 250ms
                if (now - lastStrategyRunTime >= SignalConfig.STRATEGY_RUN_MS && priceList.size >= SignalConfig.MIN_PRICES_FOR_STRATEGY) {
                    lastStrategyRunTime = now
                    val snapshot = MarketSnapshot(
                        symbol = currentSymbol,
                        currentPrice = currentPrice,
                        trades = tradeList,
                        recentPrices = priceList,
                        recentVolumes = tradeList.map { it.volume },
                        depth = _uiState.value.depth,
                        bursts = activeBursts,
                        orderFlowImbalance = ofi,
                        buyVolume1m = buyVolume1m,
                        sellVolume1m = sellVolume1m,
                        vwap = vwap,
                        exchangePrices = venuePrices.toMap(),
                        timestamp = now
                    )
                    val (_, computedConsensus) = strategyEngine.executeAll(snapshot)
                    consensus = computedConsensus
                }

                _uiState.value = _uiState.value.copy(
                    currentPrice = currentPrice,
                    layers = layers,
                    whaleOrders = recentWhales.toList(),
                    activeBursts = activeBursts,
                    consensus = consensus,
                    orderFlowImbalance = ofi,
                    vwap = vwap,
                    whaleNotional = whaleVol,
                    retailNotional = retailVol,
                    divergenceYazi = divergence.yazi,
                    divergenceKind = divergence.kind,
                    buyVolume1m = buyVolume1m,
                    sellVolume1m = sellVolume1m,
                    venuePrices = venuePrices.toMap()
                )
            }
        }
    }

    fun selectLayer(layer: LayerAggregate?) {
        _uiState.value = _uiState.value.copy(selectedLayer = layer)
        hapticController.triggerTick(_uiState.value.isHapticEnabled)
    }

    fun setTimeframe(timeframe: String) {
        viewModelScope.launch {
            preferencesRepository.updateTimeframe(timeframe)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tradeStreamJob?.cancel()
        depthStreamJob?.cancel()
        tickerAnimationJob?.cancel()
        repository.disconnectAll()
    }
}
