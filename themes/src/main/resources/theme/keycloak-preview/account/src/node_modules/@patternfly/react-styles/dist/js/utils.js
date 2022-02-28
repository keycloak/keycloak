"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.isValidStyleDeclaration = isValidStyleDeclaration;
exports.createStyleDeclaration = createStyleDeclaration;
exports.isModifier = isModifier;
exports.getModifier = getModifier;
exports.formatClassName = formatClassName;
exports.getCSSClasses = getCSSClasses;
exports.getClassName = getClassName;
exports.getInsertedStyles = getInsertedStyles;
exports.pickProperties = pickProperties;

var _camelCase = _interopRequireDefault(require("camel-case"));

var _emotion = require("emotion");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

/**
 * @param {object} styleObj - Style object
 */
function isValidStyleDeclaration(styleObj) {
  return Boolean(styleObj) && typeof styleObj.__className === 'string' && typeof styleObj.__inject === 'function';
}
/**
 * @param {string} className - Class name
 * @param {Array<Interpolation>} rawCss - raw css
 */


function createStyleDeclaration(className, rawCss) {
  return {
    __className: className.replace('.', '').trim(),
    __inject: function __inject() {
      (0, _emotion.injectGlobal)(rawCss);
    }
  };
}
/**
 * @param {string} className - class name
 */


function isModifier(className) {
  return Boolean(className && className.startsWith) && className.startsWith('.pf-m-');
}
/**
 * @param {object} styleObj - Style object
 * @param {string} modifier - Modifier string
 * @param {string} defaultModifier - Default modifier string
 */


function getModifier(styleObj, modifier, defaultModifier) {
  if (!styleObj) {
    return null;
  }

  var modifiers = styleObj.modifiers || styleObj;
  return modifiers[modifier] || modifiers[(0, _camelCase["default"])(modifier)] || defaultModifier;
}
/**
 * @param {string} className - Class name
 */


function formatClassName(className) {
  return (0, _camelCase["default"])(className.replace(/pf-((c|l|m|u|is|has)-)?/g, ''));
}
/**
 * @param {string} cssString - Css string
 */


function getCSSClasses(cssString) {
  return cssString.match(/(\.)(?!\d)([^\s\.,{\[>+~#:)]*)(?![^{]*})/g); //eslint-disable-line
}
/**
 * @param {object} styleObj - Style object
 */


function getClassName() {
  var styleObj = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};

  if (typeof styleObj === 'string') {
    return styleObj;
  }

  return isValidStyleDeclaration(styleObj) ? styleObj.__className : '';
}
/**
 *
 */


function getInsertedStyles() {
  return Object.values(_emotion.caches.inserted);
}
/**
 * @param {object} object - Object
 * @param {Array} properties - Array of properties
 */


function pickProperties(object, properties) {
  return properties.reduce(function (picked, property) {
    return _objectSpread({}, picked, _defineProperty({}, property, object[property]));
  }, {});
}
//# sourceMappingURL=utils.js.map