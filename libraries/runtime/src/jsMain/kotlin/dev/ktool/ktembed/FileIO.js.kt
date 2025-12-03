package dev.ktool.ktembed

// For JS/Browser, we don't have a traditional filesystem
// Resources will be kept in memory or IndexedDB in future
internal actual object FileIO {
    private val inMemoryStorage = mutableMapOf<String, ByteArray>()
    
    actual fun exists(path: String): Boolean {
        return inMemoryStorage.containsKey(path)
    }
    
    actual fun writeBytes(path: String, bytes: ByteArray) {
        inMemoryStorage[path] = bytes
    }
    
    actual fun readBytes(path: String): ByteArray {
        return inMemoryStorage[path] ?: throw ResourceAccessException("File not found: $path")
    }
    
    actual fun createDirectories(path: String) {
        // No-op for JS
    }
}
