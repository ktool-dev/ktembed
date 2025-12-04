package dev.ktool.embed.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for embedding static resources into Kotlin applications.
 */
class KtEmbedPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create(
            "ktEmbed",
            KtEmbedExtension::class.java,
            project
        )
        
        // Register the generate task
        val generateTask = project.tasks.register(
            "generateEmbeddedResources",
            GenerateEmbeddedResourcesTask::class.java
        ) { task ->
            task.group = "embed"
            task.description = "Generate Kotlin code for embedded resources"
            task.extension.set(extension)
        }
        
        // Hook into Kotlin compilation
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            configureKotlinMultiplatform(project, extension, generateTask)
        }
        
        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            configureKotlinJvm(project, extension, generateTask)
        }
    }
    
    private fun configureKotlinMultiplatform(
        project: Project,
        extension: KtEmbedExtension,
        generateTask: org.gradle.api.tasks.TaskProvider<GenerateEmbeddedResourcesTask>
    ) {
        project.afterEvaluate {
            val outputDir = extension.outputDir.get().asFile
            
            // Use reflection to avoid compile-time dependency on Kotlin plugin
            try {
                val kotlinExt = project.extensions.findByName("kotlin")
                if (kotlinExt != null) {
                    // Get sourceSets through reflection
                    val sourceSetsMethod = kotlinExt.javaClass.getMethod("getSourceSets")
                    val sourceSets = sourceSetsMethod.invoke(kotlinExt)
                    
                    // Add output dir to commonMain
                    val namedMethod = sourceSets.javaClass.getMethod("named", String::class.java, org.gradle.api.Action::class.java)
                    namedMethod.invoke(sourceSets, "commonMain", org.gradle.api.Action<Any> { sourceSet ->
                        val kotlinProperty = sourceSet.javaClass.getMethod("getKotlin")
                        val kotlinSourceSet = kotlinProperty.invoke(sourceSet)
                        val srcDirMethod = kotlinSourceSet.javaClass.getMethod("srcDir", Any::class.java)
                        srcDirMethod.invoke(kotlinSourceSet, outputDir)
                    })
                }
                
                // Make all Kotlin compile tasks depend on generation
                project.tasks.matching { it.name.contains("compileKotlin") }.configureEach {
                    it.dependsOn(generateTask)
                }
            } catch (e: Exception) {
                project.logger.warn("Could not configure KtEmbed for Kotlin Multiplatform: ${e.message}")
            }
        }
    }
    
    private fun configureKotlinJvm(
        project: Project,
        extension: KtEmbedExtension,
        generateTask: org.gradle.api.tasks.TaskProvider<GenerateEmbeddedResourcesTask>
    ) {
        project.afterEvaluate {
            val outputDir = extension.outputDir.get().asFile
            
            // Use reflection to avoid compile-time dependency
            try {
                val sourceSets = project.extensions.findByName("sourceSets")
                if (sourceSets != null) {
                    val namedMethod = sourceSets.javaClass.getMethod("named", String::class.java, org.gradle.api.Action::class.java)
                    namedMethod.invoke(sourceSets, "main", org.gradle.api.Action<Any> { sourceSet ->
                        val javaProperty = sourceSet.javaClass.getMethod("getJava")
                        val javaSourceSet = javaProperty.invoke(sourceSet)
                        val srcDirMethod = javaSourceSet.javaClass.getMethod("srcDir", Any::class.java)
                        srcDirMethod.invoke(javaSourceSet, outputDir)
                    })
                }
                
                // Make compileKotlin depend on generate task
                project.tasks.findByName("compileKotlin")?.dependsOn(generateTask)
            } catch (e: Exception) {
                project.logger.warn("Could not configure KtEmbed for Kotlin JVM: ${e.message}")
            }
        }
    }
}
