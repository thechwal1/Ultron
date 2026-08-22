package com.talha.ultron

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.talha.ultron.brain.AlarmTimerHandler
import com.talha.ultron.brain.AppLauncher
import com.talha.ultron.brain.BrainRouter
import com.talha.ultron.brain.ClaudeApiClient
import com.talha.ultron.brain.OfflineResponder
import com.talha.ultron.brain.SystemToggleHandler
import com.talha.ultron.brain.providers.ClaudeProvider
import com.talha.ultron.brain.providers.CohereProvider
import com.talha.ultron.brain.providers.GeminiProvider
import com.talha.ultron.brain.providers.OpenAiCompatibleProvider
import com.talha.ultron.brain.providers.ProviderChain
import com.talha.ultron.brain.providers.PuterProvider
import com.talha.ultron.memory.MemoryDatabase
import com.talha.ultron.notification.UltronNotificationService
import com.talha.ultron.ui.ChatMessage
import com.talha.ultron.ui.VoiceOrbView
import com.talha.ultron.voice.FreeWakeWordManager
import com.talha.ultron.voice.VoiceInputManager
import com.talha.ultron.voice.VoiceOutputManager
import com.talha.ultron.accessibility.AppController
import com.talha.ultron.reply.SocialAppReplyHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs

class UltronForegroundService : Service() {

    private lateinit var voiceIn: VoiceInputManager
    private lateinit var voiceOut: VoiceOutputManager
    private lateinit var freeWakeWord: FreeWakeWordManager
    private lateinit var brain: BrainRouter
    private lateinit var settings: SecureSettings
    private lateinit var puterProvider: PuterProvider
    private lateinit var appController: AppController
    private lateinit var socialReplyHandler: SocialAppReplyHandler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var windowManager: WindowManager? = null
    private var floatingOrbView: VoiceOrbView? = null
    private var floatParams: WindowManager.LayoutParams? = null
    private var orbStateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelOnce()
        startForeground(NOTIFICATION_ID, buildNotification("Ultron is running"))

        settings = SecureSettings(this)
        val db = MemoryDatabase.get(this)
        puterProvider = PuterProvider(this)
        appController = AppController(this)
        socialReplyHandler = SocialAppReplyHandler(this, settings)

        val notifIntent = Intent(this, UltronNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(notifIntent)
        } else {
            startService(notifIntent)
        }

        val providerChain = ProviderChain(
            listOf(
                OpenAiCompatibleProvider("kimi", "Kimi (Moonshot AI)", "https://api.moonshot.cn/v1/chat/completions", "moonshot-v1-8k"),
                ClaudeProvider(ClaudeApiClient()),
                OpenAiCompatibleProvider("groq", "Groq", "https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile"),
                OpenAiCompatibleProvider("openai", "OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-4o-mini"),
                GeminiProvider(),
                OpenAiCompatibleProvider("mistral", "Mistral", "https://api.mistral.ai/v1/chat/completions", "mistral-small-latest"),
                OpenAiCompatibleProvider("deepseek", "DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-chat"),
                OpenAiCompatibleProvider("together", "Together AI", "https://api.together.xyz/v1/chat/completions", "meta-llama/Llama-3.3-70B-Instruct-Turbo"),
                OpenAiCompatibleProvider("openrouter", "OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "openrouter/auto"),
                CohereProvider(),
                puterProvider,
                OpenAiCompatibleProvider("ollama", "Ollama", "", "llama3.2", needsApiKey = false)
            )
        )

        voiceOut = VoiceOutputManager(
            context = this,
            deepVoiceEnabled = settings.deepVoiceEnabled,
            preferredVoiceName = settings.selectedVoiceName,
            onSpeakingStarted = { UltronEvents.setState(UltronState.SPEAKING) },
            onSpeakingFinished = { UltronEvents.setState(UltronState.IDLE) }
        )

        brain = BrainRouter(
            networkMonitor = NetworkMonitor(this),
            providerChain = providerChain,
            offlineResponder = OfflineResponder(this),
            memoryDao = db.memoryDao(),
            priorityDao = db.priorityDao(),
            macroDao = db.macroDao(),
            settings = settings,
            appLauncher = AppLauncher(this),
            alarmTimerHandler = AlarmTimerHandler(this),
            systemToggleHandler = SystemToggleHandler(this),
            notificationService = null,
            appController = appController,
            socialReplyHandler = socialReplyHandler
        )

        voiceIn = VoiceInputManager(
            context = this,
            networkMonitor = NetworkMonitor(this),
            onResult = { heard -> handleUserSpeech(heard) },
            onError = { UltronEvents.setState(UltronState.IDLE) }
        )

        freeWakeWord = FreeWakeWordManager(
            context = this,
            onWakeWordDetected = {
                UltronEvents.setState(UltronState.LISTENING)
                voiceIn.startListening()
            },
            onSetupFailed = { msg ->
                updateNotification("Wake word: $msg — use Talk to Ultron instead")
            }
        )

        if (settings.wakeWordEnabled) freeWakeWord.start()

        scope.launch {
            kotlinx.coroutines.delay(2000)
            UltronNotificationServiceRef.get()?.let { svc ->
                svc.inject(db.notificationDao(), voiceOut)
                brain.injectNotificationService(svc)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> { pauseUltron(); return START_NOT_STICKY }
            ACTION_LISTEN_NOW -> { UltronEvents.setState(UltronState.LISTENING); voiceIn.startListening() }
            ACTION_ENABLE_FLOAT -> showFloatingOrb()
            ACTION_DISABLE_FLOAT -> hideFloatingOrb()
        }
        return START_STICKY
    }

    private fun handleUserSpeech(text: String) {
        scope.launch {
            UltronEvents.emitMessage(ChatMessage("user", text))
            UltronEvents.setState(UltronState.THINKING)
            val reply = try {
                brain.handle(text)
            } catch (e: Exception) {
                "Something went wrong (${e.message ?: e.javaClass.simpleName}) — try again."
            }
            UltronEvents.emitMessage(ChatMessage("ultron", reply))
            voiceOut.speak(reply)
        }
    }

    private fun pauseUltron() {
        freeWakeWord.stop()
        voiceIn.destroy()
        hideFloatingOrb()
        voiceOut.speak("Ultron paused.")
        UltronEvents.setState(UltronState.IDLE)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun showFloatingOrb() {
        if (floatingOrbView != null) return
        if (!Settings.canDrawOverlays(this)) {
            updateNotification("Enable Draw over other apps in Settings")
            return
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val orb = VoiceOrbView(this)
        val sizePx = (72 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 200 }

        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var isDrag = false
        orb.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; isDrag = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) isDrag = true
                    params.x = startX + dx; params.y = startY + dy
                    windowManager?.updateViewLayout(orb, params); true
                }
                MotionEvent.ACTION_UP -> { if (!isDrag) toggleListeningFromOverlay(); true }
                else -> false
            }
        }
        windowManager?.addView(orb, params)
        floatingOrbView = orb; floatParams = params
        orbStateJob = scope.launch {
            UltronEvents.state.collect { state ->
                floatingOrbView?.setState(
                    when (state) {
                        UltronState.IDLE -> com.talha.ultron.ui.OrbState.IDLE
                        UltronState.LISTENING -> com.talha.ultron.ui.OrbState.LISTENING
                        UltronState.THINKING -> com.talha.ultron.ui.OrbState.THINKING
                        UltronState.SPEAKING -> com.talha.ultron.ui.OrbState.SPEAKING
                    }
                )
            }
        }
    }

    private fun hideFloatingOrb() {
        orbStateJob?.cancel(); orbStateJob = null
        floatingOrbView?.let { windowManager?.removeView(it) }
        floatingOrbView = null; floatParams = null
    }

    private fun toggleListeningFromOverlay() {
        if (UltronEvents.state.value == UltronState.IDLE) {
            UltronEvents.setState(UltronState.LISTENING); voiceIn.startListening()
        } else {
            voiceIn.cancel(); UltronEvents.setState(UltronState.IDLE)
        }
    }

    private fun createNotificationChannelOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Ultron", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pauseIntent = Intent(this, UltronForegroundService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Ultron").setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .setOngoing(true).build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        scope.cancel(); freeWakeWord.stop(); voiceIn.destroy(); voiceOut.shutdown()
        puterProvider.destroy(); hideFloatingOrb(); super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "ultron_service"
        const val ACTION_PAUSE = "com.talha.ultron.ACTION_PAUSE"
        const val ACTION_LISTEN_NOW = "com.talha.ultron.ACTION_LISTEN_NOW"
        const val ACTION_ENABLE_FLOAT = "com.talha.ultron.ACTION_ENABLE_FLOAT"
        const val ACTION_DISABLE_FLOAT = "com.talha.ultron.ACTION_DISABLE_FLOAT"
    }
}

object UltronNotificationServiceRef {
    private var instance: UltronNotificationService? = null
    fun set(service: UltronNotificationService?) { instance = service }
    fun get(): UltronNotificationService? = instance
}
