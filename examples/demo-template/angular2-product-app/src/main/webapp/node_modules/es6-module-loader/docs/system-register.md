### What it is

System.register can be considered as a new module format designed to support the exact semantics of ES6 modules within ES5. 
It is a format that was developed out of collaboration and is supported as a module output in Traceur (as _instantiate_), 
Babel and TypeScript (as _system_). All dynamic binding and circular reference behaviors supported by ES6 modules are supported 
by this format. In this way it acts as a safe and comprehensive target format for the polyfill path into ES6 modules.

To run the format, a suitable loader implementation needs to be used that understands how to execute it. Currently these include 
[SystemJS](https://github.com/systemjs/systemjs), [SystemJS Self-Executing Bundles](https://github.com/systemjs/builder#sfx-bundles) 
and [ES6 Micro Loader](https://github.com/caridy/es6-micro-loader). The ES6 Module Loader polyfill also uses this format 
internally when transpiling and executing ES6.

#### Bundled vs On-Demand

Just like AMD define, System.register can be both named and anonymous.

When a module name string is provided as the first argument in the `System.register` call, the format is suitable for 
naming multiple modules in the same JS file creating a bundle format.

When files are separately compiled, with only one `System.register` call per module, the name should not be set. 
This allows the importing environment to name the module into whatever namespace it likes without imposing a specific 
schema for maximum portability.

### How it works

When compiling ES6 modules to ES5, the Traceur `instantiate` output and Babel `system` output generates something like the following:

```javascript
  import { p as q } from './dep';
 
  var s = 'local';
  
  export function func() {
    return q;
  }

  export class C {
  }
```

->

```javascript
  System.register(['./dep'], function($__export) {
    var s, C, q;
    function func() {
      return q;
    }
    $__export('func', func);
    return {
      setters: [
      // every time a dependency updates an export, 
      // this function is called to update the local binding
      // the setter array matches up with the dependency array above
      function(m) {
        q = m.p;
      }
      ],
      execute: function() {
        // use the export function to update the exports of this module
        s = 'local';
        $__export('C', C = $traceurRuntime.createClass(...));
      }
    };
  });
```

Initial exports and changes to exports are pushed through the setter function, `$__export`. Values of dependencies and 
changes to dependency bindings are set through the dependency setters, `setters`, corresponding to the `$__export` calls of dependencies.

Functions and variables get hoisted into the declaration scope. This outer function sets up all the bindings, 
and the execution is entirely separated from this process. Hoisted functions are immediately exported. 
All of the modules in the tree first run this first function setting up all the bindings. 
Then we separately run all the execution functions left to right from the bottom of the tree ending at circular references.

In this way we get the live binding and circular reference support exactly as expected by the spec, 
while supporting ES3 environments for the module syntax conversion.

#### Bulk exports

The `$__export` function above can also be used to export multiple exports at the same time:

```javascript
$__export({ key: 'value', another: 'value' });
```

This is useful for performance of deep re-exports where unnecessary setter operations can be avoided.

#### Metadata

The next iteration of this format will include support for ES6 module meta information through a new 
System.register argument as soon as the specification for this is proposed.

### Limitations

The main limitation with authoring in this format and transitioning to ES6 is if unresolved exports exist such as:

```javascript
import {p} from 'q';
```

Where module `q` does not export a `p` at all.

This code will run in the System.register output but not in ES6 environments.

While this format can be adjusted to handle the SyntaxErrors that get thrown when an imported name does not exist, for performance and code-size constraints this is not provided. Ideally static checking via tooling should catch these issues rather.