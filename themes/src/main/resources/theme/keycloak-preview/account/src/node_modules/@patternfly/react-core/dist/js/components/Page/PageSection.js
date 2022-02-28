"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.PageSection = exports.PageSectionTypes = exports.PageSectionVariants = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _page = _interopRequireDefault(require("@patternfly/react-styles/css/components/Page/page"));

var _reactStyles = require("@patternfly/react-styles");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var PageSectionVariants;
exports.PageSectionVariants = PageSectionVariants;

(function (PageSectionVariants) {
  PageSectionVariants["default"] = "default";
  PageSectionVariants["light"] = "light";
  PageSectionVariants["dark"] = "dark";
  PageSectionVariants["darker"] = "darker";
})(PageSectionVariants || (exports.PageSectionVariants = PageSectionVariants = {}));

var PageSectionTypes;
exports.PageSectionTypes = PageSectionTypes;

(function (PageSectionTypes) {
  PageSectionTypes["default"] = "default";
  PageSectionTypes["nav"] = "nav";
})(PageSectionTypes || (exports.PageSectionTypes = PageSectionTypes = {}));

var PageSection = function PageSection(_ref) {
  var _variantType, _variantStyle;

  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      children = _ref.children,
      _ref$variant = _ref.variant,
      variant = _ref$variant === void 0 ? 'default' : _ref$variant,
      _ref$type = _ref.type,
      type = _ref$type === void 0 ? 'default' : _ref$type,
      _ref$noPadding = _ref.noPadding,
      noPadding = _ref$noPadding === void 0 ? false : _ref$noPadding,
      _ref$noPaddingMobile = _ref.noPaddingMobile,
      noPaddingMobile = _ref$noPaddingMobile === void 0 ? false : _ref$noPaddingMobile,
      isFilled = _ref.isFilled,
      props = _objectWithoutProperties(_ref, ["className", "children", "variant", "type", "noPadding", "noPaddingMobile", "isFilled"]);

  var variantType = (_variantType = {}, _defineProperty(_variantType, PageSectionTypes["default"], _page["default"].pageMainSection), _defineProperty(_variantType, PageSectionTypes.nav, _page["default"].pageMainNav), _variantType);
  var variantStyle = (_variantStyle = {}, _defineProperty(_variantStyle, PageSectionVariants["default"], ''), _defineProperty(_variantStyle, PageSectionVariants.light, _page["default"].modifiers.light), _defineProperty(_variantStyle, PageSectionVariants.dark, _page["default"].modifiers.dark_200), _defineProperty(_variantStyle, PageSectionVariants.darker, _page["default"].modifiers.dark_100), _variantStyle);
  return React.createElement("section", _extends({}, props, {
    className: (0, _reactStyles.css)(variantType[type], noPadding && _page["default"].modifiers.noPadding, noPaddingMobile && _page["default"].modifiers.noPaddingMobile, variantStyle[variant], isFilled === false && _page["default"].modifiers.noFill, isFilled === true && _page["default"].modifiers.fill, className)
  }), children);
};

exports.PageSection = PageSection;
PageSection.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  variant: _propTypes["default"].oneOf(['default', 'light', 'dark', 'darker']),
  type: _propTypes["default"].oneOf(['default', 'nav']),
  isFilled: _propTypes["default"].bool,
  noPadding: _propTypes["default"].bool,
  noPaddingMobile: _propTypes["default"].bool
};
//# sourceMappingURL=PageSection.js.map