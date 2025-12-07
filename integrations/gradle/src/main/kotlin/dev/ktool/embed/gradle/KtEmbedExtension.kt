package dev.ktool.embed.gradle

/**
 * Extension for configuring the KtEmbed plugin.
 *
 * Example usage:
 * ```kotlin
 * ktembed {
 *     packageName = "com.example.resources"
 *     resourceDirectories = listOf("src/main/resources", "assets")
 *     exclude = { it.endsWith(".tmp") }
 * }
 * ```
 */
open class KtEmbedExtension {
    /**
     * The package name for the generated ResourceDirectory class.
     */
    var packageName: String = ""


    /**
     * The directories to scan for resource files.
     */
    var resourceDirectories: List<String> = listOf()

    /**
     * A filter function that determines which paths to ignore.
     * The function should return true for paths to ignore, false otherwise.
     * Defaults to ignoring nothing.
     */
    var exclude: (String) -> Boolean = { false }
}
