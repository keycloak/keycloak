# object.hasown <sup>[![Version Badge][npm-version-svg]][package-url]</sup>

[![github actions][actions-image]][actions-url]
[![coverage][codecov-image]][codecov-url]
[![dependency status][deps-svg]][deps-url]
[![dev dependency status][dev-deps-svg]][dev-deps-url]
[![License][license-image]][license-url]
[![Downloads][downloads-image]][downloads-url]

[![npm badge][npm-badge-png]][package-url]

An ES spec-compliant `Object.hasOwn` shim. Invoke its "shim" method to shim `Object.hasOwn` if it is unavailable or noncompliant.

This package implements the [es-shim API](https://github.com/es-shims/api) interface. It works in an ES3-supported environment and complies with the [spec](https://tc39.es/proposal-accessible-object-hasownproperty/).

Most common usage:
```js
var assert = require('assert');
var hasOwn = require('object.hasown');

var obj = { a: 1, b: 2 };

assert(hasOwn(obj, 'a'));
assert(hasOwn(obj, 'b'));
assert('toString' in obj && !hasOwn(obj, 'toString'));

if (!Object.hasOwn) {
	hasOwn.shim();
}

assert.deepEqual(Object.hasOwn(obj, 'a'), hasOwn(obj, 'a'));
```

## Tests
Simply clone the repo, `npm install`, and run `npm test`

[package-url]: https://npmjs.com/package/object.hasown
[npm-version-svg]: https://versionbadg.es/es-shims/Object.hasOwn.svg
[deps-svg]: https://david-dm.org/es-shims/Object.hasOwn.svg
[deps-url]: https://david-dm.org/es-shims/Object.hasOwn
[dev-deps-svg]: https://david-dm.org/es-shims/Object.hasOwn/dev-status.svg
[dev-deps-url]: https://david-dm.org/es-shims/Object.hasOwn#info=devDependencies
[npm-badge-png]: https://nodei.co/npm/object.hasown.png?downloads=true&stars=true
[license-image]: https://img.shields.io/npm/l/object.hasown.svg
[license-url]: LICENSE
[downloads-image]: https://img.shields.io/npm/dm/object.hasown.svg
[downloads-url]: https://npm-stat.com/charts.html?package=object.hasown
[codecov-image]: https://codecov.io/gh/es-shims/Object.hasOwn/branch/main/graphs/badge.svg
[codecov-url]: https://app.codecov.io/gh/es-shims/Object.hasOwn/
[actions-image]: https://img.shields.io/endpoint?url=https://github-actions-badge-u3jn4tfpocch.runkit.sh/es-shims/Object.hasOwn
[actions-url]: https://github.com/es-shims/Object.hasOwn/actions
