package com.example.presentation.screens.strategies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.engine.strategy.StrategyCategory
import com.example.domain.model.ConsensusResult
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import com.example.presentation.screens.pyramid.PyramidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class StrategyItemUiState(
    val id: String,
    val name: String,
    val description: String,
    val category: StrategyCategory,
    val isEnabled: Boolean,
    val result: StrategyResult
)

data class StrategiesScreenUiState(
    val items: List<StrategyItemUiState> = emptyList(),
    val selectedCategory: StrategyCategory? = null,
    val consensus: ConsensusResult? = null,
    val activeSymbol: String = "BTCUSDT"
)

class StrategiesViewModel(
    application: Application,
    private val pyramidViewModel: PyramidViewModel
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StrategiesScreenUiState())
    val uiState: StateFlow<StrategiesScreenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            pyramidViewModel.uiState.collectLatest { pyramidState ->
                val engine = pyramidViewModel.strategyEngine
                val snapshot = MarketSnapshot(
                    symbol = pyramidState.symbol,
                    currentPrice = pyramidState.currentPrice,
                    trades = pyramidState.whaleOrders, // or recent
                    depth = pyramidState.depth,
                    bursts = pyramidState.activeBursts,
                    orderFlowImbalance = pyramidState.orderFlowImbalance,
                    vwap = pyramidState.vwap,
                    timestamp = System.currentTimeMillis()
                )

                val (results, consensus) = engine.executeAll(snapshot)
                val resultMap = results.associateBy { it.strategyId }

                val items = engine.strategies.map { strategy ->
                    val isEnabled = engine.isStrategyEnabled(strategy.id)
                    val result = resultMap[strategy.id] ?: StrategyResult(
                        strategyId = strategy.id,
                        name = strategy.name,
                        signal = SignalType.NEUTRAL,
                        confidence = 0.5,
                        score = 0.0,
                        reasoning = "Standby"
                    )
                    StrategyItemUiState(
                        id = strategy.id,
                        name = strategy.name,
                        description = strategy.description,
                        category = strategy.category,
                        isEnabled = isEnabled,
                        result = result
                    )
                }

                _uiState.value = _uiState.value.copy(
                    items = items,
                    consensus = consensus,
                    activeSymbol = pyramidState.symbol
                )
            }
        }
    }

    fun toggleStrategy(id: String) {
        pyramidViewModel.strategyEngine.toggleStrategy(id)
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { item ->
                if (item.id == id) item.copy(isEnabled = !item.isEnabled) else item
            }
        )
    }

    fun selectCategory(category: StrategyCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }
}
