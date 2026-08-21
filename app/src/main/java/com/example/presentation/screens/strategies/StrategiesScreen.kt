package com.example.presentation.screens.strategies

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.theme.BgDark
import com.example.core.theme.BorderDark
import com.example.core.theme.BuyGreen
import com.example.core.theme.CardDark
import com.example.core.theme.NeonCyan
import com.example.core.theme.NeonPink
import com.example.core.theme.NeonYellow
import com.example.core.theme.PurplePastel
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.SurfaceElevated
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.TextSecondary
import com.example.core.theme.WhaleGold
import com.example.domain.engine.MarketMood
import com.example.domain.engine.strategy.StrategyCategory
import com.example.domain.model.SignalType
import kotlin.math.abs

/** Kategori başına sabit aksan rengi. */
private fun categoryColor(category: StrategyCategory): Color = when (category) {
    StrategyCategory.TREND -> NeonCyan
    StrategyCategory.MOMENTUM -> NeonYellow
    StrategyCategory.MICROSTRUCTURE -> NeonPink
    StrategyCategory.VOLATILITY -> PurplePastel
    StrategyCategory.ARBITRAGE -> WhaleGold
}

@OptIn(ExperimentalFoundationApi::class)
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

    // Sinyale göre sıralama: en güçlü (BUY/SELL) üstte, NEUTRAL en altta
    val displayItems = if (uiState.sort == StrategySort.SIGNAL) {
        filteredItems.sortedByDescending { abs(it.result.score) }
    } else {
        filteredItems
    }

    val grouped = displayItems.groupBy { it.category }
    val orderedCategories = StrategyCategory.values().filter { grouped.containsKey(it) }
    val moodEmoji = MarketMood.emoji(uiState.consensus?.consensusStrength ?: 0.0)

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
                    text = "QUANT STRATEGIES (${uiState.items.size})",
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = moodEmoji, fontSize = 18.sp)
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Kategori filtreleri (renkli) + ayrı sıralama düğmesi
        Row(verticalAlignment = Alignment.CenterVertically) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChip(
                        label = "All (${uiState.items.size})",
                        isSelected = uiState.selectedCategory == null,
                        accent = PurplePastel,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }
                items(StrategyCategory.values()) { category ->
                    val count = uiState.categoryCounts[category] ?: 0
                    CategoryChip(
                        label = "${category.label} ($count)",
                        isSelected = uiState.selectedCategory == category,
                        accent = categoryColor(category),
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            SortToggle(
                isSignal = uiState.sort == StrategySort.SIGNAL,
                onClick = { viewModel.toggleSort() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Strategy Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("strategies_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // GÜÇLÜ sıralamada global sıralama geçerli (kategori gruplaması devre dışı):
            // en güçlü sinyal kategori fark etmeksizin en üstte.
            val groupByCategory = uiState.selectedCategory == null && uiState.sort != StrategySort.SIGNAL
            if (groupByCategory) {
                orderedCategories.forEach { category ->
                    val catItems = grouped[category] ?: return@forEach
                    val bull = catItems.count {
                        it.result.signal == SignalType.BUY || it.result.signal == SignalType.STRONG_BUY
                    }
                    val bear = catItems.count {
                        it.result.signal == SignalType.SELL || it.result.signal == SignalType.STRONG_SELL
                    }
                    stickyHeader(key = "hdr_${category.name}") {
                        CategoryHeader(category, bull, bear)
                    }
                    items(catItems, key = { it.id }) { item ->
                        StrategyCard(
                            item = item,
                            onToggle = { viewModel.toggleStrategy(item.id) }
                        )
                    }
                }
            } else {
                items(displayItems, key = { it.id }) { item ->
                    StrategyCard(
                        item = item,
                        onToggle = { viewModel.toggleStrategy(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: StrategyCategory, bull: Int, bear: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgDark)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(categoryColor(category))
        )
        Text(
            text = category.label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = categoryColor(category)
        )
        Text(
            text = "▲ $bull  ▼ $bear",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )
    }
}

@Composable
private fun SortToggle(isSignal: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSignal) SurfaceElevated else SurfaceDark)
            .border(1.dp, if (isSignal) NeonCyan else BorderDark, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("sort_toggle")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "⇅", fontSize = 12.sp, color = NeonCyan)
            Text(
                text = if (isSignal) "GÜÇLÜ" else "VARSAYILAN",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSignal) NeonCyan else TextSecondary
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accent.copy(alpha = 0.2f) else SurfaceDark)
            .border(1.dp, if (isSelected) accent else BorderDark, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accent else TextSecondary
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
    val absScore = abs(result.score)
    val isStrong = absScore >= 0.45
    val accent = categoryColor(item.category)
    val borderColor = when {
        !item.isEnabled -> BorderDark.copy(alpha = 0.3f)
        isStrong -> signalColor
        else -> BorderDark
    }
    val borderWidth = if (isStrong && item.isEnabled) 1.5.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .testTag("strategy_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isEnabled) CardDark else CardDark.copy(alpha = 0.6f)
        )
    ) {
        Column {
            // Kategori aksan çizgisi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accent)
            )

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
                                color = if (item.isEnabled) TextPrimary else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accent.copy(alpha = 0.15f))
                                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.category.label.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    softWrap = false
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

                        // Win-rate rozeti — her zaman rezerve (yatay zıplama yok)
                        if (item.isEnabled) {
                            val wr = item.winRate
                            val wrText = when {
                                item.resolved == 0 -> "—"
                                wr == null -> "…(${item.resolved})"
                                else -> "${"%.0f".format(wr * 100)}%(${item.resolved})"
                            }
                            val wrColor = when {
                                item.resolved == 0 -> TextMuted
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

                // Detay bloğu — devre dışıyken soluklaşır, yükseklik sabit kalır (zıplama yok)
                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (item.isEnabled) 1f else 0.25f
                    }
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
}
