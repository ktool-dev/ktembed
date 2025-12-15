package dev.ktool.embed

import okio.FileSystem
import okio.Path

// There is no file system for the web target
actual fun getFileSystem(): FileSystem? = null

actual fun getTempDirectory(): Path? = null