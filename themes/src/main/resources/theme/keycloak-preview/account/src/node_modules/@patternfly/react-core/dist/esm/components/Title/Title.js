import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css, getModifier } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Title/title';
export let TitleLevel;

(function (TitleLevel) {
  TitleLevel["h1"] = "h1";
  TitleLevel["h2"] = "h2";
  TitleLevel["h3"] = "h3";
  TitleLevel["h4"] = "h4";
  TitleLevel["h5"] = "h5";
  TitleLevel["h6"] = "h6";
})(TitleLevel || (TitleLevel = {}));

export const Title = (_ref) => {
  let {
    size,
    className = '',
    children = '',
    headingLevel: HeadingLevel = 'h1'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["size", "className", "children", "headingLevel"]);

  return React.createElement(HeadingLevel, _extends({}, props, {
    className: css(styles.title, getModifier(styles, size), className)
  }), children);
};
Title.propTypes = {
  size: _pt.oneOfType([_pt.any, _pt.oneOf(['xs']), _pt.oneOf(['sm']), _pt.oneOf(['md']), _pt.oneOf(['lg']), _pt.oneOf(['xl']), _pt.oneOf(['2xl']), _pt.oneOf(['3xl']), _pt.oneOf(['4xl'])]).isRequired,
  children: _pt.node,
  className: _pt.string,
  headingLevel: _pt.oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6'])
};
//# sourceMappingURL=Title.js.map