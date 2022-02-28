"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AlertIcon = exports.variantIcons = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _alert = _interopRequireDefault(require("@patternfly/react-styles/css/components/Alert/alert"));

var _checkCircleIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/check-circle-icon"));

var _exclamationCircleIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/exclamation-circle-icon"));

var _exclamationTriangleIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon"));

var _infoCircleIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/info-circle-icon"));

var _bellIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/bell-icon"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var variantIcons = {
  success: _checkCircleIcon["default"],
  danger: _exclamationCircleIcon["default"],
  warning: _exclamationTriangleIcon["default"],
  info: _infoCircleIcon["default"],
  "default": _bellIcon["default"]
};
exports.variantIcons = variantIcons;

var AlertIcon = function AlertIcon(_ref) {
  var variant = _ref.variant,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      props = _objectWithoutProperties(_ref, ["variant", "className"]);

  var Icon = variantIcons[variant];
  return React.createElement("div", _extends({}, props, {
    className: (0, _reactStyles.css)(_alert["default"].alertIcon, className)
  }), React.createElement(Icon, null));
};

exports.AlertIcon = AlertIcon;
AlertIcon.propTypes = {
  variant: _propTypes["default"].oneOf(['success', 'danger', 'warning', 'info', 'default']).isRequired,
  className: _propTypes["default"].string
};
//# sourceMappingURL=AlertIcon.js.map