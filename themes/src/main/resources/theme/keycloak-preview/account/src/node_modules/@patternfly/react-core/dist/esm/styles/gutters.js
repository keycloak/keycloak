import { getModifier } from '@patternfly/react-styles';
export const GutterSize = {
  sm: 'sm',
  md: 'md',
  lg: 'lg'
};
/**
 * @param {any} styleObj - Style object
 * @param {'sm' | 'md' | 'lg'} size - Size string 'sm', 'md', or 'lg'
 * @param {any} defaultValue - Default value
 */

export function getGutterModifier(styleObj, size, defaultValue) {
  return getModifier(styleObj, `gutter-${size}`, defaultValue);
}
//# sourceMappingURL=gutters.js.map