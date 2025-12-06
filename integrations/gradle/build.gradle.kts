plugins {
    kotlin("jvm")
    signing
    alias(libs.plugins.gradle.publish)
}

kotlin {
    jvmToolchain(22)

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group = project.property("group").toString()
version = project.property("version").toString()

dependencies {
    implementation(project(":generator"))
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin"))
    implementation(libs.okio)
}

signing {
    useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
    setRequired {
        // Only require signing if not publishing to Maven Local
        gradle.taskGraph.allTasks.none { it.name.contains("ToMavenLocal") }
    }
    sign(publishing.publications)
}

gradlePlugin {
    val repoPath = project.property("scm.repo.path") as String
    val pluginPath = "https://$repoPath"
    website = "$pluginPath/blob/main/integrations/gradle"
    vcsUrl = pluginPath

    plugins {
        create("ktembedPlugin") {
            id = "${project.group}.${rootProject.name}"
            displayName = "KtEmbed Gradle Plugin"
            description = "Runs the code generation process for KtEmbed"
            tags = listOf("kotlin", "native", "resources")
            implementationClass = "dev.ktool.embed.gradle.KtEmbedPlugin"
        }
    }
}
