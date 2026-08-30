package com.talha.ultron.brain

import com.talha.ultron.NetworkMonitor
import com.talha.ultron.SecureSettings
import com.talha.ultron.brain.providers.ProviderChain
import com.talha.ultron.accessibility.AppController
import com.talha.ultron.memory.MacroDao
import com.talha.ultron.memory.MacroEntity
import com.talha.ultron.memory.MemoryDao
import com.talha.ultron.memory.MemoryEntity
import com.talha.ultron.memory.PriorityDao
import com.talha.ultron.memory.PriorityEntity
import com.talha.ultron.notification.UltronNotificationService
import com.talha.ultron.presence.PresenceDetector
import com.talha.ultron.reply.SocialAppReplyHandler

class BrainRouter(
    private val networkMonitor: NetworkMonitor,
    private val providerChain: ProviderChain,
    private val offlineResponder: OfflineResponder,
    private val memoryDao: MemoryDao,
    private val priorityDao: PriorityDao,
    private val macroDao: MacroDao,
    private val settings: SecureSettings,
    private val appLauncher: AppLauncher,
    private val alarmTimerHandler: AlarmTimerHandler,
    private val systemToggleHandler: SystemToggleHandler,
    private val notificationService: UltronNotificationService? = null,
    private val appController: AppController? = null,
    private val socialReplyHandler: SocialAppReplyHandler? = null
) {
    private val knowledgeCache = LocalKnowledgeCache(memoryDao)

    private val presenceDetector by lazy {
        val ctx = appController?.context
            ?: throw IllegalStateException("Context required for presence detection")
        PresenceDetector(ctx)
    }

    private val rememberPattern = Regex("""(?i)^remember(?: that)? (.+)$""")
    private val priorityPattern = Regex("""(?i)^add (?:a )?(?:priority|goal)[:]?\s*(.+)$""")
    private val macroCreatePattern = Regex("""(?i)^create (?:a )?macro (?:called )?([a-zA-Z0-9 ]+?):\s*(.+)$""")
    private val macroRunPattern = Regex("""(?i)^run (?:macro )?([a-zA-Z0-9 ]+?)\.?$""")

    private val notifReadPattern = Regex("""(?i)(read my (last )?notification|read (that|it) again|what was that notification)""")
    private val notifSummaryPattern = Regex("""(?i)(what notifications? do i have|what did i miss|any notifications?|check my notifications?|notification summary)""")
    private val notifReadAllPattern = Regex("""(?i)(read all (my )?notifications|read my notifications)""")
    private val notifClearPattern = Regex("""(?i)(clear (all )?notifications?|dismiss (all )?notifications?|clear my notifications)""")
    private val notifStopPattern = Regex("""(?i)(stop reading notifications?|shut up about notifications?|quiet on notifications?)""")
    private val notifEnablePattern = Regex("""(?i)(turn on notification reader|enable notification reader|start reading notifications)""")
    private val notifDisablePattern = Regex("""(?i)(turn off notification reader|disable notification reader|stop reading notifications)""")

    private val tapPattern = Regex("""(?i)(tap|click|press)\s+(?:on\s+)?(.+)""")
    private val scrollPattern = Regex("""(?i)scroll\s+(up|down|left|right)""")
    private val typePattern = Regex("""(?i)(type|enter|input)\s+['"]?(.+?)['"]?\s*(?:in(?:to)?\s+(?:the\s+)?(?:text\s+)?field)?$""")
    private val goBackPattern = Regex("""(?i)(go back|press back|back button|previous screen)""")
    private val goHomePattern = Regex("""(?i)(go home|press home|home screen)""")
    private val recentsPattern = Regex("""(?i)(recent apps|open recents|show recents|app switcher)""")
    private val readScreenPattern = Regex("""(?i)(read (the )?screen|what('s| is) on (the )?screen|what do you see|describe (the )?screen)""")
    private val isVisiblePattern = Regex("""(?i)(is .+ visible|can you see .+|do you see .+)""")

    private val presencePattern = Regex("""(?i)(am i away|check presence|where am i|am i near my phone|presence status)""")
    private val pendingRepliesPattern = Regex("""(?i)(show pending replies|pending replies|what replies are pending)""")
    private val sendPendingPattern = Regex("""(?i)(send pending replies|confirm pending replies|approve pending replies)""")
    private val cancelPendingPattern = Regex("""(?i)(cancel pending replies|dismiss pending replies|clear pending replies)""")
    private val smsStatusPattern = Regex("""(?i)(sms auto reply status|is sms auto reply on|check sms reply)""")
    private val socialStatusPattern = Regex("""(?i)(social auto reply status|is social auto reply on|check social reply)""")

    @Volatile
    private var _notificationService: UltronNotificationService? = notificationService

    fun injectNotificationService(svc: UltronNotificationService?) {
        _notificationService = svc
    }

    suspend fun handle(userInput: String): String {
        val instantReply = tryInstantHandlers(userInput)

        val (reply, source) = if (instantReply != null) {
            instantReply
        } else {
            val recent = memoryDao.recent(10).reversed().map { "${it.role}: ${it.content}" }
            if (networkMonitor.isOnlineNow()) {
                val systemPrompt = buildSystemPrompt()
                val result = providerChain.send(userInput, recent, systemPrompt, settings)
                if (result != null) {
                    result.first to "online:${result.second}"
                } else {
                    ("(no AI provider is configured or reachable right now) " + offlineReply(userInput)) to "offline-fallback"
                }
            } else {
                offlineReply(userInput) to "offline"
            }
        }

        memoryDao.insert(MemoryEntity(timestamp = System.currentTimeMillis(), role = "user", content = userInput, source = source))
        memoryDao.insert(MemoryEntity(timestamp = System.currentTimeMillis(), role = "ultron", content = reply, source = source))

        return reply
    }

    private suspend fun tryInstantHandlers(input: String): Pair<String, String>? {
        tryHandleNotificationCommand(input)?.let { return it to "notification" }
        tryHandleAppControl(input)?.let { return it to "app-control" }
        tryHandleAutoReplyCommands(input)?.let { return it to "auto-reply" }
        tryHandlePresence(input)?.let { return it to "presence" }
        tryHandleMacroCreate(input)?.let { return it to "macro-create" }
        tryHandleMacroRun(input)?.let { return it to "macro-run" }
        tryHandlePriorityAdd(input)?.let { return it to "priority-add" }
        if (settings.appLaunchEnabled) {
            tryHandleAppOpen(input)?.let { return it to "app-launch" }
        }
        alarmTimerHandler.tryHandle(input)?.let { return it to "alarm-timer" }
        systemToggleHandler.tryHandle(input)?.let { return it to "system-toggle" }
        return null
    }

    private fun tryLocalStepOnly(step: String): String? {
        tryHandleAppOpen(step)?.let { return it }
        alarmTimerHandler.tryHandle(step)?.let { return it }
        systemToggleHandler.tryHandle(step)?.let { return it }
        return null
    }

    private fun tryHandleAutoReplyCommands(input: String): String? {
        return when {
            pendingRepliesPattern.containsMatchIn(input) ->
                socialReplyHandler?.getPendingRepliesSummary() ?: "Social reply handler not available."
            sendPendingPattern.containsMatchIn(input) ->
                socialReplyHandler?.sendAllPending() ?: "Social reply handler not available."
            cancelPendingPattern.containsMatchIn(input) ->
                socialReplyHandler?.cancelAllPending() ?: "Social reply handler not available."
            smsStatusPattern.containsMatchIn(input) -> {
                val status = if (settings.smsAutoReplyEnabled) "enabled" else "disabled"
                val away = if (settings.smsAutoReplyOnlyWhenAway) " (only when away)" else ""
                "SMS auto-reply is $status$away."
            }
            socialStatusPattern.containsMatchIn(input) -> {
                val status = if (settings.socialAutoReplyEnabled) "enabled" else "disabled"
                val away = if (settings.socialAutoReplyOnlyWhenAway) " (only when away)" else ""
                "Social auto-reply is $status$away."
            }
            else -> null
        }
    }

    private fun tryHandlePresence(input: String): String? {
        return if (presencePattern.containsMatchIn(input)) {
            presenceDetector.getStatus()
        } else null
    }

    private fun tryHandleAppControl(input: String): String? {
        val controller = appController ?: return null
        tapPattern.find(input)?.let { return controller.tap(it.groupValues[2].trim()) }
        scrollPattern.find(input)?.let { return controller.scroll(it.groupValues[1].lowercase()) }
        typePattern.find(input)?.let { return controller.type(it.groupValues[2].trim()) }
        if (goBackPattern.containsMatchIn(input)) return controller.goBack()
        if (goHomePattern.containsMatchIn(input)) return controller.goHome()
        if (recentsPattern.containsMatchIn(input)) return controller.openRecents()
        readScreenPattern.find(input)?.let { return controller.readScreen() }
        isVisiblePattern.find(input)?.let {
            val target = input.replace(Regex("""(?i)(is |visible|can you see|do you see|\?)"""), "").trim()
            return controller.isVisible(target)
        }
        return null
    }

    private suspend fun tryHandleNotificationCommand(input: String): String? {
        val svc = _notificationService ?: return null
        return when {
            notifReadPattern.containsMatchIn(input) -> svc.readLastNotification()
            notifSummaryPattern.containsMatchIn(input) -> svc.summarizeUnread()
            notifReadAllPattern.containsMatchIn(input) -> svc.readAllUnread()
            notifClearPattern.containsMatchIn(input) -> svc.clearAll()
            notifStopPattern.containsMatchIn(input) -> { svc.stopSpeaking(); "I'll stop reading notifications aloud." }
            notifEnablePattern.containsMatchIn(input) -> { settings.notificationReaderEnabled = true; "Notification reader enabled." }
            notifDisablePattern.containsMatchIn(input) -> { settings.notificationReaderEnabled = false; svc.stopSpeaking(); "Notification reader disabled." }
            else -> null
        }
    }

    private suspend fun buildSystemPrompt(): String {
        val base = when (settings.personalityStyle) {
            "Sardonic" -> "You are Ultron, a personal AI assistant with dry, sardonic humor and mild deadpan wit. Stay accurate and genuinely helpful — the wit is a garnish, not a substitute for a real answer."
            "Coach" -> "You are Ultron, an encouraging personal AI assistant. Be warm, celebrate progress, and ask a brief motivating follow-up question when it fits naturally."
            "Formal" -> "You are Ultron, a formal and precise personal AI assistant. Keep responses professional and concise."
            else -> "You are Ultron, a helpful personal AI assistant."
        }
        val active = priorityDao.active()
        return if (active.isEmpty()) base
        else base + "\n\nThe user's current priorities/goals:\n" + active.joinToString("\n") { "- ${it.text}" }
    }

    private suspend fun offlineReply(input: String): String {
        if (settings.learningCacheEnabled) {
            knowledgeCache.findSimilarPastAnswer(input)?.let { return it }
        }
        return offlineResponder.respond(input)
    }

    private suspend fun tryHandlePriorityAdd(input: String): String? {
        val text = rememberPattern.find(input)?.groupValues?.get(1)
            ?: priorityPattern.find(input)?.groupValues?.get(1)
            ?: return null
        val clean = text.trim().trimEnd('.', '!', '?')
        if (clean.isBlank()) return null
        priorityDao.insert(PriorityEntity(text = clean, createdAt = System.currentTimeMillis()))
        return "Got it — I'll remember: $clean"
    }

    private fun tryHandleAppOpen(input: String): String? {
        val match = Regex("""(?i)\b(open|launch|start)\s+(.+)""").find(input) ?: return null
        val appName = match.groupValues[2].trim().trimEnd('.', '!', '?')
        return if (appLauncher.tryOpen(appName)) "Opening $appName." else null
    }

    private suspend fun tryHandleMacroCreate(input: String): String? {
        val match = macroCreatePattern.find(input) ?: return null
        val name = match.groupValues[1].trim()
        val stepsRaw = match.groupValues[2].trim()
        if (name.isBlank() || stepsRaw.isBlank()) return null
        val steps = stepsRaw.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        if (steps.isEmpty()) return null
        macroDao.insert(MacroEntity(name = name, steps = steps.joinToString("\n"), createdAt = System.currentTimeMillis()))
        return "Macro '$name' saved with ${steps.size} step${if (steps.size != 1) "s" else ""}. Say '$name' to use it."
    }

    private suspend fun tryHandleMacroRun(input: String): String? {
        val match = macroRunPattern.find(input) ?: return null
        val name = match.groupValues[1].trim()
        val macro = macroDao.findByName(name) ?: return null
        val steps = macro.steps.split("\n").filter { it.isNotBlank() }
        val results = steps.map { step -> tryLocalStepOnly(step) ?: "(skipped: '$step' needs the AI, not supported inside macros)" }
        return "Running '$name':\n" + results.joinToString("\n") { "• $it" }
    }
}
