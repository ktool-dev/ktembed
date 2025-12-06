package dev.ktool.embed.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles

/**
 * Extension for configuring the KtEmbed plugin.
 *
 * Example usage:
 * ```kotlin
 * ktembed {
 *     packageName = "com.example.resources"
 *     resourceDirectories = files("src/main/resources", "assets")
 *     filter = { path -> path.endsWith(".tmp") }
 * }
 * ```
 */
abstract class KtEmbedExtension {
    /**
     * The package name for the generated ResourceDirectory class.
     */
    @get:Input
    abstract val packageName: Property<String>

    fun setPackageName(value: String) {
        packageName.set(value)
    }

    /**
     * The directories to scan for resource files.
     */
    @get:InputFiles
    abstract val resourceDirectories: ConfigurableFileCollection

    fun setResourceDirectories(vararg paths: Any) {
        resourceDirectories.setFrom(*paths)
    }

    /**
     * A filter function that determines which paths to ignore.
     * The function should return true for paths to ignore, false otherwise.
     * Defaults to ignoring nothing.
     */
    @get:Input
    abstract val filter: Property<(String) -> Boolean>

    fun setFilter(value: (String) -> Boolean) {
        filter.set(value)
    }

    init {
        // Default to not filtering anything
        filter.convention { false }
    }
}
