package com.talha.ultron.reply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.talha.ultron.SecureSettings

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val settings = SecureSettings(context)
        if (!settings.smsAutoReplyEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val manager = SmsAutoReplyManager(context, settings)
        if (!manager.shouldAutoReply()) return

        for (msg in messages) {
            val from = msg.originatingAddress ?: continue
            if (settings.blockedNumbers.contains(from)) continue
            manager.sendReply(from)
        }
    }
}
