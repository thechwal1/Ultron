package com.talha.ultron.brain

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks to the Claude API when Ultron is online. The API key is passed
 * in per-request (from SecureSettings) instead of fixed at construction.
 */
class ClaudeApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val endpoint = "https://api.anthropic.com/v1/messages"

    fun send(userMessage: String, memoryContext: List<String>, apiKey: String, systemPrompt: String? = null): String {
        val messagesArray = JSONArray()
        if (memoryContext.isNotEmpty()) {
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", "Context from earlier: ${memoryContext.joinToString(" | ")}")
            })
        }
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        val requestBody = JSONObject().apply {
            put("model", "claude-sonnet-5")
            put("max_tokens", 500)
            if (!systemPrompt.isNullOrBlank()) put("system", systemPrompt)
            put("messages", messagesArray)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                val reason = try {
                    JSONObject(errorBody).optJSONObject("error")?.optString("message")
                } catch (e: Exception) { null } ?: errorBody.take(150)
                throw IOException("Claude API ${response.code}: $reason")
            }
            val json = JSONObject(response.body?.string() ?: "{}")
            val content = json.optJSONArray("content") ?: return "Sorry, I didn't get a response."
            return (0 until content.length())
                .joinToString(" ") { i -> content.getJSONObject(i).optString("text") }
        }
    }
}
