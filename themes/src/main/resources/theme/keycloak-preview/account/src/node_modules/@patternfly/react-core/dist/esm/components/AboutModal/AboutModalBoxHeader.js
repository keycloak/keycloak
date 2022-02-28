import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AboutModalBox/about-modal-box';
import { Title } from '../Title';
export const AboutModalBoxHeader = (_ref) => {
  let {
    className = '',
    productName = '',
    id
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "productName", "id"]);

  return React.createElement("div", _extends({
    className: css(styles.aboutModalBoxHeader, className)
  }, props), React.createElement(Title, {
    headingLevel: "h1",
    size: "4xl",
    id: id
  }, productName));
};
AboutModalBoxHeader.propTypes = {
  className: _pt.string,
  productName: _pt.string,
  id: _pt.string.isRequired
};
//# sourceMappingURL=AboutModalBoxHeader.js.map