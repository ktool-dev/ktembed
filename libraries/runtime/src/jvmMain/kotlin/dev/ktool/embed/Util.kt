@file:JvmName("UtilJvm")

package dev.ktool.embed

import okio.Path
import okio.Path.Companion.toPath

actual fun getSystemTempDirectory(): Path? {
    return System.getProperty("java.io.tmpdir")?.toPath()
}
