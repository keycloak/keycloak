'use strict';

function _interopDefault (ex) { return (ex && (typeof ex === 'object') && 'default' in ex) ? ex['default'] : ex; }

var path = require('path');
var builtinList = _interopDefault(require('builtin-modules'));
var isModule = _interopDefault(require('is-module'));
var fs = require('fs');
var fs__default = _interopDefault(fs);
var util = require('util');
var pluginutils = require('@rollup/pluginutils');
var resolveModule = _interopDefault(require('resolve'));

function asyncGeneratorStep(gen, resolve, reject, _next, _throw, key, arg) {
  try {
    var info = gen[key](arg);
    var value = info.value;
  } catch (error) {
    reject(error);
    return;
  }

  if (info.done) {
    resolve(value);
  } else {
    Promise.resolve(value).then(_next, _throw);
  }
}

function _asyncToGenerator(fn) {
  return function () {
    var self = this,
        args = arguments;
    return new Promise(function (resolve, reject) {
      var gen = fn.apply(self, args);

      function _next(value) {
        asyncGeneratorStep(gen, resolve, reject, _next, _throw, "next", value);
      }

      function _throw(err) {
        asyncGeneratorStep(gen, resolve, reject, _next, _throw, "throw", err);
      }

      _next(undefined);
    });
  };
}

const exists = util.promisify(fs__default.exists);
const readFile = util.promisify(fs__default.readFile);
const realpath = util.promisify(fs__default.realpath);
const stat = util.promisify(fs__default.stat);

const onError = error => {
  if (error.code === 'ENOENT') {
    return false;
  }

  throw error;
};

const makeCache = fn => {
  const cache = new Map();

  const wrapped =
  /*#__PURE__*/
  function () {
    var _ref = _asyncToGenerator(function* (param, done) {
      if (cache.has(param) === false) {
        cache.set(param, fn(param).catch(err => {
          cache.delete(param);
          throw err;
        }));
      }

      try {
        const result = cache.get(param);
        const value = yield result;
        return done(null, value);
      } catch (error) {
        return done(error);
      }
    });

    return function wrapped(_x, _x2) {
      return _ref.apply(this, arguments);
    };
  }();

  wrapped.clear = () => cache.clear();

  return wrapped;
};

const isDirCached = makeCache(
/*#__PURE__*/
function () {
  var _ref2 = _asyncToGenerator(function* (file) {
    try {
      const stats = yield stat(file);
      return stats.isDirectory();
    } catch (error) {
      return onError(error);
    }
  });

  return function (_x3) {
    return _ref2.apply(this, arguments);
  };
}());
const isFileCached = makeCache(
/*#__PURE__*/
function () {
  var _ref3 = _asyncToGenerator(function* (file) {
    try {
      const stats = yield stat(file);
      return stats.isFile();
    } catch (error) {
      return onError(error);
    }
  });

  return function (_x4) {
    return _ref3.apply(this, arguments);
  };
}());
const readCachedFile = makeCache(readFile);

const resolveId = util.promisify(resolveModule); // returns the imported package name for bare module imports

function getPackageName(id) {
  if (id.startsWith('.') || id.startsWith('/')) {
    return null;
  }

  const split = id.split('/'); // @my-scope/my-package/foo.js -> @my-scope/my-package
  // @my-scope/my-package -> @my-scope/my-package

  if (split[0][0] === '@') {
    return `${split[0]}/${split[1]}`;
  } // my-package/foo.js -> my-package
  // my-package -> my-package


  return split[0];
}
function getMainFields(options) {
  let mainFields;

  if (options.mainFields) {
    mainFields = options.mainFields;
  } else {
    mainFields = ['module', 'main'];
  }

  if (options.browser && mainFields.indexOf('browser') === -1) {
    return ['browser'].concat(mainFields);
  }

  if (!mainFields.length) {
    throw new Error('Please ensure at least one `mainFields` value is specified');
  }

  return mainFields;
}
function getPackageInfo(options) {
  const cache = options.cache,
        extensions = options.extensions,
        pkg = options.pkg,
        mainFields = options.mainFields,
        preserveSymlinks = options.preserveSymlinks,
        useBrowserOverrides = options.useBrowserOverrides;
  let pkgPath = options.pkgPath;

  if (cache.has(pkgPath)) {
    return cache.get(pkgPath);
  } // browserify/resolve doesn't realpath paths returned in its packageFilter callback


  if (!preserveSymlinks) {
    pkgPath = fs.realpathSync(pkgPath);
  }

  const pkgRoot = path.dirname(pkgPath);
  const packageInfo = {
    // copy as we are about to munge the `main` field of `pkg`.
    packageJson: Object.assign({}, pkg),
    // path to package.json file
    packageJsonPath: pkgPath,
    // directory containing the package.json
    root: pkgRoot,
    // which main field was used during resolution of this module (main, module, or browser)
    resolvedMainField: 'main',
    // whether the browser map was used to resolve the entry point to this module
    browserMappedMain: false,
    // the entry point of the module with respect to the selected main field and any
    // relevant browser mappings.
    resolvedEntryPoint: ''
  };
  let overriddenMain = false;

  for (let i = 0; i < mainFields.length; i++) {
    const field = mainFields[i];

    if (typeof pkg[field] === 'string') {
      pkg.main = pkg[field];
      packageInfo.resolvedMainField = field;
      overriddenMain = true;
      break;
    }
  }

  const internalPackageInfo = {
    cachedPkg: pkg,
    hasModuleSideEffects: () => null,
    hasPackageEntry: overriddenMain !== false || mainFields.indexOf('main') !== -1,
    packageBrowserField: useBrowserOverrides && typeof pkg.browser === 'object' && Object.keys(pkg.browser).reduce((browser, key) => {
      let resolved = pkg.browser[key];

      if (resolved && resolved[0] === '.') {
        resolved = path.resolve(pkgRoot, resolved);
      }
      /* eslint-disable no-param-reassign */


      browser[key] = resolved;

      if (key[0] === '.') {
        const absoluteKey = path.resolve(pkgRoot, key);
        browser[absoluteKey] = resolved;

        if (!path.extname(key)) {
          extensions.reduce((subBrowser, ext) => {
            subBrowser[absoluteKey + ext] = subBrowser[key];
            return subBrowser;
          }, browser);
        }
      }

      return browser;
    }, {}),
    packageInfo
  };
  const browserMap = internalPackageInfo.packageBrowserField;

  if (useBrowserOverrides && typeof pkg.browser === 'object' && // eslint-disable-next-line no-prototype-builtins
  browserMap.hasOwnProperty(pkg.main)) {
    packageInfo.resolvedEntryPoint = browserMap[pkg.main];
    packageInfo.browserMappedMain = true;
  } else {
    // index.node is technically a valid default entrypoint as well...
    packageInfo.resolvedEntryPoint = path.resolve(pkgRoot, pkg.main || 'index.js');
    packageInfo.browserMappedMain = false;
  }

  const packageSideEffects = pkg.sideEffects;

  if (typeof packageSideEffects === 'boolean') {
    internalPackageInfo.hasModuleSideEffects = () => packageSideEffects;
  } else if (Array.isArray(packageSideEffects)) {
    internalPackageInfo.hasModuleSideEffects = pluginutils.createFilter(packageSideEffects, null, {
      resolve: pkgRoot
    });
  }

  cache.set(pkgPath, internalPackageInfo);
  return internalPackageInfo;
}
function normalizeInput(input) {
  if (Array.isArray(input)) {
    return input;
  } else if (typeof input === 'object') {
    return Object.values(input);
  } // otherwise it's a string


  return input;
} // Resolve module specifiers in order. Promise resolves to the first module that resolves
// successfully, or the error that resulted from the last attempted module resolution.

function resolveImportSpecifiers(importSpecifierList, resolveOptions) {
  let promise = Promise.resolve();

  for (let i = 0; i < importSpecifierList.length; i++) {
    promise = promise.then(value => {
      // if we've already resolved to something, just return it.
      if (value) {
        return value;
      }

      return resolveId(importSpecifierList[i], resolveOptions).then(result => {
        if (!resolveOptions.preserveSymlinks) {
          result = fs.realpathSync(result);
        }

        return result;
      });
    });

    if (i < importSpecifierList.length - 1) {
      // swallow MODULE_NOT_FOUND errors from all but the last resolution
      promise = promise.catch(error => {
        if (error.code !== 'MODULE_NOT_FOUND') {
          throw error;
        }
      });
    }
  }

  return promise;
}

const builtins = new Set(builtinList);
const ES6_BROWSER_EMPTY = '\0node-resolve:empty.js';

const nullFn = () => null;

const defaults = {
  customResolveOptions: {},
  dedupe: [],
  // It's important that .mjs is listed before .js so that Rollup will interpret npm modules
  // which deploy both ESM .mjs and CommonJS .js files as ESM.
  extensions: ['.mjs', '.js', '.json', '.node'],
  resolveOnly: []
};
function nodeResolve(opts = {}) {
  const options = Object.assign({}, defaults, opts);
  const customResolveOptions = options.customResolveOptions,
        extensions = options.extensions,
        jail = options.jail;
  const warnings = [];
  const packageInfoCache = new Map();
  const idToPackageInfo = new Map();
  const mainFields = getMainFields(options);
  const useBrowserOverrides = mainFields.indexOf('browser') !== -1;
  const isPreferBuiltinsSet = options.preferBuiltins === true || options.preferBuiltins === false;
  const preferBuiltins = isPreferBuiltinsSet ? options.preferBuiltins : true;
  const rootDir = options.rootDir || process.cwd();
  let dedupe = options.dedupe;
  let rollupOptions;

  if (options.only) {
    warnings.push('node-resolve: The `only` options is deprecated, please use `resolveOnly`');
    options.resolveOnly = options.only;
  }

  if (typeof dedupe !== 'function') {
    dedupe = importee => options.dedupe.includes(importee) || options.dedupe.includes(getPackageName(importee));
  }

  const resolveOnly = options.resolveOnly.map(pattern => {
    if (pattern instanceof RegExp) {
      return pattern;
    }

    const normalized = pattern.replace(/[\\^$*+?.()|[\]{}]/g, '\\$&');
    return new RegExp(`^${normalized}$`);
  });
  const browserMapCache = new Map();
  let preserveSymlinks;
  return {
    name: 'node-resolve',

    buildStart(options) {
      rollupOptions = options;
      var _iteratorNormalCompletion = true;
      var _didIteratorError = false;
      var _iteratorError = undefined;

      try {
        for (var _iterator = warnings[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
          const warning = _step.value;
          this.warn(warning);
        }
      } catch (err) {
        _didIteratorError = true;
        _iteratorError = err;
      } finally {
        try {
          if (!_iteratorNormalCompletion && _iterator.return != null) {
            _iterator.return();
          }
        } finally {
          if (_didIteratorError) {
            throw _iteratorError;
          }
        }
      }

      preserveSymlinks = options.preserveSymlinks;
    },

    generateBundle() {
      readCachedFile.clear();
      isFileCached.clear();
      isDirCached.clear();
    },

    resolveId(importee, importer) {
      var _this = this;

      return _asyncToGenerator(function* () {
        if (importee === ES6_BROWSER_EMPTY) {
          return importee;
        } // ignore IDs with null character, these belong to other plugins


        if (/\0/.test(importee)) return null;
        const basedir = !importer || dedupe(importee) ? rootDir : path.dirname(importer); // https://github.com/defunctzombie/package-browser-field-spec

        const browser = browserMapCache.get(importer);

        if (useBrowserOverrides && browser) {
          const resolvedImportee = path.resolve(basedir, importee);

          if (browser[importee] === false || browser[resolvedImportee] === false) {
            return ES6_BROWSER_EMPTY;
          }

          const browserImportee = browser[importee] || browser[resolvedImportee] || browser[`${resolvedImportee}.js`] || browser[`${resolvedImportee}.json`];

          if (browserImportee) {
            importee = browserImportee;
          }
        }

        const parts = importee.split(/[/\\]/);
        let id = parts.shift();

        if (id[0] === '@' && parts.length > 0) {
          // scoped packages
          id += `/${parts.shift()}`;
        } else if (id[0] === '.') {
          // an import relative to the parent dir of the importer
          id = path.resolve(basedir, importee);
        }

        const input = normalizeInput(rollupOptions.input);

        if (resolveOnly.length && !resolveOnly.some(pattern => pattern.test(id))) {
          if (input.includes(id)) {
            return null;
          }

          return false;
        }

        let hasModuleSideEffects = nullFn;
        let hasPackageEntry = true;
        let packageBrowserField = false;
        let packageInfo;

        const filter = (pkg, pkgPath) => {
          const info = getPackageInfo({
            cache: packageInfoCache,
            extensions,
            pkg,
            pkgPath,
            mainFields,
            preserveSymlinks,
            useBrowserOverrides
          });
          packageInfo = info.packageInfo;
          hasModuleSideEffects = info.hasModuleSideEffects;
          hasPackageEntry = info.hasPackageEntry;
          packageBrowserField = info.packageBrowserField;
          return info.cachedPkg;
        };

        let resolveOptions = {
          basedir,
          packageFilter: filter,
          readFile: readCachedFile,
          isFile: isFileCached,
          isDirectory: isDirCached,
          extensions
        };

        if (preserveSymlinks !== undefined) {
          resolveOptions.preserveSymlinks = preserveSymlinks;
        }

        const importSpecifierList = [];

        if (importer === undefined && !importee[0].match(/^\.?\.?\//)) {
          // For module graph roots (i.e. when importer is undefined), we
          // need to handle 'path fragments` like `foo/bar` that are commonly
          // found in rollup config files. If importee doesn't look like a
          // relative or absolute path, we make it relative and attempt to
          // resolve it. If we don't find anything, we try resolving it as we
          // got it.
          importSpecifierList.push(`./${importee}`);
        }

        const importeeIsBuiltin = builtins.has(importee);

        if (importeeIsBuiltin && (!preferBuiltins || !isPreferBuiltinsSet)) {
          // The `resolve` library will not resolve packages with the same
          // name as a node built-in module. If we're resolving something
          // that's a builtin, and we don't prefer to find built-ins, we
          // first try to look up a local module with that name. If we don't
          // find anything, we resolve the builtin which just returns back
          // the built-in's name.
          importSpecifierList.push(`${importee}/`);
        }

        importSpecifierList.push(importee);
        resolveOptions = Object.assign(resolveOptions, customResolveOptions);

        try {
          let resolved = yield resolveImportSpecifiers(importSpecifierList, resolveOptions);

          if (resolved && packageBrowserField) {
            if (Object.prototype.hasOwnProperty.call(packageBrowserField, resolved)) {
              if (!packageBrowserField[resolved]) {
                browserMapCache.set(resolved, packageBrowserField);
                return ES6_BROWSER_EMPTY;
              }

              resolved = packageBrowserField[resolved];
            }

            browserMapCache.set(resolved, packageBrowserField);
          }

          if (hasPackageEntry && !preserveSymlinks && resolved) {
            const fileExists = yield exists(resolved);

            if (fileExists) {
              resolved = yield realpath(resolved);
            }
          }

          idToPackageInfo.set(resolved, packageInfo);

          if (hasPackageEntry) {
            if (builtins.has(resolved) && preferBuiltins && isPreferBuiltinsSet) {
              return null;
            } else if (importeeIsBuiltin && preferBuiltins) {
              if (!isPreferBuiltinsSet) {
                _this.warn(`preferring built-in module '${importee}' over local alternative at '${resolved}', pass 'preferBuiltins: false' to disable this behavior or 'preferBuiltins: true' to disable this warning`);
              }

              return null;
            } else if (jail && resolved.indexOf(path.normalize(jail.trim(path.sep))) !== 0) {
              return null;
            }
          }

          if (resolved && options.modulesOnly) {
            const code = yield readFile(resolved, 'utf-8');

            if (isModule(code)) {
              return {
                id: resolved,
                moduleSideEffects: hasModuleSideEffects(resolved)
              };
            }

            return null;
          }

          const result = {
            id: resolved,
            moduleSideEffects: hasModuleSideEffects(resolved)
          };
          return result;
        } catch (error) {
          return null;
        }
      })();
    },

    load(importee) {
      if (importee === ES6_BROWSER_EMPTY) {
        return 'export default {};';
      }

      return null;
    },

    getPackageInfoForId(id) {
      return idToPackageInfo.get(id);
    }

  };
}

module.exports = nodeResolve;
