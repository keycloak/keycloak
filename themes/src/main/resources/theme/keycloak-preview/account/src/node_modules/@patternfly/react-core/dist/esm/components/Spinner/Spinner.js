import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Spinner/spinner';
import { css, getModifier } from '@patternfly/react-styles';
export let spinnerSize;

(function (spinnerSize) {
  spinnerSize["sm"] = "sm";
  spinnerSize["md"] = "md";
  spinnerSize["lg"] = "lg";
  spinnerSize["xl"] = "xl";
})(spinnerSize || (spinnerSize = {}));

export const Spinner = (_ref) => {
  let {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '',
    size = 'xl',
    'aria-valuetext': ariaValueText = 'Loading...'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "size", "aria-valuetext"]);

  return React.createElement("span", _extends({
    className: css(styles.spinner, getModifier(styles, size)),
    role: "progressbar",
    "aria-valuetext": ariaValueText
  }, props), React.createElement("span", {
    className: css(styles.spinnerClipper)
  }), React.createElement("span", {
    className: css(styles.spinnerLeadBall)
  }), React.createElement("span", {
    className: css(styles.spinnerTailBall)
  }));
};
Spinner.propTypes = {
  className: _pt.string,
  size: _pt.oneOf(['sm', 'md', 'lg', 'xl']),
  'aria-valuetext': _pt.string
};
//# sourceMappingURL=Spinner.js.map