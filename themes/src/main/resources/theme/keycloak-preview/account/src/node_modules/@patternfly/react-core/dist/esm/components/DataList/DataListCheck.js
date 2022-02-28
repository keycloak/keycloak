import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
export const DataListCheck = (_ref) => {
  let {
    className = '',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onChange = (checked, event) => {},
    isValid = true,
    isDisabled = false,
    isChecked = null,
    checked = null
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "onChange", "isValid", "isDisabled", "isChecked", "checked"]);

  return React.createElement("div", {
    className: css(styles.dataListItemControl, className)
  }, React.createElement("div", {
    className: css('pf-c-data-list__check')
  }, React.createElement("input", _extends({}, props, {
    type: "checkbox",
    onChange: event => onChange(event.currentTarget.checked, event),
    "aria-invalid": !isValid,
    disabled: isDisabled,
    checked: isChecked || checked
  }))));
};
DataListCheck.propTypes = {
  className: _pt.string,
  isValid: _pt.bool,
  isDisabled: _pt.bool,
  isChecked: _pt.bool,
  checked: _pt.bool,
  onChange: _pt.func,
  'aria-labelledby': _pt.string.isRequired
};
//# sourceMappingURL=DataListCheck.js.map