package dev.ktool.embed

import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.SYSTEM

val Path.absolute: String get() = normalized().toString()
fun Path.readText(): String = FileSystem.SYSTEM.read(this) { readUtf8() }
fun Path.write(buffer: Buffer) = FileSystem.SYSTEM.read(this) {
    readAll(buffer)
}

fun Path.writeText(content: String) {
    FileSystem.SYSTEM.write(this) { writeUtf8(content) }
}

fun Path.list(): List<Path> = FileSystem.SYSTEM.list(this)
val Path.isDirectory: Boolean get() = FileSystem.SYSTEM.metadataOrNull(this)?.isDirectory == true
val Path.exists: Boolean get() = FileSystem.SYSTEM.exists(this)
fun Path.remove(): Boolean {
    FileSystem.SYSTEM.delete(this, false)
    return !exists
}

fun Path.mkDirs(): Path {
    if (exists) return this

    if (isDirectory) {
        FileSystem.SYSTEM.createDirectories(this, true)
    } else {
        FileSystem.SYSTEM.createDirectories(this.parent!!, true)
    }

    return this
}
