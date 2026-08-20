# HFT Pyramid Terminal - Changelog

## [1.1.0] - Multi-Venue Depth & Cross-Exchange Aggregation

### Added
- Real order book `depth()` streams for Bybit, OKX, Kraken, and KuCoin (previously empty stubs).
- KuCoin public WebSocket bullet-token handshake for both trades and depth.
- `DepthAggregator` — consolidated multi-venue L2 book (best-first, level-capped).
- `OneMinuteVolumeTracker` — rolling 60-second buy/sell volume window.
- `VenueStrip` UI — cross-venue spread (bps), 1M buy/sell flow, live book count, per-venue price chips.

### Fixed
- Cross-venue last prices now feed `MarketSnapshot.exchangePrices`, activating the
  Statistical Arbitrage strategy (previously always received an empty map).
- Removed template leftovers: unused `ui/theme` package, Example unit/robolectric/instrumented
  tests, and the stale `greeting.png` screenshot.

## [1.0.0] - Production Release

### Added
- Real-time multi-exchange WebSocket engine (Binance, Bybit, OKX, Kraken, KuCoin).
- Mathematical MicroBucket volume aggregation with logarithmic layer sizing.
- High-frequency burst cluster detector with Z-score intensity metric.
- 20 complete quantitative strategies across 5 categories with zero placeholders.
- Weighted strategy consensus engine with confidence scoring.
- Hardware-accelerated Jetpack Compose Canvas dual-sided pyramid visualizer.
- High-priority whale print ticker tape.
- Room SQLite persistence for offline trade indexing.
- DataStore reactive preferences repository.
- Multi-sensory haptic vibration engine for whale and burst alerts.
- Pastel Cyberpunk Material 3 theme.
