import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Progress/progress';
import { css } from '@patternfly/react-styles';
export const ProgressBar = (_ref) => {
  let {
    ariaProps,
    className = '',
    children = null,
    value
  } = _ref,
      props = _objectWithoutProperties(_ref, ["ariaProps", "className", "children", "value"]);

  return React.createElement("div", _extends({}, props, {
    className: css(styles.progressBar, className)
  }, ariaProps), React.createElement("div", {
    className: css(styles.progressIndicator),
    style: {
      width: `${value}%`
    }
  }, React.createElement("span", {
    className: css(styles.progressMeasure)
  }, children)));
};
ProgressBar.propTypes = {
  children: _pt.node,
  className: _pt.string,
  value: _pt.number.isRequired,
  ariaProps: _pt.shape({
    'aria-describedby': _pt.string,
    'aria-valuemin': _pt.number,
    'aria-valuenow': _pt.number,
    'aria-valuemax': _pt.number,
    'aria-valuetext': _pt.string
  })
};
//# sourceMappingURL=ProgressBar.js.map