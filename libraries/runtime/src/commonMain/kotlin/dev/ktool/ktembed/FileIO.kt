package dev.ktool.ktembed

/**
 * Platform-specific file I/O operations.
 */
internal expect object FileIO {
    /**
     * Check if a file exists at the given path.
     */
    fun exists(path: String): Boolean
    
    /**
     * Write bytes to a file, creating parent directories if needed.
     */
    fun writeBytes(path: String, bytes: ByteArray)
    
    /**
     * Read all bytes from a file.
     */
    fun readBytes(path: String): ByteArray
    
    /**
     * Create a directory (and parent directories if needed).
     */
    fun createDirectories(path: String)
}
