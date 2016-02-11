#object-keys <sup>[![Version Badge][2]][1]</sup>

[![Build Status][3]][4]
[![dependency status][5]][6]
[![dev dependency status][7]][8]
[![License][license-image]][license-url]
[![Downloads][downloads-image]][downloads-url]

[![npm badge][13]][1]

[![browser support][9]][10]

An Object.keys shim. Invoke its "shim" method to shim Object.keys if it is unavailable.

Most common usage:
```js
var keys = Object.keys || require('object-keys');
```

## Example

```js
var keys = require('object-keys');
var assert = require('assert');
var obj = {
	a: true,
	b: true,
	c: true
};

assert.deepEqual(keys(obj), ['a', 'b', 'c']);
```

```js
var keys = require('object-keys');
var assert = require('assert');
/* when Object.keys is not present */
delete Object.keys;
var shimmedKeys = keys.shim();
assert.equal(shimmedKeys, keys);
assert.deepEqual(Object.keys(obj), keys(obj));
```

```js
var keys = require('object-keys');
var assert = require('assert');
/* when Object.keys is present */
var shimmedKeys = keys.shim();
assert.equal(shimmedKeys, Object.keys);
assert.deepEqual(Object.keys(obj), keys(obj));
```

## Source
Implementation taken directly from [es5-shim][11], with modifications, including from [lodash][12].

## Tests
Simply clone the repo, `npm install`, and run `npm test`

[1]: https://npmjs.org/package/object-keys
[2]: http://vb.teelaun.ch/ljharb/object-keys.svg
[3]: https://travis-ci.org/ljharb/object-keys.svg
[4]: https://travis-ci.org/ljharb/object-keys
[5]: https://david-dm.org/ljharb/object-keys.svg
[6]: https://david-dm.org/ljharb/object-keys
[7]: https://david-dm.org/ljharb/object-keys/dev-status.svg
[8]: https://david-dm.org/ljharb/object-keys#info=devDependencies
[9]: https://ci.testling.com/ljharb/object-keys.png
[10]: https://ci.testling.com/ljharb/object-keys
[11]: https://github.com/es-shims/es5-shim/blob/master/es5-shim.js#L542-589
[12]: https://github.com/bestiejs/lodash
[13]: https://nodei.co/npm/object-keys.png?downloads=true&stars=true
[license-image]: http://img.shields.io/npm/l/object-keys.svg
[license-url]: LICENSE
[downloads-image]: http://img.shields.io/npm/dm/object-keys.svg
[downloads-url]: http://npm-stat.com/charts.html?package=object-keys

