package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrades(trades: List<TradeEntity>)

    @Query("SELECT * FROM trades WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTrades(symbol: String, limit: Int = 100): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE symbol = :symbol AND isWhale = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentWhaleTrades(symbol: String, limit: Int = 50): Flow<List<TradeEntity>>

    @Query("DELETE FROM trades WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldTrades(cutoffTimestamp: Long)

    @Query("DELETE FROM trades WHERE symbol = :symbol")
    suspend fun clearTradesForSymbol(symbol: String)
}
