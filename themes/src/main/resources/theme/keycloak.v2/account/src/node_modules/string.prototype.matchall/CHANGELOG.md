# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v4.0.7](https://github.com/ljharb/String.prototype.matchAll/compare/v4.0.6...v4.0.7) - 2022-03-18

### Commits

- [actions] reuse common workflows [`798d359`](https://github.com/ljharb/String.prototype.matchAll/commit/798d359ac36a7543ab4cb4b14f7544b7687d6a9b)
- [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `aud`, `auto-changelog`, `es5-shim`, `object-inspect`, `tape` [`8733fa4`](https://github.com/ljharb/String.prototype.matchAll/commit/8733fa45b7bf5115cb08ba6d4866b14f6b637919)
- [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `@es-shims/api`, `safe-publish-latest`, `tape` [`2f0ac7d`](https://github.com/ljharb/String.prototype.matchAll/commit/2f0ac7de6a6b585f1bd6a32c0426d27926366334)
- [actions] update codecov uploader [`40ea9ce`](https://github.com/ljharb/String.prototype.matchAll/commit/40ea9ce1b5a4f8bfbe5637e4edb63e693ff78020)
- [Robustness] use call-bound `indexOf` [`b035fdc`](https://github.com/ljharb/String.prototype.matchAll/commit/b035fdcd6b65263b41ad24786cde4217083c13db)
- [Deps] update `has-symbols`, `regexp.prototype.flags` [`95768f2`](https://github.com/ljharb/String.prototype.matchAll/commit/95768f258a8d30630f56ec862b2e356c980f57c6)

## [v4.0.6](https://github.com/ljharb/String.prototype.matchAll/compare/v4.0.5...v4.0.6) - 2021-10-04

### Commits

- [Refactor] use `CreateRegExpStringIterator` from `es-abstract` [`5c2cf33`](https://github.com/ljharb/String.prototype.matchAll/commit/5c2cf338f3568d696c978f9e1e51903d229b5fef)
- [patch] remove unused helpers [`280f47e`](https://github.com/ljharb/String.prototype.matchAll/commit/280f47ee3a36e7830e37192b8c6a958026f779f6)
- [meta] add `auto-changelog` [`2d26eda`](https://github.com/ljharb/String.prototype.matchAll/commit/2d26eda3a451cd42a4fea8028f1fb237f330bcee)
- [Deps] update `es-abstract` [`857c8b0`](https://github.com/ljharb/String.prototype.matchAll/commit/857c8b032e6e9cde53af54aec7d91d75cb0cd262)
- [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `@es-shims/api`, `es5-shim`, `object-inspect`, `object.entries`, `tape` [`9349ea2`](https://github.com/ljharb/String.prototype.matchAll/commit/9349ea2382066187aae146a2c1cf456c5473cf32)

## [v4.0.5](https://github.com/ljharb/String.prototype.matchAll/compare/v4.0.4...v4.0.5) - 2021-05-25

### Commits

- [actions] use `node/install` instead of `node/run`; use `codecov` action [`a6a7af2`](https://github.com/ljharb/String.prototype.matchAll/commit/a6a7af2304add692d429a8a5a4f44914d5b4a9b6)
- [readme] update badges, spec year [`9532ccc`](https://github.com/ljharb/String.prototype.matchAll/commit/9532ccc593cd686232717287b94be9abf497198f)
- [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `aud`, `object-inspect` [`8ea3e71`](https://github.com/ljharb/String.prototype.matchAll/commit/8ea3e71d40716fa857743a315df243270e53a49e)
- [Deps] update `es-abstract`, `has-symbols` [`e906e75`](https://github.com/ljharb/String.prototype.matchAll/commit/e906e7583b3f1e4efc3fc1f87fb0bd5742191a5e)
- [Dev Deps] update `eslint`, `tape` [`fcf2270`](https://github.com/ljharb/String.prototype.matchAll/commit/fcf227050a83bac350c7e451f57151da80783523)
- [actions] update workflows [`ba642c5`](https://github.com/ljharb/String.prototype.matchAll/commit/ba642c573af408f6106666d9a3f9261b1c99b505)
- [Refactor] use `get-intrinsic` directly [`fca987f`](https://github.com/ljharb/String.prototype.matchAll/commit/fca987f8b1abd375d752d081ee4516a6b8f6c912)
- [meta] use `prepublishOnly` script for npm 7+ [`4c5ba45`](https://github.com/ljharb/String.prototype.matchAll/commit/4c5ba452acf2ae5af9792a58f01e7cb9b839a5c3)
- [Deps] update `es-abstract` [`39d34df`](https://github.com/ljharb/String.prototype.matchAll/commit/39d34df9a615fe723411dd3fc91be49be6521ef1)

<!-- auto-changelog-above -->

4.0.4 / 2021-02-21
==================
  * [readme] fix repo URLs; remove travis badge
  * [meta] gitignore coverage output
  * [Deps] update `call-bind`, `es-abstract`, `internal-slot`, `regexp.prototype.flags`, `side-channel`
  * [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `aud`, `es5-shim`, `functions-have-names`, `object-inspect`, `object.entries`, `tape`
  * [Tests] increase coverage
  * [actions] update workflows

4.0.3 / 2020-11-19
==================
  * [meta] do not publish github action workflow files
  * [Deps] update `es-abstract`, `side-channel`; use `call-bind` where applicable; remove `function-bind`
  * [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `aud`, `es5-shim`, `es6-shim`, `functions-have-names`, `object-inspect`, `object.assign`, `object.entries`, `tape`
  * [actions] add "Allow Edits" workflow
  * [actions] switch Automatic Rebase workflow to `pull_request_target` event
  * [Tests] migrate tests to Github Actions
  * [Tests] run `nyc` on all tests
  * [Tests] run `es-shim-api` in postlint; use `tape` runner
  * [Tests] only audit prod deps

4.0.2 / 2019-12-22
==================
  * [Refactor] use `internal-slot`
  * [Refactor] use `side-channel` instead of "hidden" helper
  * [Deps] update `es-abstract`, `internal-slot`, `regexp.prototype.flags`, `side-channel`
  * [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `tape`

4.0.1 / 2019-12-13
==================
  * [Refactor] use split-up `es-abstract` (61% bundle size decrease)
  * [Fix] fix error message: matchAll requires *global*
  * [Deps] update `es-abstract`, `has-symbols`
  * [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `functions-have-names`, `object-inspect`, `evalmd`, `object.entries`; add `safe-publish-latest`
  * [meta] add `funding` field
  * [Tests] use shared travis-ci configs
  * [actions] add automatic rebasing / merge commit blocking

4.0.0 / 2019-10-03
==================
  * [Breaking] throw on non-global/nullish flags
  * [Deps] update `es-abstract`

3.0.2 / 2019-10-02
==================
  * [Fix] ensure that `flagsGetter` is only used when there is no `flags` property on the regex
  * [Fix] `RegExp.prototype[Symbol.matchAll]`: ToString the `flags` property
  * [Refactor] provide a consistent way to determine the polyfill for `RegExp.prototype[Symbol.matchAll]`
  * [meta] create FUNDING.yml
  * [Deps] update `es-abstract`
  * [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `evalmd`, `functions-have-names`, `es5-shim`, `es6-shim`, `object.entries`, `tape`
  * [Tests] up to `node` `v12.11`, `v11.15`, `v10.16`, `v8.16`, `v6.17`
  * [Tests] use `functions-have-names`
  * [Tests] bump audit level, due to https://github.com/reggi/evalmd/issues/13
  * [Tests] use `npx aud` instead of `npm audit` with hoops

3.0.1 / 2018-12-11
==================
  * [Fix] update spec to follow committee feedback
  * [Deps] update `define-properties`
  * [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `es5-shim`, `es6-shim`, `tape`
  * [Tests] use `npm audit` instead of `nsp`
  * [Tests] up to `node` `v11.4`, `v10.14`, `v8.14`, `v6.15`

3.0.0 / 2018-05-31
==================
  * [Breaking] update to match latest spec
  * [Deps] update `es-abstract`
  * [Dev Deps] update `eslint`, `nsp`, `object-inspect`, `tape`
  * [Tests] up to `node` `v10.3`, `v9.11`, `v8.11`, `v6.14`, `v4.9`
  * [Tests] regexes now have a "groups" property in ES2018
  * [Tests] run evalmd in prelint

2.0.0 / 2018-01-24
==================
  * [Breaking] change to handle nonmatching regexes
  * [Breaking] non-regex arguments that are thus coerced to RegExp now get the global flag
  * [Deps] update `es-abstract`, `regexp.prototype.flags`
  * [Dev Deps] update `es5-shim`, `eslint`, `object.assign`
  * [Tests] up to `node` `v9.4`, `v8.9`, `v6.12`; pin included builds to LTS
  * [Tests] improve and correct tests and failure messages

1.0.0 / 2017-09-28
==================
  * Initial release
