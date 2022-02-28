import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Card/card';
import { css } from '@patternfly/react-styles';
export const Card = (_ref) => {
  let {
    children = null,
    className = '',
    component = 'article',
    isHoverable = false,
    isCompact = false,
    isSelectable = false,
    isSelected = false
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "component", "isHoverable", "isCompact", "isSelectable", "isSelected"]);

  const Component = component;
  return React.createElement(Component, _extends({
    className: css(styles.card, isHoverable && styles.modifiers.hoverable, isCompact && styles.modifiers.compact, isSelectable && styles.modifiers.selectable, isSelected && isSelectable && styles.modifiers.selected, className),
    tabIndex: isSelectable ? '0' : undefined
  }, props), children);
};
Card.propTypes = {
  children: _pt.node,
  className: _pt.string,
  component: _pt.any,
  isHoverable: _pt.bool,
  isCompact: _pt.bool,
  isSelectable: _pt.bool,
  isSelected: _pt.bool
};
//# sourceMappingURL=Card.js.map