package dev.ktool.embed

import dev.ktool.kotest.BddSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import okio.ByteString.Companion.decodeBase64
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class AssetProcessorSpec : BddSpec({
    lateinit var fileSystem: FakeFileSystem
    val sourceDir = "/assets".toPath()
    val outputDir = "/output".toPath()

    beforeEach {
        fileSystem = FakeFileSystem()
    }

    "processing single directory with single file" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "com.example.resources"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("test.txt")) {
            writeUtf8("Hello, World!")
        }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("com/example/resources/ResourceDirectory.kt")
        fileSystem.exists(resourceDirFile) shouldBe true

        val resourceChunksFile = outputDir.resolve("com/example/resources/ResourceChunks1.kt")
        fileSystem.exists(resourceChunksFile) shouldBe true
    }

    "processing directory with multiple files" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.app"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("file1.txt")) { writeUtf8("Content 1") }
        fileSystem.write(sourceDir.resolve("file2.txt")) { writeUtf8("Content 2") }
        fileSystem.write(sourceDir.resolve("file3.txt")) { writeUtf8("Content 3") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/app/ResourceDirectory.kt")
        fileSystem.exists(resourceDirFile) shouldBe true

        val content = fileSystem.read(resourceDirFile) { readUtf8() }
        content shouldContain """"file1.txt""""
        content shouldContain """"file2.txt""""
        content shouldContain """"file3.txt""""
    }

    "processing directory with nested subdirectories" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.nested"

        fileSystem.createDirectories(sourceDir.resolve("subdir1/subdir2"))
        fileSystem.write(sourceDir.resolve("root.txt")) { writeUtf8("Root file") }
        fileSystem.write(sourceDir.resolve("subdir1/nested.txt")) { writeUtf8("Nested file") }
        fileSystem.write(sourceDir.resolve("subdir1/subdir2/deep.txt")) { writeUtf8("Deep file") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/nested/ResourceDirectory.kt")
        val content = fileSystem.read(resourceDirFile) { readUtf8() }

        content shouldContain """"root.txt""""
        content shouldContain """"subdir1/nested.txt""""
        content shouldContain """"subdir1/subdir2/deep.txt""""
    }

    "processing multiple directories" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val sourceDir1 = "/assets1".toPath()
        val sourceDir2 = "/assets2".toPath()
        val packageName = "test.multi"

        fileSystem.createDirectories(sourceDir1)
        fileSystem.createDirectories(sourceDir2)
        fileSystem.write(sourceDir1.resolve("file1.txt")) { writeUtf8("From dir 1") }
        fileSystem.write(sourceDir2.resolve("file2.txt")) { writeUtf8("From dir 2") }

        When
        assetProcessor.process(listOf(sourceDir1, sourceDir2), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/multi/ResourceDirectory.kt")
        val content = fileSystem.read(resourceDirFile) { readUtf8() }

        content shouldContain """"file1.txt""""
        content shouldContain """"file2.txt""""
    }

    "ignoring files based on filter" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.filter"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("include.txt")) { writeUtf8("Include this") }
        fileSystem.write(sourceDir.resolve(".hidden")) { writeUtf8("Hidden file") }
        fileSystem.write(sourceDir.resolve("ignore.bak")) { writeUtf8("Backup file") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { path ->
            path.name.startsWith(".") || path.name.endsWith(".bak")
        }

        Then
        val resourceDirFile = outputDir.resolve("test/filter/ResourceDirectory.kt")
        val content = fileSystem.read(resourceDirFile) { readUtf8() }

        content shouldContain """"include.txt""""
        content shouldNotContain """".hidden""""
        content shouldNotContain """"ignore.bak""""
    }

    "processing empty directory" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.empty"

        fileSystem.createDirectories(sourceDir)

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/empty/ResourceDirectory.kt")
        fileSystem.exists(resourceDirFile) shouldBe true

        val content = fileSystem.read(resourceDirFile) { readUtf8() }
        content shouldContain "mapOf("
    }

    "processing non-existent directory" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val sourceDir = "/nonexistent".toPath()
        val packageName = "test.missing"

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/missing/ResourceDirectory.kt")
        fileSystem.exists(resourceDirFile) shouldBe true

        val content = fileSystem.read(resourceDirFile) { readUtf8() }
        content shouldContain "mapOf("
    }

    "generated ResourceDirectory file has correct package" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "my.custom.package"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("test.txt")) { writeUtf8("test") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("my/custom/package/ResourceDirectory.kt")
        val content = fileSystem.read(resourceDirFile) { readUtf8() }

        content shouldContain "package my.custom.package"
    }

    "generated ResourceChunks file has correct package" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "my.custom.package"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("test.txt")) { writeUtf8("test") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("my/custom/package/ResourceChunks1.kt")
        val content = fileSystem.read(chunksFile) { readUtf8() }

        content shouldContain "package my.custom.package"
    }

    "resource chunks are Base64 encoded" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.base64"
        val fileContent = "Hello, World!"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("test.txt")) { writeUtf8(fileContent) }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/base64/ResourceChunks1.kt")
        val content = fileSystem.read(chunksFile) { readUtf8() }

        content shouldContain "RESOURCE_1"
        content shouldContain "listOf("
        // Base64 encoded content should be present
        content shouldContain "\""
    }

    "processing large file creates chunked content" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.large"
        val largeContent = "X".repeat(100_000) // 100KB

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("large.bin")) { writeUtf8(largeContent) }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/large/ResourceChunks1.kt")
        val content = fileSystem.read(chunksFile) { readUtf8() }

        content shouldContain "RESOURCE_1"
        content shouldContain "listOf("
        // Should have multiple chunks in the list
        content shouldContain ","
    }

    "file keys use underscores instead of dots and slashes" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.keys"

        fileSystem.createDirectories(sourceDir.resolve("images"))
        fileSystem.write(sourceDir.resolve("images/logo.png")) { writeUtf8("PNG data") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/keys/ResourceDirectory.kt")
        val content = fileSystem.read(resourceDirFile) { readUtf8() }

        content shouldContain """"images/logo.png""""
        content shouldContain """"images_logo_png""""
    }

    "processing file with unicode content" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.unicode"
        val unicodeContent = "Hello 世界 🌍"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("unicode.txt")) { writeUtf8(unicodeContent) }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/unicode/ResourceChunks1.kt")
        fileSystem.exists(chunksFile) shouldBe true

        val content = fileSystem.read(chunksFile) { readUtf8() }
        content shouldContain "RESOURCE_1"
    }

    "processing binary file content" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.binary"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("binary.dat")) {
            write(byteArrayOf(0x00, 0x01, 0x02, 0xFF.toByte()))
        }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/binary/ResourceChunks1.kt")
        fileSystem.exists(chunksFile) shouldBe true
    }

    "processing file with empty content" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.empty"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("empty.txt")) { writeUtf8("") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/empty/ResourceDirectory.kt")
        fileSystem.exists(resourceDirFile) shouldBe true

        val content = fileSystem.read(resourceDirFile) { readUtf8() }
        content shouldContain """"empty.txt""""
    }

    "output directory structure is created automatically" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val outputDir = "/output/deep/nested/path".toPath()
        val packageName = "test.app"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("test.txt")) { writeUtf8("test") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/app/ResourceDirectory.kt")
        fileSystem.exists(resourceDirFile) shouldBe true
    }

    "resource variable names are numbered sequentially" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.vars"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("a.txt")) { writeUtf8("A") }
        fileSystem.write(sourceDir.resolve("b.txt")) { writeUtf8("B") }
        fileSystem.write(sourceDir.resolve("c.txt")) { writeUtf8("C") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/vars/ResourceChunks1.kt")
        val content = fileSystem.read(chunksFile) { readUtf8() }

        content shouldContain "RESOURCE_1"
        content shouldContain "RESOURCE_2"
        content shouldContain "RESOURCE_3"
    }

    "ResourceDirectory references resource variables" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.refs"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("test.txt")) { writeUtf8("test content") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/refs/ResourceDirectory.kt")
        val content = fileSystem.read(resourceDirFile) { readUtf8() }

        content shouldContain "RESOURCE_1"
        content shouldContain """"test.txt""""
    }

    "processing preserves relative paths from base directory" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val sourceDir = "/project/assets".toPath()
        val packageName = "test.paths"

        fileSystem.createDirectories(sourceDir.resolve("css"))
        fileSystem.write(sourceDir.resolve("index.html")) { writeUtf8("HTML") }
        fileSystem.write(sourceDir.resolve("css/style.css")) { writeUtf8("CSS") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/paths/ResourceDirectory.kt")
        val content = fileSystem.read(resourceDirFile) { readUtf8() }

        content shouldContain """"index.html""""
        content shouldContain """"css/style.css""""
    }

    "ignoring hidden directories" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.hidden"

        fileSystem.createDirectories(sourceDir.resolve(".git"))
        fileSystem.write(sourceDir.resolve("visible.txt")) { writeUtf8("visible") }
        fileSystem.write(sourceDir.resolve(".git/config")) { writeUtf8("git config") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { path ->
            path.name.startsWith(".")
        }

        Then
        val resourceDirFile = outputDir.resolve("test/hidden/ResourceDirectory.kt")
        val content = fileSystem.read(resourceDirFile) { readUtf8() }

        content shouldContain """"visible.txt""""
        content shouldNotContain """".git""""
        content shouldNotContain """"config""""
    }

    "generated files use val properties" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.val"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("test.txt")) { writeUtf8("test") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/val/ResourceChunks1.kt")
        val content = fileSystem.read(chunksFile) { readUtf8() }

        content shouldContain "val RESOURCE_1"
    }

    "processing file with special characters in name" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.special"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("my-file.txt")) { writeUtf8("content") }
        fileSystem.write(sourceDir.resolve("file_2.txt")) { writeUtf8("content") }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val resourceDirFile = outputDir.resolve("test/special/ResourceDirectory.kt")
        val content = fileSystem.read(resourceDirFile) { readUtf8() }

        content shouldContain """"my-file.txt""""
        content shouldContain """"file_2.txt""""
    }

    "decoded Base64 chunks match original file content" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.decode"
        val originalContent = "The quick brown fox jumps over the lazy dog. 🦊"

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("test.txt")) { writeUtf8(originalContent) }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/decode/ResourceChunks1.kt")
        val generatedCode = fileSystem.read(chunksFile) { readUtf8() }

        val base64Chunks = extractBase64Chunks(generatedCode)
        val decodedContent = decodeChunks(base64Chunks)
        decodedContent shouldBe originalContent
    }

    "decoded Base64 chunks match original file content with multiple chunks" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.multichunk"
        val largeContent = buildString {
            repeat(100) { i ->
                append("Line $i: The quick brown fox jumps over the lazy dog.\n")
            }
        }

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("large.txt")) { writeUtf8(largeContent) }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/multichunk/ResourceChunks1.kt")
        val generatedCode = fileSystem.read(chunksFile) { readUtf8() }

        val base64Chunks = extractBase64Chunks(generatedCode)
        val decodedContent = decodeChunks(base64Chunks)
        decodedContent shouldBe largeContent
    }

    "decoded Base64 chunks match binary file content" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.binary.decode"
        val binaryData = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, // PNG header
            0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x01, 0x02, 0x03,
            0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte()
        )

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("binary.dat")) { write(binaryData) }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/binary/decode/ResourceChunks1.kt")
        val generatedCode = fileSystem.read(chunksFile) { readUtf8() }

        val base64Chunks = extractBase64Chunks(generatedCode)
        val decodedBytes = decodeChunksToBytes(base64Chunks)

        decodedBytes shouldBe binaryData
    }

    "decoded Base64 chunks match unicode file content" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.unicode.decode"
        val unicodeContent = """
            |English: Hello World
            |中文: 你好世界
            |日本語: こんにちは世界
            |한국어: 안녕하세요 세계
            |العربية: مرحبا بالعالم
            |Emoji: 🌍🌎🌏 🦊🐶🐱
        """.trimMargin()

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("unicode.txt")) { writeUtf8(unicodeContent) }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/unicode/decode/ResourceChunks1.kt")
        val generatedCode = fileSystem.read(chunksFile) { readUtf8() }

        val base64Chunks = extractBase64Chunks(generatedCode)
        val decodedContent = decodeChunks(base64Chunks)
        decodedContent shouldBe unicodeContent
    }

    "decoded Base64 chunks match very large file content" {
        Given
        val assetProcessor = AssetProcessor(fileSystem)
        val packageName = "test.verylarge"
        val largeContent = "X".repeat(150_000)

        fileSystem.createDirectories(sourceDir)
        fileSystem.write(sourceDir.resolve("verylarge.bin")) { writeUtf8(largeContent) }

        When
        assetProcessor.process(listOf(sourceDir), packageName, outputDir) { false }

        Then
        val chunksFile = outputDir.resolve("test/verylarge/ResourceChunks1.kt")
        val generatedCode = fileSystem.read(chunksFile) { readUtf8() }

        val base64Chunks = extractBase64Chunks(generatedCode)
        val decodedContent = decodeChunks(base64Chunks)

        decodedContent shouldBe largeContent
        decodedContent.length shouldBe 150_000
    }

    "integration test with real filesystem" {
        Given
        val outputDir = "build/temp-gen-test".toPath()
        outputDir.toFile().deleteRecursively()

        val assetProcessor = AssetProcessor()
        val packageName = "dev.ktool.embed.test"
        val dirs =
            listOf("../runtime/src/commonMain".toPath())

        When
        assetProcessor.process(dirs, packageName, outputDir) { it.name.startsWith(".") }

        Then
        outputDir.resolve("dev/ktool/embed/test/ResourceDirectory.kt").toFile().exists() shouldBe true
        outputDir.resolve("dev/ktool/embed/test/ResourceChunks1.kt").toFile().exists() shouldBe true
    }
})

private fun extractBase64Chunks(generatedCode: String): List<String> {
    val chunks = mutableListOf<String>()
    val regex = """"([A-Za-z0-9+/=]+)",""".toRegex()

    regex.findAll(generatedCode).forEach { matchResult ->
        chunks.add(matchResult.groupValues[1])
    }

    return chunks
}

private fun decodeChunks(base64Chunks: List<String>): String {
    return base64Chunks.joinToString("") { it.decodeBase64()!!.uncompress().utf8() }
}

private fun decodeChunksToBytes(base64Chunks: List<String>): ByteArray {
    return base64Chunks
        .map { it.decodeBase64()!!.uncompress().toByteArray() }
        .reduce { acc, bytes -> acc + bytes }
}
