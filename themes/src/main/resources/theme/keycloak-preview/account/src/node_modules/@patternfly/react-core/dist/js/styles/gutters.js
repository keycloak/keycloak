"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.getGutterModifier = getGutterModifier;
exports.GutterSize = void 0;

var _reactStyles = require("@patternfly/react-styles");

var GutterSize = {
  sm: 'sm',
  md: 'md',
  lg: 'lg'
};
/**
 * @param {any} styleObj - Style object
 * @param {'sm' | 'md' | 'lg'} size - Size string 'sm', 'md', or 'lg'
 * @param {any} defaultValue - Default value
 */

exports.GutterSize = GutterSize;

function getGutterModifier(styleObj, size, defaultValue) {
  return (0, _reactStyles.getModifier)(styleObj, "gutter-".concat(size), defaultValue);
}
//# sourceMappingURL=gutters.js.map