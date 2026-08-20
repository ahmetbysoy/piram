# HFT Pyramid Terminal - Data Flow Specification

## 1. Trade Ingestion Pipeline
1. WebSocket receives payload from exchange server (Binance/Bybit/OKX/Kraken/KuCoin).
2. Raw JSON string is parsed into strongly-typed `Order` domain model with high-precision timestamp, price, volume, and side.
3. Order is asynchronously queued for Room SQLite database storage.

## 2. MicroBucket Layer Assignment
- Layer threshold boundaries are dynamically mapped using natural logarithm scaling:
  $$\text{threshold}(k) = \text{minVolume} \cdot e^{k \cdot \text{scale}}$$
- Volume is credited to layer $k$ based on whether the order volume falls within $[\text{threshold}(k), \text{threshold}(k+1)]$.
- Apex tier ($k = N-1$) is flagged as `isWhaleTier`.

## 3. Dynamic Decay and Jitter-Free Canvas Smoothing
- At each render cycle ($dt = 80\text{ms}$):
  $$\text{currentVolume}(t + dt) = \text{currentVolume}(t) \cdot e^{-\lambda \cdot dt}$$
- Display lerping prevents visual jumping:
  $$\text{displayVolume}(t + dt) = \text{displayVolume}(t) + \alpha \cdot (\text{currentVolume}(t) - \text{displayVolume}(t))$$
- Bar horizontal scaling applies square root normalization:
  $$\text{widthFraction} = \frac{\sqrt{\text{displayVolume}}}{\sqrt{\text{maxVolume}}}$$

## 4. Quantitative Consensus Engine
- Every 250ms, a `MarketSnapshot` is assembled from live state.
- All 20 quantitative strategies compute signal, confidence, and score.
- Weighted consensus is computed as:
  $$\text{Consensus} = \frac{\sum_{i=1}^{20} w_i \cdot \text{Score}_i}{\sum_{i=1}^{20} w_i}$$
