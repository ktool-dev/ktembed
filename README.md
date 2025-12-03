# KtEmbed

> Embed static resources directly into your Kotlin binaries

KtEmbed is a Kotlin Multiplatform library + Gradle plugin that lets you embed static resources (text, JSON, HTML, images, etc.) directly into your compiled applications, inspired by Go's `//go:embed` directive.

## Features

- 🚀 **Build-time embedding**: Resources are encoded and embedded at compile time
- 💾 **Flexible access strategies**: Choose between InMemory (fast) or DiskCached (low memory)
- 🔄 **Smart caching**: Content-hash based versioning for efficient cache reuse
- 📦 **Code sharding**: Automatically split large asset sets across multiple files
- 🌍 **Full KMP support**: Works on JVM, Native (Linux, macOS, Windows), JS, and WASM

## Quick Start

### 1. Add the plugin

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.21"
    id("dev.ktool.ktembed") version "1.0.0"
}

dependencies {
    commonMainImplementation("dev.ktool:ktembed-runtime:1.0.0")
}
```

### 2. Configure resources

```kotlin
ktEmbed {
    resourceDirs.set(listOf("src/main/resources/static"))
    packageName.set("com.example.embedded")
    shardSize.set(50) // Optional: split into files of 50 resources each
}
```

### 3. Use in your code

```kotlin
import com.example.embedded.KtEmbedResources

suspend fun main() {
    // Access embedded resources
    val logo = KtEmbedResources.logo_png.asBytes()
    val config = KtEmbedResources.config_json.asString()
    
    // Stream large files
    KtEmbedResources.video_mp4.asStream().forEach { chunk ->
        // Process chunk
    }
}
```

## Configuration

```kotlin
ktEmbed {
    // List of resource directories to scan
    resourceDirs.set(listOf("src/main/resources"))
    
    // Package name for generated code
    packageName.set("embedded")
    
    // Output directory for generated sources
    outputDir.set(layout.buildDirectory.dir("generated/ktembed"))
    
    // Maximum resources per generated file (0 = no sharding)
    shardSize.set(50)
    
    // Size of Base64 string chunks (characters)
    chunkSize.set(32000)
    
    // Generate InMemory resources (fast access, higher RAM)
    generateInMemory.set(true)
    
    // Generate DiskCached resources (slower access, lower RAM)
    generateDiskCached.set(false)
}
```

## Access Modes

### InMemory
- **Best for**: Small to medium resources (< 10MB), frequently accessed
- **Pros**: Fast access after initial decode, no disk I/O
- **Cons**: Higher memory usage

### DiskCached
- **Best for**: Large resources (> 10MB), infrequently accessed
- **Pros**: Low memory usage, cache reused across runs
- **Cons**: More I/O operations, slightly slower

## How It Works

1. **Build Time**: 
   - Scans configured resource directories
   - Base64-encodes each file and splits into chunks
   - Generates Kotlin source files with embedded data
   
2. **Runtime**:
   - InMemory: Lazy decoding on first access, kept in memory
   - DiskCached: Streams to cache file (hash-based), reads from disk

## Platform Support

- ✅ JVM (Java 17+)
- ✅ Native: Linux x64, macOS (x64 & ARM64), Windows x64
- ✅ JavaScript (Browser & Node.js)
- ✅ WASM

## Example Project Structure

```
my-app/
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── Main.kt
│       └── resources/
│           └── static/
│               ├── logo.png
│               ├── config.json
│               └── index.html
└── build.gradle.kts
```

## License

Apache License 2.0

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Related Projects

- [Kelion](https://github.com/ktool/kelion) - Desktop framework that uses KtEmbed
