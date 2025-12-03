package dev.ktool.ktembed

internal actual object CacheDirectory {
    actual fun getCacheDir(): String {
        // Similar to JS, WASM doesn't have a traditional filesystem
        return ".ktembed-cache"
    }
    
    actual fun getTempDir(): String {
        return ".ktembed-temp"
    }
}
