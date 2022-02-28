import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Button/button';
import { css, getModifier } from '@patternfly/react-styles';
import { withOuiaContext } from '../withOuia';
export let ButtonVariant;

(function (ButtonVariant) {
  ButtonVariant["primary"] = "primary";
  ButtonVariant["secondary"] = "secondary";
  ButtonVariant["tertiary"] = "tertiary";
  ButtonVariant["danger"] = "danger";
  ButtonVariant["link"] = "link";
  ButtonVariant["plain"] = "plain";
  ButtonVariant["control"] = "control";
})(ButtonVariant || (ButtonVariant = {}));

export let ButtonType;

(function (ButtonType) {
  ButtonType["button"] = "button";
  ButtonType["submit"] = "submit";
  ButtonType["reset"] = "reset";
})(ButtonType || (ButtonType = {}));

const Button = (_ref) => {
  let {
    children = null,
    className = '',
    component = 'button',
    isActive = false,
    isBlock = false,
    isDisabled = false,
    isFocus = false,
    isHover = false,
    isInline = false,
    type = ButtonType.button,
    variant = ButtonVariant.primary,
    iconPosition = 'left',
    'aria-label': ariaLabel = null,
    icon = null,
    ouiaContext = null,
    ouiaId = null,
    tabIndex = null
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "component", "isActive", "isBlock", "isDisabled", "isFocus", "isHover", "isInline", "type", "variant", "iconPosition", "aria-label", "icon", "ouiaContext", "ouiaId", "tabIndex"]);

  const Component = component;
  const isButtonElement = Component === 'button';
  return React.createElement(Component, _extends({}, props, {
    "aria-disabled": isButtonElement ? null : isDisabled,
    "aria-label": ariaLabel,
    className: css(styles.button, getModifier(styles.modifiers, variant), isBlock && styles.modifiers.block, isDisabled && !isButtonElement && styles.modifiers.disabled, isActive && styles.modifiers.active, isFocus && styles.modifiers.focus, isHover && styles.modifiers.hover, isInline && variant === ButtonVariant.link && styles.modifiers.inline, className),
    disabled: isButtonElement ? isDisabled : null,
    tabIndex: isDisabled && !isButtonElement ? -1 : tabIndex,
    type: isButtonElement ? type : null
  }, ouiaContext.isOuia && {
    'data-ouia-component-type': 'Button',
    'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
  }), icon && variant === ButtonVariant.link && iconPosition === 'left' && React.createElement("span", {
    className: "pf-c-button__icon"
  }, icon), variant === ButtonVariant.link && React.createElement("span", {
    className: css(styles.buttonText)
  }, children), variant !== ButtonVariant.link && children, icon && variant === ButtonVariant.link && iconPosition === 'right' && React.createElement("span", {
    className: "pf-c-button__icon"
  }, icon));
};

Button.propTypes = {
  children: _pt.node,
  className: _pt.string,
  component: _pt.any,
  isActive: _pt.bool,
  isBlock: _pt.bool,
  isDisabled: _pt.bool,
  isFocus: _pt.bool,
  isHover: _pt.bool,
  isInline: _pt.bool,
  type: _pt.oneOf(['button', 'submit', 'reset']),
  variant: _pt.oneOf(['primary', 'secondary', 'tertiary', 'danger', 'link', 'plain', 'control']),
  iconPosition: _pt.oneOf(['left', 'right']),
  'aria-label': _pt.string,
  icon: _pt.oneOfType([_pt.node, _pt.oneOf([null])]),
  tabIndex: _pt.number
};
const ButtonWithOuiaContext = withOuiaContext(Button);
export { ButtonWithOuiaContext as Button };
//# sourceMappingURL=Button.js.map