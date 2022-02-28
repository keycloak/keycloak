import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Grid/grid';
import { css } from '@patternfly/react-styles';
import { getModifier } from '@patternfly/react-styles';
import { DeviceSizes } from '../../styles/sizes';
export const GridItem = (_ref) => {
  let {
    children = null,
    className = '',
    span = null,
    rowSpan = null,
    offset = null
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "span", "rowSpan", "offset"]);

  const classes = [styles.gridItem, span && getModifier(styles, `${span}Col`), rowSpan && getModifier(styles, `${rowSpan}Row`), offset && getModifier(styles, `offset_${offset}Col`)];
  Object.entries(DeviceSizes).forEach(([propKey, classModifier]) => {
    const key = propKey;
    const rowSpanKey = `${key}RowSpan`;
    const offsetKey = `${key}Offset`;
    const spanValue = props[key];
    const rowSpanValue = props[rowSpanKey];
    const offsetValue = props[offsetKey];

    if (spanValue) {
      classes.push(getModifier(styles, `${spanValue}ColOn${classModifier}`));
    }

    if (rowSpanValue) {
      classes.push(getModifier(styles, `${rowSpanValue}RowOn${classModifier}`));
    }

    if (offsetValue) {
      classes.push(getModifier(styles, `offset_${offsetValue}ColOn${classModifier}`));
    }

    delete props[key];
    delete props[rowSpanKey];
    delete props[offsetKey];
  });
  return React.createElement("div", _extends({
    className: css(...classes, className)
  }, props), children);
};
GridItem.propTypes = {
  children: _pt.node,
  className: _pt.string,
  span: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  rowSpan: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  offset: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  sm: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  smRowSpan: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  smOffset: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  md: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  mdRowSpan: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  mdOffset: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  lg: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  lgRowSpan: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  lgOffset: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xl: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xlRowSpan: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xlOffset: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xl2: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xl2RowSpan: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
  xl2Offset: _pt.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])
};
//# sourceMappingURL=GridItem.js.map