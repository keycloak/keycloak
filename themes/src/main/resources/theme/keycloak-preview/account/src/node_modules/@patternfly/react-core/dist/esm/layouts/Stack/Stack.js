import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Stack/stack';
import { css } from '@patternfly/react-styles';
import { getGutterModifier } from '../../styles/gutters';
export const Stack = (_ref) => {
  let {
    gutter = null,
    className = '',
    children = null,
    component = 'div'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["gutter", "className", "children", "component"]);

  const Component = component;
  return React.createElement(Component, _extends({}, props, {
    className: css(styles.stack, gutter && getGutterModifier(styles, gutter, styles.modifiers.gutter), className)
  }), children);
};
Stack.propTypes = {
  gutter: _pt.oneOf(['sm', 'md', 'lg']),
  children: _pt.node,
  className: _pt.string,
  component: _pt.node
};
//# sourceMappingURL=Stack.js.map