<!--
  This document is a "meta-roadmap" — it describes the process and points to GitHub Issues and
  Milestones as the living, authoritative roadmap rather than listing individual items inline.

  Rationale: Static roadmap files that enumerate planned features quickly become outdated and
  misleading. By keeping the process and pointers here, and the actual items in GitHub, the
  roadmap stays accurate without requiring manual synchronization. This is the same approach
  used by other graduated CNCF projects, notably:
    - containerd: https://github.com/containerd/containerd/blob/main/ROADMAP.md
    - Crossplane: https://github.com/crossplane/crossplane/blob/main/ROADMAP.md

  Maintaining this document:
    - The Release Cycle section references RELEASES.md — update that file when the release model changes.
    - Update the Feature Lifecycle section if docs/features.md evolves.
    - The Tracking Roadmap Items links should remain stable — they use GitHub query filters
      that automatically reflect current state.
    - The Roadmap Change Process section should stay aligned with GOVERNANCE.md.
-->

# Keycloak Roadmap

Keycloak uses GitHub Issues and Milestones as its living roadmap. This document explains how roadmap planning works
and how to get involved rather than listing individual items, which would quickly become outdated.

## Vision

Keycloak aims to be easy to use and lightweight, making it simple for application developers to secure modern
applications and services. For more details see [GOVERNANCE.md](GOVERNANCE.md#vision).

## Release Cycle

Keycloak releases minor versions approximately 4 times per year, with patch releases as needed for critical bugs and
security vulnerabilities. For full details on versioning, backwards compatibility, release artifacts, and the release
process see [RELEASES.md](RELEASES.md).

Each release has a corresponding GitHub Milestone that tracks the issues targeted for that release.

## Feature Lifecycle

New capabilities in Keycloak follow a graduated lifecycle:

* **Experimental** - available for evaluation, not yet feature complete
* **Preview** - feature complete and supported for production use, but seamless upgrades are not guaranteed
* **Supported** - fully supported with backwards compatibility and migration guarantees

For full details on expectations at each level see [docs/features.md](docs/features.md).

## Tracking Roadmap Items

The authoritative view of what is planned, in progress, and completed is on GitHub:

* **[Open features and enhancements](https://github.com/keycloak/keycloak/issues?q=is%3Aissue%20state%3Aopen%20(type%3Aenhancement%20OR%20type%3Afeature)%20sort%3Areactions-%2B1-desc)** - all proposed and accepted roadmap items, sorted by community interest
* **[Milestones](https://github.com/keycloak/keycloak/milestones)** - items grouped by target release
* **[Release Notes](https://www.keycloak.org/docs/latest/release_notes/index.html)** - completed releases with changelogs

Items assigned to a milestone are targeted for that release. Unassigned items are under consideration but not yet
scheduled. Milestones and priorities may shift based on community feedback and project needs.

## Roadmap Change Process

Anyone can propose a new feature or enhancement by
[opening a GitHub Issue](https://github.com/keycloak/keycloak/issues/new/choose). For larger changes, open a
[GitHub Discussion](https://github.com/keycloak/keycloak/discussions/categories/ideas) first to gather feedback
before implementation begins. Very large proposals may use a design document in the
[Keycloak Community repository](https://github.com/keycloak/keycloak-community/tree/main/design).

Maintainers review proposed items and decide on prioritization and milestone assignment. If consensus cannot be
reached, items are accepted with at least 2/3 maintainer approval as described in [GOVERNANCE.md](GOVERNANCE.md).

Keycloak is a community-driven project. Roadmap decisions are made based on the needs of the project and its users,
not directed by any single vendor.

## How to Get Involved

* **Upvote issues** with a :+1: reaction to signal interest and help steer the roadmap
* **Subscribe to issues** you care about to track progress and join the discussion
* **Propose ideas** through [GitHub Discussions](https://github.com/keycloak/keycloak/discussions/categories/ideas)
* **Try nightly releases** to provide early feedback on upcoming features
* **Contribute code** - see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines
* **Provide feedback** on experimental and preview features through their linked
  [GitHub Discussions](https://github.com/keycloak/keycloak/discussions/categories/feedback)
