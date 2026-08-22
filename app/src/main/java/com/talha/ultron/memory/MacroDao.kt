package com.talha.ultron.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MacroDao {
    @Insert
    suspend fun insert(macro: MacroEntity)

    @Query("SELECT * FROM macros WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): MacroEntity?

    @Query("SELECT * FROM macros ORDER BY createdAt DESC LIMIT :limit")
    suspend fun all(limit: Int = 50): List<MacroEntity>

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun delete(id: Long)
}
