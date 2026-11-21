# Maintaining agentic workflows

Agentic workflows are written in Markdown with [GitHub Agentic Workflows (gh-aw)](https://github.github.com/gh-aw/) and compiled to regular GitHub Actions workflows.
Workflow-specific notes are kept as comments in the frontmatter of each workflow's `.md` file.

## Files

| File | Purpose | Edit by hand? |
|---|---|---|
| `.github/workflows/<name>.md` | The workflow: YAML frontmatter (triggers, permissions, tools, safe outputs) and the prompt | Yes |
| `.github/workflows/<name>.lock.yml` | Compiled GitHub Actions workflow that actually runs | No, generated |
| `.github/aw/actions-lock.json` | Pinned (SHA) versions of the actions used by the generated workflows | No, generated |

The `.md` file is the source of truth.
Always commit the `.md` and the `.lock.yml` together.

## Updating a workflow

1. Install the CLI once: `gh extension install github/gh-aw`.
2. Edit the `.md` file. This applies to both the frontmatter and the prompt.
3. Compile: `gh aw compile <name>` (for example `gh aw compile issue-triage`). Fix all errors, and try to leave no warnings.
4. Commit the `.md`, the `.lock.yml`, and any change in `.github/aw/actions-lock.json`.

Always recompile, even if you only changed the prompt.
The lock file stores a hash of the frontmatter and of the prompt, and the workflow's activation job fails with a stale lock file error when they don't match the `.md` file.

Use `gh aw validate <name>` to check a workflow without writing a lock file.

## Things to keep in mind

- **The agent is read-only.** All writes (labels, issue types, comments, ...) go through `safe-outputs` in the frontmatter, which restricts the allowed values and the number of calls. When the agent needs to do something new, add it there with the tightest `allowed` list possible.
- **Issue and PR content is untrusted input.** The prompt must keep telling the agent to ignore instructions embedded in it. Do not widen the tools or safe outputs of a workflow that reads outside contributors' text without a security review.
- **Pin versions.** Compiling pins the actions by SHA. Review changes to the pins in `actions-lock.json` like any other action update.

## Testing changes

- Review the compiled output: `git diff` of the `.lock.yml` should show only the intended changes plus, after a CLI upgrade, compiler changes.
- A workflow only runs via `workflow_dispatch` when its file is on the default branch, so it can't be dispatched from a pull request branch.
  To test on a fork, GitHub Actions must be enabled there and the workflow must be on the fork's default branch (revert that afterwards), and any repository guard in the workflow must allow the fork.
  Run it with `gh workflow run <name>.lock.yml -R <fork> -f issue_number=<n>`.
- Inspect a run with `gh run view <id> --log` or `gh aw logs`, and check the `safe_outputs` job to see which writes were applied or rejected.

## Upgrading gh-aw

Run `gh extension upgrade gh-aw`, then `gh aw upgrade`, which applies codemods, updates the action pins, and recompiles all workflows.
Commit the resulting changes in a separate pull request, as they touch every lock file.
