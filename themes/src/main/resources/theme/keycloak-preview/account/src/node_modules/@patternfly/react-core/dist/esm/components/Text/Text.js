import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
export let TextVariants;

(function (TextVariants) {
  TextVariants["h1"] = "h1";
  TextVariants["h2"] = "h2";
  TextVariants["h3"] = "h3";
  TextVariants["h4"] = "h4";
  TextVariants["h5"] = "h5";
  TextVariants["h6"] = "h6";
  TextVariants["p"] = "p";
  TextVariants["a"] = "a";
  TextVariants["small"] = "small";
  TextVariants["blockquote"] = "blockquote";
  TextVariants["pre"] = "pre";
})(TextVariants || (TextVariants = {}));

export const Text = (_ref) => {
  let {
    children = null,
    className = '',
    component = TextVariants.p
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "component"]);

  const Component = component;
  return React.createElement(Component, _extends({}, props, {
    "data-pf-content": true,
    className: css(className)
  }), children);
};
Text.propTypes = {
  component: _pt.oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'a', 'small', 'blockquote', 'pre']),
  children: _pt.node,
  className: _pt.string
};
//# sourceMappingURL=Text.js.map