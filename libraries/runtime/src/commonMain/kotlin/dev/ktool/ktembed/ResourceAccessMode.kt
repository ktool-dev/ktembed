package dev.ktool.ktembed

/**
 * Defines how an embedded resource should be accessed at runtime.
 */
enum class ResourceAccessMode {
    /**
     * Decode the resource once and keep it in memory.
     * 
     * **Benefits:**
     * - Fast access after initial decode
     * - No disk I/O after first access
     * - Thread-safe lazy initialization
     * 
     * **Trade-offs:**
     * - Higher memory usage
     * - Not suitable for very large resources
     * 
     * **Best for:** Small to medium resources (< 10MB) that are accessed frequently
     */
    IN_MEMORY,

    /**
     * Stream chunks to a cache file on disk, then read from the cache.
     * 
     * **Benefits:**
     * - Low memory usage
     * - Suitable for large resources
     * - Cache files are reused across app runs (based on content hash)
     * 
     * **Trade-offs:**
     * - More I/O operations
     * - Slightly slower access
     * - Requires disk space for cache
     * 
     * **Best for:** Large resources (> 10MB) or resources accessed infrequently
     */
    DISK_CACHED
}
