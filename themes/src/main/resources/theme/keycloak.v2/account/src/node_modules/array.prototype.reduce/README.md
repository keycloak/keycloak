# array.prototype.reduce <sup>[![Version Badge][npm-version-svg]][package-url]</sup>

[![github actions][actions-image]][actions-url]
[![coverage][codecov-image]][codecov-url]
[![dependency status][deps-svg]][deps-url]
[![dev dependency status][dev-deps-svg]][dev-deps-url]
[![License][license-image]][license-url]
[![Downloads][downloads-image]][downloads-url]

[![npm badge][npm-badge-png]][package-url]

An ES5 spec-compliant `Array.prototype.reduce` shim/polyfill/replacement that works as far down as ES3.

This package implements the [es-shim API](https://github.com/es-shims/api) interface. It works in an ES3-supported environment and complies with the [spec](https://www.ecma-international.org/ecma-262/5.1/).

Because `Array.prototype.reduce` depends on a receiver (the “this” value), the main export takes the array to operate on as the first argument.

## Example

```js
var reduce = require('array.prototype.reduce');
var assert = require('assert');

assert.equal(reduce([1, 2, 3], function (prev, x) { return prev + x; }), 6);
assert.equal(reduce([1, 2, 3], function (prev, x) { return prev + x; }, 1), 7);
```

```js
var reduce = require('array.prototype.reduce');
var assert = require('assert');
/* when Array#reduce is not present */
delete Array.prototype.reduce;
var shimmed = reduce.shim();
assert.equal(shimmed, reduce.getPolyfill());
var arr = [1, 2, 3];
var sum = function (a, b) { return a + b; };
assert.equal(arr.reduce(sum), reduce(arr, sum));
assert.equal(arr.reduce(sum), 6);
assert.equal(arr.reduce(sum, 1), 7);
```

```js
var reduce = require('array.prototype.reduce');
var assert = require('assert');
/* when Array#reduce is present */
var shimmed = reduce.shim();
assert.equal(shimmed, Array.prototype.reduce);
assert.equal(arr.reduce(sum), reduce(arr, sum));
```

## Tests
Simply clone the repo, `npm install`, and run `npm test`

[package-url]: https://npmjs.org/package/array.prototype.reduce
[npm-version-svg]: https://versionbadg.es/es-shims/Array.prototype.reduce.svg
[deps-svg]: https://david-dm.org/es-shims/Array.prototype.reduce.svg
[deps-url]: https://david-dm.org/es-shims/Array.prototype.reduce
[dev-deps-svg]: https://david-dm.org/es-shims/Array.prototype.reduce/dev-status.svg
[dev-deps-url]: https://david-dm.org/es-shims/Array.prototype.reduce#info=devDependencies
[npm-badge-png]: https://nodei.co/npm/array.prototype.reduce.png?downloads=true&stars=true
[license-image]: https://img.shields.io/npm/l/array.prototype.reduce.svg
[license-url]: LICENSE
[downloads-image]: https://img.shields.io/npm/dm/array.prototype.reduce.svg
[downloads-url]: https://npm-stat.com/charts.html?package=array.prototype.reduce
[codecov-image]: https://codecov.io/gh/es-shims/Array.prototype.reduce/branch/main/graphs/badge.svg
[codecov-url]: https://app.codecov.io/gh/es-shims/Array.prototype.reduce/
[actions-image]: https://img.shields.io/endpoint?url=https://github-actions-badge-u3jn4tfpocch.runkit.sh/es-shims/Array.prototype.reduce
[actions-url]: https://github.com/es-shims/Array.prototype.reduce/actions
