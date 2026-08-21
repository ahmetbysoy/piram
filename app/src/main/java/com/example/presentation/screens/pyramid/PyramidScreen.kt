package com.example.presentation.screens.pyramid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.theme.BgDark
import com.example.domain.engine.MarketMood
import com.example.domain.engine.MarketPersonality
import com.example.domain.engine.RektMeter
import com.example.domain.engine.StoryGenerator
import com.example.domain.engine.StreakStats
import com.example.presentation.components.StatusIndicatorBadge
import com.example.presentation.components.TimeframeSelector

/**
 * 3 katmanlı bilgi mimarisi:
 *  Katman 1 — Hero: sembol + fiyat + büyük sinyal rozeti + güven + anlatı (tek kart).
 *  Katman 2 — Sinyal Şeridi: öncelikli tek chip; Piyasa Şeridi: kurumsal/perakende + venue.
 *  Katman 3 — Detaylar: tape/likidasyon/journal/ticker/derinlik/mikroyapı (genişletilebilir).
 */
@Composable
fun PyramidScreen(
    viewModel: PyramidViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val story = StoryGenerator.generate(
        whaleNotional = uiState.whaleNotional,
        retailNotional = uiState.retailNotional,
        ofi = uiState.orderFlowImbalance,
        burstCount = uiState.activeBursts.size,
        currentPrice = uiState.currentPrice,
        vwap = uiState.vwap
    )
    // Tek anlatı motoru: divergence (spesifik) öncelikli, yoksa "kanka özeti"
    val heroNarrative = uiState.divergenceYazi.ifBlank { story }
    val moodEmoji = MarketMood.emoji(uiState.consensus.consensusStrength)

    val totalNV = uiState.whaleNotional + uiState.retailNotional
    val whalePct = if (totalNV > 0) uiState.whaleNotional / totalNV * 100.0 else 0.0
    val personality = MarketPersonality.evaluate(
        whalePct = whalePct,
        burstCount = uiState.activeBursts.size,
        changePct = uiState.changePct,
        ofi = uiState.orderFlowImbalance
    )
    val streakCurrent = StreakStats.fromJournal(uiState.journal).current
    val quiet = totalNV < 500.0 && uiState.activeBursts.isEmpty()

    // Katman sayısına göre dinamik canvas yüksekliği (min 200dp, max 400dp)
    val canvasHeight = (uiState.layers.size * 32).dp.coerceIn(200.dp, 400.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .testTag("pyramid_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Üst kontrol çubuğu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusIndicatorBadge(statuses = uiState.exchangeStatuses)
                TimeframeSelector(
                    selectedTimeframe = uiState.timeframe,
                    onSelectTimeframe = { viewModel.setTimeframe(it) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // KATMAN 1 — HERO
            ConsensusHeader(
                consensus = uiState.consensus,
                currentPrice = uiState.currentPrice,
                symbol = uiState.symbol,
                priceDecimals = uiState.priceDecimals,
                consensusUnstable = uiState.consensusUnstable,
                moodEmoji = moodEmoji,
                narrative = heroNarrative
            )

            Spacer(modifier = Modifier.height(6.dp))

            // KATMAN 2 — Sinyal Şeridi (tek öncelikli chip)
            SignalStrip(
                calmStormYazi = uiState.calmStormYazi,
                painYazi = uiState.painYazi,
                mtfYazi = uiState.mtfYazi,
                personalityChip = "${personality.second} ${personality.first}",
                personalitySummary = uiState.personalitySummary,
                streakCurrent = streakCurrent,
                nextCandleChip = uiState.nextCandleChip,
                quiet = quiet
            )

            Spacer(modifier = Modifier.height(6.dp))

            // KATMAN 2 — Piyasa Şeridi (skor + venue birleşik)
            MarketStrip(
                whaleNotional = uiState.whaleNotional,
                retailNotional = uiState.retailNotional,
                venuePrices = uiState.venuePrices,
                venueDepths = uiState.venueDepths,
                buyVolume1m = uiState.buyVolume1m,
                sellVolume1m = uiState.sellVolume1m,
                timeframeBuyNotional = uiState.timeframeBuyNotional,
                timeframeSellNotional = uiState.timeframeSellNotional,
                timeframe = uiState.timeframe,
                oiUsdt = uiState.oiUsdt,
                oiDelta = uiState.oiDelta,
                oiState = uiState.oiState,
                priceDecimals = uiState.priceDecimals
            )

            Spacer(modifier = Modifier.height(8.dp))

            // KATMAN 2 — Piramit Canvas (dinamik yükseklik)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            ) {
                PyramidCanvas(
                    layers = uiState.layers,
                    selectedLayerIndex = uiState.selectedLayer?.layerIndex,
                    onLayerSelected = { viewModel.selectLayer(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // KATMAN 3 — Detaylar (varsayılan kapalı)
            DetailsSection {
                TickerTape(
                    whaleOrders = uiState.whaleOrders,
                    priceDecimals = uiState.priceDecimals,
                    onOrderClick = { /* could inspect whale order */ }
                )
                LiquidationBanner(
                    liquidation = uiState.lastLiquidation,
                    priceDecimals = uiState.priceDecimals,
                    rektLevel = RektMeter.level(uiState.recentLiqNotional)
                )
                SignalJournalCard(journal = uiState.journal)
                TickerStatsCard(
                    high24h = uiState.high24h,
                    low24h = uiState.low24h,
                    change24h = uiState.priceChange24h,
                    volume24h = uiState.volume24h,
                    priceDecimals = uiState.priceDecimals
                )
                OrderBookBars(
                    depth = uiState.depth,
                    priceDecimals = uiState.priceDecimals
                )
                MicrostructureStatsCard(
                    orderFlowImbalance = uiState.orderFlowImbalance,
                    vwap = uiState.vwap,
                    depth = uiState.depth,
                    whaleNotional = uiState.whaleNotional,
                    retailNotional = uiState.retailNotional,
                    burstCount = uiState.activeBursts.size,
                    priceDecimals = uiState.priceDecimals
                )
            }
        }

        // Katman detay tooltip overlay
        AnimatedVisibility(
            visible = uiState.selectedLayer != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            uiState.selectedLayer?.let { layer ->
                TooltipOverlay(
                    layer = layer,
                    onDismiss = { viewModel.selectLayer(null) }
                )
            }
        }
    }
}
