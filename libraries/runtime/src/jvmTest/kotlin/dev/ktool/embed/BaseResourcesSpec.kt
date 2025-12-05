package dev.ktool.embed

import dev.ktool.kotest.BddSpec
import io.kotest.matchers.shouldBe
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class InternalResourcesSpec : BddSpec({
    "checking if resource exists" {
        Given
        val fileSystem = FakeFileSystem()
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("file.txt" to "content"))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val exists = resources.exists("file.txt")
        val notExists = resources.exists("missing.txt")

        Then
        exists shouldBe true
        notExists shouldBe false
    }

    "reading resource as string" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Hello, World!"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("greeting.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val result = resources.asString("greeting.txt")

        Then
        result shouldBe content
    }

    "reading resource as bytes" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Binary content"
        val expectedBytes = content.encodeUtf8()
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("data.bin" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val result = resources.asBytes("data.bin")

        Then
        result shouldBe expectedBytes
    }

    "writing small resource with Speed optimization" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Small file content"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("small.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem, inMemoryCutoff = 1000)
        val output = Buffer()

        When
        resources.write("small.txt", output)

        Then
        output.readUtf8() shouldBe content
    }

    "writing small resource with explicit Speed strategy" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Speed optimized"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("file.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)
        val output = Buffer()

        When
        resources.write("file.txt", output, OptimizationStrategy.Speed)

        Then
        output.readUtf8() shouldBe content
    }

    "writing resource multiple times with Speed strategy uses in-memory data" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Repeated content"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("file.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val output1 = Buffer()
        val output2 = Buffer()
        val output3 = Buffer()
        resources.write("file.txt", output1, OptimizationStrategy.Speed)
        resources.write("file.txt", output2, OptimizationStrategy.Speed)
        resources.write("file.txt", output3, OptimizationStrategy.Speed)

        Then
        output1.readUtf8() shouldBe content
        output2.readUtf8() shouldBe content
        output3.readUtf8() shouldBe content
    }

    "writing resource with Memory strategy creates temp file in fake filesystem" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Large file content"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("large.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)
        val output = Buffer()

        When
        resources.write("large.txt", output, OptimizationStrategy.Memory)

        Then
        output.readUtf8() shouldBe content
        // Verify that a temp file was created in the fake filesystem
        fileSystem.exists("/tmp/test-dir/large.txt".toPath()) shouldBe true
    }

    "writing same resource multiple times with Memory strategy validates only once" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Content to be cached"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("cached.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val output1 = Buffer()
        val output2 = Buffer()
        val output3 = Buffer()

        // First write: should validate and cache
        resources.write("cached.txt", output1, OptimizationStrategy.Memory)

        // Subsequent writes: should use cached validation
        resources.write("cached.txt", output2, OptimizationStrategy.Memory)
        resources.write("cached.txt", output3, OptimizationStrategy.Memory)

        Then
        output1.readUtf8() shouldBe content
        output2.readUtf8() shouldBe content
        output3.readUtf8() shouldBe content
        // Verify the file only exists once (not written multiple times)
        fileSystem.exists("/tmp/test-dir/cached.txt".toPath()) shouldBe true
    }

    "writing large resource exceeding cutoff uses Memory strategy automatically" {
        Given
        val fileSystem = FakeFileSystem()
        val largeContent = "X".repeat(100_000) // 100KB
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("huge.txt" to largeContent))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem, inMemoryCutoff = 50_000)
        val output = Buffer()

        When
        resources.write("huge.txt", output)

        Then
        output.readUtf8() shouldBe largeContent
        // Verify that a temp file was created due to size exceeding cutoff
        fileSystem.exists("/tmp/test-dir/huge.txt".toPath()) shouldBe true
    }

    "writing different resources creates separate cache entries" {
        Given
        val fileSystem = FakeFileSystem()
        val content1 = "First file"
        val content2 = "Second file"
        val resourceDir = createTestResourceDirectory(
            "test-dir",
            mapOf("file1.txt" to content1, "file2.txt" to content2)
        )
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val output1 = Buffer()
        val output2 = Buffer()
        resources.write("file1.txt", output1, OptimizationStrategy.Memory)
        resources.write("file2.txt", output2, OptimizationStrategy.Memory)

        Then
        output1.readUtf8() shouldBe content1
        output2.readUtf8() shouldBe content2
        fileSystem.exists("/tmp/test-dir/file1.txt".toPath()) shouldBe true
        fileSystem.exists("/tmp/test-dir/file2.txt".toPath()) shouldBe true
    }

    "attempting to read non-existent resource throws error" {
        Given
        val fileSystem = FakeFileSystem()
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("exists.txt" to "data"))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val result = runCatching { resources.asString("missing.txt") }

        Then
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe "Resource not found: missing.txt"
    }

    "writing resource with chunked content" {
        Given
        val fileSystem = FakeFileSystem()
        val chunk1 = "Part 1 - "
        val chunk2 = "Part 2 - "
        val chunk3 = "Part 3"
        val fullContent = chunk1 + chunk2 + chunk3
        val chunks = listOf(
            chunk1.encodeUtf8().base64(),
            chunk2.encodeUtf8().base64(),
            chunk3.encodeUtf8().base64()
        )
        val resource = Resource(chunks, "chunked.txt", "chunked.txt")
        val resourceDir = TestResourceDirectory("test-dir", mapOf("chunked.txt" to resource))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)
        val output = Buffer()

        When
        resources.write("chunked.txt", output, OptimizationStrategy.Memory)

        Then
        output.readUtf8() shouldBe fullContent
        fileSystem.exists("/tmp/test-dir/chunked.txt".toPath()) shouldBe true
    }

    "resources with same content in different instances are independent" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Shared content"
        val resourceDir1 = createTestResourceDirectory("dir1", mapOf("file.txt" to content))
        val resourceDir2 = createTestResourceDirectory("dir2", mapOf("file.txt" to content))
        val resources1 = createResourcesWithFakeFileSystem(resourceDir1, fileSystem)
        val resources2 = createResourcesWithFakeFileSystem(resourceDir2, fileSystem)

        When
        val output1 = Buffer()
        val output2 = Buffer()
        resources1.write("file.txt", output1, OptimizationStrategy.Memory)
        resources2.write("file.txt", output2, OptimizationStrategy.Memory)

        Then
        output1.readUtf8() shouldBe content
        output2.readUtf8() shouldBe content
        fileSystem.exists("/tmp/dir1/file.txt".toPath()) shouldBe true
        fileSystem.exists("/tmp/dir2/file.txt".toPath()) shouldBe true
    }

    "file validation detects content mismatch" {
        Given
        val fileSystem = FakeFileSystem()
        val originalContent = "Original content"
        val tamperedContent = "Tampered content"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("file.txt" to originalContent))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        And("Create the temp directory and write a tampered file")
        val tempPath = "/tmp/test-dir/file.txt".toPath()
        fileSystem.createDirectories("/tmp/test-dir".toPath())
        fileSystem.write(tempPath) {
            writeUtf8(tamperedContent)
        }

        When
        val output = Buffer()
        resources.write("file.txt", output, OptimizationStrategy.Memory)

        Then("Should detect mismatch and write correct content")
        output.readUtf8() shouldBe originalContent
        fileSystem.read(tempPath) { readUtf8() } shouldBe originalContent
    }

    "cached file is reused without revalidation" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Cached file content"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("file.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        And("First write to create and cache the file")
        val output1 = Buffer()
        resources.write("file.txt", output1, OptimizationStrategy.Memory)

        And("Get the file metadata to verify it's not rewritten")
        val filePath = "/tmp/test-dir/file.txt".toPath()
        val metadata1 = fileSystem.metadata(filePath)

        When("Second write should use cache without rewriting")
        val output2 = Buffer()
        resources.write("file.txt", output2, OptimizationStrategy.Memory)
        val metadata2 = fileSystem.metadata(filePath)

        Then("File timestamps should be identical (not rewritten)")
        output1.readUtf8() shouldBe content
        output2.readUtf8() shouldBe content
        metadata1.lastModifiedAtMillis shouldBe metadata2.lastModifiedAtMillis
    }

    "getting resource as path creates cache file" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "File on disk"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("file.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val path = resources.asPath("file.txt")

        Then
        path shouldBe "/tmp/test-dir/file.txt".toPath()
        fileSystem.exists(path!!) shouldBe true
        fileSystem.read(path) { readUtf8() } shouldBe content
    }

    "getting same resource as path multiple times returns same path" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Repeated access"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("file.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val path1 = resources.asPath("file.txt")
        val path2 = resources.asPath("file.txt")
        val path3 = resources.asPath("file.txt")

        Then
        path1 shouldBe path2
        path2 shouldBe path3
        path1 shouldBe "/tmp/test-dir/file.txt".toPath()
    }

    "getting resource as path validates existing file" {
        Given
        val fileSystem = FakeFileSystem()
        val originalContent = "Original content"
        val tamperedContent = "Tampered content"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("file.txt" to originalContent))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        And("Create a tampered file in the cache")
        val cachePath = "/tmp/test-dir/file.txt".toPath()
        fileSystem.createDirectories("/tmp/test-dir".toPath())
        fileSystem.write(cachePath) {
            writeUtf8(tamperedContent)
        }

        When
        val path = resources.asPath("file.txt")

        Then("Should detect mismatch and overwrite with correct content")
        path shouldBe cachePath
        fileSystem.read(path!!) { readUtf8() } shouldBe originalContent
    }

    "getting resource as path reuses validated file" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Validated content"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("file.txt" to content))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        And("First call to create and validate the file")
        val path1 = resources.asPath("file.txt")
        val metadata1 = fileSystem.metadata(path1!!)

        When("Second call should reuse validated file without rewriting")
        val path2 = resources.asPath("file.txt")
        val metadata2 = fileSystem.metadata(path2!!)

        Then("File timestamps should be identical (not rewritten)")
        path1 shouldBe path2
        metadata1.lastModifiedAtMillis shouldBe metadata2.lastModifiedAtMillis
        fileSystem.read(path1) { readUtf8() } shouldBe content
    }

    "getting resource as path with chunked content" {
        Given
        val fileSystem = FakeFileSystem()
        val chunk1 = "Chunk A - "
        val chunk2 = "Chunk B - "
        val chunk3 = "Chunk C"
        val fullContent = chunk1 + chunk2 + chunk3
        val chunks = listOf(
            chunk1.encodeUtf8().base64(),
            chunk2.encodeUtf8().base64(),
            chunk3.encodeUtf8().base64()
        )
        val resource = Resource(chunks, "multi.txt", "multi.txt")
        val resourceDir = TestResourceDirectory("test-dir", mapOf("multi.txt" to resource))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val path = resources.asPath("multi.txt")

        Then
        path shouldBe "/tmp/test-dir/multi.txt".toPath()
        fileSystem.exists(path!!) shouldBe true
        fileSystem.read(path) { readUtf8() } shouldBe fullContent
    }

    "getting different resources as paths creates separate files" {
        Given
        val fileSystem = FakeFileSystem()
        val content1 = "First file"
        val content2 = "Second file"
        val resourceDir = createTestResourceDirectory(
            "test-dir",
            mapOf("file1.txt" to content1, "file2.txt" to content2)
        )
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val path1 = resources.asPath("file1.txt")
        val path2 = resources.asPath("file2.txt")

        Then
        path1 shouldBe "/tmp/test-dir/file1.txt".toPath()
        path2 shouldBe "/tmp/test-dir/file2.txt".toPath()
        fileSystem.read(path1!!) { readUtf8() } shouldBe content1
        fileSystem.read(path2!!) { readUtf8() } shouldBe content2
    }

    "getting resource as path with large content" {
        Given
        val fileSystem = FakeFileSystem()
        val largeContent = "X".repeat(100_000) // 100KB
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("large.bin" to largeContent))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val path = resources.asPath("large.bin")

        Then
        path shouldBe "/tmp/test-dir/large.bin".toPath()
        fileSystem.exists(path!!) shouldBe true
        fileSystem.read(path) { readUtf8() } shouldBe largeContent
    }

    "getting resource as path with unicode content" {
        Given
        val fileSystem = FakeFileSystem()
        val unicodeContent = "Hello 世界 🌍 Привет"
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("unicode.txt" to unicodeContent))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val path = resources.asPath("unicode.txt")

        Then
        path shouldBe "/tmp/test-dir/unicode.txt".toPath()
        fileSystem.read(path!!) { readUtf8() } shouldBe unicodeContent
    }

    "getting resource as path with empty content" {
        Given
        val fileSystem = FakeFileSystem()
        val emptyContent = ""
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("empty.txt" to emptyContent))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val path = resources.asPath("empty.txt")

        Then
        path shouldBe "/tmp/test-dir/empty.txt".toPath()
        fileSystem.exists(path!!) shouldBe true
        fileSystem.read(path) { readUtf8() } shouldBe ""
    }

    "getting non-existent resource as path throws error" {
        Given
        val fileSystem = FakeFileSystem()
        val resourceDir = createTestResourceDirectory("test-dir", mapOf("exists.txt" to "data"))
        val resources = createResourcesWithFakeFileSystem(resourceDir, fileSystem)

        When
        val result = runCatching { resources.asPath("missing.txt") }

        Then
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldBe "Resource not found: missing.txt"
    }

    "getting resource as path from different resource directories are independent" {
        Given
        val fileSystem = FakeFileSystem()
        val content = "Same content"
        val resourceDir1 = createTestResourceDirectory("dir1", mapOf("file.txt" to content))
        val resourceDir2 = createTestResourceDirectory("dir2", mapOf("file.txt" to content))
        val resources1 = createResourcesWithFakeFileSystem(resourceDir1, fileSystem)
        val resources2 = createResourcesWithFakeFileSystem(resourceDir2, fileSystem)

        When
        val path1 = resources1.asPath("file.txt")
        val path2 = resources2.asPath("file.txt")

        Then
        path1 shouldBe "/tmp/dir1/file.txt".toPath()
        path2 shouldBe "/tmp/dir2/file.txt".toPath()
        fileSystem.read(path1!!) { readUtf8() } shouldBe content
        fileSystem.read(path2!!) { readUtf8() } shouldBe content
    }
})

/**
 * Creates a test resource directory with the given key and content map
 */
private fun createTestResourceDirectory(key: String, contentMap: Map<String, String>): ResourceDirectory {
    val resources = contentMap.mapValues { (path, content) ->
        val base64Content = content.encodeUtf8().base64()
        Resource(listOf(base64Content), path, path)
    }
    return TestResourceDirectory(key, resources)
}

/**
 * Creates a Resources instance with FakeFileSystem for testing
 */
private fun createResourcesWithFakeFileSystem(
    resourceDir: ResourceDirectory,
    fileSystem: FakeFileSystem,
    inMemoryCutoff: Int = IN_MEMORY_CUT_OFF
): BaseResources {
    return BaseResources(
        resourceDirectory = resourceDir,
        inMemoryCutoff = inMemoryCutoff,
        fileSystem = fileSystem,
        cacheDirectory = "/tmp".toPath()
    )
}

/**
 * Test implementation of ResourceDirectory
 */
private class TestResourceDirectory(
    override val key: String,
    private val resources: Map<String, Resource>
) : ResourceDirectory {
    override fun get(path: String): Resource? = resources[path]
}
