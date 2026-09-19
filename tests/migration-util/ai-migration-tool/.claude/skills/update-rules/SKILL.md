---
description: Verify and update migration specs and rule files against the actual codebase
---

## User Input

```text
$ARGUMENTS
```

## Instructions

Follow the full verification procedure in `tests/migration-util/ai-migration-tool/prompts/UPDATE_RULES_PROMPT.md`.

Read all rule and spec files directly — do NOT delegate file reading to subagents, as they tend to summarize instead of returning full content, which loses critical details.
