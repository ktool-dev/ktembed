package dev.ktool.embed.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Gradle plugin for KtEmbed resource generation.
 *
 * This plugin:
 * 1. Creates a 'ktembed' extension for configuration
 * 2. Registers a 'generateKtEmbedResources' task
 * 3. Adds the generated sources to the main source set
 * 4. Makes compileKotlin depend on the generation task
 *
 * Example usage:
 * ```kotlin
 * plugins {
 *     id("dev.ktool.ktembed")
 * }
 *
 * ktembed {
 *     packageName = "com.example.resources"
 *     resourceDirectories = files("src/main/resources")
 *     filter = { path -> path.endsWith(".tmp") }
 * }
 * ```
 */
class KtEmbedPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("ktembed", KtEmbedExtension::class.java)

        val generateTask = project.tasks.register("generateKtEmbedResources", KtEmbedTask::class.java) { task ->
            task.packageName.set(extension.packageName)
            task.resourceDirectories.setFrom(extension.resourceDirectories)
            task.filter.set(extension.filter)
            task.outputDirectory.set(project.layout.buildDirectory.dir("ktembed"))
        }

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.extensions
                .getByType(KotlinJvmProjectExtension::class.java)
                .sourceSets
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                .kotlin.srcDir(generateTask.map { it.outputDirectory })
        }

        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            project.afterEvaluate {
                project.extensions.getByType(KotlinMultiplatformExtension::class.java)
                    .sourceSets
                    .getByName("commonMain")
                    .kotlin.srcDir(generateTask.map { it.outputDirectory })
            }
        }

        project.tasks.configureEach { task ->
            if (task.name.startsWith("compile") && task.name.contains("Kotlin")) {
                task.dependsOn(generateTask)
            }
        }
    }
}
