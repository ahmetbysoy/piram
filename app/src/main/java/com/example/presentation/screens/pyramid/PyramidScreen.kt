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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.theme.BgDark
import com.example.presentation.components.StatusIndicatorBadge
import com.example.presentation.components.TimeframeSelector

@Composable
fun PyramidScreen(
    viewModel: PyramidViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .testTag("pyramid_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Top Control Bar: Status Indicator + Timeframe Selector
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

            // Consensus and Price Header
            ConsensusHeader(
                consensus = uiState.consensus,
                currentPrice = uiState.currentPrice,
                symbol = uiState.symbol,
                priceDecimals = uiState.priceDecimals
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Toplama / boşaltma anlatısı
            FlowNarrative(
                divergenceYazi = uiState.divergenceYazi,
                divergenceKind = uiState.divergenceKind
            )

            // Cross-venue spread, 1M flow & per-venue prices
            VenueStrip(
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

            // Whale Ticker Tape
            TickerTape(
                whaleOrders = uiState.whaleOrders,
                priceDecimals = uiState.priceDecimals,
                onOrderClick = { /* could inspect whale order */ }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Likidasyon bandı
            LiquidationBanner(
                liquidation = uiState.lastLiquidation,
                priceDecimals = uiState.priceDecimals
            )

            // Sinyal günlüğü (toplama/boşaltma + isabet)
            SignalJournalCard(journal = uiState.journal)

            Spacer(modifier = Modifier.height(8.dp))

            // Central Interactive MicroBucket Pyramid Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                PyramidCanvas(
                    layers = uiState.layers,
                    selectedLayerIndex = uiState.selectedLayer?.layerIndex,
                    onLayerSelected = { viewModel.selectLayer(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-time Microstructure Stats Card
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

        // Layer Detail Tooltip Dialog Overlay
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
