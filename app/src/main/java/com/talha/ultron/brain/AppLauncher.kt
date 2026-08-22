package com.talha.ultron.brain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppLauncher(private val context: Context) {

    private val packageManager = context.packageManager

    private val knownApps = mapOf(
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "youtube" to "com.google.android.youtube",
        "maps" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "settings" to "com.android.settings",
        "camera" to "com.android.camera",
        "gallery" to "com.android.gallery3d",
        "photos" to "com.google.android.apps.photos",
        "calendar" to "com.google.android.calendar",
        "clock" to "com.google.android.deskclock",
        "calculator" to "com.google.android.calculator",
        "messages" to "com.google.android.apps.messaging",
        "sms" to "com.google.android.apps.messaging",
        "phone" to "com.android.dialer",
        "dialer" to "com.android.dialer",
        "contacts" to "com.android.contacts",
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "discord" to "com.discord",
        "telegram" to "org.telegram.messenger",
        "slack" to "com.Slack",
        "reddit" to "com.reddit.frontpage",
        "tiktok" to "com.zhiliaoapp.musically",
        "linkedin" to "com.linkedin.android",
        "snapchat" to "com.snapchat.android"
    )

    fun tryOpen(appName: String): Boolean {
        val normalized = appName.lowercase().trim()
        val packageName = knownApps[normalized] ?: findByLabel(normalized)
        packageName ?: return false
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        return true
    }

    private fun findByLabel(label: String): String? {
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.firstOrNull { app ->
            val appLabel = packageManager.getApplicationLabel(app).toString().lowercase()
            appLabel == label || appLabel.contains(label)
        }?.packageName
    }
}
