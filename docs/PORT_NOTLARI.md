# Piramit → Piram Taşıma Notları

> **Yön:** `piramit` (web/TypeScript — Binance USD-M futures agresyon haritası) projesinde
> **piram** (Android/Kotlin) projemize lazım olacak her şey: fonksiyonlar, kurgular/stratejiler,
> mantıksal fikirler ve geliştirme patch'leri. Her başlıkta kaynak dosya (`piramit`) ve
> önerilen yer (`piram`) belirtilir.
>
> `piram` kodu iki yönlü okundu; aşağıdaki tespitler mevcut implementasyona dayanır.

## ✅ Uygulama Durumu (2026-08-21)

| P1 maddesi | Durum |
|---|---|
| Notional (USDT) katmanlama | ✅ UYGULANDI (`MicroBucket`/`Manager`/`LayerAggregate` + `SignalConfig`) |
| Adaptif eşik + histerezis + sizeScale | ✅ UYGULANDI (`AdaptiveEdges` + ViewModel reconfigure) |
| WindowLedger + gerçek timeframe | ✅ UYGULANDI (`WindowLedger` + VenueStrip WIN hücresi) |
| Toplama/boşaltma + OI dipnotu | ✅ UYGULANDI (`DivergenceEngine` + `FlowNarrative` + **OI dipnotu**) |
| Likidasyon akışı (forceOrder) | ✅ UYGULANDI (`BinanceLiquidationClient` + `LiquidationBanner`) |
| OI + oiState ("OI yoksa yalan yok") | ✅ UYGULANDI (`OpenInterestClient` + `oiState` + VenueStrip OI hücresi) |
| Radar ekranı (miniTicker) | ✅ UYGULANDI (`BinanceMiniTickerClient` + `RadarScreen` + 4. sekme) |
| Sinyal günlüğü + isabet oranı | ✅ UYGULANDI (Room `JournalDao` + `SignalJournalCard` + later5/15/60) |
| Kod temizliği | ✅ UYGULANDI (şablon artıkları + ölü bağımlılıklar kaldırıldı) |
| **Kalan (Phase 9)** | tickSize format + sembol arama, 24s istatistik, book heatmap, backtest, push bildirim |

---

## 0. Tek Cümlelik Özet

piram'ın en büyük 5 eksiği: **(1)** katmanlar **adet (qty)** üzerinden kuruluyor, **USDT notional**
değil; **(2)** katman eşikleri **sabit**, coin'e uyarlanmıyor; **(3)** timeframe seçici **ölü**
(sadece tercih kaydediyor, pencere hesaplamıyor); **(4)** "toplama/boşaltma" gibi **anlatı katmanı**
yok (20 strateji var ama hikâye yok); **(5)** **likidasyon (forceOrder) + OI** verisi yok (spot'tayız,
futures verisi daha zengin).

---

## 1. Kavramsal Düzeltmeler (Önce Bunlar)

### 1.1 Katmanlar USDT notional'a geçmeli (fiyat × adet)

- **piramit:** `aggTrade.ts` → `notional = price × qty`. Tüm katmanlar, şekil ve divergence **USDT
  notional** üzerinden. "Adet sayılmaz" ilkesi.
- **piram (sorun):** `MicroBucket.addOrder()` → `currentVolume += order.volume` (adet). Eşikler
  `minVolume = 0.001`, `maxVolume = 25.0` (adet). Yani BTC'de 0.5 dev, SOL'de 0.5 toz → **aynı eşik
  coin'ler arası anlamsız**; altcoin'lerde her şey "Whale" katmanına düşer.
- **Patch:** `Order.value` zaten `volume × price` olarak duruyor. `MicroBucket`/`MicroBucketManager`'ı
  `value` (USDT) toplayacak şekilde çevir; `minVolume/maxVolume` parametrelerini
  `minNotional/maxNotional` yap. `LayerAggregate`'te de USDT gösterimi öne çıksın.
- **Dosyalar:** `domain/engine/bucket/MicroBucket.kt`, `MicroBucketManager.kt`, `domain/model/LayerAggregate.kt`.

### 1.2 Futures vs Spot veri kaynağı

- **piramit:** USD-M **futures** (`wss://fstream.binance.com/market`) → aggTrade'de `nq` (notional)
  alanı var, `!forceOrder@arr` (likidasyon) var, REST `openInterest` var.
- **piram:** **spot** (`wss://stream.binance.com`). Spot aggTrade'de `nq` yok, likidasyon akışı yok,
  OI yok.
- **Öneri:** piram'a Binance **fapi** (futures) WS/REST'i opsiyonel kaynak olarak ekle. En azından
  `forceOrder` + `openInterest`. Order-flow sinyali futures'ta belirgin şekilde daha zengin
  (likidasyonlar = en güçlü iz).

---

## 2. Katmanlama & Eşikler (piram'ın en zayıf yeri)

### 2.1 Adaptif eşik + histerezis — `adaptiveEdges.ts` (P1)

- **piramit:** Katman eşiklerini **bu coin'in kendi notional dağılımından** yüzdeliklerle kurar:
  `ADAPT_P = [0.5, 0.75, 0.9, 0.97, 0.99, 0.999]`, min 40 trade, **%18 histerezis** (eşik zıplamasın).
- **piram:** `MathUtils.createLogarithmicThresholds` ile sabit log eşik (0.001→25 adet). Coin değişince
  dağılım değişmiyor.
- **Patch:** `MicroBucketManager`'a "adaptif mod": histogram → yüzdelik eşik + histerezis; hazır
  `reconfigureThresholds` metodu bunun için ideal giriş noktası.

### 2.2 sizeScale / BTC referans ölçeği — `signalConfig.ts` (P1)

- **piramit:** `sizeScale(medianNotional) = clamp(median / BTC_MEDIAN_REF(4000), 0.02, 2.5)`.
  Sabit BTC tablosu (`100/1K/10K/50K/250K/1M`) bu katsayıyla coin'e ölçeklenir.
- **Patch:** "sabit mod" için piram'a `FIXED_EDGES` + `sizeScale` karşılığı ekle; medyan notional
  yoksa ham tablo kalsın.

### 2.3 Şekil etiketi (morphology) — `morphology.ts` (P2)

- **piramit:** 7 katman payından şekil: `klasik | kum | ters | mantar | yassi | bos` + Türkçe cümle.
- **piram:** Yok. `ConsensusHeader`'ın altına bir satır eklenebilir; `MicroBucketManager.getAggregatedLayers()`
  çıktısı (`LayerAggregate.buyRatio/share`) doğrudan girdi olur.

---

## 3. Zaman Pencereleri (timeframe'i gerçekten çalıştır)

### 3.1 WindowLedger — `windowLedger.ts` (P1)

- **piramit:** 1 saniyelik dilimler (`SecondSlice`) + oturum boyu toplam + `sumWindow(60|300|900|3600)`.
  `KEEP_SECONDS = 3600` ile 1 saat geriye pencere.
- **piram (sorun):** `PyramidViewModel` yalnızca son 200 trade'lik `ConcurrentLinkedDeque` tutuyor;
  `timeframe` sadece DataStore'a yazılıyor, **hiçbir pencere hesabı yok**.
- **Patch:** Kotlin'e `WindowLedger` karşılığı (saniye dilimli defter + oturum + `sumWindow`) yaz;
  `setTimeframe` bunu gerçekten değiştirsin. `PyramidUiState.timeframe` artık dekoratif olmaktan çıkar.
  Opsiyonlar piramit ile aynı: `60 / 300 / 900 / 3600 / oturum`.

---

## 4. Anlatı Katmanı (Sinyal Kurgusu)

### 4.1 Toplama / Boşaltma — `divergence.ts` (P1) ⭐

- **piramit:** tepe katman neti vs taban katman neti + fiyat (`tanh` yumuşatması) → `toplama | bosaltma | yok`.
  OI yalnız **dipnot** ("OI şişiyor — yeni long kokusu" gibi). `minVol` altındaysa sinyal üretmez.
- **piram:** 20 strateji var ama bu **anlatı** yok — hiçbiri "büyükler alıyor, küçükler satıyor"
  demiyor.
- **Patch:** `scoreDivergence`'ı Kotlin'e çevir (`domain/engine/divergence/`). Girdiler piram'da hazır:
  üst katman neti (`layerIndex >= numLayers-2`), alt katman neti, `changePct`, `oiDelta`.
  `ConsensusHeader`'a Türkçe cümle olarak bas.

### 4.2 Pencere çelişkisi — `windowClash.ts` (P2)

- **piramit:** 1dk neti vs oturum neti → `donus | dip | teyit | yok` + cümle.
- **Patch:** WindowLedger (3.1) geldikten sonra `readClash` portu tek satırlık mantık.

### 4.3 Merkezi eşik dosyası — `signalConfig.ts` (P2)

- **piramit:** tüm sihirli sayılar tek `SIGNAL` objesinde; yorum "tune buradan".
- **piram (sorun):** eşikler dağınık: `BurstDetector` (1500ms/3 order/0.3), `MicroBucketManager`
  (0.001/25), `decayFactor 0.15`, 20 strateji dosyasının içindeki katsayılar, ViewModel'de 250ms/80ms.
- **Patch:** `domain/engine/SignalConfig.kt` (object) aç; hepsini oraya taşı. Test/tune kolaylaşır.

---

## 5. Veri Kaynakları (piram'da hiç olmayanlar)

### 5.1 Likidasyon akışı — `forceOrder.ts` (P1) ⭐

- **piramit:** `!forceOrder@arr` → `Liq { symbol, side, priceStr, qty, notional }`; aktif sembolle
  eşleşenleri snapshot'a düşürür.
- **piram:** hiç yok. Likidasyon = "zorla kapatılan pozisyon", order-flow'un en sert izi.
- **Patch:** fapi WS client (`wss://fstream.binance.com/market/ws/!forceOrder@arr`) + `Order` benzeri
  `Liquidation` modeli + `TickerTape`'e "LİQ" satırı + burst/whale gibi haptic.

### 5.2 Açık Pozisyon (OI) — `openInterest.ts` + `oiState.ts` (P1)

- **piramit:** REST `openInterest` → `kontrat × fiyat = USDT`; `oiDelta(prev,next)`; durum makinesi
  `bekliyor | ok | yok | eski` — "OI yoksa yalan yok".
- **piram:** hiç yok.
- **Patch:** fapi REST periyodik sorgu + `PyramidUiState`'e `oi`/`oiDelta`/`oiState`; divergence
  cümlesine dipnot. **Durum makinesi deseni** piram'ın `ConnectionState` yaklaşımına çok benzer,
  aynı sadelikte taşınır.

### 5.3 Radar (tüm piyasa taraması) — `miniTicker.ts` + `RadarList.tsx` (P2)

- **piramit:** `!miniTicker@arr` → tüm USDT perp'leri `% değişim` + `quote hacim` sıralama; satıra
  basınca sembole geç.
- **piram:** hiç yok. Yeni bir "Radar" sekmesi (Navigation'da zaten 3 sekme var, 4. eklenir).
  Spot için `!miniTicker@arr` (spot) veya REST `ticker/24hr` kullanılır.

---

## 6. Sinyal Günlüğü + İsabet Oranı — `signalJournal.ts` (P2)

- **piramit:** toplama/boşaltma sinyali basınca kayıt; `later5/15/60` ile sonraki fiyatı işaretler;
  `hitRate()` → "sinyal tuttu mu?" sayacı. 60 sn spam koruması, son 80 kayıt.
- **piram:** hiç yok; ama **Room zaten var** — localStorage yerine `JournalEntity` + `JournalDao`
  olarak çok daha sağlam kurulur. "Sinyal isabeti" ekranı kullanıcıya güven verir (pazarlama değeri
  de var).

---

## 7. Format & Precision

### 7.1 tickSize'a göre fiyat — `formatPrice.ts` (P2)

- **piramit:** `exchangeInfo` PRICE_FILTER'dan `tickSize` alır, fiyatı **kırpmadan** doğru hanede yazar.
- **piram:** `MathUtils.formatPrice` genel (1000+ → 2 hane, 1+ → 4, yoksa 8). Altcoin'lerde (PEPE vb.)
  yanlış/eksik hane.
- **Patch:** sembol meta'sına `tickSize` ekle; formatPrice bunu kullansın.

### 7.2 Sembol listesi + arama + tohum fallback — `precision.ts` + `seedSymbols.ts` (P2)

- **piramit:** exchangeInfo'dan TRADING+PERPETUAL+USDT/USDC filtresi, `rank` skorlu arama, CORS'ta
  `SEED_SYMBOLS` tohum liste (arama hep çalışır).
- **piram:** SettingsScreen'de düz text field (`"e.g. BTCUSDT"`).
- **Patch:** `SymbolMeta` (symbol/base/tickSize/stepSize) + arama listesi + tohum liste; Settings'e
  aranabilir sembol seçici.

### 7.3 Kuruş birikimi (float kayması) — `cents.ts` (P3)

- **piramit:** `toCents/fromCents/addCents` ile toplama hatalarını keser.
- **piram:** `Double` topluyor; yüksek frekansta kümülatif hata birikir.
- **Patch:** `MathUtils`'a `addCents` karşılığı; katman toplamlarında kullan.

---

## 8. Mühendislik Patch'leri

| Patch (piramit) | piram'da durum | Ne yapmalı |
|---|---|---|
| `tapeBuffer.ts` ring buffer (GC'siz) | `ConcurrentLinkedDeque` var (çalışıyor) | Hot-path için ring buffer düşünülebilir (P3) |
| `socketRuntime.ts` 23s hot-swap | Yok | WS'yi 23 saatte bir tazele (bayat soket) (P3) |
| `streamPlan.ts` data saver | Yok | "Veri tasarrufu": depth akışlarını / ek venue'ları kapat (P3) |
| `localAlert.ts` + `watchAlerts.ts` | Haptic var (3 pattern, piram önde) | Kraken katmanı hacmi `×1.25+1` artınca alarm (P3) |
| `sessionShot.ts` metin dökümü | Yok | `ACTION_SEND` share intent ile oturum dökümü (P3) |
| `socket.worker.ts` | Var (`Dispatchers.IO`) | ✅ Eşdeğer, dokunma |
| `backoff.ts` | Var (`WsReconnectPolicy`) | ✅ Eşdeğer (jitter+cap), dokunma |

---

## 9. piram'da ZATEN VAR — dokunma (piramit'ten geri kalmıyor)

- **5 venue WebSocket + DepthAggregator** (piramit tek venue + book yok).
- **20 strateji + consensus** (piramit bilinçli olarak yok).
- **Whale-only TickerTape** (piramit tape'i filtrelemeden gösteriyor).
- **Katman tooltip paneli** (piramit'te sadece küçük `tip`).
- **Haptic 3 pattern** (piramit tek `impactOccurred`).
- **Room + DataStore** (piramit localStorage).
- **VenueStrip** (cross-venue spread).

Bunlar piramit'e taşınacak değil, **piram'ın güçlü yanları**.

---

## 10. Felsefe / Bilinçli Redler (piramit'ten öğrenilecekler)

1. **"Fiyat tahmini değil, agresyon haritası"** — piram'ın 20 stratejisi "STRONG BUY/SELL" diyor;
   piramit bu iddiayı bilinçli reddediyor. Öneri: stratejileri **tahmin** değil **mikroyapı metriği**
   olarak yeniden konumlandır (ör. "OFI Delta", "Arrival Velocity"). Marketing ve yasal açıdan daha sağlam.
2. **"OI yoksa yalan yok"** — eksik veride uydurma; `oiState` deseni piram'daki tüm opsiyonel veriler
   (derinlik, venue) için geçerli olmalı.
3. **"Mock yok"** — iki taraf da uyumlu. ✅
4. **"Canvas içine yazı yok"** — piram Canvas'ta `drawText` kullanıyor; dar katmanlarda okunabilirlik
   sorunu çıkarsa piramit yaklaşımı (metin HTML/Compose katmanında) düşünülebilir.
5. **Kullanıcı dilinde cümleler** — piramit sinyalleri Türkçe cümle veriyor (`strings.ts`); piram UI
   İngilizce. En azından sinyal cümleleri Türkçe olabilir.

---

## 11. Öncelik Sırası (piram için önerilen yol haritası)

| Öncelik | İş | Kaynak (piramit) | Efor |
|---|---|---|---|
| **P1** | Notional (USDT) katmanlama | `aggTrade.ts`, `layerWallet.ts` | Küçük-Orta |
| **P1** | Adaptif eşik + histerezis + sizeScale | `adaptiveEdges.ts`, `signalConfig.ts` | Orta |
| **P1** | WindowLedger + gerçek timeframe | `windowLedger.ts` | Orta |
| **P1** | Toplama/boşaltma + OI dipnotu | `divergence.ts`, `openInterest.ts`, `oiState.ts` | Orta |
| **P1** | Likidasyon akışı (forceOrder) | `forceOrder.ts` | Küçük-Orta |
| **P2** | Şekil etiketi + pencere çelişkisi | `morphology.ts`, `windowClash.ts` | Küçük |
| **P2** | Sinyal günlüğü + isabet (Room) | `signalJournal.ts` | Orta |
| **P2** | Radar ekranı | `miniTicker.ts`, `RadarList.tsx` | Orta-Büyük |
| **P2** | tickSize format + sembol arama | `formatPrice.ts`, `precision.ts`, `seedSymbols.ts` | Küçük |
| **P3** | Merkezi SignalConfig | `signalConfig.ts` | Küçük |
| **P3** | data saver + hot-swap + ring buffer + sessionShot | `streamPlan.ts`, `socketRuntime.ts`, `tapeBuffer.ts`, `sessionShot.ts` | Küçük |

---

## 12. Sonuç

> piramit, piram'ın **kavramsal çekirdeğini** daha doğru kurmuş: **USDT notional katmanlama,
> adaptif eşik, gerçek zaman pencereleri, toplama/boşaltma anlatısı, likidasyon + OI**.
> piram ise **altyapıda** önde: 5 venue, derinlik agregasyonu, 20 strateji, Room, haptik, Compose UI.
> En yüksek getiri, piramit'in bu 5 kavramsal parçasını piram'ın güçlü altyapısına taşımaktır.
