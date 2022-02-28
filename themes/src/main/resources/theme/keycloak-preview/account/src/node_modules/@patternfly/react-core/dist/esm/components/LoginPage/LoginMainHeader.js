import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { Title, TitleLevel } from '../Title';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Login/login';
export const LoginMainHeader = (_ref) => {
  let {
    children = null,
    className = '',
    title = '',
    subtitle = ''
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "title", "subtitle"]);

  return React.createElement("header", _extends({
    className: css(styles.loginMainHeader, className)
  }, props), title && React.createElement(Title, {
    headingLevel: TitleLevel.h2,
    size: "3xl"
  }, title), subtitle && React.createElement("p", {
    className: css(styles.loginMainHeaderDesc)
  }, subtitle), children);
};
LoginMainHeader.propTypes = {
  children: _pt.node,
  className: _pt.string,
  title: _pt.string,
  subtitle: _pt.string
};
//# sourceMappingURL=LoginMainHeader.js.map