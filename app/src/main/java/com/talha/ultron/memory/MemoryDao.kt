package com.talha.ultron.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(entry: MemoryEntity)

    @Query("SELECT * FROM memory ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int = 20): List<MemoryEntity>

    @Query("SELECT * FROM memory WHERE content LIKE '%' || :keyword || '%'")
    suspend fun search(keyword: String): List<MemoryEntity>

    @Query("SELECT COUNT(*) FROM memory WHERE source = 'offline' OR source = 'offline-fallback'")
    suspend fun offlineExchangeCount(): Int

    @Query("SELECT * FROM memory ORDER BY timestamp ASC LIMIT :limit")
    suspend fun allForLearning(limit: Int = 500): List<MemoryEntity>
}
