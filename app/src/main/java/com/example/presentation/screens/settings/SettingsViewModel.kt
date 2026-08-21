package com.example.presentation.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.AppDatabase
import com.example.data.local.prefs.UserPreferences
import com.example.data.local.prefs.UserPreferencesRepository
import com.example.data.repository.MarketDataRepository
import com.example.domain.SymbolRegistry
import com.example.domain.model.ExchangeStatus
import com.example.domain.model.SymbolMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettingsScreenUiState(
    val preferences: UserPreferences = UserPreferences(),
    val exchangeStatuses: List<ExchangeStatus> = emptyList(),
    val symbolInput: String = "BTCUSDT",
    val searchResults: List<SymbolMeta> = emptyList(),
    val isDatabaseClearing: Boolean = false,
    val statusMessage: String? = null
)

class SettingsViewModel(
    application: Application,
    private val preferencesRepository: UserPreferencesRepository,
    private val marketDataRepository: MarketDataRepository,
    private val symbolRegistry: SymbolRegistry
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val _uiState = MutableStateFlow(SettingsScreenUiState())
    val uiState: StateFlow<SettingsScreenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collectLatest { prefs ->
                _uiState.value = _uiState.value.copy(
                    preferences = prefs,
                    symbolInput = prefs.activeSymbol
                )
            }
        }

        viewModelScope.launch {
            marketDataRepository.exchangeStatuses.collectLatest { statuses ->
                _uiState.value = _uiState.value.copy(exchangeStatuses = statuses)
            }
        }
    }

    fun onSymbolInputChange(input: String) {
        val clean = input.uppercase().trim()
        _uiState.value = _uiState.value.copy(
            symbolInput = clean,
            searchResults = if (clean.isEmpty()) symbolRegistry.symbols().take(10) else symbolRegistry.search(clean)
        )
    }

    /** Sorguyu registry üzerinden çözer ve aktif sembol yapar. */
    fun applySymbol(symbol: String = _uiState.value.symbolInput) {
        val resolved = symbolRegistry.resolve(symbol) ?: symbol.uppercase().trim()
        if (resolved.isEmpty()) return
        viewModelScope.launch {
            preferencesRepository.updateActiveSymbol(resolved)
            _uiState.value = _uiState.value.copy(
                symbolInput = resolved,
                searchResults = emptyList(),
                statusMessage = "Switched active symbol to $resolved"
            )
        }
    }

    fun toggleExchange(exchange: String) {
        val isCurrentlyEnabled = _uiState.value.preferences.enabledExchanges.contains(exchange)
        viewModelScope.launch {
            preferencesRepository.setExchangeEnabled(exchange, !isCurrentlyEnabled)
        }
    }

    fun updateDecayFactor(decay: Float) {
        viewModelScope.launch {
            preferencesRepository.updateDecayFactor(decay)
        }
    }

    fun toggleHaptic() {
        val current = _uiState.value.preferences.hapticEnabled
        viewModelScope.launch {
            preferencesRepository.updateHapticEnabled(!current)
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDatabaseClearing = true)
            db.tradeDao().deleteOldTrades(System.currentTimeMillis())
            _uiState.value = _uiState.value.copy(
                isDatabaseClearing = false,
                statusMessage = "Cleared local trade cache"
            )
        }
    }
}
