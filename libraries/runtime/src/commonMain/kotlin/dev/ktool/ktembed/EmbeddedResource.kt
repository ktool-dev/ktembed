package dev.ktool.ktembed

/**
 * Represents a static resource that has been embedded into the application at build time.
 *
 * Resources can be accessed using different strategies depending on memory and performance requirements:
 * - [InMemory]: Decodes the resource once and keeps it in memory (fast access, higher memory usage)
 * - [DiskCached]: Streams chunks to a cache file on disk (low memory usage, more I/O)
 */
sealed interface EmbeddedResource {
    /**
     * The path/name of the embedded resource
     */
    val path: String

    /**
     * The content hash of the resource, used for cache versioning
     */
    val contentHash: String

    /**
     * The access strategy for this resource
     */
    val accessMode: ResourceAccessMode

    /**
     * Get the resource content as a byte array.
     * 
     * @return The decoded resource bytes
     * @throws ResourceAccessException if the resource cannot be accessed
     */
    suspend fun asBytes(): ByteArray

    /**
     * Get the resource content as a string using UTF-8 encoding.
     * 
     * @return The decoded resource as a UTF-8 string
     * @throws ResourceAccessException if the resource cannot be accessed
     */
    suspend fun asString(): String = asBytes().decodeToString()

    /**
     * Open a stream to read the resource content.
     * This is useful for large resources that should not be loaded entirely into memory.
     * 
     * @return A sequence of byte chunks representing the resource content
     * @throws ResourceAccessException if the resource cannot be accessed
     */
    suspend fun asStream(): Sequence<ByteArray>
}

/**
 * Exception thrown when a resource cannot be accessed
 */
class ResourceAccessException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
