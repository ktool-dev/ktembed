package dev.ktool.ktembed.gradle

import java.security.MessageDigest

/**
 * Computes content hashes for cache versioning.
 */
internal object ContentHasher {
    
    /**
     * Computes a hash of the given bytes using SHA-256.
     * Returns a hex string representation.
     */
    fun hash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
