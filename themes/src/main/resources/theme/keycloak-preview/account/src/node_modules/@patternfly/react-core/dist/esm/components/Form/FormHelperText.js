import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css, getModifier } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Form/form';
export const FormHelperText = (_ref) => {
  let {
    children = null,
    isError = false,
    isHidden = true,
    className = ''
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "isError", "isHidden", "className"]);

  return React.createElement("p", _extends({
    className: css(styles.formHelperText, isError ? getModifier(styles, 'error') : '', isHidden ? getModifier(styles, 'hidden') : '', className)
  }, props), children);
};
FormHelperText.propTypes = {
  children: _pt.node,
  isError: _pt.bool,
  isHidden: _pt.bool,
  className: _pt.string
};
//# sourceMappingURL=FormHelperText.js.map