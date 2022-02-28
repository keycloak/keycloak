"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.css = css;
exports.StyleSheet = void 0;

var _emotion = require("emotion");

var _utils = require("./utils");

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var StyleSheet = {
  create: function create(styleObj) {
    var keys = Object.keys(styleObj);

    if (keys.length > 0) {
      return keys.reduce(function (prev, key) {
        return _objectSpread({}, prev, _defineProperty({}, key, (0, _emotion.css)(styleObj[key])));
      }, {});
    }

    return (0, _emotion.css)(styleObj);
  },
  parse: function parse(input) {
    var classes = (0, _utils.getCSSClasses)(input);

    if (!classes) {
      return {};
    }

    return classes.reduce(function (map, className) {
      var key = (0, _utils.formatClassName)(className);

      if (map[key]) {
        return map;
      }

      var value = (0, _utils.createStyleDeclaration)(className, input);

      if ((0, _utils.isModifier)(className)) {
        map.modifiers[key] = value;
      } else {
        map[key] = value;
      }

      return map;
    }, {
      modifiers: {},
      inject: function inject() {
        return (0, _emotion.injectGlobal)(input);
      },
      raw: input
    });
  }
};
/**
 * @param {Array} styles - Array of styles
 */

exports.StyleSheet = StyleSheet;

function css() {
  var filteredStyles = [];

  for (var _len = arguments.length, styles = new Array(_len), _key = 0; _key < _len; _key++) {
    styles[_key] = arguments[_key];
  }

  styles.forEach(function (style) {
    if ((0, _utils.isValidStyleDeclaration)(style)) {
      // remove global injection of styles in favor of require(css) in the component
      // style.__inject();
      filteredStyles.push((0, _utils.getClassName)(style));
      return;
    }

    filteredStyles.push(style);
  });
  return _emotion.cx.apply(void 0, filteredStyles);
}
//# sourceMappingURL=StyleSheet.js.map