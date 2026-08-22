package com.talha.ultron.brain.providers

import com.talha.ultron.SecureSettings
import com.talha.ultron.brain.LatencyTracker
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class ProviderChain(private val providers: List<AiProvider>) {
    suspend fun send(userMessage: String, history: List<String>, systemPrompt: String, settings: SecureSettings): Pair<String, String>? {
        val order = settings.providerPriorityOrder()
        val ordered = order.mapNotNull { id -> providers.firstOrNull { it.id == id } }.ifEmpty { providers }
        for (provider in ordered) {
            if (!settings.isProviderEnabled(provider.id)) continue
            if (!provider.isReady(settings)) continue
            try {
                val start = System.currentTimeMillis()
                val response = withTimeout(12_000L) { provider.send(userMessage, history, systemPrompt, settings) }
                LatencyTracker.record(System.currentTimeMillis() - start)
                return response to provider.id
            } catch (_: TimeoutCancellationException) { continue }
            catch (_: Exception) { continue }
        }
        return null
    }
}
