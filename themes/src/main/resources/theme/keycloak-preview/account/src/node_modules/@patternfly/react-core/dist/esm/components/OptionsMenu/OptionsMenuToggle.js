import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { DropdownToggle, DropdownContext } from '../Dropdown';
export const OptionsMenuToggle = (_ref) => {
  let {
    isPlain = false,
    isHovered = false,
    isActive = false,
    isFocused = false,
    isDisabled = false,
    isOpen = false,
    parentId = '',
    toggleTemplate = React.createElement(React.Fragment, null),
    hideCaret = false,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    isSplitButton = false,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    type,
    'aria-label': ariaLabel = 'Options menu'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["isPlain", "isHovered", "isActive", "isFocused", "isDisabled", "isOpen", "parentId", "toggleTemplate", "hideCaret", "isSplitButton", "type", "aria-label"]);

  return React.createElement(DropdownContext.Consumer, null, ({
    id: contextId
  }) => React.createElement(DropdownToggle, _extends({}, (isPlain || hideCaret) && {
    iconComponent: null
  }, props, {
    isPlain: isPlain,
    isOpen: isOpen,
    isDisabled: isDisabled,
    isHovered: isHovered,
    isActive: isActive,
    isFocused: isFocused,
    id: parentId ? `${parentId}-toggle` : `${contextId}-toggle`,
    ariaHasPopup: "listbox",
    "aria-label": ariaLabel,
    "aria-expanded": isOpen
  }, toggleTemplate ? {
    children: toggleTemplate
  } : {})));
};
OptionsMenuToggle.propTypes = {
  parentId: _pt.string,
  onToggle: _pt.func,
  isOpen: _pt.bool,
  isPlain: _pt.bool,
  isFocused: _pt.bool,
  isHovered: _pt.bool,
  isSplitButton: _pt.bool,
  isActive: _pt.bool,
  isDisabled: _pt.bool,
  hideCaret: _pt.bool,
  'aria-label': _pt.string,
  onEnter: _pt.func,
  parentRef: _pt.any,
  toggleTemplate: _pt.node
};
//# sourceMappingURL=OptionsMenuToggle.js.map