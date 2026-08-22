package com.talha.ultron

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talha.ultron.accessibility.AppController
import com.talha.ultron.brain.LatencyTracker
import com.talha.ultron.notification.UltronNotificationService
import com.talha.ultron.reply.SmsAutoReplyManager
import com.talha.ultron.ui.ChatAdapter
import com.talha.ultron.ui.ChatMessage
import kotlinx.coroutines.launch

/**
 * Main UI for Ultron. Handles permission requests, settings drawers,
 * chat history display, and feature toggles.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var settings: SecureSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = SecureSettings(this)

        setupChatRecycler()
        setupFeatureToggles()
        setupButtons()
        requestEssentialPermissions()

        lifecycleScope.launch {
            UltronEvents.messages.collect { msg ->
                chatAdapter.addMessage(msg)
                findViewById<RecyclerView>(R.id.chatRecyclerView)?.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun setupChatRecycler() {
        val recycler = findViewById<RecyclerView>(R.id.chatRecyclerView)
        chatAdapter = ChatAdapter()
        recycler?.layoutManager = LinearLayoutManager(this)
        recycler?.adapter = chatAdapter
    }

    private fun setupFeatureToggles() {
        findViewById<Switch>(R.id.toggleWakeWord)?.apply {
            isChecked = settings.wakeWordEnabled
            setOnCheckedChangeListener { _, checked -> settings.wakeWordEnabled = checked }
        }
        findViewById<Switch>(R.id.toggleNotifications)?.apply {
            isChecked = settings.notificationReaderEnabled
            setOnCheckedChangeListener { _, checked ->
                settings.notificationReaderEnabled = checked
                if (checked && !UltronNotificationService.isEnabled(this@MainActivity)) {
                    UltronNotificationService.openSettings(this@MainActivity)
                }
            }
        }
        findViewById<Switch>(R.id.toggleAppLaunch)?.apply {
            isChecked = settings.appLaunchEnabled
            setOnCheckedChangeListener { _, checked -> settings.appLaunchEnabled = checked }
        }
        findViewById<Switch>(R.id.toggleDeepVoice)?.apply {
            isChecked = settings.deepVoiceEnabled
            setOnCheckedChangeListener { _, checked -> settings.deepVoiceEnabled = checked }
        }
        findViewById<Switch>(R.id.toggleLearning)?.apply {
            isChecked = settings.learningCacheEnabled
            setOnCheckedChangeListener { _, checked -> settings.learningCacheEnabled = checked }
        }
        findViewById<Switch>(R.id.toggleFloatingOrb)?.apply {
            isChecked = false
            setOnCheckedChangeListener { _, checked ->
                val intent = Intent(this@MainActivity, UltronForegroundService::class.java).apply {
                    action = if (checked) UltronForegroundService.ACTION_ENABLE_FLOAT else UltronForegroundService.ACTION_DISABLE_FLOAT
                }
                startService(intent)
            }
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnTalk)?.setOnClickListener {
            startService(Intent(this, UltronForegroundService::class.java).apply {
                action = UltronForegroundService.ACTION_LISTEN_NOW
            })
        }
        findViewById<Button>(R.id.btnSettings)?.setOnClickListener {
            findViewById<DrawerLayout>(R.id.drawerLayout)?.open()
        }
        findViewById<Button>(R.id.btnAccessibility)?.setOnClickListener {
            AppController(this).openSettings()
        }
        findViewById<Button>(R.id.btnLatency)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Latency Stats")
                .setMessage(LatencyTracker.summary())
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun requestEssentialPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1001)
        }

        // Battery optimization exemption
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val denied = permissions.zip(grantResults.toList()).filter { it.second != PackageManager.PERMISSION_GRANTED }
            if (denied.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("Ultron needs microphone access to hear you. Some features may be limited without these permissions.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}
