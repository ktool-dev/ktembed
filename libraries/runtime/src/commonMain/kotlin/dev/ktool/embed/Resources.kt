package dev.ktool.embed

import okio.*
import okio.ByteString.Companion.decodeBase64

enum class OptimizationStrategy {
    Memory, Speed,
}

/**
 * A class that provides functionality to handle resources stored in a ResourceDirectory. Resources can be accessed by a
 * path and read as strings or bytes, and written to an output sink. The class also supports caching and optimization
 * strategies for resource handling.
 *
 * @property resourceDirectory The ResourceDirectory from which the resources will be accessed.
 * @property inMemoryCutoff The size limit in bytes for caching resources in memory, default is 6MB.
 * @property cacheDirectory The directory used for caching resources on the file system, default is a temp directory.
 * @property fileSystem The file system instance used for file operations, this should only be set for testing.
 */
class Resources(
    private val resourceDirectory: ResourceDirectory,
    private val inMemoryCutoff: Int = IN_MEMORY_CUT_OFF,
    private val cacheDirectory: Path? = getSystemTempDirectory(),
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    private val validatedFiles = mutableSetOf<Path>()

    /**
     * Checks if a resource exists at the specified path.
     *
     * @param path The relative path to the resource that needs to be checked.
     * @return `true` if the resource exists, `false` otherwise.
     */
    fun exists(path: String) = resourceDirectory[path] != null

    /**
     * Converts the resource located at the given path to its string representation. The first time this is called, it
     * will cache the String in memory. All later calls will use the cached String. If you don't want the String in
     * memory, you should call the `write` function instead. This will throw an exception if the `path` doesn't exist in
     * the ResourceDirectory.
     *
     * @param path The relative path to the resource that should be converted to a string.
     * @return The string representation of the resource.
     */
    fun asString(path: String) = path.toResource().asString

    /**
     * Converts the resource located at the given path into its binary representation. The first time this is called, it
     * will cache the ByteString in memory. All later calls will use the cached ByteString. If you don't want the
     * ByteString in memory, you should call the `write` function instead. This will throw an exception if the `path`
     * doesn't exist in the ResourceDirectory.
     *
     * @param path The relative path to the resource that should be converted to bytes.
     * @return The byte representation of the resource.
     */
    fun asBytes(path: String) = path.toResource().asBytes

    /**
     * Writes the resource located at the specified path to the provided output. This will throw an exception if the
     * `path` doesn't exist in the ResourceDirectory.
     *
     * @param path The relative path to the resource that should be written.
     * @param output The output sink where the resource will be written.
     */
    fun write(path: String, output: Sink) {
        val resource = path.toResource()
        val strategy = if (resource.size > inMemoryCutoff) OptimizationStrategy.Memory else OptimizationStrategy.Speed
        write(resource, output, strategy)
    }

    /**
     * Writes the resource located at the specified path to the provided output using the given OptimizationStrategy.
     * When OptimizationStrategy is Memory, it will write the content to a file on disk fist to avoid loading the whole
     * content into memory. If the Optimization strategy is Speed, it will use the ByteString on the EmbeddedResource
     * and write it to the `sink`. This will throw an exception if the `path` doesn't exist in the ResourceDirectory.
     *
     * @param path The relative path to the resource that should be written.
     * @param output The output sink where the resource will be written.
     */
    fun write(path: String, output: Sink, optimizationStrategy: OptimizationStrategy) {
        write(path.toResource(), output, optimizationStrategy)
    }

    private fun write(resource: EmbeddedResource, output: Sink, optimizationStrategy: OptimizationStrategy) {
        output.buffer().use { buffer ->
            if (optimizationStrategy == OptimizationStrategy.Speed) {
                buffer.write(resource.asBytes)
            } else {
                val filePath = ensureFile(resourceDirectory.key, resource)
                if (filePath != null) {
                    fileSystem.source(filePath).use { buffer.writeAll(it) }
                } else {
                    buffer.write(resource.asBytes)
                }
            }
        }
    }

    private fun ensureFile(resourceDirKey: String, resource: EmbeddedResource): Path? {
        return try {
            val cacheDirectory = cacheDirectory(resourceDirKey) ?: return null
            val filePath = cacheDirectory / resource.key

            if (filePath in validatedFiles) {
                return filePath
            }

            // If the file exists, validate it once and cache the result
            if (filePath.exists() && computeHash(filePath, fileSystem) == computeHash(resource.chunks)) {
                validatedFiles.add(filePath)
                return filePath
            }

            filePath.write {
                for (chunk in resource.chunks) {
                    chunk.decodeBase64()?.also { write(it) }
                }
            }
            validatedFiles.add(filePath)

            filePath
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheDirectory(resourceDirKey: String) = cacheDirectory?.resolve(resourceDirKey)?.apply { createDirs() }

    private fun String.toResource() = resourceDirectory[this] ?: error("Resource not found: $this")

    internal fun Path.exists() = fileSystem.exists(this)
    internal fun Path.createDirs() = fileSystem.createDirectories(this)
    internal fun <T> Path.write(action: BufferedSink.() -> T) = fileSystem.write(this, false, action)
}
