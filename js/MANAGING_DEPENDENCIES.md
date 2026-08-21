# Managing JavaScript dependencies and CVEs

This guide covers day-to-day dependency and security work in the Keycloak `js/` workspace (pnpm monorepo). It complements [docs/bug-triage.md](../docs/bug-triage.md) (CVE issue triage).
All commands assume you are in the `js/` directory and have run `pnpm install` at least once.

## Overview

| Question | Tool / approach |
|----------|-----------------|
| Does the advisory apply to us? | Triage: vulnerable vs affected ([bug-triage](../docs/bug-triage.md)) |
| Is the package installed? | `pnpm why`, `pnpm list`, lockfile |
| What version is actually resolved? | `pnpm why`, `pnpm list -r`, `pnpm-lock.yaml` |
| What does the registry report? | `pnpm audit` |
| How do we fix a transitive CVE? | `pnpm-workspace.yaml` **overrides** (preferred), then `pnpm install` |

**Lockfile presence does not mean a package is shipped.** A dev-only or unused transitive dependency can appear in `pnpm-lock.yaml` but not in production JS bundles. Always verify impact before investing in a fix.

Automated dependency updates for `js/` are handled by [Dependabot](../.github/dependabot.yml) (npm, weekly). CVE issues are often opened automatically with labels `kind/cve`, `kind/bug`, and `status/triage`.

---

## CVE triage

Follow [docs/bug-triage.md — CVE reports on third-party libraries](../docs/bug-triage.md#cve-reports-on-third-party-libraries):

- **Vulnerable** — we use the code path described in the advisory (fix or mitigate).
- **Affected** — the dependency is present but the vulnerable code is not used or not reachable (document and close with rationale).
- **Not in lockfile / not in bundle** — may be a false positive for our UI surface; document findings on the issue.

---

## Viewing vulnerabilities with `pnpm audit`

```bash
cd js
pnpm audit
```

Useful variants:

```bash
# Only production dependencies (closer to what ships in apps)
pnpm audit --prod

# Only devDependencies
pnpm audit --dev

# Filter by minimum severity
pnpm audit --audit-level moderate

# Machine-readable output (CI, scripting)
pnpm audit --json

# Ignore a specific advisory when triaged (use GitHub advisory ID)
pnpm audit --ignore GHSA-xxxx-xxxx-xxxx

# Skip advisories with no fix available
pnpm audit --ignore-unfixable
```

Audit reports what the **registry** knows about versions in your lockfile. It does not know about Keycloak-specific usage, tree-shaking, or externals (e.g. `react` / `react-dom` are externalized in Admin and Account UI).

---

## Fixing vulnerabilities

### Step 1: Try automatic updates first

**Always start here.** Let pnpm attempt to update dependencies within their existing version ranges:

```bash
cd js
pnpm audit --fix update
pnpm audit --prod    # check if vulnerabilities remain
```

This updates the lockfile to use the latest compatible versions without modifying `package.json`. It's the safest approach and often resolves many CVEs automatically.

### Step 2: Identify if it's a direct or transitive dependency

If vulnerabilities remain after Step 1, determine whether the vulnerable package is direct or transitive:

```bash
# Check where the vulnerable package comes from
pnpm why <vulnerable-package-name>
```

**Direct dependency**: Listed in the `dependencies` or `devDependencies` of a workspace `package.json` (e.g., `js/apps/admin-ui/package.json`)

**Transitive dependency**: Not directly listed; it's pulled in by another package. The `pnpm why` output will show the dependency chain (e.g., `package-a > package-b > vulnerable-pkg`).

### Step 3a: Manual direct dependency bumps

If the vulnerability is in a **direct** dependency, manually bump the version in the relevant `package.json`:

```bash
# Edit the relevant package.json to update the version range
# Example: "vulnerable-pkg": "^1.0.0" → "^1.2.3"
pnpm install
pnpm audit --prod    # confirm the advisory is gone
# Run relevant UI tests / build
pnpm --filter @keycloak/keycloak-admin-ui build
```

### Step 3b: Explicit overrides (last resort for transitive dependencies)

Only use overrides when automatic updates and direct bumps don't work—typically for **transitive** dependencies (nested packages you don't directly control). Add or update an entry under `overrides` in [`pnpm-workspace.yaml`](pnpm-workspace.yaml):

```bash
cd js
# Edit pnpm-workspace.yaml, e.g.:
#   vulnerable-pkg@^1: ^1.2.3
pnpm install
pnpm audit --prod    # confirm the advisory is gone
```

**Why overrides are last resort:**
- They force specific versions across the entire workspace, potentially masking compatibility issues
- They require manual maintenance and periodic review
- They can become stale when upstream packages release proper fixes

**Reviewing existing overrides:** Periodically check if overrides are still needed by temporarily removing them, running `pnpm install && pnpm audit`, and testing. Document overrides with comments explaining why they're needed and when to revisit them.

Dependabot may open these PRs automatically for patch/minor updates in `js/`.

### `pnpm audit --fix` — use with care

```bash
pnpm audit --fix              # default method: override (adds overrides)
pnpm audit --fix --fix=update # tries to update lockfile versions
pnpm audit --fix -i           # interactive selection
```

| Method | Behavior | Recommendation |
|--------|----------|----------------|
| `override` (default) | Adds `pnpm.overrides` to **root `package.json`** | Avoid blind use here: we keep overrides in **`pnpm-workspace.yaml`**, not `package.json`. Copy any needed pins into `pnpm-workspace.yaml` manually. |
| `update` | Updates packages in the lockfile | Can be a starting point, but **review the full diff**; may bump unrelated packages. |

**Do not run `pnpm audit --fix` on a schedule in CI** or merge its output without review. Treat it as a hint, then apply a minimal, intentional change (override or targeted bump) and open a PR.

### What we generally do *not* do for CVEs

- Mass `pnpm audit --fix` on `main` without reviewing each change
- Closing CVE issues based on audit alone without checking bundle / usage
- Adding overrides to `package.json` when `pnpm-workspace.yaml` is the project convention

---

## Finding the effective version of a dependency

### `pnpm ls` returns nothing

Common causes in this repo:

1. **Wrong package name** — e.g. `lodash` vs `lodash-es`. Keycloak UI code depends on **`lodash-es`**, not `lodash`. `pnpm ls lodash` may show no matches while `lodash-es` is used widely.
2. **Missing `-r`** — from `js/`, use recursive listing for workspace packages: `pnpm ls <pkg> -r`.
3. **Package is only transitive** — use `pnpm why` instead of `pnpm ls`.

### Recommended commands

```bash
cd js

# Why is it installed? (best first step)
pnpm why lodash-es -r

# Which workspace packages declare it and at what version?
pnpm list lodash-es -r

# Only production dependency tree
pnpm list lodash-es -r --prod

# JSON for scripting
pnpm why lodash-es -r --json
```

### Lockfile (exact resolved versions)

```bash
# Importers and packages blocks list pinned versions
grep -E '^  (lodash-es|minimatch)@' pnpm-lock.yaml | head
```
---

## Release branches, backports, and cherry-picks

Keycloak maintains **release branches** (e.g. `release/26.x`) for patch releases. Dependency fixes often need to land on `main` **and** on supported release branches.

### Prefer fixing each branch separately (not cherry-picking the lockfile)

| Approach | When to use |
|----------|-------------|
| **Same change, regenerated lockfile per branch** | **Preferred** for CVE/dependency PRs. Check out the release branch, apply the same `pnpm-workspace.yaml` override or `package.json` bump, run `pnpm install`, audit, build/test, open a PR targeting that branch. |
| **Cherry-pick the commit** | Works when the commit is mostly `pnpm-workspace.yaml` / `package.json` and **little lockfile conflict**. Still run `pnpm install` on the release branch after cherry-pick to reconcile `pnpm-lock.yaml`. |
| **Cherry-pick lockfile-only diff** | **Avoid** — lockfiles diverge quickly between `main` and release branches; conflicts are frequent and error-prone. |

For **code** fixes (not lockfile-only), use [.github/scripts/pr-backport.sh](../.github/scripts/pr-backport.sh) and the `backport/<release branch>` label process described in [docs/bug-triage.md](../docs/bug-triage.md#backporting).

### Suggested workflow for a CVE fix

1. Triage the GitHub issue (`kind/cve`): vulnerable vs affected; bundle check if relevant.
2. On **`main`**: minimal override or bump → `pnpm install` → `pnpm audit --prod` → build/tests → PR with linked issue.
3. After merge, for each supported release that needs the fix:
   - Check out `release/x.y` (or use a backport PR).
   - Apply the **same logical fix** (override or version bump).
   - Run `pnpm install` on that branch (do not copy `pnpm-lock.yaml` from `main`).
   - Verify `pnpm audit` and UI build/tests on that branch.
4. Add `backport/<release branch>` on the issue when appropriate; do not add `release/x.y.z` labels manually.

Running `pnpm audit` only on `main` is **not** enough for supported releases — run audit (and the same fix) on each branch you ship from.

---

## Opening a PR for dependency / CVE fixes

Align with [CONTRIBUTING.md](../CONTRIBUTING.md):

1. **GitHub issue** for the CVE or dependency update (often already exists for `kind/cve`).
2. **One focused change per PR** — e.g. one override or one dependency bump, not unrelated dedupe/formatting.
3. **Commit message** references the issue; sign off with `--signoff`.
4. **PR description** should state:
   - Advisory ID(s) and severity
   - Vulnerable vs affected (and bundle check result if applicable)
   - What changed (`pnpm-workspace.yaml` override vs direct bump)
   - `pnpm audit` before/after (or `--json` snippet)
   - Tests run (which apps/build targets)
5. **Labels**: `area/dependencies`, team label (e.g. `team/ui` for `js/`), backport labels if needed.

Example PR title: `Bump minimatch override to address GHSA-xxxx`

---

## Quick reference

```bash
cd js

# Audit
pnpm audit
pnpm audit --prod
pnpm audit --json

# Versions / graph
pnpm why <package> -r
pnpm list <package> -r

# After override or bump
pnpm install
pnpm audit --prod

# Optional hygiene (review diff)
pnpm dedupe
pnpm dedupe --check
```

## See also

- [docs/bug-triage.md](../docs/bug-triage.md) — CVE and backport process
- [.github/dependabot.yml](../.github/dependabot.yml) — automated npm updates for `js/`
- [pnpm-workspace.yaml](pnpm-workspace.yaml) — workspace packages and overrides
