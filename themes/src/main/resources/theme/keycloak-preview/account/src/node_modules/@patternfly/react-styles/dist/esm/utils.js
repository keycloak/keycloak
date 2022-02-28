function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import camelcase from 'camel-case';
import { injectGlobal, caches as emotionCache } from 'emotion';

/**
 * @param {object} styleObj - Style object
 */
export function isValidStyleDeclaration(styleObj) {
  return Boolean(styleObj) && typeof styleObj.__className === 'string' && typeof styleObj.__inject === 'function';
}
/**
 * @param {string} className - Class name
 * @param {Array<Interpolation>} rawCss - raw css
 */

export function createStyleDeclaration(className, rawCss) {
  return {
    __className: className.replace('.', '').trim(),

    __inject() {
      injectGlobal(rawCss);
    }

  };
}
/**
 * @param {string} className - class name
 */

export function isModifier(className) {
  return Boolean(className && className.startsWith) && className.startsWith('.pf-m-');
}
/**
 * @param {object} styleObj - Style object
 * @param {string} modifier - Modifier string
 * @param {string} defaultModifier - Default modifier string
 */

export function getModifier(styleObj, modifier, defaultModifier) {
  if (!styleObj) {
    return null;
  }

  const modifiers = styleObj.modifiers || styleObj;
  return modifiers[modifier] || modifiers[camelcase(modifier)] || defaultModifier;
}
/**
 * @param {string} className - Class name
 */

export function formatClassName(className) {
  return camelcase(className.replace(/pf-((c|l|m|u|is|has)-)?/g, ''));
}
/**
 * @param {string} cssString - Css string
 */

export function getCSSClasses(cssString) {
  return cssString.match(/(\.)(?!\d)([^\s\.,{\[>+~#:)]*)(?![^{]*})/g); //eslint-disable-line
}
/**
 * @param {object} styleObj - Style object
 */

export function getClassName(styleObj = {}) {
  if (typeof styleObj === 'string') {
    return styleObj;
  }

  return isValidStyleDeclaration(styleObj) ? styleObj.__className : '';
}
/**
 *
 */

export function getInsertedStyles() {
  return Object.values(emotionCache.inserted);
}
/**
 * @param {object} object - Object
 * @param {Array} properties - Array of properties
 */

export function pickProperties(object, properties) {
  return properties.reduce((picked, property) => _objectSpread({}, picked, {
    [property]: object[property]
  }), {});
}
//# sourceMappingURL=utils.js.map