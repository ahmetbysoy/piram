package com.example.presentation.screens.radar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.data.repository.MarketDataRepository
import com.example.domain.model.MiniTickerRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RadarSort { PCT, VOL }

data class RadarUiState(
    val rows: List<MiniTickerRow> = emptyList(),
    val sort: RadarSort = RadarSort.PCT,
    val connected: Boolean = false
)

/**
 * Tüm piyasa taraması (radar): `!miniTicker@arr` üzerinden USDT perp'lerinin
 * 24s değişim ve quote hacmi. Satıra basınca aktif sembol değişir.
 */
class RadarViewModel(
    application: Application,
    private val repository: MarketDataRepository,
    private val preferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.subscribeMiniTickers().collect { all ->
                val filtered = all.filter { it.symbol.endsWith("USDT") }
                _uiState.value = _uiState.value.copy(
                    rows = sortRows(filtered, _uiState.value.sort),
                    connected = true
                )
            }
        }
    }

    fun setSort(sort: RadarSort) {
        _uiState.value = _uiState.value.copy(
            sort = sort,
            rows = sortRows(_uiState.value.rows, sort)
        )
    }

    fun pickSymbol(symbol: String) {
        viewModelScope.launch {
            preferencesRepository.updateActiveSymbol(symbol)
        }
    }

    private fun sortRows(rows: List<MiniTickerRow>, sort: RadarSort): List<MiniTickerRow> = when (sort) {
        RadarSort.PCT -> rows.sortedByDescending { it.changePct }
        RadarSort.VOL -> rows.sortedByDescending { it.quoteVol }
    }
}
