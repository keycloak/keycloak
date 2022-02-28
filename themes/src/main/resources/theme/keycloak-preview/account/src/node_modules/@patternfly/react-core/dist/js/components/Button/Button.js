"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Button = exports.ButtonType = exports.ButtonVariant = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _button = _interopRequireDefault(require("@patternfly/react-styles/css/components/Button/button"));

var _reactStyles = require("@patternfly/react-styles");

var _withOuia = require("../withOuia");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var ButtonVariant;
exports.ButtonVariant = ButtonVariant;

(function (ButtonVariant) {
  ButtonVariant["primary"] = "primary";
  ButtonVariant["secondary"] = "secondary";
  ButtonVariant["tertiary"] = "tertiary";
  ButtonVariant["danger"] = "danger";
  ButtonVariant["link"] = "link";
  ButtonVariant["plain"] = "plain";
  ButtonVariant["control"] = "control";
})(ButtonVariant || (exports.ButtonVariant = ButtonVariant = {}));

var ButtonType;
exports.ButtonType = ButtonType;

(function (ButtonType) {
  ButtonType["button"] = "button";
  ButtonType["submit"] = "submit";
  ButtonType["reset"] = "reset";
})(ButtonType || (exports.ButtonType = ButtonType = {}));

var Button = function Button(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$component = _ref.component,
      component = _ref$component === void 0 ? 'button' : _ref$component,
      _ref$isActive = _ref.isActive,
      isActive = _ref$isActive === void 0 ? false : _ref$isActive,
      _ref$isBlock = _ref.isBlock,
      isBlock = _ref$isBlock === void 0 ? false : _ref$isBlock,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      _ref$isFocus = _ref.isFocus,
      isFocus = _ref$isFocus === void 0 ? false : _ref$isFocus,
      _ref$isHover = _ref.isHover,
      isHover = _ref$isHover === void 0 ? false : _ref$isHover,
      _ref$isInline = _ref.isInline,
      isInline = _ref$isInline === void 0 ? false : _ref$isInline,
      _ref$type = _ref.type,
      type = _ref$type === void 0 ? ButtonType.button : _ref$type,
      _ref$variant = _ref.variant,
      variant = _ref$variant === void 0 ? ButtonVariant.primary : _ref$variant,
      _ref$iconPosition = _ref.iconPosition,
      iconPosition = _ref$iconPosition === void 0 ? 'left' : _ref$iconPosition,
      _ref$ariaLabel = _ref['aria-label'],
      ariaLabel = _ref$ariaLabel === void 0 ? null : _ref$ariaLabel,
      _ref$icon = _ref.icon,
      icon = _ref$icon === void 0 ? null : _ref$icon,
      _ref$ouiaContext = _ref.ouiaContext,
      ouiaContext = _ref$ouiaContext === void 0 ? null : _ref$ouiaContext,
      _ref$ouiaId = _ref.ouiaId,
      ouiaId = _ref$ouiaId === void 0 ? null : _ref$ouiaId,
      _ref$tabIndex = _ref.tabIndex,
      tabIndex = _ref$tabIndex === void 0 ? null : _ref$tabIndex,
      props = _objectWithoutProperties(_ref, ["children", "className", "component", "isActive", "isBlock", "isDisabled", "isFocus", "isHover", "isInline", "type", "variant", "iconPosition", "aria-label", "icon", "ouiaContext", "ouiaId", "tabIndex"]);

  var Component = component;
  var isButtonElement = Component === 'button';
  return React.createElement(Component, _extends({}, props, {
    "aria-disabled": isButtonElement ? null : isDisabled,
    "aria-label": ariaLabel,
    className: (0, _reactStyles.css)(_button["default"].button, (0, _reactStyles.getModifier)(_button["default"].modifiers, variant), isBlock && _button["default"].modifiers.block, isDisabled && !isButtonElement && _button["default"].modifiers.disabled, isActive && _button["default"].modifiers.active, isFocus && _button["default"].modifiers.focus, isHover && _button["default"].modifiers.hover, isInline && variant === ButtonVariant.link && _button["default"].modifiers.inline, className),
    disabled: isButtonElement ? isDisabled : null,
    tabIndex: isDisabled && !isButtonElement ? -1 : tabIndex,
    type: isButtonElement ? type : null
  }, ouiaContext.isOuia && {
    'data-ouia-component-type': 'Button',
    'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
  }), icon && variant === ButtonVariant.link && iconPosition === 'left' && React.createElement("span", {
    className: "pf-c-button__icon"
  }, icon), variant === ButtonVariant.link && React.createElement("span", {
    className: (0, _reactStyles.css)(_button["default"].buttonText)
  }, children), variant !== ButtonVariant.link && children, icon && variant === ButtonVariant.link && iconPosition === 'right' && React.createElement("span", {
    className: "pf-c-button__icon"
  }, icon));
};

Button.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  component: _propTypes["default"].any,
  isActive: _propTypes["default"].bool,
  isBlock: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  isFocus: _propTypes["default"].bool,
  isHover: _propTypes["default"].bool,
  isInline: _propTypes["default"].bool,
  type: _propTypes["default"].oneOf(['button', 'submit', 'reset']),
  variant: _propTypes["default"].oneOf(['primary', 'secondary', 'tertiary', 'danger', 'link', 'plain', 'control']),
  iconPosition: _propTypes["default"].oneOf(['left', 'right']),
  'aria-label': _propTypes["default"].string,
  icon: _propTypes["default"].oneOfType([_propTypes["default"].node, _propTypes["default"].oneOf([null])]),
  tabIndex: _propTypes["default"].number
};
var ButtonWithOuiaContext = (0, _withOuia.withOuiaContext)(Button);
exports.Button = ButtonWithOuiaContext;
//# sourceMappingURL=Button.js.map