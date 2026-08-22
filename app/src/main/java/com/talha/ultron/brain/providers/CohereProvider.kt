package com.talha.ultron.brain.providers

import com.talha.ultron.SecureSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class CohereProvider : AiProvider {
    override val id = "cohere"
    override val displayName = "Cohere"
    override val needsApiKey = true
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    override fun isReady(settings: SecureSettings): Boolean = settings.getProviderKey(id).isNotBlank()
    override suspend fun send(userMessage: String, history: List<String>, systemPrompt: String, settings: SecureSettings): String {
        val apiKey = settings.getProviderKey(id)
        val model = settings.getProviderModel(id).ifBlank { "command-r" }
        val fullMessage = buildString { append(systemPrompt); if (history.isNotEmpty()) append("\n\nContext: ").append(history.joinToString(" | ")); append("\n\n").append(userMessage) }
        val body = JSONObject().apply { put("model", model); put("message", fullMessage) }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("https://api.cohere.ai/v1/chat").post(body).addHeader("Authorization", "Bearer $apiKey").addHeader("content-type", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Cohere ${response.code}: ${response.body?.string()?.take(150)}")
            val json = JSONObject(response.body?.string() ?: "{}")
            return json.optString("text").ifBlank { json.optJSONObject("message")?.optJSONArray("content")?.optJSONObject(0)?.optString("text") ?: "(empty)" }
        }
    }
}
