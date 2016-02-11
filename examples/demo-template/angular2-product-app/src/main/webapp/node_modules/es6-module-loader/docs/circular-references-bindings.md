### Circular References & Bindings

#### Zebra-Striping

All [AMD](http://requirejs.org/docs/api.html#circular), [CommonJS](http://nodejs.org/api/modules.html#modules_cycles), 
and [ES6](https://github.com/ModuleLoader/es6-module-loader#circular-references--bindings) treat circular dependencies differently. 
Handling this problem is one of the innovations of the loader spec, using a technique called **zebra-striping**. 
This involves analyzing the dependency tree and forming alternate layers of ES6 / non-ES6 modules with circular 
references in each layer for linking. The layers are then individually linked, with the appropriate circular reference 
handling being done within each layer. This allows CommonJS circular references to interact with ES6 circular references. 
Inter-format circular references are not supported as they would be across layers.

This loader implementation handles zebra-striping automatically, allowing a loader like [SystemJS](https://github.com/systemjs/systemjs)
to support all module formats with exact circular reference support.

#### ES6 Circular References &amp; Bindings

ES6 circular references and bindings behave in the following way:

* Bindings are set up before module execution.
* Execution is run from depth-first left to right on the module tree stopping at circular references.
* Bindings are live - an adjustment to an export of one module affects all modules importing it, but it can 
only be modified in the defining module.

even.js
```javascript
  import { odd } from './odd'

  export var counter = 0;

  export function even(n) {
    counter++;
    return n == 0 || odd(n - 1);
  }
```

odd.js
```javascript
  import { even } from './even';

  export function odd(n) {
    return n != 0 && even(n - 1);
  }
```

```javascript
  System.import('even').then(function(m) {
    m.even(10);
    m.counter;
    m.even(20);
    m.counter;
  });
```