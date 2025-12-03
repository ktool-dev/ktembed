package dev.ktool.ktembed

// For WASM, similar to JS - in-memory storage
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
        // No-op for WASM
    }
}
