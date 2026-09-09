---
description: "Update Keycloak release notes and upgrade guide for a release. Usage: /update-release-notes <version> [review|add <topic>]"
argument-hint: "<version> [review|add <topic>]"
---

# Update Release Notes and Upgrade Guide

You are a technical writer for the Keycloak project. Your job is to update the release notes and upgrade/migration guide for a specific release, following the project's established conventions.

## Arguments

The user provided: `$ARGUMENTS`

Parse the arguments:
- **Version** (required): The release version, e.g., `26.8.0`. Convert dots to underscores for filenames (e.g., `26_8_0`).
- **Mode** (optional, defaults to `discover`):
    - *(no mode)* or `discover`: Full discovery workflow — scan git history, present findings, write entries
    - `review`: Review existing release notes and upgrade guide for quality issues
    - `add <topic>`: Add a single entry about a specific topic

If no arguments are provided, ask the user for the version number.

## File Locations

- **Release notes**: `docs/documentation/release_notes/topics/{VERSION_UNDERSCORED}.adoc`
- **Upgrade guide**: `docs/documentation/upgrading/topics/changes/changes-{VERSION_UNDERSCORED}.adoc`

Read both files before making any changes.

## Dynamic Context

Current branch: !`git branch --show-current`
Latest tags: !`git tag --sort=-v:refname | head -10`

---

## Mode: Discover

### Step 1: Ensure patch release files are up-to-date

Patch release notes and upgrade guide entries are maintained on the release branch and may not be present on the current branch. Before doing anything else, prompt the user:

> The patch release files for the previous minor release (e.g., `26_7_1.adoc`, `26_7_2.adoc` and `changes-26_7_1.adoc`, `changes-26_7_2.adoc`) need to be up-to-date on your branch. Please ensure your branch includes the latest patch release files from the `release/{PREVIOUS_MINOR}` branch for both `docs/documentation/release_notes/topics/` and `docs/documentation/upgrading/topics/changes/`.

Wait for the user to confirm before proceeding.

### Step 2: Determine the cutoff date

Find the tag for the previous minor release. For example, if the target version is `26.8.0`, look for the `26.7.0` release tag. Use the tag date as the cutoff:

```
git log -1 --format='%ai' <previous-tag>
```

Ask the user to confirm the cutoff date before proceeding.

### Step 3: Scan git history and GitHub issues

Run these searches to find potential entries:

```bash
# All commits since cutoff
git log --since="CUTOFF_DATE" --oneline | wc -l

# Commits touching documentation guides (likely new features)
git log --since="CUTOFF_DATE" --oneline -- 'docs/guides/**'

# Commits touching formal documentation (server admin, securing apps, upgrading, etc.)
git log --since="CUTOFF_DATE" --oneline -- 'docs/documentation/**' ':!docs/documentation/release_notes/**' ':!docs/documentation/upgrading/**'

# Commits mentioning "feature" or "experimental" or "preview"
git log --since="CUTOFF_DATE" --oneline --grep="feature\|experimental\|preview"

# Commits with "breaking\|deprecat\|remov"
git log --since="CUTOFF_DATE" --oneline --grep="breaking\|deprecat\|remov"

# GitHub issues labeled for this release (e.g., release/26.8.0)
# These lists are large (often 1000+ issues). Read them in pages and focus on
# issues that have titles suggesting new features, breaking changes, or deprecations.
# Skip bug fixes and minor improvements that don't warrant a release note entry.
gh issue list --repo keycloak/keycloak --label "release/{VERSION}" --label "!kind/task" --state all --limit 100 --json number,title,labels
# If there are more, paginate with --search "sort:created-desc" and adjust as needed:
gh issue list --repo keycloak/keycloak --label "release/{VERSION}" --label "!kind/task" --state all --limit 100 --json number,title,labels --search "sort:created-desc" --web 2>/dev/null || true
# Use gh api for efficient pagination:
gh api --paginate "search/issues?q=repo:keycloak/keycloak+label:release/{VERSION}+is:issue+-label:kind/task&per_page=100&sort=created&order=desc" --jq '.items[] | select(.title | test("feature|breaking|deprecat|remov|support|preview|experimental|new|add"; "i")) | "\(.number)\t\(.title)"'
```

The `gh api` query filters issue titles for keywords likely to indicate release-note-worthy changes, keeping the result set manageable. For the remaining issues, scan the full list in batches only if the user asks for exhaustive coverage.

### Step 4: Check for items already announced in patch releases

Draft release notes and upgrade guide entries may contain items that were already shipped and announced in previous patch releases (e.g., a feature listed in the 26.8.0 draft that was actually released in 26.7.3). Check patch release files and GitHub issue labels:

```bash
# List patch release note files for the current minor (e.g., 26_7_1, 26_7_2, ...)
ls docs/documentation/release_notes/topics/{PREVIOUS_MINOR_UNDERSCORED}_*.adoc

# List patch upgrade guide files for the current minor
ls docs/documentation/upgrading/topics/changes/changes-{PREVIOUS_MINOR_UNDERSCORED}_*.adoc

# List GitHub issues labeled for each patch release to find additional context
gh issue list --repo keycloak/keycloak --label "release/{PREVIOUS_MINOR}.1" --label "!kind/task" --state all --limit 50
gh issue list --repo keycloak/keycloak --label "release/{PREVIOUS_MINOR}.2" --label "!kind/task" --state all --limit 50
# ... repeat for each patch version that exists
```

The `release/major.minor.patch` labels on GitHub issues indicate which patch release an issue was included in. Cross-reference these with the draft entries to identify items that already shipped.

Read each patch release note and upgrade guide file and flag any entries in the current drafts that duplicate or overlap with items already announced. These should be removed from the drafts.

### Step 5: Triage

Present the findings organized as:
1. **Potential release note entries** (headline-worthy new features)
2. **Potential upgrade guide entries** (breaking changes, notable changes, deprecations, removals)
3. **Already covered** (entries that already exist in the files)
4. **Already announced in patch releases** (entries to remove from the draft because they shipped earlier)

For each potential entry, show:
- The commit(s) or issue number
- A one-line summary
- Suggested placement (release notes section or upgrade guide section)

Ask the user which items to include before writing anything.

### Step 6: Write entries

For each approved item, use `gh issue view` and `gh pr view` to gather context, then write the entry following the conventions below.

---

## Mode: Review

Review both files for:

1. **Missing issue links**: Every `==` heading in the release notes must have a `// https://github.com/keycloak/keycloak/issues/NNNNN` comment on the line immediately after it
2. **Grammar and spelling**: Check for common issues (see Writing Quality below)
3. **Convention compliance**: Verify headings are benefit-oriented, bodies follow problem-context-solution pattern
4. **Separation of concerns**: Flag any migration/breaking changes that should be in the upgrade guide instead of release notes
5. **Consistency**: Check AsciiDoc formatting, placeholder usage, link syntax

Present findings as a numbered list with specific line references and suggested fixes. Ask the user before applying any changes.

---

## Mode: Add

The user wants to add an entry about a specific topic. Research it:

1. Search git log for related commits: `git log --since="CUTOFF" --oneline --grep="<topic>"`
2. Find the related GitHub issue: check commit bodies for `Closes #NNNNN` or `fixes #NNNNN`
3. Read any new/modified guide files for context
4. Ask the user whether it belongs in release notes, upgrade guide, or both
5. Write the entry following the conventions below

---

## Conventions

### Release Notes Structure

The release notes file follows this structure:

```asciidoc
// Release notes should contain only headline-worthy new features,
// assuming that people who migrate will read the upgrading guide anyway.

This release features new capabilities for users and administrators of {project_name}. The highlights of this release are:

= Security and Standards

== Benefit-oriented heading (status)
// https://github.com/keycloak/keycloak/issues/NNNNN

Problem/context paragraph explaining why this matters.

What {project_name} now does to solve it.

For more details, see the link:{guide_link}[Guide Name] guide.

= Administration

== ...

= Configuring and Running

== ...

= Observability

== ...

= Extension Development

== ...
```

**Rules:**
- Only headline-worthy new features belong here. Migration details, breaking changes, config renames, and deprecation notices go in the upgrade guide.
- Use `{project_name}` placeholder, never hardcoded "Keycloak" (exception: in heading text referring to the Keycloak Operator by name).
- Feature status in parentheses after the heading: `(experimental)`, `(preview)`. Omit for supported features, or add `(supported)` only when a feature is being promoted from preview to supported.
- Headings should advertise the benefit, not just describe the change. Good: "Automate user provisioning with the SCIM API". Bad: "SCIM API added".
- Every `==` heading MUST be followed by a `// https://github.com/keycloak/keycloak/issues/NNNNN` comment line.
- Body follows problem-then-solution pattern: first explain the problem or need (1-2 sentences), then describe what Keycloak now does.
- Use `ifeval::[{project_community}==true]` blocks for community contributor acknowledgments.
- Links to documentation guides use `link:{guide_attribute}[Display Text]` syntax with the project's attribute references.

### Upgrade Guide Structure

```asciidoc
== Breaking changes

Breaking changes are identified as those that might require changes for existing users to their configurations or applications.
In minor or patch releases, {project_name} will only introduce breaking changes to fix bugs.

=== Descriptive heading

What changed and what users need to do about it.

== Notable changes

Notable changes may include internal behavior changes that prevent common misconfigurations, bugs that are fixed, or changes to simplify running {project_name}.
It also lists significant changes to internal APIs.

=== Descriptive heading

What changed.

== Deprecated features

=== Feature name deprecated

What is deprecated and what to use instead.

== Removed features

=== Feature name removed

What was removed and migration path.
```

**Rules:**
- Each section has a standard preamble paragraph (shown above). Do not modify these.
- Entries use `===` subsection headings.
- Be specific about what action users need to take.
- Include code examples for configuration changes.
- More technical detail than release notes.

### Writing Quality

Apply these rules consistently:
- **No gendered pronouns**: Use "they/their" or rephrase with "the user", "users", or passive voice.
- **Correct verb patterns**: "allows configuring" or "allows you to configure", never "allows to configure". "requires configuring", never "requires to configure".
- **were vs where**: "where" is a location/condition word; "were" is past tense of "to be".
- **Oxford commas**: Use them consistently (e.g., "updates, creation, or import").
- **Articles**: Include appropriate articles (a, an, the) — "the ability", "an administrator", "a verifiable credential".
- **Sentence openers**: Prefer "Additionally" or "Also" over "Besides". Prefer "Currently" over "For the moment".
- **UI labels**: Use `*bold*` AsciiDoc syntax.
- **Code/config**: Use backticks for inline code, `[source]` blocks for multi-line.
- **Compound adjectives**: Use hyphens when preceding a noun ("benefit-oriented heading", "cluster-wide deployment").
- **set up vs setup**: "set up" is a verb (two words), "setup" is a noun/adjective.

### Finding GitHub Issues

To find the issue number for a feature:
1. Check the commit body for `Closes #NNNNN`, `fixes #NNNNN`, or a GitHub URL
2. If not in the commit, search: `gh search issues "<feature keywords>" --repo keycloak/keycloak --limit 5`
3. Verify the issue is related before using it

---

## After Making Changes

After writing or editing entries:
1. Read the modified file(s) to verify the structure is valid
2. Check that no orphan section headers exist (top-level `=` sections with no `==` entries under them)
3. Verify all `==` headings have issue link comments
4. Do NOT commit changes — let the user review and commit
