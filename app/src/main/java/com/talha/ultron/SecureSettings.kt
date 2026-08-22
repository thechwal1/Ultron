package com.talha.ultron

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Encrypted preferences wrapper. All user settings and API keys stored here.
 * In production, migrate sensitive keys to Android Keystore-backed storage.
 */
class SecureSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ultron_settings", Context.MODE_PRIVATE)

    // --- API Keys ---
    var claudeApiKey: String
        get() = prefs.getString("claude_api_key", "") ?: ""
        set(value) = prefs.edit { putString("claude_api_key", value) }

    var kimiApiKey: String
        get() = prefs.getString("kimi_api_key", "") ?: ""
        set(value) = prefs.edit { putString("kimi_api_key", value) }

    // --- Feature Toggles ---
    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean("wake_word_enabled", true)
        set(value) = prefs.edit { putBoolean("wake_word_enabled", value) }

    var notificationReaderEnabled: Boolean
        get() = prefs.getBoolean("notification_reader_enabled", true)
        set(value) = prefs.edit { putBoolean("notification_reader_enabled", value) }

    var appLaunchEnabled: Boolean
        get() = prefs.getBoolean("app_launch_enabled", true)
        set(value) = prefs.edit { putBoolean("app_launch_enabled", value) }

    var deepVoiceEnabled: Boolean
        get() = prefs.getBoolean("deep_voice_enabled", true)
        set(value) = prefs.edit { putBoolean("deep_voice_enabled", value) }

    var selectedVoiceName: String?
        get() = prefs.getString("selected_voice_name", null)
        set(value) = prefs.edit { putString("selected_voice_name", value) }

    var learningCacheEnabled: Boolean
        get() = prefs.getBoolean("learning_cache_enabled", true)
        set(value) = prefs.edit { putBoolean("learning_cache_enabled", value) }

    // --- Personality ---
    var personalityStyle: String
        get() = prefs.getString("personality_style", "Default") ?: "Default"
        set(value) = prefs.edit { putString("personality_style", value) }

    // --- Notification Filters ---
    var notificationQuietHoursEnabled: Boolean
        get() = prefs.getBoolean("notif_quiet_hours", false)
        set(value) = prefs.edit { putBoolean("notif_quiet_hours", value) }

    var notificationQuietStart: Int
        get() = prefs.getInt("notif_quiet_start", 22 * 60)
        set(value) = prefs.edit { putInt("notif_quiet_start", value) }

    var notificationQuietEnd: Int
        get() = prefs.getInt("notif_quiet_end", 7 * 60)
        set(value) = prefs.edit { putInt("notif_quiet_end", value) }

    var notificationSpeakLowPriority: Boolean
        get() = prefs.getBoolean("notif_speak_low", false)
        set(value) = prefs.edit { putBoolean("notif_speak_low", value) }

    var notificationBlockedApps: Set<String>
        get() = prefs.getStringSet("notif_blocked_apps", emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet("notif_blocked_apps", value) }

    // --- SMS Auto-Reply ---
    var smsAutoReplyEnabled: Boolean
        get() = prefs.getBoolean("sms_auto_reply_enabled", false)
        set(value) = prefs.edit { putBoolean("sms_auto_reply_enabled", value) }

    var smsAutoReplyOnlyWhenAway: Boolean
        get() = prefs.getBoolean("sms_only_when_away", true)
        set(value) = prefs.edit { putBoolean("sms_only_when_away", value) }

    var smsUseAiReply: Boolean
        get() = prefs.getBoolean("sms_use_ai_reply", false)
        set(value) = prefs.edit { putBoolean("sms_use_ai_reply", value) }

    var smsReplyTemplate: String
        get() = prefs.getString("sms_reply_template", "") ?: ""
        set(value) = prefs.edit { putString("sms_reply_template", value) }

    var smsAutoConfirm: Boolean
        get() = prefs.getBoolean("sms_auto_confirm", false)
        set(value) = prefs.edit { putBoolean("sms_auto_confirm", value) }

    // --- Social Auto-Reply ---
    var socialAutoReplyEnabled: Boolean
        get() = prefs.getBoolean("social_auto_reply_enabled", false)
        set(value) = prefs.edit { putBoolean("social_auto_reply_enabled", value) }

    var socialAutoReplyOnlyWhenAway: Boolean
        get() = prefs.getBoolean("social_only_when_away", true)
        set(value) = prefs.edit { putBoolean("social_only_when_away", value) }

    var socialReplyTemplate: String
        get() = prefs.getString("social_reply_template", "") ?: ""
        set(value) = prefs.edit { putString("social_reply_template", value) }

    // --- Call Screening ---
    var callScreeningEnabled: Boolean
        get() = prefs.getBoolean("call_screening_enabled", false)
        set(value) = prefs.edit { putBoolean("call_screening_enabled", value) }

    var callAutoDeclineEnabled: Boolean
        get() = prefs.getBoolean("call_auto_decline_enabled", false)
        set(value) = prefs.edit { putBoolean("call_auto_decline_enabled", value) }

    var callAutoDeclineOnlyUnknown: Boolean
        get() = prefs.getBoolean("call_decline_unknown", true)
        set(value) = prefs.edit { putBoolean("call_decline_unknown", value) }

    var callReplyTemplate: String
        get() = prefs.getString("call_reply_template", "") ?: ""
        set(value) = prefs.edit { putString("call_reply_template", value) }

    var blockedNumbers: Set<String>
        get() = prefs.getStringSet("blocked_numbers", emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet("blocked_numbers", value) }

    // --- Presence Detection ---
    var presenceDetectionEnabled: Boolean
        get() = prefs.getBoolean("presence_enabled", true)
        set(value) = prefs.edit { putBoolean("presence_enabled", value) }

    var presenceUseScreenState: Boolean
        get() = prefs.getBoolean("presence_screen", true)
        set(value) = prefs.edit { putBoolean("presence_screen", value) }

    var presenceUseProximity: Boolean
        get() = prefs.getBoolean("presence_proximity", true)
        set(value) = prefs.edit { putBoolean("presence_proximity", value) }

    var presenceUseBluetooth: Boolean
        get() = prefs.getBoolean("presence_bluetooth", false)
        set(value) = prefs.edit { putBoolean("presence_bluetooth", value) }

    var presenceUseUsageStats: Boolean
        get() = prefs.getBoolean("presence_usage", true)
        set(value) = prefs.edit { putBoolean("presence_usage", value) }

    // --- Provider Settings ---
    fun getProviderKey(providerId: String): String {
        return prefs.getString("provider_key_$providerId", "") ?: ""
    }

    fun setProviderKey(providerId: String, key: String) {
        prefs.edit { putString("provider_key_$providerId", key) }
    }

    fun getProviderModel(providerId: String): String {
        return prefs.getString("provider_model_$providerId", "") ?: ""
    }

    fun setProviderModel(providerId: String, model: String) {
        prefs.edit { putString("provider_model_$providerId", model) }
    }

    fun getProviderUrl(providerId: String): String {
        return prefs.getString("provider_url_$providerId", "") ?: ""
    }

    fun setProviderUrl(providerId: String, url: String) {
        prefs.edit { putString("provider_url_$providerId", url) }
    }

    fun isProviderEnabled(providerId: String): Boolean {
        return prefs.getBoolean("provider_enabled_$providerId", true)
    }

    fun setProviderEnabled(providerId: String, enabled: Boolean) {
        prefs.edit { putBoolean("provider_enabled_$providerId", enabled) }
    }

    fun providerPriorityOrder(): List<String> {
        val saved = prefs.getString("provider_priority", null)
        return saved?.split(",") ?: com.talha.ultron.brain.providers.ProviderCatalog.DEFAULT_ORDER
    }

    fun setProviderPriorityOrder(order: List<String>) {
        prefs.edit { putString("provider_priority", order.joinToString(",")) }
    }
}
