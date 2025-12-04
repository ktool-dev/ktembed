package dev.ktool.embed

import okio.*
import okio.ByteString.Companion.decodeBase64

internal const val RESOURCE_CHUNK_SIZE = 60_000
internal const val IN_MEMORY_CUT_OFF = RESOURCE_CHUNK_SIZE * 100

internal expect fun getSystemTempDirectory(): Path?

/**
 * Computes a SHA-256 hash from decoded Base64 chunks.
 * This decodes each chunk and processes it individually.
 *
 * @return 64-character hexadecimal SHA-256 hash
 */
internal fun computeHash(chunks: List<String>): String {
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
internal fun computeHash(path: Path, fileSystem: FileSystem = FileSystem.SYSTEM): String {
    val hashingSink = HashingSink.sha256(blackholeSink())
    hashingSink.buffer().use { buffer ->
        fileSystem.source(path).buffer().use { buffer.writeAll(it) }
    }
    return hashingSink.hash.hex()
}
