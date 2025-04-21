'use strict';var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) {return typeof obj;} : function (obj) {return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj;};





var _ExportMap = require('../ExportMap');var _ExportMap2 = _interopRequireDefault(_ExportMap);
var _ignore = require('eslint-module-utils/ignore');
var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);
var _visit = require('eslint-module-utils/visit');var _visit2 = _interopRequireDefault(_visit);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);
var _path = require('path');
var _readPkgUp2 = require('eslint-module-utils/readPkgUp');var _readPkgUp3 = _interopRequireDefault(_readPkgUp2);
var _object = require('object.values');var _object2 = _interopRequireDefault(_object);
var _arrayIncludes = require('array-includes');var _arrayIncludes2 = _interopRequireDefault(_arrayIncludes);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}function _toConsumableArray(arr) {if (Array.isArray(arr)) {for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) {arr2[i] = arr[i];}return arr2;} else {return Array.from(arr);}} /**
                                                                                                                                                                                                                                                                                                                                                                                                       * @fileOverview Ensures that modules contain exports and/or all
                                                                                                                                                                                                                                                                                                                                                                                                       * modules are consumed within other modules.
                                                                                                                                                                                                                                                                                                                                                                                                       * @author René Fermann
                                                                                                                                                                                                                                                                                                                                                                                                       */var FileEnumerator = void 0;var listFilesToProcess = void 0;
try {var _require =
  require('eslint/use-at-your-own-risk');FileEnumerator = _require.FileEnumerator;
} catch (e) {
  try {var _require2 =

    require('eslint/lib/cli-engine/file-enumerator'); // has been moved to eslint/lib/cli-engine/file-enumerator in version 6
    FileEnumerator = _require2.FileEnumerator;} catch (e) {
    try {
      // eslint/lib/util/glob-util has been moved to eslint/lib/util/glob-utils with version 5.3
      var _require3 = require('eslint/lib/util/glob-utils'),originalListFilesToProcess = _require3.listFilesToProcess;

      // Prevent passing invalid options (extensions array) to old versions of the function.
      // https://github.com/eslint/eslint/blob/v5.16.0/lib/util/glob-utils.js#L178-L280
      // https://github.com/eslint/eslint/blob/v5.2.0/lib/util/glob-util.js#L174-L269
      listFilesToProcess = function listFilesToProcess(src, extensions) {
        return originalListFilesToProcess(src, {
          extensions: extensions });

      };
    } catch (e) {var _require4 =
      require('eslint/lib/util/glob-util'),_originalListFilesToProcess = _require4.listFilesToProcess;

      listFilesToProcess = function listFilesToProcess(src, extensions) {
        var patterns = src.reduce(function (carry, pattern) {
          return carry.concat(extensions.map(function (extension) {
            return (/\*\*|\*\./.test(pattern) ? pattern : String(pattern) + '/**/*' + String(extension));
          }));
        }, src.slice());

        return _originalListFilesToProcess(patterns);
      };
    }
  }
}

if (FileEnumerator) {
  listFilesToProcess = function listFilesToProcess(src, extensions) {
    var e = new FileEnumerator({
      extensions: extensions });


    return Array.from(e.iterateFiles(src), function (_ref) {var filePath = _ref.filePath,ignored = _ref.ignored;return {
        ignored: ignored,
        filename: filePath };});

  };
}

var EXPORT_DEFAULT_DECLARATION = 'ExportDefaultDeclaration';
var EXPORT_NAMED_DECLARATION = 'ExportNamedDeclaration';
var EXPORT_ALL_DECLARATION = 'ExportAllDeclaration';
var IMPORT_DECLARATION = 'ImportDeclaration';
var IMPORT_NAMESPACE_SPECIFIER = 'ImportNamespaceSpecifier';
var IMPORT_DEFAULT_SPECIFIER = 'ImportDefaultSpecifier';
var VARIABLE_DECLARATION = 'VariableDeclaration';
var FUNCTION_DECLARATION = 'FunctionDeclaration';
var CLASS_DECLARATION = 'ClassDeclaration';
var IDENTIFIER = 'Identifier';
var OBJECT_PATTERN = 'ObjectPattern';
var TS_INTERFACE_DECLARATION = 'TSInterfaceDeclaration';
var TS_TYPE_ALIAS_DECLARATION = 'TSTypeAliasDeclaration';
var TS_ENUM_DECLARATION = 'TSEnumDeclaration';
var DEFAULT = 'default';

function forEachDeclarationIdentifier(declaration, cb) {
  if (declaration) {
    if (
    declaration.type === FUNCTION_DECLARATION ||
    declaration.type === CLASS_DECLARATION ||
    declaration.type === TS_INTERFACE_DECLARATION ||
    declaration.type === TS_TYPE_ALIAS_DECLARATION ||
    declaration.type === TS_ENUM_DECLARATION)
    {
      cb(declaration.id.name);
    } else if (declaration.type === VARIABLE_DECLARATION) {
      declaration.declarations.forEach(function (_ref2) {var id = _ref2.id;
        if (id.type === OBJECT_PATTERN) {
          (0, _ExportMap.recursivePatternCapture)(id, function (pattern) {
            if (pattern.type === IDENTIFIER) {
              cb(pattern.name);
            }
          });
        } else {
          cb(id.name);
        }
      });
    }
  }
}

/**
   * List of imports per file.
   *
   * Represented by a two-level Map to a Set of identifiers. The upper-level Map
   * keys are the paths to the modules containing the imports, while the
   * lower-level Map keys are the paths to the files which are being imported
   * from. Lastly, the Set of identifiers contains either names being imported
   * or a special AST node name listed above (e.g ImportDefaultSpecifier).
   *
   * For example, if we have a file named foo.js containing:
   *
   *   import { o2 } from './bar.js';
   *
   * Then we will have a structure that looks like:
   *
   *   Map { 'foo.js' => Map { 'bar.js' => Set { 'o2' } } }
   *
   * @type {Map<string, Map<string, Set<string>>>}
   */
var importList = new Map();

/**
                             * List of exports per file.
                             *
                             * Represented by a two-level Map to an object of metadata. The upper-level Map
                             * keys are the paths to the modules containing the exports, while the
                             * lower-level Map keys are the specific identifiers or special AST node names
                             * being exported. The leaf-level metadata object at the moment only contains a
                             * `whereUsed` property, which contains a Set of paths to modules that import
                             * the name.
                             *
                             * For example, if we have a file named bar.js containing the following exports:
                             *
                             *   const o2 = 'bar';
                             *   export { o2 };
                             *
                             * And a file named foo.js containing the following import:
                             *
                             *   import { o2 } from './bar.js';
                             *
                             * Then we will have a structure that looks like:
                             *
                             *   Map { 'bar.js' => Map { 'o2' => { whereUsed: Set { 'foo.js' } } } }
                             *
                             * @type {Map<string, Map<string, object>>}
                             */
var exportList = new Map();

var visitorKeyMap = new Map();

var ignoredFiles = new Set();
var filesOutsideSrc = new Set();

var isNodeModule = function isNodeModule(path) {
  return (/\/(node_modules)\//.test(path));
};

/**
    * read all files matching the patterns in src and ignoreExports
    *
    * return all files matching src pattern, which are not matching the ignoreExports pattern
    */
var resolveFiles = function resolveFiles(src, ignoreExports, context) {
  var extensions = Array.from((0, _ignore.getFileExtensions)(context.settings));

  var srcFiles = new Set();
  var srcFileList = listFilesToProcess(src, extensions);

  // prepare list of ignored files
  var ignoredFilesList = listFilesToProcess(ignoreExports, extensions);
  ignoredFilesList.forEach(function (_ref3) {var filename = _ref3.filename;return ignoredFiles.add(filename);});

  // prepare list of source files, don't consider files from node_modules
  srcFileList.filter(function (_ref4) {var filename = _ref4.filename;return !isNodeModule(filename);}).forEach(function (_ref5) {var filename = _ref5.filename;
    srcFiles.add(filename);
  });
  return srcFiles;
};

/**
    * parse all source files and build up 2 maps containing the existing imports and exports
    */
var prepareImportsAndExports = function prepareImportsAndExports(srcFiles, context) {
  var exportAll = new Map();
  srcFiles.forEach(function (file) {
    var exports = new Map();
    var imports = new Map();
    var currentExports = _ExportMap2['default'].get(file, context);
    if (currentExports) {var

      dependencies =




      currentExports.dependencies,reexports = currentExports.reexports,localImportList = currentExports.imports,namespace = currentExports.namespace,visitorKeys = currentExports.visitorKeys;

      visitorKeyMap.set(file, visitorKeys);
      // dependencies === export * from
      var currentExportAll = new Set();
      dependencies.forEach(function (getDependency) {
        var dependency = getDependency();
        if (dependency === null) {
          return;
        }

        currentExportAll.add(dependency.path);
      });
      exportAll.set(file, currentExportAll);

      reexports.forEach(function (value, key) {
        if (key === DEFAULT) {
          exports.set(IMPORT_DEFAULT_SPECIFIER, { whereUsed: new Set() });
        } else {
          exports.set(key, { whereUsed: new Set() });
        }
        var reexport = value.getImport();
        if (!reexport) {
          return;
        }
        var localImport = imports.get(reexport.path);
        var currentValue = void 0;
        if (value.local === DEFAULT) {
          currentValue = IMPORT_DEFAULT_SPECIFIER;
        } else {
          currentValue = value.local;
        }
        if (typeof localImport !== 'undefined') {
          localImport = new Set([].concat(_toConsumableArray(localImport), [currentValue]));
        } else {
          localImport = new Set([currentValue]);
        }
        imports.set(reexport.path, localImport);
      });

      localImportList.forEach(function (value, key) {
        if (isNodeModule(key)) {
          return;
        }
        var localImport = imports.get(key) || new Set();
        value.declarations.forEach(function (_ref6) {var importedSpecifiers = _ref6.importedSpecifiers;return (
            importedSpecifiers.forEach(function (specifier) {return localImport.add(specifier);}));});

        imports.set(key, localImport);
      });
      importList.set(file, imports);

      // build up export list only, if file is not ignored
      if (ignoredFiles.has(file)) {
        return;
      }
      namespace.forEach(function (value, key) {
        if (key === DEFAULT) {
          exports.set(IMPORT_DEFAULT_SPECIFIER, { whereUsed: new Set() });
        } else {
          exports.set(key, { whereUsed: new Set() });
        }
      });
    }
    exports.set(EXPORT_ALL_DECLARATION, { whereUsed: new Set() });
    exports.set(IMPORT_NAMESPACE_SPECIFIER, { whereUsed: new Set() });
    exportList.set(file, exports);
  });
  exportAll.forEach(function (value, key) {
    value.forEach(function (val) {
      var currentExports = exportList.get(val);
      if (currentExports) {
        var currentExport = currentExports.get(EXPORT_ALL_DECLARATION);
        currentExport.whereUsed.add(key);
      }
    });
  });
};

/**
    * traverse through all imports and add the respective path to the whereUsed-list
    * of the corresponding export
    */
var determineUsage = function determineUsage() {
  importList.forEach(function (listValue, listKey) {
    listValue.forEach(function (value, key) {
      var exports = exportList.get(key);
      if (typeof exports !== 'undefined') {
        value.forEach(function (currentImport) {
          var specifier = void 0;
          if (currentImport === IMPORT_NAMESPACE_SPECIFIER) {
            specifier = IMPORT_NAMESPACE_SPECIFIER;
          } else if (currentImport === IMPORT_DEFAULT_SPECIFIER) {
            specifier = IMPORT_DEFAULT_SPECIFIER;
          } else {
            specifier = currentImport;
          }
          if (typeof specifier !== 'undefined') {
            var exportStatement = exports.get(specifier);
            if (typeof exportStatement !== 'undefined') {var
              whereUsed = exportStatement.whereUsed;
              whereUsed.add(listKey);
              exports.set(specifier, { whereUsed: whereUsed });
            }
          }
        });
      }
    });
  });
};

var getSrc = function getSrc(src) {
  if (src) {
    return src;
  }
  return [process.cwd()];
};

/**
    * prepare the lists of existing imports and exports - should only be executed once at
    * the start of a new eslint run
    */
var srcFiles = void 0;
var lastPrepareKey = void 0;
var doPreparation = function doPreparation(src, ignoreExports, context) {
  var prepareKey = JSON.stringify({
    src: (src || []).sort(),
    ignoreExports: (ignoreExports || []).sort(),
    extensions: Array.from((0, _ignore.getFileExtensions)(context.settings)).sort() });

  if (prepareKey === lastPrepareKey) {
    return;
  }

  importList.clear();
  exportList.clear();
  ignoredFiles.clear();
  filesOutsideSrc.clear();

  srcFiles = resolveFiles(getSrc(src), ignoreExports, context);
  prepareImportsAndExports(srcFiles, context);
  determineUsage();
  lastPrepareKey = prepareKey;
};

var newNamespaceImportExists = function newNamespaceImportExists(specifiers) {return (
    specifiers.some(function (_ref7) {var type = _ref7.type;return type === IMPORT_NAMESPACE_SPECIFIER;}));};

var newDefaultImportExists = function newDefaultImportExists(specifiers) {return (
    specifiers.some(function (_ref8) {var type = _ref8.type;return type === IMPORT_DEFAULT_SPECIFIER;}));};

var fileIsInPkg = function fileIsInPkg(file) {var _readPkgUp =
  (0, _readPkgUp3['default'])({ cwd: file }),path = _readPkgUp.path,pkg = _readPkgUp.pkg;
  var basePath = (0, _path.dirname)(path);

  var checkPkgFieldString = function checkPkgFieldString(pkgField) {
    if ((0, _path.join)(basePath, pkgField) === file) {
      return true;
    }
  };

  var checkPkgFieldObject = function checkPkgFieldObject(pkgField) {
    var pkgFieldFiles = (0, _object2['default'])(pkgField).map(function (value) {return (0, _path.join)(basePath, value);});
    if ((0, _arrayIncludes2['default'])(pkgFieldFiles, file)) {
      return true;
    }
  };

  var checkPkgField = function checkPkgField(pkgField) {
    if (typeof pkgField === 'string') {
      return checkPkgFieldString(pkgField);
    }

    if ((typeof pkgField === 'undefined' ? 'undefined' : _typeof(pkgField)) === 'object') {
      return checkPkgFieldObject(pkgField);
    }
  };

  if (pkg['private'] === true) {
    return false;
  }

  if (pkg.bin) {
    if (checkPkgField(pkg.bin)) {
      return true;
    }
  }

  if (pkg.browser) {
    if (checkPkgField(pkg.browser)) {
      return true;
    }
  }

  if (pkg.main) {
    if (checkPkgFieldString(pkg.main)) {
      return true;
    }
  }

  return false;
};

module.exports = {
  meta: {
    type: 'suggestion',
    docs: { url: (0, _docsUrl2['default'])('no-unused-modules') },
    schema: [{
      properties: {
        src: {
          description: 'files/paths to be analyzed (only for unused exports)',
          type: 'array',
          minItems: 1,
          items: {
            type: 'string',
            minLength: 1 } },


        ignoreExports: {
          description:
          'files/paths for which unused exports will not be reported (e.g module entry points)',
          type: 'array',
          minItems: 1,
          items: {
            type: 'string',
            minLength: 1 } },


        missingExports: {
          description: 'report modules without any exports',
          type: 'boolean' },

        unusedExports: {
          description: 'report exports without any usage',
          type: 'boolean' } },


      not: {
        properties: {
          unusedExports: { 'enum': [false] },
          missingExports: { 'enum': [false] } } },


      anyOf: [{
        not: {
          properties: {
            unusedExports: { 'enum': [true] } } },


        required: ['missingExports'] },
      {
        not: {
          properties: {
            missingExports: { 'enum': [true] } } },


        required: ['unusedExports'] },
      {
        properties: {
          unusedExports: { 'enum': [true] } },

        required: ['unusedExports'] },
      {
        properties: {
          missingExports: { 'enum': [true] } },

        required: ['missingExports'] }] }] },




  create: function () {function create(context) {var _ref9 =





      context.options[0] || {},src = _ref9.src,_ref9$ignoreExports = _ref9.ignoreExports,ignoreExports = _ref9$ignoreExports === undefined ? [] : _ref9$ignoreExports,missingExports = _ref9.missingExports,unusedExports = _ref9.unusedExports;

      if (unusedExports) {
        doPreparation(src, ignoreExports, context);
      }

      var file = context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename();

      var checkExportPresence = function () {function checkExportPresence(node) {
          if (!missingExports) {
            return;
          }

          if (ignoredFiles.has(file)) {
            return;
          }

          var exportCount = exportList.get(file);
          var exportAll = exportCount.get(EXPORT_ALL_DECLARATION);
          var namespaceImports = exportCount.get(IMPORT_NAMESPACE_SPECIFIER);

          exportCount['delete'](EXPORT_ALL_DECLARATION);
          exportCount['delete'](IMPORT_NAMESPACE_SPECIFIER);
          if (exportCount.size < 1) {
            // node.body[0] === 'undefined' only happens, if everything is commented out in the file
            // being linted
            context.report(node.body[0] ? node.body[0] : node, 'No exports found');
          }
          exportCount.set(EXPORT_ALL_DECLARATION, exportAll);
          exportCount.set(IMPORT_NAMESPACE_SPECIFIER, namespaceImports);
        }return checkExportPresence;}();

      var checkUsage = function () {function checkUsage(node, exportedValue) {
          if (!unusedExports) {
            return;
          }

          if (ignoredFiles.has(file)) {
            return;
          }

          if (fileIsInPkg(file)) {
            return;
          }

          if (filesOutsideSrc.has(file)) {
            return;
          }

          // make sure file to be linted is included in source files
          if (!srcFiles.has(file)) {
            srcFiles = resolveFiles(getSrc(src), ignoreExports, context);
            if (!srcFiles.has(file)) {
              filesOutsideSrc.add(file);
              return;
            }
          }

          exports = exportList.get(file);

          // special case: export * from
          var exportAll = exports.get(EXPORT_ALL_DECLARATION);
          if (typeof exportAll !== 'undefined' && exportedValue !== IMPORT_DEFAULT_SPECIFIER) {
            if (exportAll.whereUsed.size > 0) {
              return;
            }
          }

          // special case: namespace import
          var namespaceImports = exports.get(IMPORT_NAMESPACE_SPECIFIER);
          if (typeof namespaceImports !== 'undefined') {
            if (namespaceImports.whereUsed.size > 0) {
              return;
            }
          }

          // exportsList will always map any imported value of 'default' to 'ImportDefaultSpecifier'
          var exportsKey = exportedValue === DEFAULT ? IMPORT_DEFAULT_SPECIFIER : exportedValue;

          var exportStatement = exports.get(exportsKey);

          var value = exportsKey === IMPORT_DEFAULT_SPECIFIER ? DEFAULT : exportsKey;

          if (typeof exportStatement !== 'undefined') {
            if (exportStatement.whereUsed.size < 1) {
              context.report(
              node, 'exported declaration \'' +
              value + '\' not used within other modules');

            }
          } else {
            context.report(
            node, 'exported declaration \'' +
            value + '\' not used within other modules');

          }
        }return checkUsage;}();

      /**
                                 * only useful for tools like vscode-eslint
                                 *
                                 * update lists of existing exports during runtime
                                 */
      var updateExportUsage = function () {function updateExportUsage(node) {
          if (ignoredFiles.has(file)) {
            return;
          }

          var exports = exportList.get(file);

          // new module has been created during runtime
          // include it in further processing
          if (typeof exports === 'undefined') {
            exports = new Map();
          }

          var newExports = new Map();
          var newExportIdentifiers = new Set();

          node.body.forEach(function (_ref10) {var type = _ref10.type,declaration = _ref10.declaration,specifiers = _ref10.specifiers;
            if (type === EXPORT_DEFAULT_DECLARATION) {
              newExportIdentifiers.add(IMPORT_DEFAULT_SPECIFIER);
            }
            if (type === EXPORT_NAMED_DECLARATION) {
              if (specifiers.length > 0) {
                specifiers.forEach(function (specifier) {
                  if (specifier.exported) {
                    newExportIdentifiers.add(specifier.exported.name || specifier.exported.value);
                  }
                });
              }
              forEachDeclarationIdentifier(declaration, function (name) {
                newExportIdentifiers.add(name);
              });
            }
          });

          // old exports exist within list of new exports identifiers: add to map of new exports
          exports.forEach(function (value, key) {
            if (newExportIdentifiers.has(key)) {
              newExports.set(key, value);
            }
          });

          // new export identifiers added: add to map of new exports
          newExportIdentifiers.forEach(function (key) {
            if (!exports.has(key)) {
              newExports.set(key, { whereUsed: new Set() });
            }
          });

          // preserve information about namespace imports
          var exportAll = exports.get(EXPORT_ALL_DECLARATION);
          var namespaceImports = exports.get(IMPORT_NAMESPACE_SPECIFIER);

          if (typeof namespaceImports === 'undefined') {
            namespaceImports = { whereUsed: new Set() };
          }

          newExports.set(EXPORT_ALL_DECLARATION, exportAll);
          newExports.set(IMPORT_NAMESPACE_SPECIFIER, namespaceImports);
          exportList.set(file, newExports);
        }return updateExportUsage;}();

      /**
                                        * only useful for tools like vscode-eslint
                                        *
                                        * update lists of existing imports during runtime
                                        */
      var updateImportUsage = function () {function updateImportUsage(node) {
          if (!unusedExports) {
            return;
          }

          var oldImportPaths = importList.get(file);
          if (typeof oldImportPaths === 'undefined') {
            oldImportPaths = new Map();
          }

          var oldNamespaceImports = new Set();
          var newNamespaceImports = new Set();

          var oldExportAll = new Set();
          var newExportAll = new Set();

          var oldDefaultImports = new Set();
          var newDefaultImports = new Set();

          var oldImports = new Map();
          var newImports = new Map();
          oldImportPaths.forEach(function (value, key) {
            if (value.has(EXPORT_ALL_DECLARATION)) {
              oldExportAll.add(key);
            }
            if (value.has(IMPORT_NAMESPACE_SPECIFIER)) {
              oldNamespaceImports.add(key);
            }
            if (value.has(IMPORT_DEFAULT_SPECIFIER)) {
              oldDefaultImports.add(key);
            }
            value.forEach(function (val) {
              if (val !== IMPORT_NAMESPACE_SPECIFIER &&
              val !== IMPORT_DEFAULT_SPECIFIER) {
                oldImports.set(val, key);
              }
            });
          });

          function processDynamicImport(source) {
            if (source.type !== 'Literal') {
              return null;
            }
            var p = (0, _resolve2['default'])(source.value, context);
            if (p == null) {
              return null;
            }
            newNamespaceImports.add(p);
          }

          (0, _visit2['default'])(node, visitorKeyMap.get(file), {
            ImportExpression: function () {function ImportExpression(child) {
                processDynamicImport(child.source);
              }return ImportExpression;}(),
            CallExpression: function () {function CallExpression(child) {
                if (child.callee.type === 'Import') {
                  processDynamicImport(child.arguments[0]);
                }
              }return CallExpression;}() });


          node.body.forEach(function (astNode) {
            var resolvedPath = void 0;

            // support for export { value } from 'module'
            if (astNode.type === EXPORT_NAMED_DECLARATION) {
              if (astNode.source) {
                resolvedPath = (0, _resolve2['default'])(astNode.source.raw.replace(/('|")/g, ''), context);
                astNode.specifiers.forEach(function (specifier) {
                  var name = specifier.local.name || specifier.local.value;
                  if (name === DEFAULT) {
                    newDefaultImports.add(resolvedPath);
                  } else {
                    newImports.set(name, resolvedPath);
                  }
                });
              }
            }

            if (astNode.type === EXPORT_ALL_DECLARATION) {
              resolvedPath = (0, _resolve2['default'])(astNode.source.raw.replace(/('|")/g, ''), context);
              newExportAll.add(resolvedPath);
            }

            if (astNode.type === IMPORT_DECLARATION) {
              resolvedPath = (0, _resolve2['default'])(astNode.source.raw.replace(/('|")/g, ''), context);
              if (!resolvedPath) {
                return;
              }

              if (isNodeModule(resolvedPath)) {
                return;
              }

              if (newNamespaceImportExists(astNode.specifiers)) {
                newNamespaceImports.add(resolvedPath);
              }

              if (newDefaultImportExists(astNode.specifiers)) {
                newDefaultImports.add(resolvedPath);
              }

              astNode.specifiers.forEach(function (specifier) {
                if (specifier.type === IMPORT_DEFAULT_SPECIFIER ||
                specifier.type === IMPORT_NAMESPACE_SPECIFIER) {
                  return;
                }
                newImports.set(specifier.imported.name || specifier.imported.value, resolvedPath);
              });
            }
          });

          newExportAll.forEach(function (value) {
            if (!oldExportAll.has(value)) {
              var imports = oldImportPaths.get(value);
              if (typeof imports === 'undefined') {
                imports = new Set();
              }
              imports.add(EXPORT_ALL_DECLARATION);
              oldImportPaths.set(value, imports);

              var _exports = exportList.get(value);
              var currentExport = void 0;
              if (typeof _exports !== 'undefined') {
                currentExport = _exports.get(EXPORT_ALL_DECLARATION);
              } else {
                _exports = new Map();
                exportList.set(value, _exports);
              }

              if (typeof currentExport !== 'undefined') {
                currentExport.whereUsed.add(file);
              } else {
                var whereUsed = new Set();
                whereUsed.add(file);
                _exports.set(EXPORT_ALL_DECLARATION, { whereUsed: whereUsed });
              }
            }
          });

          oldExportAll.forEach(function (value) {
            if (!newExportAll.has(value)) {
              var imports = oldImportPaths.get(value);
              imports['delete'](EXPORT_ALL_DECLARATION);

              var _exports2 = exportList.get(value);
              if (typeof _exports2 !== 'undefined') {
                var currentExport = _exports2.get(EXPORT_ALL_DECLARATION);
                if (typeof currentExport !== 'undefined') {
                  currentExport.whereUsed['delete'](file);
                }
              }
            }
          });

          newDefaultImports.forEach(function (value) {
            if (!oldDefaultImports.has(value)) {
              var imports = oldImportPaths.get(value);
              if (typeof imports === 'undefined') {
                imports = new Set();
              }
              imports.add(IMPORT_DEFAULT_SPECIFIER);
              oldImportPaths.set(value, imports);

              var _exports3 = exportList.get(value);
              var currentExport = void 0;
              if (typeof _exports3 !== 'undefined') {
                currentExport = _exports3.get(IMPORT_DEFAULT_SPECIFIER);
              } else {
                _exports3 = new Map();
                exportList.set(value, _exports3);
              }

              if (typeof currentExport !== 'undefined') {
                currentExport.whereUsed.add(file);
              } else {
                var whereUsed = new Set();
                whereUsed.add(file);
                _exports3.set(IMPORT_DEFAULT_SPECIFIER, { whereUsed: whereUsed });
              }
            }
          });

          oldDefaultImports.forEach(function (value) {
            if (!newDefaultImports.has(value)) {
              var imports = oldImportPaths.get(value);
              imports['delete'](IMPORT_DEFAULT_SPECIFIER);

              var _exports4 = exportList.get(value);
              if (typeof _exports4 !== 'undefined') {
                var currentExport = _exports4.get(IMPORT_DEFAULT_SPECIFIER);
                if (typeof currentExport !== 'undefined') {
                  currentExport.whereUsed['delete'](file);
                }
              }
            }
          });

          newNamespaceImports.forEach(function (value) {
            if (!oldNamespaceImports.has(value)) {
              var imports = oldImportPaths.get(value);
              if (typeof imports === 'undefined') {
                imports = new Set();
              }
              imports.add(IMPORT_NAMESPACE_SPECIFIER);
              oldImportPaths.set(value, imports);

              var _exports5 = exportList.get(value);
              var currentExport = void 0;
              if (typeof _exports5 !== 'undefined') {
                currentExport = _exports5.get(IMPORT_NAMESPACE_SPECIFIER);
              } else {
                _exports5 = new Map();
                exportList.set(value, _exports5);
              }

              if (typeof currentExport !== 'undefined') {
                currentExport.whereUsed.add(file);
              } else {
                var whereUsed = new Set();
                whereUsed.add(file);
                _exports5.set(IMPORT_NAMESPACE_SPECIFIER, { whereUsed: whereUsed });
              }
            }
          });

          oldNamespaceImports.forEach(function (value) {
            if (!newNamespaceImports.has(value)) {
              var imports = oldImportPaths.get(value);
              imports['delete'](IMPORT_NAMESPACE_SPECIFIER);

              var _exports6 = exportList.get(value);
              if (typeof _exports6 !== 'undefined') {
                var currentExport = _exports6.get(IMPORT_NAMESPACE_SPECIFIER);
                if (typeof currentExport !== 'undefined') {
                  currentExport.whereUsed['delete'](file);
                }
              }
            }
          });

          newImports.forEach(function (value, key) {
            if (!oldImports.has(key)) {
              var imports = oldImportPaths.get(value);
              if (typeof imports === 'undefined') {
                imports = new Set();
              }
              imports.add(key);
              oldImportPaths.set(value, imports);

              var _exports7 = exportList.get(value);
              var currentExport = void 0;
              if (typeof _exports7 !== 'undefined') {
                currentExport = _exports7.get(key);
              } else {
                _exports7 = new Map();
                exportList.set(value, _exports7);
              }

              if (typeof currentExport !== 'undefined') {
                currentExport.whereUsed.add(file);
              } else {
                var whereUsed = new Set();
                whereUsed.add(file);
                _exports7.set(key, { whereUsed: whereUsed });
              }
            }
          });

          oldImports.forEach(function (value, key) {
            if (!newImports.has(key)) {
              var imports = oldImportPaths.get(value);
              imports['delete'](key);

              var _exports8 = exportList.get(value);
              if (typeof _exports8 !== 'undefined') {
                var currentExport = _exports8.get(key);
                if (typeof currentExport !== 'undefined') {
                  currentExport.whereUsed['delete'](file);
                }
              }
            }
          });
        }return updateImportUsage;}();

      return {
        'Program:exit': function () {function ProgramExit(node) {
            updateExportUsage(node);
            updateImportUsage(node);
            checkExportPresence(node);
          }return ProgramExit;}(),
        'ExportDefaultDeclaration': function () {function ExportDefaultDeclaration(node) {
            checkUsage(node, IMPORT_DEFAULT_SPECIFIER);
          }return ExportDefaultDeclaration;}(),
        'ExportNamedDeclaration': function () {function ExportNamedDeclaration(node) {
            node.specifiers.forEach(function (specifier) {
              checkUsage(node, specifier.exported.name || specifier.exported.value);
            });
            forEachDeclarationIdentifier(node.declaration, function (name) {
              checkUsage(node, name);
            });
          }return ExportNamedDeclaration;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby11bnVzZWQtbW9kdWxlcy5qcyJdLCJuYW1lcyI6WyJGaWxlRW51bWVyYXRvciIsImxpc3RGaWxlc1RvUHJvY2VzcyIsInJlcXVpcmUiLCJlIiwib3JpZ2luYWxMaXN0RmlsZXNUb1Byb2Nlc3MiLCJzcmMiLCJleHRlbnNpb25zIiwicGF0dGVybnMiLCJyZWR1Y2UiLCJjYXJyeSIsInBhdHRlcm4iLCJjb25jYXQiLCJtYXAiLCJleHRlbnNpb24iLCJ0ZXN0Iiwic2xpY2UiLCJBcnJheSIsImZyb20iLCJpdGVyYXRlRmlsZXMiLCJmaWxlUGF0aCIsImlnbm9yZWQiLCJmaWxlbmFtZSIsIkVYUE9SVF9ERUZBVUxUX0RFQ0xBUkFUSU9OIiwiRVhQT1JUX05BTUVEX0RFQ0xBUkFUSU9OIiwiRVhQT1JUX0FMTF9ERUNMQVJBVElPTiIsIklNUE9SVF9ERUNMQVJBVElPTiIsIklNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSIiwiSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSIiwiVkFSSUFCTEVfREVDTEFSQVRJT04iLCJGVU5DVElPTl9ERUNMQVJBVElPTiIsIkNMQVNTX0RFQ0xBUkFUSU9OIiwiSURFTlRJRklFUiIsIk9CSkVDVF9QQVRURVJOIiwiVFNfSU5URVJGQUNFX0RFQ0xBUkFUSU9OIiwiVFNfVFlQRV9BTElBU19ERUNMQVJBVElPTiIsIlRTX0VOVU1fREVDTEFSQVRJT04iLCJERUZBVUxUIiwiZm9yRWFjaERlY2xhcmF0aW9uSWRlbnRpZmllciIsImRlY2xhcmF0aW9uIiwiY2IiLCJ0eXBlIiwiaWQiLCJuYW1lIiwiZGVjbGFyYXRpb25zIiwiZm9yRWFjaCIsImltcG9ydExpc3QiLCJNYXAiLCJleHBvcnRMaXN0IiwidmlzaXRvcktleU1hcCIsImlnbm9yZWRGaWxlcyIsIlNldCIsImZpbGVzT3V0c2lkZVNyYyIsImlzTm9kZU1vZHVsZSIsInBhdGgiLCJyZXNvbHZlRmlsZXMiLCJpZ25vcmVFeHBvcnRzIiwiY29udGV4dCIsInNldHRpbmdzIiwic3JjRmlsZXMiLCJzcmNGaWxlTGlzdCIsImlnbm9yZWRGaWxlc0xpc3QiLCJhZGQiLCJmaWx0ZXIiLCJwcmVwYXJlSW1wb3J0c0FuZEV4cG9ydHMiLCJleHBvcnRBbGwiLCJleHBvcnRzIiwiaW1wb3J0cyIsImN1cnJlbnRFeHBvcnRzIiwiRXhwb3J0cyIsImdldCIsImZpbGUiLCJkZXBlbmRlbmNpZXMiLCJyZWV4cG9ydHMiLCJsb2NhbEltcG9ydExpc3QiLCJuYW1lc3BhY2UiLCJ2aXNpdG9yS2V5cyIsInNldCIsImN1cnJlbnRFeHBvcnRBbGwiLCJkZXBlbmRlbmN5IiwiZ2V0RGVwZW5kZW5jeSIsInZhbHVlIiwia2V5Iiwid2hlcmVVc2VkIiwicmVleHBvcnQiLCJnZXRJbXBvcnQiLCJsb2NhbEltcG9ydCIsImN1cnJlbnRWYWx1ZSIsImxvY2FsIiwiaW1wb3J0ZWRTcGVjaWZpZXJzIiwic3BlY2lmaWVyIiwiaGFzIiwidmFsIiwiY3VycmVudEV4cG9ydCIsImRldGVybWluZVVzYWdlIiwibGlzdFZhbHVlIiwibGlzdEtleSIsImN1cnJlbnRJbXBvcnQiLCJleHBvcnRTdGF0ZW1lbnQiLCJnZXRTcmMiLCJwcm9jZXNzIiwiY3dkIiwibGFzdFByZXBhcmVLZXkiLCJkb1ByZXBhcmF0aW9uIiwicHJlcGFyZUtleSIsIkpTT04iLCJzdHJpbmdpZnkiLCJzb3J0IiwiY2xlYXIiLCJuZXdOYW1lc3BhY2VJbXBvcnRFeGlzdHMiLCJzcGVjaWZpZXJzIiwic29tZSIsIm5ld0RlZmF1bHRJbXBvcnRFeGlzdHMiLCJmaWxlSXNJblBrZyIsInBrZyIsImJhc2VQYXRoIiwiY2hlY2tQa2dGaWVsZFN0cmluZyIsInBrZ0ZpZWxkIiwiY2hlY2tQa2dGaWVsZE9iamVjdCIsInBrZ0ZpZWxkRmlsZXMiLCJjaGVja1BrZ0ZpZWxkIiwiYmluIiwiYnJvd3NlciIsIm1haW4iLCJtb2R1bGUiLCJtZXRhIiwiZG9jcyIsInVybCIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJkZXNjcmlwdGlvbiIsIm1pbkl0ZW1zIiwiaXRlbXMiLCJtaW5MZW5ndGgiLCJtaXNzaW5nRXhwb3J0cyIsInVudXNlZEV4cG9ydHMiLCJub3QiLCJhbnlPZiIsInJlcXVpcmVkIiwiY3JlYXRlIiwib3B0aW9ucyIsImdldFBoeXNpY2FsRmlsZW5hbWUiLCJnZXRGaWxlbmFtZSIsImNoZWNrRXhwb3J0UHJlc2VuY2UiLCJleHBvcnRDb3VudCIsIm5hbWVzcGFjZUltcG9ydHMiLCJzaXplIiwicmVwb3J0Iiwibm9kZSIsImJvZHkiLCJjaGVja1VzYWdlIiwiZXhwb3J0ZWRWYWx1ZSIsImV4cG9ydHNLZXkiLCJ1cGRhdGVFeHBvcnRVc2FnZSIsIm5ld0V4cG9ydHMiLCJuZXdFeHBvcnRJZGVudGlmaWVycyIsImxlbmd0aCIsImV4cG9ydGVkIiwidXBkYXRlSW1wb3J0VXNhZ2UiLCJvbGRJbXBvcnRQYXRocyIsIm9sZE5hbWVzcGFjZUltcG9ydHMiLCJuZXdOYW1lc3BhY2VJbXBvcnRzIiwib2xkRXhwb3J0QWxsIiwibmV3RXhwb3J0QWxsIiwib2xkRGVmYXVsdEltcG9ydHMiLCJuZXdEZWZhdWx0SW1wb3J0cyIsIm9sZEltcG9ydHMiLCJuZXdJbXBvcnRzIiwicHJvY2Vzc0R5bmFtaWNJbXBvcnQiLCJzb3VyY2UiLCJwIiwiSW1wb3J0RXhwcmVzc2lvbiIsImNoaWxkIiwiQ2FsbEV4cHJlc3Npb24iLCJjYWxsZWUiLCJhcmd1bWVudHMiLCJyZXNvbHZlZFBhdGgiLCJhc3ROb2RlIiwicmF3IiwicmVwbGFjZSIsImltcG9ydGVkIl0sIm1hcHBpbmdzIjoiOzs7Ozs7QUFNQSx5QztBQUNBO0FBQ0Esc0Q7QUFDQSxrRDtBQUNBLHFDO0FBQ0E7QUFDQSwyRDtBQUNBLHVDO0FBQ0EsK0MsdVZBZEE7Ozs7eVlBZ0JBLElBQUlBLHVCQUFKLENBQ0EsSUFBSUMsMkJBQUo7QUFFQSxJQUFJO0FBQ29CQyxVQUFRLDZCQUFSLENBRHBCLENBQ0NGLGNBREQsWUFDQ0EsY0FERDtBQUVILENBRkQsQ0FFRSxPQUFPRyxDQUFQLEVBQVU7QUFDVixNQUFJ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Q0FUSyxFQUFQOztBQWtCRCxLQTlkRCxpQkFwRWUsRUFBakIiLCJmaWxlIjoibm8tdW51c2VkLW1vZHVsZXMuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBmaWxlT3ZlcnZpZXcgRW5zdXJlcyB0aGF0IG1vZHVsZXMgY29udGFpbiBleHBvcnRzIGFuZC9vciBhbGxcbiAqIG1vZHVsZXMgYXJlIGNvbnN1bWVkIHdpdGhpbiBvdGhlciBtb2R1bGVzLlxuICogQGF1dGhvciBSZW7DqSBGZXJtYW5uXG4gKi9cblxuaW1wb3J0IEV4cG9ydHMsIHsgcmVjdXJzaXZlUGF0dGVybkNhcHR1cmUgfSBmcm9tICcuLi9FeHBvcnRNYXAnO1xuaW1wb3J0IHsgZ2V0RmlsZUV4dGVuc2lvbnMgfSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL2lnbm9yZSc7XG5pbXBvcnQgcmVzb2x2ZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnO1xuaW1wb3J0IHZpc2l0IGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvdmlzaXQnO1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCc7XG5pbXBvcnQgeyBkaXJuYW1lLCBqb2luIH0gZnJvbSAncGF0aCc7XG5pbXBvcnQgcmVhZFBrZ1VwIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvcmVhZFBrZ1VwJztcbmltcG9ydCB2YWx1ZXMgZnJvbSAnb2JqZWN0LnZhbHVlcyc7XG5pbXBvcnQgaW5jbHVkZXMgZnJvbSAnYXJyYXktaW5jbHVkZXMnO1xuXG5sZXQgRmlsZUVudW1lcmF0b3I7XG5sZXQgbGlzdEZpbGVzVG9Qcm9jZXNzO1xuXG50cnkge1xuICAoeyBGaWxlRW51bWVyYXRvciB9ID0gcmVxdWlyZSgnZXNsaW50L3VzZS1hdC15b3VyLW93bi1yaXNrJykpO1xufSBjYXRjaCAoZSkge1xuICB0cnkge1xuICAgIC8vIGhhcyBiZWVuIG1vdmVkIHRvIGVzbGludC9saWIvY2xpLWVuZ2luZS9maWxlLWVudW1lcmF0b3IgaW4gdmVyc2lvbiA2XG4gICAgKHsgRmlsZUVudW1lcmF0b3IgfSA9IHJlcXVpcmUoJ2VzbGludC9saWIvY2xpLWVuZ2luZS9maWxlLWVudW1lcmF0b3InKSk7XG4gIH0gY2F0Y2ggKGUpIHtcbiAgICB0cnkge1xuICAgICAgLy8gZXNsaW50L2xpYi91dGlsL2dsb2ItdXRpbCBoYXMgYmVlbiBtb3ZlZCB0byBlc2xpbnQvbGliL3V0aWwvZ2xvYi11dGlscyB3aXRoIHZlcnNpb24gNS4zXG4gICAgICBjb25zdCB7IGxpc3RGaWxlc1RvUHJvY2Vzczogb3JpZ2luYWxMaXN0RmlsZXNUb1Byb2Nlc3MgfSA9IHJlcXVpcmUoJ2VzbGludC9saWIvdXRpbC9nbG9iLXV0aWxzJyk7XG5cbiAgICAgIC8vIFByZXZlbnQgcGFzc2luZyBpbnZhbGlkIG9wdGlvbnMgKGV4dGVuc2lvbnMgYXJyYXkpIHRvIG9sZCB2ZXJzaW9ucyBvZiB0aGUgZnVuY3Rpb24uXG4gICAgICAvLyBodHRwczovL2dpdGh1Yi5jb20vZXNsaW50L2VzbGludC9ibG9iL3Y1LjE2LjAvbGliL3V0aWwvZ2xvYi11dGlscy5qcyNMMTc4LUwyODBcbiAgICAgIC8vIGh0dHBzOi8vZ2l0aHViLmNvbS9lc2xpbnQvZXNsaW50L2Jsb2IvdjUuMi4wL2xpYi91dGlsL2dsb2ItdXRpbC5qcyNMMTc0LUwyNjlcbiAgICAgIGxpc3RGaWxlc1RvUHJvY2VzcyA9IGZ1bmN0aW9uIChzcmMsIGV4dGVuc2lvbnMpIHtcbiAgICAgICAgcmV0dXJuIG9yaWdpbmFsTGlzdEZpbGVzVG9Qcm9jZXNzKHNyYywge1xuICAgICAgICAgIGV4dGVuc2lvbnMsXG4gICAgICAgIH0pO1xuICAgICAgfTtcbiAgICB9IGNhdGNoIChlKSB7XG4gICAgICBjb25zdCB7IGxpc3RGaWxlc1RvUHJvY2Vzczogb3JpZ2luYWxMaXN0RmlsZXNUb1Byb2Nlc3MgfSA9IHJlcXVpcmUoJ2VzbGludC9saWIvdXRpbC9nbG9iLXV0aWwnKTtcbiAgICAgIFxuICAgICAgbGlzdEZpbGVzVG9Qcm9jZXNzID0gZnVuY3Rpb24gKHNyYywgZXh0ZW5zaW9ucykge1xuICAgICAgICBjb25zdCBwYXR0ZXJucyA9IHNyYy5yZWR1Y2UoKGNhcnJ5LCBwYXR0ZXJuKSA9PiB7XG4gICAgICAgICAgcmV0dXJuIGNhcnJ5LmNvbmNhdChleHRlbnNpb25zLm1hcCgoZXh0ZW5zaW9uKSA9PiB7XG4gICAgICAgICAgICByZXR1cm4gL1xcKlxcKnxcXCpcXC4vLnRlc3QocGF0dGVybikgPyBwYXR0ZXJuIDogYCR7cGF0dGVybn0vKiovKiR7ZXh0ZW5zaW9ufWA7XG4gICAgICAgICAgfSkpO1xuICAgICAgICB9LCBzcmMuc2xpY2UoKSk7XG4gICAgXG4gICAgICAgIHJldHVybiBvcmlnaW5hbExpc3RGaWxlc1RvUHJvY2VzcyhwYXR0ZXJucyk7XG4gICAgICB9O1xuICAgIH1cbiAgfVxufVxuXG5pZiAoRmlsZUVudW1lcmF0b3IpIHtcbiAgbGlzdEZpbGVzVG9Qcm9jZXNzID0gZnVuY3Rpb24gKHNyYywgZXh0ZW5zaW9ucykge1xuICAgIGNvbnN0IGUgPSBuZXcgRmlsZUVudW1lcmF0b3Ioe1xuICAgICAgZXh0ZW5zaW9ucyxcbiAgICB9KTtcblxuICAgIHJldHVybiBBcnJheS5mcm9tKGUuaXRlcmF0ZUZpbGVzKHNyYyksICh7IGZpbGVQYXRoLCBpZ25vcmVkIH0pID0+ICh7XG4gICAgICBpZ25vcmVkLFxuICAgICAgZmlsZW5hbWU6IGZpbGVQYXRoLFxuICAgIH0pKTtcbiAgfTtcbn1cblxuY29uc3QgRVhQT1JUX0RFRkFVTFRfREVDTEFSQVRJT04gPSAnRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uJztcbmNvbnN0IEVYUE9SVF9OQU1FRF9ERUNMQVJBVElPTiA9ICdFeHBvcnROYW1lZERlY2xhcmF0aW9uJztcbmNvbnN0IEVYUE9SVF9BTExfREVDTEFSQVRJT04gPSAnRXhwb3J0QWxsRGVjbGFyYXRpb24nO1xuY29uc3QgSU1QT1JUX0RFQ0xBUkFUSU9OID0gJ0ltcG9ydERlY2xhcmF0aW9uJztcbmNvbnN0IElNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSID0gJ0ltcG9ydE5hbWVzcGFjZVNwZWNpZmllcic7XG5jb25zdCBJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIgPSAnSW1wb3J0RGVmYXVsdFNwZWNpZmllcic7XG5jb25zdCBWQVJJQUJMRV9ERUNMQVJBVElPTiA9ICdWYXJpYWJsZURlY2xhcmF0aW9uJztcbmNvbnN0IEZVTkNUSU9OX0RFQ0xBUkFUSU9OID0gJ0Z1bmN0aW9uRGVjbGFyYXRpb24nO1xuY29uc3QgQ0xBU1NfREVDTEFSQVRJT04gPSAnQ2xhc3NEZWNsYXJhdGlvbic7XG5jb25zdCBJREVOVElGSUVSID0gJ0lkZW50aWZpZXInO1xuY29uc3QgT0JKRUNUX1BBVFRFUk4gPSAnT2JqZWN0UGF0dGVybic7XG5jb25zdCBUU19JTlRFUkZBQ0VfREVDTEFSQVRJT04gPSAnVFNJbnRlcmZhY2VEZWNsYXJhdGlvbic7XG5jb25zdCBUU19UWVBFX0FMSUFTX0RFQ0xBUkFUSU9OID0gJ1RTVHlwZUFsaWFzRGVjbGFyYXRpb24nO1xuY29uc3QgVFNfRU5VTV9ERUNMQVJBVElPTiA9ICdUU0VudW1EZWNsYXJhdGlvbic7XG5jb25zdCBERUZBVUxUID0gJ2RlZmF1bHQnO1xuXG5mdW5jdGlvbiBmb3JFYWNoRGVjbGFyYXRpb25JZGVudGlmaWVyKGRlY2xhcmF0aW9uLCBjYikge1xuICBpZiAoZGVjbGFyYXRpb24pIHtcbiAgICBpZiAoXG4gICAgICBkZWNsYXJhdGlvbi50eXBlID09PSBGVU5DVElPTl9ERUNMQVJBVElPTiB8fFxuICAgICAgZGVjbGFyYXRpb24udHlwZSA9PT0gQ0xBU1NfREVDTEFSQVRJT04gfHxcbiAgICAgIGRlY2xhcmF0aW9uLnR5cGUgPT09IFRTX0lOVEVSRkFDRV9ERUNMQVJBVElPTiB8fFxuICAgICAgZGVjbGFyYXRpb24udHlwZSA9PT0gVFNfVFlQRV9BTElBU19ERUNMQVJBVElPTiB8fFxuICAgICAgZGVjbGFyYXRpb24udHlwZSA9PT0gVFNfRU5VTV9ERUNMQVJBVElPTlxuICAgICkge1xuICAgICAgY2IoZGVjbGFyYXRpb24uaWQubmFtZSk7XG4gICAgfSBlbHNlIGlmIChkZWNsYXJhdGlvbi50eXBlID09PSBWQVJJQUJMRV9ERUNMQVJBVElPTikge1xuICAgICAgZGVjbGFyYXRpb24uZGVjbGFyYXRpb25zLmZvckVhY2goKHsgaWQgfSkgPT4ge1xuICAgICAgICBpZiAoaWQudHlwZSA9PT0gT0JKRUNUX1BBVFRFUk4pIHtcbiAgICAgICAgICByZWN1cnNpdmVQYXR0ZXJuQ2FwdHVyZShpZCwgKHBhdHRlcm4pID0+IHtcbiAgICAgICAgICAgIGlmIChwYXR0ZXJuLnR5cGUgPT09IElERU5USUZJRVIpIHtcbiAgICAgICAgICAgICAgY2IocGF0dGVybi5uYW1lKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9KTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICBjYihpZC5uYW1lKTtcbiAgICAgICAgfVxuICAgICAgfSk7XG4gICAgfVxuICB9XG59XG5cbi8qKlxuICogTGlzdCBvZiBpbXBvcnRzIHBlciBmaWxlLlxuICpcbiAqIFJlcHJlc2VudGVkIGJ5IGEgdHdvLWxldmVsIE1hcCB0byBhIFNldCBvZiBpZGVudGlmaWVycy4gVGhlIHVwcGVyLWxldmVsIE1hcFxuICoga2V5cyBhcmUgdGhlIHBhdGhzIHRvIHRoZSBtb2R1bGVzIGNvbnRhaW5pbmcgdGhlIGltcG9ydHMsIHdoaWxlIHRoZVxuICogbG93ZXItbGV2ZWwgTWFwIGtleXMgYXJlIHRoZSBwYXRocyB0byB0aGUgZmlsZXMgd2hpY2ggYXJlIGJlaW5nIGltcG9ydGVkXG4gKiBmcm9tLiBMYXN0bHksIHRoZSBTZXQgb2YgaWRlbnRpZmllcnMgY29udGFpbnMgZWl0aGVyIG5hbWVzIGJlaW5nIGltcG9ydGVkXG4gKiBvciBhIHNwZWNpYWwgQVNUIG5vZGUgbmFtZSBsaXN0ZWQgYWJvdmUgKGUuZyBJbXBvcnREZWZhdWx0U3BlY2lmaWVyKS5cbiAqXG4gKiBGb3IgZXhhbXBsZSwgaWYgd2UgaGF2ZSBhIGZpbGUgbmFtZWQgZm9vLmpzIGNvbnRhaW5pbmc6XG4gKlxuICogICBpbXBvcnQgeyBvMiB9IGZyb20gJy4vYmFyLmpzJztcbiAqXG4gKiBUaGVuIHdlIHdpbGwgaGF2ZSBhIHN0cnVjdHVyZSB0aGF0IGxvb2tzIGxpa2U6XG4gKlxuICogICBNYXAgeyAnZm9vLmpzJyA9PiBNYXAgeyAnYmFyLmpzJyA9PiBTZXQgeyAnbzInIH0gfSB9XG4gKlxuICogQHR5cGUge01hcDxzdHJpbmcsIE1hcDxzdHJpbmcsIFNldDxzdHJpbmc+Pj59XG4gKi9cbmNvbnN0IGltcG9ydExpc3QgPSBuZXcgTWFwKCk7XG5cbi8qKlxuICogTGlzdCBvZiBleHBvcnRzIHBlciBmaWxlLlxuICpcbiAqIFJlcHJlc2VudGVkIGJ5IGEgdHdvLWxldmVsIE1hcCB0byBhbiBvYmplY3Qgb2YgbWV0YWRhdGEuIFRoZSB1cHBlci1sZXZlbCBNYXBcbiAqIGtleXMgYXJlIHRoZSBwYXRocyB0byB0aGUgbW9kdWxlcyBjb250YWluaW5nIHRoZSBleHBvcnRzLCB3aGlsZSB0aGVcbiAqIGxvd2VyLWxldmVsIE1hcCBrZXlzIGFyZSB0aGUgc3BlY2lmaWMgaWRlbnRpZmllcnMgb3Igc3BlY2lhbCBBU1Qgbm9kZSBuYW1lc1xuICogYmVpbmcgZXhwb3J0ZWQuIFRoZSBsZWFmLWxldmVsIG1ldGFkYXRhIG9iamVjdCBhdCB0aGUgbW9tZW50IG9ubHkgY29udGFpbnMgYVxuICogYHdoZXJlVXNlZGAgcHJvcGVydHksIHdoaWNoIGNvbnRhaW5zIGEgU2V0IG9mIHBhdGhzIHRvIG1vZHVsZXMgdGhhdCBpbXBvcnRcbiAqIHRoZSBuYW1lLlxuICpcbiAqIEZvciBleGFtcGxlLCBpZiB3ZSBoYXZlIGEgZmlsZSBuYW1lZCBiYXIuanMgY29udGFpbmluZyB0aGUgZm9sbG93aW5nIGV4cG9ydHM6XG4gKlxuICogICBjb25zdCBvMiA9ICdiYXInO1xuICogICBleHBvcnQgeyBvMiB9O1xuICpcbiAqIEFuZCBhIGZpbGUgbmFtZWQgZm9vLmpzIGNvbnRhaW5pbmcgdGhlIGZvbGxvd2luZyBpbXBvcnQ6XG4gKlxuICogICBpbXBvcnQgeyBvMiB9IGZyb20gJy4vYmFyLmpzJztcbiAqXG4gKiBUaGVuIHdlIHdpbGwgaGF2ZSBhIHN0cnVjdHVyZSB0aGF0IGxvb2tzIGxpa2U6XG4gKlxuICogICBNYXAgeyAnYmFyLmpzJyA9PiBNYXAgeyAnbzInID0+IHsgd2hlcmVVc2VkOiBTZXQgeyAnZm9vLmpzJyB9IH0gfSB9XG4gKlxuICogQHR5cGUge01hcDxzdHJpbmcsIE1hcDxzdHJpbmcsIG9iamVjdD4+fVxuICovXG5jb25zdCBleHBvcnRMaXN0ID0gbmV3IE1hcCgpO1xuXG5jb25zdCB2aXNpdG9yS2V5TWFwID0gbmV3IE1hcCgpO1xuXG5jb25zdCBpZ25vcmVkRmlsZXMgPSBuZXcgU2V0KCk7XG5jb25zdCBmaWxlc091dHNpZGVTcmMgPSBuZXcgU2V0KCk7XG5cbmNvbnN0IGlzTm9kZU1vZHVsZSA9IHBhdGggPT4ge1xuICByZXR1cm4gL1xcLyhub2RlX21vZHVsZXMpXFwvLy50ZXN0KHBhdGgpO1xufTtcblxuLyoqXG4gKiByZWFkIGFsbCBmaWxlcyBtYXRjaGluZyB0aGUgcGF0dGVybnMgaW4gc3JjIGFuZCBpZ25vcmVFeHBvcnRzXG4gKlxuICogcmV0dXJuIGFsbCBmaWxlcyBtYXRjaGluZyBzcmMgcGF0dGVybiwgd2hpY2ggYXJlIG5vdCBtYXRjaGluZyB0aGUgaWdub3JlRXhwb3J0cyBwYXR0ZXJuXG4gKi9cbmNvbnN0IHJlc29sdmVGaWxlcyA9IChzcmMsIGlnbm9yZUV4cG9ydHMsIGNvbnRleHQpID0+IHtcbiAgY29uc3QgZXh0ZW5zaW9ucyA9IEFycmF5LmZyb20oZ2V0RmlsZUV4dGVuc2lvbnMoY29udGV4dC5zZXR0aW5ncykpO1xuXG4gIGNvbnN0IHNyY0ZpbGVzID0gbmV3IFNldCgpO1xuICBjb25zdCBzcmNGaWxlTGlzdCA9IGxpc3RGaWxlc1RvUHJvY2VzcyhzcmMsIGV4dGVuc2lvbnMpO1xuXG4gIC8vIHByZXBhcmUgbGlzdCBvZiBpZ25vcmVkIGZpbGVzXG4gIGNvbnN0IGlnbm9yZWRGaWxlc0xpc3QgPSAgbGlzdEZpbGVzVG9Qcm9jZXNzKGlnbm9yZUV4cG9ydHMsIGV4dGVuc2lvbnMpO1xuICBpZ25vcmVkRmlsZXNMaXN0LmZvckVhY2goKHsgZmlsZW5hbWUgfSkgPT4gaWdub3JlZEZpbGVzLmFkZChmaWxlbmFtZSkpO1xuXG4gIC8vIHByZXBhcmUgbGlzdCBvZiBzb3VyY2UgZmlsZXMsIGRvbid0IGNvbnNpZGVyIGZpbGVzIGZyb20gbm9kZV9tb2R1bGVzXG4gIHNyY0ZpbGVMaXN0LmZpbHRlcigoeyBmaWxlbmFtZSB9KSA9PiAhaXNOb2RlTW9kdWxlKGZpbGVuYW1lKSkuZm9yRWFjaCgoeyBmaWxlbmFtZSB9KSA9PiB7XG4gICAgc3JjRmlsZXMuYWRkKGZpbGVuYW1lKTtcbiAgfSk7XG4gIHJldHVybiBzcmNGaWxlcztcbn07XG5cbi8qKlxuICogcGFyc2UgYWxsIHNvdXJjZSBmaWxlcyBhbmQgYnVpbGQgdXAgMiBtYXBzIGNvbnRhaW5pbmcgdGhlIGV4aXN0aW5nIGltcG9ydHMgYW5kIGV4cG9ydHNcbiAqL1xuY29uc3QgcHJlcGFyZUltcG9ydHNBbmRFeHBvcnRzID0gKHNyY0ZpbGVzLCBjb250ZXh0KSA9PiB7XG4gIGNvbnN0IGV4cG9ydEFsbCA9IG5ldyBNYXAoKTtcbiAgc3JjRmlsZXMuZm9yRWFjaChmaWxlID0+IHtcbiAgICBjb25zdCBleHBvcnRzID0gbmV3IE1hcCgpO1xuICAgIGNvbnN0IGltcG9ydHMgPSBuZXcgTWFwKCk7XG4gICAgY29uc3QgY3VycmVudEV4cG9ydHMgPSBFeHBvcnRzLmdldChmaWxlLCBjb250ZXh0KTtcbiAgICBpZiAoY3VycmVudEV4cG9ydHMpIHtcbiAgICAgIGNvbnN0IHtcbiAgICAgICAgZGVwZW5kZW5jaWVzLFxuICAgICAgICByZWV4cG9ydHMsXG4gICAgICAgIGltcG9ydHM6IGxvY2FsSW1wb3J0TGlzdCxcbiAgICAgICAgbmFtZXNwYWNlLFxuICAgICAgICB2aXNpdG9yS2V5cyxcbiAgICAgIH0gPSBjdXJyZW50RXhwb3J0cztcblxuICAgICAgdmlzaXRvcktleU1hcC5zZXQoZmlsZSwgdmlzaXRvcktleXMpO1xuICAgICAgLy8gZGVwZW5kZW5jaWVzID09PSBleHBvcnQgKiBmcm9tXG4gICAgICBjb25zdCBjdXJyZW50RXhwb3J0QWxsID0gbmV3IFNldCgpO1xuICAgICAgZGVwZW5kZW5jaWVzLmZvckVhY2goZ2V0RGVwZW5kZW5jeSA9PiB7XG4gICAgICAgIGNvbnN0IGRlcGVuZGVuY3kgPSBnZXREZXBlbmRlbmN5KCk7XG4gICAgICAgIGlmIChkZXBlbmRlbmN5ID09PSBudWxsKSB7XG4gICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG5cbiAgICAgICAgY3VycmVudEV4cG9ydEFsbC5hZGQoZGVwZW5kZW5jeS5wYXRoKTtcbiAgICAgIH0pO1xuICAgICAgZXhwb3J0QWxsLnNldChmaWxlLCBjdXJyZW50RXhwb3J0QWxsKTtcblxuICAgICAgcmVleHBvcnRzLmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgICAgaWYgKGtleSA9PT0gREVGQVVMVCkge1xuICAgICAgICAgIGV4cG9ydHMuc2V0KElNUE9SVF9ERUZBVUxUX1NQRUNJRklFUiwgeyB3aGVyZVVzZWQ6IG5ldyBTZXQoKSB9KTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICBleHBvcnRzLnNldChrZXksIHsgd2hlcmVVc2VkOiBuZXcgU2V0KCkgfSk7XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgcmVleHBvcnQgPSAgdmFsdWUuZ2V0SW1wb3J0KCk7XG4gICAgICAgIGlmICghcmVleHBvcnQpIHtcbiAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgbGV0IGxvY2FsSW1wb3J0ID0gaW1wb3J0cy5nZXQocmVleHBvcnQucGF0aCk7XG4gICAgICAgIGxldCBjdXJyZW50VmFsdWU7XG4gICAgICAgIGlmICh2YWx1ZS5sb2NhbCA9PT0gREVGQVVMVCkge1xuICAgICAgICAgIGN1cnJlbnRWYWx1ZSA9IElNUE9SVF9ERUZBVUxUX1NQRUNJRklFUjtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICBjdXJyZW50VmFsdWUgPSB2YWx1ZS5sb2NhbDtcbiAgICAgICAgfVxuICAgICAgICBpZiAodHlwZW9mIGxvY2FsSW1wb3J0ICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgIGxvY2FsSW1wb3J0ID0gbmV3IFNldChbLi4ubG9jYWxJbXBvcnQsIGN1cnJlbnRWYWx1ZV0pO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgIGxvY2FsSW1wb3J0ID0gbmV3IFNldChbY3VycmVudFZhbHVlXSk7XG4gICAgICAgIH1cbiAgICAgICAgaW1wb3J0cy5zZXQocmVleHBvcnQucGF0aCwgbG9jYWxJbXBvcnQpO1xuICAgICAgfSk7XG5cbiAgICAgIGxvY2FsSW1wb3J0TGlzdC5mb3JFYWNoKCh2YWx1ZSwga2V5KSA9PiB7XG4gICAgICAgIGlmIChpc05vZGVNb2R1bGUoa2V5KSkge1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBsb2NhbEltcG9ydCA9IGltcG9ydHMuZ2V0KGtleSkgfHwgbmV3IFNldCgpO1xuICAgICAgICB2YWx1ZS5kZWNsYXJhdGlvbnMuZm9yRWFjaCgoeyBpbXBvcnRlZFNwZWNpZmllcnMgfSkgPT5cbiAgICAgICAgICBpbXBvcnRlZFNwZWNpZmllcnMuZm9yRWFjaChzcGVjaWZpZXIgPT4gbG9jYWxJbXBvcnQuYWRkKHNwZWNpZmllcikpLFxuICAgICAgICApO1xuICAgICAgICBpbXBvcnRzLnNldChrZXksIGxvY2FsSW1wb3J0KTtcbiAgICAgIH0pO1xuICAgICAgaW1wb3J0TGlzdC5zZXQoZmlsZSwgaW1wb3J0cyk7XG5cbiAgICAgIC8vIGJ1aWxkIHVwIGV4cG9ydCBsaXN0IG9ubHksIGlmIGZpbGUgaXMgbm90IGlnbm9yZWRcbiAgICAgIGlmIChpZ25vcmVkRmlsZXMuaGFzKGZpbGUpKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cbiAgICAgIG5hbWVzcGFjZS5mb3JFYWNoKCh2YWx1ZSwga2V5KSA9PiB7XG4gICAgICAgIGlmIChrZXkgPT09IERFRkFVTFQpIHtcbiAgICAgICAgICBleHBvcnRzLnNldChJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIsIHsgd2hlcmVVc2VkOiBuZXcgU2V0KCkgfSk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgZXhwb3J0cy5zZXQoa2V5LCB7IHdoZXJlVXNlZDogbmV3IFNldCgpIH0pO1xuICAgICAgICB9XG4gICAgICB9KTtcbiAgICB9XG4gICAgZXhwb3J0cy5zZXQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTiwgeyB3aGVyZVVzZWQ6IG5ldyBTZXQoKSB9KTtcbiAgICBleHBvcnRzLnNldChJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUiwgeyB3aGVyZVVzZWQ6IG5ldyBTZXQoKSB9KTtcbiAgICBleHBvcnRMaXN0LnNldChmaWxlLCBleHBvcnRzKTtcbiAgfSk7XG4gIGV4cG9ydEFsbC5mb3JFYWNoKCh2YWx1ZSwga2V5KSA9PiB7XG4gICAgdmFsdWUuZm9yRWFjaCh2YWwgPT4ge1xuICAgICAgY29uc3QgY3VycmVudEV4cG9ydHMgPSBleHBvcnRMaXN0LmdldCh2YWwpO1xuICAgICAgaWYgKGN1cnJlbnRFeHBvcnRzKSB7XG4gICAgICAgIGNvbnN0IGN1cnJlbnRFeHBvcnQgPSBjdXJyZW50RXhwb3J0cy5nZXQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTik7XG4gICAgICAgIGN1cnJlbnRFeHBvcnQud2hlcmVVc2VkLmFkZChrZXkpO1xuICAgICAgfVxuICAgIH0pO1xuICB9KTtcbn07XG5cbi8qKlxuICogdHJhdmVyc2UgdGhyb3VnaCBhbGwgaW1wb3J0cyBhbmQgYWRkIHRoZSByZXNwZWN0aXZlIHBhdGggdG8gdGhlIHdoZXJlVXNlZC1saXN0XG4gKiBvZiB0aGUgY29ycmVzcG9uZGluZyBleHBvcnRcbiAqL1xuY29uc3QgZGV0ZXJtaW5lVXNhZ2UgPSAoKSA9PiB7XG4gIGltcG9ydExpc3QuZm9yRWFjaCgobGlzdFZhbHVlLCBsaXN0S2V5KSA9PiB7XG4gICAgbGlzdFZhbHVlLmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgIGNvbnN0IGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldChrZXkpO1xuICAgICAgaWYgKHR5cGVvZiBleHBvcnRzICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICB2YWx1ZS5mb3JFYWNoKGN1cnJlbnRJbXBvcnQgPT4ge1xuICAgICAgICAgIGxldCBzcGVjaWZpZXI7XG4gICAgICAgICAgaWYgKGN1cnJlbnRJbXBvcnQgPT09IElNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSKSB7XG4gICAgICAgICAgICBzcGVjaWZpZXIgPSBJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUjtcbiAgICAgICAgICB9IGVsc2UgaWYgKGN1cnJlbnRJbXBvcnQgPT09IElNUE9SVF9ERUZBVUxUX1NQRUNJRklFUikge1xuICAgICAgICAgICAgc3BlY2lmaWVyID0gSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSO1xuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBzcGVjaWZpZXIgPSBjdXJyZW50SW1wb3J0O1xuICAgICAgICAgIH1cbiAgICAgICAgICBpZiAodHlwZW9mIHNwZWNpZmllciAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGNvbnN0IGV4cG9ydFN0YXRlbWVudCA9IGV4cG9ydHMuZ2V0KHNwZWNpZmllcik7XG4gICAgICAgICAgICBpZiAodHlwZW9mIGV4cG9ydFN0YXRlbWVudCAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgICAgY29uc3QgeyB3aGVyZVVzZWQgfSA9IGV4cG9ydFN0YXRlbWVudDtcbiAgICAgICAgICAgICAgd2hlcmVVc2VkLmFkZChsaXN0S2V5KTtcbiAgICAgICAgICAgICAgZXhwb3J0cy5zZXQoc3BlY2lmaWVyLCB7IHdoZXJlVXNlZCB9KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9XG4gICAgICAgIH0pO1xuICAgICAgfVxuICAgIH0pO1xuICB9KTtcbn07XG5cbmNvbnN0IGdldFNyYyA9IHNyYyA9PiB7XG4gIGlmIChzcmMpIHtcbiAgICByZXR1cm4gc3JjO1xuICB9XG4gIHJldHVybiBbcHJvY2Vzcy5jd2QoKV07XG59O1xuXG4vKipcbiAqIHByZXBhcmUgdGhlIGxpc3RzIG9mIGV4aXN0aW5nIGltcG9ydHMgYW5kIGV4cG9ydHMgLSBzaG91bGQgb25seSBiZSBleGVjdXRlZCBvbmNlIGF0XG4gKiB0aGUgc3RhcnQgb2YgYSBuZXcgZXNsaW50IHJ1blxuICovXG5sZXQgc3JjRmlsZXM7XG5sZXQgbGFzdFByZXBhcmVLZXk7XG5jb25zdCBkb1ByZXBhcmF0aW9uID0gKHNyYywgaWdub3JlRXhwb3J0cywgY29udGV4dCkgPT4ge1xuICBjb25zdCBwcmVwYXJlS2V5ID0gSlNPTi5zdHJpbmdpZnkoe1xuICAgIHNyYzogKHNyYyB8fCBbXSkuc29ydCgpLFxuICAgIGlnbm9yZUV4cG9ydHM6IChpZ25vcmVFeHBvcnRzIHx8IFtdKS5zb3J0KCksXG4gICAgZXh0ZW5zaW9uczogQXJyYXkuZnJvbShnZXRGaWxlRXh0ZW5zaW9ucyhjb250ZXh0LnNldHRpbmdzKSkuc29ydCgpLFxuICB9KTtcbiAgaWYgKHByZXBhcmVLZXkgPT09IGxhc3RQcmVwYXJlS2V5KSB7XG4gICAgcmV0dXJuO1xuICB9XG5cbiAgaW1wb3J0TGlzdC5jbGVhcigpO1xuICBleHBvcnRMaXN0LmNsZWFyKCk7XG4gIGlnbm9yZWRGaWxlcy5jbGVhcigpO1xuICBmaWxlc091dHNpZGVTcmMuY2xlYXIoKTtcblxuICBzcmNGaWxlcyA9IHJlc29sdmVGaWxlcyhnZXRTcmMoc3JjKSwgaWdub3JlRXhwb3J0cywgY29udGV4dCk7XG4gIHByZXBhcmVJbXBvcnRzQW5kRXhwb3J0cyhzcmNGaWxlcywgY29udGV4dCk7XG4gIGRldGVybWluZVVzYWdlKCk7XG4gIGxhc3RQcmVwYXJlS2V5ID0gcHJlcGFyZUtleTtcbn07XG5cbmNvbnN0IG5ld05hbWVzcGFjZUltcG9ydEV4aXN0cyA9IHNwZWNpZmllcnMgPT5cbiAgc3BlY2lmaWVycy5zb21lKCh7IHR5cGUgfSkgPT4gdHlwZSA9PT0gSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVIpO1xuXG5jb25zdCBuZXdEZWZhdWx0SW1wb3J0RXhpc3RzID0gc3BlY2lmaWVycyA9PlxuICBzcGVjaWZpZXJzLnNvbWUoKHsgdHlwZSB9KSA9PiB0eXBlID09PSBJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpO1xuXG5jb25zdCBmaWxlSXNJblBrZyA9IGZpbGUgPT4ge1xuICBjb25zdCB7IHBhdGgsIHBrZyB9ID0gcmVhZFBrZ1VwKHsgY3dkOiBmaWxlIH0pO1xuICBjb25zdCBiYXNlUGF0aCA9IGRpcm5hbWUocGF0aCk7XG5cbiAgY29uc3QgY2hlY2tQa2dGaWVsZFN0cmluZyA9IHBrZ0ZpZWxkID0+IHtcbiAgICBpZiAoam9pbihiYXNlUGF0aCwgcGtnRmllbGQpID09PSBmaWxlKSB7XG4gICAgICByZXR1cm4gdHJ1ZTtcbiAgICB9XG4gIH07XG5cbiAgY29uc3QgY2hlY2tQa2dGaWVsZE9iamVjdCA9IHBrZ0ZpZWxkID0+IHtcbiAgICBjb25zdCBwa2dGaWVsZEZpbGVzID0gdmFsdWVzKHBrZ0ZpZWxkKS5tYXAodmFsdWUgPT4gam9pbihiYXNlUGF0aCwgdmFsdWUpKTtcbiAgICBpZiAoaW5jbHVkZXMocGtnRmllbGRGaWxlcywgZmlsZSkpIHtcbiAgICAgIHJldHVybiB0cnVlO1xuICAgIH1cbiAgfTtcblxuICBjb25zdCBjaGVja1BrZ0ZpZWxkID0gcGtnRmllbGQgPT4ge1xuICAgIGlmICh0eXBlb2YgcGtnRmllbGQgPT09ICdzdHJpbmcnKSB7XG4gICAgICByZXR1cm4gY2hlY2tQa2dGaWVsZFN0cmluZyhwa2dGaWVsZCk7XG4gICAgfVxuXG4gICAgaWYgKHR5cGVvZiBwa2dGaWVsZCA9PT0gJ29iamVjdCcpIHtcbiAgICAgIHJldHVybiBjaGVja1BrZ0ZpZWxkT2JqZWN0KHBrZ0ZpZWxkKTtcbiAgICB9XG4gIH07XG5cbiAgaWYgKHBrZy5wcml2YXRlID09PSB0cnVlKSB7XG4gICAgcmV0dXJuIGZhbHNlO1xuICB9XG5cbiAgaWYgKHBrZy5iaW4pIHtcbiAgICBpZiAoY2hlY2tQa2dGaWVsZChwa2cuYmluKSkge1xuICAgICAgcmV0dXJuIHRydWU7XG4gICAgfVxuICB9XG5cbiAgaWYgKHBrZy5icm93c2VyKSB7XG4gICAgaWYgKGNoZWNrUGtnRmllbGQocGtnLmJyb3dzZXIpKSB7XG4gICAgICByZXR1cm4gdHJ1ZTtcbiAgICB9XG4gIH1cblxuICBpZiAocGtnLm1haW4pIHtcbiAgICBpZiAoY2hlY2tQa2dGaWVsZFN0cmluZyhwa2cubWFpbikpIHtcbiAgICAgIHJldHVybiB0cnVlO1xuICAgIH1cbiAgfVxuXG4gIHJldHVybiBmYWxzZTtcbn07XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHsgdXJsOiBkb2NzVXJsKCduby11bnVzZWQtbW9kdWxlcycpIH0sXG4gICAgc2NoZW1hOiBbe1xuICAgICAgcHJvcGVydGllczoge1xuICAgICAgICBzcmM6IHtcbiAgICAgICAgICBkZXNjcmlwdGlvbjogJ2ZpbGVzL3BhdGhzIHRvIGJlIGFuYWx5emVkIChvbmx5IGZvciB1bnVzZWQgZXhwb3J0cyknLFxuICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgbWluSXRlbXM6IDEsXG4gICAgICAgICAgaXRlbXM6IHtcbiAgICAgICAgICAgIHR5cGU6ICdzdHJpbmcnLFxuICAgICAgICAgICAgbWluTGVuZ3RoOiAxLFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgIGlnbm9yZUV4cG9ydHM6IHtcbiAgICAgICAgICBkZXNjcmlwdGlvbjpcbiAgICAgICAgICAgICdmaWxlcy9wYXRocyBmb3Igd2hpY2ggdW51c2VkIGV4cG9ydHMgd2lsbCBub3QgYmUgcmVwb3J0ZWQgKGUuZyBtb2R1bGUgZW50cnkgcG9pbnRzKScsXG4gICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICBtaW5JdGVtczogMSxcbiAgICAgICAgICBpdGVtczoge1xuICAgICAgICAgICAgdHlwZTogJ3N0cmluZycsXG4gICAgICAgICAgICBtaW5MZW5ndGg6IDEsXG4gICAgICAgICAgfSxcbiAgICAgICAgfSxcbiAgICAgICAgbWlzc2luZ0V4cG9ydHM6IHtcbiAgICAgICAgICBkZXNjcmlwdGlvbjogJ3JlcG9ydCBtb2R1bGVzIHdpdGhvdXQgYW55IGV4cG9ydHMnLFxuICAgICAgICAgIHR5cGU6ICdib29sZWFuJyxcbiAgICAgICAgfSxcbiAgICAgICAgdW51c2VkRXhwb3J0czoge1xuICAgICAgICAgIGRlc2NyaXB0aW9uOiAncmVwb3J0IGV4cG9ydHMgd2l0aG91dCBhbnkgdXNhZ2UnLFxuICAgICAgICAgIHR5cGU6ICdib29sZWFuJyxcbiAgICAgICAgfSxcbiAgICAgIH0sXG4gICAgICBub3Q6IHtcbiAgICAgICAgcHJvcGVydGllczoge1xuICAgICAgICAgIHVudXNlZEV4cG9ydHM6IHsgZW51bTogW2ZhbHNlXSB9LFxuICAgICAgICAgIG1pc3NpbmdFeHBvcnRzOiB7IGVudW06IFtmYWxzZV0gfSxcbiAgICAgICAgfSxcbiAgICAgIH0sXG4gICAgICBhbnlPZjpbe1xuICAgICAgICBub3Q6IHtcbiAgICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgICB1bnVzZWRFeHBvcnRzOiB7IGVudW06IFt0cnVlXSB9LFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgIHJlcXVpcmVkOiBbJ21pc3NpbmdFeHBvcnRzJ10sXG4gICAgICB9LCB7XG4gICAgICAgIG5vdDoge1xuICAgICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICAgIG1pc3NpbmdFeHBvcnRzOiB7IGVudW06IFt0cnVlXSB9LFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgIHJlcXVpcmVkOiBbJ3VudXNlZEV4cG9ydHMnXSxcbiAgICAgIH0sIHtcbiAgICAgICAgcHJvcGVydGllczoge1xuICAgICAgICAgIHVudXNlZEV4cG9ydHM6IHsgZW51bTogW3RydWVdIH0sXG4gICAgICAgIH0sXG4gICAgICAgIHJlcXVpcmVkOiBbJ3VudXNlZEV4cG9ydHMnXSxcbiAgICAgIH0sIHtcbiAgICAgICAgcHJvcGVydGllczoge1xuICAgICAgICAgIG1pc3NpbmdFeHBvcnRzOiB7IGVudW06IFt0cnVlXSB9LFxuICAgICAgICB9LFxuICAgICAgICByZXF1aXJlZDogWydtaXNzaW5nRXhwb3J0cyddLFxuICAgICAgfV0sXG4gICAgfV0sXG4gIH0sXG5cbiAgY3JlYXRlOiBjb250ZXh0ID0+IHtcbiAgICBjb25zdCB7XG4gICAgICBzcmMsXG4gICAgICBpZ25vcmVFeHBvcnRzID0gW10sXG4gICAgICBtaXNzaW5nRXhwb3J0cyxcbiAgICAgIHVudXNlZEV4cG9ydHMsXG4gICAgfSA9IGNvbnRleHQub3B0aW9uc1swXSB8fCB7fTtcblxuICAgIGlmICh1bnVzZWRFeHBvcnRzKSB7XG4gICAgICBkb1ByZXBhcmF0aW9uKHNyYywgaWdub3JlRXhwb3J0cywgY29udGV4dCk7XG4gICAgfVxuXG4gICAgY29uc3QgZmlsZSA9IGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSA/IGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSgpIDogY29udGV4dC5nZXRGaWxlbmFtZSgpO1xuXG4gICAgY29uc3QgY2hlY2tFeHBvcnRQcmVzZW5jZSA9IG5vZGUgPT4ge1xuICAgICAgaWYgKCFtaXNzaW5nRXhwb3J0cykge1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIGlmIChpZ25vcmVkRmlsZXMuaGFzKGZpbGUpKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgY29uc3QgZXhwb3J0Q291bnQgPSBleHBvcnRMaXN0LmdldChmaWxlKTtcbiAgICAgIGNvbnN0IGV4cG9ydEFsbCA9IGV4cG9ydENvdW50LmdldChFWFBPUlRfQUxMX0RFQ0xBUkFUSU9OKTtcbiAgICAgIGNvbnN0IG5hbWVzcGFjZUltcG9ydHMgPSBleHBvcnRDb3VudC5nZXQoSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVIpO1xuXG4gICAgICBleHBvcnRDb3VudC5kZWxldGUoRVhQT1JUX0FMTF9ERUNMQVJBVElPTik7XG4gICAgICBleHBvcnRDb3VudC5kZWxldGUoSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVIpO1xuICAgICAgaWYgKGV4cG9ydENvdW50LnNpemUgPCAxKSB7XG4gICAgICAgIC8vIG5vZGUuYm9keVswXSA9PT0gJ3VuZGVmaW5lZCcgb25seSBoYXBwZW5zLCBpZiBldmVyeXRoaW5nIGlzIGNvbW1lbnRlZCBvdXQgaW4gdGhlIGZpbGVcbiAgICAgICAgLy8gYmVpbmcgbGludGVkXG4gICAgICAgIGNvbnRleHQucmVwb3J0KG5vZGUuYm9keVswXSA/IG5vZGUuYm9keVswXSA6IG5vZGUsICdObyBleHBvcnRzIGZvdW5kJyk7XG4gICAgICB9XG4gICAgICBleHBvcnRDb3VudC5zZXQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTiwgZXhwb3J0QWxsKTtcbiAgICAgIGV4cG9ydENvdW50LnNldChJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUiwgbmFtZXNwYWNlSW1wb3J0cyk7XG4gICAgfTtcblxuICAgIGNvbnN0IGNoZWNrVXNhZ2UgPSAobm9kZSwgZXhwb3J0ZWRWYWx1ZSkgPT4ge1xuICAgICAgaWYgKCF1bnVzZWRFeHBvcnRzKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgaWYgKGlnbm9yZWRGaWxlcy5oYXMoZmlsZSkpIHtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuXG4gICAgICBpZiAoZmlsZUlzSW5Qa2coZmlsZSkpIHtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuXG4gICAgICBpZiAoZmlsZXNPdXRzaWRlU3JjLmhhcyhmaWxlKSkge1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIC8vIG1ha2Ugc3VyZSBmaWxlIHRvIGJlIGxpbnRlZCBpcyBpbmNsdWRlZCBpbiBzb3VyY2UgZmlsZXNcbiAgICAgIGlmICghc3JjRmlsZXMuaGFzKGZpbGUpKSB7XG4gICAgICAgIHNyY0ZpbGVzID0gcmVzb2x2ZUZpbGVzKGdldFNyYyhzcmMpLCBpZ25vcmVFeHBvcnRzLCBjb250ZXh0KTtcbiAgICAgICAgaWYgKCFzcmNGaWxlcy5oYXMoZmlsZSkpIHtcbiAgICAgICAgICBmaWxlc091dHNpZGVTcmMuYWRkKGZpbGUpO1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgfVxuXG4gICAgICBleHBvcnRzID0gZXhwb3J0TGlzdC5nZXQoZmlsZSk7XG5cbiAgICAgIC8vIHNwZWNpYWwgY2FzZTogZXhwb3J0ICogZnJvbVxuICAgICAgY29uc3QgZXhwb3J0QWxsID0gZXhwb3J0cy5nZXQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTik7XG4gICAgICBpZiAodHlwZW9mIGV4cG9ydEFsbCAhPT0gJ3VuZGVmaW5lZCcgJiYgZXhwb3J0ZWRWYWx1ZSAhPT0gSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSKSB7XG4gICAgICAgIGlmIChleHBvcnRBbGwud2hlcmVVc2VkLnNpemUgPiAwKSB7XG4gICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgIC8vIHNwZWNpYWwgY2FzZTogbmFtZXNwYWNlIGltcG9ydFxuICAgICAgY29uc3QgbmFtZXNwYWNlSW1wb3J0cyA9IGV4cG9ydHMuZ2V0KElNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSKTtcbiAgICAgIGlmICh0eXBlb2YgbmFtZXNwYWNlSW1wb3J0cyAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgaWYgKG5hbWVzcGFjZUltcG9ydHMud2hlcmVVc2VkLnNpemUgPiAwKSB7XG4gICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgIC8vIGV4cG9ydHNMaXN0IHdpbGwgYWx3YXlzIG1hcCBhbnkgaW1wb3J0ZWQgdmFsdWUgb2YgJ2RlZmF1bHQnIHRvICdJbXBvcnREZWZhdWx0U3BlY2lmaWVyJ1xuICAgICAgY29uc3QgZXhwb3J0c0tleSA9IGV4cG9ydGVkVmFsdWUgPT09IERFRkFVTFQgPyBJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIgOiBleHBvcnRlZFZhbHVlO1xuXG4gICAgICBjb25zdCBleHBvcnRTdGF0ZW1lbnQgPSBleHBvcnRzLmdldChleHBvcnRzS2V5KTtcblxuICAgICAgY29uc3QgdmFsdWUgPSBleHBvcnRzS2V5ID09PSBJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIgPyBERUZBVUxUIDogZXhwb3J0c0tleTtcblxuICAgICAgaWYgKHR5cGVvZiBleHBvcnRTdGF0ZW1lbnQgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgIGlmIChleHBvcnRTdGF0ZW1lbnQud2hlcmVVc2VkLnNpemUgPCAxKSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoXG4gICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgYGV4cG9ydGVkIGRlY2xhcmF0aW9uICcke3ZhbHVlfScgbm90IHVzZWQgd2l0aGluIG90aGVyIG1vZHVsZXNgLFxuICAgICAgICAgICk7XG4gICAgICAgIH1cbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KFxuICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgYGV4cG9ydGVkIGRlY2xhcmF0aW9uICcke3ZhbHVlfScgbm90IHVzZWQgd2l0aGluIG90aGVyIG1vZHVsZXNgLFxuICAgICAgICApO1xuICAgICAgfVxuICAgIH07XG5cbiAgICAvKipcbiAgICAgKiBvbmx5IHVzZWZ1bCBmb3IgdG9vbHMgbGlrZSB2c2NvZGUtZXNsaW50XG4gICAgICpcbiAgICAgKiB1cGRhdGUgbGlzdHMgb2YgZXhpc3RpbmcgZXhwb3J0cyBkdXJpbmcgcnVudGltZVxuICAgICAqL1xuICAgIGNvbnN0IHVwZGF0ZUV4cG9ydFVzYWdlID0gbm9kZSA9PiB7XG4gICAgICBpZiAoaWdub3JlZEZpbGVzLmhhcyhmaWxlKSkge1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIGxldCBleHBvcnRzID0gZXhwb3J0TGlzdC5nZXQoZmlsZSk7XG5cbiAgICAgIC8vIG5ldyBtb2R1bGUgaGFzIGJlZW4gY3JlYXRlZCBkdXJpbmcgcnVudGltZVxuICAgICAgLy8gaW5jbHVkZSBpdCBpbiBmdXJ0aGVyIHByb2Nlc3NpbmdcbiAgICAgIGlmICh0eXBlb2YgZXhwb3J0cyA9PT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgZXhwb3J0cyA9IG5ldyBNYXAoKTtcbiAgICAgIH1cblxuICAgICAgY29uc3QgbmV3RXhwb3J0cyA9IG5ldyBNYXAoKTtcbiAgICAgIGNvbnN0IG5ld0V4cG9ydElkZW50aWZpZXJzID0gbmV3IFNldCgpO1xuXG4gICAgICBub2RlLmJvZHkuZm9yRWFjaCgoeyB0eXBlLCBkZWNsYXJhdGlvbiwgc3BlY2lmaWVycyB9KSA9PiB7XG4gICAgICAgIGlmICh0eXBlID09PSBFWFBPUlRfREVGQVVMVF9ERUNMQVJBVElPTikge1xuICAgICAgICAgIG5ld0V4cG9ydElkZW50aWZpZXJzLmFkZChJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpO1xuICAgICAgICB9XG4gICAgICAgIGlmICh0eXBlID09PSBFWFBPUlRfTkFNRURfREVDTEFSQVRJT04pIHtcbiAgICAgICAgICBpZiAoc3BlY2lmaWVycy5sZW5ndGggPiAwKSB7XG4gICAgICAgICAgICBzcGVjaWZpZXJzLmZvckVhY2goc3BlY2lmaWVyID0+IHtcbiAgICAgICAgICAgICAgaWYgKHNwZWNpZmllci5leHBvcnRlZCkge1xuICAgICAgICAgICAgICAgIG5ld0V4cG9ydElkZW50aWZpZXJzLmFkZChzcGVjaWZpZXIuZXhwb3J0ZWQubmFtZSB8fCBzcGVjaWZpZXIuZXhwb3J0ZWQudmFsdWUpO1xuICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICB9XG4gICAgICAgICAgZm9yRWFjaERlY2xhcmF0aW9uSWRlbnRpZmllcihkZWNsYXJhdGlvbiwgKG5hbWUpID0+IHtcbiAgICAgICAgICAgIG5ld0V4cG9ydElkZW50aWZpZXJzLmFkZChuYW1lKTtcbiAgICAgICAgICB9KTtcbiAgICAgICAgfVxuICAgICAgfSk7XG5cbiAgICAgIC8vIG9sZCBleHBvcnRzIGV4aXN0IHdpdGhpbiBsaXN0IG9mIG5ldyBleHBvcnRzIGlkZW50aWZpZXJzOiBhZGQgdG8gbWFwIG9mIG5ldyBleHBvcnRzXG4gICAgICBleHBvcnRzLmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgICAgaWYgKG5ld0V4cG9ydElkZW50aWZpZXJzLmhhcyhrZXkpKSB7XG4gICAgICAgICAgbmV3RXhwb3J0cy5zZXQoa2V5LCB2YWx1ZSk7XG4gICAgICAgIH1cbiAgICAgIH0pO1xuXG4gICAgICAvLyBuZXcgZXhwb3J0IGlkZW50aWZpZXJzIGFkZGVkOiBhZGQgdG8gbWFwIG9mIG5ldyBleHBvcnRzXG4gICAgICBuZXdFeHBvcnRJZGVudGlmaWVycy5mb3JFYWNoKGtleSA9PiB7XG4gICAgICAgIGlmICghZXhwb3J0cy5oYXMoa2V5KSkge1xuICAgICAgICAgIG5ld0V4cG9ydHMuc2V0KGtleSwgeyB3aGVyZVVzZWQ6IG5ldyBTZXQoKSB9KTtcbiAgICAgICAgfVxuICAgICAgfSk7XG5cbiAgICAgIC8vIHByZXNlcnZlIGluZm9ybWF0aW9uIGFib3V0IG5hbWVzcGFjZSBpbXBvcnRzXG4gICAgICBjb25zdCBleHBvcnRBbGwgPSBleHBvcnRzLmdldChFWFBPUlRfQUxMX0RFQ0xBUkFUSU9OKTtcbiAgICAgIGxldCBuYW1lc3BhY2VJbXBvcnRzID0gZXhwb3J0cy5nZXQoSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVIpO1xuXG4gICAgICBpZiAodHlwZW9mIG5hbWVzcGFjZUltcG9ydHMgPT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgIG5hbWVzcGFjZUltcG9ydHMgPSB7IHdoZXJlVXNlZDogbmV3IFNldCgpIH07XG4gICAgICB9XG5cbiAgICAgIG5ld0V4cG9ydHMuc2V0KEVYUE9SVF9BTExfREVDTEFSQVRJT04sIGV4cG9ydEFsbCk7XG4gICAgICBuZXdFeHBvcnRzLnNldChJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUiwgbmFtZXNwYWNlSW1wb3J0cyk7XG4gICAgICBleHBvcnRMaXN0LnNldChmaWxlLCBuZXdFeHBvcnRzKTtcbiAgICB9O1xuXG4gICAgLyoqXG4gICAgICogb25seSB1c2VmdWwgZm9yIHRvb2xzIGxpa2UgdnNjb2RlLWVzbGludFxuICAgICAqXG4gICAgICogdXBkYXRlIGxpc3RzIG9mIGV4aXN0aW5nIGltcG9ydHMgZHVyaW5nIHJ1bnRpbWVcbiAgICAgKi9cbiAgICBjb25zdCB1cGRhdGVJbXBvcnRVc2FnZSA9IG5vZGUgPT4ge1xuICAgICAgaWYgKCF1bnVzZWRFeHBvcnRzKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgbGV0IG9sZEltcG9ydFBhdGhzID0gaW1wb3J0TGlzdC5nZXQoZmlsZSk7XG4gICAgICBpZiAodHlwZW9mIG9sZEltcG9ydFBhdGhzID09PSAndW5kZWZpbmVkJykge1xuICAgICAgICBvbGRJbXBvcnRQYXRocyA9IG5ldyBNYXAoKTtcbiAgICAgIH1cblxuICAgICAgY29uc3Qgb2xkTmFtZXNwYWNlSW1wb3J0cyA9IG5ldyBTZXQoKTtcbiAgICAgIGNvbnN0IG5ld05hbWVzcGFjZUltcG9ydHMgPSBuZXcgU2V0KCk7XG5cbiAgICAgIGNvbnN0IG9sZEV4cG9ydEFsbCA9IG5ldyBTZXQoKTtcbiAgICAgIGNvbnN0IG5ld0V4cG9ydEFsbCA9IG5ldyBTZXQoKTtcblxuICAgICAgY29uc3Qgb2xkRGVmYXVsdEltcG9ydHMgPSBuZXcgU2V0KCk7XG4gICAgICBjb25zdCBuZXdEZWZhdWx0SW1wb3J0cyA9IG5ldyBTZXQoKTtcblxuICAgICAgY29uc3Qgb2xkSW1wb3J0cyA9IG5ldyBNYXAoKTtcbiAgICAgIGNvbnN0IG5ld0ltcG9ydHMgPSBuZXcgTWFwKCk7XG4gICAgICBvbGRJbXBvcnRQYXRocy5mb3JFYWNoKCh2YWx1ZSwga2V5KSA9PiB7XG4gICAgICAgIGlmICh2YWx1ZS5oYXMoRVhQT1JUX0FMTF9ERUNMQVJBVElPTikpIHtcbiAgICAgICAgICBvbGRFeHBvcnRBbGwuYWRkKGtleSk7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHZhbHVlLmhhcyhJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUikpIHtcbiAgICAgICAgICBvbGROYW1lc3BhY2VJbXBvcnRzLmFkZChrZXkpO1xuICAgICAgICB9XG4gICAgICAgIGlmICh2YWx1ZS5oYXMoSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSKSkge1xuICAgICAgICAgIG9sZERlZmF1bHRJbXBvcnRzLmFkZChrZXkpO1xuICAgICAgICB9XG4gICAgICAgIHZhbHVlLmZvckVhY2godmFsID0+IHtcbiAgICAgICAgICBpZiAodmFsICE9PSBJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUiAmJlxuICAgICAgICAgICAgICB2YWwgIT09IElNUE9SVF9ERUZBVUxUX1NQRUNJRklFUikge1xuICAgICAgICAgICAgb2xkSW1wb3J0cy5zZXQodmFsLCBrZXkpO1xuICAgICAgICAgIH1cbiAgICAgICAgfSk7XG4gICAgICB9KTtcblxuICAgICAgZnVuY3Rpb24gcHJvY2Vzc0R5bmFtaWNJbXBvcnQoc291cmNlKSB7XG4gICAgICAgIGlmIChzb3VyY2UudHlwZSAhPT0gJ0xpdGVyYWwnKSB7XG4gICAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgcCA9IHJlc29sdmUoc291cmNlLnZhbHVlLCBjb250ZXh0KTtcbiAgICAgICAgaWYgKHAgPT0gbnVsbCkge1xuICAgICAgICAgIHJldHVybiBudWxsO1xuICAgICAgICB9XG4gICAgICAgIG5ld05hbWVzcGFjZUltcG9ydHMuYWRkKHApO1xuICAgICAgfVxuXG4gICAgICB2aXNpdChub2RlLCB2aXNpdG9yS2V5TWFwLmdldChmaWxlKSwge1xuICAgICAgICBJbXBvcnRFeHByZXNzaW9uKGNoaWxkKSB7XG4gICAgICAgICAgcHJvY2Vzc0R5bmFtaWNJbXBvcnQoY2hpbGQuc291cmNlKTtcbiAgICAgICAgfSxcbiAgICAgICAgQ2FsbEV4cHJlc3Npb24oY2hpbGQpIHtcbiAgICAgICAgICBpZiAoY2hpbGQuY2FsbGVlLnR5cGUgPT09ICdJbXBvcnQnKSB7XG4gICAgICAgICAgICBwcm9jZXNzRHluYW1pY0ltcG9ydChjaGlsZC5hcmd1bWVudHNbMF0pO1xuICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgIH0pO1xuXG4gICAgICBub2RlLmJvZHkuZm9yRWFjaChhc3ROb2RlID0+IHtcbiAgICAgICAgbGV0IHJlc29sdmVkUGF0aDtcblxuICAgICAgICAvLyBzdXBwb3J0IGZvciBleHBvcnQgeyB2YWx1ZSB9IGZyb20gJ21vZHVsZSdcbiAgICAgICAgaWYgKGFzdE5vZGUudHlwZSA9PT0gRVhQT1JUX05BTUVEX0RFQ0xBUkFUSU9OKSB7XG4gICAgICAgICAgaWYgKGFzdE5vZGUuc291cmNlKSB7XG4gICAgICAgICAgICByZXNvbHZlZFBhdGggPSByZXNvbHZlKGFzdE5vZGUuc291cmNlLnJhdy5yZXBsYWNlKC8oJ3xcIikvZywgJycpLCBjb250ZXh0KTtcbiAgICAgICAgICAgIGFzdE5vZGUuc3BlY2lmaWVycy5mb3JFYWNoKHNwZWNpZmllciA9PiB7XG4gICAgICAgICAgICAgIGNvbnN0IG5hbWUgPSBzcGVjaWZpZXIubG9jYWwubmFtZSB8fCBzcGVjaWZpZXIubG9jYWwudmFsdWU7XG4gICAgICAgICAgICAgIGlmIChuYW1lID09PSBERUZBVUxUKSB7XG4gICAgICAgICAgICAgICAgbmV3RGVmYXVsdEltcG9ydHMuYWRkKHJlc29sdmVkUGF0aCk7XG4gICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgbmV3SW1wb3J0cy5zZXQobmFtZSwgcmVzb2x2ZWRQYXRoKTtcbiAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG5cbiAgICAgICAgaWYgKGFzdE5vZGUudHlwZSA9PT0gRVhQT1JUX0FMTF9ERUNMQVJBVElPTikge1xuICAgICAgICAgIHJlc29sdmVkUGF0aCA9IHJlc29sdmUoYXN0Tm9kZS5zb3VyY2UucmF3LnJlcGxhY2UoLygnfFwiKS9nLCAnJyksIGNvbnRleHQpO1xuICAgICAgICAgIG5ld0V4cG9ydEFsbC5hZGQocmVzb2x2ZWRQYXRoKTtcbiAgICAgICAgfVxuXG4gICAgICAgIGlmIChhc3ROb2RlLnR5cGUgPT09IElNUE9SVF9ERUNMQVJBVElPTikge1xuICAgICAgICAgIHJlc29sdmVkUGF0aCA9IHJlc29sdmUoYXN0Tm9kZS5zb3VyY2UucmF3LnJlcGxhY2UoLygnfFwiKS9nLCAnJyksIGNvbnRleHQpO1xuICAgICAgICAgIGlmICghcmVzb2x2ZWRQYXRoKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgfVxuXG4gICAgICAgICAgaWYgKGlzTm9kZU1vZHVsZShyZXNvbHZlZFBhdGgpKSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgfVxuXG4gICAgICAgICAgaWYgKG5ld05hbWVzcGFjZUltcG9ydEV4aXN0cyhhc3ROb2RlLnNwZWNpZmllcnMpKSB7XG4gICAgICAgICAgICBuZXdOYW1lc3BhY2VJbXBvcnRzLmFkZChyZXNvbHZlZFBhdGgpO1xuICAgICAgICAgIH1cblxuICAgICAgICAgIGlmIChuZXdEZWZhdWx0SW1wb3J0RXhpc3RzKGFzdE5vZGUuc3BlY2lmaWVycykpIHtcbiAgICAgICAgICAgIG5ld0RlZmF1bHRJbXBvcnRzLmFkZChyZXNvbHZlZFBhdGgpO1xuICAgICAgICAgIH1cblxuICAgICAgICAgIGFzdE5vZGUuc3BlY2lmaWVycy5mb3JFYWNoKHNwZWNpZmllciA9PiB7XG4gICAgICAgICAgICBpZiAoc3BlY2lmaWVyLnR5cGUgPT09IElNUE9SVF9ERUZBVUxUX1NQRUNJRklFUiB8fFxuICAgICAgICAgICAgICAgIHNwZWNpZmllci50eXBlID09PSBJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUikge1xuICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBuZXdJbXBvcnRzLnNldChzcGVjaWZpZXIuaW1wb3J0ZWQubmFtZSB8fCBzcGVjaWZpZXIuaW1wb3J0ZWQudmFsdWUsIHJlc29sdmVkUGF0aCk7XG4gICAgICAgICAgfSk7XG4gICAgICAgIH1cbiAgICAgIH0pO1xuXG4gICAgICBuZXdFeHBvcnRBbGwuZm9yRWFjaCh2YWx1ZSA9PiB7XG4gICAgICAgIGlmICghb2xkRXhwb3J0QWxsLmhhcyh2YWx1ZSkpIHtcbiAgICAgICAgICBsZXQgaW1wb3J0cyA9IG9sZEltcG9ydFBhdGhzLmdldCh2YWx1ZSk7XG4gICAgICAgICAgaWYgKHR5cGVvZiBpbXBvcnRzID09PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgaW1wb3J0cyA9IG5ldyBTZXQoKTtcbiAgICAgICAgICB9XG4gICAgICAgICAgaW1wb3J0cy5hZGQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTik7XG4gICAgICAgICAgb2xkSW1wb3J0UGF0aHMuc2V0KHZhbHVlLCBpbXBvcnRzKTtcblxuICAgICAgICAgIGxldCBleHBvcnRzID0gZXhwb3J0TGlzdC5nZXQodmFsdWUpO1xuICAgICAgICAgIGxldCBjdXJyZW50RXhwb3J0O1xuICAgICAgICAgIGlmICh0eXBlb2YgZXhwb3J0cyAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGN1cnJlbnRFeHBvcnQgPSBleHBvcnRzLmdldChFWFBPUlRfQUxMX0RFQ0xBUkFUSU9OKTtcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgZXhwb3J0cyA9IG5ldyBNYXAoKTtcbiAgICAgICAgICAgIGV4cG9ydExpc3Quc2V0KHZhbHVlLCBleHBvcnRzKTtcbiAgICAgICAgICB9XG5cbiAgICAgICAgICBpZiAodHlwZW9mIGN1cnJlbnRFeHBvcnQgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjdXJyZW50RXhwb3J0LndoZXJlVXNlZC5hZGQoZmlsZSk7XG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGNvbnN0IHdoZXJlVXNlZCA9IG5ldyBTZXQoKTtcbiAgICAgICAgICAgIHdoZXJlVXNlZC5hZGQoZmlsZSk7XG4gICAgICAgICAgICBleHBvcnRzLnNldChFWFBPUlRfQUxMX0RFQ0xBUkFUSU9OLCB7IHdoZXJlVXNlZCB9KTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pO1xuXG4gICAgICBvbGRFeHBvcnRBbGwuZm9yRWFjaCh2YWx1ZSA9PiB7XG4gICAgICAgIGlmICghbmV3RXhwb3J0QWxsLmhhcyh2YWx1ZSkpIHtcbiAgICAgICAgICBjb25zdCBpbXBvcnRzID0gb2xkSW1wb3J0UGF0aHMuZ2V0KHZhbHVlKTtcbiAgICAgICAgICBpbXBvcnRzLmRlbGV0ZShFWFBPUlRfQUxMX0RFQ0xBUkFUSU9OKTtcblxuICAgICAgICAgIGNvbnN0IGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldCh2YWx1ZSk7XG4gICAgICAgICAgaWYgKHR5cGVvZiBleHBvcnRzICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgY29uc3QgY3VycmVudEV4cG9ydCA9IGV4cG9ydHMuZ2V0KEVYUE9SVF9BTExfREVDTEFSQVRJT04pO1xuICAgICAgICAgICAgaWYgKHR5cGVvZiBjdXJyZW50RXhwb3J0ICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgICBjdXJyZW50RXhwb3J0LndoZXJlVXNlZC5kZWxldGUoZmlsZSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KTtcblxuICAgICAgbmV3RGVmYXVsdEltcG9ydHMuZm9yRWFjaCh2YWx1ZSA9PiB7XG4gICAgICAgIGlmICghb2xkRGVmYXVsdEltcG9ydHMuaGFzKHZhbHVlKSkge1xuICAgICAgICAgIGxldCBpbXBvcnRzID0gb2xkSW1wb3J0UGF0aHMuZ2V0KHZhbHVlKTtcbiAgICAgICAgICBpZiAodHlwZW9mIGltcG9ydHMgPT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBpbXBvcnRzID0gbmV3IFNldCgpO1xuICAgICAgICAgIH1cbiAgICAgICAgICBpbXBvcnRzLmFkZChJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpO1xuICAgICAgICAgIG9sZEltcG9ydFBhdGhzLnNldCh2YWx1ZSwgaW1wb3J0cyk7XG5cbiAgICAgICAgICBsZXQgZXhwb3J0cyA9IGV4cG9ydExpc3QuZ2V0KHZhbHVlKTtcbiAgICAgICAgICBsZXQgY3VycmVudEV4cG9ydDtcbiAgICAgICAgICBpZiAodHlwZW9mIGV4cG9ydHMgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjdXJyZW50RXhwb3J0ID0gZXhwb3J0cy5nZXQoSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSKTtcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgZXhwb3J0cyA9IG5ldyBNYXAoKTtcbiAgICAgICAgICAgIGV4cG9ydExpc3Quc2V0KHZhbHVlLCBleHBvcnRzKTtcbiAgICAgICAgICB9XG5cbiAgICAgICAgICBpZiAodHlwZW9mIGN1cnJlbnRFeHBvcnQgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjdXJyZW50RXhwb3J0LndoZXJlVXNlZC5hZGQoZmlsZSk7XG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGNvbnN0IHdoZXJlVXNlZCA9IG5ldyBTZXQoKTtcbiAgICAgICAgICAgIHdoZXJlVXNlZC5hZGQoZmlsZSk7XG4gICAgICAgICAgICBleHBvcnRzLnNldChJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIsIHsgd2hlcmVVc2VkIH0pO1xuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfSk7XG5cbiAgICAgIG9sZERlZmF1bHRJbXBvcnRzLmZvckVhY2godmFsdWUgPT4ge1xuICAgICAgICBpZiAoIW5ld0RlZmF1bHRJbXBvcnRzLmhhcyh2YWx1ZSkpIHtcbiAgICAgICAgICBjb25zdCBpbXBvcnRzID0gb2xkSW1wb3J0UGF0aHMuZ2V0KHZhbHVlKTtcbiAgICAgICAgICBpbXBvcnRzLmRlbGV0ZShJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpO1xuXG4gICAgICAgICAgY29uc3QgZXhwb3J0cyA9IGV4cG9ydExpc3QuZ2V0KHZhbHVlKTtcbiAgICAgICAgICBpZiAodHlwZW9mIGV4cG9ydHMgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjb25zdCBjdXJyZW50RXhwb3J0ID0gZXhwb3J0cy5nZXQoSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSKTtcbiAgICAgICAgICAgIGlmICh0eXBlb2YgY3VycmVudEV4cG9ydCAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgICAgY3VycmVudEV4cG9ydC53aGVyZVVzZWQuZGVsZXRlKGZpbGUpO1xuICAgICAgICAgICAgfVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfSk7XG5cbiAgICAgIG5ld05hbWVzcGFjZUltcG9ydHMuZm9yRWFjaCh2YWx1ZSA9PiB7XG4gICAgICAgIGlmICghb2xkTmFtZXNwYWNlSW1wb3J0cy5oYXModmFsdWUpKSB7XG4gICAgICAgICAgbGV0IGltcG9ydHMgPSBvbGRJbXBvcnRQYXRocy5nZXQodmFsdWUpO1xuICAgICAgICAgIGlmICh0eXBlb2YgaW1wb3J0cyA9PT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGltcG9ydHMgPSBuZXcgU2V0KCk7XG4gICAgICAgICAgfVxuICAgICAgICAgIGltcG9ydHMuYWRkKElNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSKTtcbiAgICAgICAgICBvbGRJbXBvcnRQYXRocy5zZXQodmFsdWUsIGltcG9ydHMpO1xuXG4gICAgICAgICAgbGV0IGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldCh2YWx1ZSk7XG4gICAgICAgICAgbGV0IGN1cnJlbnRFeHBvcnQ7XG4gICAgICAgICAgaWYgKHR5cGVvZiBleHBvcnRzICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgY3VycmVudEV4cG9ydCA9IGV4cG9ydHMuZ2V0KElNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSKTtcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgZXhwb3J0cyA9IG5ldyBNYXAoKTtcbiAgICAgICAgICAgIGV4cG9ydExpc3Quc2V0KHZhbHVlLCBleHBvcnRzKTtcbiAgICAgICAgICB9XG5cbiAgICAgICAgICBpZiAodHlwZW9mIGN1cnJlbnRFeHBvcnQgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjdXJyZW50RXhwb3J0LndoZXJlVXNlZC5hZGQoZmlsZSk7XG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGNvbnN0IHdoZXJlVXNlZCA9IG5ldyBTZXQoKTtcbiAgICAgICAgICAgIHdoZXJlVXNlZC5hZGQoZmlsZSk7XG4gICAgICAgICAgICBleHBvcnRzLnNldChJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUiwgeyB3aGVyZVVzZWQgfSk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KTtcblxuICAgICAgb2xkTmFtZXNwYWNlSW1wb3J0cy5mb3JFYWNoKHZhbHVlID0+IHtcbiAgICAgICAgaWYgKCFuZXdOYW1lc3BhY2VJbXBvcnRzLmhhcyh2YWx1ZSkpIHtcbiAgICAgICAgICBjb25zdCBpbXBvcnRzID0gb2xkSW1wb3J0UGF0aHMuZ2V0KHZhbHVlKTtcbiAgICAgICAgICBpbXBvcnRzLmRlbGV0ZShJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUik7XG5cbiAgICAgICAgICBjb25zdCBleHBvcnRzID0gZXhwb3J0TGlzdC5nZXQodmFsdWUpO1xuICAgICAgICAgIGlmICh0eXBlb2YgZXhwb3J0cyAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGNvbnN0IGN1cnJlbnRFeHBvcnQgPSBleHBvcnRzLmdldChJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUik7XG4gICAgICAgICAgICBpZiAodHlwZW9mIGN1cnJlbnRFeHBvcnQgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICAgIGN1cnJlbnRFeHBvcnQud2hlcmVVc2VkLmRlbGV0ZShmaWxlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pO1xuXG4gICAgICBuZXdJbXBvcnRzLmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgICAgaWYgKCFvbGRJbXBvcnRzLmhhcyhrZXkpKSB7XG4gICAgICAgICAgbGV0IGltcG9ydHMgPSBvbGRJbXBvcnRQYXRocy5nZXQodmFsdWUpO1xuICAgICAgICAgIGlmICh0eXBlb2YgaW1wb3J0cyA9PT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGltcG9ydHMgPSBuZXcgU2V0KCk7XG4gICAgICAgICAgfVxuICAgICAgICAgIGltcG9ydHMuYWRkKGtleSk7XG4gICAgICAgICAgb2xkSW1wb3J0UGF0aHMuc2V0KHZhbHVlLCBpbXBvcnRzKTtcblxuICAgICAgICAgIGxldCBleHBvcnRzID0gZXhwb3J0TGlzdC5nZXQodmFsdWUpO1xuICAgICAgICAgIGxldCBjdXJyZW50RXhwb3J0O1xuICAgICAgICAgIGlmICh0eXBlb2YgZXhwb3J0cyAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGN1cnJlbnRFeHBvcnQgPSBleHBvcnRzLmdldChrZXkpO1xuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBleHBvcnRzID0gbmV3IE1hcCgpO1xuICAgICAgICAgICAgZXhwb3J0TGlzdC5zZXQodmFsdWUsIGV4cG9ydHMpO1xuICAgICAgICAgIH1cblxuICAgICAgICAgIGlmICh0eXBlb2YgY3VycmVudEV4cG9ydCAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGN1cnJlbnRFeHBvcnQud2hlcmVVc2VkLmFkZChmaWxlKTtcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgY29uc3Qgd2hlcmVVc2VkID0gbmV3IFNldCgpO1xuICAgICAgICAgICAgd2hlcmVVc2VkLmFkZChmaWxlKTtcbiAgICAgICAgICAgIGV4cG9ydHMuc2V0KGtleSwgeyB3aGVyZVVzZWQgfSk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KTtcblxuICAgICAgb2xkSW1wb3J0cy5mb3JFYWNoKCh2YWx1ZSwga2V5KSA9PiB7XG4gICAgICAgIGlmICghbmV3SW1wb3J0cy5oYXMoa2V5KSkge1xuICAgICAgICAgIGNvbnN0IGltcG9ydHMgPSBvbGRJbXBvcnRQYXRocy5nZXQodmFsdWUpO1xuICAgICAgICAgIGltcG9ydHMuZGVsZXRlKGtleSk7XG5cbiAgICAgICAgICBjb25zdCBleHBvcnRzID0gZXhwb3J0TGlzdC5nZXQodmFsdWUpO1xuICAgICAgICAgIGlmICh0eXBlb2YgZXhwb3J0cyAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGNvbnN0IGN1cnJlbnRFeHBvcnQgPSBleHBvcnRzLmdldChrZXkpO1xuICAgICAgICAgICAgaWYgKHR5cGVvZiBjdXJyZW50RXhwb3J0ICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgICBjdXJyZW50RXhwb3J0LndoZXJlVXNlZC5kZWxldGUoZmlsZSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgcmV0dXJuIHtcbiAgICAgICdQcm9ncmFtOmV4aXQnOiBub2RlID0+IHtcbiAgICAgICAgdXBkYXRlRXhwb3J0VXNhZ2Uobm9kZSk7XG4gICAgICAgIHVwZGF0ZUltcG9ydFVzYWdlKG5vZGUpO1xuICAgICAgICBjaGVja0V4cG9ydFByZXNlbmNlKG5vZGUpO1xuICAgICAgfSxcbiAgICAgICdFeHBvcnREZWZhdWx0RGVjbGFyYXRpb24nOiBub2RlID0+IHtcbiAgICAgICAgY2hlY2tVc2FnZShub2RlLCBJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpO1xuICAgICAgfSxcbiAgICAgICdFeHBvcnROYW1lZERlY2xhcmF0aW9uJzogbm9kZSA9PiB7XG4gICAgICAgIG5vZGUuc3BlY2lmaWVycy5mb3JFYWNoKHNwZWNpZmllciA9PiB7XG4gICAgICAgICAgY2hlY2tVc2FnZShub2RlLCBzcGVjaWZpZXIuZXhwb3J0ZWQubmFtZSB8fCBzcGVjaWZpZXIuZXhwb3J0ZWQudmFsdWUpO1xuICAgICAgICB9KTtcbiAgICAgICAgZm9yRWFjaERlY2xhcmF0aW9uSWRlbnRpZmllcihub2RlLmRlY2xhcmF0aW9uLCAobmFtZSkgPT4ge1xuICAgICAgICAgIGNoZWNrVXNhZ2Uobm9kZSwgbmFtZSk7XG4gICAgICAgIH0pO1xuICAgICAgfSxcbiAgICB9O1xuICB9LFxufTtcbiJdfQ==