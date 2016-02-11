# ES6 Module Loader Polyfill [![Build Status][travis-image]][travis-url]

_For upgrading to ES6 Module Loader 0.17, [read the release notes here](https://github.com/ModuleLoader/es6-module-loader/releases/tag/v0.17.0)._

Dynamically loads ES6 modules in browsers and [NodeJS](#nodejs-use) with support for loading existing and custom module formats through loader hooks.

This project implements dynamic module loading through `System` exactly to the previous ES6-specified loader API at [2014-08-24 ES6 Specification Draft Rev 27, Section 15](http://wiki.ecmascript.org/doku.php?id=harmony:specification_drafts#august_24_2014_draft_rev_27). The specification for the module loader was removed from the ES6/ES2015 specification in 2014, and a new loader implementing the new draft [WhatWG loader spec](https://github.com/whatwg/loader) is pending alpha release on the [1.0 branch](https://github.com/ModuleLoader/es6-module-loader/tree/1.0).

* Provides an asynchronous loader (`System.import`) to [dynamically load ES6 modules](#getting-started).
* Supports [Traceur](https://github.com/google/traceur-compiler), [Babel](http://babeljs.io/) and [TypeScript](https://github.com/Microsoft/TypeScript/) for compiling ES6 modules and syntax into ES5 in the browser with source map support.
* Fully supports [ES6 circular references and live bindings](docs/circular-references-bindings.md).
* Includes [`paths` implementation](docs/loader-config.md).
* Can be used as a [tracing tool](docs/tracing-api.md) for static analysis of modules.
* Supports IE8+, with IE9+ support for ES6 development without pre-compilation.
* The minified production loader is under 5KB minified and gzipped, making it suitable for production use, provided that modules are [built into ES5 making them independent of Traceur](docs/production-workflows.md).
* Supports declaring modules with `<script type="module">`, the precursor of the proposed [`<module>` tag](https://github.com/dherman/module-tag/).

For an overview of build workflows, [see the production guide](docs/production-workflows.md).

For an example of a universal module loader based on this polyfill for loading AMD, CommonJS and globals, see [SystemJS](https://github.com/systemjs/systemjs).

### Documentation

* [Configuring the loader](docs/loader-config.md)
* [Production workflows](docs/production-workflows.md)
* [Circular References &amp; Bindings](docs/circular-references-bindings.md)
* [Extending the loader through loader hooks](docs/loader-extensions.md)
* [Tracing API](docs/tracing-api.md)

### Getting Started

If using ES6 syntax (optional), include `traceur.js`, `babel.js` or `typescript.js` in the page first then include `es6-module-loader-dev.js`:

```html
  <script src="traceur.js"></script>
  <script src="es6-module-loader-dev.js"></script>
```

To use Babel, load Babel's `browser.js` instead and set the transpiler to `babel` with the loader configuration:

```html
<script>
  System.transpiler = 'babel';
</script>
```

To use TypeScript, set the transpiler to `typescript` in the loader configuration:

```html
<script>
  System.transpiler = 'typescript';
</script>
```

Then we can write any ES6 module:

mymodule.js:
```javascript
  export class q {
    constructor() {
      console.log('this is an es6 class!');
    }
  }
```

and load the module dynamically in the browser

```html
<script>
  System.import('mymodule').then(function(m) {
    new m.q();
  });
</script>
```

The dynamic loader returns a `Module` object, which contains getters for the named exports (in this case, `q`).

See the [demo folder](https://github.com/ModuleLoader/es6-module-loader/blob/master/demo/index.html) in this repo for a working example demonstrating module loading in the browser both with `System.import` and with the module-type script tag.

Although `System.import()` does not support the import of multiple modules defined in an array, because `System.import()` returns a Promise, this can be achieved by creating an array of `System.import`s and using `Promise.all()`.

#### Setting transpilation options

If using Traceur, these can be set with:

```javascript
System.traceurOptions = {...};
```

With Babel:

```javascript
System.babelOptions = {...};
```

With TypeScript:

```javascript
System.typescriptOptions = {...};
```

#### Module Tag

The module tag is specified to provide a new entry point for using module syntax in browsers. This is because normal `<script>` tags must remain backwards-compatible so won't support this new mechanism.

This polyfill provides simple support for the `<script type="module">` tag via:

```html
<script type="module">
  // loads the 'q' export from 'mymodule.js' in the same path as the page
  import { q } from 'mymodule';

  new q(); // -> 'this is an es6 class!'
</script>
```

Because it is only possible to load ES6 modules with this tag, it is not suitable for production use in this way.

#### NodeJS Use

```
  npm install es6-module-loader babel traceur typescript
```

It is important that Babel, Traceur or TypeScript is installed into the path in order to be found, since these are no longer project dependencies.

For use in NodeJS, the `Loader` and `System` globals are provided as exports:

index.js:
```javascript
  var System = require('es6-module-loader').System;
  /*  
   *  Include:
   *    System.transpiler = 'babel'; 
   *  to use Babel instead of Traceur or
   *    System.transpiler = 'typescript';
   *  to use TypeScript
   */

  System.import('some-module').then(function(m) {
    console.log(m.p);
  });
```

some-module.js:
```javascript
  export var p = 'NodeJS test';
```

Running the application:
```
> node index.js
NodeJS test
```

## Contributing
In lieu of a formal styleguide, take care to maintain the existing coding style. Add unit tests for any new or changed functionality. Lint and test your code using [grunt](https://github.com/cowboy/grunt).

_Also, please don't edit files in the "dist" subdirectory as they are generated via grunt. You'll find source code in the "lib" subdirectory!_

## Testing

- `npm run test:node` will use node to  to run the tests
- `npm run test:browser` will run `npm run test:browser-babel`, `npm run test:browser-traceur` and `npm run test:browser-typescript`
- `npm run test:browser-[transpiler]` use karma to run the tests with Traceur, Babel or TypeScript.
- `npm run test:browser:perf` will use karma to run benchmarks

`npm run test:browser-[transpiler]` supports options after a double dash (`--`) :

- You can use the `--polyfill` option to test the code with polyfill.

- You can use the `--coverage` option to test and extract coverage info.

- You can use the `--ie8` option to test the code in the ie8 scope only.

- You can use the `--saucelabs` option to use karma and saucelabs to run the tests in various browsers.
Note: you will need to export your username and key to launch it.

  ```sh
  export SAUCE_USERNAME={your user name} && export SAUCE_ACCESS_KEY={the access key that you see once logged in}
  npm run test:browsers -- --saucelabs
  ```

## Credit
Copyright (c) 2015 Luke Hoban, Addy Osmani, Guy Bedford

## License
Licensed under the MIT license.

[travis-url]: https://travis-ci.org/ModuleLoader/es6-module-loader
[travis-image]: https://travis-ci.org/ModuleLoader/es6-module-loader.svg?branch=master
[saucelabs-url]: https://saucelabs.com/u/guybedford
[saucelabs-image]: https://saucelabs.com/buildstatus/guybedford
