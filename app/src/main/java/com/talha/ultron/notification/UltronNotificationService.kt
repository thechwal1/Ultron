package com.talha.ultron.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.talha.ultron.SecureSettings
import com.talha.ultron.voice.VoiceOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UltronNotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var dao: NotificationDao
    private lateinit var filter: NotificationFilter
    private var voiceOut: VoiceOutputManager? = null
    private var isSpeaking = false

    fun inject(dao: NotificationDao, voiceOut: VoiceOutputManager) {
        this.dao = dao
        this.voiceOut = voiceOut
    }

    override fun onCreate() {
        super.onCreate()
        filter = NotificationFilter(SecureSettings(this))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val entity = NotificationEntity(
            packageName = sbn.packageName,
            title = sbn.notification.extras.getString("android.title") ?: "",
            text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: "",
            timestamp = System.currentTimeMillis(),
            priority = sbn.notification.priority
        )

        if (filter.shouldStore(entity)) {
            scope.launch { dao.insert(entity) }
        }

        if (filter.shouldRead(entity)) {
            NotificationQueue.add(entity)
            processQueue()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional: mark as read when user dismisses
    }

    private fun processQueue() {
        if (isSpeaking || voiceOut == null) return
        val next = NotificationQueue.poll() ?: return
        isSpeaking = true
        val text = "${next.title}: ${next.text}"
        voiceOut?.speak(text)
        scope.launch { dao.markRead(next.id) }
        // Reset speaking flag after a delay (TTS doesn't give us a clean callback for this use case)
        scope.launch {
            kotlinx.coroutines.delay(5000)
            isSpeaking = false
            processQueue()
        }
    }

    fun readLastNotification(): String {
        val notif = dao.recent(1).firstOrNull() ?: return "No notifications yet."
        return "${notif.title}: ${notif.text}"
    }

    fun summarizeUnread(): String {
        val unread = dao.unread()
        if (unread.isEmpty()) return "You have no unread notifications."
        return "You have ${unread.size} unread notifications. " +
            unread.take(5).joinToString(". ") { "${it.title}: ${it.text}" }
    }

    fun readAllUnread(): String {
        val unread = dao.unread()
        if (unread.isEmpty()) return "No unread notifications."
        val text = unread.joinToString(". ") { "${it.title}: ${it.text}" }
        voiceOut?.speak(text)
        scope.launch { dao.markAllRead() }
        return "Reading ${unread.size} notifications."
    }

    fun clearAll(): String {
        scope.launch { dao.clearAll() }
        return "All notifications cleared."
    }

    fun stopSpeaking() {
        isSpeaking = false
        NotificationQueue.clear()
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val cn = ComponentName(context, UltronNotificationService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat?.contains(cn.flattenToString()) == true
        }

        fun openSettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}
