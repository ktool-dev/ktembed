package dev.ktool.ktembed

/**
 * An embedded resource that decodes once and keeps the data in memory.
 * Thread-safe lazy initialization ensures the resource is decoded only once.
 */
class InMemoryResource(
    override val path: String,
    override val contentHash: String,
    private val base64Chunks: List<String>
) : EmbeddedResource {
    
    override val accessMode: ResourceAccessMode = ResourceAccessMode.IN_MEMORY
    
    // Lazy initialization ensures thread-safe, one-time decoding
    private val decodedBytes: ByteArray by lazy {
        Base64Decoder.decode(base64Chunks)
    }
    
    override suspend fun asBytes(): ByteArray {
        return decodedBytes
    }
    
    override suspend fun asStream(): Sequence<ByteArray> = sequence {
        // For in-memory resources, we just yield the entire decoded bytes
        // as a single chunk
        yield(decodedBytes)
    }
    
    override fun toString(): String {
        return "InMemoryResource(path='$path', hash='$contentHash', size=${decodedBytes.size} bytes)"
    }
}
