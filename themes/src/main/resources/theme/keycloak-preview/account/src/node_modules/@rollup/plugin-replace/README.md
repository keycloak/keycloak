[npm]: https://img.shields.io/npm/v/@rollup/plugin-replace
[npm-url]: https://www.npmjs.com/package/@rollup/plugin-replace
[size]: https://packagephobia.now.sh/badge?p=@rollup/plugin-replace
[size-url]: https://packagephobia.now.sh/result?p=@rollup/plugin-replace

[![npm][npm]][npm-url]
[![size][size]][size-url]
[![libera manifesto](https://img.shields.io/badge/libera-manifesto-lightgrey.svg)](https://liberamanifesto.com)

# @rollup/plugin-replace

🍣 A Rollup plugin which replaces strings in files while bundling.

## Requirements

This plugin requires an [LTS](https://github.com/nodejs/Release) Node version (v8.0.0+) and Rollup v1.20.0+.

## Install

Using npm:

```console
npm install @rollup/plugin-replace --save-dev
```

## Usage

Create a `rollup.config.js` [configuration file](https://www.rollupjs.org/guide/en/#configuration-files) and import the plugin:

```js
import replace from '@rollup/plugin-replace';

export default {
  input: 'src/index.js',
  output: {
    dir: 'output',
    format: 'cjs'
  },
  plugins: [replace({ __buildEnv__: 'production' })]
};
```

Then call `rollup` either via the [CLI](https://www.rollupjs.org/guide/en/#command-line-reference) or the [API](https://www.rollupjs.org/guide/en/#javascript-api).

The configuration above will replace every instance of `__buildEnv__` with `'production'` in any file included in the build. _Note: Values should always be strings. For complex values, use `JSON.stringify`._

Typically, `@rollup/plugin-replace` should be placed in `plugins` _before_ other plugins so that they may apply optimizations, such as dead code removal.

## Options

In addition to the properties and values specified for replacement, users may also specify the options below.

### `delimiters`

Type: `Array[...String, String]`<br>
Default: `['\b', '\b']`

Specifies the boundaries around which strings will be replaced. By default, delimiters are [word boundaries](https://www.regular-expressions.info/wordboundaries.html). See [Word Boundaries](#word-boundaries) below for more information.

### `exclude`

Type: `String` | `Array[...String]`<br>
Default: `null`

A [minimatch pattern](https://github.com/isaacs/minimatch), or array of patterns, which specifies the files in the build the plugin should _ignore_. By default no files are ignored.

### `include`

Type: `String` | `Array[...String]`<br>
Default: `null`

A [minimatch pattern](https://github.com/isaacs/minimatch), or array of patterns, which specifies the files in the build the plugin should operate on. By default all files are targeted.

## Word Boundaries

By default, values will only match if they are surrounded by _word boundaries_.

Consider the following options and build file:

```js
module.exports = {
  ...
  plugins: [replace({ changed: 'replaced' })]
};
```

```js
// file.js
console.log('changed');
console.log('unchanged');
```

The result would be:

```js
// file.js
console.log('replaced');
console.log('unchanged');
```

To ignore word boundaries and replace every instance of the string, wherever it may be, specify empty strings as delimiters:

```js
export default {
  ...
  plugins: [
    replace({
      changed: 'replaced',
      delimiters: ['', '']
    })
  ]
};
```

## Meta

[CONTRIBUTING](/.github/CONTRIBUTING.md)

[LICENSE (MIT)](/LICENSE)
