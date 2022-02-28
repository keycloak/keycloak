'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function _interopDefault (ex) { return (ex && (typeof ex === 'object') && 'default' in ex) ? ex['default'] : ex; }

var nodePath = _interopDefault(require('path'));
var babelUtils = require('@emotion/babel-utils');
var sourceMap = require('source-map');
var convert = _interopDefault(require('convert-source-map'));
var helperModuleImports = require('@babel/helper-module-imports');
var babelPluginMacros = require('babel-plugin-macros');
var fs = _interopDefault(require('fs'));
var findRoot = _interopDefault(require('find-root'));
var mkdirp = _interopDefault(require('mkdirp'));
var touch = require('touch');
var hashString = _interopDefault(require('@emotion/hash'));
var Stylis = _interopDefault(require('@emotion/stylis'));
var memoize = _interopDefault(require('@emotion/memoize'));

function _extends() {
  _extends = Object.assign || function (target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i];

      for (var key in source) {
        if (Object.prototype.hasOwnProperty.call(source, key)) {
          target[key] = source[key];
        }
      }
    }

    return target;
  };

  return _extends.apply(this, arguments);
}

function cloneNode(t, node) {
  return (typeof t.cloneNode === 'function' ? t.cloneNode : t.cloneDeep)(node);
}

function getRuntimeImportPath(path, t) {
  // $FlowFixMe
  var binding = path.scope.getBinding(path.node.name);

  if (!t.isImportDeclaration(binding.path.parentPath)) {
    throw binding.path.buildCodeFrameError('the emotion macro must be imported with es modules');
  }

  var importPath = binding.path.parentPath.node.source.value;
  return importPath.match(/(.*)\/macro/)[1];
}
function buildMacroRuntimeNode(path, state, importName, t) {
  var runtimeImportPath = getRuntimeImportPath(path, t);
  if (state.emotionImports === undefined) state.emotionImports = {};

  if (state.emotionImports[runtimeImportPath] === undefined) {
    state.emotionImports[runtimeImportPath] = {};
  }

  if (state.emotionImports[runtimeImportPath][importName] === undefined) {
    // $FlowFixMe
    state.emotionImports[runtimeImportPath][importName] = path.scope.generateUidIdentifier(path.node.name);
  } // $FlowFixMe


  return cloneNode(t, state.emotionImports[runtimeImportPath][importName]);
}
function addRuntimeImports(state, t) {
  if (state.emotionImports) {
    var emotionImports = state.emotionImports;
    Object.keys(emotionImports).forEach(function (importPath) {
      var importSpecifiers = [];
      Object.keys(emotionImports[importPath]).forEach(function (importName) {
        var identifier = emotionImports[importPath][importName];

        if (importName === 'default') {
          importSpecifiers.push(t.importDefaultSpecifier(identifier));
        } else {
          importSpecifiers.push(t.importSpecifier(identifier, t.identifier(importName)));
        }
      }); // $FlowFixMe

      state.file.path.node.body.unshift(t.importDeclaration(importSpecifiers, t.stringLiteral(importPath)));
    });
    state.emotionImports = undefined;
  }
}
function getName(identifierName, prefix) {
  var parts = [];
  parts.push(prefix);

  if (identifierName) {
    parts.push(identifierName);
  }

  return parts.join('-');
}
function getLabel(identifierName, autoLabel, labelFormat, filename) {
  if (!identifierName || !autoLabel) return null; // Valid Characters in CSS Class Names Selecter
  // https://stackoverflow.com/questions/448981/which-characters-are-valid-in-css-class-names-selectors#449000

  var normalizedName = identifierName.replace(/[^\w-]/g, '');
  if (!labelFormat) return normalizedName;
  var parsedPath = nodePath.parse(filename);
  var normalizedFilename = parsedPath.name.replace('.', '-').replace(/[^\w-]/g, '');
  return labelFormat.replace(/\[local\]/gi, normalizedName).replace(/\[filename\]/gi, normalizedFilename);
}
function createRawStringFromTemplateLiteral(quasi) {
  var strs = quasi.quasis.map(function (x) {
    return x.value.cooked;
  });
  var hash = hashArray(strs.concat());
  var src = strs.reduce(function (arr, str, i) {
    arr.push(str);

    if (i !== strs.length - 1) {
      arr.push("xxx" + i + "xxx");
    }

    return arr;
  }, []).join('').trim();
  return {
    src: src,
    hash: hash
  };
}
function omit(obj, testFn) {
  var target = {};
  var i;

  for (i in obj) {
    if (!testFn(i, obj)) continue;
    if (!Object.prototype.hasOwnProperty.call(obj, i)) continue;
    target[i] = obj[i];
  }

  return target;
}

function getGeneratorOpts(file) {
  return file.opts.generatorOpts ? file.opts.generatorOpts : file.opts;
}

function makeSourceMapGenerator(file) {
  var generatorOpts = getGeneratorOpts(file);
  var filename = generatorOpts.sourceFileName;
  var generator = new sourceMap.SourceMapGenerator({
    file: filename,
    sourceRoot: generatorOpts.sourceRoot
  });
  generator.setSourceContent(filename, file.code);
  return generator;
}
function addSourceMaps(offset, state) {
  var generator = makeSourceMapGenerator(state.file);
  var generatorOpts = getGeneratorOpts(state.file);
  generator.addMapping({
    generated: {
      line: 1,
      column: 0
    },
    source: generatorOpts.sourceFileName,
    original: offset
  });
  return convert.fromObject(generator).toComment({
    multiline: true
  });
}

function cssProps (path, state, t) {
  var cssPath;
  var classNamesPath;
  path.get('attributes').forEach(function (openElPath) {
    if (t.isJSXSpreadAttribute(openElPath.node)) {
      return;
    }

    var attrPath = openElPath.get('name');
    var name = attrPath.node.name;

    if (name === state.importedNames.css) {
      cssPath = attrPath;
    }

    if (name === 'className') {
      classNamesPath = attrPath;
    }
  });
  if (!cssPath) return;
  var cssPropValue = cssPath.container && cssPath.container.value;
  var classNamesValue = classNamesPath && classNamesPath.container && classNamesPath.container.value;

  if (t.isJSXExpressionContainer(cssPropValue)) {
    cssPropValue = cssPropValue.expression;
  }

  var cssTemplateExpression;

  if (t.isTemplateLiteral(cssPropValue)) {
    cssTemplateExpression = createCssTemplateExpression(cssPropValue);
  } else if (t.isStringLiteral(cssPropValue)) {
    cssTemplateExpression = createCssTemplateExpression(t.templateLiteral([t.templateElement({
      raw: cssPropValue.value,
      cooked: cssPropValue.value
    })], []));
  } else {
    var args = state.opts.sourceMap ? [cssPropValue, t.stringLiteral(addSourceMaps(cssPath.node.loc.start, state))] : [cssPropValue];
    cssTemplateExpression = t.callExpression(getCssIdentifer(), args);
  }

  if (!classNamesValue || t.isStringLiteral(classNamesValue) && !classNamesValue.value) {
    if (classNamesPath) classNamesPath.parentPath.remove();
    cssPath.parentPath.replaceWith(createClassNameAttr(cssTemplateExpression));
    return;
  }

  cssPath.parentPath.remove();

  if (classNamesPath && classNamesPath.parentPath) {
    if (t.isJSXExpressionContainer(classNamesValue)) {
      var _args = [add(cssTemplateExpression, add(t.stringLiteral(' '), classNamesValue.expression))];

      if (state.opts.sourceMap) {
        _args.push(t.stringLiteral(addSourceMaps(cssPath.node.loc.start, state)));
      }

      classNamesPath.parentPath.replaceWith(createClassNameAttr(t.callExpression(getMergeIdentifier(), _args)));
    } else {
      classNamesPath.parentPath.replaceWith(createClassNameAttr(add(cssTemplateExpression, t.stringLiteral(" " + (classNamesValue.value || '')))));
    }
  }

  function add(a, b) {
    return t.binaryExpression('+', a, b);
  }

  function createClassNameAttr(expression) {
    return t.jSXAttribute(t.jSXIdentifier('className'), t.jSXExpressionContainer(expression));
  }

  function getCssIdentifer() {
    if (state.opts.autoImportCssProp !== false) {
      var cssImport = helperModuleImports.addNamed(path, 'css', state.emotionImportPath);
      state.cssPropIdentifiers.push(cssImport);
      return cssImport;
    } else {
      return t.identifier(state.importedNames.css);
    }
  }

  function getMergeIdentifier() {
    if (state.opts.autoImportCssProp !== false) {
      return helperModuleImports.addNamed(path, 'merge', state.emotionImportPath);
    } else {
      return t.identifier(state.importedNames.merge);
    }
  }

  function createCssTemplateExpression(templateLiteral) {
    return t.taggedTemplateExpression(getCssIdentifer(), templateLiteral);
  }
}

var emotionMacro = babelPluginMacros.createMacro(macro);

function macro(_ref) {
  var references = _ref.references,
      state = _ref.state,
      t = _ref.babel.types;
  Object.keys(references).forEach(function (referenceKey) {
    var isPure = true;

    switch (referenceKey) {
      case 'injectGlobal':
        {
          isPure = false;
        }
      // eslint-disable-next-line no-fallthrough

      case 'css':
      case 'keyframes':
        {
          references[referenceKey].reverse().forEach(function (reference) {
            var path = reference.parentPath;
            var runtimeNode = buildMacroRuntimeNode(reference, state, referenceKey, t);

            if (t.isTaggedTemplateExpression(path)) {
              replaceCssWithCallExpression(path, runtimeNode, state, t, undefined, !isPure);
            } else {
              if (isPure) {
                path.addComment('leading', '#__PURE__');
              }

              reference.replaceWith(runtimeNode);
            }
          });
          break;
        }

      default:
        {
          references[referenceKey].reverse().forEach(function (reference) {
            reference.replaceWith(buildMacroRuntimeNode(reference, state, referenceKey, t));
          });
        }
    }
  });
  addRuntimeImports(state, t);
}

var styledMacro = babelPluginMacros.createMacro(macro$1);

function macro$1(options) {
  var references = options.references,
      state = options.state,
      t = options.babel.types;
  var referencesWithoutDefault = references;

  if (references.default) {
    referencesWithoutDefault = omit(references, function (key) {
      return key !== 'default';
    });
    references.default.reverse().forEach(function (styledReference) {
      var path = styledReference.parentPath.parentPath;
      var runtimeNode = buildMacroRuntimeNode(styledReference, state, 'default', t);

      if (t.isTemplateLiteral(path.node.quasi)) {
        if (t.isMemberExpression(path.node.tag)) {
          path.replaceWith(buildStyledCallExpression(runtimeNode, [t.stringLiteral(path.node.tag.property.name)], path, state, false, t));
        } else if (t.isCallExpression(path.node.tag)) {
          path.replaceWith(buildStyledCallExpression(runtimeNode, path.node.tag.arguments, path, state, true, t));
        }
      } else if (t.isCallExpression(path) && (t.isCallExpression(path.node.callee) || t.isIdentifier(path.node.callee.object))) {
        path.replaceWith(buildStyledObjectCallExpression(path, state, runtimeNode, t));
      }
    });
  }

  emotionMacro(_extends({}, options, {
    references: referencesWithoutDefault
  }));
}

var macros = {
  emotion: emotionMacro,
  styled: styledMacro
};
function hashArray(arr) {
  return hashString(arr.join(''));
}
var staticStylis = new Stylis({
  keyframe: false
});
function hoistPureArgs(path) {
  var args = path.get('arguments');

  if (args && Array.isArray(args)) {
    args.forEach(function (arg) {
      if (!arg.isIdentifier() && arg.isPure()) {
        arg.hoist();
      }
    });
  }
}
function replaceCssWithCallExpression(path, identifier, state, t, staticCSSSrcCreator, removePath, staticCSSSelectorCreator) {
  if (staticCSSSrcCreator === void 0) {
    staticCSSSrcCreator = function staticCSSSrcCreator(src) {
      return src;
    };
  }

  if (removePath === void 0) {
    removePath = false;
  }

  if (staticCSSSelectorCreator === void 0) {
    staticCSSSelectorCreator = function staticCSSSelectorCreator(name, hash) {
      return "." + name + "-" + hash;
    };
  }

  try {
    var _createRawStringFromT = createRawStringFromTemplateLiteral(path.node.quasi),
        _hash = _createRawStringFromT.hash,
        _src = _createRawStringFromT.src;

    var identifierName = babelUtils.getLabelFromPath(path, t);

    var _name = getName(identifierName, 'css');

    if (state.extractStatic && !path.node.quasi.expressions.length) {
      var staticCSSRules = staticStylis(staticCSSSelectorCreator(_name, _hash), staticCSSSrcCreator(_src, _name, _hash));
      state.insertStaticRules([staticCSSRules]);

      if (!removePath) {
        return path.replaceWith(t.stringLiteral(_name + "-" + _hash));
      }

      return path.replaceWith(t.identifier('undefined'));
    }

    if (!removePath) {
      path.addComment('leading', '#__PURE__');
    }

    var stringToAppend = '';

    if (state.opts.sourceMap === true && path.node.quasi.loc !== undefined) {
      stringToAppend += addSourceMaps(path.node.quasi.loc.start, state);
    }

    var label = getLabel(identifierName, state.opts.autoLabel, state.opts.labelFormat, state.file.opts.filename);

    if (label) {
      stringToAppend += "label:" + label + ";";
    }

    path.replaceWith(t.callExpression(identifier, babelUtils.appendStringToExpressions(babelUtils.getExpressionsFromTemplateLiteral(path.node.quasi, t), stringToAppend, t)));

    if (state.opts.hoist) {
      hoistPureArgs(path);
    }

    return;
  } catch (e) {
    if (path) {
      throw path.buildCodeFrameError(e);
    }

    throw e;
  }
}
var unsafeRequire = require;
var getPackageRootPath = memoize(function (filename) {
  return findRoot(filename);
});

function buildTargetObjectProperty(path, state, t) {
  if (state.count === undefined) {
    state.count = 0;
  }

  var filename = state.file.opts.filename; // normalize the file path to ignore folder structure
  // outside the current node project and arch-specific delimiters

  var moduleName = '';
  var rootPath = filename;

  try {
    rootPath = getPackageRootPath(filename);
    moduleName = unsafeRequire(rootPath + '/package.json').name;
  } catch (err) {}

  var finalPath = filename === rootPath ? '' : filename.slice(rootPath.length);
  var positionInFile = state.count++;
  var stuffToHash = [moduleName];

  if (finalPath) {
    stuffToHash.push(nodePath.normalize(finalPath));
  } else {
    stuffToHash.push(state.file.code);
  }

  var stableClassName = "e" + hashArray(stuffToHash) + positionInFile;
  return t.objectProperty(t.identifier('target'), t.stringLiteral(stableClassName));
}

var buildFinalOptions = function buildFinalOptions(t, options) {
  var existingProperties = [];

  if (options && !t.isObjectExpression(options)) {
    console.warn("Second argument to a styled call is not an object, it's going to be removed.");
  } else if (options) {
    // $FlowFixMe
    existingProperties = options.properties;
  }

  for (var _len = arguments.length, newProps = new Array(_len > 2 ? _len - 2 : 0), _key = 2; _key < _len; _key++) {
    newProps[_key - 2] = arguments[_key];
  }

  return t.objectExpression(existingProperties.concat(newProps.filter(Boolean)));
};

function buildStyledCallExpression(identifier, args, path, state, isCallExpression, t) {
  // unpacking "manually" to prevent array out of bounds access (deopt)
  var tag = args[0];
  var options = args.length >= 2 ? args[1] : null;
  var restArgs = args.slice(2);
  var identifierName = babelUtils.getLabelFromPath(path, t);
  var targetProperty = buildTargetObjectProperty(path, state, t);

  if (state.extractStatic && !path.node.quasi.expressions.length) {
    var _createRawStringFromT2 = createRawStringFromTemplateLiteral(path.node.quasi),
        _hash2 = _createRawStringFromT2.hash,
        _src2 = _createRawStringFromT2.src;

    var staticClassName = "css-" + _hash2;
    var staticCSSRules = staticStylis("." + staticClassName, _src2);
    state.insertStaticRules([staticCSSRules]);

    var _finalOptions = buildFinalOptions(t, options, t.objectProperty(t.identifier('e'), t.stringLiteral(staticClassName)), targetProperty);

    return t.callExpression( // $FlowFixMe
    t.callExpression(identifier, [tag, _finalOptions].concat(restArgs)), []);
  }

  path.addComment('leading', '#__PURE__');
  var stringToAppend = '';

  if (state.opts.sourceMap === true && path.node.quasi.loc !== undefined) {
    stringToAppend += addSourceMaps(path.node.quasi.loc.start, state);
  }

  var labelProperty;
  var label = getLabel(identifierName, state.opts.autoLabel, state.opts.labelFormat, state.file.opts.filename);

  if (label) {
    labelProperty = t.objectProperty(t.identifier('label'), t.stringLiteral(label));
  }

  var finalOptions = buildFinalOptions(t, options, labelProperty, targetProperty);
  var styledCall = t.isStringLiteral(tag) && !isCallExpression && // $FlowFixMe
  tag.value[0] !== tag.value[0].toLowerCase() ? // $FlowFixMe
  t.memberExpression(identifier, t.identifier(tag.value)) : // $FlowFixMe
  t.callExpression(identifier, [tag, finalOptions].concat(restArgs));
  return t.callExpression(styledCall, babelUtils.appendStringToExpressions(babelUtils.getExpressionsFromTemplateLiteral(path.node.quasi, t), stringToAppend, t));
}
function buildStyledObjectCallExpression(path, state, identifier, t) {
  var targetProperty = buildTargetObjectProperty(path, state, t);
  var identifierName = babelUtils.getLabelFromPath(path, t);
  var tag = t.isCallExpression(path.node.callee) ? path.node.callee.arguments[0] : t.stringLiteral(path.node.callee.property.name);
  var isCallExpression = t.isCallExpression(path.node.callee);
  var styledOptions = null;
  var restStyledArgs = [];

  if (t.isCallExpression(path.node.callee)) {
    var styledArgs = path.node.callee.arguments;

    if (styledArgs.length >= 2) {
      styledOptions = styledArgs[1];
    }

    restStyledArgs = styledArgs.slice(2);
  }

  var args = path.node.arguments;

  if (state.opts.sourceMap === true && path.node.loc !== undefined) {
    args.push(t.stringLiteral(addSourceMaps(path.node.loc.start, state)));
  }

  var label = getLabel(identifierName, state.opts.autoLabel, state.opts.labelFormat, state.file.opts.filename);
  var labelProperty = label ? t.objectProperty(t.identifier('label'), t.stringLiteral(label)) : null;
  path.addComment('leading', '#__PURE__');
  var styledCall = t.isStringLiteral(tag) && !isCallExpression && tag.value[0] !== tag.value[0].toLowerCase() ? t.memberExpression(identifier, t.identifier(tag.value)) : t.callExpression(identifier, [tag, buildFinalOptions(t, styledOptions, targetProperty, labelProperty)].concat(restStyledArgs));
  return t.callExpression(styledCall, args);
}
var visited = Symbol('visited');
var defaultImportedNames = {
  styled: 'styled',
  css: 'css',
  keyframes: 'keyframes',
  injectGlobal: 'injectGlobal',
  merge: 'merge'
};
var importedNameKeys = Object.keys(defaultImportedNames).map(function (key) {
  return key === 'styled' ? 'default' : key;
});
var defaultEmotionPaths = ['emotion', 'react-emotion', 'preact-emotion', '@emotion/primitives'];

function getRelativePath(filepath, absoluteInstancePath) {
  var relativePath = nodePath.relative(nodePath.dirname(filepath), absoluteInstancePath);
  return relativePath.charAt(0) === '.' ? relativePath : "./" + relativePath;
}

function getAbsolutePath(instancePath, rootPath) {
  if (instancePath.charAt(0) === '.') {
    var absoluteInstancePath = nodePath.resolve(rootPath, instancePath);
    return absoluteInstancePath;
  }

  return false;
}

function getInstancePathToImport(instancePath, filepath) {
  var absolutePath = getAbsolutePath(instancePath, process.cwd());

  if (absolutePath === false) {
    return instancePath;
  }

  return getRelativePath(filepath, absolutePath);
}

function getInstancePathToCompare(instancePath, rootPath) {
  var absolutePath = getAbsolutePath(instancePath, rootPath);

  if (absolutePath === false) {
    return instancePath;
  }

  return absolutePath;
}

var warnedAboutExtractStatic = false;
function index (babel) {
  var t = babel.types;
  return {
    name: 'emotion',
    // not required
    inherits: require('babel-plugin-syntax-jsx'),
    visitor: {
      Program: {
        enter: function enter(path, state) {
          var hasFilepath = path.hub.file.opts.filename && path.hub.file.opts.filename !== 'unknown';
          state.emotionImportPath = 'emotion';

          if (state.opts.primaryInstance !== undefined) {
            state.emotionImportPath = getInstancePathToImport(state.opts.primaryInstance, path.hub.file.opts.filename);
          }

          state.importedNames = _extends({}, defaultImportedNames, state.opts.importedNames);
          var imports = [];
          var isModule = false;

          for (var _iterator = path.node.body, _isArray = Array.isArray(_iterator), _i = 0, _iterator = _isArray ? _iterator : _iterator[Symbol.iterator]();;) {
            var _ref;

            if (_isArray) {
              if (_i >= _iterator.length) break;
              _ref = _iterator[_i++];
            } else {
              _i = _iterator.next();
              if (_i.done) break;
              _ref = _i.value;
            }

            var node = _ref;

            if (t.isModuleDeclaration(node)) {
              isModule = true;
              break;
            }
          }

          if (isModule) {
            path.traverse({
              ImportDeclaration: {
                exit: function exit(path) {
                  var node = path.node;
                  var imported = [];
                  var specifiers = [];
                  imports.push({
                    source: node.source.value,
                    imported: imported,
                    specifiers: specifiers
                  });

                  for (var _iterator2 = path.get('specifiers'), _isArray2 = Array.isArray(_iterator2), _i2 = 0, _iterator2 = _isArray2 ? _iterator2 : _iterator2[Symbol.iterator]();;) {
                    var _ref2;

                    if (_isArray2) {
                      if (_i2 >= _iterator2.length) break;
                      _ref2 = _iterator2[_i2++];
                    } else {
                      _i2 = _iterator2.next();
                      if (_i2.done) break;
                      _ref2 = _i2.value;
                    }

                    var specifier = _ref2;
                    var local = specifier.node.local.name;

                    if (specifier.isImportDefaultSpecifier()) {
                      imported.push('default');
                      specifiers.push({
                        kind: 'named',
                        imported: 'default',
                        local: local
                      });
                    }

                    if (specifier.isImportSpecifier()) {
                      var importedName = specifier.node.imported.name;
                      imported.push(importedName);
                      specifiers.push({
                        kind: 'named',
                        imported: importedName,
                        local: local
                      });
                    }
                  }
                }
              }
            });
          }

          var emotionPaths = defaultEmotionPaths.concat((state.opts.instances || []).map(function (instancePath) {
            return getInstancePathToCompare(instancePath, process.cwd());
          }));
          var dirname = hasFilepath ? nodePath.dirname(path.hub.file.opts.filename) : '';
          imports.forEach(function (_ref3) {
            var source = _ref3.source,
                imported = _ref3.imported,
                specifiers = _ref3.specifiers;

            if (emotionPaths.indexOf(getInstancePathToCompare(source, dirname)) !== -1) {
              var importedNames = specifiers.filter(function (v) {
                return importedNameKeys.indexOf(v.imported) !== -1;
              }).reduce(function (acc, _ref4) {
                var _extends2;

                var imported = _ref4.imported,
                    local = _ref4.local;
                return _extends({}, acc, (_extends2 = {}, _extends2[imported === 'default' ? 'styled' : imported] = local, _extends2));
              }, defaultImportedNames);
              state.importedNames = _extends({}, importedNames, state.opts.importedNames);
            }
          });
          state.cssPropIdentifiers = [];

          if (state.opts.extractStatic && !warnedAboutExtractStatic) {
            console.warn('extractStatic is deprecated and will be removed in emotion@10. We recommend disabling extractStatic or using other libraries like linaria or css-literal-loader'); // lots of cli tools write to the same line so
            // this moves to the next line so the warning doesn't get removed

            console.log('');
            warnedAboutExtractStatic = true;
          }

          state.extractStatic = // path.hub.file.opts.filename !== 'unknown' ||
          state.opts.extractStatic;
          state.staticRules = [];

          state.insertStaticRules = function (staticRules) {
            var _state$staticRules;

            (_state$staticRules = state.staticRules).push.apply(_state$staticRules, staticRules);
          };
        },
        exit: function exit(path, state) {
          if (state.staticRules.length !== 0) {
            var toWrite = state.staticRules.join('\n').trim();
            var cssFilename = path.hub.file.opts.generatorOpts ? path.hub.file.opts.generatorOpts.sourceFileName : path.hub.file.opts.sourceFileName;
            var cssFileOnDisk;
            var importPath;
            var cssFilenameArr = cssFilename.split('.'); // remove the extension

            cssFilenameArr.pop(); // add emotion.css as an extension

            cssFilenameArr.push('emotion.css');
            cssFilename = cssFilenameArr.join('.');

            if (state.opts.outputDir) {
              var relativeToSourceDir = nodePath.relative(nodePath.dirname(cssFilename), state.opts.outputDir);
              importPath = nodePath.join(relativeToSourceDir, cssFilename);
              cssFileOnDisk = nodePath.resolve(cssFilename, '..', importPath);
            } else {
              importPath = "./" + nodePath.basename(cssFilename);
              cssFileOnDisk = nodePath.resolve(cssFilename);
            }

            var exists = fs.existsSync(cssFileOnDisk);
            helperModuleImports.addSideEffect(path, importPath);

            if (exists ? fs.readFileSync(cssFileOnDisk, 'utf8') !== toWrite : true) {
              if (!exists) {
                if (state.opts.outputDir) {
                  mkdirp.sync(nodePath.dirname(cssFileOnDisk));
                }

                touch.touchSync(cssFileOnDisk);
              }

              fs.writeFileSync(cssFileOnDisk, toWrite);
            }
          }
        }
      },
      JSXOpeningElement: function JSXOpeningElement(path, state) {
        cssProps(path, state, t);

        if (state.opts.hoist) {
          path.traverse({
            CallExpression: function CallExpression(callExprPath) {
              if (callExprPath.node.callee.name === state.importedNames.css || state.cssPropIdentifiers.indexOf(callExprPath.node.callee) !== -1) {
                hoistPureArgs(callExprPath);
              }
            }
          });
        }
      },
      CallExpression: {
        enter: function enter(path, state) {
          // $FlowFixMe
          if (path[visited]) {
            return;
          }

          try {
            if (t.isIdentifier(path.node.callee)) {
              switch (path.node.callee.name) {
                case state.importedNames.css:
                case state.importedNames.keyframes:
                  {
                    path.addComment('leading', '#__PURE__');
                    var label = getLabel(babelUtils.getLabelFromPath(path, t), state.opts.autoLabel, state.opts.labelFormat, state.file.opts.filename);

                    if (label) {
                      path.node.arguments.push(t.stringLiteral("label:" + label + ";"));
                    }
                  }
                // eslint-disable-next-line no-fallthrough

                case state.importedNames.injectGlobal:
                  if (state.opts.sourceMap === true && path.node.loc !== undefined) {
                    path.node.arguments.push(t.stringLiteral(addSourceMaps(path.node.loc.start, state)));
                  }

              }
            }

            if (t.isCallExpression(path.node.callee) && path.node.callee.callee.name === state.importedNames.styled || t.isMemberExpression(path.node.callee) && t.isIdentifier(path.node.callee.object) && path.node.callee.object.name === state.importedNames.styled) {
              var identifier = t.isCallExpression(path.node.callee) ? path.node.callee.callee : path.node.callee.object;
              path.replaceWith(buildStyledObjectCallExpression(path, state, identifier, t));

              if (state.opts.hoist) {
                hoistPureArgs(path);
              }
            }
          } catch (e) {
            throw path.buildCodeFrameError(e);
          } // $FlowFixMe


          path[visited] = true;
        },
        exit: function exit(path, state) {
          try {
            if (path.node.callee && path.node.callee.property && path.node.callee.property.name === 'withComponent') {
              if (path.node.arguments.length === 1) {
                path.node.arguments.push(t.objectExpression([buildTargetObjectProperty(path, state, t)]));
              }
            }
          } catch (e) {
            throw path.buildCodeFrameError(e);
          }
        }
      },
      TaggedTemplateExpression: function TaggedTemplateExpression(path, state) {
        // $FlowFixMe
        if (path[visited]) {
          return;
        } // $FlowFixMe


        path[visited] = true;

        if ( // styled.h1`color:${color};`
        t.isMemberExpression(path.node.tag) && path.node.tag.object.name === state.importedNames.styled) {
          path.replaceWith(buildStyledCallExpression(path.node.tag.object, [t.stringLiteral(path.node.tag.property.name)], path, state, false, t));
        } else if ( // styled('h1')`color:${color};`
        t.isCallExpression(path.node.tag) && path.node.tag.callee.name === state.importedNames.styled) {
          path.replaceWith(buildStyledCallExpression(path.node.tag.callee, path.node.tag.arguments, path, state, true, t));
        } else if (t.isIdentifier(path.node.tag)) {
          if (path.node.tag.name === state.importedNames.css || state.cssPropIdentifiers.indexOf(path.node.tag) !== -1) {
            replaceCssWithCallExpression(path, path.node.tag, state, t);
          } else if (path.node.tag.name === state.importedNames.keyframes) {
            replaceCssWithCallExpression(path, path.node.tag, state, t, function (src, name, hash) {
              return "@keyframes " + name + "-" + hash + " { " + src + " }";
            }, false, function () {
              return '';
            });
          } else if (path.node.tag.name === state.importedNames.injectGlobal) {
            replaceCssWithCallExpression(path, path.node.tag, state, t, undefined, true, function () {
              return '';
            });
          }
        }
      }
    }
  };
}

exports.macros = macros;
exports.hashArray = hashArray;
exports.hoistPureArgs = hoistPureArgs;
exports.replaceCssWithCallExpression = replaceCssWithCallExpression;
exports.buildStyledCallExpression = buildStyledCallExpression;
exports.buildStyledObjectCallExpression = buildStyledObjectCallExpression;
exports.default = index;
