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

class GeminiProvider : AiProvider {
    override val id = "gemini"
    override val displayName = "Gemini (Google)"
    override val needsApiKey = true
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    override fun isReady(settings: SecureSettings): Boolean = settings.getProviderKey(id).isNotBlank()
    override suspend fun send(userMessage: String, history: List<String>, systemPrompt: String, settings: SecureSettings): String {
        val apiKey = settings.getProviderKey(id)
        val model = settings.getProviderModel(id).ifBlank { "gemini-2.0-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val fullPrompt = buildString { append(systemPrompt); if (history.isNotEmpty()) append("\n\nContext: ").append(history.joinToString(" | ")); append("\n\n").append(userMessage) }
        val body = JSONObject().apply { put("contents", JSONArray().put(JSONObject().apply { put("parts", JSONArray().put(JSONObject().apply { put("text", fullPrompt) })) })) }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).addHeader("content-type", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Gemini ${response.code}: ${response.body?.string()?.take(150)}")
            val json = JSONObject(response.body?.string() ?: "{}")
            return json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
    }
}
