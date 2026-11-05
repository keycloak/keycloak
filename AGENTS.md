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

Issue titles:

* For bug reports, name the problem, not the solution.
* For enhancement requests, describe the proposed change.

Issues for flaky tests get the `flaky-test` label and issues for flaky CI jobs get the `flaky-job` label. Add the `backport/<version>` labels for all supported versions the fix should be applied to.

When writing issue and pull request descriptions, put each paragraph on a single line, as GitHub renders single newlines in descriptions as line breaks. In Markdown files in the repository, put each sentence on its own line.

## Security vulnerabilities

Do not create public issues for security vulnerabilities. Follow the process in [SECURITY.md](SECURITY.md).

## Contributing code

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on pull requests, testing, documentation, and commit messages.

* Sign off every commit with `git commit --signoff`.
* Create pull requests as drafts, and only when asked.
* In pull request descriptions, state rationales, assumptions, and decisions. Do not repeat the issue or the code change. Put each paragraph on a single line.
* Run `./mvnw spotless:apply` before committing (see [CONTRIBUTING.md](CONTRIBUTING.md#spotless)); CI fails on formatting violations.
