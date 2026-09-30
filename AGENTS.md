# Instructions for AI Agents

## Creating Issues

Issues must be created using the repository's issue templates. Do not create blank issues.

Read the issue templates in `.github/ISSUE_TEMPLATE/` to understand the available types, required fields, and options. Choose the right template based on its `name` and `description` fields. Do not use the Task or Milestone templates -- those are for maintainers.

When creating an issue via the GitHub CLI or API, read the chosen template and format the issue body so that each field appears as a `### <label>` markdown header followed by the response. Fill in all required fields. For dropdowns, use exactly one of the values listed in the template's `options`.

Example for a bug report's area field (from the `Area` dropdown in `bug.yml`):

```
### Area

<one of the options from the template>
```

## Security vulnerabilities

Do not create public issues for security vulnerabilities. Follow the process in [SECURITY.md](SECURITY.md).

## Contributing code

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on pull requests, testing, documentation, and commit messages.

## Reviewing PRs

When reviewing a pull request, produce a single consolidated review with these sections:

### Structure

1. **Summary** — A short paragraph describing what the PR does, the relevant spec/RFC, and an overall assessment.
2. **Findings by Severity** — A single numbered list of findings, most severe first. Each finding must include:
    - A bold one-line title
    - The file and line reference (e.g., `ClassName.java:123`)
    - Inline code blocks showing the relevant code from the diff
    - A clear explanation of the problem and a concrete suggestion for fixing it
    - For borderline items, add a *(design consideration)* or *(minor)* tag to the title
3. **Merge recommendation** — One line stating which findings should block merge vs. which are follow-up concerns.
4. **What looks good** — Bullet list of specific things the PR does well (spec compliance, test coverage, design choices, pattern consistency, reuse of existing infrastructure, etc.).

### Guidelines

- Start the review with an attribution line: `Reviewed by <agent> <model-name>` (e.g., `Reviewed by Claude Opus 4.6`).
- Consolidate findings into one list — do not split across multiple sections or repeat findings.
- Always include code snippets from the diff to ground each finding. Do not describe code without showing it.
- Format everything as GitHub-flavored markdown so it can be pasted directly into a PR comment.
- When asked, write the review to a `.md` file for easy download.
