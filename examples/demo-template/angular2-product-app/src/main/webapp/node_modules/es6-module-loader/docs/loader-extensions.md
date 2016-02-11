### Extending the ES6 Loader

The loader pipeline is based on the following hooks:

* Normalize: Given the import name, provide the normalized name for the resource.
* Locate: Given a normalized module name, provide the URL for the resource.
* Fetch: Given a URL for a resource, fetch its content.
* Translate: Given module source, make any source modifications.
* Instantiate: Given module source, determine its dependencies, and how to execute it.

Variations of these hooks can allow creating many different styles of loader.

Each hook can either return a result directly, or a promise for the result.

To use custom loader hooks, one would typically override the System loader hooks on the `System` global directly:

```javascript
  // store the old normalization function
  var systemNormalize = System.normalize;
  // override the normalization function
  System.normalize = function(name, parentName, parentAddress) {
    if (name == 'my/custom/rule')
      return 'custom/name';
    else
      return systemNormalize.call(this, name, parentName, parentAddress);
  }
```

### Custom Extension Example - Cache Busting Extension

```javascript
var systemLocate = System.locate;
System.locate = function(load) {
  var System = this; // its good to ensure exact instance-binding
  return Promise.resolve(systemLocate.call(this, load)).then(function(address) {
    return address + System.cacheBust;
  });
}
System.cacheBust = '?bust=' + Date.now();
```

The above will add a customizable cache-busting query parameter to all requests. Custom filtering could even be added as well to only do this for certain requests.

### Creating a Custom Loader

A custom loader can be created with:

```javascript
  var myLoader = new LoaderPolyfill({
    normalize: ...,
    locate: ...,
    fetch: ...,
    translate: ...,
    instantiate: ...
  });
```

### Loader Hooks

The signatures for all the loader hooks is provided below:

```javascript

/*
 * name: the unnormalized module name
 * parentName: the canonical module name for the requesting module
 * parentAddress: the address of the requesting module
 */
function normalize(name, parentName, parentAddress) {
  return resolvedName;
}

/*
 * load.name the canonical module name
 * load.metadata a metadata object that can be used to store
 *   derived metadata for reference in other hooks
 */
function locate(load) {
  return this.baseURL + '/' + load.name + '.js';
}

/*
 * load.name: the canonical module name
 * load.address: the URL returned from locate
 * load.metadata: the same metadata object by reference, which
 *   can be modified
 */
function fetch(load) {
  return new Promise(function(resolve, reject) {
    myFetchMethod.get(load.address, resolve, reject);
  });
}

/*
 * load.name
 * load.address
 * load.metadata
 * load.source: the fetched source
 */
function translate(load) {
  return load.source;
}

/*
 * load identical to previous hooks, but load.source
 * is now the translated source
 */
function instantiate(load) {
  // an empty return indicates standard ES6 linking and execution
  return;

  // a return value creates a "dynamic" module linking
  return {
    deps: ['some', 'dependencies'],
    execute: function() {
      return loader.newModule({
        some: 'export'
      });
    }
  };
}
```

For a more in-depth overview of creating with custom loaders, some resources are provided below:

* [ES6 Loader API guide](https://gist.github.com/dherman/7568080)
* [Yehuda Katz's essay](https://gist.github.com/wycats/51c96e3adcdb3a68cbc3) (outdated)