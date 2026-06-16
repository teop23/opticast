package com.opticast.stream

class ReconnectController(
    private val baseMs: Long,
    private val capMs: Long,
    val maxAttempts: Int
) {
    /** Exponential backoff: base * 2^(attempt-1), capped at capMs. Attempt is 1-based. */
    fun delayForAttempt(attempt: Int): Long {
        val raw = baseMs shl (attempt - 1).coerceAtMost(30)
        return raw.coerceAtMost(capMs)
    }

    /**
     * Tries [attempt] up to [maxAttempts] times. Waits delayForAttempt(n) BEFORE attempts 2..n.
     * Returns true on first success, false if all attempts fail.
     */
    suspend fun run(delayFn: suspend (Long) -> Unit, attempt: suspend () -> Boolean): Boolean {
        for (n in 1..maxAttempts) {
            if (n > 1) delayFn(delayForAttempt(n - 1))
            if (attempt()) return true
        }
        return false
    }
}
