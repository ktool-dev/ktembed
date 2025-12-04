package dev.ktool.embed

import dev.ktool.kotest.BddSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class UtilSpec : BddSpec({
    "computing hash from empty base64 chunks" {
        Given
        val emptyChunks = emptyList<String>()

        When
        val hash = computeHash(emptyChunks)

        Then
        hash shouldBe "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }

    "computing hash from single base64 chunk" {
        Given
        val content = "Hello, World!"
        val base64Chunk = content.encodeUtf8().base64()

        When
        val hash = computeHash(listOf(base64Chunk))

        Then
        hash shouldHaveLength 64
        hash shouldMatch Regex("[0-9a-f]{64}")
        hash shouldBe "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
    }

    "computing hash from multiple base64 chunks" {
        Given
        val chunk1 = "Hello".encodeUtf8().base64()
        val chunk2 = ", ".encodeUtf8().base64()
        val chunk3 = "World!".encodeUtf8().base64()

        When
        val hash = computeHash(listOf(chunk1, chunk2, chunk3))

        Then
        hash shouldHaveLength 64
        hash shouldMatch Regex("[0-9a-f]{64}")
        hash shouldBe "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
    }

    "hashing same content produces same hash" {
        Given
        val content = "Test content"
        val base64 = content.encodeUtf8().base64()

        When
        val hash1 = computeHash(listOf(base64))
        val hash2 = computeHash(listOf(base64))

        Then
        hash1 shouldBe hash2
    }

    "hashing different content produces different hash" {
        Given
        val content1 = "First content"
        val content2 = "Second content"

        When
        val hash1 = computeHash(listOf(content1.encodeUtf8().base64()))
        val hash2 = computeHash(listOf(content2.encodeUtf8().base64()))

        Then
        hash1 shouldNotBe hash2
    }

    "handling invalid base64 chunks gracefully" {
        Given
        val validChunk = "Hello".encodeUtf8().base64()
        val invalidChunk = "not-valid-base64!!!"

        When
        val hash = computeHash(listOf(validChunk, invalidChunk))

        Then
        hash shouldHaveLength 64
        hash shouldMatch Regex("[0-9a-f]{64}")
        hash shouldBe "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969"
    }

    "computing hash from file content" {
        Given
        val fileSystem = FakeFileSystem()
        val filePath = "/test/file.txt".toPath()
        val content = "File content for testing"
        fileSystem.createDirectories("/test".toPath())
        fileSystem.write(filePath) {
            writeUtf8(content)
        }

        When
        val hash = computeHash(filePath, fileSystem)

        Then
        hash shouldHaveLength 64
        hash shouldMatch Regex("[0-9a-f]{64}")
    }

    "hashing same file content produces same hash" {
        Given
        val fileSystem = FakeFileSystem()
        val filePath1 = "/test/file1.txt".toPath()
        val filePath2 = "/test/file2.txt".toPath()
        val content = "Identical content"
        fileSystem.createDirectories("/test".toPath())
        fileSystem.write(filePath1) { writeUtf8(content) }
        fileSystem.write(filePath2) { writeUtf8(content) }

        When
        val hash1 = computeHash(filePath1, fileSystem)
        val hash2 = computeHash(filePath2, fileSystem)

        Then
        hash1 shouldBe hash2
    }

    "hashing different file content produces different hash" {
        Given
        val fileSystem = FakeFileSystem()
        val filePath1 = "/test/file1.txt".toPath()
        val filePath2 = "/test/file2.txt".toPath()
        fileSystem.createDirectories("/test".toPath())
        fileSystem.write(filePath1) { writeUtf8("Content A") }
        fileSystem.write(filePath2) { writeUtf8("Content B") }

        When
        val hash1 = computeHash(filePath1, fileSystem)
        val hash2 = computeHash(filePath2, fileSystem)

        Then
        hash1 shouldNotBe hash2
    }

    "hashing large file content efficiently" {
        Given
        val fileSystem = FakeFileSystem()
        val filePath = "/test/largefile.txt".toPath()
        val largeContent = "A".repeat(100_000) // 100KB of data
        fileSystem.createDirectories("/test".toPath())
        fileSystem.write(filePath) {
            writeUtf8(largeContent)
        }

        When
        val hash = computeHash(filePath, fileSystem)

        Then
        hash shouldHaveLength 64
        hash shouldMatch Regex("[0-9a-f]{64}")
    }

    "hashing empty file produces consistent hash" {
        Given
        val fileSystem = FakeFileSystem()
        val filePath = "/test/empty.txt".toPath()
        fileSystem.createDirectories("/test".toPath())
        fileSystem.write(filePath) {
            // Write nothing - empty file
        }

        When
        val hash = computeHash(filePath, fileSystem)

        Then
        hash shouldBe "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }

    "base64 chunks and file produce same hash for same content"(
        row("Hello, World!"),
        row("Test data"),
        row("12345"),
        row("Special chars: @#$%^&*()")
    ) { (content) ->
        Given("content: $content")
        val fileSystem = FakeFileSystem()
        val filePath = "/test/file.txt".toPath()
        val base64Chunk = content.encodeUtf8().base64()
        fileSystem.createDirectories("/test".toPath())
        fileSystem.write(filePath) { writeUtf8(content) }

        When
        val hashFromBase64 = computeHash(listOf(base64Chunk))
        val hashFromFile = computeHash(filePath, fileSystem)

        Then
        hashFromBase64 shouldBe hashFromFile
    }

    "hash format is always 64 hex characters"(
        row(listOf("")),
        row(listOf("a")),
        row(listOf("short")),
        row(listOf("A much longer string with lots of content")),
        row(listOf("chunk1", "chunk2", "chunk3"))
    ) { (contents) ->
        Given("base64 chunks from: $contents")
        val base64Chunks = contents.map { it.encodeUtf8().base64() }

        When
        val hash = computeHash(base64Chunks)

        Then
        hash shouldHaveLength 64
        hash shouldMatch Regex("[0-9a-f]{64}")
    }
})
