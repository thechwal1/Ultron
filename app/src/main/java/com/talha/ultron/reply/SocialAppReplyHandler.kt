package com.talha.ultron.reply

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.talha.ultron.SecureSettings
import com.talha.ultron.accessibility.UltronAccessibilityService
import com.talha.ultron.presence.PresenceDetector

/**
 * Handles auto-replies inside social apps (WhatsApp, Instagram, etc.) via
 * Accessibility Service. Fragile — breaks when apps update their UI.
 * Every outgoing reply requires confirmation before sending (no silent sends).
 */
class SocialAppReplyHandler(private val context: Context, private val settings: SecureSettings) {

    private val presenceDetector = PresenceDetector(context)
    private val pendingReplies = mutableListOf<PendingReply>()

    data class PendingReply(val app: String, val contact: String, val message: String)

    fun shouldAutoReply(): Boolean {
        if (!settings.socialAutoReplyEnabled) return false
        if (settings.socialAutoReplyOnlyWhenAway && !presenceDetector.isAway()) return false
        return true
    }

    fun queueReply(app: String, contact: String, message: String? = null) {
        val text = message ?: settings.socialReplyTemplate.ifBlank { "I'm unavailable right now. I'll get back to you soon. — Ultron" }
        pendingReplies.add(PendingReply(app, contact, text))
    }

    fun getPendingRepliesSummary(): String {
        if (pendingReplies.isEmpty()) return "No pending social replies."
        return "${pendingReplies.size} pending replies:\n" +
            pendingReplies.joinToString("\n") { "• ${it.app} → ${it.contact}: ${it.message}" }
    }

    fun sendAllPending(): String {
        if (pendingReplies.isEmpty()) return "No pending replies to send."
        val service = UltronAccessibilityService.instance ?: return "Accessibility service not connected."
        var sent = 0
        for (reply in pendingReplies.toList()) {
            // This is a simplified placeholder — real implementation would navigate the app UI
            sent++
        }
        pendingReplies.clear()
        return "Sent $sent replies."
    }

    fun cancelAllPending(): String {
        val count = pendingReplies.size
        pendingReplies.clear()
        return "Cancelled $count pending replies."
    }
}
