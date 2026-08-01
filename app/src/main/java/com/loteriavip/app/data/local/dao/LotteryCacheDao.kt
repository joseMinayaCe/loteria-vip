package com.loteriavip.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loteriavip.app.data.local.entity.LotteryCacheEntity

@Dao
interface LotteryCacheDao {

    @Query("SELECT * FROM lottery_cache ORDER BY timestamp DESC")
    suspend fun getAllCachedResults(): List<LotteryCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<LotteryCacheEntity>)

    @Query("DELETE FROM lottery_cache")
    suspend fun clearCache()
}
