# @rollup/plugin-inject ChangeLog

## v4.0.4

_2021-12-28_

### Bugfixes

- fix: add types for `sourceMap` option (#1066)

## v4.0.3

_2021-10-19_

### Bugfixes

- fix: escape metacharacters in module name string (#897)
- fix: isReference check bug (#804)

### Updates

- chore: update dependencies (c1a0b07)

## v4.0.2

_2020-05-11_

### Updates

- chore: rollup v2 peerDep. (dupe for pub) (f0d8440)

## v4.0.1

_2020-02-01_

### Updates

- chore: update dependencies (73d8ae7)

## 3.0.2

- Fix bug with sourcemap usage

## 3.0.1

- Generate sourcemap when sourcemap enabled

## 3.0.0

- Remove node v6 from support
- Use modern js

## 2.1.0

- Update all dependencies ([#15](https://github.com/rollup/rollup-plugin-inject/pull/15))

## 2.0.0

- Work with all file extensions, not just `.js` (unless otherwise specified via `options.include` and `options.exclude`) ([#6](https://github.com/rollup/rollup-plugin-inject/pull/6))
- Allow `*` imports ([#9](https://github.com/rollup/rollup-plugin-inject/pull/9))
- Ignore replacements that are superseded (e.g. if `Buffer.isBuffer` is replaced, ignore `Buffer` replacement) ([#10](https://github.com/rollup/rollup-plugin-inject/pull/10))

## 1.4.1

- Return a `name`

## 1.4.0

- Use `string.search` instead of `regex.test` to avoid state-related mishaps ([#5](https://github.com/rollup/rollup-plugin-inject/issues/5))
- Prevent self-importing module bug

## 1.3.0

- Windows support ([#2](https://github.com/rollup/rollup-plugin-inject/issues/2))
- Node 0.12 support

## 1.2.0

- Generate sourcemaps by default

## 1.1.1

- Use `modules` option

## 1.1.0

- Handle shorthand properties

## 1.0.0

- First release
