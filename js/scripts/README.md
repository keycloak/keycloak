# JS scripts

For CVE handling, `pnpm audit`, overrides, and release-branch workflow, see [MANAGING_DEPENDENCIES.md](../MANAGING_DEPENDENCIES.md).

## `check-bundle-dependency.mjs`

Checks whether an npm package version appears in the pnpm lockfile and/or production JS bundles.

```bash
cd js
pnpm install
pnpm check-bundle lodash 4.17.23
```

- **Lockfile** — installed anywhere in the workspace (may be dev-only).
- **Bundle** — included in the Rollup module graph for shipped UI assets (in-memory build by default).

`themes-vendor` only bundles explicit vendor entries (React, etc.). Admin and Account UI externalize `react` / `react-dom`; those appear as externals, not inside chunks.

Options: `--app`, `--lockfile-only`, `--skip-build`, `--semver`, `--strict-name`, `--json`, `--verbose`. See `--help`.
