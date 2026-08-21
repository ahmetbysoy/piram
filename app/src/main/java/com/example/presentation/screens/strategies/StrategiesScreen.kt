package com.example.presentation.screens.strategies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.theme.BgDark
import com.example.core.theme.BorderDark
import com.example.core.theme.BuyGreen
import com.example.core.theme.CardDark
import com.example.core.theme.NeonCyan
import com.example.core.theme.NeonPink
import com.example.core.theme.PinkPastel
import com.example.core.theme.PurpleDark
import com.example.core.theme.PurplePastel
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.TextSecondary
import com.example.domain.engine.strategy.StrategyCategory
import com.example.domain.model.SignalType

@Composable
fun StrategiesScreen(
    viewModel: StrategiesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val filteredItems = if (uiState.selectedCategory == null) {
        uiState.items
    } else {
        uiState.items.filter { it.category == uiState.selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("strategies_screen")
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "QUANT STRATEGIES (20)",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Real-time Mathematical Inference Engine",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PurplePastel.copy(alpha = 0.2f))
                    .border(1.dp, PurplePastel, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = uiState.activeSymbol,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PurplePastel
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryChip(
                    label = "All (20)",
                    isSelected = uiState.selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) }
                )
            }
            items(StrategyCategory.values()) { category ->
                val count = uiState.items.count { it.category == category }
                CategoryChip(
                    label = "${category.label} ($count)",
                    isSelected = uiState.selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Strategy Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("strategies_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredItems, key = { it.id }) { item ->
                StrategyCard(
                    item = item,
                    onToggle = { viewModel.toggleStrategy(item.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PurplePastel else SurfaceDark)
            .border(1.dp, if (isSelected) PurplePastel else BorderDark, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) BgDark else TextSecondary
        )
    }
}

@Composable
private fun StrategyCard(
    item: StrategyItemUiState,
    onToggle: () -> Unit
) {
    val result = item.result
    val (signalColor, signalBg, signalLabel) = when (result.signal) {
        SignalType.STRONG_BUY -> Triple(BuyGreen, BuyGreen.copy(alpha = 0.2f), "STRONG BUY")
        SignalType.BUY -> Triple(BuyGreen, BuyGreen.copy(alpha = 0.12f), "BUY")
        SignalType.STRONG_SELL -> Triple(SellRed, SellRed.copy(alpha = 0.2f), "STRONG SELL")
        SignalType.SELL -> Triple(SellRed, SellRed.copy(alpha = 0.12f), "SELL")
        SignalType.NEUTRAL -> Triple(TextSecondary, SurfaceDark, "NEUTRAL")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (item.isEnabled) BorderDark else BorderDark.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .testTag("strategy_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isEnabled) CardDark else CardDark.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isEnabled) TextPrimary else TextMuted
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.category.label.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePastel
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Signal Badge
                    if (item.isEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(signalBg)
                                .border(1.dp, signalColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = signalLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = signalColor
                            )
                        }
                    }

                    // Win-rate rozeti (performans izleyici #21)
                    if (item.isEnabled && item.resolved > 0) {
                        val wr = item.winRate
                        val wrText = if (wr != null) "${"%.0f".format(wr * 100)}%(${item.resolved})" else "…(${item.resolved})"
                        val wrColor = when {
                            wr == null -> TextMuted
                            wr >= 0.5 -> BuyGreen
                            else -> SellRed
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = wrText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = wrColor
                            )
                        }
                    }

                    Switch(
                        checked = item.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgDark,
                            checkedTrackColor = PurplePastel,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDark
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (item.isEnabled) {
                Spacer(modifier = Modifier.height(10.dp))

                // Reasoning & Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = result.reasoning,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Score: ${if (result.score > 0) "+" else ""}${"%.2f".format(result.score)}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = signalColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Confidence indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Confidence: ${"%.0f".format(result.confidence * 100)}%",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                    LinearProgressIndicator(
                        progress = { result.confidence.toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PurplePastel,
                        trackColor = SurfaceDark
                    )
                }
            }
        }
    }
}
