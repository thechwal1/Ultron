package com.talha.ultron.brain.providers

import com.talha.ultron.SecureSettings
import com.talha.ultron.brain.ClaudeApiClient

class ClaudeProvider(private val client: ClaudeApiClient) : AiProvider {
    override val id = "claude"
    override val displayName = "Claude (Anthropic)"
    override val needsApiKey = true
    override fun isReady(settings: SecureSettings): Boolean = settings.claudeApiKey.isNotBlank()
    override suspend fun send(userMessage: String, history: List<String>, systemPrompt: String, settings: SecureSettings): String =
        client.send(userMessage, history, settings.claudeApiKey, systemPrompt)
}
