package com.talha.ultron.brain.providers

data class ProviderMeta(val id: String, val displayName: String, val needsKey: Boolean, val fieldHint: String, val modelHint: String)

object ProviderCatalog {
    val CONFIGURABLE = listOf(
        ProviderMeta("groq", "Groq", true, "API key", "llama-3.3-70b-versatile"),
        ProviderMeta("openai", "OpenAI", true, "API key", "gpt-4o-mini"),
        ProviderMeta("gemini", "Gemini (Google)", true, "API key", "gemini-2.0-flash"),
        ProviderMeta("mistral", "Mistral", true, "API key", "mistral-small-latest"),
        ProviderMeta("deepseek", "DeepSeek", true, "API key", "deepseek-chat"),
        ProviderMeta("together", "Together AI", true, "API key", "meta-llama/Llama-3.3-70B-Instruct-Turbo"),
        ProviderMeta("openrouter", "OpenRouter", true, "API key", "openrouter/auto"),
        ProviderMeta("cohere", "Cohere", true, "API key", "command-r"),
        ProviderMeta("puter", "Puter.js (free, no key)", false, "No key needed", "claude-sonnet-5"),
        ProviderMeta("ollama", "Ollama (local)", false, "Server URL", "llama3.2")
    )
    val DEFAULT_ORDER = listOf("claude", "groq", "openai", "gemini", "mistral", "deepseek", "together", "openrouter", "cohere", "puter", "ollama")
}
