# PİRAM — Kod Analizi & Geliştirme Beyin Fırtınası

> Kaynak: kullanıcı analizi (2026-08-21). Bu doküman bulguları + **aksiyon takibini** tutar.

## Doğrulanan Kritik Tespitler

| # | Tespit | Durum |
|---|---|---|
| 1 | `BurstDetector.processOrder` her trade'de tüm deque'yi 2× `filter` ediyor (O(n)) | ✅ DÜZELTİLDİ |
| 2 | `DivergenceStrategy.rsi` hesaplanıp skora girmiyor (ölü) | ✅ DÜZELTİLDİ (RSI koşula entegre) |
| 3 | `BollingerBandsStrategy.bandwidthPct` skora girmiyor (squeeze sinyali yok) | ✅ DÜZELTİLDİ (squeeze boost) |
| 4 | `MarketMicrostructureStrategy.spreadPct` skora girmiyor (spread compression yok) | ✅ DÜZELTİLDİ (liquidity factor) |
| 5 | `MeanReversionStrategy.category = MOMENTUM` (yanlış etiket) | ✅ DÜZELTİLDİ (VOLATILITY) |
| 6 | Skor eşikleri (0.15/0.45/0.55…) her stratejide kopyalanmış (DRY) | ⬜ Backlog |
| 7 | Confidence hesapları `base + |score|*k` keyfi sabitler | ⬜ Backlog |
| 8 | Stratejiler performans izlemiyor (win-rate/Sharpe yok), adaptif ağırlık yok | ⬜ Backlog (P3) |
| 9 | `DepthAggregator` aynı fiyat seviyesini birleştirmiyor (concat, gerçek aggregate değil) | ⬜ Backlog |
| 10 | `DivergenceEngine` topFrom=6/bottomTo=3 sabit — numLayers'a bağlı olmalı | ⬜ Backlog |
| 11 | `AdaptiveEdges.percentile` interpolasyonsuz | ⬜ Backlog |
| 12 | `WindowLedger.sumWindow` O(n) tarama | ⬜ Backlog (prefix-sum) |
| 13 | `StrategyEngine.strategies` val (runtime plugin yok) | ⬜ Backlog |
| 14 | `TechnicalIndicators.ema` ilk değeri SMA-seed değil (hafif sapma) | ℹ️ Kabul edilebilir |

## 50 Öneri — Durum Haritası (özet)

### A. Yeni Stratejiler (1-10)
1 WhaleFootprint · 2 FundingRateSqueeze · 3 ExchangeLeadLag · 4 SpoofingDetector ·
5 WickRejection · 6 SessionOverlap · 7 LiquidationCascade · 8 OIDivergence ·
9 RoundNumberMagnet · 10 TapeReadingSpeed → **hepsi ⬜ Backlog**

### B. Eğlenceli/Anlatı Katmanı (11-20)
11 MarketMoodEmoji · 12 WhaleAlertSound · 13 generateTradeStory · 14 ConfidenceMeterAnimation ·
15 StreakTracker · 16 PainScoreNarrator · 17 RektMeter · 18 CalmBeforeStormBadge ·
19 WhaleVsRetailScoreboard · 20 MarketPersonalityQuiz → **hepsi ⬜ Backlog (P4)**

### C. Consensus/Engine (21-30)
21 StrategyPerformanceTracker ⭐ · 22 adaptiveWeighting · 23 conflictResolver ·
24 strategyCorrelationMatrix · 25 register/unregisterStrategy · 26 backtestStrategy ·
27 strategyDecayScore · 28 consensusVolatilityBand · 29 multiTimeframeConsensus ·
30 strategyVetoRule → **⬜ Backlog (P3)**

### D. Mikroyapı (31-40)
31 icebergDetector · 32 bookPressureVelocity · 33 tradeSizeDistribution · 34 sweepReplayLog ·
35 vwapBandDeviation · 36 cumulativeDeltaChart · 37 absorptionIndex · 38 orderClusteringScore ·
39 depthImbalanceHistory · 40 crossVenueLeadIndicator → **⬜ Backlog**

### E. Altyapı (41-50)
41 replayHistoricalSession · 42 adaptiveBackoff · 43 perSymbolOverride · 44 autoTagOutcome ·
45 riskProfile · 46 healthScore · 47 exportStrategyReport · 48 multiCoinScanner ·
49 pruneOldSessions · 50 explainSignal → **⬜ Backlog**

## Öncelik Sırası (uygulanacak)

| Sıra | İş | Efor | Durum |
|---|---|---|---|
| P1 | BurstDetector O(n) → O(1) amortized | Küçük | ✅ YAPILDI |
| P2 | Ölü kod (rsi/bandwidthPct/spreadPct) + kategori fix | Küçük | ✅ YAPILDI |
| P3 | StrategyPerformanceTracker (#21) + adaptif ağırlık | Orta-Büyük | ✅ YAPILDI |
| P4 | Eğlenceli/anlatı katmanı (#11-20) | Orta | ⬜ Sıradaki |
| — | DRY: ortak SignalThresholds + confidence | Orta | ⬜ |

### P3 detayı (#21 — uygulandı)
- `StrategyPerformanceTracker` — saf Kotlin, injectable clock, throttle'lı kayıt (5sn/strateji),
  60sn sonra fiyatla sonuçlandırma, win-rate + `weight(id)` (0.3..1.2, soğuk başlangıç 1.0).
- `StrategyEngine.executeAll` — ağırlık artık `confidence × performansAğırlığı`; kötü strateji
  otomatik zayıflar (U5 — konsensüs oynaklığı — bu şekilde azaltılır).
- `StrategiesScreen` — her kartta win-rate rozeti `%60(15)` (yeşil/ kırmızı/ "…" soğuk).
- Not: win-rate oturum içi (in-memory); Room'a kalıcılık backlog. 

## UI Bug Düzeltmeleri (kod incelemesi turu 2 — 2026-08-21)

| # | Tespit | Durum |
|---|---|---|
| U1 | `DepthHeatmap` bar satırı `height(10.dp)` sabit → 8sp Monospace metin taşıyor, fiyat satırları üst üste biniyor | ✅ YAPILDI (`.heightIn(min = 13.dp)`, iki bar da) |
| U2 | `PyramidCanvas.layerHeight` alt sınırı çıplak `16f` piksel (dp değil) → yüksek yoğunlukta etiket çorbası | ✅ YAPILDI (`28.dp.toPx()`) |
| U3 | Canvas, üstündeki 7-8 sabit kartlı `Column` içinde `weight(1f)` ile sıkışıyor | ✅ YAPILDI (Column `verticalScroll` + canvas `height(280.dp)`) |
| U4 | INSTITUTIONAL %85→%0 1dk içinde — decay'in doğal sonucu ama yanıltıcı UX (smoothing floor düşünülebilir) | ⬜ Backlog (UX kararı) |
| U5 | Consensus fiyattan çok oynak (BUY+38→SELL-26, fiyat +%0.04) — #21 StrategyPerformanceTracker ihtiyacını doğruluyor | ✅ P3 ile UYGULANDI (performans ağırlığı) |

## UI/UX İncelemesi (tur 3 — layout zıplaması)

| # | Tespit | Durum |
|---|---|---|
| V1 | `animateContentSize` kullanımı sıfır; `FlowNarrative`/`LiquidationBanner`/`SignalJournalCard` erken-return ile 0dp↔boyut zıplatıyor | ✅ YAPILDI (sabit yükseklik rezervi + placeholder) |
| V2 | `StrategiesViewModel` 12.5fps'te 20 stratejiyi komple yeniden hesaplıyor (flicker) | ✅ YAPILDI (`.sample(250)` → 4fps) |
| V3 | `StrategyCard` switch aç/kapa aniden çöküp genişliyor | ✅ YAPILDI (`animateContentSize(tween(200))`) |
| V4 | `"QUANT STRATEGIES (20)"` hardcoded | ✅ YAPILDI (dinamik `items.size`) |
| V5 | `reasoning` metninde `maxLines` yok (basamak artışında titreşim) | ✅ YAPILDI (`maxLines=1` + ellipsis) |
| V6 | Sinyale göre sıralama yok (20 eşit kart, hiyerarşi yok) | ✅ YAPILDI ("GÜÇLÜ ÜSTTE" sıralama chip'i, varsayılan açık) |
| V7 | Kategori chip sayaçları her recomposition'da yeniden sayılıyor | ✅ YAPILDI (VM'de `categoryCounts` precompute) |
| V8 | Win-rate rozeti aniden Row'a ekleniyor (yatay kayma) | ✅ YAPILDI (her zaman rezerve, "—" boş durumda) |
