"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Title = exports.TitleLevel = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _title = _interopRequireDefault(require("@patternfly/react-styles/css/components/Title/title"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var TitleLevel;
exports.TitleLevel = TitleLevel;

(function (TitleLevel) {
  TitleLevel["h1"] = "h1";
  TitleLevel["h2"] = "h2";
  TitleLevel["h3"] = "h3";
  TitleLevel["h4"] = "h4";
  TitleLevel["h5"] = "h5";
  TitleLevel["h6"] = "h6";
})(TitleLevel || (exports.TitleLevel = TitleLevel = {}));

var Title = function Title(_ref) {
  var size = _ref.size,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$children = _ref.children,
      children = _ref$children === void 0 ? '' : _ref$children,
      _ref$headingLevel = _ref.headingLevel,
      HeadingLevel = _ref$headingLevel === void 0 ? 'h1' : _ref$headingLevel,
      props = _objectWithoutProperties(_ref, ["size", "className", "children", "headingLevel"]);

  return React.createElement(HeadingLevel, _extends({}, props, {
    className: (0, _reactStyles.css)(_title["default"].title, (0, _reactStyles.getModifier)(_title["default"], size), className)
  }), children);
};

exports.Title = Title;
Title.propTypes = {
  size: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf(['xs']), _propTypes["default"].oneOf(['sm']), _propTypes["default"].oneOf(['md']), _propTypes["default"].oneOf(['lg']), _propTypes["default"].oneOf(['xl']), _propTypes["default"].oneOf(['2xl']), _propTypes["default"].oneOf(['3xl']), _propTypes["default"].oneOf(['4xl'])]).isRequired,
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  headingLevel: _propTypes["default"].oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6'])
};
//# sourceMappingURL=Title.js.map