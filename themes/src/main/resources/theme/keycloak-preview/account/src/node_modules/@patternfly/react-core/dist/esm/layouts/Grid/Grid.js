import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Grid/grid';
import { css } from '@patternfly/react-styles';
import { getModifier } from '@patternfly/react-styles';
import { DeviceSizes } from '../../styles/sizes';
export const Grid = (_ref) => {
  let {
    children = null,
    className = '',
    gutter = null,
    span = null
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "gutter", "span"]);

  const classes = [styles.grid, span && getModifier(styles, `all_${span}Col`)];
  Object.entries(DeviceSizes).forEach(([propKey, gridSpanModifier]) => {
    const key = propKey;
    const propValue = props[key];

    if (propValue) {
      classes.push(getModifier(styles, `all_${propValue}ColOn${gridSpanModifier}`));
    }

    delete props[key];
  });
  return React.createElement("div", _extends({
    className: css(...classes, gutter && styles.modifiers.gutter, className)
  }, props), children);
};
Grid.propTypes = {
  children: _pt.node,
  className: _pt.string,
  gutter: _pt.oneOf(['sm', 'md', 'lg']),
  span: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  sm: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  md: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  lg: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xl: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xl2: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])
};
//# sourceMappingURL=Grid.js.map