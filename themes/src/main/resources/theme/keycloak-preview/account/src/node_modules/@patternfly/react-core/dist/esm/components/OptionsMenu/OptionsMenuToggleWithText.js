import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css, getModifier } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
export const OptionsMenuToggleWithText = (_ref) => {
  let {
    parentId = '',
    toggleText,
    toggleTextClassName = '',
    toggleButtonContents,
    toggleButtonContentsClassName = '',
    onToggle = () => null,
    isOpen = false,
    isPlain = false,
    isHovered = false,
    isActive = false,
    isFocused = false,
    isDisabled = false,

    /* eslint-disable @typescript-eslint/no-unused-vars */
    ariaHasPopup,
    parentRef,
    onEnter,

    /* eslint-enable @typescript-eslint/no-unused-vars */
    'aria-label': ariaLabel = 'Options menu'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["parentId", "toggleText", "toggleTextClassName", "toggleButtonContents", "toggleButtonContentsClassName", "onToggle", "isOpen", "isPlain", "isHovered", "isActive", "isFocused", "isDisabled", "ariaHasPopup", "parentRef", "onEnter", "aria-label"]);

  return React.createElement("div", _extends({
    className: css(styles.optionsMenuToggle, getModifier(styles, 'text'), isPlain && getModifier(styles, 'plain'), isHovered && getModifier(styles, 'hover'), isActive && getModifier(styles, 'active'), isFocused && getModifier(styles, 'focus'), isDisabled && getModifier(styles, 'disabled'))
  }, props), React.createElement("span", {
    className: css(styles.optionsMenuToggleText, toggleTextClassName)
  }, toggleText), React.createElement("button", {
    className: css(styles.optionsMenuToggleButton, toggleButtonContentsClassName),
    id: `${parentId}-toggle`,
    "aria-haspopup": "listbox",
    "aria-label": ariaLabel,
    "aria-expanded": isOpen,
    onClick: () => onToggle(!isOpen)
  }, toggleButtonContents));
};
OptionsMenuToggleWithText.propTypes = {
  parentId: _pt.string,
  toggleText: _pt.node.isRequired,
  toggleTextClassName: _pt.string,
  toggleButtonContents: _pt.node,
  toggleButtonContentsClassName: _pt.string,
  onToggle: _pt.func,
  onEnter: _pt.func,
  isOpen: _pt.bool,
  isPlain: _pt.bool,
  isFocused: _pt.bool,
  isHovered: _pt.bool,
  isActive: _pt.bool,
  isDisabled: _pt.bool,
  parentRef: _pt.any,
  ariaHasPopup: _pt.oneOfType([_pt.bool, _pt.oneOf(['dialog']), _pt.oneOf(['menu']), _pt.oneOf(['false']), _pt.oneOf(['true']), _pt.oneOf(['listbox']), _pt.oneOf(['tree']), _pt.oneOf(['grid'])]),
  'aria-label': _pt.string
};
//# sourceMappingURL=OptionsMenuToggleWithText.js.map