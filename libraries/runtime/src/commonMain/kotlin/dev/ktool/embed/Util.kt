package dev.ktool.embed

import korlibs.io.compression.compress
import korlibs.io.compression.deflate.ZLib
import korlibs.io.compression.uncompress
import okio.*
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

const val RESOURCE_CHUNK_SIZE = 40_000L
internal const val IN_MEMORY_CUT_OFF = RESOURCE_CHUNK_SIZE * 100

expect fun getFileSystem(): FileSystem?
expect fun getTempDirectory(): Path?

/**
 * Computes a SHA-256 hash from decoded Base64 chunks.
 * This decodes each chunk and processes it individually.
 *
 * @return 64-character hexadecimal SHA-256 hash
 */
fun computeHash(chunks: List<String>): String {
    val hashingSink = HashingSink.sha256(blackholeSink())
    hashingSink.buffer().use { buffer ->
        chunks.forEach { it.decodeBase64()?.also(buffer::write) }
    }
    return hashingSink.hash.hex()
}

/**
 * Computes a SHA-256 hash from file content by streaming it in chunks.
 *
 * @return 64-character hexadecimal SHA-256 hash
 */
internal fun computeHash(path: Path, fileSystem: FileSystem): String {
    val hashingSink = HashingSink.sha256(blackholeSink())
    hashingSink.buffer().use { buffer ->
        fileSystem.source(path).buffer().use { buffer.writeAll(it) }
    }
    return hashingSink.hash.hex()
}

fun String.decodeChunk(): ByteArray = decodeBase64()?.toByteArray()?.uncompress(ZLib) ?: error("Unable to decode chunk")

fun Buffer.encodeChunk(): String = readByteString().encodeChunk()
fun ByteString.encodeChunk() = toByteArray().encodeChunk()
fun ByteArray.encodeChunk() = compress(ZLib).toByteString().base64()
