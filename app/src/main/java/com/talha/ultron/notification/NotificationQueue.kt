package com.talha.ultron.notification

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe queue for notifications that need to be read aloud.
 * Decouples the NotificationListenerService from the TTS engine.
 */
object NotificationQueue {
    private val queue = ConcurrentLinkedQueue<NotificationEntity>()

    fun add(notification: NotificationEntity) {
        queue.offer(notification)
    }

    fun poll(): NotificationEntity? = queue.poll()

    fun peek(): NotificationEntity? = queue.peek()

    fun clear() = queue.clear()

    fun isEmpty() = queue.isEmpty()
}
