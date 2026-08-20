# HFT Pyramid Terminal - Roadmap

## Phase 1: Foundation & Core Math (Completed)
- [x] Strict logarithmic binning and bucket manager
- [x] Exponential decay functions and lerp smoothing
- [x] Real-time burst and cluster detector

## Phase 2: Quantitative Strategies (Completed)
- [x] 20 Mathematical Quantitative Strategies across 5 categories
- [x] Consensus scoring engine with dynamic weights
- [x] Technical indicator formulas (VWAP, OFI, RSI, MACD, Bollinger Bands)

## Phase 3: Networking & Data Persistence (Completed)
- [x] Live WebSocket clients for Binance, Bybit, OKX, Kraken, KuCoin
- [x] Reconnection policy with exponential backoff and jitter
- [x] Room SQLite database with TradeDao and TradeEntity
- [x] DataStore preferences for symbol, venues, and parameters

## Phase 4: UI & Visual Engine (Completed)
- [x] Jetpack Compose Canvas dual-sided pyramid renderer (60 FPS)
- [x] Real-time scrolling whale ticker tape
- [x] Tooltip overlay for layer-by-layer inspection
- [x] Strategy management and configuration screens
- [x] Multi-sensory haptic pulse feedback

## Phase 5: Multi-Venue Depth & Cross-Exchange Aggregation (Completed)
- [x] Real order book `depth()` streams for Bybit, OKX, Kraken, KuCoin (replaced empty stubs)
- [x] KuCoin public WebSocket bullet-token handshake (trades + depth)
- [x] `DepthAggregator` — consolidated multi-venue L2 book (best-first, capped)
- [x] Live cross-venue last-price feed wired into `exchangePrices` (activates Statistical Arbitrage)
- [x] `OneMinuteVolumeTracker` — rolling 60s buy/sell volume for the 1M FLOW metric
- [x] `VenueStrip` UI — cross-venue spread, 1M flow, per-venue price chips
- [x] Cleanup: removed template leftovers (unused ui/theme package, Example tests, stale screenshot)

## Phase 6: Next (Backlog)
- [ ] 24h ticker stats (high/low/volume/change) per venue
- [ ] Aggregated order book heatmap + depth imbalance chart
- [ ] Trade replay & offline backtest harness
- [ ] Push-notification whale/burst alerts
- [ ] Per-strategy backtest metrics and parameter tuning UI
