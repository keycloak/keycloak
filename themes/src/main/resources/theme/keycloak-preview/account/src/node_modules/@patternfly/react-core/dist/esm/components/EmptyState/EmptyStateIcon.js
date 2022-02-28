import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/EmptyState/empty-state';
export let IconSize;

(function (IconSize) {
  IconSize["sm"] = "sm";
  IconSize["md"] = "md";
  IconSize["lg"] = "lg";
  IconSize["xl"] = "xl";
})(IconSize || (IconSize = {}));

export const EmptyStateIcon = (_ref) => {
  let {
    className = '',
    icon: IconComponent = null,
    component: AnyComponent = null,
    variant = 'icon'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "icon", "component", "variant"]);

  const classNames = css(styles.emptyStateIcon, className);
  return variant === 'icon' ? React.createElement(IconComponent, _extends({
    className: classNames
  }, props, {
    "aria-hidden": "true"
  })) : React.createElement("div", {
    className: classNames
  }, React.createElement(AnyComponent, null));
};
EmptyStateIcon.propTypes = {
  color: _pt.string,
  size: _pt.oneOf(['sm', 'md', 'lg', 'xl']),
  title: _pt.string,
  className: _pt.string,
  icon: _pt.oneOfType([_pt.string, _pt.any]),
  component: _pt.any,
  variant: _pt.oneOf(['icon', 'container'])
};
//# sourceMappingURL=EmptyStateIcon.js.map