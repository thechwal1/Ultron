package com.talha.ultron.reply

import android.content.Context
import android.telephony.SmsManager
import com.talha.ultron.SecureSettings
import com.talha.ultron.presence.PresenceDetector

class SmsAutoReplyManager(private val context: Context, private val settings: SecureSettings) {

    private val presenceDetector = PresenceDetector(context)

    fun shouldAutoReply(): Boolean {
        if (!settings.smsAutoReplyEnabled) return false
        if (settings.smsAutoReplyOnlyWhenAway && !presenceDetector.isAway()) return false
        return true
    }

    fun sendReply(phoneNumber: String, message: String? = null) {
        val text = message ?: settings.smsReplyTemplate.ifBlank { "I'm unavailable right now. I'll get back to you soon. — Ultron" }
        try {
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, text, null, null)
        } catch (e: Exception) {
            // Log or handle error
        }
    }
}
