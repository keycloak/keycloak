<a href="http://promises-aplus.github.com/promises-spec"><img src="http://promises-aplus.github.com/promises-spec/assets/logo-small.png" alt="Promises/A+ logo" align="right" /></a>

[![Build Status](https://travis-ci.org/cujojs/when.svg?branch=master)](https://travis-ci.org/cujojs/when)
[![Inline docs](http://inch-ci.org/github/cujojs/when.svg?branch=master)](http://inch-ci.org/github/cujojs/when)

when.js
=======

When.js is a rock solid, battle-tested [Promises/A+](http://promises-aplus.github.com/promises-spec) and `when()` implementation, including a complete [ES6 Promise shim](docs/es6-promise-shim.md).  It's a powerful combination of small size, high performance, debuggability, and rich features:

* Resolve arrays and hashes of promises, as well as infinite promise sequences
* Execute tasks in parallel or sequentially
* Transform Node-style and other callback-based APIs into promise-based APIs

When.js is one of the many stand-alone components of [cujoJS](http://cujojs.com), the JavaScript Architectural Toolkit. 

Check it out:

- [What's new](CHANGES.md)
- [API docs](docs/api.md#api)
- Read more about how [promises simplify async programming](http://know.cujojs.com/tutorials/async/simplifying-async-with-promises)

Installation
------------

#### AMD

Available as `when` through [bower](http://bower.io), or just clone the repo and load `when.js` from the root.

```
bower install --save when
```

#### CommonJS/Node

```
npm install --save when
```

[More help & other environments &raquo;](docs/installation.md)

Usage
-----

Promises can be used to help manage complex and/or nested callback flows in a simple manner. To get a better handle on how promise flows look and how they can be helpful, there are a couple examples below (using commonjs).

This first example will print `"hello world!!!!"` if all went well, or `"drat!"` if there was a problem. It also uses [rest](https://github.com/cujojs/rest) to make an ajax request to a (fictional) external service.

```js
var rest = require('rest');

fetchRemoteGreeting()
    .then(addExclamation)
    .catch(handleError)
    .done(function(greeting) {
        console.log(greeting);
    });

function fetchRemoteGreeting() {
    // returns a when.js promise for 'hello world'
    return rest('http://example.com/greeting');
}

function addExclamation(greeting) {
    return greeting + '!!!!'
}

function handleError(e) {
    return 'drat!';
}
```

The second example shows off the power that comes with when's promise logic. Here, we get an array of numbers from a remote source and reduce them. The example will print `150` if all went well, and if there was a problem will print a full stack trace.

```js
var when = require('when');
var rest = require('rest');

when.reduce(when.map(getRemoteNumberList(), times10), sum)
    .done(function(result) {
        console.log(result);
    });

function getRemoteNumberList() {
    // Get a remote array [1, 2, 3, 4, 5]
    return rest('http://example.com/numbers').then(JSON.parse);
}

function sum(x, y) { return x + y; }
function times10(x) {return x * 10; }
```

License
-------

Licensed under MIT. [Full license here &raquo;](LICENSE.txt)

Contributing
------------

Please see the [contributing guide](CONTRIBUTING.md) for more information on running tests, opening issues, and contributing code to the project.

References
----------

Much of this code was inspired by the async innards of [wire.js](https://github.com/cujojs/wire), and has been influenced by the great work in [Q](https://github.com/kriskowal/q), [Dojo's Deferred](https://github.com/dojo/dojo), and [uber.js](https://github.com/phiggins42/uber.js).
