import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Check/check';
import { css, getModifier } from '@patternfly/react-styles';

// tslint:disable-next-line:no-empty
const defaultOnChange = () => {};

export class Checkbox extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleChange", event => {
      this.props.onChange(event.currentTarget.checked, event);
    });
  }

  render() {
    const _this$props = this.props,
          {
      'aria-label': ariaLabel,
      className,
      onChange,
      isValid,
      isDisabled,
      isChecked,
      label,
      checked,
      defaultChecked,
      description
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["aria-label", "className", "onChange", "isValid", "isDisabled", "isChecked", "label", "checked", "defaultChecked", "description"]);

    const checkedProps = {};

    if ([true, false].includes(checked) || isChecked === true) {
      checkedProps.checked = checked || isChecked;
    }

    if (onChange !== defaultOnChange) {
      checkedProps.checked = isChecked;
    }

    if ([false, true].includes(defaultChecked)) {
      checkedProps.defaultChecked = defaultChecked;
    }

    checkedProps.checked = checkedProps.checked === null ? false : checkedProps.checked;
    return React.createElement("div", {
      className: css(styles.check, className)
    }, React.createElement("input", _extends({}, props, {
      className: css(styles.checkInput),
      type: "checkbox",
      onChange: this.handleChange,
      "aria-invalid": !isValid,
      "aria-label": ariaLabel,
      disabled: isDisabled,
      ref: elem => elem && (elem.indeterminate = isChecked === null)
    }, checkedProps)), label && React.createElement("label", {
      className: css(styles.checkLabel, isDisabled ? getModifier(styles, 'disabled') : ''),
      htmlFor: props.id
    }, label), description && React.createElement("div", {
      className: css(styles.checkDescription)
    }, description));
  }

}

_defineProperty(Checkbox, "propTypes", {
  className: _pt.string,
  isValid: _pt.bool,
  isDisabled: _pt.bool,
  isChecked: _pt.bool,
  checked: _pt.bool,
  onChange: _pt.func,
  label: _pt.node,
  id: _pt.string.isRequired,
  'aria-label': _pt.string,
  description: _pt.node
});

_defineProperty(Checkbox, "defaultProps", {
  className: '',
  isValid: true,
  isDisabled: false,
  isChecked: false,
  onChange: defaultOnChange
});
//# sourceMappingURL=Checkbox.js.map