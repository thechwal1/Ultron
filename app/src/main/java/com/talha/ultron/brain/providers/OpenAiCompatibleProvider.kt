package com.talha.ultron.brain.providers

import com.talha.ultron.SecureSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAiCompatibleProvider(
    override val id: String,
    override val displayName: String,
    private val defaultUrl: String,
    private val defaultModel: String,
    override val needsApiKey: Boolean = true
) : AiProvider {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    override fun isReady(settings: SecureSettings): Boolean = if (needsApiKey) settings.getProviderKey(id).isNotBlank() else settings.getProviderUrl(id).isNotBlank()
    override suspend fun send(userMessage: String, history: List<String>, systemPrompt: String, settings: SecureSettings): String {
        val url = if (needsApiKey) settings.getProviderUrl(id).ifBlank { defaultUrl } else settings.getProviderUrl(id)
        val model = settings.getProviderModel(id).ifBlank { defaultModel }
        val apiKey = settings.getProviderKey(id)
        val messages = JSONArray().apply {
            put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
            if (history.isNotEmpty()) put(JSONObject().apply { put("role", "user"); put("content", "Context: " + history.joinToString(" | ")) })
            put(JSONObject().apply { put("role", "user"); put("content", userMessage) })
        }
        val body = JSONObject().apply { put("model", model); put("messages", messages) }.toString().toRequestBody("application/json".toMediaType())
        val requestBuilder = Request.Builder().url(url).post(body).addHeader("content-type", "application/json")
        if (apiKey.isNotBlank()) requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("$displayName ${response.code}: ${response.body?.string()?.take(150)}")
            return JSONObject(response.body?.string() ?: "{}").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }
}
