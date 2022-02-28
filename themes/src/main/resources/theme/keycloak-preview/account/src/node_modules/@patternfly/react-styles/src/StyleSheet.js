import { css as emotionCSS, cx, injectGlobal } from 'emotion';
import {
  formatClassName,
  getCSSClasses,
  isModifier,
  createStyleDeclaration,
  isValidStyleDeclaration,
  getClassName
} from './utils';

export const StyleSheet = {
  create(styleObj) {
    const keys = Object.keys(styleObj);
    if (keys.length > 0) {
      return keys.reduce(
        (prev, key) => ({
          ...prev,
          [key]: emotionCSS(styleObj[key])
        }),
        {}
      );
    }
    return emotionCSS(styleObj);
  },
  parse(input) {
    const classes = getCSSClasses(input);
    if (!classes) {
      return {};
    }

    return classes.reduce(
      (map, className) => {
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
      },
      {
        modifiers: {},
        inject: () => injectGlobal(input),
        raw: input
      }
    );
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
