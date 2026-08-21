package com.example.presentation.screens.settings

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.input.ImeAction
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
import com.example.core.theme.PurplePastel
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.TextSecondary
import com.example.domain.model.ConnectionState

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val popularPresets = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT", "ADAUSDT", "AVAXUSDT")
    val allExchanges = listOf("Binance", "Bybit", "OKX", "Kraken", "KuCoin")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Header
        Text(
            text = "TERMINAL CONFIGURATION",
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Exchange WebSockets, Market Feeds & HFT Parameters",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: Active Symbol Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TARGET ASSET SYMBOL",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePastel,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Stream any crypto pair from live exchange order books",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.symbolInput,
                        onValueChange = { viewModel.onSymbolInputChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("symbol_input_field"),
                        placeholder = { Text("e.g. BTCUSDT", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePastel,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PurplePastel
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.applySymbol() })
                    )

                    Button(
                        onClick = { viewModel.applySymbol() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePastel,
                            contentColor = BgDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("apply_symbol_button")
                    ) {
                        Text("APPLY", fontWeight = FontWeight.Bold)
                    }
                }

                // Arama önerileri (registry'den)
                if (uiState.searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        uiState.searchResults.take(8).forEach { meta ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDark)
                                    .clickable { viewModel.applySymbol(meta.symbol) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("symbol_suggestion_${meta.symbol}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = meta.symbol,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = meta.tickSize,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(popularPresets) { preset ->
                        val isSelected = preset == uiState.preferences.activeSymbol
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PurplePastel else SurfaceDark)
                                .border(1.dp, if (isSelected) PurplePastel else BorderDark, RoundedCornerShape(8.dp))
                                .clickable { viewModel.applySymbol(preset) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BgDark else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: Multi-Exchange Feeds
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EXCHANGE WEBSOCKET CLIENTS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePastel,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Toggle multi-venue order flow aggregation",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                allExchanges.forEach { exchange ->
                    val isEnabled = uiState.preferences.enabledExchanges.contains(exchange)
                    val status = uiState.exchangeStatuses.find { it.exchange == exchange }
                    val stateColor = when (status?.state) {
                        is ConnectionState.Connected -> BuyGreen
                        is ConnectionState.Connecting, is ConnectionState.Reconnecting -> Color(0xFFFFD600)
                        else -> SellRed
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isEnabled) stateColor else TextMuted)
                            )

                            Column {
                                Text(
                                    text = exchange,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEnabled) TextPrimary else TextMuted
                                )
                                Text(
                                    text = "${status?.messageCount ?: 0} msgs received",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.toggleExchange(exchange) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BgDark,
                                checkedTrackColor = PurplePastel,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceDark
                            ),
                            modifier = Modifier.testTag("switch_exchange_$exchange")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: Visual Decay & Haptics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ENGINE TUNING & SENSORY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePastel,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Decay Factor Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Exponential Decay Rate", fontSize = 13.sp, color = TextPrimary)
                    Text(
                        "${"%.2f".format(uiState.preferences.decayFactor)}x",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Slider(
                    value = uiState.preferences.decayFactor,
                    onValueChange = { viewModel.updateDecayFactor(it) },
                    valueRange = 0.05f..0.40f,
                    colors = SliderDefaults.colors(
                        thumbColor = PurplePastel,
                        activeTrackColor = PurplePastel,
                        inactiveTrackColor = SurfaceDark
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Haptic Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Whale Alert Vibration", fontSize = 13.sp, color = TextPrimary)
                        Text("Haptic pulse on institutional volume trades", fontSize = 10.sp, color = TextMuted)
                    }

                    Switch(
                        checked = uiState.preferences.hapticEnabled,
                        onCheckedChange = { viewModel.toggleHaptic() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgDark,
                            checkedTrackColor = PurplePastel,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDark
                        ),
                        modifier = Modifier.testTag("switch_haptics")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Push Bildirim Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Whale & Salvo Bildirimleri", fontSize = 13.sp, color = TextPrimary)
                        Text("Kurumsal emir ve salvo uyarıları (30 sn arayla)", fontSize = 10.sp, color = TextMuted)
                    }

                    Switch(
                        checked = uiState.preferences.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgDark,
                            checkedTrackColor = PurplePastel,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDark
                        ),
                        modifier = Modifier.testTag("switch_notifications")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 4: Database & Cache Maintenance
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STORAGE & PERSISTENCE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePastel,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Room SQLite Database", fontSize = 13.sp, color = TextPrimary)
                        Text("Persistent L2 trade history", fontSize = 10.sp, color = TextMuted)
                    }

                    Button(
                        onClick = { viewModel.clearDatabase() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceDark,
                            contentColor = SellRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SellRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("clear_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Cache",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Cache", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
