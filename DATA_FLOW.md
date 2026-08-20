# HFT Pyramid Terminal - Data Flow Specification

## 1. Trade Ingestion Pipeline
1. WebSocket receives payload from exchange server (Binance/Bybit/OKX/Kraken/KuCoin).
2. Raw JSON string is parsed into strongly-typed `Order` domain model with high-precision timestamp, price, volume, and side.
3. Order is asynchronously queued for Room SQLite database storage.

## 2. MicroBucket Layer Assignment (USDT Notional)
- Katman eşikleri doğal logaritma ölçeklemesiyle kurulur (varsayılan aralık 100 USDT → 1M USDT):
  $$\text{threshold}(k) = \text{minNotional} \cdot e^{k \cdot \text{scale}}$$
- Sipariş **adeti değil**, `Order.value = fiyat × adet` (USDT notional) katman $k$'ya yazılır;
  `value ∈ [threshold(k), threshold(k+1)]` aralığına göre atanır.
- Apex tier ($k = N-1$) `isWhaleTier` olarak işaretlenir; üst 2 katman (Shark + Whale) "kurumsal" sayılır.

## 3. Dynamic Decay and Jitter-Free Canvas Smoothing
- Her render döngüsünde ($dt = 80\text{ms}$):
  $$\text{notional}(t + dt) = \text{notional}(t) \cdot e^{-\lambda \cdot dt}$$
- Display lerp görsel sıçramayı önler:
  $$\text{displayNotional}(t + dt) = \text{displayNotional}(t) + \alpha \cdot (\text{notional}(t) - \text{displayNotional}(t))$$
- Bar yatay ölçekleme karekök normalizasyonu kullanır:
  $$\text{widthFraction} = \frac{\sqrt{\text{displayNotional}}}{\sqrt{\text{maxNotional}}}$$

## 4. Quantitative Consensus Engine
- Every 250ms, a `MarketSnapshot` is assembled from live state.
- All 20 quantitative strategies compute signal, confidence, and score.
- Weighted consensus is computed as:
  $$\text{Consensus} = \frac{\sum_{i=1}^{20} w_i \cdot \text{Score}_i}{\sum_{i=1}^{20} w_i}$$
