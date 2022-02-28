import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import CaretDownIcon from '@patternfly/react-icons/dist/js/icons/caret-down-icon';
import { Toggle } from './Toggle';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { DropdownContext } from './dropdownConstants';
import { css } from '@patternfly/react-styles';
export const DropdownToggle = (_ref) => {
  let {
    id = '',
    children = null,
    className = '',
    isOpen = false,
    parentRef = null,
    isFocused = false,
    isHovered = false,
    isActive = false,
    isDisabled = false,
    isPlain = false,
    isPrimary = false,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onToggle = _isOpen => undefined,
    iconComponent: IconComponent = CaretDownIcon,
    splitButtonItems,
    splitButtonVariant = 'checkbox',
    ariaHasPopup,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref
  } = _ref,
      props = _objectWithoutProperties(_ref, ["id", "children", "className", "isOpen", "parentRef", "isFocused", "isHovered", "isActive", "isDisabled", "isPlain", "isPrimary", "onToggle", "iconComponent", "splitButtonItems", "splitButtonVariant", "ariaHasPopup", "ref"]);

  const toggle = React.createElement(DropdownContext.Consumer, null, ({
    toggleTextClass,
    toggleIconClass
  }) => React.createElement(Toggle, _extends({}, props, {
    id: id,
    className: className,
    isOpen: isOpen,
    parentRef: parentRef,
    isFocused: isFocused,
    isHovered: isHovered,
    isActive: isActive,
    isDisabled: isDisabled,
    isPlain: isPlain,
    isPrimary: isPrimary,
    onToggle: onToggle,
    ariaHasPopup: ariaHasPopup
  }, splitButtonItems && {
    isSplitButton: true,
    'aria-label': props['aria-label'] || 'Select'
  }), children && React.createElement("span", {
    className: IconComponent && css(toggleTextClass)
  }, children), IconComponent && React.createElement(IconComponent, {
    className: css(children && toggleIconClass)
  })));

  if (splitButtonItems) {
    return React.createElement("div", {
      className: css(styles.dropdownToggle, styles.modifiers.splitButton, splitButtonVariant === 'action' && styles.modifiers.action, isDisabled && styles.modifiers.disabled)
    }, splitButtonItems, toggle);
  }

  return toggle;
};
DropdownToggle.propTypes = {
  id: _pt.string,
  children: _pt.node,
  className: _pt.string,
  isOpen: _pt.bool,
  onToggle: _pt.func,
  parentRef: _pt.any,
  isFocused: _pt.bool,
  isHovered: _pt.bool,
  isActive: _pt.bool,
  isPlain: _pt.bool,
  isDisabled: _pt.bool,
  isPrimary: _pt.bool,
  iconComponent: _pt.oneOfType([_pt.any, _pt.oneOf([null])]),
  splitButtonItems: _pt.arrayOf(_pt.node),
  splitButtonVariant: _pt.oneOf(['action', 'checkbox']),
  'aria-label': _pt.string,
  ariaHasPopup: _pt.oneOfType([_pt.bool, _pt.oneOf(['listbox']), _pt.oneOf(['menu']), _pt.oneOf(['dialog']), _pt.oneOf(['grid']), _pt.oneOf(['listbox']), _pt.oneOf(['tree'])]),
  type: _pt.oneOf(['button', 'submit', 'reset']),
  onEnter: _pt.func
};
//# sourceMappingURL=DropdownToggle.js.map