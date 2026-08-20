# HFT Pyramid Terminal

An institutional-grade High-Frequency Trading (HFT) and Market Microstructure visualizer for Android, built with Kotlin, Jetpack Compose Canvas, Room SQLite, DataStore, and multi-venue WebSocket streaming.

## Highlights
- **Mathematical Order Flow Aggregation**: MicroBuckets dynamically bin live trades into logarithmic tiers.
- **20 Quantitative Strategies**: Trend, Momentum, Order Flow Imbalance, Volatility, and Statistical Arbitrage models running live consensus scoring.
- **Multi-Exchange Ingestion**: Direct WebSocket streams from Binance, Bybit, OKX, Kraken, and KuCoin.
- **Jitter-Free 60 FPS Canvas Rendering**: Smooth exponential decay and display lerp for fluid visual tracking.
- **Whale Footprint Alerts**: Real-time ticker tape and tactile haptic pulses on institutional trades.
- **Local Persistence**: Room SQLite storage for continuous trade history and offline analysis.

## Docs

- [PORT_NOTLARI](docs/PORT_NOTLARI.md) — `piramit` (web) projesinden Android'e taşınacak her şey
- [ARCHITECTURE](ARCHITECTURE.md) · [DATA_FLOW](DATA_FLOW.md) · [API](API.md) · [WALKTHROUGH](WALKTHROUGH.md)

## Tech Stack
- Kotlin & Jetpack Compose
- Room Database & SQLite
- AndroidX DataStore Preferences
- OkHttp WebSocket Streaming
- Moshi JSON Serialization
- Material Design 3 (Pastel Cyberpunk Theme)
