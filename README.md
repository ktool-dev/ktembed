# KtEmbed

> Embed static resources directly into your Kotlin binaries

KtEmbed is a Kotlin Multiplatform library and Gradle plugin that lets you embed static resources (text, JSON, HTML,
images, etc.) directly into your compiled applications. Resources are encoded as Base64 strings at compile time and can
be accessed efficiently at runtime with automatic caching and optimization strategies.

## Features

- 🚀 **Zero Runtime Dependencies** - Resources are embedded directly in your compiled code
- 🔧 **Gradle Integration** - Simple plugin configuration with automatic code generation
- 🎯 **Kotlin Multiplatform** - Works on JVM, Native, and other Kotlin targets
- 💾 **Smart Caching** - Automatic memory and disk caching with configurable strategies
- ⚡ **Optimized Access** - Choose between speed (in-memory) or memory efficiency (streaming)

## Quick Start

### 1. Apply the Plugin

Add the KtEmbed plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("dev.ktool.ktembed") version "0.0.1"
}
```

### 2. Configure Resource Directories

```kotlin
ktembed {
    packageName = "com.example.resources"
    resourceDirectories = files("src/main/resources")

    // Optional: filter out specific files
    filter = { path ->
        path.endsWith(".tmp") || path.contains("secret")
    }
}
```

### 3. Access Your Resources

```kotlin
import com.example.resources.ResourceDirectory
import dev.ktool.embed.Resources

// Initialize the Resources instance
val resources = Resources(ResourceDirectory)

// Read as string
val config = resources.asString("config.json")

// Read as bytes
val image = resources.asBytes("logo.png")

// Check if resource exists
if (resources.exists("optional.txt")) {
    println(resources.asString("optional.txt"))
}

// Write to output stream (JVM only)
FileOutputStream("output.png").use { out ->
    resources.write("logo.png", out)
}

// Get file path (cached to temp directory)
val filePath = resources.asPath("data.bin")
```

## Installation

### Gradle (Kotlin DSL)

```kotlin
// In settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

// In build.gradle.kts
plugins {
    id("dev.ktool.ktembed") version "0.0.1"
}

dependencies {
    implementation("dev.ktool:ktembed-runtime:0.0.1")
}
```

## Configuration

### Plugin Extension

The `ktembed` extension provides the following configuration options:

| Property              | Type                  | Required | Description                                              |
|-----------------------|-----------------------|----------|----------------------------------------------------------|
| `packageName`         | `String`              | Yes      | Package name for the generated `ResourceDirectory` class |
| `resourceDirectories` | `FileCollection`      | Yes      | Directories to scan for resources                        |
| `filter`              | `(String) -> Boolean` | No       | Function to exclude paths (returns `true` to ignore)     |

### Example Configuration

```kotlin
ktembed {
    // Package for generated code
    packageName = "com.myapp.assets"

    // Multiple resource directories
    resourceDirectories = files(
        "src/main/resources",
        "assets",
        "static"
    )

    // Filter function to exclude files
    filter = { path ->
        path.endsWith(".tmp") ||           // Ignore temp files
                path.contains(".git") ||           // Ignore git files
                path.startsWith("private/")        // Ignore private directory
    }
}
```

## API Reference

### Resources Class

The main API for accessing embedded resources.

#### Methods

##### `exists(path: String): Boolean`

Check if a resource exists at the given path.

```kotlin
if (resources.exists("config.json")) {
    // Resource exists
}
```

##### `asString(path: String): String`

Read resource as UTF-8 string. Result is cached in memory.

```kotlin
val json = resources.asString("data.json")
```

##### `asBytes(path: String): ByteString`

Read resource as bytes. Result is cached in memory.

```kotlin
val imageData = resources.asBytes("image.png")
```

##### `asPath(path: String): Path?`

Get a file system path to the resource. The resource is extracted to a cache directory and validated.

```kotlin
val configFile = resources.asPath("config.properties")
// Use with libraries that need file paths
```

##### `write(path: String, output: Sink)`

Write resource to an Okio `Sink`. Automatically chooses optimization strategy based on `inMemoryCutoff`.

```kotlin
FileSystem.SYSTEM.sink(outputPath).use { sink ->
    resources.write("large-file.bin", sink)
}
```

##### `write(path: String, output: Sink, strategy: OptimizationStrategy)`

Write resource with explicit optimization strategy.

```kotlin
resources.write("data.bin", sink, OptimizationStrategy.Memory)
```

##### `write(path: String, out: OutputStream)` (JVM only)

Write resource to a Java `OutputStream`.

```kotlin
FileOutputStream("output.txt").use { out ->
    resources.write("input.txt", out)
}
```

### Optimization Strategies

Choose between speed and memory efficiency:

```kotlin
enum class OptimizationStrategy {
    Speed,   // Load entire resource into memory (faster)
    Memory   // Stream from disk cache (uses less memory)
}
```

**When to use:**

- `Speed`: Small resources, frequent access, plenty of memory
- `Memory`: Large resources (>6MB), infrequent access, memory-constrained environments

### Generated Code

The plugin generates a `ResourceDirectory` class in your specified package:

```kotlin
package com.example.resources

import dev.ktool.embed.Resource
import dev.ktool.embed.ResourceDirectory

object ResourceDirectory : ResourceDirectory {
    override val key: String = "com-example-resources"

    private val resources: Map<String, Resource> = mapOf(
        "config.json" to Resource("config.json", listOf(...)
    ),
    "logo.png" to Resource("logo.png", listOf(...)),
    // ... more resources
    )

    override operator fun get(path: String): Resource? = resources[path]
}
```

## How It Works

1. **Build Time**: The Gradle plugin scans your resource directories and generates Kotlin code with Base64-encoded
   resources
2. **Compile Time**: Resources are compiled directly into your application binary
3. **Runtime**: Resources accessed through `Resources` class and are lazily decoded and cached as needed

### Resource Encoding

- Resources are split into chunks (to avoid JVM string literal limits)
- Each chunk is Base64-encoded
- Chunks are stored as string literals in generated Kotlin code
- Decoding happens lazily on first access

### Caching Strategy

- **In-Memory**: Decoded `ByteString` and `String` values are cached per `Resource` instance
- **Disk Cache**: Large resources can be cached in the system temp directory
- **Validation**: Cached files are validated by hash to ensure integrity

### Resource Validation

Resources extracted to disk are automatically validated:

```kotlin
val filePath = resources.asPath("important.dat")
// File is validated by comparing hashes
// If validation fails, the file is regenerated
```

## Performance Tips

1. **Use `write()` for large files** instead of `asBytes()` to avoid loading into memory
2. **Set appropriate `inMemoryCutoff`** based on your application's memory constraints
3. **Use `OptimizationStrategy.Memory`** for resources >6MB
4. **Cache `Resources` instance** - don't create multiple instances of the same `ResourceDirectory`

## Troubleshooting

### Resources not found at runtime

Ensure the `generateKtEmbedResources` task runs before compilation:

```kotlin
tasks.named("compileKotlin") {
    dependsOn("generateKtEmbedResources")
}
```

This is handled automatically by the plugin, but can be added explicitly if needed.

### Out of memory errors

Reduce the `inMemoryCutoff` value:

```kotlin
val resources = Resources(ResourceDirectory(), inMemoryCutoff = 1_000_000) // 1MB
```

Or use `OptimizationStrategy.Memory` for large resources:

```kotlin
resources.write("large-file.bin", output, OptimizationStrategy.Memory)
```

### Filtered resources still appearing

The filter function returns `true` for paths to **ignore**:

```kotlin
// Correct: ignore .tmp files
filter = { path -> path.endsWith(".tmp") }

// Incorrect: this would ignore everything EXCEPT .tmp files
filter = { path -> !path.endsWith(".tmp") }
```
