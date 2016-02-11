var absURLRegEx = /^([^\/]+:\/\/|\/)/;

// Normalization with module names as absolute URLs
SystemLoader.prototype.normalize = function(name, parentName, parentAddress) {
  // NB does `import 'file.js'` import relative to the parent name or baseURL?
  //    have assumed that it is baseURL-relative here, but spec may well align with URLs to be the latter
  //    safe option for users is to always use "./file.js" for relative

  // not absolute or relative -> apply paths (what will be sites)
  if (!name.match(absURLRegEx) && name[0] != '.')
    name = new URL(applyPaths(this.paths, name) || name, baseURI).href;
  // apply parent-relative normalization, parentAddress is already normalized
  else
    name = new URL(name, parentName || baseURI).href;

  return name;
};

SystemLoader.prototype.locate = function(load) {
  return load.name;
};


// ensure the transpiler is loaded correctly
SystemLoader.prototype.instantiate = function(load) {
  var self = this;
  return Promise.resolve(self.normalize(self.transpiler))
  .then(function(transpilerNormalized) {
    // load transpiler as a global (avoiding System clobbering)
    if (load.address === transpilerNormalized) {
      return {
        deps: [],
        execute: function() {
          var curSystem = __global.System;
          var curLoader = __global.Reflect.Loader;
          // ensure not detected as CommonJS
          __eval('(function(require,exports,module){' + load.source + '})();', load.address, __global);
          __global.System = curSystem;
          __global.Reflect.Loader = curLoader;
          return self.newModule({ 'default': __global[self.transpiler], __useDefault: true });
        }
      };
    }
  });
};