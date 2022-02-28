"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.BreadcrumbItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _angleRightIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-right-icon"));

var _breadcrumb = _interopRequireDefault(require("@patternfly/react-styles/css/components/Breadcrumb/breadcrumb"));

var _reactStyles = require("@patternfly/react-styles");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var BreadcrumbItem = function BreadcrumbItem(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$to = _ref.to,
      to = _ref$to === void 0 ? null : _ref$to,
      _ref$isActive = _ref.isActive,
      isActive = _ref$isActive === void 0 ? false : _ref$isActive,
      _ref$target = _ref.target,
      target = _ref$target === void 0 ? null : _ref$target,
      _ref$component = _ref.component,
      component = _ref$component === void 0 ? 'a' : _ref$component,
      props = _objectWithoutProperties(_ref, ["children", "className", "to", "isActive", "target", "component"]);

  var Component = component;
  return React.createElement("li", _extends({}, props, {
    className: (0, _reactStyles.css)(_breadcrumb["default"].breadcrumbItem, className)
  }), to && React.createElement(Component, {
    href: to,
    target: target,
    className: (0, _reactStyles.css)(_breadcrumb["default"].breadcrumbLink, isActive ? (0, _reactStyles.getModifier)(_breadcrumb["default"], 'current') : ''),
    "aria-current": isActive ? 'page' : undefined
  }, children), !to && React.createElement(React.Fragment, null, children), !isActive && React.createElement("span", {
    className: (0, _reactStyles.css)(_breadcrumb["default"].breadcrumbItemDivider)
  }, React.createElement(_angleRightIcon["default"], null)));
};

exports.BreadcrumbItem = BreadcrumbItem;
BreadcrumbItem.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  to: _propTypes["default"].string,
  isActive: _propTypes["default"].bool,
  target: _propTypes["default"].string,
  component: _propTypes["default"].node
};
//# sourceMappingURL=BreadcrumbItem.js.map