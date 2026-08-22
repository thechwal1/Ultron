package com.talha.ultron.reply

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.talha.ultron.SecureSettings

class UltronCallScreeningService : CallScreeningService() {

    private lateinit var settings: SecureSettings

    override fun onCreate() {
        super.onCreate()
        settings = SecureSettings(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val number = callDetails.handle?.schemeSpecificPart ?: return

        if (settings.blockedNumbers.contains(number)) {
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(false)
                .build())
            return
        }

        if (settings.callAutoDeclineEnabled) {
            if (!settings.callAutoDeclineOnlyUnknown || callDetails.callerNumberVerificationStatus == Call.Details.VERIFICATION_STATUS_NOT_VERIFIED) {
                respondToCall(callDetails, CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(false)
                    .build())
                // Send SMS reply
                SmsAutoReplyManager(this, settings).sendReply(number, settings.callReplyTemplate)
                return
            }
        }

        respondToCall(callDetails, CallResponse.Builder().build())
    }
}
