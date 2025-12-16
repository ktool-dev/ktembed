package dev.ktool.embed

import okio.Path

/**
 * Represents a set of resources. A ResourceDirectory can be queried for specific resources
 * using a path.
 */
interface ResourceDirectory {
    /**
     * Retrieves a resource from this directory using the specified path.
     *
     * @param path The unique path of the resource to be retrieved.
     * @return The resource identified by the path, or null if no such resource exists.
     */
    operator fun get(path: String): Resource?

    /**
     * A unique identifier for this directory.
     */
    val key: String

    val allPaths: List<String>

    /**
     * Converts the resource directory into a Resources object, allowing resources to be managed
     * with specific settings for in-memory storage and caching.
     *
     * @param inMemoryCutoff The size threshold, in bytes, above which resources will be stored
     * on disk rather than in memory. Defaults to the constant value [IN_MEMORY_CUT_OFF].
     * @param cacheDirectory The directory where disk-based resources will be stored. If not
     * specified, a temporary directory will be used. Defaults to the result of [getTempDirectory].
     */
    fun toResources(inMemoryCutoff: Long = IN_MEMORY_CUT_OFF, cacheDirectory: Path? = getTempDirectory()) =
        Resources(this, inMemoryCutoff, cacheDirectory)
}