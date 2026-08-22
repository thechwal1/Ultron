package com.talha.ultron.brain

/**
 * Simple ring buffer for tracking AI provider latency. Not persisted —
 * resets on process death, which is fine for a quick diagnostic tool.
 */
object LatencyTracker {
    private val samples = ArrayDeque<Long>(50)
    private const val MAX_SAMPLES = 50

    fun record(ms: Long) {
        if (samples.size >= MAX_SAMPLES) samples.removeFirst()
        samples.addLast(ms)
    }

    fun summary(): String {
        if (samples.isEmpty()) return "No latency data yet."
        val avg = samples.average().toLong()
        val min = samples.minOrNull() ?: 0
        val max = samples.maxOrNull() ?: 0
        val count = samples.size
        return "Latency (last $count calls):\navg ${avg}ms | min ${min}ms | max ${max}ms"
    }
}
