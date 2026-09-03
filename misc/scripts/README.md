## dependency-report.sh

Search for a dependency in the Keycloak project to identify where it is used, and if there are multiple versions in use.

For example:

```
misc/scripts/dependency-report.sh org.twitter4j:twitter4j-core
```

Will output a report like:

```
===================================================================================================
Dependency tree for org.twitter4j:twitter4j-core
---------------------------------------------------------------------------------------------------
org.keycloak:keycloak-services:jar:999.0.0-SNAPSHOT
\- org.twitter4j:twitter4j-core:jar:4.1.2:compile
org.keycloak:keycloak-crypto-fips1402:jar:999.0.0-SNAPSHOT
\- org.keycloak:keycloak-services:jar:999.0.0-SNAPSHOT:compile
   \- org.twitter4j:twitter4j-core:jar:4.1.2:compile
...
```

# kcw

Provides a quick and convenient way of starting a Keycloak server, supporting a specific version, a locally built version,
or the nightly release.

Examples:

```
kcw dev start-dev
kcw nightly start --hostname=mykeycloak
```


# migrate-login-theme-pf5-to-pf6.mjs

Runs a login-theme migration pipeline for custom themes by combining PatternFly codemods with a
Keycloak-specific rewrite step.

What it does:

- Resolves your custom login theme path
- Runs PatternFly migration tools on a staged copy:
  - `@patternfly/pf-codemods` (`tokens-update`)
  - `@patternfly/css-vars-updater`
  - `@patternfly/class-name-updater`
- Rewrites `theme.properties` from `patternfly-v5` references to `patternfly-v6`
- Produces markdown/json reports in `misc/scripts/reports`
- Supports dry-run by default; applies changes only with `--apply`

Example:

```bash
# Dry-run (default)
node misc/scripts/migrate-login-theme-pf5-to-pf6.mjs \
  --theme-path /path/to/my-theme

# Apply changes and keep a backup copy
node misc/scripts/migrate-login-theme-pf5-to-pf6.mjs \
  --theme-path /path/to/my-theme \
  --apply \
  --fail-on-unresolved
```

Useful options:

- `--report-dir PATH` to customize report output location
- `--backup-dir PATH` to customize apply-mode backup location
- `--skip-pf-codemods`, `--skip-css-vars-updater`, `--skip-class-updater` for partial runs
- `--allow-builtin-theme` to run against built-in themes intentionally

The script prints `REPORT_READY:<path>` when done so the generated report can be consumed by automation.
