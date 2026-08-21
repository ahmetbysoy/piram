# HFT Pyramid Terminal Architecture

## System Overview
HFT Pyramid Terminal is an institutional-grade High-Frequency Trading (HFT) Market Microstructure and Order Flow visualizer built on Android with Kotlin, Jetpack Compose Canvas (60 FPS), Room SQLite, and multi-venue WebSocket streaming.

```
┌──────────────────────────────────────────────────────────────┐
│                    Exchange WebSockets                      │
│        (Binance, Bybit, OKX, Kraken, KuCoin)                 │
└──────────────────────────────┬───────────────────────────────┘
                               │ Live Trade & Depth Streams
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                  MarketDataRepository                        │
│    - Concurrent flow merging                                 │
│    - Exponential backoff reconnection policy                 │
│    - Room Database persistence (TradeEntity/TradeDao)        │
└──────────────────────────────┬───────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│     MicroBucketManager       │ │       BurstDetector          │
│ - Logarithmic layer mapping  │ │ - Sliding window clustering  │
│ - Dynamic volume decay       │ │ - Z-score intensity scoring  │
│ - Display lerp smoothing     │ │ - Whale footprint tracker    │
└──────────────┬───────────────┘ └──────────────┬───────────────┘
               │                                │
               └───────────────┬────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                   20-Strategy Quant Engine                   │
│   - Trend, Momentum, Microstructure, Volatility, Arbitrage    │
│   - Weighted consensus scoring & confidence index            │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                    PyramidViewModel                          │
│   - StateFlow<PyramidUiState> @ ~12-60 FPS updates           │
│   - Multi-sensory Haptic engine triggers                     │
└──────────────────────────────┬───────────────────────────────┘
                               │
               ┌───────────────┼───────────────┐
               ▼               ▼               ▼
┌────────────────────┐ ┌───────────────┐ ┌─────────────────────┐
│   PyramidScreen    │ │StrategiesScrn │ │   SettingsScreen    │
│ - Dual Canvas Bars │ │- 20 Live Cards│ │ - Multi-Venue Togl  │
│ - Ticker Tape      │ │- Signal Meters│ │ - Symbol Override   │
│ - Layer Tooltip    │ │- Configurable │ │ - Decay Tuning      │
└────────────────────┘ └───────────────┘ └─────────────────────┘
```

## Core Modules
1. **`core/`**: Math utilities (logarithmic binning, decay lerp, EWMA, indicators) and Pastel Cyberpunk Material 3 theme.
2. **`data/`**:
   - `remote/ws/`: Real-time WebSocket clients for Binance, Bybit, OKX, Kraken, KuCoin, plus futures
     `!forceOrder@arr` (likidasyon) and `!miniTicker@arr` (radar) clients.
   - `remote/rest/`: Binance fapi `openInterest` client.
   - `local/db/`: Room database (`AppDatabase`, `TradeDao`, `TradeEntity`, `JournalDao`, `JournalEntity`).
   - `local/prefs/`: DataStore `UserPreferencesRepository`.
3. **`domain/`**:
   - `bucket/`: MicroBucket & MicroBucketManager (USDT notional) with smooth display interpolation.
   - `burst/`: High-frequency cluster detection and trade velocity calculation.
   - `strategy/`: 20 quantitative strategies implementing the `Strategy` interface and `StrategyEngine` consensus evaluator.
   - `engine/`: `SignalConfig`, `AdaptiveEdges`, `DivergenceEngine`, `WindowLedger`, `DepthAggregator`,
     `OneMinuteVolumeTracker`.
4. **`presentation/`**:
   - `screens/pyramid/`: Hardware-accelerated Canvas pyramid renderer, whale ticker, stats cards,
     `FlowNarrative` (toplama/boşaltma), `LiquidationBanner`, `SignalJournalCard`, `VenueStrip`.
   - `screens/strategies/`: Live strategy monitors with category filtering.
   - `screens/radar/`: Full-market miniTicker scan with PCT/VOL sorting and symbol pick.
   - `screens/settings/`: Dynamic symbol switcher, exchange toggles, and decay rate calibration.
