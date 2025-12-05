package dev.ktool.embed

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv

/**
 * A class that provides functionality to handle resources stored in a ResourceDirectory. Resources can be accessed by a
 * path and read as strings or bytes, and written to an output sink. The class also supports caching and optimization
 * strategies for resource handling.
 *
 * @property resourceDirectory The ResourceDirectory from which the resources will be accessed.
 * @property inMemoryCutoff The size limit in bytes for caching resources in memory when calling the `write` function
 * with no OptimizationStrategy. The default is 6MB.
 */
class Resources(
    private val resourceDirectory: ResourceDirectory,
    private val inMemoryCutoff: Int = IN_MEMORY_CUT_OFF,
) : BaseResources(
    resourceDirectory,
    inMemoryCutoff,
    getSystemTempDirectory(),
    FileSystem.SYSTEM
)

@OptIn(ExperimentalForeignApi::class)
private fun getSystemTempDirectory(): Path {
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