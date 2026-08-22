package com.talha.ultron.ui

data class ChatMessage(
    val role: String, // "user" or "ultron"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
