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
