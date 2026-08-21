package com.example.presentation.screens.pyramid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.MathUtils
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.JournalEntity
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.data.remote.rest.ExchangeInfoClient
import com.example.data.repository.MarketDataRepository
import com.example.data.repository.MarketDataRepositoryImpl
import com.example.domain.SymbolRegistry
import com.example.domain.engine.AdaptiveEdges
import com.example.domain.engine.DepthAggregator
import com.example.domain.engine.DivergenceEngine
import com.example.domain.engine.CalmBeforeStorm
import com.example.domain.engine.DivergenceKind
import com.example.domain.engine.LiquidationTracker
import com.example.domain.engine.MarketPersonality
import com.example.domain.engine.MultiTimeframeConsensus
import com.example.domain.engine.NextCandleGame
import com.example.domain.engine.OneMinuteVolumeTracker
import com.example.domain.engine.PainScore
import com.example.domain.engine.PersonalityHistory
import com.example.domain.engine.SignalConfig
import com.example.domain.engine.WindowLedger
import com.example.domain.engine.bucket.MicroBucketManager
import com.example.domain.engine.burst.BurstDetector
import com.example.domain.engine.strategy.StrategyEngine
import com.example.domain.engine.strategy.TechnicalIndicators
import com.example.domain.model.BurstCluster
import com.example.domain.model.ConsensusResult
import com.example.domain.model.Depth
import com.example.domain.model.ExchangeStatus
import com.example.domain.model.JournalRow
import com.example.domain.model.LayerAggregate
import com.example.domain.model.Liquidation
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.OiSnap
import com.example.domain.model.OiState
import com.example.domain.model.OpenInterestParser
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import com.example.presentation.components.HapticController
import com.example.presentation.components.NotificationHelper
import com.example.presentation.components.SoundController
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
        activeStrategiesCount = 25,
        bullishCount = 0,
        bearishCount = 0,
        neutralCount = 25
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
    val changePct: Double = 0.0,
    val recentLiqNotional: Double = 0.0,
    val painYazi: String = "",
    val calmStormYazi: String = "",
    val personalitySummary: String = "",
    val mtfYazi: String = "",
    val nextCandleChip: String = "",
    val divergenceYazi: String = "",
    val divergenceKind: DivergenceKind = DivergenceKind.YOK,
    val timeframeBuyNotional: Double = 0.0,
    val timeframeSellNotional: Double = 0.0,
    val lastLiquidation: Liquidation? = null,
    val oiUsdt: Double? = null,
    val oiDelta: Double? = null,
    val oiState: OiState = OiState.BEKLIYOR,
    val fundingRate: Double? = null,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val volume24h: Double = 0.0,
    val priceChange24h: Double = 0.0,
    val journal: List<JournalRow> = emptyList(),
    val priceDecimals: Int = -1,   // -1 = otomatik; aksi halde tickSize hanesi
    val timeframe: String = "1M",
    val isHapticEnabled: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val soundEnabled: Boolean = false
)

class PyramidViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository: MarketDataRepository = MarketDataRepositoryImpl(db)
    val preferencesRepository = UserPreferencesRepository(application)
    val symbolRegistry = SymbolRegistry()
    val strategyEngine = StrategyEngine()
    private val hapticController = HapticController(application)
    private val soundController = SoundController()

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
    private val venueTimes = ConcurrentHashMap<String, Long>()
    private val minuteVolume = OneMinuteVolumeTracker()
    private val recentNotionals = ConcurrentLinkedDeque<Double>()
    private val windowLedger = WindowLedger()
    private val liqTracker = LiquidationTracker()
    private val personalityHistory = PersonalityHistory()
    private val candleGame = NextCandleGame()
    private val notificationHelper by lazy { NotificationHelper(getApplication()) }
    private var lastMtfRun = 0L
    private var lastPersonality = ""
    private var lastWhaleNotifAt = 0L
    private var lastBurstNotifAt = 0L
    private var sessionOpenPrice = 0.0
    private var lastAdaptRun = 0L
    private var adaptLo: Double = SignalConfig.MIN_NOTIONAL
    private var adaptHi: Double = SignalConfig.MAX_NOTIONAL

    private var tradeStreamJob: Job? = null
    private var depthStreamJob: Job? = null
    private var liquidationStreamJob: Job? = null
    private var oiPollingJob: Job? = null
    private var tickerAnimationJob: Job? = null
    private var prevOi: OiSnap? = null
    private var lastJournalKind: DivergenceKind = DivergenceKind.YOK
    private var lastJournalAt = 0L
    private var lastJournalMark = 0L
    private var currentSymbol = "BTCUSDT"
    private var decayFactor = 0.15f

    init {
        // Sembol listesi + tickSize (exchangeInfo; tohum liste fallback)
        viewModelScope.launch(Dispatchers.IO) {
            ExchangeInfoClient().fetch()?.let { symbolRegistry.ingest(it) }
            val decimals = symbolRegistry.tickDecimals(currentSymbol) ?: -1
            _uiState.value = _uiState.value.copy(priceDecimals = decimals)
        }

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
                val decimals = symbolRegistry.tickDecimals(prefs.activeSymbol) ?: -1
                _uiState.value = _uiState.value.copy(
                    symbol = prefs.activeSymbol,
                    timeframe = prefs.timeframe,
                    isHapticEnabled = prefs.hapticEnabled,
                    notificationsEnabled = prefs.notificationsEnabled,
                    soundEnabled = prefs.soundEnabled,
                    priceDecimals = decimals
                )

                if (symbolChanged || tradeStreamJob == null) {
                    startStreaming(prefs.activeSymbol, prefs.enabledExchanges)
                }
            }
        }

        // Collect journal (sinyal günlüğü) — Room Flow
        viewModelScope.launch {
            db.journalDao().getRecent(12).collect { entities ->
                _uiState.value = _uiState.value.copy(
                    journal = entities.map {
                        JournalRow(
                            kind = it.kind,
                            price = it.price,
                            at = it.at,
                            later5 = it.later5,
                            later15 = it.later15,
                            later60 = it.later60
                        )
                    }
                )
            }
        }

        startLiquidationStream()
        startEngineLoop()
    }

    /** Açık pozisyon (OI) periyodik sorgusu — durum makinesi: bekliyor / ok / yok / eski. */
    private fun startOiPolling(symbol: String) {
        oiPollingJob?.cancel()
        prevOi = null
        _uiState.value = _uiState.value.copy(
            oiUsdt = null,
            oiDelta = null,
            oiState = OiState.BEKLIYOR
        )
        oiPollingJob = viewModelScope.launch(Dispatchers.IO) {
            var tickerPollCount = 0
            while (isActive) {
                val snap = repository.fetchOpenInterest(symbol)
                val price = _uiState.value.currentPrice
                if (snap != null && snap.symbol.equals(symbol, ignoreCase = true)) {
                    val prev = prevOi
                    val delta = if (prev != null && prev.symbol == snap.symbol) snap.oi - prev.oi else null
                    prevOi = snap
                    _uiState.value = _uiState.value.copy(
                        oiUsdt = OpenInterestParser.oiToUsdt(snap.oi, price),
                        oiDelta = delta,
                        oiState = OiState.OK
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        oiState = if (_uiState.value.oiUsdt != null) OiState.ESKI else OiState.YOK
                    )
                }

                // Funding rate (squeeze stratejisi için) — OI ile aynı ritimde
                val funding = repository.fetchFundingRate(symbol)
                _uiState.value = _uiState.value.copy(fundingRate = funding?.lastFundingRate)

                // 24s ticker istatistikleri (her 3. turda bir — 30 sn)
                tickerPollCount++
                if (tickerPollCount % 3 == 0) {
                    val t = repository.fetchTicker24h(symbol)
                    if (t != null && t.symbol.equals(symbol, ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(
                            high24h = t.highPrice,
                            low24h = t.lowPrice,
                            volume24h = t.quoteVolume,
                            priceChange24h = t.changePct
                        )
                    }
                }

                delay(10_000L)
            }
        }
    }

    /** Binance futures likidasyon akışı (global; aktif sembole göre filtreler). */
    private fun startLiquidationStream() {
        liquidationStreamJob?.cancel()
        liquidationStreamJob = viewModelScope.launch(Dispatchers.IO) {
            repository.subscribeLiquidations().collect { liq ->
                if (liq.symbol == _uiState.value.symbol) {
                    liqTracker.record(liq.side, liq.notional, liq.timestamp)
                    _uiState.value = _uiState.value.copy(lastLiquidation = liq)
                    hapticController.triggerBurstAlert(_uiState.value.isHapticEnabled)
                }
            }
        }
    }

    private fun startStreaming(symbol: String, enabledExchanges: Set<String>) {
        tradeStreamJob?.cancel()
        depthStreamJob?.cancel()

        startOiPolling(symbol)

        bucketManager.reset()
        burstDetector.clear()
        recentTrades.clear()
        recentPrices.clear()
        recentWhales.clear()
        venuePrices.clear()
        venueTimes.clear()
        minuteVolume.clear()
        recentNotionals.clear()
        windowLedger.reset()
        liqTracker.clear()
        personalityHistory.clear()
        candleGame.clear()
        lastMtfRun = 0L
        lastPersonality = ""
        sessionOpenPrice = 0.0
        lastAdaptRun = 0L
        lastJournalKind = DivergenceKind.YOK
        lastJournalAt = 0L

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

                windowLedger.ingest(processedOrder)

                // Cross-venue last price + rolling 1-minute volume flow
                venuePrices[processedOrder.exchange] = processedOrder.price
                venueTimes[processedOrder.exchange] = processedOrder.timestamp
                minuteVolume.record(processedOrder.side, processedOrder.value)

                if (processedOrder.isWhale) {
                    recentWhales.addFirst(processedOrder)
                    while (recentWhales.size > 25) recentWhales.pollLast()
                    hapticController.triggerWhaleAlert(_uiState.value.isHapticEnabled)
                    soundController.playWhale(_uiState.value.soundEnabled)
                    maybeNotifyWhale(processedOrder)
                }

                val burst = burstDetector.processOrder(processedOrder)
                if (burst != null) {
                    hapticController.triggerBurstAlert(_uiState.value.isHapticEnabled)
                    soundController.playBurst(_uiState.value.soundEnabled)
                    maybeNotifyBurst(burst)
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
            var mtfYazi = ""

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
                val divergence = DivergenceEngine.evaluate(layers, changePct, oiDelta = _uiState.value.oiDelta)

                // Eğlenceli/özet katman: acı skoru, fırtına rozeti, kişilik geçmişi
                val painYazi = PainScore.evaluate(liqTracker.sumSell(now), liqTracker.sumBuy(now)) ?: ""
                val calmStormYazi = CalmBeforeStorm.evaluate(priceList, _uiState.value.depth) ?: ""
                val totalNV = whaleVol + retailVol
                val whalePct = if (totalNV > 0) whaleVol / totalNV * 100.0 else 0.0
                val personalityLabel = MarketPersonality.evaluate(
                    whalePct = whalePct,
                    burstCount = activeBursts.size,
                    changePct = changePct,
                    ofi = ofi
                ).first
                if (personalityLabel != lastPersonality) {
                    lastPersonality = personalityLabel
                    personalityHistory.record(personalityLabel, now)
                }
                val personalitySummary = personalityHistory.summaryChip(now)

                // Sinyal günlüğü: toplama/boşaltma kaydet (60sn spam koruması)
                if (divergence.kind != DivergenceKind.YOK &&
                    divergence.kind != lastJournalKind &&
                    now - lastJournalAt >= 60_000L &&
                    currentPrice > 0
                ) {
                    lastJournalKind = divergence.kind
                    lastJournalAt = now
                    val entry = JournalEntity(
                        id = now,
                        symbol = currentSymbol,
                        kind = divergence.kind.name,
                        price = currentPrice,
                        at = now
                    )
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching { db.journalDao().insert(entry) }
                    }
                }

                // later5/15/60 işaretleme (her 5 sn)
                if (now - lastJournalMark >= 5_000L) {
                    lastJournalMark = now
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching {
                            db.journalDao().markLater5(now, currentPrice)
                            db.journalDao().markLater15(now, currentPrice)
                            db.journalDao().markLater60(now, currentPrice)
                        }
                    }
                }

                // Seçili timeframe'e göre pencere toplamı (WindowLedger)
                windowLedger.pruneKeep(now)
                val winSec = windowSeconds(_uiState.value.timeframe)
                val winSum = if (winSec != null) windowLedger.sumWindow(winSec, now) else windowLedger.sessionSum()

                // Run 20 Strategies every 250ms
                if (now - lastStrategyRunTime >= SignalConfig.STRATEGY_RUN_MS && priceList.size >= SignalConfig.MIN_PRICES_FOR_STRATEGY) {
                    lastStrategyRunTime = now
                    val snapshot = MarketSnapshot(
                        symbol = currentSymbol,
                        currentPrice = currentPrice,
                        high24h = _uiState.value.high24h,
                        low24h = _uiState.value.low24h,
                        volume24h = _uiState.value.volume24h,
                        priceChange24h = _uiState.value.priceChange24h,
                        trades = tradeList,
                        recentPrices = priceList,
                        recentVolumes = tradeList.map { it.volume },
                        depth = _uiState.value.depth,
                        bursts = activeBursts,
                        orderFlowImbalance = ofi,
                        buyVolume1m = buyVolume1m,
                        sellVolume1m = sellVolume1m,
                        liquidationNotional60s = liqTracker.sum(now),
                        liquidationCount60s = liqTracker.count(now),
                        fundingRate = _uiState.value.fundingRate,
                        oiDelta = _uiState.value.oiDelta,
                        vwap = vwap,
                        exchangePrices = venuePrices.toMap(),
                        venueTimes = venueTimes.toMap(),
                        timestamp = now
                    )
                    val (_, computedConsensus) = strategyEngine.executeAll(snapshot)
                    consensus = computedConsensus

                    // #7 multi-timeframe: 60sn pencerede ayrı konsensüs (1sn'de bir)
                    if (now - lastMtfRun >= 1000L) {
                        lastMtfRun = now
                        val shortTrades = tradeList.filter { now - it.timestamp <= 60_000L }
                        if (shortTrades.size >= SignalConfig.MIN_PRICES_FOR_STRATEGY) {
                            val shortSnap = MarketSnapshot(
                                symbol = currentSymbol,
                                currentPrice = currentPrice,
                                trades = shortTrades,
                                recentPrices = shortTrades.map { it.price },
                                recentVolumes = shortTrades.map { it.volume },
                                depth = _uiState.value.depth,
                                bursts = activeBursts,
                                orderFlowImbalance = TechnicalIndicators.orderFlowImbalance(shortTrades),
                                liquidationNotional60s = liqTracker.sum(now),
                                liquidationCount60s = liqTracker.count(now),
                                fundingRate = _uiState.value.fundingRate,
                                oiDelta = _uiState.value.oiDelta,
                                vwap = TechnicalIndicators.vwap(shortTrades),
                                exchangePrices = venuePrices.toMap(),
                                venueTimes = venueTimes.toMap(),
                                timestamp = now
                            )
                            val (_, shortConsensus) = strategyEngine.executeAll(shortSnap)
                            mtfYazi = MultiTimeframeConsensus.compare(shortConsensus.overallSignal, consensus.overallSignal) ?: ""
                        }
                    }

                    // #10 next-candle tahmin oyunu
                    candleGame.predict(consensus.consensusStrength, currentPrice, now)
                }
                candleGame.resolve(currentPrice, now)
                val nextCandleChip = candleGame.chip()

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
                    changePct = changePct,
                    recentLiqNotional = liqTracker.sum(now),
                    painYazi = painYazi,
                    calmStormYazi = calmStormYazi,
                    personalitySummary = personalitySummary,
                    mtfYazi = mtfYazi,
                    nextCandleChip = nextCandleChip,
                    divergenceYazi = divergence.yazi,
                    divergenceKind = divergence.kind,
                    timeframeBuyNotional = winSum.buyNotional,
                    timeframeSellNotional = winSum.sellNotional,
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

    /** Timeframe etiketini pencere saniyesine çevirir; null → oturum (ALL). */
    private fun windowSeconds(timeframe: String): Long? = when (timeframe) {
        "1M" -> 60L
        "5M" -> 300L
        "15M" -> 900L
        else -> null // "ALL" → oturum boyu
    }

    /** Whale bildirimi (30 sn throttle, tercih kapalıysa no-op). */
    private fun maybeNotifyWhale(order: Order) {
        if (!_uiState.value.notificationsEnabled) return
        val now = System.currentTimeMillis()
        if (now - lastWhaleNotifAt < 30_000L) return
        lastWhaleNotifAt = now
        val side = if (order.side == OrderSide.BUY) "ALIŞ" else "SATIŞ"
        notificationHelper.postWhale(
            symbol = _uiState.value.symbol,
            side = side,
            volume = order.volume,
            price = order.price,
            value = order.value
        )
    }

    /** Salvo bildirimi (30 sn throttle, tercih kapalıysa no-op). */
    private fun maybeNotifyBurst(burst: BurstCluster) {
        if (!_uiState.value.notificationsEnabled) return
        val now = System.currentTimeMillis()
        if (now - lastBurstNotifAt < 30_000L) return
        lastBurstNotifAt = now
        val side = if (burst.side == OrderSide.BUY) "ALIŞ" else "SATIŞ"
        notificationHelper.postBurst(
            symbol = _uiState.value.symbol,
            side = side,
            orderCount = burst.orderCount,
            totalValue = burst.totalValue
        )
    }

    override fun onCleared() {
        super.onCleared()
        tradeStreamJob?.cancel()
        depthStreamJob?.cancel()
        liquidationStreamJob?.cancel()
        oiPollingJob?.cancel()
        tickerAnimationJob?.cancel()
        repository.disconnectAll()
    }
}
