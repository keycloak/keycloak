# Developing Features for Keycloak

Keycloak Features allow individual capabilities of Keycloak to have different support levels; additionally features
can be disabled when they are not needed.

In most cases all significant new capabilities added to Keycloak should be wrapped by a new experimental feature. The
feature is then graduated to preview when it is considered feature complete, and eventually graduates to supported
after feedback from the community.

When implementing a new feature in Keycloak it is highly recommended to define requirements and a minimum viable feature
initially, with incremental value added to the feature in later stages. Keeping the scope of the feature small allows
implementing the feature quicker and increases the quality as better test coverage and security can be provided.

## Feature level expectations

### Experimental

* Is not supported for production use-cases, only for evaluation purposes
* Does not have to be feature complete
* Good test coverage is required
* Documentation is required, and should be updated as the feature evolves
* Does not have to provide backwards compatibility for APIs, nor migrating support

### Preview

* Is supported for production use-cases, but does not guarantee seamless upgrades
* Must be feature complete (minimum viable feature)
* A review of test coverage is required prior to graduating a feature to preview
* A security review is required prior to graduating a feature to preview
* Review of any feedback or open issues must be resolved prior to graduating a feature to preview
* Should aim to provide backwards compatible APIs, and migration. However, this can be omitted if needed
* When a feature is graduated to preview it is expected the feature will graduate to supported in the next major or minor release

### Supported

* Is supported for production use-cases
* Must provide backwards compatible APIs and migration support if needed
* Review of any feedback or open issues must be resolved prior to graduating a feature to supported

# Breaking changes and removal of features

Breaking changes to supported features must be introduced through a new version of the feature. During a major release
life-cycle the default version can not be changed. However, there may be multiple supported versions. Previous versions
may be deprecated, but should remain the default. Migration to a newer version should be as simple as possible, where the ideal is a fully automated migration provided.

New major releases can remove features completely or older feature versions. In general this should only be done after
the feature or feature version having been deprecated for a minimum of one release (not including patch releases).

In the majority of cases preview features should follow the process for supported features. However, if breaking changes are required to preview features that can be made in the same version. Preview features can also be removed without deprecation, but ideally should be deprecated first.

Experimental features have no contract in terms of breaking changes. That means breaking changes can be introduced at any point, without new versions, deprecations, or any other warning. Additionally, no migration support is required.

# Documentation

## Experimental

Relevant documentation and guides for the feature should be clearly marked as experimental, and as under active 
development. This should include a link to the nightly release for those that want to evaluate the latest changes to 
the feature.

To encourage feedback there should be a link to a single GitHub Discussion (in the [Feedback category](https://github.com/keycloak/keycloak/discussions/categories/feedback)). The Discussion may optionally further link to GitHub Issues
relevant to the feature.

## Preview

Relevant documentation and guides for the feature should be clearly marked as preview, with a notice that the feature
can be used in production, but seamless upgrades are not guaranteed.

The link to the GitHub Discussion should remain for preview features.

## Supported

As a feature is graduated to supported, any experimental/preview notices should be removed, including the link to the GitHub
Discussions marked for feedback. The GitHub Discussion should be marked as closed.