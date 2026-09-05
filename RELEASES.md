<!--
  This document describes Keycloak's release process, versioning, and support expectations.
  It is a social contract between the project and its end users. Changes to these expectations
  should be discussed, socialized, and agreed upon by project leadership before being rolled out.

  Similar documents in other CNCF projects:
    - containerd: https://github.com/containerd/containerd/blob/main/RELEASES.md
    - OpenTelemetry: https://github.com/open-telemetry/community/blob/main/RELEASE.md
    - OpenTelemetry Java: https://github.com/open-telemetry/opentelemetry-java/blob/main/RELEASING.md

  Maintaining this document:
    - Update the Release Cadence section when the release model changes.
    - Update the Release Artifacts section when new artifacts are added or removed.
    - Keep the Support and End of Life section aligned with actual support commitments.
-->

# Keycloak Releases

## Versioning

Keycloak uses `<major>.<minor>.<patch>` versioning. Releases are tagged with the format `<major>.<minor>.<patch>`
in the [keycloak/keycloak](https://github.com/keycloak/keycloak) repository.

## Release Cadence

* **Minor releases** are released approximately 4 times per year. They include new features, enhancements, and bug
  fixes.
* **Major releases** occur every 2-3 years.
* **Patch releases** are made as needed to address critical bugs and security vulnerabilities.

## Backwards Compatibility

Keycloak delivers new features and enhancements in a backwards compatible way, making it seamless and easy to upgrade.

* Breaking changes in minor releases are **opt-in**: a new version of a feature or API can be introduced and
  explicitly enabled, but the current default version cannot change in a minor release. Deprecated versions are not
  removed until the next major release.
* Major releases may remove previously deprecated features or API versions.
* Backwards compatibility guarantees apply to supported features and APIs only. Preview and experimental features, as
  well as non-public APIs, may change at any time. See the [Feature Lifecycle](#feature-lifecycle) section below.

Users should consult the [upgrading guide](https://www.keycloak.org/docs/latest/upgrading/) before upgrading.

## Release Types

### Stable Releases

Stable releases are minor and patch releases that are supported for production use. Each stable release is tagged in
the repository and published with full release artifacts and release notes.

### Pre-release and Nightly Builds

Nightly builds are published from the `main` branch for early testing and feedback. They are not supported for
production use.

### Security Releases

Security fixes are released as patch releases for the current minor release. Security advisories are published
through [GitHub Security Advisories](https://github.com/keycloak/keycloak/security/advisories) or [GitHub issues of type CVE](https://github.com/keycloak/keycloak/issues?q=is%3Aissue%20type%3Acve).

<!-- TODO: We currently only publish CVEs issue types, IMHO GitHub Security Advisories should be preferred. -->

## Branch and Tag Strategy

* Development happens on the `main` branch.
* Each minor release is tagged from `main` or from a release branch.
* After a minor release, a `release/<major>.<minor>` branch is created for subsequent patch releases.
* Patch releases are tagged from the corresponding release branch.
* Tags use the format `<major>.<minor>.<patch>` (no `v` prefix).

## Support and End of Life

The current minor release receives patch releases for critical bugs and security vulnerabilities. When a new minor
release is published, the previous minor release is no longer supported.

Users are expected to upgrade to each new minor release to continue receiving support.

## Feature Lifecycle

New capabilities in Keycloak follow a graduated lifecycle:

* **Experimental** - available for evaluation, not yet feature complete
* **Preview** - feature complete and supported for production use, but seamless upgrades are not guaranteed
* **Supported** - fully supported with backwards compatibility and migration guarantees

For full details on expectations at each level see [docs/features.md](docs/features.md).

## Client Libraries

Client libraries (Admin Client, Authorization Client, JavaScript adapter) are released separately from the server.
The latest client library release supports all currently supported Keycloak server releases.

## Release Artifacts

Each Keycloak release publishes the following artifacts:

| Artifact | Repository / Registry |
|----------|----------------------|
| Server distribution (tar.gz, zip) | [GitHub Releases](https://github.com/keycloak/keycloak/releases), [Downloads page](https://www.keycloak.org/downloads) |
| Container image | [quay.io/keycloak/keycloak](https://quay.io/repository/keycloak/keycloak) |
| Operator | [quay.io/keycloak/keycloak-operator](https://quay.io/repository/keycloak/keycloak-operator), [OperatorHub](https://operatorhub.io/operator/keycloak-operator) |
| Kubernetes resources | [keycloak/keycloak-k8s-resources](https://github.com/keycloak/keycloak-k8s-resources) |
| Maven artifacts (client libraries, SPIs) | [Maven Central](https://central.sonatype.com/namespace/org.keycloak) |
| JavaScript adapter | [npm](https://www.npmjs.com/package/keycloak-js) |
| Release notes | [Keycloak website](https://www.keycloak.org/docs/latest/release_notes/index.html) |

Nightly builds use the `nightly` tag for container images and `999.0.0-SNAPSHOT` for Maven artifacts.

## Release Process

Release automation is maintained in the [keycloak-rel/keycloak-rel](https://github.com/keycloak-rel/keycloak-rel)
repository, separate from day-to-day development to sandbox release credentials and job queues. A
[testing fork](https://github.com/keycloak-rel-testing/keycloak-rel) is used to verify changes to the release
process before applying them to the main release repository.

Key workflows:

* **Release** - performs a regular Keycloak release from a release branch
* **Release Nightly** - performs a nightly scratch release, scheduled daily
* **Branch - Create Release** - creates release branches across all relevant repositories for a new minor release
* **Announce Release** - announces the release by updating the website
* **Re-spin Containers** - re-spins container images to incorporate base image security fixes without a full release

Releases are triggered and verified by maintainers. For details on how to run each workflow, see the
[keycloak-rel README](https://github.com/keycloak-rel/keycloak-rel#readme).

## Changes to This Document

Changes to release expectations are a social contract between the project and its end users. Proposed changes should
be discussed in [GitHub Discussions](https://github.com/keycloak/keycloak/discussions/categories/ideas) and agreed
upon by maintainers before being applied.
