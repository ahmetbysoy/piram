# HFT Pyramid Terminal - API & WebSocket Reference

## Live Exchange Endpoints

### 1. Binance Public Stream
- **Aggregated Trades**: `wss://stream.binance.com:9443/ws/{symbol}@aggTrade`
- **Level 2 Depth (20 Levels)**: `wss://stream.binance.com:9443/ws/{symbol}@depth20@100ms`

### 2. Bybit v5 Public Spot Stream
- **Endpoint**: `wss://stream.bybit.com/v5/public/spot`
- **Subscription**: `{"op": "subscribe", "args": ["publicTrade.{SYMBOL}"]}`

### 3. OKX v5 Public Stream
- **Endpoint**: `wss://ws.okx.com:8443/ws/v5/public`
- **Subscription**: `{"op": "subscribe", "args": [{"channel": "trades", "instId": "{BASE}-{QUOTE}"}]}`

### 4. Kraken Public Stream
- **Endpoint**: `wss://ws.kraken.com`
- **Subscription**: `{"event": "subscribe", "pair": ["{PAIR}"], "subscription": {"name": "trade"}}`

### 5. KuCoin Public Stream
- **Endpoint**: `wss://ws-api-spot.kucoin.com/?connectId={timestamp}`
- **Subscription**: `{"id": 1, "type": "subscribe", "topic": "/market/match:{SYMBOL}", "privateChannel": false, "response": true}`
