package dev.ktool.embed

enum class OptimizationStrategy {
    /**
     * Represents an optimization strategy where operations prioritize minimizing memory usage.
     * When this strategy is used, resources or data are handled in a way that avoids loading
     * large chunks into memory, potentially favoring disk usage or streaming to prevent memory-related issues.
     *
     * Commonly used for handling large resources that exceed the defined memory cutoff threshold or
     * when an operation benefits from restricting memory consumption.
     */
    Memory,

    /**
     * Represents an optimization strategy focused on maximizing performance and minimizing latency.
     *
     * When using this strategy, operations prioritize speed by leveraging in-memory processing
     * and avoiding disk operations where possible. This is commonly used for resources
     * that are smaller than a predefined memory cutoff and can comfortably fit in memory.
     */
    Speed,
}