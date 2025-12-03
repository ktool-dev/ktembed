package dev.ktool.ktembed

internal actual object CacheDirectory {
    actual fun getCacheDir(): String {
        // In browser/Node.js, we don't have a traditional cache directory
        // Return a logical path that will be used for IndexedDB or localStorage keys
        return ".ktembed-cache"
    }
    
    actual fun getTempDir(): String {
        return ".ktembed-temp"
    }
}
