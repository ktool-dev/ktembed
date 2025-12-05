package dev.ktool.embed

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
}