package dev.ktool.ktembed

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual object CacheDirectory {
    actual fun getCacheDir(): String {
        // Try to get XDG_CACHE_HOME or HOME environment variables
        val xdgCache = getenv("XDG_CACHE_HOME")?.toKString()
        if (xdgCache != null && xdgCache.isNotEmpty()) {
            ensureDirectoryExists(xdgCache)
            return xdgCache
        }
        
        val home = getenv("HOME")?.toKString()
        if (home != null && home.isNotEmpty()) {
            val cacheDir = "$home/.cache"
            ensureDirectoryExists(cacheDir)
            return cacheDir
        }
        
        // Fallback to /tmp
        return "/tmp"
    }
    
    actual fun getTempDir(): String {
        val tmpDir = getenv("TMPDIR")?.toKString()
            ?: getenv("TEMP")?.toKString()
            ?: getenv("TMP")?.toKString()
            ?: "/tmp"
        
        ensureDirectoryExists(tmpDir)
        return tmpDir
    }
    
    private fun ensureDirectoryExists(path: String) {
        // Note: Directory creation is platform-specific and complex in K/N
        // We'll rely on the directory already existing or being created by the OS
        // Future improvement: use platform-specific APIs for directory creation
    }
}
