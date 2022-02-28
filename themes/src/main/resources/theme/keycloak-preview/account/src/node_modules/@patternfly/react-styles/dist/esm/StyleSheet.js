function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import { css as emotionCSS, cx, injectGlobal } from 'emotion';
import { formatClassName, getCSSClasses, isModifier, createStyleDeclaration, isValidStyleDeclaration, getClassName } from './utils';
export const StyleSheet = {
  create(styleObj) {
    const keys = Object.keys(styleObj);

    if (keys.length > 0) {
      return keys.reduce((prev, key) => _objectSpread({}, prev, {
        [key]: emotionCSS(styleObj[key])
      }), {});
    }

    return emotionCSS(styleObj);
  },

  parse(input) {
    const classes = getCSSClasses(input);

    if (!classes) {
      return {};
    }

    return classes.reduce((map, className) => {
      const key = formatClassName(className);

      if (map[key]) {
        return map;
      }

      const value = createStyleDeclaration(className, input);

      if (isModifier(className)) {
        map.modifiers[key] = value;
      } else {
        map[key] = value;
      }

      return map;
    }, {
      modifiers: {},
      inject: () => injectGlobal(input),
      raw: input
    });
  }

};
/**
 * @param {Array} styles - Array of styles
 */

export function css(...styles) {
  const filteredStyles = [];
  styles.forEach(style => {
    if (isValidStyleDeclaration(style)) {
      // remove global injection of styles in favor of require(css) in the component
      // style.__inject();
      filteredStyles.push(getClassName(style));
      return;
    }

    filteredStyles.push(style);
  });
  return cx(...filteredStyles);
}
//# sourceMappingURL=StyleSheet.js.map