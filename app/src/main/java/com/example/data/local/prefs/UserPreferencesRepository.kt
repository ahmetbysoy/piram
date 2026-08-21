package com.example.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hft_pyramid_preferences")

data class UserPreferences(
    val activeSymbol: String = "BTCUSDT",
    val enabledExchanges: Set<String> = setOf("Binance", "Bybit", "OKX"),
    val decayFactor: Float = 0.15f,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val timeframe: String = "1M",
    val pyramidLayers: Int = 8
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ACTIVE_SYMBOL = stringPreferencesKey("active_symbol")
        val ENABLED_EXCHANGES = stringSetPreferencesKey("enabled_exchanges")
        val DECAY_FACTOR = floatPreferencesKey("decay_factor")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val TIMEFRAME = stringPreferencesKey("timeframe")
        val PYRAMID_LAYERS = intPreferencesKey("pyramid_layers")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            activeSymbol = preferences[PreferencesKeys.ACTIVE_SYMBOL] ?: "BTCUSDT",
            enabledExchanges = preferences[PreferencesKeys.ENABLED_EXCHANGES] ?: setOf("Binance", "Bybit", "OKX"),
            decayFactor = preferences[PreferencesKeys.DECAY_FACTOR] ?: 0.15f,
            hapticEnabled = preferences[PreferencesKeys.HAPTIC_ENABLED] ?: true,
            soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: false,
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: false,
            timeframe = preferences[PreferencesKeys.TIMEFRAME] ?: "1M",
            pyramidLayers = preferences[PreferencesKeys.PYRAMID_LAYERS] ?: 8
        )
    }

    suspend fun updateActiveSymbol(symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_SYMBOL] = symbol.uppercase().trim()
        }
    }

    suspend fun setExchangeEnabled(exchange: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.ENABLED_EXCHANGES]?.toMutableSet() ?: mutableSetOf("Binance", "Bybit", "OKX")
            if (enabled) current.add(exchange) else current.remove(exchange)
            preferences[PreferencesKeys.ENABLED_EXCHANGES] = current
        }
    }

    suspend fun updateDecayFactor(decay: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DECAY_FACTOR] = decay
        }
    }

    suspend fun updateHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun updateTimeframe(timeframe: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIMEFRAME] = timeframe
        }
    }
}
