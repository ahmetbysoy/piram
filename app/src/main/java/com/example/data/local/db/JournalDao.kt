package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntity)

    @Query("SELECT * FROM journal ORDER BY at DESC LIMIT :limit")
    fun getRecent(limit: Int = 12): Flow<List<JournalEntity>>

    @Query("DELETE FROM journal WHERE at < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    /** Zamanı gelen satırların later5/15/60 alanlarını o anki fiyatla doldurur. */
    @Query("UPDATE journal SET later5 = :price WHERE later5 IS NULL AND :now - at >= 300000")
    suspend fun markLater5(now: Long, price: Double)

    @Query("UPDATE journal SET later15 = :price WHERE later15 IS NULL AND :now - at >= 900000")
    suspend fun markLater15(now: Long, price: Double)

    @Query("UPDATE journal SET later60 = :price WHERE later60 IS NULL AND :now - at >= 3600000")
    suspend fun markLater60(now: Long, price: Double)
}
