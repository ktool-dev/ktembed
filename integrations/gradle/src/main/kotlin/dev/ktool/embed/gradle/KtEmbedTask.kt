package dev.ktool.embed.gradle

import dev.ktool.embed.AssetProcessor
import okio.Path.Companion.toOkioPath
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Task that processes resource directories and generates Kotlin code using AssetProcessor.
 */
@CacheableTask
abstract class KtEmbedTask : DefaultTask() {

    /**
     * The package name for the generated ResourceDirectory class.
     */
    @get:Input
    abstract val packageName: Property<String>

    /**
     * The directories to scan for resource files.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourceDirectories: ConfigurableFileCollection

    /**
     * A filter function that determines which paths to ignore.
     * The function should return true for paths to ignore, false otherwise.
     */
    @get:Input
    abstract val exclude: Property<(String) -> Boolean>

    /**
     * The output directory where generated Kotlin files will be written.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        group = "ktembed"
        description = "Generate Kotlin ResourceDirectory from asset files"
    }

    @TaskAction
    fun generate() {
        val packageNameValue = packageName.get()
        val outputDir = outputDirectory.get().asFile.toOkioPath()
        val filterFunc = exclude.get()

        val directories = resourceDirectories.files.map { it.toOkioPath() }

        require(packageNameValue.isNotEmpty()) { "Package name must not be empty, you must set it using `packageName`" }
        require(directories.isNotEmpty()) { "You must specify at least one directory for `resourceDirectories`" }

        logger.lifecycle("Generating ResourceDirectory for package: $packageNameValue")
        logger.lifecycle("Resource directories: ${directories.joinToString(", ")}")
        logger.lifecycle("Output directory: $outputDir")

        val processor = AssetProcessor()
        processor.process(
            directories = directories,
            packageName = packageNameValue,
            baseOutputDir = outputDir,
            ignore = { path -> filterFunc(path.toString()) }
        )

        logger.lifecycle("Successfully generated ResourceDirectory")
    }
}
