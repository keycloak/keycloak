//

(function (__global) {


  var customModules = {};
  var customFactories = {};

  var executeModule = function (name) {
    if (!customFactories[name]) {
      return;
    }
    var module = customFactories[name].apply(null, []);
    customModules[name] = module;
    return module;
  };

  function CustomLoader(baseURL) {
    System.constructor.call(this, baseURL);
  }

  // inline Object.create-style class extension
  function CustomLoaderProto() {}
  CustomLoaderProto.prototype = System.constructor.prototype;
  CustomLoader.prototype = new CustomLoaderProto();


  
  CustomLoader.prototype.normalize = function (name, parentName, parentAddress) {
    return new Promise(function(resolve, reject) {
      if (name == 'asdfasdf') {
        return setTimeout(function () {
          resolve('test/loader/async-norm.js');
        }, 500);
      }

      if (name == 'error1') {
        return setTimeout(function () { reject('error1'); }, 100);
      }

      var normalized = System.normalize(name, parentName, parentAddress);
      resolve(normalized);
    });
  };

  CustomLoader.prototype.locate = function (load) {
    if (load.name == 'error2') {
      return new Promise(function (resolve, reject) {
        setTimeout(function () { reject('error2'); }, 100);
      });
    }

    if (load.name.match(/path\//))
      load.name = load.name.replace(/path\//, 'test/loader/');

    return System.locate(load);
  };

  
  CustomLoader.prototype.fetch = function (load) {
    if (load.name == 'error3') {
      throw 'error3';
    }
    if (load.name == 'error4' || load.name == 'error5') {
      return 'asdf';
    }
    return System.fetch.apply(this, arguments);
  };

  CustomLoader.prototype.translate = function (load) {
    if (load.name == 'error4') {
      return new Promise(function (resolve, reject) {
        setTimeout(function () { reject('error4'); }, 100);
      });
    }
    return System.translate.apply(this, arguments);
  };

  CustomLoader.prototype.instantiate = function (load) {
    if (load.name.match(/(traceur|babel.+\/browser).js$/)) {
      var transpiler = this.transpiler;
      return System['import'](transpiler).then(function() {
        return {
          deps: [],
          execute: function() {
            return System.get(System.normalize(transpiler));
          }
        };
      });
    }

    if (load.name == 'error5') {
      return new Promise(function (resolve, reject) {
        setTimeout(function () { reject('error5'); }, 100);
      });
    }
    // very bad AMD support
    if (load.source.indexOf('define') == -1) {
      return System.instantiate(load);
    }

    var factory, deps;
    var define = function (_deps, _factory) {
      deps = _deps;
      factory = _factory;
    };
    eval(load.source);

    customFactories[load.name] = factory;

    // normalize all dependencies now
    var normalizePromises = [];
    for (var i = 0; i < deps.length; i++) {
      normalizePromises.push(Promise.resolve(System.normalize(deps[i], load.name, load.address)));
    }

    return Promise.all(normalizePromises).then(function (resolvedDeps) {

      return {
        deps: deps,
        execute: function () {
          if (customModules[load.name]) {
            return System.newModule(customModules[load.name]);
          }

          // first ensure all dependencies have been executed
          for (var i = 0; i < resolvedDeps.length; i++) {
            resolvedDeps[i] = executeModule(resolvedDeps[i]);
          }

          var module = factory.apply(null, resolvedDeps);

          customModules[load.name] = module;
          return System.newModule(module);
        }
      };
    });
  };

  var customLoader = new CustomLoader(System.baseURL);
  customLoader.transpiler = System.transpiler;


  if (typeof exports === 'object')
    module.exports = customLoader;

  __global.customLoader = customLoader;
}(typeof window != 'undefined' ? window : global));
