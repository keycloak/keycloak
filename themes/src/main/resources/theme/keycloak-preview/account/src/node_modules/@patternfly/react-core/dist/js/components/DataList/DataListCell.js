"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DataListCell = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _dataList = _interopRequireDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var DataListCell = function DataListCell(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$width = _ref.width,
      width = _ref$width === void 0 ? 1 : _ref$width,
      _ref$isFilled = _ref.isFilled,
      isFilled = _ref$isFilled === void 0 ? true : _ref$isFilled,
      _ref$alignRight = _ref.alignRight,
      alignRight = _ref$alignRight === void 0 ? false : _ref$alignRight,
      _ref$isIcon = _ref.isIcon,
      isIcon = _ref$isIcon === void 0 ? false : _ref$isIcon,
      props = _objectWithoutProperties(_ref, ["children", "className", "width", "isFilled", "alignRight", "isIcon"]);

  return React.createElement("div", _extends({
    className: (0, _reactStyles.css)(_dataList["default"].dataListCell, width > 1 && (0, _reactStyles.getModifier)(_dataList["default"], "flex_".concat(width), ''), !isFilled && _dataList["default"].modifiers.noFill, alignRight && _dataList["default"].modifiers.alignRight, isIcon && _dataList["default"].modifiers.icon, className)
  }, props), children);
};

exports.DataListCell = DataListCell;
DataListCell.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  width: _propTypes["default"].oneOf([1, 2, 3, 4, 5]),
  isFilled: _propTypes["default"].bool,
  alignRight: _propTypes["default"].bool,
  isIcon: _propTypes["default"].bool
};
//# sourceMappingURL=DataListCell.js.map