package dev.ktool.ktembed

import java.io.File

internal actual object FileIO {
    actual fun exists(path: String): Boolean {
        return File(path).exists()
    }
    
    actual fun writeBytes(path: String, bytes: ByteArray) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }
    
    actual fun readBytes(path: String): ByteArray {
        return File(path).readBytes()
    }
    
    actual fun createDirectories(path: String) {
        File(path).mkdirs()
    }
}
