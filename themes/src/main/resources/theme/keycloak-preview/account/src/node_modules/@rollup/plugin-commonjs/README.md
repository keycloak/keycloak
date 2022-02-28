[npm]: https://img.shields.io/npm/v/@rollup/plugin-commonjs
[npm-url]: https://www.npmjs.com/package/@rollup/plugin-commonjs
[size]: https://packagephobia.now.sh/badge?p=@rollup/plugin-commonjs
[size-url]: https://packagephobia.now.sh/result?p=@rollup/plugin-commonjs

[![npm][npm]][npm-url]
[![size][size]][size-url]
[![libera manifesto](https://img.shields.io/badge/libera-manifesto-lightgrey.svg)](https://liberamanifesto.com)

# @rollup/plugin-commonjs

🍣 A Rollup plugin to convert CommonJS modules to ES6, so they can be included in a Rollup bundle

## Requirements

This plugin requires an [LTS](https://github.com/nodejs/Release) Node version (v8.0.0+) and Rollup v1.20.0+.

## Install

Using npm:

```bash
npm install @rollup/plugin-commonjs --save-dev
```

## Usage

Create a `rollup.config.js` [configuration file](https://www.rollupjs.org/guide/en/#configuration-files) and import the plugin:

```js
import commonjs from '@rollup/plugin-commonjs';

export default {
  input: 'src/index.js',
  output: {
    dir: 'output',
    format: 'cjs'
  },
  plugins: [commonjs()]
};
```

Then call `rollup` either via the [CLI](https://www.rollupjs.org/guide/en/#command-line-reference) or the [API](https://www.rollupjs.org/guide/en/#javascript-api).

## Options

### `dynamicRequireTargets`

Type: `String|Array[String]`<br>
Default: `[]`

Some modules contain dynamic `require` calls, or require modules that contain circular dependencies, which are not handled well by static imports.
Including those modules as `dynamicRequireTargets` will simulate a CommonJS (NodeJS-like) environment for them with support for dynamic and circular dependencies.

_Note: In extreme cases, this feature may result in some paths being rendered as absolute in the final bundle. The plugin tries to avoid exposing paths from the local machine, but if you are `dynamicRequirePaths` with paths that are far away from your project's folder, that may require replacing strings like `"/Users/John/Desktop/foo-project/"` -> `"/"`._

Example:

```js
commonjs({
  dynamicRequireTargets: [
    // include using a glob pattern (either a string or an array of strings)
    'node_modules/logform/*.js',

    // exclude files that are known to not be required dynamically, this allows for better optimizations
    '!node_modules/logform/index.js',
    '!node_modules/logform/format.js',
    '!node_modules/logform/levels.js',
    '!node_modules/logform/browser.js'
  ]
});
```

### `exclude`

Type: `String` | `Array[...String]`<br>
Default: `null`

A [minimatch pattern](https://github.com/isaacs/minimatch), or array of patterns, which specifies the files in the build the plugin should _ignore_. By default non-CommonJS modules are ignored.

### `include`

Type: `String` | `Array[...String]`<br>
Default: `null`

A [minimatch pattern](https://github.com/isaacs/minimatch), or array of patterns, which specifies the files in the build the plugin should operate on. By default CommonJS modules are targeted.

### `extensions`

Type: `Array[...String]`<br>
Default: `['.js']`

Search for extensions other than .js in the order specified.

### `ignoreGlobal`

Type: `Boolean`<br>
Default: `false`

If true, uses of `global` won't be dealt with by this plugin.

### `sourceMap`

Type: `Boolean`<br>
Default: `true`

If false, skips source map generation for CommonJS modules.

### `namedExports`

Type: `Object`<br>
Default: `null`

Explicitly specify unresolvable named exports.

This plugin will attempt to create named exports, where appropriate, so you can do this...

```js
// importer.js
import { named } from './exporter.js';

// exporter.js
module.exports = { named: 42 }; // or `exports.named = 42;`
```

...but that's not always possible:

```js
// importer.js
import { named } from 'my-lib';

// my-lib.js
var myLib = exports;
myLib.named = "you can't see me";
```

In those cases, you can specify custom named exports:

```js
commonjs({
  namedExports: {
    // left-hand side can be an absolute path, a path
    // relative to the current directory, or the name
    // of a module in node_modules
    'my-lib': ['named']
  }
});
```

### `ignore`

Type: `Array[...String | (String) => Boolean]`<br>
Default: `[]`

Sometimes you have to leave require statements unconverted. Pass an array containing the IDs or an `id => boolean` function. Only use this option if you know what you're doing!

## Using with @rollup/plugin-node-resolve

Since most CommonJS packages you are importing are probably depdenencies in `node_modules`, you may need to use [@rollup/plugin-node-resolve](https://github.com/rollup/plugins/tree/master/packages/node-resolve):

```js
// rollup.config.js
import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';

export default {
  input: 'main.js',
  output: {
    file: 'bundle.js',
    format: 'iife',
    name: 'MyModule'
  },
  plugins: [resolve(), commonjs()]
};
```

## Usage with symlinks

Symlinks are common in monorepos and are also created by the `npm link` command. Rollup with `@rollup/plugin-node-resolve` resolves modules to their real paths by default. So `include` and `exclude` paths should handle real paths rather than symlinked paths (e.g. `../common/node_modules/**` instead of `node_modules/**`). You may also use a regular expression for `include` that works regardless of base path. Try this:

```js
commonjs({
  include: /node_modules/
});
```

Whether symlinked module paths are [realpathed](http://man7.org/linux/man-pages/man3/realpath.3.html) or preserved depends on Rollup's `preserveSymlinks` setting, which is false by default, matching Node.js' default behavior. Setting `preserveSymlinks` to true in your Rollup config will cause `import` and `export` to match based on symlinked paths instead.

## Strict mode

ES modules are _always_ parsed in strict mode. That means that certain non-strict constructs (like octal literals) will be treated as syntax errors when Rollup parses modules that use them. Some older CommonJS modules depend on those constructs, and if you depend on them your bundle will blow up. There's basically nothing we can do about that.

Luckily, there is absolutely no good reason _not_ to use strict mode for everything — so the solution to this problem is to lobby the authors of those modules to update them.

## Meta

[CONTRIBUTING](/.github/CONTRIBUTING.md)

[LICENSE (MIT)](/LICENSE)
