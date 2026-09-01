# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

INFORM's fork of Keycloak. `origin` is `Inform-Software/keycloak`, `upstream` is `keycloak/keycloak`.

`main` is kept as a near-pure mirror of upstream. Only two files are fork-owned:
`.github/workflows/publish-backport-image.yml` and this one — everything else on `main` should
match `upstream/main`. Preserve that. Fork changes belong on backport branches and tags, not on
`main`: every edit to an upstream-owned file becomes a merge conflict on the next upstream sync,
so only touch one when the change is genuinely fork-specific and worth paying that cost forever.

Everything under `.github/copilot-instructions.md` applies here too (toolchain, focused-build
patterns, testing strategy); this file covers what that one doesn't.

## Backporting and publishing an image

The fork exists to ship patched Keycloak images. `.github/workflows/publish-backport-image.yml`
is dispatched manually with a git **tag**, builds Keycloak from source at that ref, and pushes
`ghcr.io/inform-software/keycloak:<tag>`.

The consumer is `inform-cloud-services/co-keycloak-container-green` — its `Dockerfile` takes
`KEYCLOAK_IMAGE` / `KEYCLOAK_VERSION` build args and its `.github/workflows/backport.yaml`
sets `keycloak_image: 'ghcr.io/inform-software/keycloak'`. It runs `kc.sh build` itself, so the
image published from here is deliberately the *unoptimized* base image.

### Case A — rebuilding an unchanged upstream release

Just dispatch the workflow with `tag: 26.6.6`. Nothing else. Every upstream release tag already
points at its own "Set version to X" commit (e.g. `26.6.6` → `85af03cdf8`) with the pom version
already stamped, so the pom, the tarball, the image tag and the `version` label all agree.

### Case B — fork changes on top of a release

A branch like `fix/26.6.4-fix` is upstream tag `26.6.4` plus cherry-picks, and its pom still reads
`26.6.4` — an image built from it would be indistinguishable from upstream's. Give it its own
identity before publishing:

```bash
git switch -c release/26.6.4-INFORM origin/fix/26.6.4-fix
./set-version.sh 26.6.4-INFORM-1     # rewrites all poms, project.version.npm, docs attrs, 4 package.json
git commit -am "Set version to 26.6.4-INFORM-1"
git tag 26.6.4-INFORM-1
git push origin 26.6.4-INFORM-1
```

Then dispatch with `tag: 26.6.4-INFORM-1` and no `image-tag`. Keep the version-bump commit a
**leaf** — tagged, never merged back — which is what upstream does (release branches stay at
`999.0.0-SNAPSHOT`). Naming convention: reuse upstream's version string when the build is
identical to upstream, otherwise add an `-INFORM-n` postfix (see tags `22.0.1-INFORM-1`,
`22.0.3-INFORM-1`).

Everything downstream follows automatically: `dist.archive.file.version` and
`dist.archive.dir.version` default to `${project.version}` (`pom.xml`), so you get
`keycloak-26.6.4-INFORM-1.tar.gz` with a matching inner directory, which is what the workflow's
`cp` glob and the Dockerfile's `mv /tmp/keycloak/keycloak-*` both expect.

### Constraints and traps in the publish workflow

- **Tags only**, though nothing enforces it. A branch name containing `/` is illegal in a Docker
  tag and fails at the push step *after* the ~20-minute build; a slash-free branch name is worse
  still — it succeeds and publishes a mutable, mislabelled image.
- **`linux/amd64` only** — a consequence of the `ubuntu-latest` runner, not an explicit pin: no
  `platforms:` key is set, and none is wanted, because the consumer pins
  `ARG IMAGE_PLATFORM=linux/amd64`.
- **Local composite actions resolve against the checked-out tag, not `main`.** This is
  load-bearing: `java-setup`'s default JDK is 17 on 22.0.2+, 21 on 26.0–26.5, 25 on 26.6+, so
  each tag builds on its own era's JDK for free. The flip side: `java-setup` does not exist
  before 22.0.2, so `22.0.1-INFORM-1` cannot be dispatched as-is — it dies at `Setup Java` with
  "Can't find action.yml".
- For the same reason, **do not replace the inlined build steps with
  `uses: ./.github/actions/build-keycloak`** and **do not add `pnpm-store-cache`**. The former
  differs per tag (22.x's copy uses the retired `actions/upload-artifact@v3`); the latter does not
  exist before 26.0.6, and a missing local action is a fatal step error. There is a comment in the
  workflow saying so — leave it there.
- `set-version.sh` seds `ENV KEYCLOAK_VERSION` in `quarkus/container/Dockerfile`. That became a
  no-op in 26.2.0, when the Dockerfile switched to `ARG KEYCLOAK_VERSION=999.0.0-SNAPSHOT`; on
  22.x through 26.1.x the `ENV` line is still there and the sed does fire. Either way it does not
  matter here, because the workflow passes `--build-arg KEYCLOAK_VERSION` explicitly. Left alone
  on purpose — it is an upstream-owned file.

## Building

Use `./mvnw`, never system Maven. JDK 17, 21 or 25 (compiler release is 17).

| Goal                          | Command                                                                     |
|-------------------------------|-----------------------------------------------------------------------------|
| Everything, no tests          | `./mvnw clean install -DskipTests`                                          |
| Server distribution only      | `./mvnw -pl quarkus/deployment,quarkus/dist -am -DskipTests clean install`   |
| One module, fast              | `./mvnw install -Pdistribution -DskipTests -DskipExamples -DskipTestsuite -DskipAdapters -DskipDocs -pl <module> -am` |
| Formatting check / apply      | `./mvnw -Pdocs,distribution,operator spotless:check` (`spotless:apply`)      |
| Operator (excluded by default)| add `-Poperator`                                                            |

The distribution lands in `quarkus/dist/target/`. Add `-DskipProtoLock=true` if the
proto-schema-compatibility check fails behind a proxy.

CI's canonical recipe is `.github/actions/build-keycloak` — the licenses-processor plugin must be
installed first or the main build emits warnings:

```bash
./mvnw install -Pdistribution -am -pl distribution/maven-plugins/licenses-processor
./mvnw install dependency:resolve -V -e -DskipTests -DskipExamples \
  -DexcludeGroupIds=org.keycloak -Dsilent=true -DcommitProtoLockChanges=true
```

## Testing

New tests go under `tests/` (Keycloak Test Framework). `testsuite/` is deprecated — see
`testsuite/DEPRECATED.md`; do not add tests there.

- Single test: `./mvnw test -pl tests/base -Dtest=MyProviderTest`
- Which modules have unit tests: `.github/scripts/find-modules-with-unit-tests.sh`
- Guides: `tests/docs/README.md`, `test-framework/docs/`, `docs/tests.md`, `docs/tests-db.md`

## Frontend

`js/` is a PNPM workspace (Node 24+). `pnpm install` from `js/`;
`pnpm -C apps/admin-ui lint`; `pnpm build` for the whole workspace. The Java build drives these
via frontend-maven-plugin, so a plain `./mvnw install` also builds the UIs.

## Layout

Server runtime is `quarkus/` (`quarkus/server` runs the Quarkus augmentation that pre-bakes
`kc.sh build`; `quarkus/dist` assembles the tar.gz/zip). Domain logic is in `services/`, `model/`,
`server-spi*`, `core/`. The container build context is `quarkus/container/` — see its `README.md`
for the three ways to build the image locally, and `ubi-null.sh`, which carves the minimal UBI
rootfs (runtime JDK is **21**, even though CI builds with 25).

## Conventions

Conventional-commit subjects (`chore:`, `fix:`, `feat:`) and DCO sign-off (`git commit -s`) — see
the fork's own commits. Upstream expects every PR to map to an issue and to keep a focused scope.

## Finding things in the code

This is a large monorepo; the fastest route to most questions is the table below rather than a
repo-wide grep.

| Looking for                                   | Start here                                                                 |
|-----------------------------------------------|----------------------------------------------------------------------------|
| An Admin REST endpoint                        | `services/src/main/java/org/keycloak/services/resources/admin/`             |
| An Account REST endpoint                      | `services/.../services/resources/account/`                                 |
| Login / registration / logout HTTP handling   | `services/.../services/resources/LoginActionsService.java`, `RealmsResource.java` |
| Authentication flow engine                    | `services/.../authentication/` (`AuthenticationProcessor`, `DefaultAuthenticationFlow`) |
| A specific authenticator or required action   | `services/.../authentication/authenticators/`, `.../requiredactions/`      |
| OIDC / SAML endpoints, token issuance, mappers| `services/.../protocol/oidc/`, `.../protocol/saml/`                        |
| Identity brokering / social login             | `services/.../broker/` (`oidc`, `saml`, `oauth`, `provider`)               |
| A `kc.sh` / `--option` config option          | `quarkus/config-api/src/main/java/org/keycloak/config/*Options.java` (24 files), wired in `quarkus/runtime/.../configuration/` |
| A DB table or column                          | entity in `model/jpa/src/main/java/org/keycloak/models/jpa/entities/`, migration in `model/jpa/src/main/resources/META-INF/jpa-changelog-*.xml` |
| Caching / user + auth sessions                | `model/infinispan/`                                                        |
| LDAP / Kerberos / SSSD user federation        | `federation/{ldap,kerberos,sssd,ipatuura}/`                                |
| A provider interface (SPI) to implement       | `server-spi/`, `server-spi-private/` under `org/keycloak/{models,storage,provider,sessions,credential,userprofile}` |
| Every implementation of an SPI                | grep `META-INF/services/<factory FQN>` files, not `implements` — that is how providers are discovered (`services/src/main/resources/META-INF/services/` alone holds 95) |
| A string shown on a login page                | `themes/src/main/resources/theme/base/login/messages/messages_en.properties`, then the matching `*.ftl` |
| Admin console UI                              | `js/apps/admin-ui/src/`, split by console section (`clients`, `realm-settings`, `user-federation`, `identity-providers`, `authentication`, `events`, `sessions`, `organizations`) |
| Typed admin REST client (TS)                  | `js/libs/keycloak-admin-client/`                                           |
| Crypto / FIPS                                 | `crypto/{default,fips1402,elytron}/`                                       |
| Authorization services (UMA, policies)        | `authz/`, `services/.../authorization/`                                    |

Module roles at a glance: `services/` holds nearly all runtime logic; `server-spi*` defines the
plug-in contracts; `model/` is persistence and caching; `core/` and `common/` are shared
value types and utilities; `quarkus/` is the runtime wiring (`runtime` + `deployment` +
`config-api`, then `server` → `dist` → `container`); `themes/` is server-rendered UI; `js/` is
the React consoles.
