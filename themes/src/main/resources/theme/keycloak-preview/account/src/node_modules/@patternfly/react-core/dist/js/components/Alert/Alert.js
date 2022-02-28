"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Alert = exports.AlertVariant = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _alert = _interopRequireDefault(require("@patternfly/react-styles/css/components/Alert/alert"));

var _accessibility = _interopRequireDefault(require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"));

var _AlertIcon = require("./AlertIcon");

var _util = require("../../helpers/util");

var _withOuia = require("../withOuia");

var _AlertContext = require("./AlertContext");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var AlertVariant;
exports.AlertVariant = AlertVariant;

(function (AlertVariant) {
  AlertVariant["success"] = "success";
  AlertVariant["danger"] = "danger";
  AlertVariant["warning"] = "warning";
  AlertVariant["info"] = "info";
  AlertVariant["default"] = "default";
})(AlertVariant || (exports.AlertVariant = AlertVariant = {}));

var Alert = function Alert(_ref) {
  var _ref$variant = _ref.variant,
      variant = _ref$variant === void 0 ? AlertVariant.info : _ref$variant,
      _ref$isInline = _ref.isInline,
      isInline = _ref$isInline === void 0 ? false : _ref$isInline,
      _ref$isLiveRegion = _ref.isLiveRegion,
      isLiveRegion = _ref$isLiveRegion === void 0 ? false : _ref$isLiveRegion,
      _ref$variantLabel = _ref.variantLabel,
      variantLabel = _ref$variantLabel === void 0 ? "".concat((0, _util.capitalize)(variant), " alert:") : _ref$variantLabel,
      _ref$ariaLabel = _ref['aria-label'],
      ariaLabel = _ref$ariaLabel === void 0 ? "".concat((0, _util.capitalize)(variant), " Alert") : _ref$ariaLabel,
      _ref$action = _ref.action,
      action = _ref$action === void 0 ? null : _ref$action,
      title = _ref.title,
      _ref$children = _ref.children,
      children = _ref$children === void 0 ? '' : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$ouiaContext = _ref.ouiaContext,
      ouiaContext = _ref$ouiaContext === void 0 ? null : _ref$ouiaContext,
      _ref$ouiaId = _ref.ouiaId,
      ouiaId = _ref$ouiaId === void 0 ? null : _ref$ouiaId,
      props = _objectWithoutProperties(_ref, ["variant", "isInline", "isLiveRegion", "variantLabel", "aria-label", "action", "title", "children", "className", "ouiaContext", "ouiaId"]);

  var getHeadingContent = React.createElement(React.Fragment, null, React.createElement("span", {
    className: (0, _reactStyles.css)(_accessibility["default"].screenReader)
  }, variantLabel), title);
  var customClassName = (0, _reactStyles.css)(_alert["default"].alert, isInline && _alert["default"].modifiers.inline, variant !== AlertVariant["default"] && (0, _reactStyles.getModifier)(_alert["default"], variant, _alert["default"].modifiers.info), className);
  return React.createElement("div", _extends({}, props, {
    className: customClassName,
    "aria-label": ariaLabel
  }, ouiaContext.isOuia && {
    'data-ouia-component-type': 'Alert',
    'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
  }, isLiveRegion && {
    'aria-live': 'polite',
    'aria-atomic': 'false'
  }), React.createElement(_AlertIcon.AlertIcon, {
    variant: variant
  }), React.createElement("h4", {
    className: (0, _reactStyles.css)(_alert["default"].alertTitle)
  }, getHeadingContent), children && React.createElement("div", {
    className: (0, _reactStyles.css)(_alert["default"].alertDescription)
  }, children), React.createElement(_AlertContext.AlertContext.Provider, {
    value: {
      title: title,
      variantLabel: variantLabel
    }
  }, action && (_typeof(action) === 'object' || typeof action === 'string') && React.createElement("div", {
    className: (0, _reactStyles.css)(_alert["default"].alertAction)
  }, action)));
};

Alert.propTypes = {
  variant: _propTypes["default"].oneOf(['success', 'danger', 'warning', 'info', 'default']),
  isInline: _propTypes["default"].bool,
  title: _propTypes["default"].node.isRequired,
  action: _propTypes["default"].node,
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  'aria-label': _propTypes["default"].string,
  variantLabel: _propTypes["default"].string,
  isLiveRegion: _propTypes["default"].bool
};
var AlertWithOuiaContext = (0, _withOuia.withOuiaContext)(Alert);
exports.Alert = AlertWithOuiaContext;
//# sourceMappingURL=Alert.js.map