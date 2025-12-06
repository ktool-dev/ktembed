package dev.ktool.embed

import dev.ktool.gen.types.*
import okio.FileSystem
import okio.SYSTEM

data class ResourceMapping(val path: String, val chunkVariableName: String, val key: String)

class ResourceDirectoryGenerator(private val fileSystem: FileSystem = FileSystem.SYSTEM) {
    /**
     * Generates the ResourceDirectory.kt file.
     *
     * @param packageName The package name for the generated class
     * @param mappings List of resource mappings to include
     * @return A KotlinFile with the ResourceDirectory implementation
     */
    fun generate(packageName: String, mappings: List<ResourceMapping>) = KotlinFile(packageName) {
        +Import("dev.ktool.embed.Resource")
        +Import("dev.ktool.embed.ResourceDirectory")

        +Object("ResourceDirectory") {
            +SuperType("ResourceDirectory")

            +Property("key", type = StringType, modifiers = listOf(Modifier.Override)) {
                initializer = ExpressionBody("\"${packageName.replace('.', '-')}\"")
            }

            +Property(
                "resources",
                type = Type("Map", typeArguments = listOf(TypeArgument("String"), TypeArgument("Resource"))),
                modifiers = listOf(Modifier.Private)
            ) {
                initializer = ExpressionBody {
                    write("mapOf(")
                    withIndent {
                        mappings.forEach {
                            newLine(""""${it.path}" to Resource("${it.key}", ${it.chunkVariableName}),""")
                            newLine()
                        }
                    }
                    trimEnd()
                    newLine(")")
                }
            }

            +Function("get", returnType = Type("Resource?")) {
                +Modifier.Override
                +Modifier.Operator
                +Parameter("path", StringType)
                body = ExpressionBody("resources[path]")
            }
        }
    }
}