package dev.ktool.embed

import dev.ktool.gen.types.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM

/**
 * Processes asset files from directories and generates Kotlin code that implements ResourceDirectory.
 *
 * This class scans directories for resource files, encodes them as Base64 chunks, and generates
 * a Kotlin class that contains all resources as embedded data.
 *
 * @property fileSystem The file system to use for reading files (defaults to SYSTEM)
 */
class AssetProcessor(private val fileSystem: FileSystem = FileSystem.SYSTEM) {
    /**
     * Processes a list of directories and generates a ResourceDirectory implementation.
     *
     * @param directories List of directory paths to scan for resources
     * @param packageName The package name for the generated class
     * @return A KotlinFile containing the generated code
     */
    fun process(directories: List<String>, packageName: String): KotlinFile =
        genResourceDirectory(packageName, directories.flatMap { scanDirectory(it.toPath()) })

    /**
     * Scans a directory recursively and collects all files as ResourceData.
     *
     * @param path The directory path to scan
     * @param baseDir The base directory for computing relative paths (defaults to directory)
     * @return List of ResourceData objects representing all files found
     */
    private fun scanDirectory(path: Path, baseDir: Path = path): List<ResourceData> {
        if (!path.exists || !path.isDirectory) return listOf()

        return buildList {
            path.list().forEach {
                when {
                    it.isDirectory -> addAll(scanDirectory(it, baseDir))
                    else -> add(createResourceData(path, baseDir))
                }
            }
        }
    }

    /**
     * Creates ResourceData from a file.
     *
     * @param filePath The path to the file
     * @param baseDir The base directory for computing the relative path
     * @return ResourceData containing the file's encoded chunks and metadata
     */
    private fun createResourceData(filePath: Path, baseDir: Path): ResourceData {
        val relativePath = filePath.normalized().toString()
            .removePrefix(baseDir.normalized().toString())
            .trimStart('/')

        val key = relativePath.replace("/", "_").replace("\\", "_")

        val chunks = buildList {
            fileSystem.read(filePath) {
                while (!exhausted()) {
                    add(readByteString(RESOURCE_CHUNK_SIZE).base64())
                }
            }
        }

        return ResourceData(path = relativePath, chunks = chunks, key = key)
    }

    /**
     * Generates a KotlinFile containing a ResourceDirectory implementation.
     *
     * @param packageName The package name for the generated class
     * @param resources List of resources to include in the generated class
     * @return A KotlinFile with the complete implementation
     */
    private fun genResourceDirectory(packageName: String, resources: List<ResourceData>) = KotlinFile(packageName) {
        +Import("dev.ktool.embed.Resource")
        +Import("dev.ktool.embed.ResourceDirectory")

        +Class("ResourceDirectory") {
            +SuperType("ResourceDirectory")

            +Property("key", type = StringType, modifiers = listOf(Modifier.Override)) {
                initializer = ExpressionBody("\"${packageName.replace('.', '_')}\"")
            }

            +Property(
                "resources",
                type = Type("Map", typeArguments = listOf(TypeArgument("String"), TypeArgument("Resource"))),
                modifiers = listOf(Modifier.Private)
            ) {
                initializer = ExpressionBody(buildResourcesMapInitializer(resources))
            }

            +Function("get", returnType = Type("Resource?")) {
                +Modifier.Override
                +Modifier.Operator
                +Parameter("path", StringType)
                body = ExpressionBody("resources[path]")
            }
        }
    }

    /**
     * Builds the initializer for the resource map.
     *
     * @param resources List of resources to include
     * @return String containing the Kotlin code for the map initializer
     */
    private fun buildResourcesMapInitializer(resources: List<ResourceData>): String {
        if (resources.isEmpty()) {
            return "mapOf()"
        }

        val entries = resources.joinToString(",\n        ") { resource ->
            val chunksStr = resource.chunks.joinToString(",\n            ") { "\"$it\"" }

            """
            "${resource.path}" to Resource(
                chunks = listOf(
                    $chunksStr
                ),
                key = "${resource.key}"
            )
            """.trimIndent()
        }

        return """
        mapOf(
            $entries
        )
        """.trimIndent()
    }

    private data class ResourceData(val path: String, val chunks: List<String>, val key: String)

    private fun Path.list(): List<Path> = fileSystem.list(this)
    private val Path.isDirectory: Boolean get() = fileSystem.metadataOrNull(this)?.isDirectory == true
    private val Path.exists: Boolean get() = fileSystem.exists(this)
}