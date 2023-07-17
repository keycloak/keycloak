# Checklist for merging PRs

Before merging a PR the PR should follow these rules:

* The PR does not have the label "hold"
* There is an associated GitHub Issue linked to the PR
  * A GitHub Issue is not required if the PR is not introducing a new feature or enhancement to Keycloak, or is resolving a bug where released versions of Keycloak are not affected
* There is sufficient test coverage
* Required documentation is included in the PR or an associated PR. Including updates to release notes and upgrade guide if applicable
* There are no unresolved negative reviews, or unresolved comments
* The PR does not contain changes not relevant to the GitHub Issue
* PRs has been reviewed by the relevant team, and approved by either a global maintainer or a team maintainer
* All required status checks have passed successfully
* If the PR is affected by unstable workflows or tests verify the issues are not introduced by the PR

Merging a PR:

* In most cases a PR should be merged by a team maintainer in the affected area
* Global maintainers can merge any PRs, but this is considered a fallback
* If a PR affects multiple areas it can be merged by any of the affected team maintainers as long as someone from each affected team has completed a review.
