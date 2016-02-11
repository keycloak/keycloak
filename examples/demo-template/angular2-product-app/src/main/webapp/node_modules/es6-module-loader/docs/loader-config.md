### Paths Implementation

The System loader provides paths rules used by the standard `locate` function.

For example, we might want to load `jquery` from a CDN location. For this we can provide a paths rule:

```javascript
  System.paths['jquery'] = '//ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js';
  System.import('jquery').then(function($) {
    // ...
  });
```

Any reference to `jquery` in other modules will also use this same version.

It is also possible to define wildcard paths rules. The most specific rule will be used:

```javascript
  System.paths['lodash/*'] = '/js/lodash/*.js'
  System.import('lodash/map').then(function(map) {
    // ...
  });
```

Rule specificity is determined by an exact paths match first, followed by the last deepest wildcard match.

### Custom Compilation Options

Custom [Traceur compilation options](https://github.com/google/traceur-compiler/wiki/Options-for-Compiling) can be set through `System.traceurOptions`, eg:

```javascript
System.traceurOptions = { annotations = true };
```

or if using Babel:

```javascript
System.babelOptions = { experimental: true };
```

or TypeScript:

```javascript
System.typescriptOptions = {};
```

### Finding the Transpiler

For Babel use the `browser.js` file contained in the `babel-core` npm module. For Traceur use the `traceur.js` file contained in the `traceur` npm module bin folder.

The transpiler is loaded as a module itself, so will follow normal paths rules.
> 
To set custom paths to Babel or Traceur use paths configuration:

```javascript
System.paths['traceur'] = 'path/to/traceur.js';
```

Alternatively if you know that the transpiler will be needed, it will be detected from the global (`window[System.transpiler]`) when the first module is loaded, so can be loaded before ES6 Module Loader:

```html
<script src="traceur.js"></script>
<script src="es6-module-loader.js"></script>
```

> TypeScript can be loaded in the browser, but in Node, must already be defined as a global before loading an ES6 file.