package dev.ktool.ktembed

/**
 * Decodes Base64-encoded data that may be split across multiple string chunks.
 * 
 * This decoder handles the case where Base64 data is stored as a list of strings
 * (to avoid excessively long string literals in generated code).
 */
internal object Base64Decoder {
    
    private val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val base64LookupTable = IntArray(256) { -1 }.apply {
        BASE64_CHARS.forEachIndexed { index, char ->
            this[char.code] = index
        }
        this['='.code] = 0
    }

    /**
     * Decodes a single Base64-encoded string into bytes.
     */
    fun decode(base64: String): ByteArray {
        val cleanInput = base64.filter { it in BASE64_CHARS || it == '=' }
        if (cleanInput.isEmpty()) return ByteArray(0)

        val outputLength = calculateOutputLength(cleanInput)
        val output = ByteArray(outputLength)
        
        var outputIndex = 0
        var i = 0
        
        while (i < cleanInput.length) {
            val b1 = base64LookupTable[cleanInput[i++].code]
            if (i >= cleanInput.length) break
            
            val b2 = base64LookupTable[cleanInput[i++].code]
            val b3 = if (i < cleanInput.length && cleanInput[i] != '=') {
                base64LookupTable[cleanInput[i++].code]
            } else {
                i++
                -1
            }
            val b4 = if (i < cleanInput.length && cleanInput[i] != '=') {
                base64LookupTable[cleanInput[i++].code]
            } else {
                i++
                -1
            }

            // Combine the bits
            output[outputIndex++] = ((b1 shl 2) or (b2 shr 4)).toByte()
            
            if (b3 != -1) {
                output[outputIndex++] = ((b2 shl 4) or (b3 shr 2)).toByte()
                
                if (b4 != -1) {
                    output[outputIndex++] = ((b3 shl 6) or b4).toByte()
                }
            }
        }
        
        return output.copyOf(outputIndex)
    }

    /**
     * Decodes Base64 data that has been split across multiple string chunks.
     * The chunks are concatenated before decoding.
     */
    fun decode(chunks: List<String>): ByteArray {
        if (chunks.isEmpty()) return ByteArray(0)
        if (chunks.size == 1) return decode(chunks[0])
        
        // Concatenate all chunks
        val combined = buildString(chunks.sumOf { it.length }) {
            chunks.forEach { append(it) }
        }
        
        return decode(combined)
    }

    /**
     * Decodes Base64 data in a streaming fashion, yielding decoded chunks.
     * This is useful for large resources to avoid loading everything into memory at once.
     */
    fun decodeStream(chunks: List<String>, chunkSize: Int = 8192): Sequence<ByteArray> = sequence {
        if (chunks.isEmpty()) return@sequence
        
        // For simplicity, we'll decode the entire thing and then chunk the output
        // A more sophisticated implementation could decode chunks incrementally
        val decoded = decode(chunks)
        
        var offset = 0
        while (offset < decoded.size) {
            val size = minOf(chunkSize, decoded.size - offset)
            yield(decoded.copyOfRange(offset, offset + size))
            offset += size
        }
    }

    private fun calculateOutputLength(base64: String): Int {
        val padding = base64.count { it == '=' }
        val dataLength = base64.length - padding
        return (dataLength * 3) / 4
    }
}
