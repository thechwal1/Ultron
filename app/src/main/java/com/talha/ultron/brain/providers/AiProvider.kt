package com.talha.ultron.brain.providers

import com.talha.ultron.SecureSettings

interface AiProvider {
    val id: String
    val displayName: String
    val needsApiKey: Boolean
    fun isReady(settings: SecureSettings): Boolean
    suspend fun send(userMessage: String, history: List<String>, systemPrompt: String, settings: SecureSettings): String
}
