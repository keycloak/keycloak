import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { DropdownContext } from '../Dropdown';
import { DropdownWithContext } from '../Dropdown/DropdownWithContext';
export let OptionsMenuPosition;

(function (OptionsMenuPosition) {
  OptionsMenuPosition["right"] = "right";
  OptionsMenuPosition["left"] = "left";
})(OptionsMenuPosition || (OptionsMenuPosition = {}));

export let OptionsMenuDirection;

(function (OptionsMenuDirection) {
  OptionsMenuDirection["up"] = "up";
  OptionsMenuDirection["down"] = "down";
})(OptionsMenuDirection || (OptionsMenuDirection = {}));

export const OptionsMenu = (_ref) => {
  let {
    className = '',
    menuItems,
    toggle,
    isText = false,
    isGrouped = false,
    id,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "menuItems", "toggle", "isText", "isGrouped", "id", "ref"]);

  return React.createElement(DropdownContext.Provider, {
    value: {
      id,
      onSelect: () => undefined,
      toggleIconClass: styles.optionsMenuToggleIcon,
      toggleTextClass: styles.optionsMenuToggleText,
      menuClass: styles.optionsMenuMenu,
      itemClass: styles.optionsMenuMenuItem,
      toggleClass: isText ? styles.optionsMenuToggleButton : styles.optionsMenuToggle,
      baseClass: styles.optionsMenu,
      disabledClass: styles.modifiers.disabled,
      menuComponent: isGrouped ? 'div' : 'ul',
      baseComponent: 'div'
    }
  }, React.createElement(DropdownWithContext, _extends({}, props, {
    id: id,
    dropdownItems: menuItems,
    className: className,
    isGrouped: isGrouped,
    toggle: toggle
  })));
};
OptionsMenu.propTypes = {
  className: _pt.string,
  id: _pt.string.isRequired,
  menuItems: _pt.arrayOf(_pt.node).isRequired,
  toggle: _pt.element.isRequired,
  isPlain: _pt.bool,
  isOpen: _pt.bool,
  isText: _pt.bool,
  isGrouped: _pt.bool,
  ariaLabelMenu: _pt.string,
  position: _pt.oneOf(['right', 'left']),
  direction: _pt.oneOf(['up', 'down'])
};
//# sourceMappingURL=OptionsMenu.js.map