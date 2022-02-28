import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/FormControl/form-control';
import { css } from '@patternfly/react-styles';
import { ValidatedOptions } from '../../helpers/constants';
export class FormSelect extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleChange", event => {
      this.props.onChange(event.currentTarget.value, event);
    });

    if (!props.id && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('FormSelect requires either an id or aria-label to be specified');
    }
  }

  render() {
    const _this$props = this.props,
          {
      children,
      className,
      value,
      isValid,
      validated,
      isDisabled,
      isRequired
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "className", "value", "isValid", "validated", "isDisabled", "isRequired"]);

    return React.createElement("select", _extends({}, props, {
      className: css(styles.formControl, className, validated === ValidatedOptions.success && styles.modifiers.success),
      "aria-invalid": !isValid || validated === ValidatedOptions.error,
      onChange: this.handleChange,
      disabled: isDisabled,
      required: isRequired,
      value: value
    }), children);
  }

}

_defineProperty(FormSelect, "propTypes", {
  children: _pt.node.isRequired,
  className: _pt.string,
  value: _pt.any,
  isValid: _pt.bool,
  validated: _pt.oneOf(['success', 'error', 'default']),
  isDisabled: _pt.bool,
  isRequired: _pt.bool,
  onBlur: _pt.func,
  onFocus: _pt.func,
  onChange: _pt.func,
  'aria-label': _pt.string
});

_defineProperty(FormSelect, "defaultProps", {
  className: '',
  value: '',
  isValid: true,
  validated: 'default',
  isDisabled: false,
  isRequired: false,
  onBlur: () => undefined,
  onFocus: () => undefined,
  onChange: () => undefined
});
//# sourceMappingURL=FormSelect.js.map