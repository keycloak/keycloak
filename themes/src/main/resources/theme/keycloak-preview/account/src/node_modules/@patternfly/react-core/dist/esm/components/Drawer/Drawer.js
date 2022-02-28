import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
export const DrawerContext = React.createContext({
  isExpanded: false
});
export const Drawer = (_ref) => {
  let {
    className = '',
    children,
    isExpanded = false,
    isInline = false,
    isStatic = false,
    position = 'right'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "children", "isExpanded", "isInline", "isStatic", "position"]);

  return React.createElement(DrawerContext.Provider, {
    value: {
      isExpanded
    }
  }, React.createElement("div", _extends({
    className: css(styles.drawer, isExpanded && styles.modifiers.expanded, isInline && styles.modifiers.inline, isStatic && styles.modifiers.static, position === 'left' && styles.modifiers.panelLeft, className)
  }, props), children));
};
Drawer.propTypes = {
  className: _pt.string,
  children: _pt.node,
  isExpanded: _pt.bool,
  isInline: _pt.bool,
  isStatic: _pt.bool,
  position: _pt.oneOf(['left', 'right'])
};
//# sourceMappingURL=Drawer.js.map