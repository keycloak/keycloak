import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css, getModifier } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
export const DataListCell = (_ref) => {
  let {
    children = null,
    className = '',
    width = 1,
    isFilled = true,
    alignRight = false,
    isIcon = false
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "width", "isFilled", "alignRight", "isIcon"]);

  return React.createElement("div", _extends({
    className: css(styles.dataListCell, width > 1 && getModifier(styles, `flex_${width}`, ''), !isFilled && styles.modifiers.noFill, alignRight && styles.modifiers.alignRight, isIcon && styles.modifiers.icon, className)
  }, props), children);
};
DataListCell.propTypes = {
  children: _pt.node,
  className: _pt.string,
  width: _pt.oneOf([1, 2, 3, 4, 5]),
  isFilled: _pt.bool,
  alignRight: _pt.bool,
  isIcon: _pt.bool
};
//# sourceMappingURL=DataListCell.js.map