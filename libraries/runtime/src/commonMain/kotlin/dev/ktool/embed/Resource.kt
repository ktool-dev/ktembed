package dev.ktool.embed

import okio.ByteString
import okio.ByteString.Companion.decodeBase64

data class Resource(val key: String, val chunks: List<String>) {
    val size = (chunks.size - 1) * RESOURCE_CHUNK_SIZE + chunks.last().length
    val asString: String by lazy { asBytes.utf8() }

    val asBytes: ByteString by lazy { chunks.joinToString("").decodeBase64()!! }
}

