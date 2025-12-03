package dev.ktool.ktembed

// JS is single-threaded, so synchronization is a no-op
internal actual inline fun <T> synchronized(lock: Any, block: () -> T): T {
    return block()
}
