---
description: Migrate a single test class from the legacy Arquillian testsuite to the new test framework
---

## User Input

```text
$ARGUMENTS
```

You **MUST** have a test class name provided in the user input. If no test class name is provided, ask the user for one and stop.

## Instructions

Follow the full migration procedure in `tests/migration-util/ai-migration-tool/prompts/MIGRATION_PROMPT.md`.

Read all rule and spec files directly — do NOT delegate file reading to subagents, as they tend to summarize instead of returning full content, which loses critical details.
