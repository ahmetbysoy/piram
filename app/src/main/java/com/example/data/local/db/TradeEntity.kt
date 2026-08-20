package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Order
import com.example.domain.model.OrderSide

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey
    val id: String,
    val symbol: String,
    val side: String, // "BUY" or "SELL"
    val volume: Double,
    val price: Double,
    val value: Double,
    val exchange: String,
    val timestamp: Long,
    val isWhale: Boolean,
    val layerIndex: Int
) {
    fun toDomain(): Order {
        return Order(
            id = id,
            side = if (side == "BUY") OrderSide.BUY else OrderSide.SELL,
            volume = volume,
            price = price,
            value = value,
            exchange = exchange,
            timestamp = timestamp,
            isWhale = isWhale,
            layerIndex = layerIndex
        )
    }

    companion object {
        fun fromDomain(order: Order, symbol: String): TradeEntity {
            return TradeEntity(
                id = order.id,
                symbol = symbol,
                side = order.side.name,
                volume = order.volume,
                price = order.price,
                value = order.value,
                exchange = order.exchange,
                timestamp = order.timestamp,
                isWhale = order.isWhale,
                layerIndex = order.layerIndex
            )
        }
    }
}
