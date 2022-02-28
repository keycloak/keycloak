import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import EllipsisVIcon from '@patternfly/react-icons/dist/js/icons/ellipsis-v-icon';
import { Toggle } from './Toggle';
export const KebabToggle = (_ref) => {
  let {
    id = '',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    children = null,
    className = '',
    isOpen = false,
    'aria-label': ariaLabel = 'Actions',
    parentRef = null,
    isFocused = false,
    isHovered = false,
    isActive = false,
    isPlain = false,
    isDisabled = false,
    bubbleEvent = false,
    onToggle = () => undefined,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref
  } = _ref,
      props = _objectWithoutProperties(_ref, ["id", "children", "className", "isOpen", "aria-label", "parentRef", "isFocused", "isHovered", "isActive", "isPlain", "isDisabled", "bubbleEvent", "onToggle", "ref"]);

  return React.createElement(Toggle, _extends({
    id: id,
    className: className,
    isOpen: isOpen,
    "aria-label": ariaLabel,
    parentRef: parentRef,
    isFocused: isFocused,
    isHovered: isHovered,
    isActive: isActive,
    isPlain: isPlain,
    isDisabled: isDisabled,
    onToggle: onToggle,
    bubbleEvent: bubbleEvent
  }, props), React.createElement(EllipsisVIcon, null));
};
KebabToggle.propTypes = {
  id: _pt.string,
  children: _pt.node,
  className: _pt.string,
  isOpen: _pt.bool,
  'aria-label': _pt.string,
  onToggle: _pt.func,
  parentRef: _pt.any,
  isFocused: _pt.bool,
  isHovered: _pt.bool,
  isActive: _pt.bool,
  isDisabled: _pt.bool,
  isPlain: _pt.bool,
  type: _pt.oneOf(['button', 'submit', 'reset']),
  bubbleEvent: _pt.bool
};
//# sourceMappingURL=KebabToggle.js.map