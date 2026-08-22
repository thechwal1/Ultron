package com.talha.ultron.brain

import com.talha.ultron.memory.MemoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Caches past online answers so offline mode can still answer questions
 * that were previously asked while online. Simple fuzzy match on user input.
 */
class LocalKnowledgeCache(private val memoryDao: MemoryDao) {

    suspend fun findSimilarPastAnswer(input: String): String? = withContext(Dispatchers.IO) {
        val recent = memoryDao.recent(200)
        val normalizedInput = input.lowercase().trim()
        for (entry in recent) {
            if (entry.role == "user" && entry.content.lowercase().trim() == normalizedInput) {
                // Find the matching ultron response
                val idx = recent.indexOf(entry)
                if (idx > 0 && recent[idx - 1].role == "ultron" && recent[idx - 1].source.startsWith("online")) {
                    return@withContext recent[idx - 1].content
                }
            }
        }
        null
    }
}
