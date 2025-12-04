package dev.ktool.embed.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Gradle task that generates Kotlin code for embedded resources.
 */
abstract class GenerateEmbeddedResourcesTask : DefaultTask() {
    
    @get:Internal
    abstract val extension: Property<KtEmbedExtension>
    
    @get:OutputDirectory
    val outputDirectory: DirectoryProperty
        get() = extension.get().outputDir
    
    @TaskAction
    fun generate() {
        val ext = extension.get()
        val resourceDirs = ext.resourceDirs.get()
        val packageName = ext.packageName.get()
        val outputDir = ext.outputDir.get().asFile
        val shardSize = ext.shardSize.get()
        val chunkSize = ext.chunkSize.get()
        val generateInMemory = ext.generateInMemory.get()
        val generateDiskCached = ext.generateDiskCached.get()
        
        logger.lifecycle("KtEmbed: Scanning resource directories: $resourceDirs")
        
        // Scan for resources
        val scanner = ResourceScanner(resourceDirs, project.projectDir)
        val resources = scanner.scan()
        
        logger.lifecycle("KtEmbed: Found ${resources.size} resources")
        
        if (resources.isEmpty()) {
            logger.warn("KtEmbed: No resources found to embed")
            return
        }
        
        // Generate code
        val generator = CodeGenerator(
            packageName = packageName,
            chunkSize = chunkSize,
            shardSize = shardSize,
            generateInMemory = generateInMemory,
            generateDiskCached = generateDiskCached
        )
        
        generator.generate(resources, outputDir)
        
        val totalSize = resources.sumOf { it.size }
        logger.lifecycle("KtEmbed: Generated code for ${resources.size} resources (${formatBytes(totalSize)})")
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
