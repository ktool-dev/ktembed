# AGENTS.md

### Managing AI-Generated Planning Documents

AI assistants often create planning and design documents during development:

- PLAN.md, IMPLEMENTATION.md, ARCHITECTURE.md
- DESIGN.md, CODEBASE_SUMMARY.md, INTEGRATION_PLAN.md
- TESTING_GUIDE.md, TECHNICAL_DESIGN.md, and similar files

**Best Practice: Use a dedicated directory for these ephemeral files**

**Recommended approach:**

- Create a `history/` directory in the project root
- Store ALL AI-generated planning/design docs in `history/`
- Keep the repository root clean and focused on permanent project files
- Only access `history/` when explicitly asked to review past planning

**Example .gitignore entry (optional):**

```
# AI planning documents (ephemeral)
history/
```

**Benefits:**

- ✅ Clean repository root
- ✅ Clear separation between ephemeral and permanent documentation
- ✅ Easy to exclude from version control if desired
- ✅ Preserves planning history for archeological research
- ✅ Reduces noise when browsing the project

### Writing Tests

This project uses **Kotest** with **BDD-style specs**. Follow these patterns:

**Test structure:**

- Extend `BddSpec` from `dev.ktool.kotest`
- Use Given-When-Then structure with explicit labels
- Write descriptive test names in string literals

**Example:**

```kotlin
class MySpec : BddSpec({
    "descriptive test name" {
        Given
        val input = "test data"

        When
        val result = functionUnderTest(input)

        Then
        result shouldBe expected
    }
})
```

**Best practices:**

- Use Kotest matchers: `shouldBe`, `shouldNotBe`, `shouldContain`, `shouldMatch`, etc.
- For parameterized tests, use `row()` function with test data
- Extract helper functions to reduce boilerplate (see test files for examples)
- Use `FakeFileSystem` from Okio for file system testing
- Test edge cases: empty input, large data, unicode, special characters
- Verify lazy evaluation and caching behavior when relevant

**See examples:** `libraries/runtime/src/jvmTest/kotlin/dev/ktool/embed/`
