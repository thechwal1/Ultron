package com.talha.ultron.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "priorities")
data class PriorityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long,
    val done: Boolean = false
)
