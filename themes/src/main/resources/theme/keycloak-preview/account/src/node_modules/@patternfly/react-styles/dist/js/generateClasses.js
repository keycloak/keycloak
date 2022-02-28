"use strict";

function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance"); }

function _iterableToArray(iter) { if (Symbol.iterator in Object(iter) || Object.prototype.toString.call(iter) === "[object Arguments]") return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = new Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } }

var camelcase = require('camel-case');

var glob = require('glob');

var _require = require('path'),
    dirname = _require.dirname,
    basename = _require.basename,
    resolve = _require.resolve,
    join = _require.join,
    parse = _require.parse;

var _require2 = require('fs'),
    readFileSync = _require2.readFileSync;

var _require3 = require('fs-extra'),
    outputFileSync = _require3.outputFileSync;

var outDir = resolve(__dirname, '../css');
var pfStylesDir = dirname(require.resolve('@patternfly/patternfly/patternfly.css'));
var cssFiles = glob.sync('**/*.css', {
  cwd: pfStylesDir,
  ignore: ['assets/**', '*ie11*.css', '*.css']
});
/* Copy @patternfly/patternfly styles */

cssFiles.forEach(function (filePath) {
  var absFilePath = resolve(pfStylesDir, filePath);
  var cssContent = readFileSync(absFilePath, 'utf8');
  var cssOutputPath = getCSSOutputPath(outDir, filePath);
  var newClass = cssToJSNew(cssContent, "./".concat(basename(cssOutputPath)));
  outputFileSync(cssOutputPath, cssContent);
  outputFileSync(cssOutputPath.replace('.css', '.ts'), newClass);
});
/* Copy inline styles in the src/css folder */

var inlineCssFiles = glob.sync('src/css/**/*.css');
inlineCssFiles.forEach(function (filePath) {
  var absFilePath = resolve(filePath);
  var cssContent = readFileSync(absFilePath, 'utf8');
  var cssOutputPath = getCSSOutputPath(outDir, filePath).replace('src/css/', '');
  var newClass = cssToJSNew(cssContent, "./".concat(basename(cssOutputPath)));
  outputFileSync(cssOutputPath, cssContent);
  outputFileSync(cssOutputPath.replace('.css', '.ts'), newClass);
});
/**
 * @param {string} cssString - CSS string
 * @param {string} cssOutputPath - Path string
 */

function cssToJSNew(cssString) {
  var cssOutputPath = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : '';
  var cssClasses = getCSSClasses(cssString); // eslint-disable-next-line no-undef

  var distinctValues = _toConsumableArray(new Set(cssClasses));

  var classDeclaration = [];
  var modifiersDeclaration = [];
  distinctValues.forEach(function (className) {
    var key = formatClassName(className);
    var cleanClass = className.replace('.', '').trim();

    if (isModifier(className)) {
      modifiersDeclaration.push("'".concat(key, "': '").concat(cleanClass, "'"));
    } else {
      classDeclaration.push("".concat(key, ": '").concat(cleanClass, "'"));
    }
  });
  var classSection = classDeclaration.length > 0 ? "".concat(classDeclaration.join(',\n  '), ",") : '';
  return "import '".concat(cssOutputPath, "';\n\nexport default {\n  ").concat(classSection, "\n  modifiers: {\n    ").concat(modifiersDeclaration.join(',\n    '), "\n  }\n}");
}
/**
 * @param {string} cssString - CSS string
 */


function getCSSClasses(cssString) {
  return cssString.match(/(\.)(?!\d)([^\s\.,{\[>+~#:)]*)(?![^{]*})/g); //eslint-disable-line
}
/**
 * @param {string} className - Class name
 */


function formatClassName(className) {
  return camelcase(className.replace(/pf-((c|l|m|u|is|has)-)?/g, ''));
}
/**
 * @param {string} className - Class name
 */


function isModifier(className) {
  return Boolean(className && className.startsWith) && className.startsWith('.pf-m-');
}
/**
 * @param {any} absFilePath - Absolute file path
 * @param {any} pathToCSSFile - Path to CSS file
 */


function getCSSOutputPath(absFilePath, pathToCSSFile) {
  return join(absFilePath, getFormattedCSSOutputPath(pathToCSSFile));
}
/**
 * @param {any} pathToCSSFile - Path to CSS file
 */


function getFormattedCSSOutputPath(pathToCSSFile) {
  var _parse = parse(pathToCSSFile),
      dir = _parse.dir,
      name = _parse.name;

  var formattedDir = dir;
  var nodeText = 'node_modules';
  var nodeIndex = formattedDir.lastIndexOf(nodeText);

  if (nodeIndex !== -1) {
    formattedDir = formattedDir.substring(nodeIndex + nodeText.length);
  }

  return join(formattedDir, "".concat(name, ".css"));
}
//# sourceMappingURL=generateClasses.js.map