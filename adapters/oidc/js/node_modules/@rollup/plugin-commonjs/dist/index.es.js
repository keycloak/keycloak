import { basename, extname, dirname, join, resolve, sep } from 'path';
import { makeLegalIdentifier, attachScopes, extractAssignedNames, createFilter } from '@rollup/pluginutils';
import getCommonDir from 'commondir';
import { existsSync, readFileSync, statSync } from 'fs';
import glob from 'glob';
import { walk } from 'estree-walker';
import MagicString from 'magic-string';
import isReference from 'is-reference';
import { sync } from 'resolve';

var peerDependencies = {
	rollup: "^2.38.3"
};

function tryParse(parse, code, id) {
  try {
    return parse(code, { allowReturnOutsideFunction: true });
  } catch (err) {
    err.message += ` in ${id}`;
    throw err;
  }
}

const firstpassGlobal = /\b(?:require|module|exports|global)\b/;

const firstpassNoGlobal = /\b(?:require|module|exports)\b/;

function hasCjsKeywords(code, ignoreGlobal) {
  const firstpass = ignoreGlobal ? firstpassNoGlobal : firstpassGlobal;
  return firstpass.test(code);
}

/* eslint-disable no-underscore-dangle */

function analyzeTopLevelStatements(parse, code, id) {
  const ast = tryParse(parse, code, id);

  let isEsModule = false;
  let hasDefaultExport = false;
  let hasNamedExports = false;

  for (const node of ast.body) {
    switch (node.type) {
      case 'ExportDefaultDeclaration':
        isEsModule = true;
        hasDefaultExport = true;
        break;
      case 'ExportNamedDeclaration':
        isEsModule = true;
        if (node.declaration) {
          hasNamedExports = true;
        } else {
          for (const specifier of node.specifiers) {
            if (specifier.exported.name === 'default') {
              hasDefaultExport = true;
            } else {
              hasNamedExports = true;
            }
          }
        }
        break;
      case 'ExportAllDeclaration':
        isEsModule = true;
        if (node.exported && node.exported.name === 'default') {
          hasDefaultExport = true;
        } else {
          hasNamedExports = true;
        }
        break;
      case 'ImportDeclaration':
        isEsModule = true;
        break;
    }
  }

  return { isEsModule, hasDefaultExport, hasNamedExports, ast };
}

const isWrappedId = (id, suffix) => id.endsWith(suffix);
const wrapId = (id, suffix) => `\0${id}${suffix}`;
const unwrapId = (wrappedId, suffix) => wrappedId.slice(1, -suffix.length);

const PROXY_SUFFIX = '?commonjs-proxy';
const REQUIRE_SUFFIX = '?commonjs-require';
const EXTERNAL_SUFFIX = '?commonjs-external';
const EXPORTS_SUFFIX = '?commonjs-exports';
const MODULE_SUFFIX = '?commonjs-module';

const DYNAMIC_REGISTER_SUFFIX = '?commonjs-dynamic-register';
const DYNAMIC_JSON_PREFIX = '\0commonjs-dynamic-json:';
const DYNAMIC_PACKAGES_ID = '\0commonjs-dynamic-packages';

const HELPERS_ID = '\0commonjsHelpers.js';

// `x['default']` is used instead of `x.default` for backward compatibility with ES3 browsers.
// Minifiers like uglify will usually transpile it back if compatibility with ES3 is not enabled.
// This will no longer be necessary once Rollup switches to ES6 output, likely
// in Rollup 3

const HELPERS = `
export var commonjsGlobal = typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : typeof global !== 'undefined' ? global : typeof self !== 'undefined' ? self : {};

export function getDefaultExportFromCjs (x) {
	return x && x.__esModule && Object.prototype.hasOwnProperty.call(x, 'default') ? x['default'] : x;
}

export function getDefaultExportFromNamespaceIfPresent (n) {
	return n && Object.prototype.hasOwnProperty.call(n, 'default') ? n['default'] : n;
}

export function getDefaultExportFromNamespaceIfNotNamed (n) {
	return n && Object.prototype.hasOwnProperty.call(n, 'default') && Object.keys(n).length === 1 ? n['default'] : n;
}

export function getAugmentedNamespace(n) {
	if (n.__esModule) return n;
	var a = Object.defineProperty({}, '__esModule', {value: true});
	Object.keys(n).forEach(function (k) {
		var d = Object.getOwnPropertyDescriptor(n, k);
		Object.defineProperty(a, k, d.get ? d : {
			enumerable: true,
			get: function () {
				return n[k];
			}
		});
	});
	return a;
}
`;

const FAILED_REQUIRE_ERROR = `throw new Error('Could not dynamically require "' + path + '". Please configure the dynamicRequireTargets or/and ignoreDynamicRequires option of @rollup/plugin-commonjs appropriately for this require call to work.');`;

const HELPER_NON_DYNAMIC = `
export function commonjsRequire (path) {
	${FAILED_REQUIRE_ERROR}
}
`;

const getDynamicHelpers = (ignoreDynamicRequires) => `
export function createModule(modulePath) {
	return {
		path: modulePath,
		exports: {},
		require: function (path, base) {
			return commonjsRequire(path, base == null ? modulePath : base);
		}
	};
}

export function commonjsRegister (path, loader) {
	DYNAMIC_REQUIRE_LOADERS[path] = loader;
}

export function commonjsRegisterOrShort (path, to) {
	const resolvedPath = commonjsResolveImpl(path, null, true);
	if (resolvedPath !== null && DYNAMIC_REQUIRE_CACHE[resolvedPath]) {
	  DYNAMIC_REQUIRE_CACHE[path] = DYNAMIC_REQUIRE_CACHE[resolvedPath];
	} else {
	  DYNAMIC_REQUIRE_SHORTS[path] = to;
	}
}

const DYNAMIC_REQUIRE_LOADERS = Object.create(null);
const DYNAMIC_REQUIRE_CACHE = Object.create(null);
const DYNAMIC_REQUIRE_SHORTS = Object.create(null);
const DEFAULT_PARENT_MODULE = {
	id: '<' + 'rollup>', exports: {}, parent: undefined, filename: null, loaded: false, children: [], paths: []
};
const CHECKED_EXTENSIONS = ['', '.js', '.json'];

function normalize (path) {
	path = path.replace(/\\\\/g, '/');
	const parts = path.split('/');
	const slashed = parts[0] === '';
	for (let i = 1; i < parts.length; i++) {
		if (parts[i] === '.' || parts[i] === '') {
			parts.splice(i--, 1);
		}
	}
	for (let i = 1; i < parts.length; i++) {
		if (parts[i] !== '..') continue;
		if (i > 0 && parts[i - 1] !== '..' && parts[i - 1] !== '.') {
			parts.splice(--i, 2);
			i--;
		}
	}
	path = parts.join('/');
	if (slashed && path[0] !== '/')
	  path = '/' + path;
	else if (path.length === 0)
	  path = '.';
	return path;
}

function join () {
	if (arguments.length === 0)
	  return '.';
	let joined;
	for (let i = 0; i < arguments.length; ++i) {
	  let arg = arguments[i];
	  if (arg.length > 0) {
		if (joined === undefined)
		  joined = arg;
		else
		  joined += '/' + arg;
	  }
	}
	if (joined === undefined)
	  return '.';

	return joined;
}

function isPossibleNodeModulesPath (modulePath) {
	let c0 = modulePath[0];
	if (c0 === '/' || c0 === '\\\\') return false;
	let c1 = modulePath[1], c2 = modulePath[2];
	if ((c0 === '.' && (!c1 || c1 === '/' || c1 === '\\\\')) ||
		(c0 === '.' && c1 === '.' && (!c2 || c2 === '/' || c2 === '\\\\'))) return false;
	if (c1 === ':' && (c2 === '/' || c2 === '\\\\'))
		return false;
	return true;
}

function dirname (path) {
  if (path.length === 0)
    return '.';

  let i = path.length - 1;
  while (i > 0) {
    const c = path.charCodeAt(i);
    if ((c === 47 || c === 92) && i !== path.length - 1)
      break;
    i--;
  }

  if (i > 0)
    return path.substr(0, i);

  if (path.chartCodeAt(0) === 47 || path.chartCodeAt(0) === 92)
    return path.charAt(0);

  return '.';
}

export function commonjsResolveImpl (path, originalModuleDir, testCache) {
	const shouldTryNodeModules = isPossibleNodeModulesPath(path);
	path = normalize(path);
	let relPath;
	if (path[0] === '/') {
		originalModuleDir = '/';
	}
	while (true) {
		if (!shouldTryNodeModules) {
			relPath = originalModuleDir ? normalize(originalModuleDir + '/' + path) : path;
		} else if (originalModuleDir) {
			relPath = normalize(originalModuleDir + '/node_modules/' + path);
		} else {
			relPath = normalize(join('node_modules', path));
		}

		if (relPath.endsWith('/..')) {
			break; // Travelled too far up, avoid infinite loop
		}

		for (let extensionIndex = 0; extensionIndex < CHECKED_EXTENSIONS.length; extensionIndex++) {
			const resolvedPath = relPath + CHECKED_EXTENSIONS[extensionIndex];
			if (DYNAMIC_REQUIRE_CACHE[resolvedPath]) {
				return resolvedPath;
			}
			if (DYNAMIC_REQUIRE_SHORTS[resolvedPath]) {
			  return resolvedPath;
			}
			if (DYNAMIC_REQUIRE_LOADERS[resolvedPath]) {
				return resolvedPath;
			}
		}
		if (!shouldTryNodeModules) break;
		const nextDir = normalize(originalModuleDir + '/..');
		if (nextDir === originalModuleDir) break;
		originalModuleDir = nextDir;
	}
	return null;
}

export function commonjsResolve (path, originalModuleDir) {
	const resolvedPath = commonjsResolveImpl(path, originalModuleDir);
	if (resolvedPath !== null) {
		return resolvedPath;
	}
	return require.resolve(path);
}

export function commonjsRequire (path, originalModuleDir) {
	let resolvedPath = commonjsResolveImpl(path, originalModuleDir, true);
	if (resolvedPath !== null) {
    let cachedModule = DYNAMIC_REQUIRE_CACHE[resolvedPath];
    if (cachedModule) return cachedModule.exports;
    let shortTo = DYNAMIC_REQUIRE_SHORTS[resolvedPath];
    if (shortTo) {
      cachedModule = DYNAMIC_REQUIRE_CACHE[shortTo];
      if (cachedModule)
        return cachedModule.exports;
      resolvedPath = commonjsResolveImpl(shortTo, null, true);
    }
    const loader = DYNAMIC_REQUIRE_LOADERS[resolvedPath];
    if (loader) {
      DYNAMIC_REQUIRE_CACHE[resolvedPath] = cachedModule = {
        id: resolvedPath,
        filename: resolvedPath,
        path: dirname(resolvedPath),
        exports: {},
        parent: DEFAULT_PARENT_MODULE,
        loaded: false,
        children: [],
        paths: [],
        require: function (path, base) {
          return commonjsRequire(path, (base === undefined || base === null) ? cachedModule.path : base);
        }
      };
      try {
        loader.call(commonjsGlobal, cachedModule, cachedModule.exports);
      } catch (error) {
        delete DYNAMIC_REQUIRE_CACHE[resolvedPath];
        throw error;
      }
      cachedModule.loaded = true;
      return cachedModule.exports;
    };
	}
	${ignoreDynamicRequires ? 'return require(path);' : FAILED_REQUIRE_ERROR}
}

commonjsRequire.cache = DYNAMIC_REQUIRE_CACHE;
commonjsRequire.resolve = commonjsResolve;
`;

function getHelpersModule(isDynamicRequireModulesEnabled, ignoreDynamicRequires) {
  return `${HELPERS}${
    isDynamicRequireModulesEnabled ? getDynamicHelpers(ignoreDynamicRequires) : HELPER_NON_DYNAMIC
  }`;
}

/* eslint-disable import/prefer-default-export */

function deconflict(scopes, globals, identifier) {
  let i = 1;
  let deconflicted = makeLegalIdentifier(identifier);
  const hasConflicts = () =>
    scopes.some((scope) => scope.contains(deconflicted)) || globals.has(deconflicted);

  while (hasConflicts()) {
    deconflicted = makeLegalIdentifier(`${identifier}_${i}`);
    i += 1;
  }

  for (const scope of scopes) {
    scope.declarations[deconflicted] = true;
  }

  return deconflicted;
}

function getName(id) {
  const name = makeLegalIdentifier(basename(id, extname(id)));
  if (name !== 'index') {
    return name;
  }
  return makeLegalIdentifier(basename(dirname(id)));
}

function normalizePathSlashes(path) {
  return path.replace(/\\/g, '/');
}

const VIRTUAL_PATH_BASE = '/$$rollup_base$$';
const getVirtualPathForDynamicRequirePath = (path, commonDir) => {
  const normalizedPath = normalizePathSlashes(path);
  return normalizedPath.startsWith(commonDir)
    ? VIRTUAL_PATH_BASE + normalizedPath.slice(commonDir.length)
    : normalizedPath;
};

function getPackageEntryPoint(dirPath) {
  let entryPoint = 'index.js';

  try {
    if (existsSync(join(dirPath, 'package.json'))) {
      entryPoint =
        JSON.parse(readFileSync(join(dirPath, 'package.json'), { encoding: 'utf8' })).main ||
        entryPoint;
    }
  } catch (ignored) {
    // ignored
  }

  return entryPoint;
}

function getDynamicPackagesModule(dynamicRequireModuleDirPaths, commonDir) {
  let code = `const commonjsRegisterOrShort = require('${HELPERS_ID}?commonjsRegisterOrShort');`;
  for (const dir of dynamicRequireModuleDirPaths) {
    const entryPoint = getPackageEntryPoint(dir);

    code += `\ncommonjsRegisterOrShort(${JSON.stringify(
      getVirtualPathForDynamicRequirePath(dir, commonDir)
    )}, ${JSON.stringify(getVirtualPathForDynamicRequirePath(join(dir, entryPoint), commonDir))});`;
  }
  return code;
}

function getDynamicPackagesEntryIntro(
  dynamicRequireModuleDirPaths,
  dynamicRequireModuleSet
) {
  let dynamicImports = Array.from(
    dynamicRequireModuleSet,
    (dynamicId) => `require(${JSON.stringify(wrapId(dynamicId, DYNAMIC_REGISTER_SUFFIX))});`
  ).join('\n');

  if (dynamicRequireModuleDirPaths.length) {
    dynamicImports += `require(${JSON.stringify(
      wrapId(DYNAMIC_PACKAGES_ID, DYNAMIC_REGISTER_SUFFIX)
    )});`;
  }

  return dynamicImports;
}

function isDynamicModuleImport(id, dynamicRequireModuleSet) {
  const normalizedPath = normalizePathSlashes(id);
  return dynamicRequireModuleSet.has(normalizedPath) && !normalizedPath.endsWith('.json');
}

function isDirectory(path) {
  try {
    if (statSync(path).isDirectory()) return true;
  } catch (ignored) {
    // Nothing to do here
  }
  return false;
}

function getDynamicRequirePaths(patterns) {
  const dynamicRequireModuleSet = new Set();
  for (const pattern of !patterns || Array.isArray(patterns) ? patterns || [] : [patterns]) {
    const isNegated = pattern.startsWith('!');
    const modifySet = Set.prototype[isNegated ? 'delete' : 'add'].bind(dynamicRequireModuleSet);
    for (const path of glob.sync(isNegated ? pattern.substr(1) : pattern)) {
      modifySet(normalizePathSlashes(resolve(path)));
      if (isDirectory(path)) {
        modifySet(normalizePathSlashes(resolve(join(path, getPackageEntryPoint(path)))));
      }
    }
  }
  const dynamicRequireModuleDirPaths = Array.from(dynamicRequireModuleSet.values()).filter((path) =>
    isDirectory(path)
  );
  return { dynamicRequireModuleSet, dynamicRequireModuleDirPaths };
}

function getCommonJSMetaPromise(commonJSMetaPromises, id) {
  let commonJSMetaPromise = commonJSMetaPromises.get(id);
  if (commonJSMetaPromise) return commonJSMetaPromise.promise;

  const promise = new Promise((resolve) => {
    commonJSMetaPromise = {
      resolve,
      promise: null
    };
    commonJSMetaPromises.set(id, commonJSMetaPromise);
  });
  commonJSMetaPromise.promise = promise;

  return promise;
}

function setCommonJSMetaPromise(commonJSMetaPromises, id, commonjsMeta) {
  const commonJSMetaPromise = commonJSMetaPromises.get(id);
  if (commonJSMetaPromise) {
    if (commonJSMetaPromise.resolve) {
      commonJSMetaPromise.resolve(commonjsMeta);
      commonJSMetaPromise.resolve = null;
    }
  } else {
    commonJSMetaPromises.set(id, { promise: Promise.resolve(commonjsMeta), resolve: null });
  }
}

// e.g. id === "commonjsHelpers?commonjsRegister"
function getSpecificHelperProxy(id) {
  return `export {${id.split('?')[1]} as default} from "${HELPERS_ID}";`;
}

function getUnknownRequireProxy(id, requireReturnsDefault) {
  if (requireReturnsDefault === true || id.endsWith('.json')) {
    return `export {default} from ${JSON.stringify(id)};`;
  }
  const name = getName(id);
  const exported =
    requireReturnsDefault === 'auto'
      ? `import {getDefaultExportFromNamespaceIfNotNamed} from "${HELPERS_ID}"; export default /*@__PURE__*/getDefaultExportFromNamespaceIfNotNamed(${name});`
      : requireReturnsDefault === 'preferred'
      ? `import {getDefaultExportFromNamespaceIfPresent} from "${HELPERS_ID}"; export default /*@__PURE__*/getDefaultExportFromNamespaceIfPresent(${name});`
      : !requireReturnsDefault
      ? `import {getAugmentedNamespace} from "${HELPERS_ID}"; export default /*@__PURE__*/getAugmentedNamespace(${name});`
      : `export default ${name};`;
  return `import * as ${name} from ${JSON.stringify(id)}; ${exported}`;
}

function getDynamicJsonProxy(id, commonDir) {
  const normalizedPath = normalizePathSlashes(id.slice(DYNAMIC_JSON_PREFIX.length));
  return `const commonjsRegister = require('${HELPERS_ID}?commonjsRegister');\ncommonjsRegister(${JSON.stringify(
    getVirtualPathForDynamicRequirePath(normalizedPath, commonDir)
  )}, function (module, exports) {
  module.exports = require(${JSON.stringify(normalizedPath)});
});`;
}

function getDynamicRequireProxy(normalizedPath, commonDir) {
  return `const commonjsRegister = require('${HELPERS_ID}?commonjsRegister');\ncommonjsRegister(${JSON.stringify(
    getVirtualPathForDynamicRequirePath(normalizedPath, commonDir)
  )}, function (module, exports) {
  ${readFileSync(normalizedPath, { encoding: 'utf8' })}
});`;
}

async function getStaticRequireProxy(
  id,
  requireReturnsDefault,
  esModulesWithDefaultExport,
  esModulesWithNamedExports,
  commonJsMetaPromises
) {
  const name = getName(id);
  const commonjsMeta = await getCommonJSMetaPromise(commonJsMetaPromises, id);
  if (commonjsMeta && commonjsMeta.isCommonJS) {
    return `export { __moduleExports as default } from ${JSON.stringify(id)};`;
  } else if (commonjsMeta === null) {
    return getUnknownRequireProxy(id, requireReturnsDefault);
  } else if (!requireReturnsDefault) {
    return `import { getAugmentedNamespace } from "${HELPERS_ID}"; import * as ${name} from ${JSON.stringify(
      id
    )}; export default /*@__PURE__*/getAugmentedNamespace(${name});`;
  } else if (
    requireReturnsDefault !== true &&
    (requireReturnsDefault === 'namespace' ||
      !esModulesWithDefaultExport.has(id) ||
      (requireReturnsDefault === 'auto' && esModulesWithNamedExports.has(id)))
  ) {
    return `import * as ${name} from ${JSON.stringify(id)}; export default ${name};`;
  }
  return `export { default } from ${JSON.stringify(id)};`;
}

/* eslint-disable no-param-reassign, no-undefined */

function getCandidatesForExtension(resolved, extension) {
  return [resolved + extension, `${resolved}${sep}index${extension}`];
}

function getCandidates(resolved, extensions) {
  return extensions.reduce(
    (paths, extension) => paths.concat(getCandidatesForExtension(resolved, extension)),
    [resolved]
  );
}

function getResolveId(extensions) {
  function resolveExtensions(importee, importer) {
    // not our problem
    if (importee[0] !== '.' || !importer) return undefined;

    const resolved = resolve(dirname(importer), importee);
    const candidates = getCandidates(resolved, extensions);

    for (let i = 0; i < candidates.length; i += 1) {
      try {
        const stats = statSync(candidates[i]);
        if (stats.isFile()) return { id: candidates[i] };
      } catch (err) {
        /* noop */
      }
    }

    return undefined;
  }

  return function resolveId(importee, rawImporter, resolveOptions) {
    if (isWrappedId(importee, MODULE_SUFFIX) || isWrappedId(importee, EXPORTS_SUFFIX)) {
      return importee;
    }

    const importer =
      rawImporter && isWrappedId(rawImporter, DYNAMIC_REGISTER_SUFFIX)
        ? unwrapId(rawImporter, DYNAMIC_REGISTER_SUFFIX)
        : rawImporter;

    // Except for exports, proxies are only importing resolved ids,
    // no need to resolve again
    if (importer && isWrappedId(importer, PROXY_SUFFIX)) {
      return importee;
    }

    const isProxyModule = isWrappedId(importee, PROXY_SUFFIX);
    const isRequiredModule = isWrappedId(importee, REQUIRE_SUFFIX);
    let isModuleRegistration = false;

    if (isProxyModule) {
      importee = unwrapId(importee, PROXY_SUFFIX);
    } else if (isRequiredModule) {
      importee = unwrapId(importee, REQUIRE_SUFFIX);

      isModuleRegistration = isWrappedId(importee, DYNAMIC_REGISTER_SUFFIX);
      if (isModuleRegistration) {
        importee = unwrapId(importee, DYNAMIC_REGISTER_SUFFIX);
      }
    }

    if (
      importee.startsWith(HELPERS_ID) ||
      importee === DYNAMIC_PACKAGES_ID ||
      importee.startsWith(DYNAMIC_JSON_PREFIX)
    ) {
      return importee;
    }

    if (importee.startsWith('\0')) {
      return null;
    }

    return this.resolve(
      importee,
      importer,
      Object.assign({}, resolveOptions, {
        skipSelf: true,
        custom: Object.assign({}, resolveOptions.custom, {
          'node-resolve': { isRequire: isProxyModule || isRequiredModule }
        })
      })
    ).then((resolved) => {
      if (!resolved) {
        resolved = resolveExtensions(importee, importer);
      }
      if (resolved && isProxyModule) {
        resolved.id = wrapId(resolved.id, resolved.external ? EXTERNAL_SUFFIX : PROXY_SUFFIX);
        resolved.external = false;
      } else if (resolved && isModuleRegistration) {
        resolved.id = wrapId(resolved.id, DYNAMIC_REGISTER_SUFFIX);
      } else if (!resolved && (isProxyModule || isRequiredModule)) {
        return { id: wrapId(importee, EXTERNAL_SUFFIX), external: false };
      }
      return resolved;
    });
  };
}

function validateRollupVersion(rollupVersion, peerDependencyVersion) {
  const [major, minor] = rollupVersion.split('.').map(Number);
  const versionRegexp = /\^(\d+\.\d+)\.\d+/g;
  let minMajor = Infinity;
  let minMinor = Infinity;
  let foundVersion;
  // eslint-disable-next-line no-cond-assign
  while ((foundVersion = versionRegexp.exec(peerDependencyVersion))) {
    const [foundMajor, foundMinor] = foundVersion[1].split('.').map(Number);
    if (foundMajor < minMajor) {
      minMajor = foundMajor;
      minMinor = foundMinor;
    }
  }
  if (major < minMajor || (major === minMajor && minor < minMinor)) {
    throw new Error(
      `Insufficient Rollup version: "@rollup/plugin-commonjs" requires at least rollup@${minMajor}.${minMinor} but found rollup@${rollupVersion}.`
    );
  }
}

const operators = {
  '==': (x) => equals(x.left, x.right, false),

  '!=': (x) => not(operators['=='](x)),

  '===': (x) => equals(x.left, x.right, true),

  '!==': (x) => not(operators['==='](x)),

  '!': (x) => isFalsy(x.argument),

  '&&': (x) => isTruthy(x.left) && isTruthy(x.right),

  '||': (x) => isTruthy(x.left) || isTruthy(x.right)
};

function not(value) {
  return value === null ? value : !value;
}

function equals(a, b, strict) {
  if (a.type !== b.type) return null;
  // eslint-disable-next-line eqeqeq
  if (a.type === 'Literal') return strict ? a.value === b.value : a.value == b.value;
  return null;
}

function isTruthy(node) {
  if (!node) return false;
  if (node.type === 'Literal') return !!node.value;
  if (node.type === 'ParenthesizedExpression') return isTruthy(node.expression);
  if (node.operator in operators) return operators[node.operator](node);
  return null;
}

function isFalsy(node) {
  return not(isTruthy(node));
}

function getKeypath(node) {
  const parts = [];

  while (node.type === 'MemberExpression') {
    if (node.computed) return null;

    parts.unshift(node.property.name);
    // eslint-disable-next-line no-param-reassign
    node = node.object;
  }

  if (node.type !== 'Identifier') return null;

  const { name } = node;
  parts.unshift(name);

  return { name, keypath: parts.join('.') };
}

const KEY_COMPILED_ESM = '__esModule';

function isDefineCompiledEsm(node) {
  const definedProperty =
    getDefinePropertyCallName(node, 'exports') || getDefinePropertyCallName(node, 'module.exports');
  if (definedProperty && definedProperty.key === KEY_COMPILED_ESM) {
    return isTruthy(definedProperty.value);
  }
  return false;
}

function getDefinePropertyCallName(node, targetName) {
  const {
    callee: { object, property }
  } = node;
  if (!object || object.type !== 'Identifier' || object.name !== 'Object') return;
  if (!property || property.type !== 'Identifier' || property.name !== 'defineProperty') return;
  if (node.arguments.length !== 3) return;

  const targetNames = targetName.split('.');
  const [target, key, value] = node.arguments;
  if (targetNames.length === 1) {
    if (target.type !== 'Identifier' || target.name !== targetNames[0]) {
      return;
    }
  }

  if (targetNames.length === 2) {
    if (
      target.type !== 'MemberExpression' ||
      target.object.name !== targetNames[0] ||
      target.property.name !== targetNames[1]
    ) {
      return;
    }
  }

  if (value.type !== 'ObjectExpression' || !value.properties) return;

  const valueProperty = value.properties.find((p) => p.key && p.key.name === 'value');
  if (!valueProperty || !valueProperty.value) return;

  // eslint-disable-next-line consistent-return
  return { key: key.value, value: valueProperty.value };
}

function isShorthandProperty(parent) {
  return parent && parent.type === 'Property' && parent.shorthand;
}

function hasDefineEsmProperty(node) {
  return node.properties.some((property) => {
    if (
      property.type === 'Property' &&
      property.key.type === 'Identifier' &&
      property.key.name === '__esModule' &&
      isTruthy(property.value)
    ) {
      return true;
    }
    return false;
  });
}

function wrapCode(magicString, uses, moduleName, exportsName) {
  const args = [];
  const passedArgs = [];
  if (uses.module) {
    args.push('module');
    passedArgs.push(moduleName);
  }
  if (uses.exports) {
    args.push('exports');
    passedArgs.push(exportsName);
  }
  magicString
    .trim()
    .prepend(`(function (${args.join(', ')}) {\n`)
    .append(`\n}(${passedArgs.join(', ')}));`);
}

function rewriteExportsAndGetExportsBlock(
  magicString,
  moduleName,
  exportsName,
  wrapped,
  moduleExportsAssignments,
  firstTopLevelModuleExportsAssignment,
  exportsAssignmentsByName,
  topLevelAssignments,
  defineCompiledEsmExpressions,
  deconflictedExportNames,
  code,
  HELPERS_NAME,
  exportMode,
  detectWrappedDefault,
  defaultIsModuleExports
) {
  const exports = [];
  const exportDeclarations = [];

  if (exportMode === 'replace') {
    getExportsForReplacedModuleExports(
      magicString,
      exports,
      exportDeclarations,
      moduleExportsAssignments,
      firstTopLevelModuleExportsAssignment,
      exportsName
    );
  } else {
    exports.push(`${exportsName} as __moduleExports`);
    if (wrapped) {
      getExportsWhenWrapping(
        exportDeclarations,
        exportsName,
        detectWrappedDefault,
        HELPERS_NAME,
        defaultIsModuleExports
      );
    } else {
      getExports(
        magicString,
        exports,
        exportDeclarations,
        moduleExportsAssignments,
        exportsAssignmentsByName,
        deconflictedExportNames,
        topLevelAssignments,
        moduleName,
        exportsName,
        defineCompiledEsmExpressions,
        HELPERS_NAME,
        defaultIsModuleExports
      );
    }
  }
  if (exports.length) {
    exportDeclarations.push(`export { ${exports.join(', ')} };`);
  }

  return `\n\n${exportDeclarations.join('\n')}`;
}

function getExportsForReplacedModuleExports(
  magicString,
  exports,
  exportDeclarations,
  moduleExportsAssignments,
  firstTopLevelModuleExportsAssignment,
  exportsName
) {
  for (const { left } of moduleExportsAssignments) {
    magicString.overwrite(left.start, left.end, exportsName);
  }
  magicString.prependRight(firstTopLevelModuleExportsAssignment.left.start, 'var ');
  exports.push(`${exportsName} as __moduleExports`);
  exportDeclarations.push(`export default ${exportsName};`);
}

function getExportsWhenWrapping(
  exportDeclarations,
  exportsName,
  detectWrappedDefault,
  HELPERS_NAME,
  defaultIsModuleExports
) {
  exportDeclarations.push(
    `export default ${
      detectWrappedDefault && defaultIsModuleExports === 'auto'
        ? `/*@__PURE__*/${HELPERS_NAME}.getDefaultExportFromCjs(${exportsName})`
        : defaultIsModuleExports === false
        ? `${exportsName}.default`
        : exportsName
    };`
  );
}

function getExports(
  magicString,
  exports,
  exportDeclarations,
  moduleExportsAssignments,
  exportsAssignmentsByName,
  deconflictedExportNames,
  topLevelAssignments,
  moduleName,
  exportsName,
  defineCompiledEsmExpressions,
  HELPERS_NAME,
  defaultIsModuleExports
) {
  let deconflictedDefaultExportName;
  // Collect and rewrite module.exports assignments
  for (const { left } of moduleExportsAssignments) {
    magicString.overwrite(left.start, left.end, `${moduleName}.exports`);
  }

  // Collect and rewrite named exports
  for (const [exportName, { nodes }] of exportsAssignmentsByName) {
    const deconflicted = deconflictedExportNames[exportName];
    let needsDeclaration = true;
    for (const node of nodes) {
      let replacement = `${deconflicted} = ${exportsName}.${exportName}`;
      if (needsDeclaration && topLevelAssignments.has(node)) {
        replacement = `var ${replacement}`;
        needsDeclaration = false;
      }
      magicString.overwrite(node.start, node.left.end, replacement);
    }
    if (needsDeclaration) {
      magicString.prepend(`var ${deconflicted};\n`);
    }

    if (exportName === 'default') {
      deconflictedDefaultExportName = deconflicted;
    } else {
      exports.push(exportName === deconflicted ? exportName : `${deconflicted} as ${exportName}`);
    }
  }

  // Collect and rewrite exports.__esModule assignments
  let isRestorableCompiledEsm = false;
  for (const expression of defineCompiledEsmExpressions) {
    isRestorableCompiledEsm = true;
    const moduleExportsExpression =
      expression.type === 'CallExpression' ? expression.arguments[0] : expression.left.object;
    magicString.overwrite(moduleExportsExpression.start, moduleExportsExpression.end, exportsName);
  }

  if (!isRestorableCompiledEsm || defaultIsModuleExports === true) {
    exportDeclarations.push(`export default ${exportsName};`);
  } else if (moduleExportsAssignments.length === 0 || defaultIsModuleExports === false) {
    exports.push(`${deconflictedDefaultExportName || exportsName} as default`);
  } else {
    exportDeclarations.push(
      `export default /*@__PURE__*/${HELPERS_NAME}.getDefaultExportFromCjs(${exportsName});`
    );
  }
}

function isRequireStatement(node, scope) {
  if (!node) return false;
  if (node.type !== 'CallExpression') return false;

  // Weird case of `require()` or `module.require()` without arguments
  if (node.arguments.length === 0) return false;

  return isRequire(node.callee, scope);
}

function isRequire(node, scope) {
  return (
    (node.type === 'Identifier' && node.name === 'require' && !scope.contains('require')) ||
    (node.type === 'MemberExpression' && isModuleRequire(node, scope))
  );
}

function isModuleRequire({ object, property }, scope) {
  return (
    object.type === 'Identifier' &&
    object.name === 'module' &&
    property.type === 'Identifier' &&
    property.name === 'require' &&
    !scope.contains('module')
  );
}

function isStaticRequireStatement(node, scope) {
  if (!isRequireStatement(node, scope)) return false;
  return !hasDynamicArguments(node);
}

function hasDynamicArguments(node) {
  return (
    node.arguments.length > 1 ||
    (node.arguments[0].type !== 'Literal' &&
      (node.arguments[0].type !== 'TemplateLiteral' || node.arguments[0].expressions.length > 0))
  );
}

const reservedMethod = { resolve: true, cache: true, main: true };

function isNodeRequirePropertyAccess(parent) {
  return parent && parent.property && reservedMethod[parent.property.name];
}

function isIgnoredRequireStatement(requiredNode, ignoreRequire) {
  return ignoreRequire(requiredNode.arguments[0].value);
}

function getRequireStringArg(node) {
  return node.arguments[0].type === 'Literal'
    ? node.arguments[0].value
    : node.arguments[0].quasis[0].value.cooked;
}

function hasDynamicModuleForPath(source, id, dynamicRequireModuleSet) {
  if (!/^(?:\.{0,2}[/\\]|[A-Za-z]:[/\\])/.test(source)) {
    try {
      const resolvedPath = normalizePathSlashes(sync(source, { basedir: dirname(id) }));
      if (dynamicRequireModuleSet.has(resolvedPath)) {
        return true;
      }
    } catch (ex) {
      // Probably a node.js internal module
      return false;
    }

    return false;
  }

  for (const attemptExt of ['', '.js', '.json']) {
    const resolvedPath = normalizePathSlashes(resolve(dirname(id), source + attemptExt));
    if (dynamicRequireModuleSet.has(resolvedPath)) {
      return true;
    }
  }

  return false;
}

function getRequireHandlers() {
  const requiredSources = [];
  const requiredBySource = Object.create(null);
  const requiredByNode = new Map();
  const requireExpressionsWithUsedReturnValue = [];

  function addRequireStatement(sourceId, node, scope, usesReturnValue) {
    const required = getRequired(sourceId);
    requiredByNode.set(node, { scope, required });
    if (usesReturnValue) {
      required.nodesUsingRequired.push(node);
      requireExpressionsWithUsedReturnValue.push(node);
    }
  }

  function getRequired(sourceId) {
    if (!requiredBySource[sourceId]) {
      requiredSources.push(sourceId);

      requiredBySource[sourceId] = {
        source: sourceId,
        name: null,
        nodesUsingRequired: []
      };
    }

    return requiredBySource[sourceId];
  }

  function rewriteRequireExpressionsAndGetImportBlock(
    magicString,
    topLevelDeclarations,
    topLevelRequireDeclarators,
    reassignedNames,
    helpersName,
    dynamicRegisterSources,
    moduleName,
    exportsName,
    id,
    exportMode
  ) {
    setRemainingImportNamesAndRewriteRequires(
      requireExpressionsWithUsedReturnValue,
      requiredByNode,
      magicString
    );
    const imports = [];
    imports.push(`import * as ${helpersName} from "${HELPERS_ID}";`);
    if (exportMode === 'module') {
      imports.push(
        `import { __module as ${moduleName}, exports as ${exportsName} } from ${JSON.stringify(
          wrapId(id, MODULE_SUFFIX)
        )}`
      );
    } else if (exportMode === 'exports') {
      imports.push(
        `import { __exports as ${exportsName} } from ${JSON.stringify(wrapId(id, EXPORTS_SUFFIX))}`
      );
    }
    for (const source of dynamicRegisterSources) {
      imports.push(`import ${JSON.stringify(wrapId(source, REQUIRE_SUFFIX))};`);
    }
    for (const source of requiredSources) {
      if (!source.startsWith('\0')) {
        imports.push(`import ${JSON.stringify(wrapId(source, REQUIRE_SUFFIX))};`);
      }
      const { name, nodesUsingRequired } = requiredBySource[source];
      imports.push(
        `import ${nodesUsingRequired.length ? `${name} from ` : ''}${JSON.stringify(
          source.startsWith('\0') ? source : wrapId(source, PROXY_SUFFIX)
        )};`
      );
    }
    return imports.length ? `${imports.join('\n')}\n\n` : '';
  }

  return {
    addRequireStatement,
    requiredSources,
    rewriteRequireExpressionsAndGetImportBlock
  };
}

function setRemainingImportNamesAndRewriteRequires(
  requireExpressionsWithUsedReturnValue,
  requiredByNode,
  magicString
) {
  let uid = 0;
  for (const requireExpression of requireExpressionsWithUsedReturnValue) {
    const { required } = requiredByNode.get(requireExpression);
    if (!required.name) {
      let potentialName;
      const isUsedName = (node) => requiredByNode.get(node).scope.contains(potentialName);
      do {
        potentialName = `require$$${uid}`;
        uid += 1;
      } while (required.nodesUsingRequired.some(isUsedName));
      required.name = potentialName;
    }
    magicString.overwrite(requireExpression.start, requireExpression.end, required.name);
  }
}

/* eslint-disable no-param-reassign, no-shadow, no-underscore-dangle, no-continue */

const exportsPattern = /^(?:module\.)?exports(?:\.([a-zA-Z_$][a-zA-Z_$0-9]*))?$/;

const functionType = /^(?:FunctionDeclaration|FunctionExpression|ArrowFunctionExpression)$/;

function transformCommonjs(
  parse,
  code,
  id,
  isEsModule,
  ignoreGlobal,
  ignoreRequire,
  ignoreDynamicRequires,
  getIgnoreTryCatchRequireStatementMode,
  sourceMap,
  isDynamicRequireModulesEnabled,
  dynamicRequireModuleSet,
  disableWrap,
  commonDir,
  astCache,
  defaultIsModuleExports
) {
  const ast = astCache || tryParse(parse, code, id);
  const magicString = new MagicString(code);
  const uses = {
    module: false,
    exports: false,
    global: false,
    require: false
  };
  let usesDynamicRequire = false;
  const virtualDynamicRequirePath =
    isDynamicRequireModulesEnabled && getVirtualPathForDynamicRequirePath(dirname(id), commonDir);
  let scope = attachScopes(ast, 'scope');
  let lexicalDepth = 0;
  let programDepth = 0;
  let currentTryBlockEnd = null;
  let shouldWrap = false;

  const globals = new Set();

  // TODO technically wrong since globals isn't populated yet, but ¯\_(ツ)_/¯
  const HELPERS_NAME = deconflict([scope], globals, 'commonjsHelpers');
  const dynamicRegisterSources = new Set();
  let hasRemovedRequire = false;

  const {
    addRequireStatement,
    requiredSources,
    rewriteRequireExpressionsAndGetImportBlock
  } = getRequireHandlers();

  // See which names are assigned to. This is necessary to prevent
  // illegally replacing `var foo = require('foo')` with `import foo from 'foo'`,
  // where `foo` is later reassigned. (This happens in the wild. CommonJS, sigh)
  const reassignedNames = new Set();
  const topLevelDeclarations = [];
  const topLevelRequireDeclarators = new Set();
  const skippedNodes = new Set();
  const moduleAccessScopes = new Set([scope]);
  const exportsAccessScopes = new Set([scope]);
  const moduleExportsAssignments = [];
  let firstTopLevelModuleExportsAssignment = null;
  const exportsAssignmentsByName = new Map();
  const topLevelAssignments = new Set();
  const topLevelDefineCompiledEsmExpressions = [];

  walk(ast, {
    enter(node, parent) {
      if (skippedNodes.has(node)) {
        this.skip();
        return;
      }

      if (currentTryBlockEnd !== null && node.start > currentTryBlockEnd) {
        currentTryBlockEnd = null;
      }

      programDepth += 1;
      if (node.scope) ({ scope } = node);
      if (functionType.test(node.type)) lexicalDepth += 1;
      if (sourceMap) {
        magicString.addSourcemapLocation(node.start);
        magicString.addSourcemapLocation(node.end);
      }

      // eslint-disable-next-line default-case
      switch (node.type) {
        case 'TryStatement':
          if (currentTryBlockEnd === null) {
            currentTryBlockEnd = node.block.end;
          }
          return;
        case 'AssignmentExpression':
          if (node.left.type === 'MemberExpression') {
            const flattened = getKeypath(node.left);
            if (!flattened || scope.contains(flattened.name)) return;

            const exportsPatternMatch = exportsPattern.exec(flattened.keypath);
            if (!exportsPatternMatch || flattened.keypath === 'exports') return;

            const [, exportName] = exportsPatternMatch;
            uses[flattened.name] = true;

            // we're dealing with `module.exports = ...` or `[module.]exports.foo = ...` –
            if (flattened.keypath === 'module.exports') {
              moduleExportsAssignments.push(node);
              if (programDepth > 3) {
                moduleAccessScopes.add(scope);
              } else if (!firstTopLevelModuleExportsAssignment) {
                firstTopLevelModuleExportsAssignment = node;
              }

              if (defaultIsModuleExports === false) {
                shouldWrap = true;
              } else if (defaultIsModuleExports === 'auto') {
                if (node.right.type === 'ObjectExpression') {
                  if (hasDefineEsmProperty(node.right)) {
                    shouldWrap = true;
                  }
                } else if (defaultIsModuleExports === false) {
                  shouldWrap = true;
                }
              }
            } else if (exportName === KEY_COMPILED_ESM) {
              if (programDepth > 3) {
                shouldWrap = true;
              } else {
                topLevelDefineCompiledEsmExpressions.push(node);
              }
            } else {
              const exportsAssignments = exportsAssignmentsByName.get(exportName) || {
                nodes: [],
                scopes: new Set()
              };
              exportsAssignments.nodes.push(node);
              exportsAssignments.scopes.add(scope);
              exportsAccessScopes.add(scope);
              exportsAssignmentsByName.set(exportName, exportsAssignments);
              if (programDepth <= 3) {
                topLevelAssignments.add(node);
              }
            }

            skippedNodes.add(node.left);
          } else {
            for (const name of extractAssignedNames(node.left)) {
              reassignedNames.add(name);
            }
          }
          return;
        case 'CallExpression': {
          if (isDefineCompiledEsm(node)) {
            if (programDepth === 3 && parent.type === 'ExpressionStatement') {
              // skip special handling for [module.]exports until we know we render this
              skippedNodes.add(node.arguments[0]);
              topLevelDefineCompiledEsmExpressions.push(node);
            } else {
              shouldWrap = true;
            }
            return;
          }

          if (
            node.callee.object &&
            node.callee.object.name === 'require' &&
            node.callee.property.name === 'resolve' &&
            hasDynamicModuleForPath(id, '/', dynamicRequireModuleSet)
          ) {
            const requireNode = node.callee.object;
            magicString.appendLeft(
              node.end - 1,
              `,${JSON.stringify(
                dirname(id) === '.' ? null /* default behavior */ : virtualDynamicRequirePath
              )}`
            );
            magicString.overwrite(
              requireNode.start,
              requireNode.end,
              `${HELPERS_NAME}.commonjsRequire`,
              {
                storeName: true
              }
            );
            return;
          }

          if (!isStaticRequireStatement(node, scope)) return;
          if (!isDynamicRequireModulesEnabled) {
            skippedNodes.add(node.callee);
          }
          if (!isIgnoredRequireStatement(node, ignoreRequire)) {
            skippedNodes.add(node.callee);
            const usesReturnValue = parent.type !== 'ExpressionStatement';

            let canConvertRequire = true;
            let shouldRemoveRequireStatement = false;

            if (currentTryBlockEnd !== null) {
              ({
                canConvertRequire,
                shouldRemoveRequireStatement
              } = getIgnoreTryCatchRequireStatementMode(node.arguments[0].value));

              if (shouldRemoveRequireStatement) {
                hasRemovedRequire = true;
              }
            }

            let sourceId = getRequireStringArg(node);
            const isDynamicRegister = isWrappedId(sourceId, DYNAMIC_REGISTER_SUFFIX);
            if (isDynamicRegister) {
              sourceId = unwrapId(sourceId, DYNAMIC_REGISTER_SUFFIX);
              if (sourceId.endsWith('.json')) {
                sourceId = DYNAMIC_JSON_PREFIX + sourceId;
              }
              dynamicRegisterSources.add(wrapId(sourceId, DYNAMIC_REGISTER_SUFFIX));
            } else {
              if (
                !sourceId.endsWith('.json') &&
                hasDynamicModuleForPath(sourceId, id, dynamicRequireModuleSet)
              ) {
                if (shouldRemoveRequireStatement) {
                  magicString.overwrite(node.start, node.end, `undefined`);
                } else if (canConvertRequire) {
                  magicString.overwrite(
                    node.start,
                    node.end,
                    `${HELPERS_NAME}.commonjsRequire(${JSON.stringify(
                      getVirtualPathForDynamicRequirePath(sourceId, commonDir)
                    )}, ${JSON.stringify(
                      dirname(id) === '.' ? null /* default behavior */ : virtualDynamicRequirePath
                    )})`
                  );
                  usesDynamicRequire = true;
                }
                return;
              }

              if (canConvertRequire) {
                addRequireStatement(sourceId, node, scope, usesReturnValue);
              }
            }

            if (usesReturnValue) {
              if (shouldRemoveRequireStatement) {
                magicString.overwrite(node.start, node.end, `undefined`);
                return;
              }

              if (
                parent.type === 'VariableDeclarator' &&
                !scope.parent &&
                parent.id.type === 'Identifier'
              ) {
                // This will allow us to reuse this variable name as the imported variable if it is not reassigned
                // and does not conflict with variables in other places where this is imported
                topLevelRequireDeclarators.add(parent);
              }
            } else {
              // This is a bare import, e.g. `require('foo');`

              if (!canConvertRequire && !shouldRemoveRequireStatement) {
                return;
              }

              magicString.remove(parent.start, parent.end);
            }
          }
          return;
        }
        case 'ConditionalExpression':
        case 'IfStatement':
          // skip dead branches
          if (isFalsy(node.test)) {
            skippedNodes.add(node.consequent);
          } else if (node.alternate && isTruthy(node.test)) {
            skippedNodes.add(node.alternate);
          }
          return;
        case 'Identifier': {
          const { name } = node;
          if (!(isReference(node, parent) && !scope.contains(name))) return;
          switch (name) {
            case 'require':
              if (isNodeRequirePropertyAccess(parent)) {
                if (hasDynamicModuleForPath(id, '/', dynamicRequireModuleSet)) {
                  if (parent.property.name === 'cache') {
                    magicString.overwrite(node.start, node.end, `${HELPERS_NAME}.commonjsRequire`, {
                      storeName: true
                    });
                  }
                }

                return;
              }

              if (isDynamicRequireModulesEnabled && isRequireStatement(parent, scope)) {
                magicString.appendLeft(
                  parent.end - 1,
                  `,${JSON.stringify(
                    dirname(id) === '.' ? null /* default behavior */ : virtualDynamicRequirePath
                  )}`
                );
              }
              if (!ignoreDynamicRequires) {
                if (isShorthandProperty(parent)) {
                  magicString.appendRight(node.end, `: ${HELPERS_NAME}.commonjsRequire`);
                } else {
                  magicString.overwrite(node.start, node.end, `${HELPERS_NAME}.commonjsRequire`, {
                    storeName: true
                  });
                }
              }
              usesDynamicRequire = true;
              return;
            case 'module':
            case 'exports':
              shouldWrap = true;
              uses[name] = true;
              return;
            case 'global':
              uses.global = true;
              if (!ignoreGlobal) {
                magicString.overwrite(node.start, node.end, `${HELPERS_NAME}.commonjsGlobal`, {
                  storeName: true
                });
              }
              return;
            case 'define':
              magicString.overwrite(node.start, node.end, 'undefined', {
                storeName: true
              });
              return;
            default:
              globals.add(name);
              return;
          }
        }
        case 'MemberExpression':
          if (!isDynamicRequireModulesEnabled && isModuleRequire(node, scope)) {
            magicString.overwrite(node.start, node.end, `${HELPERS_NAME}.commonjsRequire`, {
              storeName: true
            });
            skippedNodes.add(node.object);
            skippedNodes.add(node.property);
          }
          return;
        case 'ReturnStatement':
          // if top-level return, we need to wrap it
          if (lexicalDepth === 0) {
            shouldWrap = true;
          }
          return;
        case 'ThisExpression':
          // rewrite top-level `this` as `commonjsHelpers.commonjsGlobal`
          if (lexicalDepth === 0) {
            uses.global = true;
            if (!ignoreGlobal) {
              magicString.overwrite(node.start, node.end, `${HELPERS_NAME}.commonjsGlobal`, {
                storeName: true
              });
            }
          }
          return;
        case 'UnaryExpression':
          // rewrite `typeof module`, `typeof module.exports` and `typeof exports` (https://github.com/rollup/rollup-plugin-commonjs/issues/151)
          if (node.operator === 'typeof') {
            const flattened = getKeypath(node.argument);
            if (!flattened) return;

            if (scope.contains(flattened.name)) return;

            if (
              flattened.keypath === 'module.exports' ||
              flattened.keypath === 'module' ||
              flattened.keypath === 'exports'
            ) {
              magicString.overwrite(node.start, node.end, `'object'`, {
                storeName: false
              });
            }
          }
          return;
        case 'VariableDeclaration':
          if (!scope.parent) {
            topLevelDeclarations.push(node);
          }
      }
    },

    leave(node) {
      programDepth -= 1;
      if (node.scope) scope = scope.parent;
      if (functionType.test(node.type)) lexicalDepth -= 1;
    }
  });

  const nameBase = getName(id);
  const exportsName = deconflict([...exportsAccessScopes], globals, nameBase);
  const moduleName = deconflict([...moduleAccessScopes], globals, `${nameBase}Module`);
  const deconflictedExportNames = Object.create(null);
  for (const [exportName, { scopes }] of exportsAssignmentsByName) {
    deconflictedExportNames[exportName] = deconflict([...scopes], globals, exportName);
  }

  // We cannot wrap ES/mixed modules
  shouldWrap =
    !isEsModule &&
    !disableWrap &&
    (shouldWrap || (uses.exports && moduleExportsAssignments.length > 0));
  const detectWrappedDefault =
    shouldWrap &&
    (topLevelDefineCompiledEsmExpressions.length > 0 || code.indexOf('__esModule') >= 0);

  if (
    !(
      requiredSources.length ||
      dynamicRegisterSources.size ||
      uses.module ||
      uses.exports ||
      uses.require ||
      usesDynamicRequire ||
      hasRemovedRequire ||
      topLevelDefineCompiledEsmExpressions.length > 0
    ) &&
    (ignoreGlobal || !uses.global)
  ) {
    return { meta: { commonjs: { isCommonJS: false } } };
  }

  let leadingComment = '';
  if (code.startsWith('/*')) {
    const commentEnd = code.indexOf('*/', 2) + 2;
    leadingComment = `${code.slice(0, commentEnd)}\n`;
    magicString.remove(0, commentEnd).trim();
  }

  const exportMode = shouldWrap
    ? uses.module
      ? 'module'
      : 'exports'
    : firstTopLevelModuleExportsAssignment
    ? exportsAssignmentsByName.size === 0 && topLevelDefineCompiledEsmExpressions.length === 0
      ? 'replace'
      : 'module'
    : moduleExportsAssignments.length === 0
    ? 'exports'
    : 'module';

  const importBlock = rewriteRequireExpressionsAndGetImportBlock(
    magicString,
    topLevelDeclarations,
    topLevelRequireDeclarators,
    reassignedNames,
    HELPERS_NAME,
    dynamicRegisterSources,
    moduleName,
    exportsName,
    id,
    exportMode
  );

  const exportBlock = isEsModule
    ? ''
    : rewriteExportsAndGetExportsBlock(
        magicString,
        moduleName,
        exportsName,
        shouldWrap,
        moduleExportsAssignments,
        firstTopLevelModuleExportsAssignment,
        exportsAssignmentsByName,
        topLevelAssignments,
        topLevelDefineCompiledEsmExpressions,
        deconflictedExportNames,
        code,
        HELPERS_NAME,
        exportMode,
        detectWrappedDefault,
        defaultIsModuleExports
      );

  if (shouldWrap) {
    wrapCode(magicString, uses, moduleName, exportsName);
  }

  magicString
    .trim()
    .prepend(leadingComment + importBlock)
    .append(exportBlock);

  return {
    code: magicString.toString(),
    map: sourceMap ? magicString.generateMap() : null,
    syntheticNamedExports: isEsModule ? false : '__moduleExports',
    meta: { commonjs: { isCommonJS: !isEsModule } }
  };
}

function commonjs(options = {}) {
  const extensions = options.extensions || ['.js'];
  const filter = createFilter(options.include, options.exclude);
  const {
    ignoreGlobal,
    ignoreDynamicRequires,
    requireReturnsDefault: requireReturnsDefaultOption,
    esmExternals
  } = options;
  const getRequireReturnsDefault =
    typeof requireReturnsDefaultOption === 'function'
      ? requireReturnsDefaultOption
      : () => requireReturnsDefaultOption;
  let esmExternalIds;
  const isEsmExternal =
    typeof esmExternals === 'function'
      ? esmExternals
      : Array.isArray(esmExternals)
      ? ((esmExternalIds = new Set(esmExternals)), (id) => esmExternalIds.has(id))
      : () => esmExternals;
  const defaultIsModuleExports =
    typeof options.defaultIsModuleExports === 'boolean' ? options.defaultIsModuleExports : 'auto';

  const { dynamicRequireModuleSet, dynamicRequireModuleDirPaths } = getDynamicRequirePaths(
    options.dynamicRequireTargets
  );
  const isDynamicRequireModulesEnabled = dynamicRequireModuleSet.size > 0;
  const commonDir = isDynamicRequireModulesEnabled
    ? getCommonDir(null, Array.from(dynamicRequireModuleSet).concat(process.cwd()))
    : null;

  const esModulesWithDefaultExport = new Set();
  const esModulesWithNamedExports = new Set();
  const commonJsMetaPromises = new Map();

  const ignoreRequire =
    typeof options.ignore === 'function'
      ? options.ignore
      : Array.isArray(options.ignore)
      ? (id) => options.ignore.includes(id)
      : () => false;

  const getIgnoreTryCatchRequireStatementMode = (id) => {
    const mode =
      typeof options.ignoreTryCatch === 'function'
        ? options.ignoreTryCatch(id)
        : Array.isArray(options.ignoreTryCatch)
        ? options.ignoreTryCatch.includes(id)
        : typeof options.ignoreTryCatch !== 'undefined'
        ? options.ignoreTryCatch
        : true;

    return {
      canConvertRequire: mode !== 'remove' && mode !== true,
      shouldRemoveRequireStatement: mode === 'remove'
    };
  };

  const resolveId = getResolveId(extensions);

  const sourceMap = options.sourceMap !== false;

  function transformAndCheckExports(code, id) {
    if (isDynamicRequireModulesEnabled && this.getModuleInfo(id).isEntry) {
      // eslint-disable-next-line no-param-reassign
      code =
        getDynamicPackagesEntryIntro(dynamicRequireModuleDirPaths, dynamicRequireModuleSet) + code;
    }

    const { isEsModule, hasDefaultExport, hasNamedExports, ast } = analyzeTopLevelStatements(
      this.parse,
      code,
      id
    );
    if (hasDefaultExport) {
      esModulesWithDefaultExport.add(id);
    }
    if (hasNamedExports) {
      esModulesWithNamedExports.add(id);
    }

    if (
      !dynamicRequireModuleSet.has(normalizePathSlashes(id)) &&
      (!hasCjsKeywords(code, ignoreGlobal) || (isEsModule && !options.transformMixedEsModules))
    ) {
      return { meta: { commonjs: { isCommonJS: false } } };
    }

    // avoid wrapping as this is a commonjsRegister call
    const disableWrap = isWrappedId(id, DYNAMIC_REGISTER_SUFFIX);
    if (disableWrap) {
      // eslint-disable-next-line no-param-reassign
      id = unwrapId(id, DYNAMIC_REGISTER_SUFFIX);
    }

    return transformCommonjs(
      this.parse,
      code,
      id,
      isEsModule,
      ignoreGlobal || isEsModule,
      ignoreRequire,
      ignoreDynamicRequires && !isDynamicRequireModulesEnabled,
      getIgnoreTryCatchRequireStatementMode,
      sourceMap,
      isDynamicRequireModulesEnabled,
      dynamicRequireModuleSet,
      disableWrap,
      commonDir,
      ast,
      defaultIsModuleExports
    );
  }

  return {
    name: 'commonjs',

    buildStart() {
      validateRollupVersion(this.meta.rollupVersion, peerDependencies.rollup);
      if (options.namedExports != null) {
        this.warn(
          'The namedExports option from "@rollup/plugin-commonjs" is deprecated. Named exports are now handled automatically.'
        );
      }
    },

    resolveId,

    load(id) {
      if (id === HELPERS_ID) {
        return getHelpersModule(isDynamicRequireModulesEnabled, ignoreDynamicRequires);
      }

      if (id.startsWith(HELPERS_ID)) {
        return getSpecificHelperProxy(id);
      }

      if (isWrappedId(id, MODULE_SUFFIX)) {
        const actualId = unwrapId(id, MODULE_SUFFIX);
        let name = getName(actualId);
        let code;
        if (isDynamicRequireModulesEnabled) {
          if (['modulePath', 'commonjsRequire', 'createModule'].includes(name)) {
            name = `${name}_`;
          }
          code =
            `import {commonjsRequire, createModule} from "${HELPERS_ID}";\n` +
            `var ${name} = createModule(${JSON.stringify(
              getVirtualPathForDynamicRequirePath(dirname(actualId), commonDir)
            )});\n` +
            `export {${name} as __module}`;
        } else {
          code = `var ${name} = {exports: {}}; export {${name} as __module}`;
        }
        return {
          code,
          syntheticNamedExports: '__module',
          meta: { commonjs: { isCommonJS: false } }
        };
      }

      if (isWrappedId(id, EXPORTS_SUFFIX)) {
        const actualId = unwrapId(id, EXPORTS_SUFFIX);
        const name = getName(actualId);
        return {
          code: `var ${name} = {}; export {${name} as __exports}`,
          meta: { commonjs: { isCommonJS: false } }
        };
      }

      if (isWrappedId(id, EXTERNAL_SUFFIX)) {
        const actualId = unwrapId(id, EXTERNAL_SUFFIX);
        return getUnknownRequireProxy(
          actualId,
          isEsmExternal(actualId) ? getRequireReturnsDefault(actualId) : true
        );
      }

      if (id === DYNAMIC_PACKAGES_ID) {
        return getDynamicPackagesModule(dynamicRequireModuleDirPaths, commonDir);
      }

      if (id.startsWith(DYNAMIC_JSON_PREFIX)) {
        return getDynamicJsonProxy(id, commonDir);
      }

      if (isDynamicModuleImport(id, dynamicRequireModuleSet)) {
        return `export default require(${JSON.stringify(normalizePathSlashes(id))});`;
      }

      if (isWrappedId(id, DYNAMIC_REGISTER_SUFFIX)) {
        return getDynamicRequireProxy(
          normalizePathSlashes(unwrapId(id, DYNAMIC_REGISTER_SUFFIX)),
          commonDir
        );
      }

      if (isWrappedId(id, PROXY_SUFFIX)) {
        const actualId = unwrapId(id, PROXY_SUFFIX);
        return getStaticRequireProxy(
          actualId,
          getRequireReturnsDefault(actualId),
          esModulesWithDefaultExport,
          esModulesWithNamedExports,
          commonJsMetaPromises
        );
      }

      return null;
    },

    transform(code, rawId) {
      let id = rawId;

      if (isWrappedId(id, DYNAMIC_REGISTER_SUFFIX)) {
        id = unwrapId(id, DYNAMIC_REGISTER_SUFFIX);
      }

      const extName = extname(id);
      if (
        extName !== '.cjs' &&
        id !== DYNAMIC_PACKAGES_ID &&
        !id.startsWith(DYNAMIC_JSON_PREFIX) &&
        (!filter(id) || !extensions.includes(extName))
      ) {
        return null;
      }

      try {
        return transformAndCheckExports.call(this, code, rawId);
      } catch (err) {
        return this.error(err, err.loc);
      }
    },

    moduleParsed({ id, meta: { commonjs: commonjsMeta } }) {
      if (commonjsMeta && commonjsMeta.isCommonJS != null) {
        setCommonJSMetaPromise(commonJsMetaPromises, id, commonjsMeta);
        return;
      }
      setCommonJSMetaPromise(commonJsMetaPromises, id, null);
    }
  };
}

export { commonjs as default };
//# sourceMappingURL=index.es.js.map
