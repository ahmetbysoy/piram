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

## Phase 6: Notional & Anlatı Katmanı (Completed — piramit portu)
- [x] `SignalConfig` — merkezi motor eşikleri
- [x] USDT notional katmanlama (`Order.value = fiyat × adet`; eşik aralığı 100 USDT → 1M USDT)
- [x] `AdaptiveEdges` — coin'in notional dağılımından yüzdelik eşik (histerezisli)
- [x] `DivergenceEngine` — toplama/boşaltma anlatısı + `FlowNarrative` satırı
- [x] `WindowLedger` — 1sn dilimli defter; timeframe (1DK/5DK/15DK/Açılış) gerçek pencere
- [x] Likidasyon akışı — fapi `!forceOrder@arr` + `LiquidationBanner` + haptik

## Phase 7: OI & Radar (Completed)
- [x] Açık pozisyon (OI) + `oiState` ("OI yoksa yalan yok") — divergence dipnotu + VenueStrip OI hücresi
- [x] Radar ekranı (tüm piyasa taraması, `!miniTicker@arr`, PCT/VOL sıralama, sembol seçimi)

## Phase 8: Sinyal Günlüğü & Temizlik (Completed)
- [x] Sinyal günlüğü + isabet oranı (Room tabanlı journal, later5/15/60)
- [x] Kod temizliği: şablon artıkları + kullanılmayan bağımlılıklar (Moshi/Retrofit/Firebase/Roborazzi/UI-test)

## Phase 9: Format & Sembol Arama (Completed)
- [x] tickSize'a göre fiyat formatı (altcoin'lerde doğru hane)
- [x] Aranabilir sembol listesi + tohum fallback (`SymbolRegistry` + `ExchangeInfoClient`)

## Phase 10: Derinlik Isı Haritası (Completed)
- [x] Order book heatmap + kitap dengesizliği (`BookProfile` + `DepthHeatmap`)

## Phase 11: Performans & Kalite (Completed — kod analizi P1+P2)
- [x] BurstDetector O(n) → O(1) amortized (side pencereler + kayan toplam)
- [x] Ölü kod: rsi / bandwidthPct / spreadPct skora entegre edildi; MeanReversion kategori fix
- [x] docs/KOD_ANALIZI.md — 50 öneri + aksiyon takibi

## Phase 12: Strateji Performans İzleyici (Completed — #21)
- [x] StrategyPerformanceTracker + adaptif ağırlıklandırma (executeAll)
- [x] StrategiesScreen win-rate rozeti

## Phase 13: Layout Zıplama Düzeltmeleri (Completed — UX)
- [x] Sabit yükseklik rezervi (FlowNarrative/LiquidationBanner/SignalJournalCard) + placeholder
- [x] StrategiesViewModel 4fps throttle (.sample) + animateContentSize + maxLines
- [x] "GÜÇLÜ ÜSTTE" sıralama + dinamik başlık + kategori sayaç precompute

## Phase 14: Eğlenceli Katman — İlk Dilim (Completed)
- [x] MarketMood (emoji+etiket) · StreakStats (seri) · StoryGenerator ("kanka özet") · MoodStrip

## Phase 15: Teknik Borç (Completed)
- [x] DRY: `SignalThresholds` (sinyal eşikleri + güven formülü tek noktada)
- [x] DepthAggregator gerçek seviye birleştirme (consolidated book)
- [x] DivergenceEngine numLayers-bağımlı topFrom/bottomTo

## Phase 16: Zıplama Kalıntıları + Strateji Tab Hiyerarşisi (Completed)
- [x] ConsensusHeader satır fix + MoodStrip string bug + SignalJournalCard sabit 3 satır
- [x] StrategyCard alpha fade + sıralama toggle ayrımı + kategori renkleri + sticky header + emoji

## Phase 17: Eğlenceli Katman 2 (Completed)
- [x] MarketPersonality (#20) + RektMeter/LiquidationTracker (#17) + WhaleRetailBoard/ScoreboardBar (#19)

## Phase 18: Görsel İnceleme Tur 5 (Completed)
- [x] Kategori rozeti softWrap + GÜÇLÜ global sıralama + VenueStrip fade
- [x] Kanka özet bilgi sıralaması + boş bucket soluk render

## Phase 19: 15 Öneri Uygulaması (Completed)
- [x] 3 yeni strateji (WhaleFootprint, RoundNumberMagnet, LiquidationCascade) → 23 strateji
- [x] conflictResolver (kategori ağırlığı) + AbsorptionIndex + IcebergDetector
- [x] PainScore + CalmBeforeStorm + PersonalityHistory

## Phase 20: Funding + Lead-Lag (Completed)
- [x] FundingRateSqueeze (#2) + FundingRateClient
- [x] ExchangeLeadLag (#5) + venueTimes

## Phase 21: Çapraz Zaman + Oyun (Completed)
- [x] MultiTimeframeConsensus (#7) + NextCandleGame (#10)

## Phase 22: Push Bildirimler (Completed)
- [x] NotificationHelper + POST_NOTIFICATIONS + runtime izin + Settings toggle + whale/burst bildirim

## Phase 23: explainSignal + Redundancy (Completed — 15 öneri tamam)
- [x] SignalExplainer (#9) + StrategyCorrelation (#8, redundancy cezası)

## Phase 24: 24 Saatlik Ticker (Completed)
- [x] Ticker24h + TickerStatsCard + 30sn poll (ölü alanlar canlandı)

## Phase 25: Ses + Güven Animasyonu (Completed)
- [x] SoundController (#12) + Settings toggle
- [x] PulseBar (#14) + ConsensusHeader güven barı

## Phase 26: Next (Backlog)
- [ ] Trade replay/backtest
