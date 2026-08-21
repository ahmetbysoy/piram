# HFT Pyramid Terminal - Changelog

## [1.21.0] - 5 Yeni Strateji + Açık Tema + Pasif/Kararsız Rozetleri

### Added (strateji — toplam 30)
- `WickRejectionStrategy` (#17) — gerçek üst/alt wick oranı ile yön reddi.
- `OIDivergenceStrategy` (#19) — OI+fiyat yönünden yeni pozisyon vs kapanış ayrımı.
- `TapeReadingSpeedStrategy` (#20) — küçükten büyüğe emir geçiş ivmesi.
- `VwapReversionStrategy` (#21) — VWAP sapma z-score ile geri dönüş.
- `FibonacciConfluenceStrategy` (#22) — swing fib seviyeleriyle fiyat çakışması.

### Added (engine)
- `StrategyEngine.isPassive` (#26) — uzun süredir NÖTR stratejiler UI'da soluklaşır.
- `ConsensusVolatility` (#27) — son N konsensüs stdDev'i; "🎢 KARARSIZ" rozeti (ConsensusHeader).

### Added (tema)
- `Color.kt` — açık pastel palet (BgLight, CardLight, BuyMint, SellCoral, koyu mor metin).
- `Theme.kt` — `isSystemInDarkTheme()` ile gerçek açık/koyu şema seçimi (ölü import canlandı).
- STRONG_BUY 🚀 / STRONG_SELL 🐻 emoji rozetleri; kategori chip'leri dolgun pastel.

## [1.20.0] - Final: Ses + Güven Nabız Barı (proje tamamlandı)

### Added
- `SoundController` (#12) — whale = çift bip, salvo = kısa bip (ToneGenerator, ses dosyası yok).
- `UserPreferences.soundEnabled` + Settings'te "Whale & Salvo Ses Efekti" toggle.
- `PulseBar` (#14) — nabız animasyonlu güven ısı çubuğu; ConsensusHeader'a "GÜVEN %" barı eklendi.

> 📌 Not: Backtest/replay bilinçli olarak kapsam dışı bırakıldı (kullanıcı kararı).

## [1.19.0] - 24 Saatlik Ticker İstatistikleri

### Added
- `Ticker24h` + `Ticker24hParser` + `Ticker24hClient` — fapi `ticker/24hr` REST.
- `TickerStatsCard` — 24H High / Low / Δ% / Hacim (sabit yükseklik, placeholder'lı).
- ViewModel — 30 sn'de bir 24s ticker poll; MarketSnapshot'ın ölü `high24h/low24h/volume24h/priceChange24h`
  alanları artık gerçek veriyle besleniyor ("tanımla → besle → göster").

## [1.18.0] - explainSignal + Redundancy Cezası (15 öneri tamam)

### Added
- `SignalExplainer` (#9) — strateji sonucunu insan diline çevirir ("ALIŞ (%65) · EMA9: ..., VWAP: ...");
  StrategiesScreen kartları artık Türkçe açıklama gösterir.
- `StrategyCorrelation` (#8) — strateji yön oy geçmişinden benzerlik tespiti; sürekli aynı yönde
  oylayan benzer stratejilere otomatik ağırlık cezası (0.7). `StrategyEngine` yön geçmişi tutar
  ve ağırlığa redundancy çarpanı ekler.

## [1.17.0] - Push Bildirimleri

### Added
- `NotificationHelper` — whale (🐋) ve salvo (⚡) bildirim kanalları + bildirim basımı
  (izin yoksa veya bildirimler kapalıysa sessiz no-op).
- `POST_NOTIFICATIONS` izni + MainActivity'de API 33+ runtime izin isteği.
- `UserPreferences.notificationsEnabled` + Settings'e "Whale & Salvo Bildirimleri" toggle.
- ViewModel — whale/salvo olaylarında bildirim (30 sn throttle, tercih kapalıysa kapalı).

## [1.16.0] - Çapraz Zaman + Next-Candle Oyunu

### Added
- `MultiTimeframeConsensus` (#7) — 60sn vs oturum konsensüs çakışması ("⏱ 1dk SATIŞ, açılıştan ALIŞ — karışık zaman") MoodStrip çipi.
- `NextCandleGame` (#10) — konsensüse göre 1dk tahmin (🟢/🔴), 60sn sonra doğruluk sayacı + seri ("🎯 3/5 🔥2").

## [1.15.0] - Funding Rate + Exchange Lead-Lag (toplam 25 strateji)

### Added
- `FundingRateSqueezeStrategy` (#2) — aşırı funding + OI artışı ile long/short squeeze riski.
- `ExchangeLeadLagStrategy` (#5) — en son işlem gören borsanın fiyat öncülüğü ile kısa vade yön.
- `FundingRateClient` + `FundingParser` — fapi `premiumIndex` REST.
- `MarketSnapshot` — `fundingRate`, `oiDelta`, `venueTimes`; ViewModel OI poll'üne funding eklendi,
  venue timestamp takibi eklendi.

## [1.14.0] - 15 Öneri Uygulaması (Strateji + Mikroyapı + Eğlence)

### Added (yeni stratejiler — toplam 23)
- `WhaleFootprintStrategy` — ortalama trade'in katları büyüklüğündeki kurumsal giriş izleri.
- `RoundNumberMagnetStrategy` — psikolojik yuvarlak seviyelere çekim etkisi.
- `LiquidationCascadeStrategy` — 60sn likidasyon baskısı + fiyat yönü ile kademeli çözülme riski.

### Added (engine)
- `StrategyEngine` kategori ağırlığı (#6): MICROSTRUCTURE 1.3 / ARBITRAGE 1.1 / TREND 0.9.
- `ConsensusResult.conflict` — güçlü fikir ayrılığında "⚠️ KARIŞIK" rozeti (ConsensusHeader).
- `MarketSnapshot.liquidationNotional60s / liquidationCount60s` beslemesi.

### Added (mikroyapı)
- `AbsorptionIndex` (#15) — yüksek hacim + dar aralık = emilim; LiquidityHunt'a entegre.
- `IcebergDetector` (#14) — dolup-boşalan gizli büyük emir tespiti.

### Added (eğlence)
- `PainScore` (#16) — "long'lar/short'lar acı çekiyor" anlatısı (LiquidationTracker side'lı).
- `CalmBeforeStorm` (#18) — sıkışma + kitap dengesizliği → "🌪️ Fırtına öncesi sessizlik".
- `PersonalityHistory` (#13) — kişilik değişim günlüğü ("📅 ÇILGIN×3" çipi).

## [1.13.0] - Görsel İnceleme Tur 5

### Fixed
- Strateji kartı kategori rozeti dar alanda harf harf dikey wrap oluyordu → `softWrap = false`
  + strateji adı `maxLines=1`/ellipsis.
- "GÜÇLÜ" sıralama artık gerçekten global (kategori gruplaması bypass) — en güçlü sinyal
  kategori fark etmeksizin en üstte.
- VenueStrip sağ kenara alpha fade (kaydırılabilir olduğu artık belli).
- "Kanka özet" cümlesinde kritik yüzde/akış bilgisi başa alındı (ellipsis'e takılmıyor).
- Boş bucket katmanları (notional=0) soluk render (bar + metin) — "yarı boş" hissi.

## [1.12.0] - Eğlenceli Katman 2 (#17/#19/#20)

### Added
- `MarketPersonality` (#20) — coin kişiliği: AGRESİF 🦈 / SİNSİ 🐙 / ÇILGIN 🔥 / SAKİN 😴 / KARARSIZ 🎲 (MoodStrip çipi).
- `RektMeter` (#17) + `LiquidationTracker` — 60sn likidasyon notional'ına göre rekt seviyesi (0-5, 🔥, REKT/KIYIM/SIKINTI) LiquidationBanner'da.
- `WhaleRetailBoard` (#19) + `ScoreboardBar` — kurumsal vs perakende skor tablosu (🐋 vs 🐟, "3-0", tug-of-war bar).

## [1.11.0] - Zıplama Kalıntıları + Strateji Tab Hiyerarşisi

### Fixed
- `ConsensusHeader` — "Top Bull/Top Bear" satırı uzun isimde sarıp kart yüksekliğini değiştiriyordu;
  sabit 14dp + `maxLines=1` + ellipsis.
- `MoodStrip` — `"🔥 $streak.current seri"` string template bug'ı (`toString()` basıyordu) → `${streak.current}`.
- `SignalJournalCard` — her zaman 3 satır + placeholder (animasyon kaldırıldı), yükseklik gerçekten sabit.
- `StrategyCard` — switch kapatınca içerik kaybolup kart çöküyordu; artık `alpha 0.25` ile soluklaşır,
  yükseklik sabit kalır.

### Added (Strateji tab görsel hiyerarşi)
- Sıralama düğmesi ayrı "⇅ GÜÇLÜ" toggle (kategori chip'lerinden ayrıldı).
- Kategori renk kodlaması: TREND=camgöbeği, MOMENTUM=sarı, MICROSTRUCTURE=pembe,
  VOLATILITY=mor, ARBITRAGE=altın — badge + kart üstü aksan çizgisi.
- Güçlü sinyal (|score|≥0.45) kartlara renkli kalın border.
- Kategori bazlı sticky header'lar + ▲/▼ sayaçları.
- Header'a MarketMood emoji rozeti (ana ekranla görsel tutarlılık).

## [1.10.0] - Teknik Borç Temizliği (DRY + Doğruluk)

### Added
- `SignalThresholds` — 20 stratejide kopyalanmış sinyal eşikleri ve güven formülü tek noktada
  (`signalFor(score, strong, weak)` + `confidenceFor(score, base, scale)`); davranış korundu.

### Fixed
- `DepthAggregator` — aynı fiyat seviyesindeki farklı borsa hacimleri artık birleştiriliyor
  (gerçek consolidated book; önceki hâli üst üste bindirilmiş listeydi).
- `DivergenceEngine` — `topFrom=6/bottomTo=3` sabit indeksleri `numLayers`'tan türetiliyor
  (top = üst 2 katman, bottom = alt ~%40); katman sayısı değişse de çalışır.

## [1.9.0] - Ruh Hali Şeridi (Eğlenceli Katman #11/13/15)

### Added
- `MarketMood` — konsensüs gücüne göre emoji (😱🐻😐🐂🚀) + etiket (Panik/Ayı/Kararsız/Boğa/FOMO).
- `StreakStats` — sinyal günlüğünden art arda doğru tahmin serisi (aktif/en uzun/win-rate).
- `StoryGenerator` — "Kanka özet" tek cümlelik Türkçe anlatı (kurumsal-perakende + akış + salvo + VWAP).
- `MoodStrip` — üçünü tek kompakt kartta gösterir (PyramidScreen).

## [1.8.0] - Layout Zıplama Düzeltmeleri (UX)

### Fixed
- `FlowNarrative` / `LiquidationBanner` / `SignalJournalCard` erken-return yerine sabit yükseklik
  rezerv ediyor — içerik gelip gittiğinde ekran yukarı-aşağı zıplamıyor.
- `SignalJournalCard` boşken placeholder gösterir; satır eklenince `animateContentSize` ile yumuşar.
- `StrategiesViewModel` — 12.5fps yerine `.sample(250)` (4fps): 20 strateji artık insan okuma
  hızında güncelleniyor, gereksiz CPU + metin flicker'ı bitti.
- `StrategyCard` switch aç/kapa `animateContentSize(tween(200))` ile yumuşak; `reasoning` metni
  `maxLines=1` + ellipsis.
- Win-rate rozeti her zaman rezerve ediliyor (yatay kayma yok).

### Added
- Strateji tab'ına "GÜÇLÜ ÜSTTE" sıralama (en güçlü BUY/SELL üstte, NEUTRAL altta) — varsayılan.
- Kategori chip sayaçları ViewModel'de önceden hesaplanıyor; başlık sayısı dinamik.

## [1.7.0] - Strateji Performans İzleyici (#21)

### Added
- `StrategyPerformanceTracker` — her stratejinin yön sinyalini kaydeder, 60 sn sonra fiyatla
  sonuçlandırıp win-rate üretir (throttle 5 sn/strateji, soğuk başlangıçta nötr ağırlık).
- `StrategyEngine.executeAll` — konsensüs ağırlığı artık `confidence × performansAğırlığı`;
  gürültü üreten stratejiler otomatik zayıflar (0.3..1.2 aralığı).
- `StrategiesScreen` — her strateji kartında win-rate rozeti (`%60(15)`).

## [1.6.1] - UI Layout Düzeltmeleri

### Fixed
- `DepthHeatmap` — bar satırı sabit `10.dp` yükseklikte 8sp metni taşıyor, fiyat satırları üst üste
  biniyordu; `.heightIn(min = 13.dp)` ile düzeltildi.
- `PyramidCanvas` — `layerHeight` alt sınırı çıplak pikseldi (`16f`), yüksek yoğunluklu ekranda
  etiketler çorbaya dönüyordu; `28.dp.toPx()` yapıldı.
- `PyramidScreen` — canvas, üstündeki sabit kartlı `Column` içinde `weight(1f)` ile sıkışıyordu;
  Column `verticalScroll` + canvas'a sabit `height(280.dp)` verildi.

## [1.6.0] - Performans & Kalite (Kod Analizi P1+P2)

### Fixed
- `BurstDetector` — her trade'de tam deque'yi 2× filter eden O(n) tarama yerine alış/satış
  için ayrı pencereler + kayan toplamlar (O(1) amortized).
- `DivergenceStrategy` — hesaplanıp kullanılmayan `rsi` artık koşula giriyor
  (bull: RSI < 45, bear: RSI > 55) ve reason'a yazılıyor.
- `BollingerBandsStrategy` — ölü `bandwidthPct` artık squeeze boost olarak skora giriyor.
- `MarketMicrostructureStrategy` — ölü `spreadPct` artık likidite faktörü olarak skora giriyor.
- `MeanReversionStrategy.category` — MOMENTUM → VOLATILITY (yanlış etiket).

### Added
- `docs/KOD_ANALIZI.md` — kod incelemesi + 50 geliştirme önerisi + aksiyon takibi.

## [1.5.0] - Derinlik Isı Haritası

### Added
- `BookProfile` — konsolide kitap profili (bid/ask toplam, dengesizlik, duvar fiyatları). Saf, test edilebilir.
- `DepthHeatmap` — ilk 5 bid/ask seviyesini aynalı barlarla gösteren kompakt derinlik ısı haritası;
  kitap dengesizliği + bid/ask duvar fiyatları.

### Cleanup
- PyramidScreen'de kullanılmayan importlar kaldırıldı.

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
