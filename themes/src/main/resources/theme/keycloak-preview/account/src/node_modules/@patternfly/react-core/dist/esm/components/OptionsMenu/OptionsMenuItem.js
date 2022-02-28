import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { DropdownItem } from '../Dropdown';
import CheckIcon from '@patternfly/react-icons/dist/js/icons/check-icon';
export const OptionsMenuItem = (_ref) => {
  let {
    children = null,
    isSelected = false,
    onSelect = () => null,
    id = '',
    isDisabled
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "isSelected", "onSelect", "id", "isDisabled"]);

  return React.createElement(DropdownItem, _extends({
    id: id,
    component: "button",
    isDisabled: isDisabled,
    onClick: event => onSelect(event)
  }, isDisabled && {
    'aria-disabled': true
  }, props), children, isSelected && React.createElement(CheckIcon, {
    className: css(styles.optionsMenuMenuItemIcon),
    "aria-hidden": isSelected
  }));
};
OptionsMenuItem.propTypes = {
  children: _pt.node,
  className: _pt.string,
  isSelected: _pt.bool,
  isDisabled: _pt.bool,
  onSelect: _pt.func,
  id: _pt.string
};
//# sourceMappingURL=OptionsMenuItem.js.map