package com.talha.ultron.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val role: String,
    val content: String,
    val source: String
)
