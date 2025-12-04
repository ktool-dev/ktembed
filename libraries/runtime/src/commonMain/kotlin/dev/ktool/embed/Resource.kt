package dev.ktool.embed

import okio.ByteString
import okio.ByteString.Companion.decodeBase64

data class EmbeddedResource(
    val chunks: List<String>,
    val path: String,
    val key: String,
) {
    val size = (chunks.size - 1) * RESOURCE_CHUNK_SIZE + chunks.last().length
    val asString: String by lazy { asBytes.utf8() }

    val asBytes: ByteString by lazy { chunks.joinToString("").decodeBase64()!! }
}

interface ResourceDirectory {
    operator fun get(path: String): EmbeddedResource?
    val key: String
}
