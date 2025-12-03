package dev.ktool.ktembed.gradle

import java.io.File

/**
 * Scans directories for resources to embed.
 */
internal class ResourceScanner(
    private val resourceDirs: List<String>,
    private val baseDir: File
) {
    
    /**
     * Scans all configured resource directories and returns a list of resources.
     */
    fun scan(): List<ResourceFile> {
        val resources = mutableListOf<ResourceFile>()
        
        for (dirPath in resourceDirs) {
            val dir = baseDir.resolve(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                println("Skipping non-existent resource directory: $dir")
                continue
            }
            
            scanDirectory(dir, dir, resources)
        }
        
        return resources
    }
    
    private fun scanDirectory(rootDir: File, currentDir: File, resources: MutableList<ResourceFile>) {
        currentDir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> scanDirectory(rootDir, file, resources)
                file.isFile -> {
                    val relativePath = file.relativeTo(rootDir).path
                    resources.add(ResourceFile(file, relativePath))
                }
            }
        }
    }
}

/**
 * Represents a resource file to be embedded.
 */
data class ResourceFile(
    val file: File,
    val path: String
) {
    val bytes: ByteArray by lazy { file.readBytes() }
    val size: Long get() = file.length()
}
