# HFT Pyramid Terminal - Changelog

## [1.4.0] - tickSize Fiyat Formatı + Sembol Arama

### Added
- `SymbolRegistry` + `SymbolMeta` — exchangeInfo'dan sembol listesi/tickSize (tohum liste fallback),
  rank skorlu arama, `resolve` (ETH → ETHUSDT).
- `ExchangeInfoClient` — Binance spot exchangeInfo REST.
- `MathUtils.formatPrice(price, decimals)` — tickSize hanesine göre fiyat (altcoin'lerde doğru hane).
- Settings'e aranabilir sembol önerileri; Radar ve tüm fiyat görünümleri tickSize hanesi kullanır.

## [1.3.0] - Sinyal Günlüğü + Kod Temizliği

### Added
- Sinyal günlüğü (journal): Room tabanlı `JournalEntity` + `JournalDao`; toplama/boşaltma sinyalleri
  kaydedilir (60 sn spam koruması), 5/15/60 dakika sonraki fiyatlar işaretlenir.
- `SignalJournalCard` — isabet oranı (later15) + son sinyaller.

### Removed (temizlik)
- Şablon artıkları: `metadata.json`, `assets/.aistudio/`, `.env.example`, `ExchangeDtos.kt`.
- Kullanılmayan bağımlılıklar: Moshi(+codegen), Retrofit, logging-interceptor, Firebase
  (ai/appcheck/bom + google-services/secrets pluginleri), Roborazzi, Compose UI-test/Espresso/
  androidTest, kotlinx-coroutines-test, tooling/preview.
- `gradle/libs.versions.toml` yalnızca kullanılan girişlere indirildi.

## [1.2.0] - Notional Engine, Anlatı, Likidasyon, OI & Radar

### Changed
- Katmanlama artık **USDT notional** (`fiyat × adet`) üzerinden; adet (qty) kullanılmıyor.
  `MicroBucket` / `MicroBucketManager` / `LayerAggregate` alanları notional'a geçirildi.
- Varsayılan eşik aralığı 0.001–25 adet yerine **100 USDT → 1M USDT** (coin bağımsız).
- 1 dakikalık akış (1M FLOW) artık notional üzerinden toplanıyor.

### Added
- `SignalConfig` — tüm motor eşikleri tek yerde.
- `DivergenceEngine` — toplama/boşaltma anlatısı (büyükler vs küçükler + fiyat `tanh`), `FlowNarrative` satırı.
- `AdaptiveEdges` — coin'in notional dağılımından yüzdelik eşik aralığı (histerezisli).
- `WindowLedger` — 1 saniyelik dilimli pencere defteri; timeframe (1DK/5DK/15DK/Açılış) gerçek pencere.
- Likidasyon akışı — Binance USD-M `!forceOrder@arr` + `Liquidation` modeli + `LiquidationBanner` + haptik.
- Açık pozisyon (OI) — fapi `openInterest` REST + `oiState` durum makinesi (bekliyor/ok/yok/eski);
  divergence cümlesine OI dipnotu, VenueStrip'te "OI" hücresi.
- Radar ekranı — `!miniTicker@arr` üzerinden tüm USDT perp'lerinin 24s değişim + hacim listesi,
  PCT/VOL sıralama, satıra basınca sembol değiştirme; Navigation'a 4. sekme.

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
