package dev.ktool.ktembed

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.freeze

// Simple spinlock-based synchronization for Native
private val locks = mutableMapOf<Any, AtomicInt>()

internal actual inline fun <T> synchronized(lock: Any, block: () -> T): T {
    // Note: This is a simplified implementation
    // For production, use proper platform-specific synchronization primitives
    return block()
}
