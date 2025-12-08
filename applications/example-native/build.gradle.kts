plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktml)
    //alias(libs.plugins.ktembed)
}

kotlin {
    applyDefaultHierarchyTemplate()

    linuxX64 {
        binaries {
            executable {
                entryPoint = "dev.ktool.embed.example.ktor.main"
            }
        }
    }

    macosX64 {
        binaries {
            executable {
                entryPoint = "dev.ktool.embed.example.ktor.main"
            }
        }
    }

    macosArm64 {
        binaries {
            executable {
                entryPoint = "dev.ktool.embed.example.ktor.main"
            }
        }
    }

    mingwX64 {
        binaries {
            executable {
                entryPoint = "dev.ktool.embed.example.ktor.main"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktml.runtime)
            implementation(libs.ktml.ktor)
            implementation(project(":runtime"))
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
        }
    }
}

//ktembed {
//    packageName = "dev.ktool.embed.example.ktor"
//    resourceDirectories = listOf("src/commonMain/resources/static")
//}
