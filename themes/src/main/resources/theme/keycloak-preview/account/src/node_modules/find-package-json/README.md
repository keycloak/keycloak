# find-package-json

Easily find package.json files that you're searching for. It spiders parent
directories for `package.json` files and correctly reads the results. It follows
an ES6 iterator design so it should be easy to implement. 

## Installation

The module is released in the public npm registry and can be installed by
running:

```
npm install --save find-package-json
```

## Usage

We expose a single function as module interface. So in all examples we assume
that you've already required the code as illustrated in the example below:

```js
'use strict';

var finder = require('find-package-json');
```

The function accepts 1 optional argument which is the directory it should start
searching in or a module object with a `filename` key. If nothing is provided
it will default to `process.cwd()` as entry point.

As we're build upon the iterator interface you can simply call the `.next()`
function of the returned result to find the first package.json. If you don't
like the result, call `.next()` again to find the next file. Please do note that
these methods do synchronous API calls in Node.js so they are blocking.

```js
var f = finder(__dirname);

console.log(f.next().value); // the package.json object
console.log(f.next().filename); // the path to the package.json file
```

You can also search for the global `module` object:

```js
var f = finder(module);

console.log(f.next().value); // the package.json object
console.log(f.next().filename); // the path to the package.json file
```

If there is no more package.json's to be found, the method will set the returned
`done` key as `true`;

```js
var f = finder(__dirname);

f.next().done // false
f.next().done // true
```

## License

MIT
