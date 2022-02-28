'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.recursivePatternCapture = recursivePatternCapture;

var _fs = require('fs');

var _fs2 = _interopRequireDefault(_fs);

var _doctrine = require('doctrine');

var _doctrine2 = _interopRequireDefault(_doctrine);

var _debug = require('debug');

var _debug2 = _interopRequireDefault(_debug);

var _sourceCode = require('eslint/lib/util/source-code');

var _sourceCode2 = _interopRequireDefault(_sourceCode);

var _parse = require('eslint-module-utils/parse');

var _parse2 = _interopRequireDefault(_parse);

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _ignore = require('eslint-module-utils/ignore');

var _ignore2 = _interopRequireDefault(_ignore);

var _hash = require('eslint-module-utils/hash');

var _unambiguous = require('eslint-module-utils/unambiguous');

var unambiguous = _interopRequireWildcard(_unambiguous);

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } else { var newObj = {}; if (obj != null) { for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) newObj[key] = obj[key]; } } newObj.default = obj; return newObj; } }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const log = (0, _debug2.default)('eslint-plugin-import:ExportMap');

const exportCache = new Map();

class ExportMap {
  constructor(path) {
    this.path = path;
    this.namespace = new Map();
    // todo: restructure to key on path, value is resolver + map of names
    this.reexports = new Map();
    /**
     * star-exports
     * @type {Set} of () => ExportMap
     */
    this.dependencies = new Set();
    /**
     * dependencies of this module that are not explicitly re-exported
     * @type {Map} from path = () => ExportMap
     */
    this.imports = new Map();
    this.errors = [];
  }

  get hasDefault() {
    return this.get('default') != null;
  } // stronger than this.has

  get size() {
    let size = this.namespace.size + this.reexports.size;
    this.dependencies.forEach(dep => {
      const d = dep();
      // CJS / ignored dependencies won't exist (#717)
      if (d == null) return;
      size += d.size;
    });
    return size;
  }

  /**
   * Note that this does not check explicitly re-exported names for existence
   * in the base namespace, but it will expand all `export * from '...'` exports
   * if not found in the explicit namespace.
   * @param  {string}  name
   * @return {Boolean} true if `name` is exported by this module.
   */
  has(name) {
    if (this.namespace.has(name)) return true;
    if (this.reexports.has(name)) return true;

    // default exports must be explicitly re-exported (#328)
    if (name !== 'default') {
      for (let dep of this.dependencies) {
        let innerMap = dep();

        // todo: report as unresolved?
        if (!innerMap) continue;

        if (innerMap.has(name)) return true;
      }
    }

    return false;
  }

  /**
   * ensure that imported name fully resolves.
   * @param  {[type]}  name [description]
   * @return {Boolean}      [description]
   */
  hasDeep(name) {
    if (this.namespace.has(name)) return { found: true, path: [this] };

    if (this.reexports.has(name)) {
      const reexports = this.reexports.get(name),
            imported = reexports.getImport();

      // if import is ignored, return explicit 'null'
      if (imported == null) return { found: true, path: [this]

        // safeguard against cycles, only if name matches
      };if (imported.path === this.path && reexports.local === name) {
        return { found: false, path: [this] };
      }

      const deep = imported.hasDeep(reexports.local);
      deep.path.unshift(this);

      return deep;
    }

    // default exports must be explicitly re-exported (#328)
    if (name !== 'default') {
      for (let dep of this.dependencies) {
        let innerMap = dep();
        // todo: report as unresolved?
        if (!innerMap) continue;

        // safeguard against cycles
        if (innerMap.path === this.path) continue;

        let innerValue = innerMap.hasDeep(name);
        if (innerValue.found) {
          innerValue.path.unshift(this);
          return innerValue;
        }
      }
    }

    return { found: false, path: [this] };
  }

  get(name) {
    if (this.namespace.has(name)) return this.namespace.get(name);

    if (this.reexports.has(name)) {
      const reexports = this.reexports.get(name),
            imported = reexports.getImport();

      // if import is ignored, return explicit 'null'
      if (imported == null) return null;

      // safeguard against cycles, only if name matches
      if (imported.path === this.path && reexports.local === name) return undefined;

      return imported.get(reexports.local);
    }

    // default exports must be explicitly re-exported (#328)
    if (name !== 'default') {
      for (let dep of this.dependencies) {
        let innerMap = dep();
        // todo: report as unresolved?
        if (!innerMap) continue;

        // safeguard against cycles
        if (innerMap.path === this.path) continue;

        let innerValue = innerMap.get(name);
        if (innerValue !== undefined) return innerValue;
      }
    }

    return undefined;
  }

  forEach(callback, thisArg) {
    this.namespace.forEach((v, n) => callback.call(thisArg, v, n, this));

    this.reexports.forEach((reexports, name) => {
      const reexported = reexports.getImport();
      // can't look up meta for ignored re-exports (#348)
      callback.call(thisArg, reexported && reexported.get(reexports.local), name, this);
    });

    this.dependencies.forEach(dep => {
      const d = dep();
      // CJS / ignored dependencies won't exist (#717)
      if (d == null) return;

      d.forEach((v, n) => n !== 'default' && callback.call(thisArg, v, n, this));
    });
  }

  // todo: keys, values, entries?

  reportErrors(context, declaration) {
    context.report({
      node: declaration.source,
      message: `Parse errors in imported module '${declaration.source.value}': ` + `${this.errors.map(e => `${e.message} (${e.lineNumber}:${e.column})`).join(', ')}`
    });
  }
}

exports.default = ExportMap; /**
                              * parse docs from the first node that has leading comments
                              */

function captureDoc(source, docStyleParsers) {
  const metadata = {};

  // 'some' short-circuits on first 'true'

  for (var _len = arguments.length, nodes = Array(_len > 2 ? _len - 2 : 0), _key = 2; _key < _len; _key++) {
    nodes[_key - 2] = arguments[_key];
  }

  nodes.some(n => {
    try {

      let leadingComments;

      // n.leadingComments is legacy `attachComments` behavior
      if ('leadingComments' in n) {
        leadingComments = n.leadingComments;
      } else if (n.range) {
        leadingComments = source.getCommentsBefore(n);
      }

      if (!leadingComments || leadingComments.length === 0) return false;

      for (let name in docStyleParsers) {
        const doc = docStyleParsers[name](leadingComments);
        if (doc) {
          metadata.doc = doc;
        }
      }

      return true;
    } catch (err) {
      return false;
    }
  });

  return metadata;
}

const availableDocStyleParsers = {
  jsdoc: captureJsDoc,
  tomdoc: captureTomDoc

  /**
   * parse JSDoc from leading comments
   * @param  {...[type]} comments [description]
   * @return {{doc: object}}
   */
};function captureJsDoc(comments) {
  let doc;

  // capture XSDoc
  comments.forEach(comment => {
    // skip non-block comments
    if (comment.value.slice(0, 4) !== '*\n *') return;
    try {
      doc = _doctrine2.default.parse(comment.value, { unwrap: true });
    } catch (err) {
      /* don't care, for now? maybe add to `errors?` */
    }
  });

  return doc;
}

/**
  * parse TomDoc section from comments
  */
function captureTomDoc(comments) {
  // collect lines up to first paragraph break
  const lines = [];
  for (let i = 0; i < comments.length; i++) {
    const comment = comments[i];
    if (comment.value.match(/^\s*$/)) break;
    lines.push(comment.value.trim());
  }

  // return doctrine-like object
  const statusMatch = lines.join(' ').match(/^(Public|Internal|Deprecated):\s*(.+)/);
  if (statusMatch) {
    return {
      description: statusMatch[2],
      tags: [{
        title: statusMatch[1].toLowerCase(),
        description: statusMatch[2]
      }]
    };
  }
}

ExportMap.get = function (source, context) {
  const path = (0, _resolve2.default)(source, context);
  if (path == null) return null;

  return ExportMap.for(childContext(path, context));
};

ExportMap.for = function (context) {
  const path = context.path;


  const cacheKey = (0, _hash.hashObject)(context).digest('hex');
  let exportMap = exportCache.get(cacheKey);

  // return cached ignore
  if (exportMap === null) return null;

  const stats = _fs2.default.statSync(path);
  if (exportMap != null) {
    // date equality check
    if (exportMap.mtime - stats.mtime === 0) {
      return exportMap;
    }
    // future: check content equality?
  }

  // check valid extensions first
  if (!(0, _ignore.hasValidExtension)(path, context)) {
    exportCache.set(cacheKey, null);
    return null;
  }

  const content = _fs2.default.readFileSync(path, { encoding: 'utf8' });

  // check for and cache ignore
  if ((0, _ignore2.default)(path, context) || !unambiguous.test(content)) {
    log('ignored path due to unambiguous regex or ignore settings:', path);
    exportCache.set(cacheKey, null);
    return null;
  }

  log('cache miss', cacheKey, 'for path', path);
  exportMap = ExportMap.parse(path, content, context);

  // ambiguous modules return null
  if (exportMap == null) return null;

  exportMap.mtime = stats.mtime;

  exportCache.set(cacheKey, exportMap);
  return exportMap;
};

ExportMap.parse = function (path, content, context) {
  var m = new ExportMap(path);

  try {
    var ast = (0, _parse2.default)(path, content, context);
  } catch (err) {
    log('parse error:', path, err);
    m.errors.push(err);
    return m; // can't continue
  }

  if (!unambiguous.isModule(ast)) return null;

  const docstyle = context.settings && context.settings['import/docstyle'] || ['jsdoc'];
  const docStyleParsers = {};
  docstyle.forEach(style => {
    docStyleParsers[style] = availableDocStyleParsers[style];
  });

  // attempt to collect module doc
  if (ast.comments) {
    ast.comments.some(c => {
      if (c.type !== 'Block') return false;
      try {
        const doc = _doctrine2.default.parse(c.value, { unwrap: true });
        if (doc.tags.some(t => t.title === 'module')) {
          m.doc = doc;
          return true;
        }
      } catch (err) {/* ignore */}
      return false;
    });
  }

  const namespaces = new Map();

  function remotePath(value) {
    return _resolve2.default.relative(value, path, context.settings);
  }

  function resolveImport(value) {
    const rp = remotePath(value);
    if (rp == null) return null;
    return ExportMap.for(childContext(rp, context));
  }

  function getNamespace(identifier) {
    if (!namespaces.has(identifier.name)) return;

    return function () {
      return resolveImport(namespaces.get(identifier.name));
    };
  }

  function addNamespace(object, identifier) {
    const nsfn = getNamespace(identifier);
    if (nsfn) {
      Object.defineProperty(object, 'namespace', { get: nsfn });
    }

    return object;
  }

  function captureDependency(declaration) {
    if (declaration.source == null) return null;

    const p = remotePath(declaration.source.value);
    if (p == null) return null;
    const existing = m.imports.get(p);
    if (existing != null) return existing.getter;

    const getter = thunkFor(p, context);
    m.imports.set(p, {
      getter,
      source: { // capturing actual node reference holds full AST in memory!
        value: declaration.source.value,
        loc: declaration.source.loc
      }
    });
    return getter;
  }

  const source = makeSourceCode(content, ast);

  ast.body.forEach(function (n) {

    if (n.type === 'ExportDefaultDeclaration') {
      const exportMeta = captureDoc(source, docStyleParsers, n);
      if (n.declaration.type === 'Identifier') {
        addNamespace(exportMeta, n.declaration);
      }
      m.namespace.set('default', exportMeta);
      return;
    }

    if (n.type === 'ExportAllDeclaration') {
      const getter = captureDependency(n);
      if (getter) m.dependencies.add(getter);
      return;
    }

    // capture namespaces in case of later export
    if (n.type === 'ImportDeclaration') {
      captureDependency(n);
      let ns;
      if (n.specifiers.some(s => s.type === 'ImportNamespaceSpecifier' && (ns = s))) {
        namespaces.set(ns.local.name, n.source.value);
      }
      return;
    }

    if (n.type === 'ExportNamedDeclaration') {
      // capture declaration
      if (n.declaration != null) {
        switch (n.declaration.type) {
          case 'FunctionDeclaration':
          case 'ClassDeclaration':
          case 'TypeAlias': // flowtype with babel-eslint parser
          case 'InterfaceDeclaration':
          case 'TSEnumDeclaration':
          case 'TSInterfaceDeclaration':
          case 'TSAbstractClassDeclaration':
          case 'TSModuleDeclaration':
            m.namespace.set(n.declaration.id.name, captureDoc(source, docStyleParsers, n));
            break;
          case 'VariableDeclaration':
            n.declaration.declarations.forEach(d => recursivePatternCapture(d.id, id => m.namespace.set(id.name, captureDoc(source, docStyleParsers, d, n))));
            break;
        }
      }

      const nsource = n.source && n.source.value;
      n.specifiers.forEach(s => {
        const exportMeta = {};
        let local;

        switch (s.type) {
          case 'ExportDefaultSpecifier':
            if (!n.source) return;
            local = 'default';
            break;
          case 'ExportNamespaceSpecifier':
            m.namespace.set(s.exported.name, Object.defineProperty(exportMeta, 'namespace', {
              get() {
                return resolveImport(nsource);
              }
            }));
            return;
          case 'ExportSpecifier':
            if (!n.source) {
              m.namespace.set(s.exported.name, addNamespace(exportMeta, s.local));
              return;
            }
          // else falls through
          default:
            local = s.local.name;
            break;
        }

        // todo: JSDoc
        m.reexports.set(s.exported.name, { local, getImport: () => resolveImport(nsource) });
      });
    }
  });

  return m;
};

/**
 * The creation of this closure is isolated from other scopes
 * to avoid over-retention of unrelated variables, which has
 * caused memory leaks. See #1266.
 */
function thunkFor(p, context) {
  return () => ExportMap.for(childContext(p, context));
}

/**
 * Traverse a pattern/identifier node, calling 'callback'
 * for each leaf identifier.
 * @param  {node}   pattern
 * @param  {Function} callback
 * @return {void}
 */
function recursivePatternCapture(pattern, callback) {
  switch (pattern.type) {
    case 'Identifier':
      // base case
      callback(pattern);
      break;

    case 'ObjectPattern':
      pattern.properties.forEach(p => {
        recursivePatternCapture(p.value, callback);
      });
      break;

    case 'ArrayPattern':
      pattern.elements.forEach(element => {
        if (element == null) return;
        recursivePatternCapture(element, callback);
      });
      break;

    case 'AssignmentPattern':
      callback(pattern.left);
      break;
  }
}

/**
 * don't hold full context object in memory, just grab what we need.
 */
function childContext(path, context) {
  const settings = context.settings,
        parserOptions = context.parserOptions,
        parserPath = context.parserPath;

  return {
    settings,
    parserOptions,
    parserPath,
    path
  };
}

/**
 * sometimes legacy support isn't _that_ hard... right?
 */
function makeSourceCode(text, ast) {
  if (_sourceCode2.default.length > 1) {
    // ESLint 3
    return new _sourceCode2.default(text, ast);
  } else {
    // ESLint 4, 5
    return new _sourceCode2.default({ text, ast });
  }
}
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIkV4cG9ydE1hcC5qcyJdLCJuYW1lcyI6WyJyZWN1cnNpdmVQYXR0ZXJuQ2FwdHVyZSIsInVuYW1iaWd1b3VzIiwibG9nIiwiZXhwb3J0Q2FjaGUiLCJNYXAiLCJFeHBvcnRNYXAiLCJjb25zdHJ1Y3RvciIsInBhdGgiLCJuYW1lc3BhY2UiLCJyZWV4cG9ydHMiLCJkZXBlbmRlbmNpZXMiLCJTZXQiLCJpbXBvcnRzIiwiZXJyb3JzIiwiaGFzRGVmYXVsdCIsImdldCIsInNpemUiLCJmb3JFYWNoIiwiZGVwIiwiZCIsImhhcyIsIm5hbWUiLCJpbm5lck1hcCIsImhhc0RlZXAiLCJmb3VuZCIsImltcG9ydGVkIiwiZ2V0SW1wb3J0IiwibG9jYWwiLCJkZWVwIiwidW5zaGlmdCIsImlubmVyVmFsdWUiLCJ1bmRlZmluZWQiLCJjYWxsYmFjayIsInRoaXNBcmciLCJ2IiwibiIsImNhbGwiLCJyZWV4cG9ydGVkIiwicmVwb3J0RXJyb3JzIiwiY29udGV4dCIsImRlY2xhcmF0aW9uIiwicmVwb3J0Iiwibm9kZSIsInNvdXJjZSIsIm1lc3NhZ2UiLCJ2YWx1ZSIsIm1hcCIsImUiLCJsaW5lTnVtYmVyIiwiY29sdW1uIiwiam9pbiIsImNhcHR1cmVEb2MiLCJkb2NTdHlsZVBhcnNlcnMiLCJtZXRhZGF0YSIsIm5vZGVzIiwic29tZSIsImxlYWRpbmdDb21tZW50cyIsInJhbmdlIiwiZ2V0Q29tbWVudHNCZWZvcmUiLCJsZW5ndGgiLCJkb2MiLCJlcnIiLCJhdmFpbGFibGVEb2NTdHlsZVBhcnNlcnMiLCJqc2RvYyIsImNhcHR1cmVKc0RvYyIsInRvbWRvYyIsImNhcHR1cmVUb21Eb2MiLCJjb21tZW50cyIsImNvbW1lbnQiLCJzbGljZSIsImRvY3RyaW5lIiwicGFyc2UiLCJ1bndyYXAiLCJsaW5lcyIsImkiLCJtYXRjaCIsInB1c2giLCJ0cmltIiwic3RhdHVzTWF0Y2giLCJkZXNjcmlwdGlvbiIsInRhZ3MiLCJ0aXRsZSIsInRvTG93ZXJDYXNlIiwiZm9yIiwiY2hpbGRDb250ZXh0IiwiY2FjaGVLZXkiLCJkaWdlc3QiLCJleHBvcnRNYXAiLCJzdGF0cyIsImZzIiwic3RhdFN5bmMiLCJtdGltZSIsInNldCIsImNvbnRlbnQiLCJyZWFkRmlsZVN5bmMiLCJlbmNvZGluZyIsInRlc3QiLCJtIiwiYXN0IiwiaXNNb2R1bGUiLCJkb2NzdHlsZSIsInNldHRpbmdzIiwic3R5bGUiLCJjIiwidHlwZSIsInQiLCJuYW1lc3BhY2VzIiwicmVtb3RlUGF0aCIsInJlc29sdmUiLCJyZWxhdGl2ZSIsInJlc29sdmVJbXBvcnQiLCJycCIsImdldE5hbWVzcGFjZSIsImlkZW50aWZpZXIiLCJhZGROYW1lc3BhY2UiLCJvYmplY3QiLCJuc2ZuIiwiT2JqZWN0IiwiZGVmaW5lUHJvcGVydHkiLCJjYXB0dXJlRGVwZW5kZW5jeSIsInAiLCJleGlzdGluZyIsImdldHRlciIsInRodW5rRm9yIiwibG9jIiwibWFrZVNvdXJjZUNvZGUiLCJib2R5IiwiZXhwb3J0TWV0YSIsImFkZCIsIm5zIiwic3BlY2lmaWVycyIsInMiLCJpZCIsImRlY2xhcmF0aW9ucyIsIm5zb3VyY2UiLCJleHBvcnRlZCIsInBhdHRlcm4iLCJwcm9wZXJ0aWVzIiwiZWxlbWVudHMiLCJlbGVtZW50IiwibGVmdCIsInBhcnNlck9wdGlvbnMiLCJwYXJzZXJQYXRoIiwidGV4dCIsIlNvdXJjZUNvZGUiXSwibWFwcGluZ3MiOiI7Ozs7O1FBdWdCZ0JBLHVCLEdBQUFBLHVCOztBQXZnQmhCOzs7O0FBRUE7Ozs7QUFFQTs7OztBQUVBOzs7O0FBRUE7Ozs7QUFDQTs7OztBQUNBOzs7O0FBRUE7O0FBQ0E7O0lBQVlDLFc7Ozs7OztBQUVaLE1BQU1DLE1BQU0scUJBQU0sZ0NBQU4sQ0FBWjs7QUFFQSxNQUFNQyxjQUFjLElBQUlDLEdBQUosRUFBcEI7O0FBRWUsTUFBTUMsU0FBTixDQUFnQjtBQUM3QkMsY0FBWUMsSUFBWixFQUFrQjtBQUNoQixTQUFLQSxJQUFMLEdBQVlBLElBQVo7QUFDQSxTQUFLQyxTQUFMLEdBQWlCLElB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QUFXLElBQWYsRUFBcUI7QUFDckIzSSxnQ0FBd0IySSxPQUF4QixFQUFpQzNHLFFBQWpDO0FBQ0QsT0FIRDtBQUlBOztBQUVGLFNBQUssbUJBQUw7QUFDRUEsZUFBU3dHLFFBQVFJLElBQWpCO0FBQ0E7QUFwQko7QUFzQkQ7O0FBRUQ7OztBQUdBLFNBQVN4RCxZQUFULENBQXNCN0UsSUFBdEIsRUFBNEJnQyxPQUE1QixFQUFxQztBQUFBLFFBQzNCOEQsUUFEMkIsR0FDYTlELE9BRGIsQ0FDM0I4RCxRQUQyQjtBQUFBLFFBQ2pCd0MsYUFEaUIsR0FDYXRHLE9BRGIsQ0FDakJzRyxhQURpQjtBQUFBLFFBQ0ZDLFVBREUsR0FDYXZHLE9BRGIsQ0FDRnVHLFVBREU7O0FBRW5DLFNBQU87QUFDTHpDLFlBREs7QUFFTHdDLGlCQUZLO0FBR0xDLGNBSEs7QUFJTHZJO0FBSkssR0FBUDtBQU1EOztBQUdEOzs7QUFHQSxTQUFTc0gsY0FBVCxDQUF3QmtCLElBQXhCLEVBQThCN0MsR0FBOUIsRUFBbUM7QUFDakMsTUFBSThDLHFCQUFXckYsTUFBWCxHQUFvQixDQUF4QixFQUEyQjtBQUN6QjtBQUNBLFdBQU8sSUFBSXFGLG9CQUFKLENBQWVELElBQWYsRUFBcUI3QyxHQUFyQixDQUFQO0FBQ0QsR0FIRCxNQUdPO0FBQ0w7QUFDQSxXQUFPLElBQUk4QyxvQkFBSixDQUFlLEVBQUVELElBQUYsRUFBUTdDLEdBQVIsRUFBZixDQUFQO0FBQ0Q7QUFDRiIsImZpbGUiOiJFeHBvcnRNYXAuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZnMgZnJvbSAnZnMnXG5cbmltcG9ydCBkb2N0cmluZSBmcm9tICdkb2N0cmluZSdcblxuaW1wb3J0IGRlYnVnIGZyb20gJ2RlYnVnJ1xuXG5pbXBvcnQgU291cmNlQ29kZSBmcm9tICdlc2xpbnQvbGliL3V0aWwvc291cmNlLWNvZGUnXG5cbmltcG9ydCBwYXJzZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3BhcnNlJ1xuaW1wb3J0IHJlc29sdmUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9yZXNvbHZlJ1xuaW1wb3J0IGlzSWdub3JlZCwgeyBoYXNWYWxpZEV4dGVuc2lvbiB9IGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvaWdub3JlJ1xuXG5pbXBvcnQgeyBoYXNoT2JqZWN0IH0gZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9oYXNoJ1xuaW1wb3J0ICogYXMgdW5hbWJpZ3VvdXMgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy91bmFtYmlndW91cydcblxuY29uc3QgbG9nID0gZGVidWcoJ2VzbGludC1wbHVnaW4taW1wb3J0OkV4cG9ydE1hcCcpXG5cbmNvbnN0IGV4cG9ydENhY2hlID0gbmV3IE1hcCgpXG5cbmV4cG9ydCBkZWZhdWx0IGNsYXNzIEV4cG9ydE1hcCB7XG4gIGNvbnN0cnVjdG9yKHBhdGgpIHtcbiAgICB0aGlzLnBhdGggPSBwYXRoXG4gICAgdGhpcy5uYW1lc3BhY2UgPSBuZXcgTWFwKClcbiAgICAvLyB0b2RvOiByZXN0cnVjdHVyZSB0byBrZXkgb24gcGF0aCwgdmFsdWUgaXMgcmVzb2x2ZXIgKyBtYXAgb2YgbmFtZXNcbiAgICB0aGlzLnJlZXhwb3J0cyA9IG5ldyBNYXAoKVxuICAgIC8qKlxuICAgICAqIHN0YXItZXhwb3J0c1xuICAgICAqIEB0eXBlIHtTZXR9IG9mICgpID0+IEV4cG9ydE1hcFxuICAgICAqL1xuICAgIHRoaXMuZGVwZW5kZW5jaWVzID0gbmV3IFNldCgpXG4gICAgLyoqXG4gICAgICogZGVwZW5kZW5jaWVzIG9mIHRoaXMgbW9kdWxlIHRoYXQgYXJlIG5vdCBleHBsaWNpdGx5IHJlLWV4cG9ydGVkXG4gICAgICogQHR5cGUge01hcH0gZnJvbSBwYXRoID0gKCkgPT4gRXhwb3J0TWFwXG4gICAgICovXG4gICAgdGhpcy5pbXBvcnRzID0gbmV3IE1hcCgpXG4gICAgdGhpcy5lcnJvcnMgPSBbXVxuICB9XG5cbiAgZ2V0IGhhc0RlZmF1bHQoKSB7IHJldHVybiB0aGlzLmdldCgnZGVmYXVsdCcpICE9IG51bGwgfSAvLyBzdHJvbmdlciB0aGFuIHRoaXMuaGFzXG5cbiAgZ2V0IHNpemUoKSB7XG4gICAgbGV0IHNpemUgPSB0aGlzLm5hbWVzcGFjZS5zaXplICsgdGhpcy5yZWV4cG9ydHMuc2l6ZVxuICAgIHRoaXMuZGVwZW5kZW5jaWVzLmZvckVhY2goZGVwID0+IHtcbiAgICAgIGNvbnN0IGQgPSBkZXAoKVxuICAgICAgLy8gQ0pTIC8gaWdub3JlZCBkZXBlbmRlbmNpZXMgd29uJ3QgZXhpc3QgKCM3MTcpXG4gICAgICBpZiAoZCA9PSBudWxsKSByZXR1cm5cbiAgICAgIHNpemUgKz0gZC5zaXplXG4gICAgfSlcbiAgICByZXR1cm4gc2l6ZVxuICB9XG5cbiAgLyoqXG4gICAqIE5vdGUgdGhhdCB0aGlzIGRvZXMgbm90IGNoZWNrIGV4cGxpY2l0bHkgcmUtZXhwb3J0ZWQgbmFtZXMgZm9yIGV4aXN0ZW5jZVxuICAgKiBpbiB0aGUgYmFzZSBuYW1lc3BhY2UsIGJ1dCBpdCB3aWxsIGV4cGFuZCBhbGwgYGV4cG9ydCAqIGZyb20gJy4uLidgIGV4cG9ydHNcbiAgICogaWYgbm90IGZvdW5kIGluIHRoZSBleHBsaWNpdCBuYW1lc3BhY2UuXG4gICAqIEBwYXJhbSAge3N0cmluZ30gIG5hbWVcbiAgICogQHJldHVybiB7Qm9vbGVhbn0gdHJ1ZSBpZiBgbmFtZWAgaXMgZXhwb3J0ZWQgYnkgdGhpcyBtb2R1bGUuXG4gICAqL1xuICBoYXMobmFtZSkge1xuICAgIGlmICh0aGlzLm5hbWVzcGFjZS5oYXMobmFtZSkpIHJldHVybiB0cnVlXG4gICAgaWYgKHRoaXMucmVleHBvcnRzLmhhcyhuYW1lKSkgcmV0dXJuIHRydWVcblxuICAgIC8vIGRlZmF1bHQgZXhwb3J0cyBtdXN0IGJlIGV4cGxpY2l0bHkgcmUtZXhwb3J0ZWQgKCMzMjgpXG4gICAgaWYgKG5hbWUgIT09ICdkZWZhdWx0Jykge1xuICAgICAgZm9yIChsZXQgZGVwIG9mIHRoaXMuZGVwZW5kZW5jaWVzKSB7XG4gICAgICAgIGxldCBpbm5lck1hcCA9IGRlcCgpXG5cbiAgICAgICAgLy8gdG9kbzogcmVwb3J0IGFzIHVucmVzb2x2ZWQ/XG4gICAgICAgIGlmICghaW5uZXJNYXApIGNvbnRpbnVlXG5cbiAgICAgICAgaWYgKGlubmVyTWFwLmhhcyhuYW1lKSkgcmV0dXJuIHRydWVcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gZmFsc2VcbiAgfVxuXG4gIC8qKlxuICAgKiBlbnN1cmUgdGhhdCBpbXBvcnRlZCBuYW1lIGZ1bGx5IHJlc29sdmVzLlxuICAgKiBAcGFyYW0gIHtbdHlwZV19ICBuYW1lIFtkZXNjcmlwdGlvbl1cbiAgICogQHJldHVybiB7Qm9vbGVhbn0gICAgICBbZGVzY3JpcHRpb25dXG4gICAqL1xuICBoYXNEZWVwKG5hbWUpIHtcbiAgICBpZiAodGhpcy5uYW1lc3BhY2UuaGFzKG5hbWUpKSByZXR1cm4geyBmb3VuZDogdHJ1ZSwgcGF0aDogW3RoaXNdIH1cblxuICAgIGlmICh0aGlzLnJlZXhwb3J0cy5oYXMobmFtZSkpIHtcbiAgICAgIGNvbnN0IHJlZXhwb3J0cyA9IHRoaXMucmVleHBvcnRzLmdldChuYW1lKVxuICAgICAgICAgICwgaW1wb3J0ZWQgPSByZWV4cG9ydHMuZ2V0SW1wb3J0KClcblxuICAgICAgLy8gaWYgaW1wb3J0IGlzIGlnbm9yZWQsIHJldHVybiBleHBsaWNpdCAnbnVsbCdcbiAgICAgIGlmIChpbXBvcnRlZCA9PSBudWxsKSByZXR1cm4geyBmb3VuZDogdHJ1ZSwgcGF0aDogW3RoaXNdIH1cblxuICAgICAgLy8gc2FmZWd1YXJkIGFnYWluc3QgY3ljbGVzLCBvbmx5IGlmIG5hbWUgbWF0Y2hlc1xuICAgICAgaWYgKGltcG9ydGVkLnBhdGggPT09IHRoaXMucGF0aCAmJiByZWV4cG9ydHMubG9jYWwgPT09IG5hbWUpIHtcbiAgICAgICAgcmV0dXJuIHsgZm91bmQ6IGZhbHNlLCBwYXRoOiBbdGhpc10gfVxuICAgICAgfVxuXG4gICAgICBjb25zdCBkZWVwID0gaW1wb3J0ZWQuaGFzRGVlcChyZWV4cG9ydHMubG9jYWwpXG4gICAgICBkZWVwLnBhdGgudW5zaGlmdCh0aGlzKVxuXG4gICAgICByZXR1cm4gZGVlcFxuICAgIH1cblxuXG4gICAgLy8gZGVmYXVsdCBleHBvcnRzIG11c3QgYmUgZXhwbGljaXRseSByZS1leHBvcnRlZCAoIzMyOClcbiAgICBpZiAobmFtZSAhPT0gJ2RlZmF1bHQnKSB7XG4gICAgICBmb3IgKGxldCBkZXAgb2YgdGhpcy5kZXBlbmRlbmNpZXMpIHtcbiAgICAgICAgbGV0IGlubmVyTWFwID0gZGVwKClcbiAgICAgICAgLy8gdG9kbzogcmVwb3J0IGFzIHVucmVzb2x2ZWQ/XG4gICAgICAgIGlmICghaW5uZXJNYXApIGNvbnRpbnVlXG5cbiAgICAgICAgLy8gc2FmZWd1YXJkIGFnYWluc3QgY3ljbGVzXG4gICAgICAgIGlmIChpbm5lck1hcC5wYXRoID09PSB0aGlzLnBhdGgpIGNvbnRpbnVlXG5cbiAgICAgICAgbGV0IGlubmVyVmFsdWUgPSBpbm5lck1hcC5oYXNEZWVwKG5hbWUpXG4gICAgICAgIGlmIChpbm5lclZhbHVlLmZvdW5kKSB7XG4gICAgICAgICAgaW5uZXJWYWx1ZS5wYXRoLnVuc2hpZnQodGhpcylcbiAgICAgICAgICByZXR1cm4gaW5uZXJWYWx1ZVxuICAgICAgICB9XG4gICAgICB9XG4gICAgfVxuXG4gICAgcmV0dXJuIHsgZm91bmQ6IGZhbHNlLCBwYXRoOiBbdGhpc10gfVxuICB9XG5cbiAgZ2V0KG5hbWUpIHtcbiAgICBpZiAodGhpcy5uYW1lc3BhY2UuaGFzKG5hbWUpKSByZXR1cm4gdGhpcy5uYW1lc3BhY2UuZ2V0KG5hbWUpXG5cbiAgICBpZiAodGhpcy5yZWV4cG9ydHMuaGFzKG5hbWUpKSB7XG4gICAgICBjb25zdCByZWV4cG9ydHMgPSB0aGlzLnJlZXhwb3J0cy5nZXQobmFtZSlcbiAgICAgICAgICAsIGltcG9ydGVkID0gcmVleHBvcnRzLmdldEltcG9ydCgpXG5cbiAgICAgIC8vIGlmIGltcG9ydCBpcyBpZ25vcmVkLCByZXR1cm4gZXhwbGljaXQgJ251bGwnXG4gICAgICBpZiAoaW1wb3J0ZWQgPT0gbnVsbCkgcmV0dXJuIG51bGxcblxuICAgICAgLy8gc2FmZWd1YXJkIGFnYWluc3QgY3ljbGVzLCBvbmx5IGlmIG5hbWUgbWF0Y2hlc1xuICAgICAgaWYgKGltcG9ydGVkLnBhdGggPT09IHRoaXMucGF0aCAmJiByZWV4cG9ydHMubG9jYWwgPT09IG5hbWUpIHJldHVybiB1bmRlZmluZWRcblxuICAgICAgcmV0dXJuIGltcG9ydGVkLmdldChyZWV4cG9ydHMubG9jYWwpXG4gICAgfVxuXG4gICAgLy8gZGVmYXVsdCBleHBvcnRzIG11c3QgYmUgZXhwbGljaXRseSByZS1leHBvcnRlZCAoIzMyOClcbiAgICBpZiAobmFtZSAhPT0gJ2RlZmF1bHQnKSB7XG4gICAgICBmb3IgKGxldCBkZXAgb2YgdGhpcy5kZXBlbmRlbmNpZXMpIHtcbiAgICAgICAgbGV0IGlubmVyTWFwID0gZGVwKClcbiAgICAgICAgLy8gdG9kbzogcmVwb3J0IGFzIHVucmVzb2x2ZWQ/XG4gICAgICAgIGlmICghaW5uZXJNYXApIGNvbnRpbnVlXG5cbiAgICAgICAgLy8gc2FmZWd1YXJkIGFnYWluc3QgY3ljbGVzXG4gICAgICAgIGlmIChpbm5lck1hcC5wYXRoID09PSB0aGlzLnBhdGgpIGNvbnRpbnVlXG5cbiAgICAgICAgbGV0IGlubmVyVmFsdWUgPSBpbm5lck1hcC5nZXQobmFtZSlcbiAgICAgICAgaWYgKGlubmVyVmFsdWUgIT09IHVuZGVmaW5lZCkgcmV0dXJuIGlubmVyVmFsdWVcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gdW5kZWZpbmVkXG4gIH1cblxuICBmb3JFYWNoKGNhbGxiYWNrLCB0aGlzQXJnKSB7XG4gICAgdGhpcy5uYW1lc3BhY2UuZm9yRWFjaCgodiwgbikgPT5cbiAgICAgIGNhbGxiYWNrLmNhbGwodGhpc0FyZywgdiwgbiwgdGhpcykpXG5cbiAgICB0aGlzLnJlZXhwb3J0cy5mb3JFYWNoKChyZWV4cG9ydHMsIG5hbWUpID0+IHtcbiAgICAgIGNvbnN0IHJlZXhwb3J0ZWQgPSByZWV4cG9ydHMuZ2V0SW1wb3J0KClcbiAgICAgIC8vIGNhbid0IGxvb2sgdXAgbWV0YSBmb3IgaWdub3JlZCByZS1leHBvcnRzICgjMzQ4KVxuICAgICAgY2FsbGJhY2suY2FsbCh0aGlzQXJnLCByZWV4cG9ydGVkICYmIHJlZXhwb3J0ZWQuZ2V0KHJlZXhwb3J0cy5sb2NhbCksIG5hbWUsIHRoaXMpXG4gICAgfSlcblxuICAgIHRoaXMuZGVwZW5kZW5jaWVzLmZvckVhY2goZGVwID0+IHtcbiAgICAgIGNvbnN0IGQgPSBkZXAoKVxuICAgICAgLy8gQ0pTIC8gaWdub3JlZCBkZXBlbmRlbmNpZXMgd29uJ3QgZXhpc3QgKCM3MTcpXG4gICAgICBpZiAoZCA9PSBudWxsKSByZXR1cm5cblxuICAgICAgZC5mb3JFYWNoKCh2LCBuKSA9PlxuICAgICAgICBuICE9PSAnZGVmYXVsdCcgJiYgY2FsbGJhY2suY2FsbCh0aGlzQXJnLCB2LCBuLCB0aGlzKSlcbiAgICB9KVxuICB9XG5cbiAgLy8gdG9kbzoga2V5cywgdmFsdWVzLCBlbnRyaWVzP1xuXG4gIHJlcG9ydEVycm9ycyhjb250ZXh0LCBkZWNsYXJhdGlvbikge1xuICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgIG5vZGU6IGRlY2xhcmF0aW9uLnNvdXJjZSxcbiAgICAgIG1lc3NhZ2U6IGBQYXJzZSBlcnJvcnMgaW4gaW1wb3J0ZWQgbW9kdWxlICcke2RlY2xhcmF0aW9uLnNvdXJjZS52YWx1ZX0nOiBgICtcbiAgICAgICAgICAgICAgICAgIGAke3RoaXMuZXJyb3JzXG4gICAgICAgICAgICAgICAgICAgICAgICAubWFwKGUgPT4gYCR7ZS5tZXNzYWdlfSAoJHtlLmxpbmVOdW1iZXJ9OiR7ZS5jb2x1bW59KWApXG4gICAgICAgICAgICAgICAgICAgICAgICAuam9pbignLCAnKX1gLFxuICAgIH0pXG4gIH1cbn1cblxuLyoqXG4gKiBwYXJzZSBkb2NzIGZyb20gdGhlIGZpcnN0IG5vZGUgdGhhdCBoYXMgbGVhZGluZyBjb21tZW50c1xuICovXG5mdW5jdGlvbiBjYXB0dXJlRG9jKHNvdXJjZSwgZG9jU3R5bGVQYXJzZXJzLCAuLi5ub2Rlcykge1xuICBjb25zdCBtZXRhZGF0YSA9IHt9XG5cbiAgLy8gJ3NvbWUnIHNob3J0LWNpcmN1aXRzIG9uIGZpcnN0ICd0cnVlJ1xuICBub2Rlcy5zb21lKG4gPT4ge1xuICAgIHRyeSB7XG5cbiAgICAgIGxldCBsZWFkaW5nQ29tbWVudHNcblxuICAgICAgLy8gbi5sZWFkaW5nQ29tbWVudHMgaXMgbGVnYWN5IGBhdHRhY2hDb21tZW50c2AgYmVoYXZpb3JcbiAgICAgIGlmICgnbGVhZGluZ0NvbW1lbnRzJyBpbiBuKSB7XG4gICAgICAgIGxlYWRpbmdDb21tZW50cyA9IG4ubGVhZGluZ0NvbW1lbnRzXG4gICAgICB9IGVsc2UgaWYgKG4ucmFuZ2UpIHtcbiAgICAgICAgbGVhZGluZ0NvbW1lbnRzID0gc291cmNlLmdldENvbW1lbnRzQmVmb3JlKG4pXG4gICAgICB9XG5cbiAgICAgIGlmICghbGVhZGluZ0NvbW1lbnRzIHx8IGxlYWRpbmdDb21tZW50cy5sZW5ndGggPT09IDApIHJldHVybiBmYWxzZVxuXG4gICAgICBmb3IgKGxldCBuYW1lIGluIGRvY1N0eWxlUGFyc2Vycykge1xuICAgICAgICBjb25zdCBkb2MgPSBkb2NTdHlsZVBhcnNlcnNbbmFtZV0obGVhZGluZ0NvbW1lbnRzKVxuICAgICAgICBpZiAoZG9jKSB7XG4gICAgICAgICAgbWV0YWRhdGEuZG9jID0gZG9jXG4gICAgICAgIH1cbiAgICAgIH1cblxuICAgICAgcmV0dXJuIHRydWVcbiAgICB9IGNhdGNoIChlcnIpIHtcbiAgICAgIHJldHVybiBmYWxzZVxuICAgIH1cbiAgfSlcblxuICByZXR1cm4gbWV0YWRhdGFcbn1cblxuY29uc3QgYXZhaWxhYmxlRG9jU3R5bGVQYXJzZXJzID0ge1xuICBqc2RvYzogY2FwdHVyZUpzRG9jLFxuICB0b21kb2M6IGNhcHR1cmVUb21Eb2MsXG59XG5cbi8qKlxuICogcGFyc2UgSlNEb2MgZnJvbSBsZWFkaW5nIGNvbW1lbnRzXG4gKiBAcGFyYW0gIHsuLi5bdHlwZV19IGNvbW1lbnRzIFtkZXNjcmlwdGlvbl1cbiAqIEByZXR1cm4ge3tkb2M6IG9iamVjdH19XG4gKi9cbmZ1bmN0aW9uIGNhcHR1cmVKc0RvYyhjb21tZW50cykge1xuICBsZXQgZG9jXG5cbiAgLy8gY2FwdHVyZSBYU0RvY1xuICBjb21tZW50cy5mb3JFYWNoKGNvbW1lbnQgPT4ge1xuICAgIC8vIHNraXAgbm9uLWJsb2NrIGNvbW1lbnRzXG4gICAgaWYgKGNvbW1lbnQudmFsdWUuc2xpY2UoMCwgNCkgIT09ICcqXFxuIConKSByZXR1cm5cbiAgICB0cnkge1xuICAgICAgZG9jID0gZG9jdHJpbmUucGFyc2UoY29tbWVudC52YWx1ZSwgeyB1bndyYXA6IHRydWUgfSlcbiAgICB9IGNhdGNoIChlcnIpIHtcbiAgICAgIC8qIGRvbid0IGNhcmUsIGZvciBub3c/IG1heWJlIGFkZCB0byBgZXJyb3JzP2AgKi9cbiAgICB9XG4gIH0pXG5cbiAgcmV0dXJuIGRvY1xufVxuXG4vKipcbiAgKiBwYXJzZSBUb21Eb2Mgc2VjdGlvbiBmcm9tIGNvbW1lbnRzXG4gICovXG5mdW5jdGlvbiBjYXB0dXJlVG9tRG9jKGNvbW1lbnRzKSB7XG4gIC8vIGNvbGxlY3QgbGluZXMgdXAgdG8gZmlyc3QgcGFyYWdyYXBoIGJyZWFrXG4gIGNvbnN0IGxpbmVzID0gW11cbiAgZm9yIChsZXQgaSA9IDA7IGkgPCBjb21tZW50cy5sZW5ndGg7IGkrKykge1xuICAgIGNvbnN0IGNvbW1lbnQgPSBjb21tZW50c1tpXVxuICAgIGlmIChjb21tZW50LnZhbHVlLm1hdGNoKC9eXFxzKiQvKSkgYnJlYWtcbiAgICBsaW5lcy5wdXNoKGNvbW1lbnQudmFsdWUudHJpbSgpKVxuICB9XG5cbiAgLy8gcmV0dXJuIGRvY3RyaW5lLWxpa2Ugb2JqZWN0XG4gIGNvbnN0IHN0YXR1c01hdGNoID0gbGluZXMuam9pbignICcpLm1hdGNoKC9eKFB1YmxpY3xJbnRlcm5hbHxEZXByZWNhdGVkKTpcXHMqKC4rKS8pXG4gIGlmIChzdGF0dXNNYXRjaCkge1xuICAgIHJldHVybiB7XG4gICAgICBkZXNjcmlwdGlvbjogc3RhdHVzTWF0Y2hbMl0sXG4gICAgICB0YWdzOiBbe1xuICAgICAgICB0aXRsZTogc3RhdHVzTWF0Y2hbMV0udG9Mb3dlckNhc2UoKSxcbiAgICAgICAgZGVzY3JpcHRpb246IHN0YXR1c01hdGNoWzJdLFxuICAgICAgfV0sXG4gICAgfVxuICB9XG59XG5cbkV4cG9ydE1hcC5nZXQgPSBmdW5jdGlvbiAoc291cmNlLCBjb250ZXh0KSB7XG4gIGNvbnN0IHBhdGggPSByZXNvbHZlKHNvdXJjZSwgY29udGV4dClcbiAgaWYgKHBhdGggPT0gbnVsbCkgcmV0dXJuIG51bGxcblxuICByZXR1cm4gRXhwb3J0TWFwLmZvcihjaGlsZENvbnRleHQocGF0aCwgY29udGV4dCkpXG59XG5cbkV4cG9ydE1hcC5mb3IgPSBmdW5jdGlvbiAoY29udGV4dCkge1xuICBjb25zdCB7IHBhdGggfSA9IGNvbnRleHRcblxuICBjb25zdCBjYWNoZUtleSA9IGhhc2hPYmplY3QoY29udGV4dCkuZGlnZXN0KCdoZXgnKVxuICBsZXQgZXhwb3J0TWFwID0gZXhwb3J0Q2FjaGUuZ2V0KGNhY2hlS2V5KVxuXG4gIC8vIHJldHVybiBjYWNoZWQgaWdub3JlXG4gIGlmIChleHBvcnRNYXAgPT09IG51bGwpIHJldHVybiBudWxsXG5cbiAgY29uc3Qgc3RhdHMgPSBmcy5zdGF0U3luYyhwYXRoKVxuICBpZiAoZXhwb3J0TWFwICE9IG51bGwpIHtcbiAgICAvLyBkYXRlIGVxdWFsaXR5IGNoZWNrXG4gICAgaWYgKGV4cG9ydE1hcC5tdGltZSAtIHN0YXRzLm10aW1lID09PSAwKSB7XG4gICAgICByZXR1cm4gZXhwb3J0TWFwXG4gICAgfVxuICAgIC8vIGZ1dHVyZTogY2hlY2sgY29udGVudCBlcXVhbGl0eT9cbiAgfVxuXG4gIC8vIGNoZWNrIHZhbGlkIGV4dGVuc2lvbnMgZmlyc3RcbiAgaWYgKCFoYXNWYWxpZEV4dGVuc2lvbihwYXRoLCBjb250ZXh0KSkge1xuICAgIGV4cG9ydENhY2hlLnNldChjYWNoZUtleSwgbnVsbClcbiAgICByZXR1cm4gbnVsbFxuICB9XG5cbiAgY29uc3QgY29udGVudCA9IGZzLnJlYWRGaWxlU3luYyhwYXRoLCB7IGVuY29kaW5nOiAndXRmOCcgfSlcblxuICAvLyBjaGVjayBmb3IgYW5kIGNhY2hlIGlnbm9yZVxuICBpZiAoaXNJZ25vcmVkKHBhdGgsIGNvbnRleHQpIHx8ICF1bmFtYmlndW91cy50ZXN0KGNvbnRlbnQpKSB7XG4gICAgbG9nKCdpZ25vcmVkIHBhdGggZHVlIHRvIHVuYW1iaWd1b3VzIHJlZ2V4IG9yIGlnbm9yZSBzZXR0aW5nczonLCBwYXRoKVxuICAgIGV4cG9ydENhY2hlLnNldChjYWNoZUtleSwgbnVsbClcbiAgICByZXR1cm4gbnVsbFxuICB9XG5cbiAgbG9nKCdjYWNoZSBtaXNzJywgY2FjaGVLZXksICdmb3IgcGF0aCcsIHBhdGgpXG4gIGV4cG9ydE1hcCA9IEV4cG9ydE1hcC5wYXJzZShwYXRoLCBjb250ZW50LCBjb250ZXh0KVxuXG4gIC8vIGFtYmlndW91cyBtb2R1bGVzIHJldHVybiBudWxsXG4gIGlmIChleHBvcnRNYXAgPT0gbnVsbCkgcmV0dXJuIG51bGxcblxuICBleHBvcnRNYXAubXRpbWUgPSBzdGF0cy5tdGltZVxuXG4gIGV4cG9ydENhY2hlLnNldChjYWNoZUtleSwgZXhwb3J0TWFwKVxuICByZXR1cm4gZXhwb3J0TWFwXG59XG5cblxuRXhwb3J0TWFwLnBhcnNlID0gZnVuY3Rpb24gKHBhdGgsIGNvbnRlbnQsIGNvbnRleHQpIHtcbiAgdmFyIG0gPSBuZXcgRXhwb3J0TWFwKHBhdGgpXG5cbiAgdHJ5IHtcbiAgICB2YXIgYXN0ID0gcGFyc2UocGF0aCwgY29udGVudCwgY29udGV4dClcbiAgfSBjYXRjaCAoZXJyKSB7XG4gICAgbG9nKCdwYXJzZSBlcnJvcjonLCBwYXRoLCBlcnIpXG4gICAgbS5lcnJvcnMucHVzaChlcnIpXG4gICAgcmV0dXJuIG0gLy8gY2FuJ3QgY29udGludWVcbiAgfVxuXG4gIGlmICghdW5hbWJpZ3VvdXMuaXNNb2R1bGUoYXN0KSkgcmV0dXJuIG51bGxcblxuICBjb25zdCBkb2NzdHlsZSA9IChjb250ZXh0LnNldHRpbmdzICYmIGNvbnRleHQuc2V0dGluZ3NbJ2ltcG9ydC9kb2NzdHlsZSddKSB8fCBbJ2pzZG9jJ11cbiAgY29uc3QgZG9jU3R5bGVQYXJzZXJzID0ge31cbiAgZG9jc3R5bGUuZm9yRWFjaChzdHlsZSA9PiB7XG4gICAgZG9jU3R5bGVQYXJzZXJzW3N0eWxlXSA9IGF2YWlsYWJsZURvY1N0eWxlUGFyc2Vyc1tzdHlsZV1cbiAgfSlcblxuICAvLyBhdHRlbXB0IHRvIGNvbGxlY3QgbW9kdWxlIGRvY1xuICBpZiAoYXN0LmNvbW1lbnRzKSB7XG4gICAgYXN0LmNvbW1lbnRzLnNvbWUoYyA9PiB7XG4gICAgICBpZiAoYy50eXBlICE9PSAnQmxvY2snKSByZXR1cm4gZmFsc2VcbiAgICAgIHRyeSB7XG4gICAgICAgIGNvbnN0IGRvYyA9IGRvY3RyaW5lLnBhcnNlKGMudmFsdWUsIHsgdW53cmFwOiB0cnVlIH0pXG4gICAgICAgIGlmIChkb2MudGFncy5zb21lKHQgPT4gdC50aXRsZSA9PT0gJ21vZHVsZScpKSB7XG4gICAgICAgICAgbS5kb2MgPSBkb2NcbiAgICAgICAgICByZXR1cm4gdHJ1ZVxuICAgICAgICB9XG4gICAgICB9IGNhdGNoIChlcnIpIHsgLyogaWdub3JlICovIH1cbiAgICAgIHJldHVybiBmYWxzZVxuICAgIH0pXG4gIH1cblxuICBjb25zdCBuYW1lc3BhY2VzID0gbmV3IE1hcCgpXG5cbiAgZnVuY3Rpb24gcmVtb3RlUGF0aCh2YWx1ZSkge1xuICAgIHJldHVybiByZXNvbHZlLnJlbGF0aXZlKHZhbHVlLCBwYXRoLCBjb250ZXh0LnNldHRpbmdzKVxuICB9XG5cbiAgZnVuY3Rpb24gcmVzb2x2ZUltcG9ydCh2YWx1ZSkge1xuICAgIGNvbnN0IHJwID0gcmVtb3RlUGF0aCh2YWx1ZSlcbiAgICBpZiAocnAgPT0gbnVsbCkgcmV0dXJuIG51bGxcbiAgICByZXR1cm4gRXhwb3J0TWFwLmZvcihjaGlsZENvbnRleHQocnAsIGNvbnRleHQpKVxuICB9XG5cbiAgZnVuY3Rpb24gZ2V0TmFtZXNwYWNlKGlkZW50aWZpZXIpIHtcbiAgICBpZiAoIW5hbWVzcGFjZXMuaGFzKGlkZW50aWZpZXIubmFtZSkpIHJldHVyblxuXG4gICAgcmV0dXJuIGZ1bmN0aW9uICgpIHtcbiAgICAgIHJldHVybiByZXNvbHZlSW1wb3J0KG5hbWVzcGFjZXMuZ2V0KGlkZW50aWZpZXIubmFtZSkpXG4gICAgfVxuICB9XG5cbiAgZnVuY3Rpb24gYWRkTmFtZXNwYWNlKG9iamVjdCwgaWRlbnRpZmllcikge1xuICAgIGNvbnN0IG5zZm4gPSBnZXROYW1lc3BhY2UoaWRlbnRpZmllcilcbiAgICBpZiAobnNmbikge1xuICAgICAgT2JqZWN0LmRlZmluZVByb3BlcnR5KG9iamVjdCwgJ25hbWVzcGFjZScsIHsgZ2V0OiBuc2ZuIH0pXG4gICAgfVxuXG4gICAgcmV0dXJuIG9iamVjdFxuICB9XG5cbiAgZnVuY3Rpb24gY2FwdHVyZURlcGVuZGVuY3koZGVjbGFyYXRpb24pIHtcbiAgICBpZiAoZGVjbGFyYXRpb24uc291cmNlID09IG51bGwpIHJldHVybiBudWxsXG5cbiAgICBjb25zdCBwID0gcmVtb3RlUGF0aChkZWNsYXJhdGlvbi5zb3VyY2UudmFsdWUpXG4gICAgaWYgKHAgPT0gbnVsbCkgcmV0dXJuIG51bGxcbiAgICBjb25zdCBleGlzdGluZyA9IG0uaW1wb3J0cy5nZXQocClcbiAgICBpZiAoZXhpc3RpbmcgIT0gbnVsbCkgcmV0dXJuIGV4aXN0aW5nLmdldHRlclxuXG4gICAgY29uc3QgZ2V0dGVyID0gdGh1bmtGb3IocCwgY29udGV4dClcbiAgICBtLmltcG9ydHMuc2V0KHAsIHtcbiAgICAgIGdldHRlcixcbiAgICAgIHNvdXJjZTogeyAgLy8gY2FwdHVyaW5nIGFjdHVhbCBub2RlIHJlZmVyZW5jZSBob2xkcyBmdWxsIEFTVCBpbiBtZW1vcnkhXG4gICAgICAgIHZhbHVlOiBkZWNsYXJhdGlvbi5zb3VyY2UudmFsdWUsXG4gICAgICAgIGxvYzogZGVjbGFyYXRpb24uc291cmNlLmxvYyxcbiAgICAgIH0sXG4gICAgfSlcbiAgICByZXR1cm4gZ2V0dGVyXG4gIH1cblxuICBjb25zdCBzb3VyY2UgPSBtYWtlU291cmNlQ29kZShjb250ZW50LCBhc3QpXG5cbiAgYXN0LmJvZHkuZm9yRWFjaChmdW5jdGlvbiAobikge1xuXG4gICAgaWYgKG4udHlwZSA9PT0gJ0V4cG9ydERlZmF1bHREZWNsYXJhdGlvbicpIHtcbiAgICAgIGNvbnN0IGV4cG9ydE1ldGEgPSBjYXB0dXJlRG9jKHNvdXJjZSwgZG9jU3R5bGVQYXJzZXJzLCBuKVxuICAgICAgaWYgKG4uZGVjbGFyYXRpb24udHlwZSA9PT0gJ0lkZW50aWZpZXInKSB7XG4gICAgICAgIGFkZE5hbWVzcGFjZShleHBvcnRNZXRhLCBuLmRlY2xhcmF0aW9uKVxuICAgICAgfVxuICAgICAgbS5uYW1lc3BhY2Uuc2V0KCdkZWZhdWx0JywgZXhwb3J0TWV0YSlcbiAgICAgIHJldHVyblxuICAgIH1cblxuICAgIGlmIChuLnR5cGUgPT09ICdFeHBvcnRBbGxEZWNsYXJhdGlvbicpIHtcbiAgICAgIGNvbnN0IGdldHRlciA9IGNhcHR1cmVEZXBlbmRlbmN5KG4pXG4gICAgICBpZiAoZ2V0dGVyKSBtLmRlcGVuZGVuY2llcy5hZGQoZ2V0dGVyKVxuICAgICAgcmV0dXJuXG4gICAgfVxuXG4gICAgLy8gY2FwdHVyZSBuYW1lc3BhY2VzIGluIGNhc2Ugb2YgbGF0ZXIgZXhwb3J0XG4gICAgaWYgKG4udHlwZSA9PT0gJ0ltcG9ydERlY2xhcmF0aW9uJykge1xuICAgICAgY2FwdHVyZURlcGVuZGVuY3kobilcbiAgICAgIGxldCBuc1xuICAgICAgaWYgKG4uc3BlY2lmaWVycy5zb21lKHMgPT4gcy50eXBlID09PSAnSW1wb3J0TmFtZXNwYWNlU3BlY2lmaWVyJyAmJiAobnMgPSBzKSkpIHtcbiAgICAgICAgbmFtZXNwYWNlcy5zZXQobnMubG9jYWwubmFtZSwgbi5zb3VyY2UudmFsdWUpXG4gICAgICB9XG4gICAgICByZXR1cm5cbiAgICB9XG5cbiAgICBpZiAobi50eXBlID09PSAnRXhwb3J0TmFtZWREZWNsYXJhdGlvbicpIHtcbiAgICAgIC8vIGNhcHR1cmUgZGVjbGFyYXRpb25cbiAgICAgIGlmIChuLmRlY2xhcmF0aW9uICE9IG51bGwpIHtcbiAgICAgICAgc3dpdGNoIChuLmRlY2xhcmF0aW9uLnR5cGUpIHtcbiAgICAgICAgICBjYXNlICdGdW5jdGlvbkRlY2xhcmF0aW9uJzpcbiAgICAgICAgICBjYXNlICdDbGFzc0RlY2xhcmF0aW9uJzpcbiAgICAgICAgICBjYXNlICdUeXBlQWxpYXMnOiAvLyBmbG93dHlwZSB3aXRoIGJhYmVsLWVzbGludCBwYXJzZXJcbiAgICAgICAgICBjYXNlICdJbnRlcmZhY2VEZWNsYXJhdGlvbic6XG4gICAgICAgICAgY2FzZSAnVFNFbnVtRGVjbGFyYXRpb24nOlxuICAgICAgICAgIGNhc2UgJ1RTSW50ZXJmYWNlRGVjbGFyYXRpb24nOlxuICAgICAgICAgIGNhc2UgJ1RTQWJzdHJhY3RDbGFzc0RlY2xhcmF0aW9uJzpcbiAgICAgICAgICBjYXNlICdUU01vZHVsZURlY2xhcmF0aW9uJzpcbiAgICAgICAgICAgIG0ubmFtZXNwYWNlLnNldChuLmRlY2xhcmF0aW9uLmlkLm5hbWUsIGNhcHR1cmVEb2Moc291cmNlLCBkb2NTdHlsZVBhcnNlcnMsIG4pKVxuICAgICAgICAgICAgYnJlYWtcbiAgICAgICAgICBjYXNlICdWYXJpYWJsZURlY2xhcmF0aW9uJzpcbiAgICAgICAgICAgIG4uZGVjbGFyYXRpb24uZGVjbGFyYXRpb25zLmZvckVhY2goKGQpID0+XG4gICAgICAgICAgICAgIHJlY3Vyc2l2ZVBhdHRlcm5DYXB0dXJlKGQuaWQsXG4gICAgICAgICAgICAgICAgaWQgPT4gbS5uYW1lc3BhY2Uuc2V0KGlkLm5hbWUsIGNhcHR1cmVEb2Moc291cmNlLCBkb2NTdHlsZVBhcnNlcnMsIGQsIG4pKSkpXG4gICAgICAgICAgICBicmVha1xuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgIGNvbnN0IG5zb3VyY2UgPSBuLnNvdXJjZSAmJiBuLnNvdXJjZS52YWx1ZVxuICAgICAgbi5zcGVjaWZpZXJzLmZvckVhY2goKHMpID0+IHtcbiAgICAgICAgY29uc3QgZXhwb3J0TWV0YSA9IHt9XG4gICAgICAgIGxldCBsb2NhbFxuXG4gICAgICAgIHN3aXRjaCAocy50eXBlKSB7XG4gICAgICAgICAgY2FzZSAnRXhwb3J0RGVmYXVsdFNwZWNpZmllcic6XG4gICAgICAgICAgICBpZiAoIW4uc291cmNlKSByZXR1cm5cbiAgICAgICAgICAgIGxvY2FsID0gJ2RlZmF1bHQnXG4gICAgICAgICAgICBicmVha1xuICAgICAgICAgIGNhc2UgJ0V4cG9ydE5hbWVzcGFjZVNwZWNpZmllcic6XG4gICAgICAgICAgICBtLm5hbWVzcGFjZS5zZXQocy5leHBvcnRlZC5uYW1lLCBPYmplY3QuZGVmaW5lUHJvcGVydHkoZXhwb3J0TWV0YSwgJ25hbWVzcGFjZScsIHtcbiAgICAgICAgICAgICAgZ2V0KCkgeyByZXR1cm4gcmVzb2x2ZUltcG9ydChuc291cmNlKSB9LFxuICAgICAgICAgICAgfSkpXG4gICAgICAgICAgICByZXR1cm5cbiAgICAgICAgICBjYXNlICdFeHBvcnRTcGVjaWZpZXInOlxuICAgICAgICAgICAgaWYgKCFuLnNvdXJjZSkge1xuICAgICAgICAgICAgICBtLm5hbWVzcGFjZS5zZXQocy5leHBvcnRlZC5uYW1lLCBhZGROYW1lc3BhY2UoZXhwb3J0TWV0YSwgcy5sb2NhbCkpXG4gICAgICAgICAgICAgIHJldHVyblxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgLy8gZWxzZSBmYWxscyB0aHJvdWdoXG4gICAgICAgICAgZGVmYXVsdDpcbiAgICAgICAgICAgIGxvY2FsID0gcy5sb2NhbC5uYW1lXG4gICAgICAgICAgICBicmVha1xuICAgICAgICB9XG5cbiAgICAgICAgLy8gdG9kbzogSlNEb2NcbiAgICAgICAgbS5yZWV4cG9ydHMuc2V0KHMuZXhwb3J0ZWQubmFtZSwgeyBsb2NhbCwgZ2V0SW1wb3J0OiAoKSA9PiByZXNvbHZlSW1wb3J0KG5zb3VyY2UpIH0pXG4gICAgICB9KVxuICAgIH1cbiAgfSlcblxuICByZXR1cm4gbVxufVxuXG4vKipcbiAqIFRoZSBjcmVhdGlvbiBvZiB0aGlzIGNsb3N1cmUgaXMgaXNvbGF0ZWQgZnJvbSBvdGhlciBzY29wZXNcbiAqIHRvIGF2b2lkIG92ZXItcmV0ZW50aW9uIG9mIHVucmVsYXRlZCB2YXJpYWJsZXMsIHdoaWNoIGhhc1xuICogY2F1c2VkIG1lbW9yeSBsZWFrcy4gU2VlICMxMjY2LlxuICovXG5mdW5jdGlvbiB0aHVua0ZvcihwLCBjb250ZXh0KSB7XG4gIHJldHVybiAoKSA9PiBFeHBvcnRNYXAuZm9yKGNoaWxkQ29udGV4dChwLCBjb250ZXh0KSlcbn1cblxuXG4vKipcbiAqIFRyYXZlcnNlIGEgcGF0dGVybi9pZGVudGlmaWVyIG5vZGUsIGNhbGxpbmcgJ2NhbGxiYWNrJ1xuICogZm9yIGVhY2ggbGVhZiBpZGVudGlmaWVyLlxuICogQHBhcmFtICB7bm9kZX0gICBwYXR0ZXJuXG4gKiBAcGFyYW0gIHtGdW5jdGlvbn0gY2FsbGJhY2tcbiAqIEByZXR1cm4ge3ZvaWR9XG4gKi9cbmV4cG9ydCBmdW5jdGlvbiByZWN1cnNpdmVQYXR0ZXJuQ2FwdHVyZShwYXR0ZXJuLCBjYWxsYmFjaykge1xuICBzd2l0Y2ggKHBhdHRlcm4udHlwZSkge1xuICAgIGNhc2UgJ0lkZW50aWZpZXInOiAvLyBiYXNlIGNhc2VcbiAgICAgIGNhbGxiYWNrKHBhdHRlcm4pXG4gICAgICBicmVha1xuXG4gICAgY2FzZSAnT2JqZWN0UGF0dGVybic6XG4gICAgICBwYXR0ZXJuLnByb3BlcnRpZXMuZm9yRWFjaChwID0+IHtcbiAgICAgICAgcmVjdXJzaXZlUGF0dGVybkNhcHR1cmUocC52YWx1ZSwgY2FsbGJhY2spXG4gICAgICB9KVxuICAgICAgYnJlYWtcblxuICAgIGNhc2UgJ0FycmF5UGF0dGVybic6XG4gICAgICBwYXR0ZXJuLmVsZW1lbnRzLmZvckVhY2goKGVsZW1lbnQpID0+IHtcbiAgICAgICAgaWYgKGVsZW1lbnQgPT0gbnVsbCkgcmV0dXJuXG4gICAgICAgIHJlY3Vyc2l2ZVBhdHRlcm5DYXB0dXJlKGVsZW1lbnQsIGNhbGxiYWNrKVxuICAgICAgfSlcbiAgICAgIGJyZWFrXG5cbiAgICBjYXNlICdBc3NpZ25tZW50UGF0dGVybic6XG4gICAgICBjYWxsYmFjayhwYXR0ZXJuLmxlZnQpXG4gICAgICBicmVha1xuICB9XG59XG5cbi8qKlxuICogZG9uJ3QgaG9sZCBmdWxsIGNvbnRleHQgb2JqZWN0IGluIG1lbW9yeSwganVzdCBncmFiIHdoYXQgd2UgbmVlZC5cbiAqL1xuZnVuY3Rpb24gY2hpbGRDb250ZXh0KHBhdGgsIGNvbnRleHQpIHtcbiAgY29uc3QgeyBzZXR0aW5ncywgcGFyc2VyT3B0aW9ucywgcGFyc2VyUGF0aCB9ID0gY29udGV4dFxuICByZXR1cm4ge1xuICAgIHNldHRpbmdzLFxuICAgIHBhcnNlck9wdGlvbnMsXG4gICAgcGFyc2VyUGF0aCxcbiAgICBwYXRoLFxuICB9XG59XG5cblxuLyoqXG4gKiBzb21ldGltZXMgbGVnYWN5IHN1cHBvcnQgaXNuJ3QgX3RoYXRfIGhhcmQuLi4gcmlnaHQ/XG4gKi9cbmZ1bmN0aW9uIG1ha2VTb3VyY2VDb2RlKHRleHQsIGFzdCkge1xuICBpZiAoU291cmNlQ29kZS5sZW5ndGggPiAxKSB7XG4gICAgLy8gRVNMaW50IDNcbiAgICByZXR1cm4gbmV3IFNvdXJjZUNvZGUodGV4dCwgYXN0KVxuICB9IGVsc2Uge1xuICAgIC8vIEVTTGludCA0LCA1XG4gICAgcmV0dXJuIG5ldyBTb3VyY2VDb2RlKHsgdGV4dCwgYXN0IH0pXG4gIH1cbn1cbiJdfQ==