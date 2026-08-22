package com.talha.ultron.notification

import com.talha.ultron.SecureSettings

class NotificationFilter(private val settings: SecureSettings) {

    fun shouldRead(notification: NotificationEntity): Boolean {
        if (!settings.notificationReaderEnabled) return false
        if (settings.notificationBlockedApps.contains(notification.packageName)) return false
        if (settings.notificationQuietHoursEnabled && isInQuietHours()) return false
        if (!settings.notificationSpeakLowPriority && notification.priority <= 0) return false
        return true
    }

    fun shouldStore(notification: NotificationEntity): Boolean {
        return !settings.notificationBlockedApps.contains(notification.packageName)
    }

    private fun isInQuietHours(): Boolean {
        val now = java.util.Calendar.getInstance()
        val minutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val start = settings.notificationQuietStart
        val end = settings.notificationQuietEnd
        return if (start < end) {
            minutes in start..end
        } else {
            minutes >= start || minutes <= end
        }
    }
}
