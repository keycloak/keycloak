'use strict';Object.defineProperty(exports, "__esModule", { value: true });var _createClass = function () {function defineProperties(target, props) {for (var i = 0; i < props.length; i++) {var descriptor = props[i];descriptor.enumerable = descriptor.enumerable || false;descriptor.configurable = true;if ("value" in descriptor) descriptor.writable = true;Object.defineProperty(target, descriptor.key, descriptor);}}return function (Constructor, protoProps, staticProps) {if (protoProps) defineProperties(Constructor.prototype, protoProps);if (staticProps) defineProperties(Constructor, staticProps);return Constructor;};}();exports.



































































































































































































































































































































































































































































































































































































































































































































































recursivePatternCapture = recursivePatternCapture;var _fs = require('fs');var _fs2 = _interopRequireDefault(_fs);var _path = require('path');var _doctrine = require('doctrine');var _doctrine2 = _interopRequireDefault(_doctrine);var _debug = require('debug');var _debug2 = _interopRequireDefault(_debug);var _eslint = require('eslint');var _parse = require('eslint-module-utils/parse');var _parse2 = _interopRequireDefault(_parse);var _visit = require('eslint-module-utils/visit');var _visit2 = _interopRequireDefault(_visit);var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);var _ignore = require('eslint-module-utils/ignore');var _ignore2 = _interopRequireDefault(_ignore);var _hash = require('eslint-module-utils/hash');var _unambiguous = require('eslint-module-utils/unambiguous');var unambiguous = _interopRequireWildcard(_unambiguous);var _tsconfigLoader = require('tsconfig-paths/lib/tsconfig-loader');var _arrayIncludes = require('array-includes');var _arrayIncludes2 = _interopRequireDefault(_arrayIncludes);function _interopRequireWildcard(obj) {if (obj && obj.__esModule) {return obj;} else {var newObj = {};if (obj != null) {for (var key in obj) {if (Object.prototype.hasOwnProperty.call(obj, key)) newObj[key] = obj[key];}}newObj['default'] = obj;return newObj;}}function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}function _classCallCheck(instance, Constructor) {if (!(instance instanceof Constructor)) {throw new TypeError("Cannot call a class as a function");}}var ts = void 0;var log = (0, _debug2['default'])('eslint-plugin-import:ExportMap');var exportCache = new Map();var tsConfigCache = new Map();var ExportMap = function () {function ExportMap(path) {_classCallCheck(this, ExportMap);this.path = path;this.namespace = new Map(); // todo: restructure to key on path, value is resolver + map of names
    this.reexports = new Map(); /**
                                 * star-exports
                                 * @type {Set} of () => ExportMap
                                 */this.dependencies = new Set(); /**
                                                                   * dependencies of this module that are not explicitly re-exported
                                                                   * @type {Map} from path = () => ExportMap
                                                                   */this.imports = new Map();this.errors = []; /**
                                                                                                                 * type {'ambiguous' | 'Module' | 'Script'}
                                                                                                                 */this.parseGoal = 'ambiguous';}_createClass(ExportMap, [{ key: 'has', /**
                                                                                                                                                                                         * Note that this does not check explicitly re-exported names for existence
                                                                                                                                                                                         * in the base namespace, but it will expand all `export * from '...'` exports
                                                                                                                                                                                         * if not found in the explicit namespace.
                                                                                                                                                                                         * @param  {string}  name
                                                                                                                                                                                         * @return {Boolean} true if `name` is exported by this module.
                                                                                                                                                                                         */value: function () {function has(name) {if (this.namespace.has(name)) return true;if (this.reexports.has(name)) return true; // default exports must be explicitly re-exported (#328)
        if (name !== 'default') {var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {for (var _iterator = this.dependencies[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var dep = _step.value;var innerMap = dep(); // todo: report as unresolved?
              if (!innerMap) continue;if (innerMap.has(name)) return true;}} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}}return false;}return has;}() /**
                                                                                                                                                                                                                                                                                                                                 * ensure that imported name fully resolves.
                                                                                                                                                                                                                                                                                                                                 * @param  {string} name
                                                                                                                                                                                                                                                                                                                                 * @return {{ found: boolean, path: ExportMap[] }}
                                                                                                                                                                                                                                                                                                                                 */ }, { key: 'hasDeep', value: function () {function hasDeep(name) {if (this.namespace.has(name)) return { found: true, path: [this] };if (this.reexports.has(name)) {var reexports = this.reexports.get(name);var imported = reexports.getImport(); // if import is ignored, return explicit 'null'
          if (imported == null) return { found: true, path: [this] }; // safeguard against cycles, only if name matches
          if (imported.path === this.path && reexports.local === name) {return { found: false, path: [this] };}var deep = imported.hasDeep(reexports.local);deep.path.unshift(this);return deep;} // default exports must be explicitly re-exported (#328)
        if (name !== 'default') {var _iteratorNormalCompletion2 = true;var _didIteratorError2 = false;var _iteratorError2 = undefined;try {for (var _iterator2 = this.dependencies[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {var dep = _step2.value;var innerMap = dep();if (innerMap == null) return { found: true, path: [this] }; // todo: report as unresolved?
              if (!innerMap) continue; // safeguard against cycles
              if (innerMap.path === this.path) continue;var innerValue = innerMap.hasDeep(name);if (innerValue.found) {innerValue.path.unshift(this);return innerValue;}}} catch (err) {_didIteratorError2 = true;_iteratorError2 = err;} finally {try {if (!_iteratorNormalCompletion2 && _iterator2['return']) {_iterator2['return']();}} finally {if (_didIteratorError2) {throw _iteratorError2;}}}}return { found: false, path: [this] };}return hasDeep;}() }, { key: 'get', value: function () {function get(name) {if (this.namespace.has(name)) return this.namespace.get(name);if (this.reexports.has(name)) {var reexports = this.reexports.get(name);var imported = reexports.getImport(); // if import is ignored, return explicit 'null'
          if (imported == null) return null; // safeguard against cycles, only if name matches
          if (imported.path === this.path && reexports.local === name) return undefined;return imported.get(reexports.local);} // default exports must be explicitly re-exported (#328)
        if (name !== 'default') {var _iteratorNormalCompletion3 = true;var _didIteratorError3 = false;var _iteratorError3 = undefined;try {for (var _iterator3 = this.dependencies[Symbol.iterator](), _step3; !(_iteratorNormalCompletion3 = (_step3 = _iterator3.next()).done); _iteratorNormalCompletion3 = true) {var dep = _step3.value;var innerMap = dep(); // todo: report as unresolved?
              if (!innerMap) continue; // safeguard against cycles
              if (innerMap.path === this.path) continue;var innerValue = innerMap.get(name);if (innerValue !== undefined) return innerValue;}} catch (err) {_didIteratorError3 = true;_iteratorError3 = err;} finally {try {if (!_iteratorNormalCompletion3 && _iterator3['return']) {_iterator3['return']();}} finally {if (_didIteratorError3) {throw _iteratorError3;}}}}return undefined;}return get;}() }, { key: 'forEach', value: function () {function forEach(callback, thisArg) {var _this = this;this.namespace.forEach(function (v, n) {return callback.call(thisArg, v, n, _this);});this.reexports.forEach(function (reexports, name) {var reexported = reexports.getImport(); // can't look up meta for ignored re-exports (#348)
          callback.call(thisArg, reexported && reexported.get(reexports.local), name, _this);});this.dependencies.forEach(function (dep) {var d = dep(); // CJS / ignored dependencies won't exist (#717)
          if (d == null) return;d.forEach(function (v, n) {return n !== 'default' && callback.call(thisArg, v, n, _this);});});}return forEach;}() // todo: keys, values, entries?
  }, { key: 'reportErrors', value: function () {function reportErrors(context, declaration) {context.report({ node: declaration.source, message: 'Parse errors in imported module \'' + String(declaration.source.value) + '\': ' + ('' + String(this.errors.map(function (e) {return String(e.message) + ' (' + String(e.lineNumber) + ':' + String(e.column) + ')';}).join(', '))) });}return reportErrors;}() }, { key: 'hasDefault', get: function () {function get() {return this.get('default') != null;}return get;}() // stronger than this.has
  }, { key: 'size', get: function () {function get() {var size = this.namespace.size + this.reexports.size;this.dependencies.forEach(function (dep) {var d = dep(); // CJS / ignored dependencies won't exist (#717)
          if (d == null) return;size += d.size;});return size;}return get;}() }]);return ExportMap;}(); /**
                                                                                                         * parse docs from the first node that has leading comments
                                                                                                         */exports['default'] = ExportMap;function captureDoc(source, docStyleParsers) {var metadata = {}; // 'some' short-circuits on first 'true'
  for (var _len = arguments.length, nodes = Array(_len > 2 ? _len - 2 : 0), _key = 2; _key < _len; _key++) {nodes[_key - 2] = arguments[_key];}nodes.some(function (n) {try {var leadingComments = void 0; // n.leadingComments is legacy `attachComments` behavior
      if ('leadingComments' in n) {leadingComments = n.leadingComments;} else if (n.range) {leadingComments = source.getCommentsBefore(n);}if (!leadingComments || leadingComments.length === 0) return false;for (var name in docStyleParsers) {var doc = docStyleParsers[name](leadingComments);if (doc) {metadata.doc = doc;}}return true;} catch (err) {return false;}});return metadata;}var availableDocStyleParsers = { jsdoc: captureJsDoc, tomdoc: captureTomDoc }; /**
                                                                                                                                                                                                                                                                                                                                                                                                                                                                              * parse JSDoc from leading comments
                                                                                                                                                                                                                                                                                                                                                                                                                                                                              * @param {object[]} comments
                                                                                                                                                                                                                                                                                                                                                                                                                                                                              * @return {{ doc: object }}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                              */function captureJsDoc(comments) {var doc = void 0; // capture XSDoc
  comments.forEach(function (comment) {// skip non-block comments
    if (comment.type !== 'Block') return;try {doc = _doctrine2['default'].parse(comment.value, { unwrap: true });} catch (err) {/* don't care, for now? maybe add to `errors?` */}});return doc;} /**
                                                                                                                                                                                                    * parse TomDoc section from comments
                                                                                                                                                                                                    */function captureTomDoc(comments) {// collect lines up to first paragraph break
  var lines = [];for (var i = 0; i < comments.length; i++) {var comment = comments[i];if (comment.value.match(/^\s*$/)) break;lines.push(comment.value.trim());} // return doctrine-like object
  var statusMatch = lines.join(' ').match(/^(Public|Internal|Deprecated):\s*(.+)/);if (statusMatch) {return { description: statusMatch[2], tags: [{ title: statusMatch[1].toLowerCase(), description: statusMatch[2] }] };}}var supportedImportTypes = new Set(['ImportDefaultSpecifier', 'ImportNamespaceSpecifier']);ExportMap.get = function (source, context) {var path = (0, _resolve2['default'])(source, context);if (path == null) return null;return ExportMap['for'](childContext(path, context));};ExportMap['for'] = function (context) {var path = context.path;var cacheKey = (0, _hash.hashObject)(context).digest('hex');var exportMap = exportCache.get(cacheKey); // return cached ignore
  if (exportMap === null) return null;var stats = _fs2['default'].statSync(path);if (exportMap != null) {// date equality check
    if (exportMap.mtime - stats.mtime === 0) {return exportMap;} // future: check content equality?
  } // check valid extensions first
  if (!(0, _ignore.hasValidExtension)(path, context)) {exportCache.set(cacheKey, null);return null;} // check for and cache ignore
  if ((0, _ignore2['default'])(path, context)) {log('ignored path due to ignore settings:', path);exportCache.set(cacheKey, null);return null;}var content = _fs2['default'].readFileSync(path, { encoding: 'utf8' }); // check for and cache unambiguous modules
  if (!unambiguous.test(content)) {log('ignored path due to unambiguous regex:', path);exportCache.set(cacheKey, null);return null;}log('cache miss', cacheKey, 'for path', path);exportMap = ExportMap.parse(path, content, context); // ambiguous modules return null
  if (exportMap == null) return null;exportMap.mtime = stats.mtime;exportCache.set(cacheKey, exportMap);return exportMap;};ExportMap.parse = function (path, content, context) {var m = new ExportMap(path);var isEsModuleInteropTrue = isEsModuleInterop();var ast = void 0;var visitorKeys = void 0;try {var result = (0, _parse2['default'])(path, content, context);ast = result.ast;visitorKeys = result.visitorKeys;} catch (err) {m.errors.push(err);return m; // can't continue
  }m.visitorKeys = visitorKeys;var hasDynamicImports = false;function processDynamicImport(source) {hasDynamicImports = true;if (source.type !== 'Literal') {return null;}var p = remotePath(source.value);if (p == null) {return null;}var importedSpecifiers = new Set();importedSpecifiers.add('ImportNamespaceSpecifier');var getter = thunkFor(p, context);m.imports.set(p, { getter: getter, declarations: new Set([{ source: { // capturing actual node reference holds full AST in memory!
          value: source.value, loc: source.loc }, importedSpecifiers: importedSpecifiers }]) });}(0, _visit2['default'])(ast, visitorKeys, { ImportExpression: function () {function ImportExpression(node) {processDynamicImport(node.source);}return ImportExpression;}(), CallExpression: function () {function CallExpression(node) {if (node.callee.type === 'Import') {processDynamicImport(node.arguments[0]);}}return CallExpression;}() });var unambiguouslyESM = unambiguous.isModule(ast);if (!unambiguouslyESM && !hasDynamicImports) return null;var docstyle = context.settings && context.settings['import/docstyle'] || ['jsdoc'];var docStyleParsers = {};docstyle.forEach(function (style) {docStyleParsers[style] = availableDocStyleParsers[style];}); // attempt to collect module doc
  if (ast.comments) {ast.comments.some(function (c) {if (c.type !== 'Block') return false;try {var doc = _doctrine2['default'].parse(c.value, { unwrap: true });if (doc.tags.some(function (t) {return t.title === 'module';})) {m.doc = doc;return true;}} catch (err) {/* ignore */}return false;});}var namespaces = new Map();function remotePath(value) {return _resolve2['default'].relative(value, path, context.settings);}function resolveImport(value) {var rp = remotePath(value);if (rp == null) return null;return ExportMap['for'](childContext(rp, context));}function getNamespace(identifier) {if (!namespaces.has(identifier.name)) return;return function () {return resolveImport(namespaces.get(identifier.name));};}function addNamespace(object, identifier) {var nsfn = getNamespace(identifier);if (nsfn) {Object.defineProperty(object, 'namespace', { get: nsfn });}return object;}function processSpecifier(s, n, m) {var nsource = n.source && n.source.value;var exportMeta = {};var local = void 0;switch (s.type) {case 'ExportDefaultSpecifier':if (!nsource) return;local = 'default';break;case 'ExportNamespaceSpecifier':m.namespace.set(s.exported.name, Object.defineProperty(exportMeta, 'namespace', { get: function () {function get() {return resolveImport(nsource);}return get;}() }));return;case 'ExportAllDeclaration':m.namespace.set(s.exported.name || s.exported.value, addNamespace(exportMeta, s.source.value));return;case 'ExportSpecifier':if (!n.source) {m.namespace.set(s.exported.name || s.exported.value, addNamespace(exportMeta, s.local));return;} // else falls through
      default:local = s.local.name;break;} // todo: JSDoc
    m.reexports.set(s.exported.name, { local: local, getImport: function () {function getImport() {return resolveImport(nsource);}return getImport;}() });}function captureDependency(_ref, isOnlyImportingTypes) {var source = _ref.source;var importedSpecifiers = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : new Set();if (source == null) return null;var p = remotePath(source.value);if (p == null) return null;var declarationMetadata = { // capturing actual node reference holds full AST in memory!
      source: { value: source.value, loc: source.loc }, isOnlyImportingTypes: isOnlyImportingTypes, importedSpecifiers: importedSpecifiers };var existing = m.imports.get(p);if (existing != null) {existing.declarations.add(declarationMetadata);return existing.getter;}var getter = thunkFor(p, context);m.imports.set(p, { getter: getter, declarations: new Set([declarationMetadata]) });return getter;}var source = makeSourceCode(content, ast);function readTsConfig() {var tsConfigInfo = (0, _tsconfigLoader.tsConfigLoader)({ cwd: context.parserOptions && context.parserOptions.tsconfigRootDir || process.cwd(), getEnv: function () {function getEnv(key) {return process.env[key];}return getEnv;}() });try {if (tsConfigInfo.tsConfigPath !== undefined) {// Projects not using TypeScript won't have `typescript` installed.
        if (!ts) {ts = require('typescript');}var configFile = ts.readConfigFile(tsConfigInfo.tsConfigPath, ts.sys.readFile);return ts.parseJsonConfigFileContent(configFile.config, ts.sys, (0, _path.dirname)(tsConfigInfo.tsConfigPath));}} catch (e) {// Catch any errors
    }return null;}function isEsModuleInterop() {var cacheKey = (0, _hash.hashObject)({ tsconfigRootDir: context.parserOptions && context.parserOptions.tsconfigRootDir }).digest('hex');var tsConfig = tsConfigCache.get(cacheKey);if (typeof tsConfig === 'undefined') {tsConfig = readTsConfig(context);tsConfigCache.set(cacheKey, tsConfig);}return tsConfig && tsConfig.options ? tsConfig.options.esModuleInterop : false;}ast.body.forEach(function (n) {if (n.type === 'ExportDefaultDeclaration') {var exportMeta = captureDoc(source, docStyleParsers, n);if (n.declaration.type === 'Identifier') {addNamespace(exportMeta, n.declaration);}m.namespace.set('default', exportMeta);return;}if (n.type === 'ExportAllDeclaration') {var getter = captureDependency(n, n.exportKind === 'type');if (getter) m.dependencies.add(getter);if (n.exported) {processSpecifier(n, n.exported, m);}return;} // capture namespaces in case of later export
    if (n.type === 'ImportDeclaration') {// import type { Foo } (TS and Flow)
      var declarationIsType = n.importKind === 'type'; // import './foo' or import {} from './foo' (both 0 specifiers) is a side effect and
      // shouldn't be considered to be just importing types
      var specifiersOnlyImportingTypes = n.specifiers.length;var importedSpecifiers = new Set();n.specifiers.forEach(function (specifier) {if (supportedImportTypes.has(specifier.type)) {importedSpecifiers.add(specifier.type);}if (specifier.type === 'ImportSpecifier') {importedSpecifiers.add(specifier.imported.name || specifier.imported.value);} // import { type Foo } (Flow)
        specifiersOnlyImportingTypes = specifiersOnlyImportingTypes && specifier.importKind === 'type';});captureDependency(n, declarationIsType || specifiersOnlyImportingTypes, importedSpecifiers);var ns = n.specifiers.find(function (s) {return s.type === 'ImportNamespaceSpecifier';});if (ns) {namespaces.set(ns.local.name, n.source.value);}return;}if (n.type === 'ExportNamedDeclaration') {// capture declaration
      if (n.declaration != null) {switch (n.declaration.type) {case 'FunctionDeclaration':case 'ClassDeclaration':case 'TypeAlias': // flowtype with babel-eslint parser
          case 'InterfaceDeclaration':case 'DeclareFunction':case 'TSDeclareFunction':case 'TSEnumDeclaration':case 'TSTypeAliasDeclaration':case 'TSInterfaceDeclaration':case 'TSAbstractClassDeclaration':case 'TSModuleDeclaration':m.namespace.set(n.declaration.id.name, captureDoc(source, docStyleParsers, n));break;case 'VariableDeclaration':n.declaration.declarations.forEach(function (d) {return recursivePatternCapture(d.id, function (id) {return m.namespace.set(id.name, captureDoc(source, docStyleParsers, d, n));});});break;}}n.specifiers.forEach(function (s) {return processSpecifier(s, n, m);});}var exports = ['TSExportAssignment'];if (isEsModuleInteropTrue) {exports.push('TSNamespaceExportDeclaration');} // This doesn't declare anything, but changes what's being exported.
    if ((0, _arrayIncludes2['default'])(exports, n.type)) {var exportedName = n.type === 'TSNamespaceExportDeclaration' ? (n.id || n.name).name : n.expression && n.expression.name || n.expression.id && n.expression.id.name || null;var declTypes = ['VariableDeclaration', 'ClassDeclaration', 'TSDeclareFunction', 'TSEnumDeclaration', 'TSTypeAliasDeclaration', 'TSInterfaceDeclaration', 'TSAbstractClassDeclaration', 'TSModuleDeclaration'];var exportedDecls = ast.body.filter(function (_ref2) {var type = _ref2.type,id = _ref2.id,declarations = _ref2.declarations;return (0, _arrayIncludes2['default'])(declTypes, type) && (id && id.name === exportedName || declarations && declarations.find(function (d) {return d.id.name === exportedName;}));});if (exportedDecls.length === 0) {// Export is not referencing any local declaration, must be re-exporting
        m.namespace.set('default', captureDoc(source, docStyleParsers, n));return;}if (isEsModuleInteropTrue // esModuleInterop is on in tsconfig
      && !m.namespace.has('default') // and default isn't added already
      ) {m.namespace.set('default', {}); // add default export
        }exportedDecls.forEach(function (decl) {if (decl.type === 'TSModuleDeclaration') {if (decl.body && decl.body.type === 'TSModuleDeclaration') {m.namespace.set(decl.body.id.name, captureDoc(source, docStyleParsers, decl.body));} else if (decl.body && decl.body.body) {decl.body.body.forEach(function (moduleBlockNode) {// Export-assignment exports all members in the namespace,
              // explicitly exported or not.
              var namespaceDecl = moduleBlockNode.type === 'ExportNamedDeclaration' ? moduleBlockNode.declaration : moduleBlockNode;if (!namespaceDecl) {// TypeScript can check this for us; we needn't
              } else if (namespaceDecl.type === 'VariableDeclaration') {namespaceDecl.declarations.forEach(function (d) {return recursivePatternCapture(d.id, function (id) {return m.namespace.set(id.name, captureDoc(source, docStyleParsers, decl, namespaceDecl, moduleBlockNode));});});} else {m.namespace.set(namespaceDecl.id.name, captureDoc(source, docStyleParsers, moduleBlockNode));}});}} else {// Export as default
          m.namespace.set('default', captureDoc(source, docStyleParsers, decl));}});}});if (isEsModuleInteropTrue // esModuleInterop is on in tsconfig
  && m.namespace.size > 0 // anything is exported
  && !m.namespace.has('default') // and default isn't added already
  ) {m.namespace.set('default', {}); // add default export
    }if (unambiguouslyESM) {m.parseGoal = 'Module';}return m;}; /**
                                                                 * The creation of this closure is isolated from other scopes
                                                                 * to avoid over-retention of unrelated variables, which has
                                                                 * caused memory leaks. See #1266.
                                                                 */function thunkFor(p, context) {return function () {return ExportMap['for'](childContext(p, context));};} /**
                                                                                                                                                                             * Traverse a pattern/identifier node, calling 'callback'
                                                                                                                                                                             * for each leaf identifier.
                                                                                                                                                                             * @param  {node}   pattern
                                                                                                                                                                             * @param  {Function} callback
                                                                                                                                                                             * @return {void}
                                                                                                                                                                             */function recursivePatternCapture(pattern, callback) {switch (pattern.type) {case 'Identifier': // base case
      callback(pattern);break;case 'ObjectPattern':pattern.properties.forEach(function (p) {if (p.type === 'ExperimentalRestProperty' || p.type === 'RestElement') {callback(p.argument);return;}recursivePatternCapture(p.value, callback);});break;case 'ArrayPattern':pattern.elements.forEach(function (element) {if (element == null) return;if (element.type === 'ExperimentalRestProperty' || element.type === 'RestElement') {callback(element.argument);return;}recursivePatternCapture(element, callback);});break;case 'AssignmentPattern':callback(pattern.left);break;}} /**
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       * don't hold full context object in memory, just grab what we need.
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       */function childContext(path, context) {var settings = context.settings,parserOptions = context.parserOptions,parserPath = context.parserPath;return { settings: settings, parserOptions: parserOptions, parserPath: parserPath, path: path };} /**
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        * sometimes legacy support isn't _that_ hard... right?
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        */function makeSourceCode(text, ast) {if (_eslint.SourceCode.length > 1) {// ESLint 3
    return new _eslint.SourceCode(text, ast);} else {// ESLint 4, 5
    return new _eslint.SourceCode({ text: text, ast: ast });}}
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uL3NyYy9FeHBvcnRNYXAuanMiXSwibmFtZXMiOlsicmVjdXJzaXZlUGF0dGVybkNhcHR1cmUiLCJ1bmFtYmlndW91cyIsInRzIiwibG9nIiwiZXhwb3J0Q2FjaGUiLCJNYXAiLCJ0c0NvbmZpZ0NhY2hlIiwiRXhwb3J0TWFwIiwicGF0aCIsIm5hbWVzcGFjZSIsInJlZXhwb3J0cyIsImRlcGVuZGVuY2llcyIsIlNldCIsImltcG9ydHMiLCJlcnJvcnMiLCJwYXJzZUdvYWwiLCJuYW1lIiwiaGFzIiwiZGVwIiwiaW5uZXJNYXAiLCJmb3VuZCIsImdldCIsImltcG9ydGVkIiwiZ2V0SW1wb3J0IiwibG9jYWwiLCJkZWVwIiwiaGFzRGVlcCIsInVuc2hpZnQiLCJpbm5lclZhbHVlIiwidW5kZWZpbmVkIiwiY2FsbGJhY2siLCJ0aGlzQXJnIiwiZm9yRWFjaCIsInYiLCJuIiwiY2FsbCIsInJlZXhwb3J0ZWQiLCJkIiwiY29udGV4dCIsImRlY2xhcmF0aW9uIiwicmVwb3J0Iiwibm9kZSIsInNvdXJjZSIsIm1lc3NhZ2UiLCJ2YWx1ZSIsIm1hcCIsImUiLCJsaW5lTnVtYmVyIiwiY29sdW1uIiwiam9pbiIsInNpemUiLCJjYXB0dXJlRG9jIiwiZG9jU3R5bGVQYXJzZXJzIiwibWV0YWRhdGEiLCJub2RlcyIsInNvbWUiLCJsZWFkaW5nQ29tbWVudHMiLCJyYW5nZSIsImdldENvbW1lbnRzQmVmb3JlIiwibGVuZ3RoIiwiZG9jIiwiZXJyIiwiYXZhaWxhYmxlRG9jU3R5bGVQYXJzZXJzIiwianNkb2MiLCJjYXB0dXJlSnNEb2MiLCJ0b21kb2MiLCJjYXB0dXJlVG9tRG9jIiwiY29tbWVudHMiLCJjb21tZW50IiwidHlwZSIsImRvY3RyaW5lIiwicGFyc2UiLCJ1bndyYXAiLCJsaW5lcyIsImkiLCJtYXRjaCIsInB1c2giLCJ0cmltIiwic3RhdHVzTWF0Y2giLCJkZXNjcmlwdGlvbiIsInRhZ3MiLCJ0aXRsZSIsInRvTG93ZXJDYXNlIiwic3VwcG9ydGVkSW1wb3J0VHlwZXMiLCJjaGlsZENvbnRleHQiLCJjYWNoZUtleSIsImRpZ2VzdCIsImV4cG9ydE1hcCIsInN0YXRzIiwiZnMiLCJzdGF0U3luYyIsIm10aW1lIiwic2V0IiwiY29udGVudCIsInJlYWRGaWxlU3luYyIsImVuY29kaW5nIiwidGVzdCIsIm0iLCJpc0VzTW9kdWxlSW50ZXJvcFRydWUiLCJpc0VzTW9kdWxlSW50ZXJvcCIsImFzdCIsInZpc2l0b3JLZXlzIiwicmVzdWx0IiwiaGFzRHluYW1pY0ltcG9ydHMiLCJwcm9jZXNzRHluYW1pY0ltcG9ydCIsInAiLCJyZW1vdGVQYXRoIiwiaW1wb3J0ZWRTcGVjaWZpZXJzIiwiYWRkIiwiZ2V0dGVyIiwidGh1bmtGb3IiLCJkZWNsYXJhdGlvbnMiLCJsb2MiLCJJbXBvcnRFeHByZXNzaW9uIiwiQ2FsbEV4cHJlc3Npb24iLCJjYWxsZWUiLCJhcmd1bWVudHMiLCJ1bmFtYmlndW91c2x5RVNNIiwiaXNNb2R1bGUiLCJkb2NzdHlsZSIsInNldHRpbmdzIiwic3R5bGUiLCJjIiwidCIsIm5hbWVzcGFjZXMiLCJyZXNvbHZlIiwicmVsYXRpdmUiLCJyZXNvbHZlSW1wb3J0IiwicnAiLCJnZXROYW1lc3BhY2UiLCJpZGVudGlmaWVyIiwiYWRkTmFtZXNwYWNlIiwib2JqZWN0IiwibnNmbiIsIk9iamVjdCIsImRlZmluZVByb3BlcnR5IiwicHJvY2Vzc1NwZWNpZmllciIsInMiLCJuc291cmNlIiwiZXhwb3J0TWV0YSIsImV4cG9ydGVkIiwiY2FwdHVyZURlcGVuZGVuY3kiLCJpc09ubHlJbXBvcnRpbmdUeXBlcyIsImRlY2xhcmF0aW9uTWV0YWRhdGEiLCJleGlzdGluZyIsIm1ha2VTb3VyY2VDb2RlIiwicmVhZFRzQ29uZmlnIiwidHNDb25maWdJbmZvIiwiY3dkIiwicGFyc2VyT3B0aW9ucyIsInRzY29uZmlnUm9vdERpciIsInByb2Nlc3MiLCJnZXRFbnYiLCJrZXkiLCJlbnYiLCJ0c0NvbmZpZ1BhdGgiLCJyZXF1aXJlIiwiY29uZmlnRmlsZSIsInJlYWRDb25maWdGaWxlIiwic3lzIiwicmVhZEZpbGUiLCJwYXJzZUpzb25Db25maWdGaWxlQ29udGVudCIsImNvbmZpZyIsInRzQ29uZmlnIiwib3B0aW9ucyIsImVzTW9kdWxlSW50ZXJvcCIsImJvZHkiLCJleHBvcnRLaW5kIiwiZGVjbGFyYXRpb25Jc1R5cGUiLCJpbXBvcnRLaW5kIiwic3BlY2lmaWVyc09ubHlJbXBvcnRpbmdUeXBlcyIsInNwZWNpZmllcnMiLCJzcGVjaWZpZXIiLCJucyIsImZpbmQiLCJpZCIsImV4cG9ydHMiLCJleHBvcnRlZE5hbWUiLCJleHByZXNzaW9uIiwiZGVjbFR5cGVzIiwiZXhwb3J0ZWREZWNscyIsImZpbHRlciIsImRlY2wiLCJtb2R1bGVCbG9ja05vZGUiLCJuYW1lc3BhY2VEZWNsIiwicGF0dGVybiIsInByb3BlcnRpZXMiLCJhcmd1bWVudCIsImVsZW1lbnRzIiwiZWxlbWVudCIsImxlZnQiLCJwYXJzZXJQYXRoIiwidGV4dCIsIlNvdXJjZUNvZGUiXSwibWFwcGluZ3MiOiI7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7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Q2F6SixPQURiLENBQ0Z5SixVQURFLENBRW5DLE9BQU8sRUFDTHZFLGtCQURLLEVBRUw2Qiw0QkFGSyxFQUdMMEMsc0JBSEssRUFJTHZMLFVBSkssRUFBUCxDQU1ELEMsQ0FHRDs7MHlCQUdBLFNBQVN5SSxjQUFULENBQXdCK0MsSUFBeEIsRUFBOEI1RixHQUE5QixFQUFtQyxDQUNqQyxJQUFJNkYsbUJBQVd0SSxNQUFYLEdBQW9CLENBQXhCLEVBQTJCLENBQ3pCO0FBQ0EsV0FBTyxJQUFJc0ksa0JBQUosQ0FBZUQsSUFBZixFQUFxQjVGLEdBQXJCLENBQVAsQ0FDRCxDQUhELE1BR08sQ0FDTDtBQUNBLFdBQU8sSUFBSTZGLGtCQUFKLENBQWUsRUFBRUQsVUFBRixFQUFRNUYsUUFBUixFQUFmLENBQVAsQ0FDRCxDQUNGIiwiZmlsZSI6IkV4cG9ydE1hcC5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBmcyBmcm9tICdmcyc7XG5pbXBvcnQgeyBkaXJuYW1lIH0gZnJvbSAncGF0aCc7XG5cbmltcG9ydCBkb2N0cmluZSBmcm9tICdkb2N0cmluZSc7XG5cbmltcG9ydCBkZWJ1ZyBmcm9tICdkZWJ1Zyc7XG5cbmltcG9ydCB7IFNvdXJjZUNvZGUgfSBmcm9tICdlc2xpbnQnO1xuXG5pbXBvcnQgcGFyc2UgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9wYXJzZSc7XG5pbXBvcnQgdmlzaXQgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy92aXNpdCc7XG5pbXBvcnQgcmVzb2x2ZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnO1xuaW1wb3J0IGlzSWdub3JlZCwgeyBoYXNWYWxpZEV4dGVuc2lvbiB9IGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvaWdub3JlJztcblxuaW1wb3J0IHsgaGFzaE9iamVjdCB9IGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvaGFzaCc7XG5pbXBvcnQgKiBhcyB1bmFtYmlndW91cyBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3VuYW1iaWd1b3VzJztcblxuaW1wb3J0IHsgdHNDb25maWdMb2FkZXIgfSBmcm9tICd0c2NvbmZpZy1wYXRocy9saWIvdHNjb25maWctbG9hZGVyJztcblxuaW1wb3J0IGluY2x1ZGVzIGZyb20gJ2FycmF5LWluY2x1ZGVzJztcblxubGV0IHRzO1xuXG5jb25zdCBsb2cgPSBkZWJ1ZygnZXNsaW50LXBsdWdpbi1pbXBvcnQ6RXhwb3J0TWFwJyk7XG5cbmNvbnN0IGV4cG9ydENhY2hlID0gbmV3IE1hcCgpO1xuY29uc3QgdHNDb25maWdDYWNoZSA9IG5ldyBNYXAoKTtcblxuZXhwb3J0IGRlZmF1bHQgY2xhc3MgRXhwb3J0TWFwIHtcbiAgY29uc3RydWN0b3IocGF0aCkge1xuICAgIHRoaXMucGF0aCA9IHBhdGg7XG4gICAgdGhpcy5uYW1lc3BhY2UgPSBuZXcgTWFwKCk7XG4gICAgLy8gdG9kbzogcmVzdHJ1Y3R1cmUgdG8ga2V5IG9uIHBhdGgsIHZhbHVlIGlzIHJlc29sdmVyICsgbWFwIG9mIG5hbWVzXG4gICAgdGhpcy5yZWV4cG9ydHMgPSBuZXcgTWFwKCk7XG4gICAgLyoqXG4gICAgICogc3Rhci1leHBvcnRzXG4gICAgICogQHR5cGUge1NldH0gb2YgKCkgPT4gRXhwb3J0TWFwXG4gICAgICovXG4gICAgdGhpcy5kZXBlbmRlbmNpZXMgPSBuZXcgU2V0KCk7XG4gICAgLyoqXG4gICAgICogZGVwZW5kZW5jaWVzIG9mIHRoaXMgbW9kdWxlIHRoYXQgYXJlIG5vdCBleHBsaWNpdGx5IHJlLWV4cG9ydGVkXG4gICAgICogQHR5cGUge01hcH0gZnJvbSBwYXRoID0gKCkgPT4gRXhwb3J0TWFwXG4gICAgICovXG4gICAgdGhpcy5pbXBvcnRzID0gbmV3IE1hcCgpO1xuICAgIHRoaXMuZXJyb3JzID0gW107XG4gICAgLyoqXG4gICAgICogdHlwZSB7J2FtYmlndW91cycgfCAnTW9kdWxlJyB8ICdTY3JpcHQnfVxuICAgICAqL1xuICAgIHRoaXMucGFyc2VHb2FsID0gJ2FtYmlndW91cyc7XG4gIH1cblxuICBnZXQgaGFzRGVmYXVsdCgpIHsgcmV0dXJuIHRoaXMuZ2V0KCdkZWZhdWx0JykgIT0gbnVsbDsgfSAvLyBzdHJvbmdlciB0aGFuIHRoaXMuaGFzXG5cbiAgZ2V0IHNpemUoKSB7XG4gICAgbGV0IHNpemUgPSB0aGlzLm5hbWVzcGFjZS5zaXplICsgdGhpcy5yZWV4cG9ydHMuc2l6ZTtcbiAgICB0aGlzLmRlcGVuZGVuY2llcy5mb3JFYWNoKGRlcCA9PiB7XG4gICAgICBjb25zdCBkID0gZGVwKCk7XG4gICAgICAvLyBDSlMgLyBpZ25vcmVkIGRlcGVuZGVuY2llcyB3b24ndCBleGlzdCAoIzcxNylcbiAgICAgIGlmIChkID09IG51bGwpIHJldHVybjtcbiAgICAgIHNpemUgKz0gZC5zaXplO1xuICAgIH0pO1xuICAgIHJldHVybiBzaXplO1xuICB9XG5cbiAgLyoqXG4gICAqIE5vdGUgdGhhdCB0aGlzIGRvZXMgbm90IGNoZWNrIGV4cGxpY2l0bHkgcmUtZXhwb3J0ZWQgbmFtZXMgZm9yIGV4aXN0ZW5jZVxuICAgKiBpbiB0aGUgYmFzZSBuYW1lc3BhY2UsIGJ1dCBpdCB3aWxsIGV4cGFuZCBhbGwgYGV4cG9ydCAqIGZyb20gJy4uLidgIGV4cG9ydHNcbiAgICogaWYgbm90IGZvdW5kIGluIHRoZSBleHBsaWNpdCBuYW1lc3BhY2UuXG4gICAqIEBwYXJhbSAge3N0cmluZ30gIG5hbWVcbiAgICogQHJldHVybiB7Qm9vbGVhbn0gdHJ1ZSBpZiBgbmFtZWAgaXMgZXhwb3J0ZWQgYnkgdGhpcyBtb2R1bGUuXG4gICAqL1xuICBoYXMobmFtZSkge1xuICAgIGlmICh0aGlzLm5hbWVzcGFjZS5oYXMobmFtZSkpIHJldHVybiB0cnVlO1xuICAgIGlmICh0aGlzLnJlZXhwb3J0cy5oYXMobmFtZSkpIHJldHVybiB0cnVlO1xuXG4gICAgLy8gZGVmYXVsdCBleHBvcnRzIG11c3QgYmUgZXhwbGljaXRseSByZS1leHBvcnRlZCAoIzMyOClcbiAgICBpZiAobmFtZSAhPT0gJ2RlZmF1bHQnKSB7XG4gICAgICBmb3IgKGNvbnN0IGRlcCBvZiB0aGlzLmRlcGVuZGVuY2llcykge1xuICAgICAgICBjb25zdCBpbm5lck1hcCA9IGRlcCgpO1xuXG4gICAgICAgIC8vIHRvZG86IHJlcG9ydCBhcyB1bnJlc29sdmVkP1xuICAgICAgICBpZiAoIWlubmVyTWFwKSBjb250aW51ZTtcblxuICAgICAgICBpZiAoaW5uZXJNYXAuaGFzKG5hbWUpKSByZXR1cm4gdHJ1ZTtcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gZmFsc2U7XG4gIH1cblxuICAvKipcbiAgICogZW5zdXJlIHRoYXQgaW1wb3J0ZWQgbmFtZSBmdWxseSByZXNvbHZlcy5cbiAgICogQHBhcmFtICB7c3RyaW5nfSBuYW1lXG4gICAqIEByZXR1cm4ge3sgZm91bmQ6IGJvb2xlYW4sIHBhdGg6IEV4cG9ydE1hcFtdIH19XG4gICAqL1xuICBoYXNEZWVwKG5hbWUpIHtcbiAgICBpZiAodGhpcy5uYW1lc3BhY2UuaGFzKG5hbWUpKSByZXR1cm4geyBmb3VuZDogdHJ1ZSwgcGF0aDogW3RoaXNdIH07XG5cbiAgICBpZiAodGhpcy5yZWV4cG9ydHMuaGFzKG5hbWUpKSB7XG4gICAgICBjb25zdCByZWV4cG9ydHMgPSB0aGlzLnJlZXhwb3J0cy5nZXQobmFtZSk7XG4gICAgICBjb25zdCBpbXBvcnRlZCA9IHJlZXhwb3J0cy5nZXRJbXBvcnQoKTtcblxuICAgICAgLy8gaWYgaW1wb3J0IGlzIGlnbm9yZWQsIHJldHVybiBleHBsaWNpdCAnbnVsbCdcbiAgICAgIGlmIChpbXBvcnRlZCA9PSBudWxsKSByZXR1cm4geyBmb3VuZDogdHJ1ZSwgcGF0aDogW3RoaXNdIH07XG5cbiAgICAgIC8vIHNhZmVndWFyZCBhZ2FpbnN0IGN5Y2xlcywgb25seSBpZiBuYW1lIG1hdGNoZXNcbiAgICAgIGlmIChpbXBvcnRlZC5wYXRoID09PSB0aGlzLnBhdGggJiYgcmVleHBvcnRzLmxvY2FsID09PSBuYW1lKSB7XG4gICAgICAgIHJldHVybiB7IGZvdW5kOiBmYWxzZSwgcGF0aDogW3RoaXNdIH07XG4gICAgICB9XG5cbiAgICAgIGNvbnN0IGRlZXAgPSBpbXBvcnRlZC5oYXNEZWVwKHJlZXhwb3J0cy5sb2NhbCk7XG4gICAgICBkZWVwLnBhdGgudW5zaGlmdCh0aGlzKTtcblxuICAgICAgcmV0dXJuIGRlZXA7XG4gICAgfVxuXG5cbiAgICAvLyBkZWZhdWx0IGV4cG9ydHMgbXVzdCBiZSBleHBsaWNpdGx5IHJlLWV4cG9ydGVkICgjMzI4KVxuICAgIGlmIChuYW1lICE9PSAnZGVmYXVsdCcpIHtcbiAgICAgIGZvciAoY29uc3QgZGVwIG9mIHRoaXMuZGVwZW5kZW5jaWVzKSB7XG4gICAgICAgIGNvbnN0IGlubmVyTWFwID0gZGVwKCk7XG4gICAgICAgIGlmIChpbm5lck1hcCA9PSBudWxsKSByZXR1cm4geyBmb3VuZDogdHJ1ZSwgcGF0aDogW3RoaXNdIH07XG4gICAgICAgIC8vIHRvZG86IHJlcG9ydCBhcyB1bnJlc29sdmVkP1xuICAgICAgICBpZiAoIWlubmVyTWFwKSBjb250aW51ZTtcblxuICAgICAgICAvLyBzYWZlZ3VhcmQgYWdhaW5zdCBjeWNsZXNcbiAgICAgICAgaWYgKGlubmVyTWFwLnBhdGggPT09IHRoaXMucGF0aCkgY29udGludWU7XG5cbiAgICAgICAgY29uc3QgaW5uZXJWYWx1ZSA9IGlubmVyTWFwLmhhc0RlZXAobmFtZSk7XG4gICAgICAgIGlmIChpbm5lclZhbHVlLmZvdW5kKSB7XG4gICAgICAgICAgaW5uZXJWYWx1ZS5wYXRoLnVuc2hpZnQodGhpcyk7XG4gICAgICAgICAgcmV0dXJuIGlubmVyVmFsdWU7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4geyBmb3VuZDogZmFsc2UsIHBhdGg6IFt0aGlzXSB9O1xuICB9XG5cbiAgZ2V0KG5hbWUpIHtcbiAgICBpZiAodGhpcy5uYW1lc3BhY2UuaGFzKG5hbWUpKSByZXR1cm4gdGhpcy5uYW1lc3BhY2UuZ2V0KG5hbWUpO1xuXG4gICAgaWYgKHRoaXMucmVleHBvcnRzLmhhcyhuYW1lKSkge1xuICAgICAgY29uc3QgcmVleHBvcnRzID0gdGhpcy5yZWV4cG9ydHMuZ2V0KG5hbWUpO1xuICAgICAgY29uc3QgaW1wb3J0ZWQgPSByZWV4cG9ydHMuZ2V0SW1wb3J0KCk7XG5cbiAgICAgIC8vIGlmIGltcG9ydCBpcyBpZ25vcmVkLCByZXR1cm4gZXhwbGljaXQgJ251bGwnXG4gICAgICBpZiAoaW1wb3J0ZWQgPT0gbnVsbCkgcmV0dXJuIG51bGw7XG5cbiAgICAgIC8vIHNhZmVndWFyZCBhZ2FpbnN0IGN5Y2xlcywgb25seSBpZiBuYW1lIG1hdGNoZXNcbiAgICAgIGlmIChpbXBvcnRlZC5wYXRoID09PSB0aGlzLnBhdGggJiYgcmVleHBvcnRzLmxvY2FsID09PSBuYW1lKSByZXR1cm4gdW5kZWZpbmVkO1xuXG4gICAgICByZXR1cm4gaW1wb3J0ZWQuZ2V0KHJlZXhwb3J0cy5sb2NhbCk7XG4gICAgfVxuXG4gICAgLy8gZGVmYXVsdCBleHBvcnRzIG11c3QgYmUgZXhwbGljaXRseSByZS1leHBvcnRlZCAoIzMyOClcbiAgICBpZiAobmFtZSAhPT0gJ2RlZmF1bHQnKSB7XG4gICAgICBmb3IgKGNvbnN0IGRlcCBvZiB0aGlzLmRlcGVuZGVuY2llcykge1xuICAgICAgICBjb25zdCBpbm5lck1hcCA9IGRlcCgpO1xuICAgICAgICAvLyB0b2RvOiByZXBvcnQgYXMgdW5yZXNvbHZlZD9cbiAgICAgICAgaWYgKCFpbm5lck1hcCkgY29udGludWU7XG5cbiAgICAgICAgLy8gc2FmZWd1YXJkIGFnYWluc3QgY3ljbGVzXG4gICAgICAgIGlmIChpbm5lck1hcC5wYXRoID09PSB0aGlzLnBhdGgpIGNvbnRpbnVlO1xuXG4gICAgICAgIGNvbnN0IGlubmVyVmFsdWUgPSBpbm5lck1hcC5nZXQobmFtZSk7XG4gICAgICAgIGlmIChpbm5lclZhbHVlICE9PSB1bmRlZmluZWQpIHJldHVybiBpbm5lclZhbHVlO1xuICAgICAgfVxuICAgIH1cblxuICAgIHJldHVybiB1bmRlZmluZWQ7XG4gIH1cblxuICBmb3JFYWNoKGNhbGxiYWNrLCB0aGlzQXJnKSB7XG4gICAgdGhpcy5uYW1lc3BhY2UuZm9yRWFjaCgodiwgbikgPT5cbiAgICAgIGNhbGxiYWNrLmNhbGwodGhpc0FyZywgdiwgbiwgdGhpcykpO1xuXG4gICAgdGhpcy5yZWV4cG9ydHMuZm9yRWFjaCgocmVleHBvcnRzLCBuYW1lKSA9PiB7XG4gICAgICBjb25zdCByZWV4cG9ydGVkID0gcmVleHBvcnRzLmdldEltcG9ydCgpO1xuICAgICAgLy8gY2FuJ3QgbG9vayB1cCBtZXRhIGZvciBpZ25vcmVkIHJlLWV4cG9ydHMgKCMzNDgpXG4gICAgICBjYWxsYmFjay5jYWxsKHRoaXNBcmcsIHJlZXhwb3J0ZWQgJiYgcmVleHBvcnRlZC5nZXQocmVleHBvcnRzLmxvY2FsKSwgbmFtZSwgdGhpcyk7XG4gICAgfSk7XG5cbiAgICB0aGlzLmRlcGVuZGVuY2llcy5mb3JFYWNoKGRlcCA9PiB7XG4gICAgICBjb25zdCBkID0gZGVwKCk7XG4gICAgICAvLyBDSlMgLyBpZ25vcmVkIGRlcGVuZGVuY2llcyB3b24ndCBleGlzdCAoIzcxNylcbiAgICAgIGlmIChkID09IG51bGwpIHJldHVybjtcblxuICAgICAgZC5mb3JFYWNoKCh2LCBuKSA9PlxuICAgICAgICBuICE9PSAnZGVmYXVsdCcgJiYgY2FsbGJhY2suY2FsbCh0aGlzQXJnLCB2LCBuLCB0aGlzKSk7XG4gICAgfSk7XG4gIH1cblxuICAvLyB0b2RvOiBrZXlzLCB2YWx1ZXMsIGVudHJpZXM/XG5cbiAgcmVwb3J0RXJyb3JzKGNvbnRleHQsIGRlY2xhcmF0aW9uKSB7XG4gICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgbm9kZTogZGVjbGFyYXRpb24uc291cmNlLFxuICAgICAgbWVzc2FnZTogYFBhcnNlIGVycm9ycyBpbiBpbXBvcnRlZCBtb2R1bGUgJyR7ZGVjbGFyYXRpb24uc291cmNlLnZhbHVlfSc6IGAgK1xuICAgICAgICAgICAgICAgICAgYCR7dGhpcy5lcnJvcnNcbiAgICAgICAgICAgICAgICAgICAgLm1hcChlID0+IGAke2UubWVzc2FnZX0gKCR7ZS5saW5lTnVtYmVyfToke2UuY29sdW1ufSlgKVxuICAgICAgICAgICAgICAgICAgICAuam9pbignLCAnKX1gLFxuICAgIH0pO1xuICB9XG59XG5cbi8qKlxuICogcGFyc2UgZG9jcyBmcm9tIHRoZSBmaXJzdCBub2RlIHRoYXQgaGFzIGxlYWRpbmcgY29tbWVudHNcbiAqL1xuZnVuY3Rpb24gY2FwdHVyZURvYyhzb3VyY2UsIGRvY1N0eWxlUGFyc2VycywgLi4ubm9kZXMpIHtcbiAgY29uc3QgbWV0YWRhdGEgPSB7fTtcblxuICAvLyAnc29tZScgc2hvcnQtY2lyY3VpdHMgb24gZmlyc3QgJ3RydWUnXG4gIG5vZGVzLnNvbWUobiA9PiB7XG4gICAgdHJ5IHtcblxuICAgICAgbGV0IGxlYWRpbmdDb21tZW50cztcblxuICAgICAgLy8gbi5sZWFkaW5nQ29tbWVudHMgaXMgbGVnYWN5IGBhdHRhY2hDb21tZW50c2AgYmVoYXZpb3JcbiAgICAgIGlmICgnbGVhZGluZ0NvbW1lbnRzJyBpbiBuKSB7XG4gICAgICAgIGxlYWRpbmdDb21tZW50cyA9IG4ubGVhZGluZ0NvbW1lbnRzO1xuICAgICAgfSBlbHNlIGlmIChuLnJhbmdlKSB7XG4gICAgICAgIGxlYWRpbmdDb21tZW50cyA9IHNvdXJjZS5nZXRDb21tZW50c0JlZm9yZShuKTtcbiAgICAgIH1cblxuICAgICAgaWYgKCFsZWFkaW5nQ29tbWVudHMgfHwgbGVhZGluZ0NvbW1lbnRzLmxlbmd0aCA9PT0gMCkgcmV0dXJuIGZhbHNlO1xuXG4gICAgICBmb3IgKGNvbnN0IG5hbWUgaW4gZG9jU3R5bGVQYXJzZXJzKSB7XG4gICAgICAgIGNvbnN0IGRvYyA9IGRvY1N0eWxlUGFyc2Vyc1tuYW1lXShsZWFkaW5nQ29tbWVudHMpO1xuICAgICAgICBpZiAoZG9jKSB7XG4gICAgICAgICAgbWV0YWRhdGEuZG9jID0gZG9jO1xuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgIHJldHVybiB0cnVlO1xuICAgIH0gY2F0Y2ggKGVycikge1xuICAgICAgcmV0dXJuIGZhbHNlO1xuICAgIH1cbiAgfSk7XG5cbiAgcmV0dXJuIG1ldGFkYXRhO1xufVxuXG5jb25zdCBhdmFpbGFibGVEb2NTdHlsZVBhcnNlcnMgPSB7XG4gIGpzZG9jOiBjYXB0dXJlSnNEb2MsXG4gIHRvbWRvYzogY2FwdHVyZVRvbURvYyxcbn07XG5cbi8qKlxuICogcGFyc2UgSlNEb2MgZnJvbSBsZWFkaW5nIGNvbW1lbnRzXG4gKiBAcGFyYW0ge29iamVjdFtdfSBjb21tZW50c1xuICogQHJldHVybiB7eyBkb2M6IG9iamVjdCB9fVxuICovXG5mdW5jdGlvbiBjYXB0dXJlSnNEb2MoY29tbWVudHMpIHtcbiAgbGV0IGRvYztcblxuICAvLyBjYXB0dXJlIFhTRG9jXG4gIGNvbW1lbnRzLmZvckVhY2goY29tbWVudCA9PiB7XG4gICAgLy8gc2tpcCBub24tYmxvY2sgY29tbWVudHNcbiAgICBpZiAoY29tbWVudC50eXBlICE9PSAnQmxvY2snKSByZXR1cm47XG4gICAgdHJ5IHtcbiAgICAgIGRvYyA9IGRvY3RyaW5lLnBhcnNlKGNvbW1lbnQudmFsdWUsIHsgdW53cmFwOiB0cnVlIH0pO1xuICAgIH0gY2F0Y2ggKGVycikge1xuICAgICAgLyogZG9uJ3QgY2FyZSwgZm9yIG5vdz8gbWF5YmUgYWRkIHRvIGBlcnJvcnM/YCAqL1xuICAgIH1cbiAgfSk7XG5cbiAgcmV0dXJuIGRvYztcbn1cblxuLyoqXG4gICogcGFyc2UgVG9tRG9jIHNlY3Rpb24gZnJvbSBjb21tZW50c1xuICAqL1xuZnVuY3Rpb24gY2FwdHVyZVRvbURvYyhjb21tZW50cykge1xuICAvLyBjb2xsZWN0IGxpbmVzIHVwIHRvIGZpcnN0IHBhcmFncmFwaCBicmVha1xuICBjb25zdCBsaW5lcyA9IFtdO1xuICBmb3IgKGxldCBpID0gMDsgaSA8IGNvbW1lbnRzLmxlbmd0aDsgaSsrKSB7XG4gICAgY29uc3QgY29tbWVudCA9IGNvbW1lbnRzW2ldO1xuICAgIGlmIChjb21tZW50LnZhbHVlLm1hdGNoKC9eXFxzKiQvKSkgYnJlYWs7XG4gICAgbGluZXMucHVzaChjb21tZW50LnZhbHVlLnRyaW0oKSk7XG4gIH1cblxuICAvLyByZXR1cm4gZG9jdHJpbmUtbGlrZSBvYmplY3RcbiAgY29uc3Qgc3RhdHVzTWF0Y2ggPSBsaW5lcy5qb2luKCcgJykubWF0Y2goL14oUHVibGljfEludGVybmFsfERlcHJlY2F0ZWQpOlxccyooLispLyk7XG4gIGlmIChzdGF0dXNNYXRjaCkge1xuICAgIHJldHVybiB7XG4gICAgICBkZXNjcmlwdGlvbjogc3RhdHVzTWF0Y2hbMl0sXG4gICAgICB0YWdzOiBbe1xuICAgICAgICB0aXRsZTogc3RhdHVzTWF0Y2hbMV0udG9Mb3dlckNhc2UoKSxcbiAgICAgICAgZGVzY3JpcHRpb246IHN0YXR1c01hdGNoWzJdLFxuICAgICAgfV0sXG4gICAgfTtcbiAgfVxufVxuXG5jb25zdCBzdXBwb3J0ZWRJbXBvcnRUeXBlcyA9IG5ldyBTZXQoWydJbXBvcnREZWZhdWx0U3BlY2lmaWVyJywgJ0ltcG9ydE5hbWVzcGFjZVNwZWNpZmllciddKTtcblxuRXhwb3J0TWFwLmdldCA9IGZ1bmN0aW9uIChzb3VyY2UsIGNvbnRleHQpIHtcbiAgY29uc3QgcGF0aCA9IHJlc29sdmUoc291cmNlLCBjb250ZXh0KTtcbiAgaWYgKHBhdGggPT0gbnVsbCkgcmV0dXJuIG51bGw7XG5cbiAgcmV0dXJuIEV4cG9ydE1hcC5mb3IoY2hpbGRDb250ZXh0KHBhdGgsIGNvbnRleHQpKTtcbn07XG5cbkV4cG9ydE1hcC5mb3IgPSBmdW5jdGlvbiAoY29udGV4dCkge1xuICBjb25zdCB7IHBhdGggfSA9IGNvbnRleHQ7XG5cbiAgY29uc3QgY2FjaGVLZXkgPSBoYXNoT2JqZWN0KGNvbnRleHQpLmRpZ2VzdCgnaGV4Jyk7XG4gIGxldCBleHBvcnRNYXAgPSBleHBvcnRDYWNoZS5nZXQoY2FjaGVLZXkpO1xuXG4gIC8vIHJldHVybiBjYWNoZWQgaWdub3JlXG4gIGlmIChleHBvcnRNYXAgPT09IG51bGwpIHJldHVybiBudWxsO1xuXG4gIGNvbnN0IHN0YXRzID0gZnMuc3RhdFN5bmMocGF0aCk7XG4gIGlmIChleHBvcnRNYXAgIT0gbnVsbCkge1xuICAgIC8vIGRhdGUgZXF1YWxpdHkgY2hlY2tcbiAgICBpZiAoZXhwb3J0TWFwLm10aW1lIC0gc3RhdHMubXRpbWUgPT09IDApIHtcbiAgICAgIHJldHVybiBleHBvcnRNYXA7XG4gICAgfVxuICAgIC8vIGZ1dHVyZTogY2hlY2sgY29udGVudCBlcXVhbGl0eT9cbiAgfVxuXG4gIC8vIGNoZWNrIHZhbGlkIGV4dGVuc2lvbnMgZmlyc3RcbiAgaWYgKCFoYXNWYWxpZEV4dGVuc2lvbihwYXRoLCBjb250ZXh0KSkge1xuICAgIGV4cG9ydENhY2hlLnNldChjYWNoZUtleSwgbnVsbCk7XG4gICAgcmV0dXJuIG51bGw7XG4gIH1cblxuICAvLyBjaGVjayBmb3IgYW5kIGNhY2hlIGlnbm9yZVxuICBpZiAoaXNJZ25vcmVkKHBhdGgsIGNvbnRleHQpKSB7XG4gICAgbG9nKCdpZ25vcmVkIHBhdGggZHVlIHRvIGlnbm9yZSBzZXR0aW5nczonLCBwYXRoKTtcbiAgICBleHBvcnRDYWNoZS5zZXQoY2FjaGVLZXksIG51bGwpO1xuICAgIHJldHVybiBudWxsO1xuICB9XG5cbiAgY29uc3QgY29udGVudCA9IGZzLnJlYWRGaWxlU3luYyhwYXRoLCB7IGVuY29kaW5nOiAndXRmOCcgfSk7XG5cbiAgLy8gY2hlY2sgZm9yIGFuZCBjYWNoZSB1bmFtYmlndW91cyBtb2R1bGVzXG4gIGlmICghdW5hbWJpZ3VvdXMudGVzdChjb250ZW50KSkge1xuICAgIGxvZygnaWdub3JlZCBwYXRoIGR1ZSB0byB1bmFtYmlndW91cyByZWdleDonLCBwYXRoKTtcbiAgICBleHBvcnRDYWNoZS5zZXQoY2FjaGVLZXksIG51bGwpO1xuICAgIHJldHVybiBudWxsO1xuICB9XG5cbiAgbG9nKCdjYWNoZSBtaXNzJywgY2FjaGVLZXksICdmb3IgcGF0aCcsIHBhdGgpO1xuICBleHBvcnRNYXAgPSBFeHBvcnRNYXAucGFyc2UocGF0aCwgY29udGVudCwgY29udGV4dCk7XG5cbiAgLy8gYW1iaWd1b3VzIG1vZHVsZXMgcmV0dXJuIG51bGxcbiAgaWYgKGV4cG9ydE1hcCA9PSBudWxsKSByZXR1cm4gbnVsbDtcblxuICBleHBvcnRNYXAubXRpbWUgPSBzdGF0cy5tdGltZTtcblxuICBleHBvcnRDYWNoZS5zZXQoY2FjaGVLZXksIGV4cG9ydE1hcCk7XG4gIHJldHVybiBleHBvcnRNYXA7XG59O1xuXG5cbkV4cG9ydE1hcC5wYXJzZSA9IGZ1bmN0aW9uIChwYXRoLCBjb250ZW50LCBjb250ZXh0KSB7XG4gIGNvbnN0IG0gPSBuZXcgRXhwb3J0TWFwKHBhdGgpO1xuICBjb25zdCBpc0VzTW9kdWxlSW50ZXJvcFRydWUgPSBpc0VzTW9kdWxlSW50ZXJvcCgpO1xuXG4gIGxldCBhc3Q7XG4gIGxldCB2aXNpdG9yS2V5cztcbiAgdHJ5IHtcbiAgICBjb25zdCByZXN1bHQgPSBwYXJzZShwYXRoLCBjb250ZW50LCBjb250ZXh0KTtcbiAgICBhc3QgPSByZXN1bHQuYXN0O1xuICAgIHZpc2l0b3JLZXlzID0gcmVzdWx0LnZpc2l0b3JLZXlzO1xuICB9IGNhdGNoIChlcnIpIHtcbiAgICBtLmVycm9ycy5wdXNoKGVycik7XG4gICAgcmV0dXJuIG07IC8vIGNhbid0IGNvbnRpbnVlXG4gIH1cblxuICBtLnZpc2l0b3JLZXlzID0gdmlzaXRvcktleXM7XG5cbiAgbGV0IGhhc0R5bmFtaWNJbXBvcnRzID0gZmFsc2U7XG5cbiAgZnVuY3Rpb24gcHJvY2Vzc0R5bmFtaWNJbXBvcnQoc291cmNlKSB7XG4gICAgaGFzRHluYW1pY0ltcG9ydHMgPSB0cnVlO1xuICAgIGlmIChzb3VyY2UudHlwZSAhPT0gJ0xpdGVyYWwnKSB7XG4gICAgICByZXR1cm4gbnVsbDtcbiAgICB9XG4gICAgY29uc3QgcCA9IHJlbW90ZVBhdGgoc291cmNlLnZhbHVlKTtcbiAgICBpZiAocCA9PSBudWxsKSB7XG4gICAgICByZXR1cm4gbnVsbDtcbiAgICB9XG4gICAgY29uc3QgaW1wb3J0ZWRTcGVjaWZpZXJzID0gbmV3IFNldCgpO1xuICAgIGltcG9ydGVkU3BlY2lmaWVycy5hZGQoJ0ltcG9ydE5hbWVzcGFjZVNwZWNpZmllcicpO1xuICAgIGNvbnN0IGdldHRlciA9IHRodW5rRm9yKHAsIGNvbnRleHQpO1xuICAgIG0uaW1wb3J0cy5zZXQocCwge1xuICAgICAgZ2V0dGVyLFxuICAgICAgZGVjbGFyYXRpb25zOiBuZXcgU2V0KFt7XG4gICAgICAgIHNvdXJjZToge1xuICAgICAgICAvLyBjYXB0dXJpbmcgYWN0dWFsIG5vZGUgcmVmZXJlbmNlIGhvbGRzIGZ1bGwgQVNUIGluIG1lbW9yeSFcbiAgICAgICAgICB2YWx1ZTogc291cmNlLnZhbHVlLFxuICAgICAgICAgIGxvYzogc291cmNlLmxvYyxcbiAgICAgICAgfSxcbiAgICAgICAgaW1wb3J0ZWRTcGVjaWZpZXJzLFxuICAgICAgfV0pLFxuICAgIH0pO1xuICB9XG5cbiAgdmlzaXQoYXN0LCB2aXNpdG9yS2V5cywge1xuICAgIEltcG9ydEV4cHJlc3Npb24obm9kZSkge1xuICAgICAgcHJvY2Vzc0R5bmFtaWNJbXBvcnQobm9kZS5zb3VyY2UpO1xuICAgIH0sXG4gICAgQ2FsbEV4cHJlc3Npb24obm9kZSkge1xuICAgICAgaWYgKG5vZGUuY2FsbGVlLnR5cGUgPT09ICdJbXBvcnQnKSB7XG4gICAgICAgIHByb2Nlc3NEeW5hbWljSW1wb3J0KG5vZGUuYXJndW1lbnRzWzBdKTtcbiAgICAgIH1cbiAgICB9LFxuICB9KTtcblxuICBjb25zdCB1bmFtYmlndW91c2x5RVNNID0gdW5hbWJpZ3VvdXMuaXNNb2R1bGUoYXN0KTtcbiAgaWYgKCF1bmFtYmlndW91c2x5RVNNICYmICFoYXNEeW5hbWljSW1wb3J0cykgcmV0dXJuIG51bGw7XG5cbiAgY29uc3QgZG9jc3R5bGUgPSAoY29udGV4dC5zZXR0aW5ncyAmJiBjb250ZXh0LnNldHRpbmdzWydpbXBvcnQvZG9jc3R5bGUnXSkgfHwgWydqc2RvYyddO1xuICBjb25zdCBkb2NTdHlsZVBhcnNlcnMgPSB7fTtcbiAgZG9jc3R5bGUuZm9yRWFjaChzdHlsZSA9PiB7XG4gICAgZG9jU3R5bGVQYXJzZXJzW3N0eWxlXSA9IGF2YWlsYWJsZURvY1N0eWxlUGFyc2Vyc1tzdHlsZV07XG4gIH0pO1xuXG4gIC8vIGF0dGVtcHQgdG8gY29sbGVjdCBtb2R1bGUgZG9jXG4gIGlmIChhc3QuY29tbWVudHMpIHtcbiAgICBhc3QuY29tbWVudHMuc29tZShjID0+IHtcbiAgICAgIGlmIChjLnR5cGUgIT09ICdCbG9jaycpIHJldHVybiBmYWxzZTtcbiAgICAgIHRyeSB7XG4gICAgICAgIGNvbnN0IGRvYyA9IGRvY3RyaW5lLnBhcnNlKGMudmFsdWUsIHsgdW53cmFwOiB0cnVlIH0pO1xuICAgICAgICBpZiAoZG9jLnRhZ3Muc29tZSh0ID0+IHQudGl0bGUgPT09ICdtb2R1bGUnKSkge1xuICAgICAgICAgIG0uZG9jID0gZG9jO1xuICAgICAgICAgIHJldHVybiB0cnVlO1xuICAgICAgICB9XG4gICAgICB9IGNhdGNoIChlcnIpIHsgLyogaWdub3JlICovIH1cbiAgICAgIHJldHVybiBmYWxzZTtcbiAgICB9KTtcbiAgfVxuXG4gIGNvbnN0IG5hbWVzcGFjZXMgPSBuZXcgTWFwKCk7XG5cbiAgZnVuY3Rpb24gcmVtb3RlUGF0aCh2YWx1ZSkge1xuICAgIHJldHVybiByZXNvbHZlLnJlbGF0aXZlKHZhbHVlLCBwYXRoLCBjb250ZXh0LnNldHRpbmdzKTtcbiAgfVxuXG4gIGZ1bmN0aW9uIHJlc29sdmVJbXBvcnQodmFsdWUpIHtcbiAgICBjb25zdCBycCA9IHJlbW90ZVBhdGgodmFsdWUpO1xuICAgIGlmIChycCA9PSBudWxsKSByZXR1cm4gbnVsbDtcbiAgICByZXR1cm4gRXhwb3J0TWFwLmZvcihjaGlsZENvbnRleHQocnAsIGNvbnRleHQpKTtcbiAgfVxuXG4gIGZ1bmN0aW9uIGdldE5hbWVzcGFjZShpZGVudGlmaWVyKSB7XG4gICAgaWYgKCFuYW1lc3BhY2VzLmhhcyhpZGVudGlmaWVyLm5hbWUpKSByZXR1cm47XG5cbiAgICByZXR1cm4gZnVuY3Rpb24gKCkge1xuICAgICAgcmV0dXJuIHJlc29sdmVJbXBvcnQobmFtZXNwYWNlcy5nZXQoaWRlbnRpZmllci5uYW1lKSk7XG4gICAgfTtcbiAgfVxuXG4gIGZ1bmN0aW9uIGFkZE5hbWVzcGFjZShvYmplY3QsIGlkZW50aWZpZXIpIHtcbiAgICBjb25zdCBuc2ZuID0gZ2V0TmFtZXNwYWNlKGlkZW50aWZpZXIpO1xuICAgIGlmIChuc2ZuKSB7XG4gICAgICBPYmplY3QuZGVmaW5lUHJvcGVydHkob2JqZWN0LCAnbmFtZXNwYWNlJywgeyBnZXQ6IG5zZm4gfSk7XG4gICAgfVxuXG4gICAgcmV0dXJuIG9iamVjdDtcbiAgfVxuXG4gIGZ1bmN0aW9uIHByb2Nlc3NTcGVjaWZpZXIocywgbiwgbSkge1xuICAgIGNvbnN0IG5zb3VyY2UgPSBuLnNvdXJjZSAmJiBuLnNvdXJjZS52YWx1ZTtcbiAgICBjb25zdCBleHBvcnRNZXRhID0ge307XG4gICAgbGV0IGxvY2FsO1xuXG4gICAgc3dpdGNoIChzLnR5cGUpIHtcbiAgICBjYXNlICdFeHBvcnREZWZhdWx0U3BlY2lmaWVyJzpcbiAgICAgIGlmICghbnNvdXJjZSkgcmV0dXJuO1xuICAgICAgbG9jYWwgPSAnZGVmYXVsdCc7XG4gICAgICBicmVhaztcbiAgICBjYXNlICdFeHBvcnROYW1lc3BhY2VTcGVjaWZpZXInOlxuICAgICAgbS5uYW1lc3BhY2Uuc2V0KHMuZXhwb3J0ZWQubmFtZSwgT2JqZWN0LmRlZmluZVByb3BlcnR5KGV4cG9ydE1ldGEsICduYW1lc3BhY2UnLCB7XG4gICAgICAgIGdldCgpIHsgcmV0dXJuIHJlc29sdmVJbXBvcnQobnNvdXJjZSk7IH0sXG4gICAgICB9KSk7XG4gICAgICByZXR1cm47XG4gICAgY2FzZSAnRXhwb3J0QWxsRGVjbGFyYXRpb24nOlxuICAgICAgbS5uYW1lc3BhY2Uuc2V0KHMuZXhwb3J0ZWQubmFtZSB8fCBzLmV4cG9ydGVkLnZhbHVlLCBhZGROYW1lc3BhY2UoZXhwb3J0TWV0YSwgcy5zb3VyY2UudmFsdWUpKTtcbiAgICAgIHJldHVybjtcbiAgICBjYXNlICdFeHBvcnRTcGVjaWZpZXInOlxuICAgICAgaWYgKCFuLnNvdXJjZSkge1xuICAgICAgICBtLm5hbWVzcGFjZS5zZXQocy5leHBvcnRlZC5uYW1lIHx8IHMuZXhwb3J0ZWQudmFsdWUsIGFkZE5hbWVzcGFjZShleHBvcnRNZXRhLCBzLmxvY2FsKSk7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cbiAgICAgIC8vIGVsc2UgZmFsbHMgdGhyb3VnaFxuICAgIGRlZmF1bHQ6XG4gICAgICBsb2NhbCA9IHMubG9jYWwubmFtZTtcbiAgICAgIGJyZWFrO1xuICAgIH1cblxuICAgIC8vIHRvZG86IEpTRG9jXG4gICAgbS5yZWV4cG9ydHMuc2V0KHMuZXhwb3J0ZWQubmFtZSwgeyBsb2NhbCwgZ2V0SW1wb3J0OiAoKSA9PiByZXNvbHZlSW1wb3J0KG5zb3VyY2UpIH0pO1xuICB9XG5cbiAgZnVuY3Rpb24gY2FwdHVyZURlcGVuZGVuY3koeyBzb3VyY2UgfSwgaXNPbmx5SW1wb3J0aW5nVHlwZXMsIGltcG9ydGVkU3BlY2lmaWVycyA9IG5ldyBTZXQoKSkge1xuICAgIGlmIChzb3VyY2UgPT0gbnVsbCkgcmV0dXJuIG51bGw7XG5cbiAgICBjb25zdCBwID0gcmVtb3RlUGF0aChzb3VyY2UudmFsdWUpO1xuICAgIGlmIChwID09IG51bGwpIHJldHVybiBudWxsO1xuXG4gICAgY29uc3QgZGVjbGFyYXRpb25NZXRhZGF0YSA9IHtcbiAgICAgIC8vIGNhcHR1cmluZyBhY3R1YWwgbm9kZSByZWZlcmVuY2UgaG9sZHMgZnVsbCBBU1QgaW4gbWVtb3J5IVxuICAgICAgc291cmNlOiB7IHZhbHVlOiBzb3VyY2UudmFsdWUsIGxvYzogc291cmNlLmxvYyB9LFxuICAgICAgaXNPbmx5SW1wb3J0aW5nVHlwZXMsXG4gICAgICBpbXBvcnRlZFNwZWNpZmllcnMsXG4gICAgfTtcblxuICAgIGNvbnN0IGV4aXN0aW5nID0gbS5pbXBvcnRzLmdldChwKTtcbiAgICBpZiAoZXhpc3RpbmcgIT0gbnVsbCkge1xuICAgICAgZXhpc3RpbmcuZGVjbGFyYXRpb25zLmFkZChkZWNsYXJhdGlvbk1ldGFkYXRhKTtcbiAgICAgIHJldHVybiBleGlzdGluZy5nZXR0ZXI7XG4gICAgfVxuXG4gICAgY29uc3QgZ2V0dGVyID0gdGh1bmtGb3IocCwgY29udGV4dCk7XG4gICAgbS5pbXBvcnRzLnNldChwLCB7IGdldHRlciwgZGVjbGFyYXRpb25zOiBuZXcgU2V0KFtkZWNsYXJhdGlvbk1ldGFkYXRhXSkgfSk7XG4gICAgcmV0dXJuIGdldHRlcjtcbiAgfVxuXG4gIGNvbnN0IHNvdXJjZSA9IG1ha2VTb3VyY2VDb2RlKGNvbnRlbnQsIGFzdCk7XG5cbiAgZnVuY3Rpb24gcmVhZFRzQ29uZmlnKCkge1xuICAgIGNvbnN0IHRzQ29uZmlnSW5mbyA9IHRzQ29uZmlnTG9hZGVyKHtcbiAgICAgIGN3ZDpcbiAgICAgICAgKGNvbnRleHQucGFyc2VyT3B0aW9ucyAmJiBjb250ZXh0LnBhcnNlck9wdGlvbnMudHNjb25maWdSb290RGlyKSB8fFxuICAgICAgICBwcm9jZXNzLmN3ZCgpLFxuICAgICAgZ2V0RW52OiAoa2V5KSA9PiBwcm9jZXNzLmVudltrZXldLFxuICAgIH0pO1xuICAgIHRyeSB7XG4gICAgICBpZiAodHNDb25maWdJbmZvLnRzQ29uZmlnUGF0aCAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIC8vIFByb2plY3RzIG5vdCB1c2luZyBUeXBlU2NyaXB0IHdvbid0IGhhdmUgYHR5cGVzY3JpcHRgIGluc3RhbGxlZC5cbiAgICAgICAgaWYgKCF0cykgeyB0cyA9IHJlcXVpcmUoJ3R5cGVzY3JpcHQnKTsgfVxuICBcbiAgICAgICAgY29uc3QgY29uZmlnRmlsZSA9IHRzLnJlYWRDb25maWdGaWxlKHRzQ29uZmlnSW5mby50c0NvbmZpZ1BhdGgsIHRzLnN5cy5yZWFkRmlsZSk7XG4gICAgICAgIHJldHVybiB0cy5wYXJzZUpzb25Db25maWdGaWxlQ29udGVudChcbiAgICAgICAgICBjb25maWdGaWxlLmNvbmZpZyxcbiAgICAgICAgICB0cy5zeXMsXG4gICAgICAgICAgZGlybmFtZSh0c0NvbmZpZ0luZm8udHNDb25maWdQYXRoKSxcbiAgICAgICAgKTtcbiAgICAgIH1cbiAgICB9IGNhdGNoIChlKSB7XG4gICAgICAvLyBDYXRjaCBhbnkgZXJyb3JzXG4gICAgfVxuXG4gICAgcmV0dXJuIG51bGw7XG4gIH1cblxuICBmdW5jdGlvbiBpc0VzTW9kdWxlSW50ZXJvcCgpIHtcbiAgICBjb25zdCBjYWNoZUtleSA9IGhhc2hPYmplY3Qoe1xuICAgICAgdHNjb25maWdSb290RGlyOiBjb250ZXh0LnBhcnNlck9wdGlvbnMgJiYgY29udGV4dC5wYXJzZXJPcHRpb25zLnRzY29uZmlnUm9vdERpcixcbiAgICB9KS5kaWdlc3QoJ2hleCcpO1xuICAgIGxldCB0c0NvbmZpZyA9IHRzQ29uZmlnQ2FjaGUuZ2V0KGNhY2hlS2V5KTtcbiAgICBpZiAodHlwZW9mIHRzQ29uZmlnID09PSAndW5kZWZpbmVkJykge1xuICAgICAgdHNDb25maWcgPSByZWFkVHNDb25maWcoY29udGV4dCk7XG4gICAgICB0c0NvbmZpZ0NhY2hlLnNldChjYWNoZUtleSwgdHNDb25maWcpO1xuICAgIH1cblxuICAgIHJldHVybiB0c0NvbmZpZyAmJiB0c0NvbmZpZy5vcHRpb25zID8gdHNDb25maWcub3B0aW9ucy5lc01vZHVsZUludGVyb3AgOiBmYWxzZTtcbiAgfVxuXG4gIGFzdC5ib2R5LmZvckVhY2goZnVuY3Rpb24gKG4pIHtcbiAgICBpZiAobi50eXBlID09PSAnRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uJykge1xuICAgICAgY29uc3QgZXhwb3J0TWV0YSA9IGNhcHR1cmVEb2Moc291cmNlLCBkb2NTdHlsZVBhcnNlcnMsIG4pO1xuICAgICAgaWYgKG4uZGVjbGFyYXRpb24udHlwZSA9PT0gJ0lkZW50aWZpZXInKSB7XG4gICAgICAgIGFkZE5hbWVzcGFjZShleHBvcnRNZXRhLCBuLmRlY2xhcmF0aW9uKTtcbiAgICAgIH1cbiAgICAgIG0ubmFtZXNwYWNlLnNldCgnZGVmYXVsdCcsIGV4cG9ydE1ldGEpO1xuICAgICAgcmV0dXJuO1xuICAgIH1cblxuICAgIGlmIChuLnR5cGUgPT09ICdFeHBvcnRBbGxEZWNsYXJhdGlvbicpIHtcbiAgICAgIGNvbnN0IGdldHRlciA9IGNhcHR1cmVEZXBlbmRlbmN5KG4sIG4uZXhwb3J0S2luZCA9PT0gJ3R5cGUnKTtcbiAgICAgIGlmIChnZXR0ZXIpIG0uZGVwZW5kZW5jaWVzLmFkZChnZXR0ZXIpO1xuICAgICAgaWYgKG4uZXhwb3J0ZWQpIHtcbiAgICAgICAgcHJvY2Vzc1NwZWNpZmllcihuLCBuLmV4cG9ydGVkLCBtKTtcbiAgICAgIH1cbiAgICAgIHJldHVybjtcbiAgICB9XG5cbiAgICAvLyBjYXB0dXJlIG5hbWVzcGFjZXMgaW4gY2FzZSBvZiBsYXRlciBleHBvcnRcbiAgICBpZiAobi50eXBlID09PSAnSW1wb3J0RGVjbGFyYXRpb24nKSB7XG4gICAgICAvLyBpbXBvcnQgdHlwZSB7IEZvbyB9IChUUyBhbmQgRmxvdylcbiAgICAgIGNvbnN0IGRlY2xhcmF0aW9uSXNUeXBlID0gbi5pbXBvcnRLaW5kID09PSAndHlwZSc7XG4gICAgICAvLyBpbXBvcnQgJy4vZm9vJyBvciBpbXBvcnQge30gZnJvbSAnLi9mb28nIChib3RoIDAgc3BlY2lmaWVycykgaXMgYSBzaWRlIGVmZmVjdCBhbmRcbiAgICAgIC8vIHNob3VsZG4ndCBiZSBjb25zaWRlcmVkIHRvIGJlIGp1c3QgaW1wb3J0aW5nIHR5cGVzXG4gICAgICBsZXQgc3BlY2lmaWVyc09ubHlJbXBvcnRpbmdUeXBlcyA9IG4uc3BlY2lmaWVycy5sZW5ndGg7XG4gICAgICBjb25zdCBpbXBvcnRlZFNwZWNpZmllcnMgPSBuZXcgU2V0KCk7XG4gICAgICBuLnNwZWNpZmllcnMuZm9yRWFjaChzcGVjaWZpZXIgPT4ge1xuICAgICAgICBpZiAoc3VwcG9ydGVkSW1wb3J0VHlwZXMuaGFzKHNwZWNpZmllci50eXBlKSkge1xuICAgICAgICAgIGltcG9ydGVkU3BlY2lmaWVycy5hZGQoc3BlY2lmaWVyLnR5cGUpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChzcGVjaWZpZXIudHlwZSA9PT0gJ0ltcG9ydFNwZWNpZmllcicpIHtcbiAgICAgICAgICBpbXBvcnRlZFNwZWNpZmllcnMuYWRkKHNwZWNpZmllci5pbXBvcnRlZC5uYW1lIHx8IHNwZWNpZmllci5pbXBvcnRlZC52YWx1ZSk7XG4gICAgICAgIH1cblxuICAgICAgICAvLyBpbXBvcnQgeyB0eXBlIEZvbyB9IChGbG93KVxuICAgICAgICBzcGVjaWZpZXJzT25seUltcG9ydGluZ1R5cGVzID1cbiAgICAgICAgICBzcGVjaWZpZXJzT25seUltcG9ydGluZ1R5cGVzICYmIHNwZWNpZmllci5pbXBvcnRLaW5kID09PSAndHlwZSc7XG4gICAgICB9KTtcbiAgICAgIGNhcHR1cmVEZXBlbmRlbmN5KG4sIGRlY2xhcmF0aW9uSXNUeXBlIHx8IHNwZWNpZmllcnNPbmx5SW1wb3J0aW5nVHlwZXMsIGltcG9ydGVkU3BlY2lmaWVycyk7XG5cbiAgICAgIGNvbnN0IG5zID0gbi5zcGVjaWZpZXJzLmZpbmQocyA9PiBzLnR5cGUgPT09ICdJbXBvcnROYW1lc3BhY2VTcGVjaWZpZXInKTtcbiAgICAgIGlmIChucykge1xuICAgICAgICBuYW1lc3BhY2VzLnNldChucy5sb2NhbC5uYW1lLCBuLnNvdXJjZS52YWx1ZSk7XG4gICAgICB9XG4gICAgICByZXR1cm47XG4gICAgfVxuXG4gICAgaWYgKG4udHlwZSA9PT0gJ0V4cG9ydE5hbWVkRGVjbGFyYXRpb24nKSB7XG4gICAgICAvLyBjYXB0dXJlIGRlY2xhcmF0aW9uXG4gICAgICBpZiAobi5kZWNsYXJhdGlvbiAhPSBudWxsKSB7XG4gICAgICAgIHN3aXRjaCAobi5kZWNsYXJhdGlvbi50eXBlKSB7XG4gICAgICAgIGNhc2UgJ0Z1bmN0aW9uRGVjbGFyYXRpb24nOlxuICAgICAgICBjYXNlICdDbGFzc0RlY2xhcmF0aW9uJzpcbiAgICAgICAgY2FzZSAnVHlwZUFsaWFzJzogLy8gZmxvd3R5cGUgd2l0aCBiYWJlbC1lc2xpbnQgcGFyc2VyXG4gICAgICAgIGNhc2UgJ0ludGVyZmFjZURlY2xhcmF0aW9uJzpcbiAgICAgICAgY2FzZSAnRGVjbGFyZUZ1bmN0aW9uJzpcbiAgICAgICAgY2FzZSAnVFNEZWNsYXJlRnVuY3Rpb24nOlxuICAgICAgICBjYXNlICdUU0VudW1EZWNsYXJhdGlvbic6XG4gICAgICAgIGNhc2UgJ1RTVHlwZUFsaWFzRGVjbGFyYXRpb24nOlxuICAgICAgICBjYXNlICdUU0ludGVyZmFjZURlY2xhcmF0aW9uJzpcbiAgICAgICAgY2FzZSAnVFNBYnN0cmFjdENsYXNzRGVjbGFyYXRpb24nOlxuICAgICAgICBjYXNlICdUU01vZHVsZURlY2xhcmF0aW9uJzpcbiAgICAgICAgICBtLm5hbWVzcGFjZS5zZXQobi5kZWNsYXJhdGlvbi5pZC5uYW1lLCBjYXB0dXJlRG9jKHNvdXJjZSwgZG9jU3R5bGVQYXJzZXJzLCBuKSk7XG4gICAgICAgICAgYnJlYWs7XG4gICAgICAgIGNhc2UgJ1ZhcmlhYmxlRGVjbGFyYXRpb24nOlxuICAgICAgICAgIG4uZGVjbGFyYXRpb24uZGVjbGFyYXRpb25zLmZvckVhY2goKGQpID0+XG4gICAgICAgICAgICByZWN1cnNpdmVQYXR0ZXJuQ2FwdHVyZShkLmlkLFxuICAgICAgICAgICAgICBpZCA9PiBtLm5hbWVzcGFjZS5zZXQoaWQubmFtZSwgY2FwdHVyZURvYyhzb3VyY2UsIGRvY1N0eWxlUGFyc2VycywgZCwgbikpKSk7XG4gICAgICAgICAgYnJlYWs7XG4gICAgICAgIH1cbiAgICAgIH1cblxuICAgICAgbi5zcGVjaWZpZXJzLmZvckVhY2goKHMpID0+IHByb2Nlc3NTcGVjaWZpZXIocywgbiwgbSkpO1xuICAgIH1cblxuICAgIGNvbnN0IGV4cG9ydHMgPSBbJ1RTRXhwb3J0QXNzaWdubWVudCddO1xuICAgIGlmIChpc0VzTW9kdWxlSW50ZXJvcFRydWUpIHtcbiAgICAgIGV4cG9ydHMucHVzaCgnVFNOYW1lc3BhY2VFeHBvcnREZWNsYXJhdGlvbicpO1xuICAgIH1cblxuICAgIC8vIFRoaXMgZG9lc24ndCBkZWNsYXJlIGFueXRoaW5nLCBidXQgY2hhbmdlcyB3aGF0J3MgYmVpbmcgZXhwb3J0ZWQuXG4gICAgaWYgKGluY2x1ZGVzKGV4cG9ydHMsIG4udHlwZSkpIHtcbiAgICAgIGNvbnN0IGV4cG9ydGVkTmFtZSA9IG4udHlwZSA9PT0gJ1RTTmFtZXNwYWNlRXhwb3J0RGVjbGFyYXRpb24nXG4gICAgICAgID8gKG4uaWQgfHwgbi5uYW1lKS5uYW1lXG4gICAgICAgIDogKG4uZXhwcmVzc2lvbiAmJiBuLmV4cHJlc3Npb24ubmFtZSB8fCAobi5leHByZXNzaW9uLmlkICYmIG4uZXhwcmVzc2lvbi5pZC5uYW1lKSB8fCBudWxsKTtcbiAgICAgIGNvbnN0IGRlY2xUeXBlcyA9IFtcbiAgICAgICAgJ1ZhcmlhYmxlRGVjbGFyYXRpb24nLFxuICAgICAgICAnQ2xhc3NEZWNsYXJhdGlvbicsXG4gICAgICAgICdUU0RlY2xhcmVGdW5jdGlvbicsXG4gICAgICAgICdUU0VudW1EZWNsYXJhdGlvbicsXG4gICAgICAgICdUU1R5cGVBbGlhc0RlY2xhcmF0aW9uJyxcbiAgICAgICAgJ1RTSW50ZXJmYWNlRGVjbGFyYXRpb24nLFxuICAgICAgICAnVFNBYnN0cmFjdENsYXNzRGVjbGFyYXRpb24nLFxuICAgICAgICAnVFNNb2R1bGVEZWNsYXJhdGlvbicsXG4gICAgICBdO1xuICAgICAgY29uc3QgZXhwb3J0ZWREZWNscyA9IGFzdC5ib2R5LmZpbHRlcigoeyB0eXBlLCBpZCwgZGVjbGFyYXRpb25zIH0pID0+IGluY2x1ZGVzKGRlY2xUeXBlcywgdHlwZSkgJiYgKFxuICAgICAgICAoaWQgJiYgaWQubmFtZSA9PT0gZXhwb3J0ZWROYW1lKSB8fCAoZGVjbGFyYXRpb25zICYmIGRlY2xhcmF0aW9ucy5maW5kKChkKSA9PiBkLmlkLm5hbWUgPT09IGV4cG9ydGVkTmFtZSkpXG4gICAgICApKTtcbiAgICAgIGlmIChleHBvcnRlZERlY2xzLmxlbmd0aCA9PT0gMCkge1xuICAgICAgICAvLyBFeHBvcnQgaXMgbm90IHJlZmVyZW5jaW5nIGFueSBsb2NhbCBkZWNsYXJhdGlvbiwgbXVzdCBiZSByZS1leHBvcnRpbmdcbiAgICAgICAgbS5uYW1lc3BhY2Uuc2V0KCdkZWZhdWx0JywgY2FwdHVyZURvYyhzb3VyY2UsIGRvY1N0eWxlUGFyc2VycywgbikpO1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG4gICAgICBpZiAoXG4gICAgICAgIGlzRXNNb2R1bGVJbnRlcm9wVHJ1ZSAvLyBlc01vZHVsZUludGVyb3AgaXMgb24gaW4gdHNjb25maWdcbiAgICAgICAgJiYgIW0ubmFtZXNwYWNlLmhhcygnZGVmYXVsdCcpIC8vIGFuZCBkZWZhdWx0IGlzbid0IGFkZGVkIGFscmVhZHlcbiAgICAgICkge1xuICAgICAgICBtLm5hbWVzcGFjZS5zZXQoJ2RlZmF1bHQnLCB7fSk7IC8vIGFkZCBkZWZhdWx0IGV4cG9ydFxuICAgICAgfVxuICAgICAgZXhwb3J0ZWREZWNscy5mb3JFYWNoKChkZWNsKSA9PiB7XG4gICAgICAgIGlmIChkZWNsLnR5cGUgPT09ICdUU01vZHVsZURlY2xhcmF0aW9uJykge1xuICAgICAgICAgIGlmIChkZWNsLmJvZHkgJiYgZGVjbC5ib2R5LnR5cGUgPT09ICdUU01vZHVsZURlY2xhcmF0aW9uJykge1xuICAgICAgICAgICAgbS5uYW1lc3BhY2Uuc2V0KGRlY2wuYm9keS5pZC5uYW1lLCBjYXB0dXJlRG9jKHNvdXJjZSwgZG9jU3R5bGVQYXJzZXJzLCBkZWNsLmJvZHkpKTtcbiAgICAgICAgICB9IGVsc2UgaWYgKGRlY2wuYm9keSAmJiBkZWNsLmJvZHkuYm9keSkge1xuICAgICAgICAgICAgZGVjbC5ib2R5LmJvZHkuZm9yRWFjaCgobW9kdWxlQmxvY2tOb2RlKSA9PiB7XG4gICAgICAgICAgICAgIC8vIEV4cG9ydC1hc3NpZ25tZW50IGV4cG9ydHMgYWxsIG1lbWJlcnMgaW4gdGhlIG5hbWVzcGFjZSxcbiAgICAgICAgICAgICAgLy8gZXhwbGljaXRseSBleHBvcnRlZCBvciBub3QuXG4gICAgICAgICAgICAgIGNvbnN0IG5hbWVzcGFjZURlY2wgPSBtb2R1bGVCbG9ja05vZGUudHlwZSA9PT0gJ0V4cG9ydE5hbWVkRGVjbGFyYXRpb24nID9cbiAgICAgICAgICAgICAgICBtb2R1bGVCbG9ja05vZGUuZGVjbGFyYXRpb24gOlxuICAgICAgICAgICAgICAgIG1vZHVsZUJsb2NrTm9kZTtcblxuICAgICAgICAgICAgICBpZiAoIW5hbWVzcGFjZURlY2wpIHtcbiAgICAgICAgICAgICAgICAvLyBUeXBlU2NyaXB0IGNhbiBjaGVjayB0aGlzIGZvciB1czsgd2UgbmVlZG4ndFxuICAgICAgICAgICAgICB9IGVsc2UgaWYgKG5hbWVzcGFjZURlY2wudHlwZSA9PT0gJ1ZhcmlhYmxlRGVjbGFyYXRpb24nKSB7XG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlRGVjbC5kZWNsYXJhdGlvbnMuZm9yRWFjaCgoZCkgPT5cbiAgICAgICAgICAgICAgICAgIHJlY3Vyc2l2ZVBhdHRlcm5DYXB0dXJlKGQuaWQsIChpZCkgPT4gbS5uYW1lc3BhY2Uuc2V0KFxuICAgICAgICAgICAgICAgICAgICBpZC5uYW1lLFxuICAgICAgICAgICAgICAgICAgICBjYXB0dXJlRG9jKHNvdXJjZSwgZG9jU3R5bGVQYXJzZXJzLCBkZWNsLCBuYW1lc3BhY2VEZWNsLCBtb2R1bGVCbG9ja05vZGUpLFxuICAgICAgICAgICAgICAgICAgKSksXG4gICAgICAgICAgICAgICAgKTtcbiAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBtLm5hbWVzcGFjZS5zZXQoXG4gICAgICAgICAgICAgICAgICBuYW1lc3BhY2VEZWNsLmlkLm5hbWUsXG4gICAgICAgICAgICAgICAgICBjYXB0dXJlRG9jKHNvdXJjZSwgZG9jU3R5bGVQYXJzZXJzLCBtb2R1bGVCbG9ja05vZGUpKTtcbiAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgfVxuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgIC8vIEV4cG9ydCBhcyBkZWZhdWx0XG4gICAgICAgICAgbS5uYW1lc3BhY2Uuc2V0KCdkZWZhdWx0JywgY2FwdHVyZURvYyhzb3VyY2UsIGRvY1N0eWxlUGFyc2VycywgZGVjbCkpO1xuICAgICAgICB9XG4gICAgICB9KTtcbiAgICB9XG4gIH0pO1xuXG4gIGlmIChcbiAgICBpc0VzTW9kdWxlSW50ZXJvcFRydWUgLy8gZXNNb2R1bGVJbnRlcm9wIGlzIG9uIGluIHRzY29uZmlnXG4gICAgJiYgbS5uYW1lc3BhY2Uuc2l6ZSA+IDAgLy8gYW55dGhpbmcgaXMgZXhwb3J0ZWRcbiAgICAmJiAhbS5uYW1lc3BhY2UuaGFzKCdkZWZhdWx0JykgLy8gYW5kIGRlZmF1bHQgaXNuJ3QgYWRkZWQgYWxyZWFkeVxuICApIHtcbiAgICBtLm5hbWVzcGFjZS5zZXQoJ2RlZmF1bHQnLCB7fSk7IC8vIGFkZCBkZWZhdWx0IGV4cG9ydFxuICB9XG5cbiAgaWYgKHVuYW1iaWd1b3VzbHlFU00pIHtcbiAgICBtLnBhcnNlR29hbCA9ICdNb2R1bGUnO1xuICB9XG4gIHJldHVybiBtO1xufTtcblxuLyoqXG4gKiBUaGUgY3JlYXRpb24gb2YgdGhpcyBjbG9zdXJlIGlzIGlzb2xhdGVkIGZyb20gb3RoZXIgc2NvcGVzXG4gKiB0byBhdm9pZCBvdmVyLXJldGVudGlvbiBvZiB1bnJlbGF0ZWQgdmFyaWFibGVzLCB3aGljaCBoYXNcbiAqIGNhdXNlZCBtZW1vcnkgbGVha3MuIFNlZSAjMTI2Ni5cbiAqL1xuZnVuY3Rpb24gdGh1bmtGb3IocCwgY29udGV4dCkge1xuICByZXR1cm4gKCkgPT4gRXhwb3J0TWFwLmZvcihjaGlsZENvbnRleHQocCwgY29udGV4dCkpO1xufVxuXG5cbi8qKlxuICogVHJhdmVyc2UgYSBwYXR0ZXJuL2lkZW50aWZpZXIgbm9kZSwgY2FsbGluZyAnY2FsbGJhY2snXG4gKiBmb3IgZWFjaCBsZWFmIGlkZW50aWZpZXIuXG4gKiBAcGFyYW0gIHtub2RlfSAgIHBhdHRlcm5cbiAqIEBwYXJhbSAge0Z1bmN0aW9ufSBjYWxsYmFja1xuICogQHJldHVybiB7dm9pZH1cbiAqL1xuZXhwb3J0IGZ1bmN0aW9uIHJlY3Vyc2l2ZVBhdHRlcm5DYXB0dXJlKHBhdHRlcm4sIGNhbGxiYWNrKSB7XG4gIHN3aXRjaCAocGF0dGVybi50eXBlKSB7XG4gIGNhc2UgJ0lkZW50aWZpZXInOiAvLyBiYXNlIGNhc2VcbiAgICBjYWxsYmFjayhwYXR0ZXJuKTtcbiAgICBicmVhaztcblxuICBjYXNlICdPYmplY3RQYXR0ZXJuJzpcbiAgICBwYXR0ZXJuLnByb3BlcnRpZXMuZm9yRWFjaChwID0+IHtcbiAgICAgIGlmIChwLnR5cGUgPT09ICdFeHBlcmltZW50YWxSZXN0UHJvcGVydHknIHx8IHAudHlwZSA9PT0gJ1Jlc3RFbGVtZW50Jykge1xuICAgICAgICBjYWxsYmFjayhwLmFyZ3VtZW50KTtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuICAgICAgcmVjdXJzaXZlUGF0dGVybkNhcHR1cmUocC52YWx1ZSwgY2FsbGJhY2spO1xuICAgIH0pO1xuICAgIGJyZWFrO1xuXG4gIGNhc2UgJ0FycmF5UGF0dGVybic6XG4gICAgcGF0dGVybi5lbGVtZW50cy5mb3JFYWNoKChlbGVtZW50KSA9PiB7XG4gICAgICBpZiAoZWxlbWVudCA9PSBudWxsKSByZXR1cm47XG4gICAgICBpZiAoZWxlbWVudC50eXBlID09PSAnRXhwZXJpbWVudGFsUmVzdFByb3BlcnR5JyB8fCBlbGVtZW50LnR5cGUgPT09ICdSZXN0RWxlbWVudCcpIHtcbiAgICAgICAgY2FsbGJhY2soZWxlbWVudC5hcmd1bWVudCk7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cbiAgICAgIHJlY3Vyc2l2ZVBhdHRlcm5DYXB0dXJlKGVsZW1lbnQsIGNhbGxiYWNrKTtcbiAgICB9KTtcbiAgICBicmVhaztcblxuICBjYXNlICdBc3NpZ25tZW50UGF0dGVybic6XG4gICAgY2FsbGJhY2socGF0dGVybi5sZWZ0KTtcbiAgICBicmVhaztcbiAgfVxufVxuXG4vKipcbiAqIGRvbid0IGhvbGQgZnVsbCBjb250ZXh0IG9iamVjdCBpbiBtZW1vcnksIGp1c3QgZ3JhYiB3aGF0IHdlIG5lZWQuXG4gKi9cbmZ1bmN0aW9uIGNoaWxkQ29udGV4dChwYXRoLCBjb250ZXh0KSB7XG4gIGNvbnN0IHsgc2V0dGluZ3MsIHBhcnNlck9wdGlvbnMsIHBhcnNlclBhdGggfSA9IGNvbnRleHQ7XG4gIHJldHVybiB7XG4gICAgc2V0dGluZ3MsXG4gICAgcGFyc2VyT3B0aW9ucyxcbiAgICBwYXJzZXJQYXRoLFxuICAgIHBhdGgsXG4gIH07XG59XG5cblxuLyoqXG4gKiBzb21ldGltZXMgbGVnYWN5IHN1cHBvcnQgaXNuJ3QgX3RoYXRfIGhhcmQuLi4gcmlnaHQ/XG4gKi9cbmZ1bmN0aW9uIG1ha2VTb3VyY2VDb2RlKHRleHQsIGFzdCkge1xuICBpZiAoU291cmNlQ29kZS5sZW5ndGggPiAxKSB7XG4gICAgLy8gRVNMaW50IDNcbiAgICByZXR1cm4gbmV3IFNvdXJjZUNvZGUodGV4dCwgYXN0KTtcbiAgfSBlbHNlIHtcbiAgICAvLyBFU0xpbnQgNCwgNVxuICAgIHJldHVybiBuZXcgU291cmNlQ29kZSh7IHRleHQsIGFzdCB9KTtcbiAgfVxufVxuIl19