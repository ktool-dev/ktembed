package dev.ktool.ktembed.gradle

import java.util.Base64

/**
 * Encodes bytes as Base64 and splits into chunks for generated code.
 */
internal object Base64Encoder {
    
    private val encoder = Base64.getEncoder()
    
    /**
     * Encodes bytes to Base64 and splits into chunks of the specified size.
     * 
     * @param bytes The bytes to encode
     * @param chunkSize Maximum size of each chunk (in characters)
     * @return List of Base64-encoded string chunks
     */
    fun encodeToChunks(bytes: ByteArray, chunkSize: Int = 32000): List<String> {
        if (bytes.isEmpty()) return emptyList()
        
        val base64 = encoder.encodeToString(bytes)
        
        // Split into chunks
        val chunks = mutableListOf<String>()
        var offset = 0
        
        while (offset < base64.length) {
            val end = minOf(offset + chunkSize, base64.length)
            chunks.add(base64.substring(offset, end))
            offset = end
        }
        
        return chunks
    }
}
