"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Grid = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _grid = _interopRequireDefault(require("@patternfly/react-styles/css/layouts/Grid/grid"));

var _reactStyles = require("@patternfly/react-styles");

var _sizes = require("../../styles/sizes");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _slicedToArray(arr, i) { return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _nonIterableRest(); }

function _nonIterableRest() { throw new TypeError("Invalid attempt to destructure non-iterable instance"); }

function _iterableToArrayLimit(arr, i) { if (!(Symbol.iterator in Object(arr) || Object.prototype.toString.call(arr) === "[object Arguments]")) { return; } var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"] != null) _i["return"](); } finally { if (_d) throw _e; } } return _arr; }

function _arrayWithHoles(arr) { if (Array.isArray(arr)) return arr; }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var Grid = function Grid(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$gutter = _ref.gutter,
      gutter = _ref$gutter === void 0 ? null : _ref$gutter,
      _ref$span = _ref.span,
      span = _ref$span === void 0 ? null : _ref$span,
      props = _objectWithoutProperties(_ref, ["children", "className", "gutter", "span"]);

  var classes = [_grid["default"].grid, span && (0, _reactStyles.getModifier)(_grid["default"], "all_".concat(span, "Col"))];
  Object.entries(_sizes.DeviceSizes).forEach(function (_ref2) {
    var _ref3 = _slicedToArray(_ref2, 2),
        propKey = _ref3[0],
        gridSpanModifier = _ref3[1];

    var key = propKey;
    var propValue = props[key];

    if (propValue) {
      classes.push((0, _reactStyles.getModifier)(_grid["default"], "all_".concat(propValue, "ColOn").concat(gridSpanModifier)));
    }

    delete props[key];
  });
  return React.createElement("div", _extends({
    className: _reactStyles.css.apply(void 0, classes.concat([gutter && _grid["default"].modifiers.gutter, className]))
  }, props), children);
};

exports.Grid = Grid;
Grid.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  gutter: _propTypes["default"].oneOf(['sm', 'md', 'lg']),
  span: _propTypes["default"].oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  sm: _propTypes["default"].oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  md: _propTypes["default"].oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  lg: _propTypes["default"].oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xl: _propTypes["default"].oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xl2: _propTypes["default"].oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])
};
//# sourceMappingURL=Grid.js.map