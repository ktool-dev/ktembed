package dev.ktool.ktembed

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual object FileIO {
    // For now, use in-memory storage similar to JS/WASM
    // A full native file implementation would require more complex platform-specific code
    private val inMemoryStorage = mutableMapOf<String, ByteArray>()
    
    actual fun exists(path: String): Boolean {
        // Try both in-memory and actual filesystem
        if (inMemoryStorage.containsKey(path)) {
            return true
        }
        
        memScoped {
            val statBuf = alloc<stat>()
            return stat(path, statBuf.ptr) == 0
        }
    }
    
    actual fun writeBytes(path: String, bytes: ByteArray) {
        // Store in memory for simplicity
        // TODO: Implement proper file writing for native targets
        inMemoryStorage[path] = bytes
    }
    
    actual fun readBytes(path: String): ByteArray {
        // Try in-memory first
        return inMemoryStorage[path] 
            ?: throw ResourceAccessException("File not found in cache: $path")
    }
    
    actual fun createDirectories(path: String) {
        // No-op for simplified implementation
    }
}
