# HFT Pyramid Terminal - Changelog

## [1.2.0] - Notional (USDT) Layer Engine

### Changed
- Katmanlama artık **USDT notional** (`fiyat × adet`) üzerinden; adet (qty) kullanılmıyor.
  `MicroBucket` / `MicroBucketManager` / `LayerAggregate` alanları notional'a geçirildi.
- Varsayılan eşik aralığı 0.001–25 adet yerine **100 USDT → 1M USDT** (coin bağımsız).
- 1 dakikalık akış (1M FLOW) artık notional üzerinden toplanıyor.

### Added
- `SignalConfig` — tüm motor eşikleri tek yerde (adaptif eşik, sönüm, loop, divergence sabitleri hazır).

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
