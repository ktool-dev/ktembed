package dev.ktool.ktembed

internal actual inline fun <T> synchronized(lock: Any, block: () -> T): T {
    return kotlin.synchronized(lock, block)
}
