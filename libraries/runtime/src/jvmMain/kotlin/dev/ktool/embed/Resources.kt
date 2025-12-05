package dev.ktool.embed

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.sink
import java.io.OutputStream

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
    private val inMemoryCutoff: Long = IN_MEMORY_CUT_OFF,
) : BaseResources(
    resourceDirectory,
    inMemoryCutoff,
    System.getProperty("java.io.tmpdir")?.toPath(),
    FileSystem.SYSTEM
) {
    /**
     * Writes the resource located at the specified path to the provided output stream. This will throw an exception if
     * the `path` does not exist in the ResourceDirectory.
     *
     * @param path The relative path to the resource that should be written.
     * @param out The output stream where the resource will be written.
     */
    fun write(path: String, out: OutputStream) {
        write(path, out.sink())
    }

    /**
     * Writes the resource located at the specified path to the provided output stream using the specified optimization strategy.
     * The optimization strategy determines how the resource content is handled while writing. If the strategy is `Memory`, the
     * content is written to a temporary file on disk first to minimize memory usage. If the strategy is `Speed`, the resource
     * is directly processed from memory for faster writing. This method will throw an exception if the `path` does not exist
     * in the ResourceDirectory.
     *
     * @param path The relative path to the resource that should be written.
     * @param out The output stream where the resource will be written.
     * @param strategy The optimization strategy to use when writing the resource.
     */
    fun write(path: String, out: OutputStream, strategy: OptimizationStrategy) {
        write(path, out.sink(), strategy)
    }
}
