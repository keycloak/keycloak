import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
import { DrawerContext } from './Drawer';
export const DrawerPanelContent = (_ref) => {
  let {
    className = '',
    children,
    hasBorder = false,
    width,
    widthOnLg,
    widthOnXl,
    widthOn2Xl
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "children", "hasBorder", "width", "widthOnLg", "widthOnXl", "widthOn2Xl"]);

  return React.createElement(DrawerContext.Consumer, null, ({
    isExpanded
  }) => React.createElement("div", _extends({
    className: css(styles.drawerPanel, hasBorder && styles.modifiers.border, width && styles.modifiers[`width_${width}`], widthOnLg && styles.modifiers[`width_${widthOnLg}OnLg`], widthOnXl && styles.modifiers[`width_${widthOnXl}OnXl`], widthOn2Xl && styles.modifiers[`width_${widthOn2Xl}On_2xl`], className),
    hidden: !isExpanded,
    "aria-hidden": !isExpanded,
    "aria-expanded": isExpanded
  }, props), children));
};
DrawerPanelContent.propTypes = {
  className: _pt.string,
  children: _pt.node,
  hasBorder: _pt.bool,
  width: _pt.oneOf([25, 33, 50, 66, 75, 100]),
  widthOnLg: _pt.oneOf([25, 33, 50, 66, 75, 100]),
  widthOnXl: _pt.oneOf([25, 33, 50, 66, 75, 100]),
  widthOn2Xl: _pt.oneOf([25, 33, 50, 66, 75, 100])
};
//# sourceMappingURL=DrawerPanelContent.js.map