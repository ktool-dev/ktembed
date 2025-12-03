# KtEmbed - Initial Project Plan
*Generated: December 2, 2025*

## Project Overview

KtEmbed is a Kotlin Multiplatform library + Gradle plugin that enables embedding static resources (text, JSON, HTML, images, etc.) directly into compiled binaries for both JVM and Kotlin/Native targets.

**Inspired by**: Go's `//go:embed` directive

### Key Features

1. **Build-time Processing**: Scans resource directories, Base64-encodes files, chunks data, and generates Kotlin source code
2. **Runtime Flexibility**: Two access strategies per resource
   - **InMemory**: Decode once, keep in RAM (fast access, higher memory)
   - **DiskCached**: Stream to cache file, read from disk (low RAM, more I/O)
3. **Smart Caching**: Content-hash versioning for efficient cache reuse
4. **Code Sharding**: Split generated code across multiple files to avoid compiler/IDE issues
5. **Multiplatform**: Full KMP support (JVM, Native, JS, WASM)

## Architecture

### Three Main Components

1. **Runtime Module** (multiplatform)
   - Public API for consuming embedded resources
   - Base64 decoding and chunked string handling
   - Platform-specific cache/temp directory management
   - InMemory and DiskCached strategies
   - Content hash computation

2. **Gradle Plugin Module**
   - File system scanner at build time
   - Base64 encoder with chunking
   - Content hash computation for files
   - Kotlin code generator
   - Sharding logic for large asset sets
   - KMP integration

3. **Generated Code**
   - Kotlin objects (e.g., `KtEmbedResources`)
   - Private `List<String>` Base64 chunk storage
   - Public `EmbeddedResource` instances with mode and hash

## Project Structure

```
KtEmbed/
├── libraries/
│   ├── runtime/          # Multiplatform runtime library
│   └── gradle-plugin/    # Gradle plugin (to be created)
├── applications/         # Example applications
└── integrations/         # Framework integrations
```

## Implementation Plan

All tasks are tracked in bd (beads). Use `bd ready` to see available work.

### Phase 1: Runtime Module (Priority 0-1)

**Epic: KtEmbed-1nm - Implement runtime module core API**

Critical Path (P0):
- `KtEmbed-kwz`: Define EmbeddedResource interface/sealed class hierarchy
- `KtEmbed-2t5`: Implement ResourceAccessMode (InMemory/DiskCached)
- `KtEmbed-ol5`: Implement Base64 decoder with chunked string handling
- `KtEmbed-btg`: Implement platform-specific temp/cache directory paths

High Priority (P1):
- `KtEmbed-qlc`: Implement InMemory strategy with lazy decoding
- `KtEmbed-xsk`: Implement DiskCached strategy with streaming and hash-based cache keys
- `KtEmbed-131`: Implement content hash computation for cache versioning
- `KtEmbed-vlh`: Add public API for resource access (bytes, string, stream)

### Phase 2: Gradle Plugin (Priority 0-1)

**Epic: KtEmbed-9i9 - Implement Gradle plugin**

Critical Path (P0):
- `KtEmbed-kds`: Create gradle-plugin module structure
- `KtEmbed-dw3`: Define KtEmbedExtension DSL for plugin configuration
- `KtEmbed-1ts`: Implement resource directory scanner
- `KtEmbed-qh9`: Implement Base64 encoder with chunking strategy
- `KtEmbed-xfg`: Implement content hash computation for files

High Priority (P1):
- `KtEmbed-sjh`: Implement code generator for KtEmbedResources object
- `KtEmbed-541`: Create Gradle task to generate embedded resources
- `KtEmbed-o80`: Integrate plugin with Kotlin multiplatform source sets

Medium Priority (P2):
- `KtEmbed-5ga`: Implement sharding logic to split generated code across multiple files

### Phase 3: Testing & Validation (Priority 1-2)

**Epic: KtEmbed-ekl - Testing and validation**

High Priority (P1):
- `KtEmbed-198`: Write unit tests for runtime Base64 decoder
- `KtEmbed-tsw`: Write unit tests for InMemory strategy
- `KtEmbed-p9x`: Write unit tests for DiskCached strategy
- `KtEmbed-ca3`: Write tests for Gradle plugin code generation
- `KtEmbed-0fx`: Test on all KMP targets (JVM, Native, JS, WASM)

Medium Priority (P2):
- `KtEmbed-hro`: Create integration test with sample project

### Phase 4: Documentation & Publishing (Priority 1-2)

**Epic: KtEmbed-999 - Setup project structure and documentation**

High Priority (P1):
- `KtEmbed-ufb`: Create project README with overview and usage examples

Medium Priority (P2):
- `KtEmbed-b3g`: Add KDoc documentation to public API
- `KtEmbed-1lq`: Setup Maven publishing configuration

### Phase 5: Examples (Priority 2)

- `KtEmbed-0r8`: Create example application demonstrating usage

## Usage Example (Target API)

```kotlin
// In build.gradle.kts
plugins {
    id("dev.ktool.ktembed")
}

ktEmbed {
    resourceDirs = listOf("src/main/resources/static")
    packageName = "com.example.embedded"
    shardSize = 50 // Split into files of 50 resources each
}

// Generated code usage
import com.example.embedded.KtEmbedResources

fun main() {
    // InMemory - fast access
    val logo = KtEmbedResources.logo_png.asBytes()
    
    // DiskCached - low memory
    val largeFile = KtEmbedResources.video_mp4.asStream()
    
    // As string (for text resources)
    val html = KtEmbedResources.index_html.asString()
}
```

## Technology Stack

- **Language**: Kotlin 2.2.21
- **Build System**: Gradle with KMP
- **Testing**: Kotest (already configured)
- **Targets**: JVM, Native (Linux, macOS, Windows), JS, WASM
- **Code Generation**: kotlin-gen library (available in libs.versions.toml)

## Development Workflow

1. Check available work: `bd ready --json`
2. Claim a task: `bd update <id> --status in_progress`
3. Implement and test
4. Mark complete: `bd close <id> --reason "Completed"`
5. Commit code + `.beads/issues.jsonl` together

## Next Steps

Start with the critical path items (Priority 0) in the Runtime Module:

1. `KtEmbed-kwz`: Define core API types
2. `KtEmbed-ol5`: Implement Base64 decoder
3. `KtEmbed-btg`: Platform-specific paths
4. Then move to InMemory and DiskCached strategies

Use `bd ready` to see which tasks are currently unblocked and ready to work on.
