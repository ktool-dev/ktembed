package dev.ktool.ktembed

/**
 * Computes a simple hash of byte content for cache versioning.
 * This is not a cryptographic hash, just a simple checksum for cache invalidation.
 */
internal object ContentHash {
    /**
     * Computes a hash string from the given bytes using a simple FNV-1a algorithm.
     * This is fast and good enough for cache invalidation purposes.
     */
    fun compute(bytes: ByteArray): String {
        var hash = 0x811c9dc5L // FNV offset basis
        val prime = 0x01000193L

        for (byte in bytes) {
            hash = hash xor (byte.toLong() and 0xff)
            hash = (hash * prime) and 0xffffffffL
        }

        return hash.toString(16).padStart(8, '0')
    }

    /**
     * Computes a hash from Base64 chunks without decoding them.
     * This is useful when we want to compute the hash of embedded data
     * that's already in Base64 form.
     */
    fun computeFromBase64Chunks(chunks: List<String>): String {
        var hash = 0x811c9dc5L // FNV offset basis
        val prime = 0x01000193L

        for (chunk in chunks) {
            for (char in chunk) {
                val byte = char.code.toByte()
                hash = hash xor (byte.toLong() and 0xff)
                hash = (hash * prime) and 0xffffffffL
            }
        }

        return hash.toString(16).padStart(8, '0')
    }
}
