package com.talha.ultron.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int = 50): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE read = 0 ORDER BY timestamp DESC")
    suspend fun unread(): List<NotificationEntity>

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notifications SET read = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM notifications WHERE read = 0")
    suspend fun unreadCount(): Int
}
