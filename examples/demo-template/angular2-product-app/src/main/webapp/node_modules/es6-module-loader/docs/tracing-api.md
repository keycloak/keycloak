This is not in the specification, but is provided since it is such a natural extension of loading 
and not much code at all.

Enable tracing and start importing modules:

```javascript
  loader.trace = true;
  loader.execute = false; // optional, disables execution of module contents

  loader.import('some/module').then(function() {
    /*
      Now we have:
      
        loader.loads['some/module'] == {
          name: 'some/module',
          deps: ['./unnormalized', 'deps'],
          depMap: {
            './unnormalized': 'normalized',
            'deps': 'deps'
          },
          address: '/resolvedURL',
          metadata: { metadata object from load },
          source: 'translated source code string',
          kind: 'dynamic' (instantiated) or 'declarative' (ES6 module pipeline)
        }

      With the dependency load records
        loader.loads['normalized']
        loader.loads['deps']
      also set.
    */
  });
```

So tracing can be done by importing a module, then reading its normalized name off of `loader.loads` 
(it is probably advisable to separately call `loader.normalize` to determine this).