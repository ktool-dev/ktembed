package dev.ktool.embed

import okio.Buffer
import okio.ByteString

data class Resource(val key: String, val chunks: List<String>) {
    val size = (chunks.size - 1) * RESOURCE_CHUNK_SIZE + chunks.last().length
    val asString: String by lazy { byteString.utf8() }

    val asByteArray: ByteArray by lazy { byteString.toByteArray() }

    private val byteString: ByteString by lazy {
        val buffer = Buffer()
        chunks.forEach { buffer.write(it.decodeChunk()) }
        buffer.readByteString()
    }
}
