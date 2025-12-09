package dev.ktool.embed

import dev.ktool.gen.types.ExpressionBody
import dev.ktool.gen.types.KotlinFile
import dev.ktool.gen.types.Property
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.SYSTEM

private const val MAX_CHUNKS_PER_KOTLIN_FILE = 100

/**
 * Processes asset files from directories and generates Kotlin code that implements ResourceDirectory.
 *
 * This class scans directories for resource files, encodes them as Base64 chunks, and generates
 * multiple Kotlin files: one ResourceDirectory class and multiple ResourcesN.kt files containing
 * the Base64-encoded chunks. This approach prevents large files from being loaded into memory
 * and avoids generating overly large class files.
 *
 * @property fileSystem The file system to use for reading files (defaults to SYSTEM)
 */
class AssetProcessor(private val fileSystem: FileSystem = FileSystem.SYSTEM) {
    private val resourceDirectoryGenerator = ResourceDirectoryGenerator(fileSystem)

    /**
     * Processes a list of directories and generates a ResourceDirectory implementation plus ResourceChunk files.
     *
     * @param directories List of directory paths to scan for resources
     * @param packageName The package name for the generated classes
     * @param baseOutputDir The base directory to write the generated Kotlin code into
     * @param ignore A filter that will be called for all paths and the path ignored if it returns true
     */
    fun process(
        directories: List<Path>,
        packageName: String,
        baseOutputDir: Path,
        ignore: (Path) -> Boolean = { true },
    ) {
        val filePaths = directories.flatMap { scanDirectoryForPaths(path = it, ignore = ignore) }
        val resourceMappings = generateResourceFiles(packageName, filePaths, baseOutputDir)
        val resourceDirectoryFile = resourceDirectoryGenerator.generate(packageName, resourceMappings)
        resourceDirectoryFile.write(baseOutputDir, "ResourceDirectory")
    }

    /**
     * Scans a directory recursively and collects all file paths (without reading content).
     *
     * @param path The directory path to scan
     * @param baseDir The base directory for computing relative paths (defaults to directory)
     * @return List of FilePathInfo objects containing path metadata
     */
    private fun scanDirectoryForPaths(path: Path, baseDir: Path = path, ignore: (Path) -> Boolean): List<FilePathInfo> {
        if (!path.exists || !path.isDirectory) return listOf()

        return buildList {
            path.list().forEach {
                when {
                    ignore(it) -> Unit
                    it.isDirectory -> addAll(scanDirectoryForPaths(it, baseDir, ignore))
                    else -> {
                        val relativePath = it.normalized().toString()
                            .removePrefix(baseDir.normalized().toString())
                            .trimStart('/')

                        val key = relativePath.replace("/", "_").replace("\\", "_").replace(".", "_")

                        add(FilePathInfo(absolutePath = it, relativePath = relativePath, key = key))
                    }
                }
            }
        }
    }

    /**
     * Generates all Kotlin files: ResourceDirectory + ResourcesN files.
     * Processes files one at a time to minimize memory usage.
     *
     * @param packageName The package name for the generated classes
     * @param filePaths List of file paths to process
     * @return List of generated KotlinFiles
     */
    private fun generateResourceFiles(
        packageName: String,
        filePaths: List<FilePathInfo>,
        baseOutputDir: Path
    ): List<ResourceMapping> {
        val kotlinFile = KotlinFile(packageName)
        var fileCounter = 0
        var chunkCount = 0

        fun writeFile() {
            if (chunkCount > 0) {
                kotlinFile.write(baseOutputDir, "ResourceChunks${++fileCounter}")
                kotlinFile.members.clear()
            }
            chunkCount = 0
        }

        return buildList {
            filePaths.forEach { fileInfo ->
                val chunkVariableName = "RESOURCE_${size + 1}"

                kotlinFile.members += Property(chunkVariableName) {
                    initializer = ExpressionBody {
                        write("listOf(")
                        withIndent {
                            val chunkBuffer = Buffer()
                            var chunkSize = 0L

                            fun writeChunk() {
                                newLine(""""${chunkBuffer.readByteString().compress().base64()}",""")
                                chunkCount++
                                chunkBuffer.clear()
                                chunkSize = 0L
                            }
                            
                            fileSystem.read(fileInfo.absolutePath) {
                                while (!exhausted()) {
                                    chunkSize += buffer.size
                                    chunkBuffer.write(buffer.readByteString())
                                    if (chunkSize >= RESOURCE_CHUNK_SIZE) {
                                        writeChunk()
                                    }
                                }

                                if (chunkSize > 0) {
                                    writeChunk()
                                }
                            }
                        }
                        newLine(")")
                    }
                }

                add(ResourceMapping(fileInfo.relativePath, chunkVariableName, fileInfo.key))

                if (chunkCount > MAX_CHUNKS_PER_KOTLIN_FILE) {
                    writeFile()
                }
            }

            writeFile()
        }
    }

    private fun KotlinFile.write(baseOutputDir: Path, name: String) {
        val folders = packageName?.split(".") ?: error("package name not found")

        folders.fold(baseOutputDir) { dir, folder -> dir.resolve(folder) }
            .resolve("$name.kt")
            .mkDirs()
            .write(render())
    }

    private data class FilePathInfo(val absolutePath: Path, val relativePath: String, val key: String)

    private fun Path.list(): List<Path> = fileSystem.list(this)
    private val Path.isDirectory: Boolean get() = fileSystem.metadataOrNull(this)?.isDirectory == true
    private val Path.exists: Boolean get() = fileSystem.exists(this)
    private fun Path.write(text: String) = fileSystem.write(this, false) { writeUtf8(text) }

    private fun Path.mkDirs(): Path {
        if (exists) return this

        if (isDirectory) {
            fileSystem.createDirectories(this, false)
        } else {
            fileSystem.createDirectories(this.parent!!, false)
        }

        return this
    }
}
