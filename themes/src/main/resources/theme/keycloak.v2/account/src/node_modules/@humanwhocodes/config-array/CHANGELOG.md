# Changelog

### [0.9.5](https://www.github.com/humanwhocodes/config-array/compare/v0.9.4...v0.9.5) (2022-02-23)


### Bug Fixes

* Ensure dot directories are matched correctly ([6e8d180](https://www.github.com/humanwhocodes/config-array/commit/6e8d180f43cedf3c2072d8a1229470e9fafabf5b))
* preprocessConfig should have correct 'this' value ([9641540](https://www.github.com/humanwhocodes/config-array/commit/96415402cf0012ccf8e4af6c7b934dfc1a058986))

### [0.9.4](https://www.github.com/humanwhocodes/config-array/compare/v0.9.3...v0.9.4) (2022-01-27)


### Bug Fixes

* Negated patterns to work when files match ([398c811](https://www.github.com/humanwhocodes/config-array/commit/398c8119d359493dc7b82b40df4d92ea6528375f))

### [0.9.3](https://www.github.com/humanwhocodes/config-array/compare/v0.9.2...v0.9.3) (2022-01-26)


### Bug Fixes

* Make negated ignore patterns work like gitignore ([4ee8e99](https://www.github.com/humanwhocodes/config-array/commit/4ee8e998436e2c4538b06476e0bead8a44fe5a1b))

### [0.9.2](https://www.github.com/humanwhocodes/config-array/compare/v0.9.1...v0.9.2) (2021-11-02)


### Bug Fixes

* Object merging error by upgrading object-schema ([377d06d](https://www.github.com/humanwhocodes/config-array/commit/377d06d2a44d781b0bec70b3389c48b3d5a63f94))

### [0.9.1](https://www.github.com/humanwhocodes/config-array/compare/v0.9.0...v0.9.1) (2021-10-05)


### Bug Fixes

* Properly build package for release ([168155f](https://www.github.com/humanwhocodes/config-array/commit/168155f3fed91ab35566c452efd28debf8ec2b85))

## [0.9.0](https://www.github.com/humanwhocodes/config-array/compare/v0.8.0...v0.9.0) (2021-10-04)


### Features

* getConfig() now returns undefined when no configs match. ([a563b82](https://www.github.com/humanwhocodes/config-array/commit/a563b8255d4eb2bb7745314e3f00ef53792b343f))

## [0.8.0](https://www.github.com/humanwhocodes/config-array/compare/v0.7.0...v0.8.0) (2021-10-01)


### Features

* Add isIgnored() method ([343e5a0](https://www.github.com/humanwhocodes/config-array/commit/343e5a0a9e32028bfc6c0bf1ec0c6badf74f47f9))


### Bug Fixes

* Ensure global ignores are honored ([343e5a0](https://www.github.com/humanwhocodes/config-array/commit/343e5a0a9e32028bfc6c0bf1ec0c6badf74f47f9))

## [0.7.0](https://www.github.com/humanwhocodes/config-array/compare/v0.6.0...v0.7.0) (2021-09-24)


### Features

* Only object configs by default ([5645f24](https://www.github.com/humanwhocodes/config-array/commit/5645f241b2412a3263a02ef9e3a9bd19cc86035d))

## [0.6.0](https://www.github.com/humanwhocodes/config-array/compare/v0.5.0...v0.6.0) (2021-04-20)


### Features

* Add the normalizeSync() method ([3e347f9](https://www.github.com/humanwhocodes/config-array/commit/3e347f9d77c5ca2b15995e75ff7bc4fb96b7d66e))
* Allow async config functions ([a9def0f](https://www.github.com/humanwhocodes/config-array/commit/a9def0faf579c223349dfe08d2486756840538c3))
