package dev.ktool.embed

import dev.ktool.kotest.BddSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ResourceDirectoryGeneratorSpec : BddSpec({
    "generating ResourceDirectory with empty mappings" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "com.example.test"
        val mappings = emptyList<ResourceMapping>()

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain "package com.example.test"
        output shouldContain "import dev.ktool.embed.Resource"
        output shouldContain "import dev.ktool.embed.ResourceDirectory"
        output shouldContain "class ResourceDirectory : ResourceDirectory"
        output shouldContain """override val key: String = "com-example-test""""
        output shouldContain "private val resources: Map<String, Resource> = mapOf("
        output shouldContain ")"
    }

    "generating ResourceDirectory with single mapping" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "dev.ktool.embed.test"
        val mapping = ResourceMapping(
            path = "file.txt",
            chunkVariableName = "chunk_0",
            key = "file_txt"
        )
        val mappings = listOf(mapping)

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain "package dev.ktool.embed.test"
        output shouldContain """override val key: String = "dev-ktool-embed-test""""
        output shouldContain """"file.txt" to Resource("file_txt", chunk_0)"""
        output shouldContain "override operator fun get(path: String): Resource? = resources[path]"
    }

    "generating ResourceDirectory with multiple mappings" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "my.app.resources"
        val mappings = listOf(
            ResourceMapping(
                path = "index.html",
                chunkVariableName = "chunk_0",
                key = "index_html"
            ),
            ResourceMapping(
                path = "styles/main.css",
                chunkVariableName = "chunk_1",
                key = "styles_main_css"
            ),
            ResourceMapping(
                path = "scripts/app.js",
                chunkVariableName = "chunk_2",
                key = "scripts_app_js"
            )
        )

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain """"index.html" to Resource("index_html", chunk_0)"""
        output shouldContain """"styles/main.css" to Resource("styles_main_css", chunk_1)"""
        output shouldContain """"scripts/app.js" to Resource("scripts_app_js", chunk_2)"""
    }

    "package name is converted to key correctly" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "org.example.my.app"
        val mappings = listOf(
            ResourceMapping("test.txt", "chunk_0", "test_txt")
        )

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain """override val key: String = "org-example-my-app""""
    }

    "generated class extends ResourceDirectory" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test.pkg"
        val mappings = emptyList<ResourceMapping>()

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain "class ResourceDirectory : ResourceDirectory"
    }

    "generated class has private resources property" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test.app"
        val mappings = emptyList<ResourceMapping>()

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain "private val resources: Map<String, Resource>"
    }

    "generated class has override operator get function" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test.app"
        val mappings = emptyList<ResourceMapping>()

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain "override operator fun get(path: String): Resource? = resources[path]"
    }

    "resources map entries are properly formatted with commas" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test"
        val mappings = listOf(
            ResourceMapping("a.txt", "chunk_0", "a"),
            ResourceMapping("b.txt", "chunk_1", "b")
        )

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain """"a.txt" to Resource("a", chunk_0),"""
        output shouldContain """"b.txt" to Resource("b", chunk_1),"""
    }

    "mapping with special characters in path" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test"
        val mappings = listOf(
            ResourceMapping(
                path = "assets/images/logo-2024.png",
                chunkVariableName = "chunk_42",
                key = "assets_images_logo_2024_png"
            )
        )

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain """"assets/images/logo-2024.png" to Resource("assets_images_logo_2024_png", chunk_42)"""
    }

    "package name with single segment" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "app"
        val mappings = emptyList<ResourceMapping>()

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain "package app"
        output shouldContain """override val key: String = "app""""
    }

    "generated code has required imports" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test"
        val mappings = listOf(
            ResourceMapping("file.txt", "chunk_0", "key")
        )

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain "import dev.ktool.embed.Resource"
        output shouldContain "import dev.ktool.embed.ResourceDirectory"
    }

    "generated code does not have extra imports" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test"
        val mappings = emptyList<ResourceMapping>()

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldNotContain "import java."
        output shouldNotContain "import kotlin."
    }

    "chunk variable names are used correctly without quotes" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test"
        val mappings = listOf(
            ResourceMapping("file.txt", "resourceChunks1_0", "key")
        )

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain """Resource("key", resourceChunks1_0)"""
        output shouldNotContain """Resource("key", "resourceChunks1_0")"""
    }

    "large number of mappings" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test"
        val mappings = (0 until 100).map { i ->
            ResourceMapping(
                path = "file$i.txt",
                chunkVariableName = "chunk_$i",
                key = "file${i}_txt"
            )
        }

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain """"file0.txt" to Resource("file0_txt", chunk_0)"""
        output shouldContain """"file50.txt" to Resource("file50_txt", chunk_50)"""
        output shouldContain """"file99.txt" to Resource("file99_txt", chunk_99)"""
    }

    "mapping with unicode characters in path" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test"
        val mappings = listOf(
            ResourceMapping(
                path = "文件/测试.txt",
                chunkVariableName = "chunk_0",
                key = "unicode_key"
            )
        )

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain """"文件/测试.txt" to Resource("unicode_key", chunk_0)"""
    }

    "resources map is initialized inline" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test"
        val mappings = listOf(
            ResourceMapping("a.txt", "chunk_0", "a")
        )

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain "private val resources: Map<String, Resource> = mapOf("
    }

    "package name with underscores gets converted correctly" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "my_package.sub_package"
        val mappings = emptyList<ResourceMapping>()

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain """override val key: String = "my_package-sub_package""""
    }

    "mapping resource keys preserve original format" {
        Given
        val generator = ResourceDirectoryGenerator()
        val packageName = "test"
        val mappings = listOf(
            ResourceMapping(
                path = "file.txt",
                chunkVariableName = "chunk_0",
                key = "MyCustomKey_123"
            )
        )

        When
        val kotlinFile = generator.generate(packageName, mappings)

        Then
        val output = kotlinFile.render()
        output shouldContain """Resource("MyCustomKey_123", chunk_0)"""
    }
})
