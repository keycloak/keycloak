import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Breadcrumb/breadcrumb';
import { css } from '@patternfly/react-styles';
export const BreadcrumbHeading = (_ref) => {
  let {
    children = null,
    className = '',
    to = null,
    target = null,
    component = 'a'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "to", "target", "component"]);

  const Component = component;
  return React.createElement("li", _extends({}, props, {
    className: css(styles.breadcrumbItem, className)
  }), React.createElement("h1", {
    className: css(styles.breadcrumbHeading)
  }, to && React.createElement(Component, {
    href: to,
    target: target,
    className: css(styles.breadcrumbLink, styles.modifiers.current),
    "aria-current": "page"
  }, children), !to && React.createElement(React.Fragment, null, children)));
};
BreadcrumbHeading.propTypes = {
  children: _pt.node,
  className: _pt.string,
  to: _pt.string,
  target: _pt.string,
  component: _pt.node
};
//# sourceMappingURL=BreadcrumbHeading.js.map