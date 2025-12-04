package dev.ktool.embed.gradle

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration extension for the KtEmbed plugin.
 */
abstract class KtEmbedExtension @Inject constructor(private val project: Project) {
    
    /**
     * List of resource directories to scan for embedded resources.
     * Defaults to ["src/main/resources"]
     */
    abstract val resourceDirs: ListProperty<String>
    
    /**
     * Package name for generated Kotlin code.
     * Defaults to "embedded"
     */
    abstract val packageName: Property<String>
    
    /**
     * Output directory for generated Kotlin source files.
     * Defaults to build/generated/embed
     */
    abstract val outputDir: DirectoryProperty
    
    /**
     * Maximum number of resources per generated file (for sharding).
     * Set to 0 to disable sharding.
     * Defaults to 50
     */
    abstract val shardSize: Property<Int>
    
    /**
     * Size of Base64 string chunks (in characters).
     * Defaults to 32000 (to stay well under string literal limits)
     */
    abstract val chunkSize: Property<Int>
    
    /**
     * Whether to generate InMemory resources (faster access, more RAM).
     * Defaults to true
     */
    abstract val generateInMemory: Property<Boolean>
    
    /**
     * Whether to generate DiskCached resources (slower access, less RAM).
     * Defaults to false
     */
    abstract val generateDiskCached: Property<Boolean>
    
    init {
        resourceDirs.convention(listOf("src/main/resources"))
        packageName.convention("embedded")
        outputDir.convention(project.layout.buildDirectory.dir("generated/embed"))
        shardSize.convention(50)
        chunkSize.convention(32000)
        generateInMemory.convention(true)
        generateDiskCached.convention(false)
    }
}
