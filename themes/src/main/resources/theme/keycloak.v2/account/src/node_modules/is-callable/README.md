# is-callable <sup>[![Version Badge][2]][1]</sup>

[![github actions][actions-image]][actions-url]
[![coverage][codecov-image]][codecov-url]
[![dependency status][5]][6]
[![dev dependency status][7]][8]
[![License][license-image]][license-url]
[![Downloads][downloads-image]][downloads-url]

[![npm badge][11]][1]

Is this JS value callable? Works with Functions and GeneratorFunctions, despite ES6 @@toStringTag.

## Example

```js
var isCallable = require('is-callable');
var assert = require('assert');

assert.notOk(isCallable(undefined));
assert.notOk(isCallable(null));
assert.notOk(isCallable(false));
assert.notOk(isCallable(true));
assert.notOk(isCallable([]));
assert.notOk(isCallable({}));
assert.notOk(isCallable(/a/g));
assert.notOk(isCallable(new RegExp('a', 'g')));
assert.notOk(isCallable(new Date()));
assert.notOk(isCallable(42));
assert.notOk(isCallable(NaN));
assert.notOk(isCallable(Infinity));
assert.notOk(isCallable(new Number(42)));
assert.notOk(isCallable('foo'));
assert.notOk(isCallable(Object('foo')));

assert.ok(isCallable(function () {}));
assert.ok(isCallable(function* () {}));
assert.ok(isCallable(x => x * x));
```

## Install

Install with

```
npm install is-callable
```

## Tests

Simply clone the repo, `npm install`, and run `npm test`

[1]: https://npmjs.org/package/is-callable
[2]: https://versionbadg.es/inspect-js/is-callable.svg
[5]: https://david-dm.org/inspect-js/is-callable.svg
[6]: https://david-dm.org/inspect-js/is-callable
[7]: https://david-dm.org/inspect-js/is-callable/dev-status.svg
[8]: https://david-dm.org/inspect-js/is-callable#info=devDependencies
[11]: https://nodei.co/npm/is-callable.png?downloads=true&stars=true
[license-image]: https://img.shields.io/npm/l/is-callable.svg
[license-url]: LICENSE
[downloads-image]: https://img.shields.io/npm/dm/is-callable.svg
[downloads-url]: https://npm-stat.com/charts.html?package=is-callable
[codecov-image]: https://codecov.io/gh/inspect-js/is-callable/branch/main/graphs/badge.svg
[codecov-url]: https://app.codecov.io/gh/inspect-js/is-callable/
[actions-image]: https://img.shields.io/endpoint?url=https://github-actions-badge-u3jn4tfpocch.runkit.sh/inspect-js/is-callable
[actions-url]: https://github.com/inspect-js/is-callable/actions
