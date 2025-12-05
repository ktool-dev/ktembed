plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.gradle.publish)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlin.reflect)

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

java {
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
}

kotlin {
    jvmToolchain(22)
}

gradlePlugin {
    website = project.property("scm.repo.path") as String
    vcsUrl = "https://${project.property("scm.repo.path")}.git"

    plugins {

    }
}

tasks.test {
    useJUnitPlatform()
}
