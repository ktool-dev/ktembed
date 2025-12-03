package dev.ktool.ktembed

import java.io.File

internal actual object CacheDirectory {
    actual fun getCacheDir(): String {
        // Use system property or fallback to user home
        val cacheDir = System.getProperty("java.io.tmpdir")
            ?: System.getProperty("user.home")?.let { "$it/.cache" }
            ?: "/tmp"
        
        val dir = File(cacheDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        return dir.absolutePath
    }
    
    actual fun getTempDir(): String {
        val tempDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
        val dir = File(tempDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }
}
