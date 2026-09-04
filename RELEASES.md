# Keycloak Releases

## Versioning

Keycloak uses SemVer versioning (`<major>.<minor>.<patch>`).

The latest release and links to archived releases are available at https://www.keycloak.org/downloads. 

## Release Cadence

* **Minor releases** are released approximately 4 times per year. They include new features, enhancements, and bug
  fixes.
* **Major releases** occur every 2-3 years.
* **Patch releases** are made as needed to address critical bugs and security vulnerabilities.

For future release dates, refer to the [milestones in the GitHub repository](https://github.com/keycloak/keycloak/milestones). 

The project publishes nightly builds.  
Refer to https://www.keycloak.org/nightly/ for downloads.

## Backwards Compatibility

Keycloak delivers new features and enhancements in a backwards compatible way, making it seamless and easy to upgrade.

* Important bug fixes and security fixes might introduce non-opt-in breaking changes in any release.
* The aim for minor releases is to allow a seamless upgrade by making breaking changes opt-in. This should allow gradual roll out of feature changes and breaking changes.
* Major releases may introduce breaking changes that are not opt-in, and remove previously deprecated features or API versions.
* Backwards compatibility guarantees apply to supported features and APIs only. Preview and experimental features, as
  well as non-public APIs, may change at any time. See the [Feature Lifecycle](#feature-lifecycle) section below.

As any release can contain breaking changes or change internal behavior or APIs,
users should consult the [upgrading guide](https://www.keycloak.org/docs/latest/upgrading/) and conduct testing in a test environment before upgrading to any release.

## Security Releases

Security fixes are released as patch releases for the current minor release. Security advisories are published
in the release notes or [GitHub issues of type CVE](https://github.com/keycloak/keycloak/issues?q=is%3Aissue%20type%3Acve).

<!-- TODO: We currently only publish CVEs issue types, IMHO GitHub Security Advisories should be preferred. -->

## Branch and Tag Strategy

In the [GitHub repository](https://github.com/keycloak/keycloak), the following branching and tagging strategy is used:  

* Development happens on the `main` branch.
* Each minor release is tagged from `main` or from a release branch.
* After a minor release, a `release/<major>.<minor>` branch is created for subsequent patch releases.
* Patch releases are tagged from the corresponding release branch.
* Tags use the format `<major>.<minor>.<patch>` (no `v` prefix).

## Support and End of Life

The current minor release receives patch releases for critical bugs and security vulnerabilities. 
When a new minor release is published, the previous minor release is no longer supported.

Once a new major release is published, the last minor release of the previous major version receives 6 months of patch releases.

Users are expected to upgrade to each new minor release to continue receiving support.

## Feature Lifecycle

New capabilities in Keycloak follow a graduated lifecycle:

* **Experimental** - available for evaluation, not yet feature complete
* **Preview** - feature complete but not ready for production use yet. Seamless upgrades are not guaranteed
  <!-- Will change with https://github.com/keycloak/keycloak/issues/44551 -->
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
| Release notes | [Keycloak website](https://www.keycloak.org/docs/latest/release_notes/index.html) |

