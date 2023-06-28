'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var path = require('path');
var builtinList = require('builtin-modules');
var deepMerge = require('deepmerge');
var isModule = require('is-module');
var fs = require('fs');
var util = require('util');
var url = require('url');
var resolve = require('resolve');
var pluginutils = require('@rollup/pluginutils');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e : { 'default': e }; }

var path__default = /*#__PURE__*/_interopDefaultLegacy(path);
var builtinList__default = /*#__PURE__*/_interopDefaultLegacy(builtinList);
var deepMerge__default = /*#__PURE__*/_interopDefaultLegacy(deepMerge);
var isModule__default = /*#__PURE__*/_interopDefaultLegacy(isModule);
var fs__default = /*#__PURE__*/_interopDefaultLegacy(fs);
var resolve__default = /*#__PURE__*/_interopDefaultLegacy(resolve);

var version = "13.1.3";

util.promisify(fs__default["default"].access);
const readFile$1 = util.promisify(fs__default["default"].readFile);
const realpath = util.promisify(fs__default["default"].realpath);
const stat = util.promisify(fs__default["default"].stat);

async function fileExists(filePath) {
  try {
    const res = await stat(filePath);
    return res.isFile();
  } catch {
    return false;
  }
}

async function resolveSymlink(path) {
  return (await fileExists(path)) ? realpath(path) : path;
}

const onError = (error) => {
  if (error.code === 'ENOENT') {
    return false;
  }
  throw error;
};

const makeCache = (fn) => {
  const cache = new Map();
  const wrapped = async (param, done) => {
    if (cache.has(param) === false) {
      cache.set(
        param,
        fn(param).catch((err) => {
          cache.delete(param);
          throw err;
        })
      );
    }

    try {
      const result = cache.get(param);
      const value = await result;
      return done(null, value);
    } catch (error) {
      return done(error);
    }
  };

  wrapped.clear = () => cache.clear();

  return wrapped;
};

const isDirCached = makeCache(async (file) => {
  try {
    const stats = await stat(file);
    return stats.isDirectory();
  } catch (error) {
    return onError(error);
  }
});

const isFileCached = makeCache(async (file) => {
  try {
    const stats = await stat(file);
    return stats.isFile();
  } catch (error) {
    return onError(error);
  }
});

const readCachedFile = makeCache(readFile$1);

function handleDeprecatedOptions(opts) {
  const warnings = [];

  if (opts.customResolveOptions) {
    const { customResolveOptions } = opts;
    if (customResolveOptions.moduleDirectory) {
      // eslint-disable-next-line no-param-reassign
      opts.moduleDirectories = Array.isArray(customResolveOptions.moduleDirectory)
        ? customResolveOptions.moduleDirectory
        : [customResolveOptions.moduleDirectory];

      warnings.push(
        'node-resolve: The `customResolveOptions.moduleDirectory` option has been deprecated. Use `moduleDirectories`, which must be an array.'
      );
    }

    if (customResolveOptions.preserveSymlinks) {
      throw new Error(
        'node-resolve: `customResolveOptions.preserveSymlinks` is no longer an option. We now always use the rollup `preserveSymlinks` option.'
      );
    }

    [
      'basedir',
      'package',
      'extensions',
      'includeCoreModules',
      'readFile',
      'isFile',
      'isDirectory',
      'realpath',
      'packageFilter',
      'pathFilter',
      'paths',
      'packageIterator'
    ].forEach((resolveOption) => {
      if (customResolveOptions[resolveOption]) {
        throw new Error(
          `node-resolve: \`customResolveOptions.${resolveOption}\` is no longer an option. If you need this, please open an issue.`
        );
      }
    });
  }

  return { warnings };
}

// returns the imported package name for bare module imports
function getPackageName(id) {
  if (id.startsWith('.') || id.startsWith('/')) {
    return null;
  }

  const split = id.split('/');

  // @my-scope/my-package/foo.js -> @my-scope/my-package
  // @my-scope/my-package -> @my-scope/my-package
  if (split[0][0] === '@') {
    return `${split[0]}/${split[1]}`;
  }

  // my-package/foo.js -> my-package
  // my-package -> my-package
  return split[0];
}

function getMainFields(options) {
  let mainFields;
  if (options.mainFields) {
    ({ mainFields } = options);
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
  const {
    cache,
    extensions,
    pkg,
    mainFields,
    preserveSymlinks,
    useBrowserOverrides,
    rootDir,
    ignoreSideEffectsForRoot
  } = options;
  let { pkgPath } = options;

  if (cache.has(pkgPath)) {
    return cache.get(pkgPath);
  }

  // browserify/resolve doesn't realpath paths returned in its packageFilter callback
  if (!preserveSymlinks) {
    pkgPath = fs.realpathSync(pkgPath);
  }

  const pkgRoot = path.dirname(pkgPath);

  const packageInfo = {
    // copy as we are about to munge the `main` field of `pkg`.
    packageJson: { ...pkg },

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
    packageBrowserField:
      useBrowserOverrides &&
      typeof pkg.browser === 'object' &&
      Object.keys(pkg.browser).reduce((browser, key) => {
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
  if (
    useBrowserOverrides &&
    typeof pkg.browser === 'object' &&
    // eslint-disable-next-line no-prototype-builtins
    browserMap.hasOwnProperty(pkg.main)
  ) {
    packageInfo.resolvedEntryPoint = browserMap[pkg.main];
    packageInfo.browserMappedMain = true;
  } else {
    // index.node is technically a valid default entrypoint as well...
    packageInfo.resolvedEntryPoint = path.resolve(pkgRoot, pkg.main || 'index.js');
    packageInfo.browserMappedMain = false;
  }

  if (!ignoreSideEffectsForRoot || rootDir !== pkgRoot) {
    const packageSideEffects = pkg.sideEffects;
    if (typeof packageSideEffects === 'boolean') {
      internalPackageInfo.hasModuleSideEffects = () => packageSideEffects;
    } else if (Array.isArray(packageSideEffects)) {
      internalPackageInfo.hasModuleSideEffects = pluginutils.createFilter(packageSideEffects, null, {
        resolve: pkgRoot
      });
    }
  }

  cache.set(pkgPath, internalPackageInfo);
  return internalPackageInfo;
}

function normalizeInput(input) {
  if (Array.isArray(input)) {
    return input;
  } else if (typeof input === 'object') {
    return Object.values(input);
  }

  // otherwise it's a string
  return [input];
}

/* eslint-disable no-await-in-loop */

function isModuleDir(current, moduleDirs) {
  return moduleDirs.some((dir) => current.endsWith(dir));
}

async function findPackageJson(base, moduleDirs) {
  const { root } = path__default["default"].parse(base);
  let current = base;

  while (current !== root && !isModuleDir(current, moduleDirs)) {
    const pkgJsonPath = path__default["default"].join(current, 'package.json');
    if (await fileExists(pkgJsonPath)) {
      const pkgJsonString = fs__default["default"].readFileSync(pkgJsonPath, 'utf-8');
      return { pkgJson: JSON.parse(pkgJsonString), pkgPath: current, pkgJsonPath };
    }
    current = path__default["default"].resolve(current, '..');
  }
  return null;
}

function isUrl(str) {
  try {
    return !!new URL(str);
  } catch (_) {
    return false;
  }
}

function isConditions(exports) {
  return typeof exports === 'object' && Object.keys(exports).every((k) => !k.startsWith('.'));
}

function isMappings(exports) {
  return typeof exports === 'object' && !isConditions(exports);
}

function isMixedExports(exports) {
  const keys = Object.keys(exports);
  return keys.some((k) => k.startsWith('.')) && keys.some((k) => !k.startsWith('.'));
}

function createBaseErrorMsg(importSpecifier, importer) {
  return `Could not resolve import "${importSpecifier}" in ${importer}`;
}

function createErrorMsg(context, reason, internal) {
  const { importSpecifier, importer, pkgJsonPath } = context;
  const base = createBaseErrorMsg(importSpecifier, importer);
  const field = internal ? 'imports' : 'exports';
  return `${base} using ${field} defined in ${pkgJsonPath}.${reason ? ` ${reason}` : ''}`;
}

class ResolveError extends Error {}

class InvalidConfigurationError extends ResolveError {
  constructor(context, reason) {
    super(createErrorMsg(context, `Invalid "exports" field. ${reason}`));
  }
}

class InvalidModuleSpecifierError extends ResolveError {
  constructor(context, internal, reason) {
    super(createErrorMsg(context, reason, internal));
  }
}

class InvalidPackageTargetError extends ResolveError {
  constructor(context, reason) {
    super(createErrorMsg(context, reason));
  }
}

/* eslint-disable no-await-in-loop, no-undefined */

function includesInvalidSegments(pathSegments, moduleDirs) {
  return pathSegments
    .split('/')
    .slice(1)
    .some((t) => ['.', '..', ...moduleDirs].includes(t));
}

async function resolvePackageTarget(context, { target, subpath, pattern, internal }) {
  if (typeof target === 'string') {
    if (!pattern && subpath.length > 0 && !target.endsWith('/')) {
      throw new InvalidModuleSpecifierError(context);
    }

    if (!target.startsWith('./')) {
      if (internal && !['/', '../'].some((p) => target.startsWith(p)) && !isUrl(target)) {
        // this is a bare package import, remap it and resolve it using regular node resolve
        if (pattern) {
          const result = await context.resolveId(
            target.replace(/\*/g, subpath),
            context.pkgURL.href
          );
          return result ? url.pathToFileURL(result.location).href : null;
        }

        const result = await context.resolveId(`${target}${subpath}`, context.pkgURL.href);
        return result ? url.pathToFileURL(result.location).href : null;
      }
      throw new InvalidPackageTargetError(context, `Invalid mapping: "${target}".`);
    }

    if (includesInvalidSegments(target, context.moduleDirs)) {
      throw new InvalidPackageTargetError(context, `Invalid mapping: "${target}".`);
    }

    const resolvedTarget = new URL(target, context.pkgURL);
    if (!resolvedTarget.href.startsWith(context.pkgURL.href)) {
      throw new InvalidPackageTargetError(
        context,
        `Resolved to ${resolvedTarget.href} which is outside package ${context.pkgURL.href}`
      );
    }

    if (includesInvalidSegments(subpath, context.moduleDirs)) {
      throw new InvalidModuleSpecifierError(context);
    }

    if (pattern) {
      return resolvedTarget.href.replace(/\*/g, subpath);
    }
    return new URL(subpath, resolvedTarget).href;
  }

  if (Array.isArray(target)) {
    let lastError;
    for (const item of target) {
      try {
        const resolved = await resolvePackageTarget(context, {
          target: item,
          subpath,
          pattern,
          internal
        });

        // return if defined or null, but not undefined
        if (resolved !== undefined) {
          return resolved;
        }
      } catch (error) {
        if (!(error instanceof InvalidPackageTargetError)) {
          throw error;
        } else {
          lastError = error;
        }
      }
    }

    if (lastError) {
      throw lastError;
    }
    return null;
  }

  if (target && typeof target === 'object') {
    for (const [key, value] of Object.entries(target)) {
      if (key === 'default' || context.conditions.includes(key)) {
        const resolved = await resolvePackageTarget(context, {
          target: value,
          subpath,
          pattern,
          internal
        });

        // return if defined or null, but not undefined
        if (resolved !== undefined) {
          return resolved;
        }
      }
    }
    return undefined;
  }

  if (target === null) {
    return null;
  }

  throw new InvalidPackageTargetError(context, `Invalid exports field.`);
}

/* eslint-disable no-await-in-loop */

async function resolvePackageImportsExports(context, { matchKey, matchObj, internal }) {
  if (!matchKey.endsWith('*') && matchKey in matchObj) {
    const target = matchObj[matchKey];
    const resolved = await resolvePackageTarget(context, { target, subpath: '', internal });
    return resolved;
  }

  const expansionKeys = Object.keys(matchObj)
    .filter((k) => k.endsWith('/') || k.endsWith('*'))
    .sort((a, b) => b.length - a.length);

  for (const expansionKey of expansionKeys) {
    const prefix = expansionKey.substring(0, expansionKey.length - 1);

    if (expansionKey.endsWith('*') && matchKey.startsWith(prefix)) {
      const target = matchObj[expansionKey];
      const subpath = matchKey.substring(expansionKey.length - 1);
      const resolved = await resolvePackageTarget(context, {
        target,
        subpath,
        pattern: true,
        internal
      });
      return resolved;
    }

    if (matchKey.startsWith(expansionKey)) {
      const target = matchObj[expansionKey];
      const subpath = matchKey.substring(expansionKey.length);

      const resolved = await resolvePackageTarget(context, { target, subpath, internal });
      return resolved;
    }
  }

  throw new InvalidModuleSpecifierError(context, internal);
}

async function resolvePackageExports(context, subpath, exports) {
  if (isMixedExports(exports)) {
    throw new InvalidConfigurationError(
      context,
      'All keys must either start with ./, or without one.'
    );
  }

  if (subpath === '.') {
    let mainExport;
    // If exports is a String or Array, or an Object containing no keys starting with ".", then
    if (typeof exports === 'string' || Array.isArray(exports) || isConditions(exports)) {
      mainExport = exports;
    } else if (isMappings(exports)) {
      mainExport = exports['.'];
    }

    if (mainExport) {
      const resolved = await resolvePackageTarget(context, { target: mainExport, subpath: '' });
      if (resolved) {
        return resolved;
      }
    }
  } else if (isMappings(exports)) {
    const resolvedMatch = await resolvePackageImportsExports(context, {
      matchKey: subpath,
      matchObj: exports
    });

    if (resolvedMatch) {
      return resolvedMatch;
    }
  }

  throw new InvalidModuleSpecifierError(context);
}

async function resolvePackageImports({
  importSpecifier,
  importer,
  moduleDirs,
  conditions,
  resolveId
}) {
  const result = await findPackageJson(importer, moduleDirs);
  if (!result) {
    throw new Error(createBaseErrorMsg('. Could not find a parent package.json.'));
  }

  const { pkgPath, pkgJsonPath, pkgJson } = result;
  const pkgURL = url.pathToFileURL(`${pkgPath}/`);
  const context = {
    importer,
    importSpecifier,
    moduleDirs,
    pkgURL,
    pkgJsonPath,
    conditions,
    resolveId
  };

  const { imports } = pkgJson;
  if (!imports) {
    throw new InvalidModuleSpecifierError(context, true);
  }

  if (importSpecifier === '#' || importSpecifier.startsWith('#/')) {
    throw new InvalidModuleSpecifierError(context, true, 'Invalid import specifier.');
  }

  return resolvePackageImportsExports(context, {
    matchKey: importSpecifier,
    matchObj: imports,
    internal: true
  });
}

const resolveImportPath = util.promisify(resolve__default["default"]);
const readFile = util.promisify(fs__default["default"].readFile);

async function getPackageJson(importer, pkgName, resolveOptions, moduleDirectories) {
  if (importer) {
    const selfPackageJsonResult = await findPackageJson(importer, moduleDirectories);
    if (selfPackageJsonResult && selfPackageJsonResult.pkgJson.name === pkgName) {
      // the referenced package name is the current package
      return selfPackageJsonResult;
    }
  }

  try {
    const pkgJsonPath = await resolveImportPath(`${pkgName}/package.json`, resolveOptions);
    const pkgJson = JSON.parse(await readFile(pkgJsonPath, 'utf-8'));
    return { pkgJsonPath, pkgJson, pkgPath: path.dirname(pkgJsonPath) };
  } catch (_) {
    return null;
  }
}

async function resolveIdClassic({
  importSpecifier,
  packageInfoCache,
  extensions,
  mainFields,
  preserveSymlinks,
  useBrowserOverrides,
  baseDir,
  moduleDirectories,
  rootDir,
  ignoreSideEffectsForRoot
}) {
  let hasModuleSideEffects = () => null;
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
      useBrowserOverrides,
      rootDir,
      ignoreSideEffectsForRoot
    });

    ({ packageInfo, hasModuleSideEffects, hasPackageEntry, packageBrowserField } = info);

    return info.cachedPkg;
  };

  const resolveOptions = {
    basedir: baseDir,
    readFile: readCachedFile,
    isFile: isFileCached,
    isDirectory: isDirCached,
    extensions,
    includeCoreModules: false,
    moduleDirectory: moduleDirectories,
    preserveSymlinks,
    packageFilter: filter
  };

  let location;
  try {
    location = await resolveImportPath(importSpecifier, resolveOptions);
  } catch (error) {
    if (error.code !== 'MODULE_NOT_FOUND') {
      throw error;
    }
    return null;
  }

  return {
    location: preserveSymlinks ? location : await resolveSymlink(location),
    hasModuleSideEffects,
    hasPackageEntry,
    packageBrowserField,
    packageInfo
  };
}

async function resolveWithExportMap({
  importer,
  importSpecifier,
  exportConditions,
  packageInfoCache,
  extensions,
  mainFields,
  preserveSymlinks,
  useBrowserOverrides,
  baseDir,
  moduleDirectories,
  rootDir,
  ignoreSideEffectsForRoot
}) {
  if (importSpecifier.startsWith('#')) {
    // this is a package internal import, resolve using package imports field
    const resolveResult = await resolvePackageImports({
      importSpecifier,
      importer,
      moduleDirs: moduleDirectories,
      conditions: exportConditions,
      resolveId(id /* , parent*/) {
        return resolveIdClassic({
          importSpecifier: id,
          packageInfoCache,
          extensions,
          mainFields,
          preserveSymlinks,
          useBrowserOverrides,
          baseDir,
          moduleDirectories
        });
      }
    });

    const location = url.fileURLToPath(resolveResult);
    return {
      location: preserveSymlinks ? location : await resolveSymlink(location),
      hasModuleSideEffects: () => null,
      hasPackageEntry: true,
      packageBrowserField: false,
      // eslint-disable-next-line no-undefined
      packageInfo: undefined
    };
  }

  const pkgName = getPackageName(importSpecifier);
  if (pkgName) {
    // it's a bare import, find the package.json and resolve using package exports if available
    let hasModuleSideEffects = () => null;
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
        useBrowserOverrides,
        rootDir,
        ignoreSideEffectsForRoot
      });

      ({ packageInfo, hasModuleSideEffects, hasPackageEntry, packageBrowserField } = info);

      return info.cachedPkg;
    };

    const resolveOptions = {
      basedir: baseDir,
      readFile: readCachedFile,
      isFile: isFileCached,
      isDirectory: isDirCached,
      extensions,
      includeCoreModules: false,
      moduleDirectory: moduleDirectories,
      preserveSymlinks,
      packageFilter: filter
    };

    const result = await getPackageJson(importer, pkgName, resolveOptions, moduleDirectories);

    if (result && result.pkgJson.exports) {
      const { pkgJson, pkgJsonPath } = result;
      const subpath =
        pkgName === importSpecifier ? '.' : `.${importSpecifier.substring(pkgName.length)}`;
      const pkgDr = pkgJsonPath.replace('package.json', '');
      const pkgURL = url.pathToFileURL(pkgDr);

      const context = {
        importer,
        importSpecifier,
        moduleDirs: moduleDirectories,
        pkgURL,
        pkgJsonPath,
        conditions: exportConditions
      };
      const resolvedPackageExport = await resolvePackageExports(context, subpath, pkgJson.exports);
      const location = url.fileURLToPath(resolvedPackageExport);
      if (location) {
        return {
          location: preserveSymlinks ? location : await resolveSymlink(location),
          hasModuleSideEffects,
          hasPackageEntry,
          packageBrowserField,
          packageInfo
        };
      }
    }
  }

  return null;
}

async function resolveWithClassic({
  importer,
  importSpecifierList,
  exportConditions,
  warn,
  packageInfoCache,
  extensions,
  mainFields,
  preserveSymlinks,
  useBrowserOverrides,
  baseDir,
  moduleDirectories,
  rootDir,
  ignoreSideEffectsForRoot
}) {
  for (let i = 0; i < importSpecifierList.length; i++) {
    // eslint-disable-next-line no-await-in-loop
    const result = await resolveIdClassic({
      importer,
      importSpecifier: importSpecifierList[i],
      exportConditions,
      warn,
      packageInfoCache,
      extensions,
      mainFields,
      preserveSymlinks,
      useBrowserOverrides,
      baseDir,
      moduleDirectories,
      rootDir,
      ignoreSideEffectsForRoot
    });

    if (result) {
      return result;
    }
  }

  return null;
}

// Resolves to the module if found or `null`.
// The first import specificer will first be attempted with the exports algorithm.
// If this is unsuccesful because export maps are not being used, then all of `importSpecifierList`
// will be tried with the classic resolution algorithm
async function resolveImportSpecifiers({
  importer,
  importSpecifierList,
  exportConditions,
  warn,
  packageInfoCache,
  extensions,
  mainFields,
  preserveSymlinks,
  useBrowserOverrides,
  baseDir,
  moduleDirectories,
  rootDir,
  ignoreSideEffectsForRoot
}) {
  try {
    const exportMapRes = await resolveWithExportMap({
      importer,
      importSpecifier: importSpecifierList[0],
      exportConditions,
      packageInfoCache,
      extensions,
      mainFields,
      preserveSymlinks,
      useBrowserOverrides,
      baseDir,
      moduleDirectories,
      rootDir,
      ignoreSideEffectsForRoot
    });
    if (exportMapRes) return exportMapRes;
  } catch (error) {
    if (error instanceof ResolveError) {
      warn(error);
      return null;
    }
    throw error;
  }

  // package has no imports or exports, use classic node resolve
  return resolveWithClassic({
    importer,
    importSpecifierList,
    exportConditions,
    warn,
    packageInfoCache,
    extensions,
    mainFields,
    preserveSymlinks,
    useBrowserOverrides,
    baseDir,
    moduleDirectories,
    rootDir,
    ignoreSideEffectsForRoot
  });
}

/* eslint-disable no-param-reassign, no-shadow, no-undefined */

const builtins = new Set(builtinList__default["default"]);
const ES6_BROWSER_EMPTY = '\0node-resolve:empty.js';
const deepFreeze = (object) => {
  Object.freeze(object);

  for (const value of Object.values(object)) {
    if (typeof value === 'object' && !Object.isFrozen(value)) {
      deepFreeze(value);
    }
  }

  return object;
};

const baseConditions = ['default', 'module'];
const baseConditionsEsm = [...baseConditions, 'import'];
const baseConditionsCjs = [...baseConditions, 'require'];
const defaults = {
  dedupe: [],
  // It's important that .mjs is listed before .js so that Rollup will interpret npm modules
  // which deploy both ESM .mjs and CommonJS .js files as ESM.
  extensions: ['.mjs', '.js', '.json', '.node'],
  resolveOnly: [],
  moduleDirectories: ['node_modules'],
  ignoreSideEffectsForRoot: false
};
const DEFAULTS = deepFreeze(deepMerge__default["default"]({}, defaults));

function nodeResolve(opts = {}) {
  const { warnings } = handleDeprecatedOptions(opts);

  const options = { ...defaults, ...opts };
  const { extensions, jail, moduleDirectories, ignoreSideEffectsForRoot } = options;
  const conditionsEsm = [...baseConditionsEsm, ...(options.exportConditions || [])];
  const conditionsCjs = [...baseConditionsCjs, ...(options.exportConditions || [])];
  const packageInfoCache = new Map();
  const idToPackageInfo = new Map();
  const mainFields = getMainFields(options);
  const useBrowserOverrides = mainFields.indexOf('browser') !== -1;
  const isPreferBuiltinsSet = options.preferBuiltins === true || options.preferBuiltins === false;
  const preferBuiltins = isPreferBuiltinsSet ? options.preferBuiltins : true;
  const rootDir = path.resolve(options.rootDir || process.cwd());
  let { dedupe } = options;
  let rollupOptions;

  if (typeof dedupe !== 'function') {
    dedupe = (importee) =>
      options.dedupe.includes(importee) || options.dedupe.includes(getPackageName(importee));
  }

  const resolveOnly = options.resolveOnly.map((pattern) => {
    if (pattern instanceof RegExp) {
      return pattern;
    }
    const normalized = pattern.replace(/[\\^$*+?.()|[\]{}]/g, '\\$&');
    return new RegExp(`^${normalized}$`);
  });

  const browserMapCache = new Map();
  let preserveSymlinks;

  const doResolveId = async (context, importee, importer, custom) => {
    // strip query params from import
    const [importPath, params] = importee.split('?');
    const importSuffix = `${params ? `?${params}` : ''}`;
    importee = importPath;

    const baseDir = !importer || dedupe(importee) ? rootDir : path.dirname(importer);

    // https://github.com/defunctzombie/package-browser-field-spec
    const browser = browserMapCache.get(importer);
    if (useBrowserOverrides && browser) {
      const resolvedImportee = path.resolve(baseDir, importee);
      if (browser[importee] === false || browser[resolvedImportee] === false) {
        return { id: ES6_BROWSER_EMPTY };
      }
      const browserImportee =
        (importee[0] !== '.' && browser[importee]) ||
        browser[resolvedImportee] ||
        browser[`${resolvedImportee}.js`] ||
        browser[`${resolvedImportee}.json`];
      if (browserImportee) {
        importee = browserImportee;
      }
    }

    const parts = importee.split(/[/\\]/);
    let id = parts.shift();
    let isRelativeImport = false;

    if (id[0] === '@' && parts.length > 0) {
      // scoped packages
      id += `/${parts.shift()}`;
    } else if (id[0] === '.') {
      // an import relative to the parent dir of the importer
      id = path.resolve(baseDir, importee);
      isRelativeImport = true;
    }

    if (
      !isRelativeImport &&
      resolveOnly.length &&
      !resolveOnly.some((pattern) => pattern.test(id))
    ) {
      if (normalizeInput(rollupOptions.input).includes(importee)) {
        return null;
      }
      return false;
    }

    const importSpecifierList = [importee];

    if (importer === undefined && !importee[0].match(/^\.?\.?\//)) {
      // For module graph roots (i.e. when importer is undefined), we
      // need to handle 'path fragments` like `foo/bar` that are commonly
      // found in rollup config files. If importee doesn't look like a
      // relative or absolute path, we make it relative and attempt to
      // resolve it.
      importSpecifierList.push(`./${importee}`);
    }

    // TypeScript files may import '.js' to refer to either '.ts' or '.tsx'
    if (importer && importee.endsWith('.js')) {
      for (const ext of ['.ts', '.tsx']) {
        if (importer.endsWith(ext) && extensions.includes(ext)) {
          importSpecifierList.push(importee.replace(/.js$/, ext));
        }
      }
    }

    const warn = (...args) => context.warn(...args);
    const isRequire = custom && custom['node-resolve'] && custom['node-resolve'].isRequire;
    const exportConditions = isRequire ? conditionsCjs : conditionsEsm;

    if (useBrowserOverrides && !exportConditions.includes('browser'))
      exportConditions.push('browser');

    const resolvedWithoutBuiltins = await resolveImportSpecifiers({
      importer,
      importSpecifierList,
      exportConditions,
      warn,
      packageInfoCache,
      extensions,
      mainFields,
      preserveSymlinks,
      useBrowserOverrides,
      baseDir,
      moduleDirectories,
      rootDir,
      ignoreSideEffectsForRoot
    });

    const importeeIsBuiltin = builtins.has(importee);
    const resolved =
      importeeIsBuiltin && preferBuiltins
        ? {
            packageInfo: undefined,
            hasModuleSideEffects: () => null,
            hasPackageEntry: true,
            packageBrowserField: false
          }
        : resolvedWithoutBuiltins;
    if (!resolved) {
      return null;
    }

    const { packageInfo, hasModuleSideEffects, hasPackageEntry, packageBrowserField } = resolved;
    let { location } = resolved;
    if (packageBrowserField) {
      if (Object.prototype.hasOwnProperty.call(packageBrowserField, location)) {
        if (!packageBrowserField[location]) {
          browserMapCache.set(location, packageBrowserField);
          return { id: ES6_BROWSER_EMPTY };
        }
        location = packageBrowserField[location];
      }
      browserMapCache.set(location, packageBrowserField);
    }

    if (hasPackageEntry && !preserveSymlinks) {
      const exists = await fileExists(location);
      if (exists) {
        location = await realpath(location);
      }
    }

    idToPackageInfo.set(location, packageInfo);

    if (hasPackageEntry) {
      if (importeeIsBuiltin && preferBuiltins) {
        if (!isPreferBuiltinsSet && resolvedWithoutBuiltins && resolved !== importee) {
          context.warn(
            `preferring built-in module '${importee}' over local alternative at '${resolvedWithoutBuiltins.location}', pass 'preferBuiltins: false' to disable this behavior or 'preferBuiltins: true' to disable this warning`
          );
        }
        return false;
      } else if (jail && location.indexOf(path.normalize(jail.trim(path.sep))) !== 0) {
        return null;
      }
    }

    if (options.modulesOnly && (await fileExists(location))) {
      const code = await readFile$1(location, 'utf-8');
      if (isModule__default["default"](code)) {
        return {
          id: `${location}${importSuffix}`,
          moduleSideEffects: hasModuleSideEffects(location)
        };
      }
      return null;
    }
    return {
      id: `${location}${importSuffix}`,
      moduleSideEffects: hasModuleSideEffects(location)
    };
  };

  return {
    name: 'node-resolve',

    version,

    buildStart(options) {
      rollupOptions = options;

      for (const warning of warnings) {
        this.warn(warning);
      }

      ({ preserveSymlinks } = options);
    },

    generateBundle() {
      readCachedFile.clear();
      isFileCached.clear();
      isDirCached.clear();
    },

    async resolveId(importee, importer, resolveOptions) {
      if (importee === ES6_BROWSER_EMPTY) {
        return importee;
      }
      // ignore IDs with null character, these belong to other plugins
      if (/\0/.test(importee)) return null;

      if (/\0/.test(importer)) {
        importer = undefined;
      }

      const resolved = await doResolveId(this, importee, importer, resolveOptions.custom);
      if (resolved) {
        const resolvedResolved = await this.resolve(
          resolved.id,
          importer,
          Object.assign({ skipSelf: true }, resolveOptions)
        );
        if (resolvedResolved) {
          // Handle plugins that manually make the result external
          if (resolvedResolved.external) {
            return false;
          }
          // Pass on meta information added by other plugins
          return { ...resolved, meta: resolvedResolved.meta };
        }
      }
      return resolved;
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

exports.DEFAULTS = DEFAULTS;
exports["default"] = nodeResolve;
exports.nodeResolve = nodeResolve;
