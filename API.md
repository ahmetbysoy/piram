# HFT Pyramid Terminal - API & WebSocket Reference

## Live Exchange Endpoints

### 1. Binance Public Stream
- **Aggregated Trades**: `wss://stream.binance.com:9443/ws/{symbol}@aggTrade`
- **Level 2 Depth (20 Levels)**: `wss://stream.binance.com:9443/ws/{symbol}@depth20@100ms`

### 2. Bybit v5 Public Spot Stream
- **Endpoint**: `wss://stream.bybit.com/v5/public/spot`
- **Trades**: `{"op": "subscribe", "args": ["publicTrade.{SYMBOL}"]}`
- **Order Book (50 Levels)**: `{"op": "subscribe", "args": ["orderbook.50.{SYMBOL}"]}`

### 3. OKX v5 Public Stream
- **Endpoint**: `wss://ws.okx.com:8443/ws/v5/public`
- **Trades**: `{"op": "subscribe", "args": [{"channel": "trades", "instId": "{BASE}-{QUOTE}"}]}`
- **Order Book (50 Levels)**: `{"op": "subscribe", "args": [{"channel": "books50-l2-tbt", "instId": "{BASE}-{QUOTE}"}]}`

### 4. Kraken Public Stream
- **Trades (v1)**: `wss://ws.kraken.com` → `{"event": "subscribe", "pair": ["{PAIR}"], "subscription": {"name": "trade"}}`
- **Order Book (v2, 25 Levels)**: `wss://ws.kraken.com/v2` → `{"method": "subscribe", "params": {"channel": "book", "symbol": ["{PAIR}"], "depth": 25}}`

### 5. KuCoin Public Stream
- **Token**: `POST https://api.kucoin.com/api/v1/bullet-public` → `data.token`
- **Endpoint**: `wss://ws-api-spot.kucoin.com/?token={token}&connectId={timestamp}`
- **Trades**: `{"type": "subscribe", "topic": "/market/match:{SYMBOL}", ...}`
- **Order Book (20 Levels)**: `{"type": "subscribe", "topic": "/spotMarket/level2Depth20:{SYMBOL}", ...}`

## Aggregation

The five per-venue depth streams are merged into a single consolidated book by
`DepthAggregator` (bids best-first, asks best-first, capped at 50 levels per side),
which feeds the microstructure strategies and the UI. Cross-venue last trade prices
flow into `MarketSnapshot.exchangePrices` and power the Statistical Arbitrage strategy.
