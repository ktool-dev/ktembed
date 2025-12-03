package dev.ktool.ktembed

/**
 * An embedded resource that streams chunks to a cache file on disk, then reads from the cache.
 * This is ideal for large resources where keeping everything in memory would be wasteful.
 */
class DiskCachedResource(
    override val path: String,
    override val contentHash: String,
    private val base64Chunks: List<String>
) : EmbeddedResource {
    
    override val accessMode: ResourceAccessMode = ResourceAccessMode.DISK_CACHED
    
    private val cacheFilePath: String by lazy {
        val extension = path.substringAfterLast('.', "")
        KtEmbedCache.getCachePath(contentHash, extension.ifEmpty { null })
    }
    
    /**
     * Ensures the cache file exists, creating it if necessary.
     */
    private fun ensureCached() {
        if (FileIO.exists(cacheFilePath)) {
            return // Cache hit!
        }
        
        // Cache miss - decode and write to cache
        synchronized(this) {
            // Double-check after acquiring lock
            if (FileIO.exists(cacheFilePath)) {
                return
            }
            
            // Ensure cache directory exists
            val cacheDir = KtEmbedCache.getCacheDirectory()
            FileIO.createDirectories(cacheDir)
            
            // Decode and write to cache
            val decoded = Base64Decoder.decode(base64Chunks)
            FileIO.writeBytes(cacheFilePath, decoded)
        }
    }
    
    override suspend fun asBytes(): ByteArray {
        ensureCached()
        return FileIO.readBytes(cacheFilePath)
    }
    
    override suspend fun asStream(): Sequence<ByteArray> = sequence {
        ensureCached()
        
        // Read the entire file and yield in chunks
        val allBytes = FileIO.readBytes(cacheFilePath)
        val chunkSize = 8192
        
        var offset = 0
        while (offset < allBytes.size) {
            val size = minOf(chunkSize, allBytes.size - offset)
            yield(allBytes.copyOfRange(offset, offset + size))
            offset += size
        }
    }
    
    override fun toString(): String {
        return "DiskCachedResource(path='$path', hash='$contentHash', cache='$cacheFilePath')"
    }
}

/**
 * Simple synchronization helper for multiplatform.
 * Note: This is a simplified implementation. For production use,
 * consider using kotlinx.coroutines Mutex or platform-specific locking.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect inline fun <T> synchronized(lock: Any, block: () -> T): T
