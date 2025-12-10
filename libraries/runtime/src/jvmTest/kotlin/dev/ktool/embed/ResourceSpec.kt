package dev.ktool.embed

import dev.ktool.kotest.BddSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

class ResourceSpec : BddSpec({
    "creating resource with single chunk" {
        Given
        val content = "Hello, World!"
        val base64Chunk = content.encodeUtf8().base64()

        When
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "greeting.txt"
        )

        Then
        resource.key shouldBe "greeting.txt"
        resource.chunks.size shouldBe 1
    }

    "creating resource with multiple chunks" {
        Given
        val chunk1 = "First part"
        val chunk2 = "Second part"
        val chunk3 = "Third part"
        val base64Chunks = listOf(
            chunk1.encodeUtf8().encodeChunk(),
            chunk2.encodeUtf8().encodeChunk(),
            chunk3.encodeUtf8().encodeChunk()
        )

        When
        val resource = Resource(
            chunks = base64Chunks,
            key = "multipart.txt"
        )

        Then
        resource.chunks.size shouldBe 3
        resource.key shouldBe "multipart.txt"
    }

    "reading resource as string from single chunk" {
        Given
        val content = "Test content"
        val base64Chunk = content.encodeUtf8().encodeChunk()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "test.txt"
        )

        When
        val result = resource.asString

        Then
        result shouldBe content
    }

    "reading resource as bytes from single chunk" {
        Given
        val content = "Binary content"
        val expectedBytes = content.encodeUtf8().toByteArray()
        val base64Chunk = content.encodeUtf8().encodeChunk()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "binary.bin"
        )

        When
        val result = resource.asByteArray

        Then
        result shouldBe expectedBytes
    }

    "reading resource as string from multiple chunks" {
        Given
        val fullContent = "Hello beautiful world!"
        val base64Chunks = createBase64Chunks(fullContent, 3)
        val resource = Resource(
            chunks = base64Chunks,
            key = "multi.txt"
        )

        When
        val result = resource.asString

        Then
        result shouldBe fullContent
    }

    "reading resource as bytes from multiple chunks" {
        Given
        val fullContent = "Part1Part2Part3"
        val expectedBytes = fullContent.encodeUtf8().toByteArray()
        val base64Chunks = createBase64Chunks(fullContent, 3)
        val resource = Resource(
            chunks = base64Chunks,
            key = "parts.bin"
        )

        When
        val result = resource.asByteArray

        Then
        result shouldBe expectedBytes
    }

    "calculating size for single chunk resource" {
        Given
        val content = "Small content"
        val base64Chunk = content.encodeUtf8().base64()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "small.txt"
        )

        When
        val size = resource.size

        Then
        // For single chunk: size = base64 chunk length
        size shouldBe base64Chunk.length
    }

    "calculating size for multiple chunk resource" {
        Given
        val chunk1 = "A".repeat(100)
        val chunk2 = "B".repeat(200)
        val chunk3 = "C".repeat(300)
        val base64Chunks = listOf(
            chunk1.encodeUtf8().base64(),
            chunk2.encodeUtf8().base64(),
            chunk3.encodeUtf8().base64()
        )
        val resource = Resource(
            chunks = base64Chunks,
            key = "sized.txt"
        )

        When
        val size = resource.size

        Then
        // Size = (chunks.size - 1) * RESOURCE_CHUNK_SIZE + last chunk length
        val expectedSize = 2 * RESOURCE_CHUNK_SIZE + base64Chunks.last().length
        size shouldBe expectedSize
    }

    "size calculation uses RESOURCE_CHUNK_SIZE constant" {
        Given
        val base64Chunks = listOf(
            "chunk1".encodeUtf8().base64(),
            "chunk2".encodeUtf8().base64(),
            "chunk3".encodeUtf8().base64(),
            "final".encodeUtf8().base64()
        )
        val resource = Resource(
            chunks = base64Chunks,
            key = "test.txt"
        )

        When
        val size = resource.size

        Then
        // Size = 3 * 60000 + length of last chunk
        size shouldBe 3 * RESOURCE_CHUNK_SIZE + base64Chunks.last().length
    }

    "asString is lazily evaluated" {
        Given
        val content = "Lazy content"
        val base64Chunk = content.encodeUtf8().encodeChunk()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "lazy.txt"
        )

        When
        // Access asString twice
        val result1 = resource.asString
        val result2 = resource.asString

        Then
        // Both should be the same instance (lazy evaluation)
        result1 shouldBeSameInstanceAs result2
        result1 shouldBe content
    }

    "asBytes is lazily evaluated" {
        Given
        val content = "Lazy bytes"
        val base64Chunk = content.encodeUtf8().encodeChunk()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "lazy.bin"
        )

        When
        // Access asBytes twice
        val result1 = resource.asByteArray
        val result2 = resource.asByteArray

        Then
        // Both should be the same instance (lazy evaluation)
        result1 shouldBeSameInstanceAs result2
        result1 shouldBe content.encodeUtf8().toByteArray()
    }

    "empty content resource" {
        Given
        val emptyContent = ""
        val base64Chunk = emptyContent.encodeUtf8().encodeChunk()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "empty.txt"
        )

        When
        val asString = resource.asString
        val asBytes = resource.asByteArray

        Then
        asString shouldBe ""
        asBytes.size shouldBe 0
    }

    "resource with unicode content" {
        Given
        val unicodeContent = "Hello 世界 🌍 Привет"
        val base64Chunk = unicodeContent.encodeUtf8().encodeChunk()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "unicode.txt"
        )

        When
        val result = resource.asString

        Then
        result shouldBe unicodeContent
        result shouldContain "世界"
        result shouldContain "🌍"
        result shouldContain "Привет"
    }

    "resource with special characters" {
        Given
        val specialContent = "Tab:\t Newline:\n Quote:\" Backslash:\\ Null:\u0000"
        val base64Chunk = specialContent.encodeUtf8().encodeChunk()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "special.txt"
        )

        When
        val result = resource.asString

        Then
        result shouldBe specialContent
    }

    "resource with large content in single chunk" {
        Given
        val largeContent = "X".repeat(10_000)
        val base64Chunk = largeContent.encodeUtf8().encodeChunk()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "large.txt"
        )

        When
        val result = resource.asString

        Then
        result shouldBe largeContent
        result.length shouldBe 10_000
    }

    "resource with large content in multiple chunks" {
        Given
        val fullContent = "A".repeat(50_000) + "B".repeat(50_000) + "C".repeat(50_000)
        val base64Chunks = createBase64Chunks(fullContent, 3)
        val resource = Resource(
            chunks = base64Chunks,
            key = "huge.txt"
        )

        When
        val result = resource.asString

        Then
        result shouldBe fullContent
        result.length shouldBe 150_000
    }

    "data class equality with identical content" {
        Given
        val content = "Same content"
        val base64Chunk = content.encodeUtf8().base64()
        val resource1 = Resource(
            chunks = listOf(base64Chunk),
            key = "file.txt"
        )
        val resource2 = Resource(
            chunks = listOf(base64Chunk),
            key = "file.txt"
        )

        When
        val areEqual = resource1 == resource2

        Then
        areEqual shouldBe true
        resource1.hashCode() shouldBe resource2.hashCode()
    }

    "data class inequality with different keys" {
        Given
        val content = "Same content"
        val base64Chunk = content.encodeUtf8().base64()
        val resource1 = Resource(
            chunks = listOf(base64Chunk),
            key = "file1.txt"
        )
        val resource2 = Resource(
            chunks = listOf(base64Chunk),
            key = "file2.txt"
        )

        When
        val areEqual = resource1 == resource2

        Then
        areEqual shouldBe false
    }

    "data class inequality with different content" {
        Given
        val content1 = "First content"
        val content2 = "Second content"
        val resource1 = Resource(
            chunks = listOf(content1.encodeUtf8().base64()),
            key = "file.txt"
        )
        val resource2 = Resource(
            chunks = listOf(content2.encodeUtf8().base64()),
            key = "file.txt"
        )

        When
        val areEqual = resource1 == resource2

        Then
        areEqual shouldBe false
    }

    "data class copy functionality" {
        Given
        val original = Resource(
            chunks = listOf("content".encodeUtf8().base64()),
            key = "original.txt"
        )

        When
        val copied = original.copy(key = "copied.txt")

        Then
        copied.key shouldBe "copied.txt"
        copied.chunks shouldBe original.chunks
        copied shouldNotBe original
    }

    "resource key is preserved" {
        Given
        val content = "Test content"
        val base64Chunk = content.encodeUtf8().base64()

        When
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "unique-key-123"
        )

        Then
        resource.key shouldBe "unique-key-123"
    }

    "asString and asBytes represent same content" {
        Given
        val content = "Consistent content"
        val base64Chunk = content.encodeUtf8().encodeChunk()
        val resource = Resource(
            chunks = listOf(base64Chunk),
            key = "consistent.txt"
        )

        When
        val asString = resource.asString
        val asBytes = resource.asByteArray

        Then
        asString shouldBe content
        asBytes.toByteString().utf8() shouldBe content
        asBytes.toByteString().utf8() shouldBe asString
    }

    "chunks are joined then decoded correctly"(
        row("ABC", 2),
        row("Hello World", 3),
        row("12345", 5),
        row("", 1),
        row("Single", 1)
    ) { (content, numChunks) ->
        Given("content: '$content' split into $numChunks chunks")
        val base64Chunks = createBase64Chunks(content, numChunks)
        val resource = Resource(
            chunks = base64Chunks,
            key = "test.txt"
        )

        When
        val result = resource.asString

        Then
        result shouldBe content
    }

    "size calculation is correct for various chunk counts"(
        row(1, "X".repeat(100)),
        row(2, "A".repeat(5000)),
        row(3, "B".repeat(10000)),
        row(5, "12345")
    ) { (targetChunkCount, content) ->
        Given("targeting $targetChunkCount chunks")
        val base64Chunks = createBase64Chunks(content, targetChunkCount)
        val resource = Resource(
            chunks = base64Chunks,
            key = "test.txt"
        )

        When
        val size = resource.size

        Then
        val expectedSize = if (base64Chunks.size == 1) {
            base64Chunks.first().length
        } else {
            (base64Chunks.size - 1) * RESOURCE_CHUNK_SIZE + base64Chunks.last().length
        }
        size shouldBe expectedSize
    }
})

/**
 * Helper function to create base64 chunks from content.
 * Mimics the behavior of the actual code generator: split the raw bytes into chunks,
 * then encode each chunk separately to base64.
 */
private fun createBase64Chunks(content: String, numChunks: Int): List<String> {
    val bytes = content.encodeUtf8()

    return when {
        bytes.size == 0 -> listOf("".encodeUtf8().encodeChunk())
        numChunks <= 1 -> listOf(bytes.encodeChunk())
        else -> {
            val chunkSize = maxOf(1, bytes.size / numChunks)
            buildList {
                var offset = 0
                while (offset < bytes.size) {
                    val end = minOf(offset + chunkSize, bytes.size)
                    add(bytes.substring(offset, end).encodeChunk())
                    offset = end
                }
            }
        }
    }
}
