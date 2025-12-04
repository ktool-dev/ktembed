package dev.ktool.embed

import okio.Path
import okio.Path.Companion.toPath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getSystemTempDirectory(): Path? {
    // Try TMPDIR first (standard on Unix-like systems including macOS)
    val tmpDir = getenv("TMPDIR")?.toKString()
    if (tmpDir != null && tmpDir.isNotEmpty()) {
        return tmpDir.toPath()
    }
    
    // Fall back to TMP (Windows)
    val tmp = getenv("TMP")?.toKString()
    if (tmp != null && tmp.isNotEmpty()) {
        return tmp.toPath()
    }
    
    // Fall back to TEMP (Windows alternative)
    val temp = getenv("TEMP")?.toKString()
    if (temp != null && temp.isNotEmpty()) {
        return temp.toPath()
    }
    
    // Last resort: use /tmp (Unix-like systems)
    return "/tmp".toPath()
}
