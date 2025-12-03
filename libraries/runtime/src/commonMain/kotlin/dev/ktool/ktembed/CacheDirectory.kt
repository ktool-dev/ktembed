package dev.ktool.ktembed

/**
 * Platform-specific cache directory provider.
 * Each platform implements this to return the appropriate cache/temp directory path.
 */
internal expect object CacheDirectory {
    /**
     * Returns the platform-specific cache directory path where KtEmbed can store cached resources.
     * The directory will be created if it doesn't exist.
     * 
     * @return Absolute path to the cache directory
     */
    fun getCacheDir(): String
    
    /**
     * Returns a platform-specific temporary directory path.
     * 
     * @return Absolute path to the temp directory
     */
    fun getTempDir(): String
}

/**
 * Provides access to cache paths for KtEmbed resources.
 */
object KtEmbedCache {
    private const val CACHE_SUBDIR = "ktembed"
    
    /**
     * Returns the KtEmbed-specific cache directory.
     * This is a subdirectory of the platform's cache directory.
     */
    fun getCacheDirectory(): String {
        val baseCache = CacheDirectory.getCacheDir()
        return "$baseCache/$CACHE_SUBDIR"
    }
    
    /**
     * Returns the full path for a cached resource file based on its content hash.
     * 
     * @param contentHash The content hash of the resource
     * @param extension Optional file extension (e.g., "png", "json")
     * @return Full path to the cache file
     */
    fun getCachePath(contentHash: String, extension: String? = null): String {
        val cacheDir = getCacheDirectory()
        return if (extension != null) {
            "$cacheDir/$contentHash.$extension"
        } else {
            "$cacheDir/$contentHash"
        }
    }
}
