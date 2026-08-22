package com.talha.ultron.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PriorityDao {
    @Insert
    suspend fun insert(priority: PriorityEntity)

    @Query("SELECT * FROM priorities WHERE done = 0 ORDER BY createdAt DESC")
    suspend fun active(): List<PriorityEntity>

    @Query("SELECT * FROM priorities ORDER BY createdAt DESC LIMIT :limit")
    suspend fun all(limit: Int = 50): List<PriorityEntity>

    @Query("UPDATE priorities SET done = 1 WHERE id = :id")
    suspend fun markDone(id: Long)

    @Query("DELETE FROM priorities WHERE id = :id")
    suspend fun delete(id: Long)
}
