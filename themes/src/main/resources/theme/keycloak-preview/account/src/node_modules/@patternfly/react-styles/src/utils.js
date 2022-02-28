import camelcase from 'camel-case';
import { injectGlobal, caches as emotionCache } from 'emotion';
import { Interpolation } from 'create-emotion';

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
  return properties.reduce((picked, property) => ({ ...picked, [property]: object[property] }), {});
}
