import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css, getModifier } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/EmptyState/empty-state';
export let EmptyStateVariant;

(function (EmptyStateVariant) {
  EmptyStateVariant["xl"] = "xl";
  EmptyStateVariant["large"] = "large";
  EmptyStateVariant["small"] = "small";
  EmptyStateVariant["full"] = "full";
})(EmptyStateVariant || (EmptyStateVariant = {}));

const maxWidthModifiers = {
  xl: 'xl',
  large: 'lg',
  small: 'sm',
  full: ''
};
export const EmptyState = (_ref) => {
  let {
    children,
    className = '',
    variant = EmptyStateVariant.large
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "variant"]);

  const maxWidthModifier = maxWidthModifiers[variant];
  return React.createElement("div", _extends({
    className: css(styles.emptyState, getModifier(styles, maxWidthModifier, null), className)
  }, props), children);
};
EmptyState.propTypes = {
  className: _pt.string,
  children: _pt.node.isRequired,
  variant: _pt.oneOf(['small', 'large', 'full', 'xl'])
};
//# sourceMappingURL=EmptyState.js.map