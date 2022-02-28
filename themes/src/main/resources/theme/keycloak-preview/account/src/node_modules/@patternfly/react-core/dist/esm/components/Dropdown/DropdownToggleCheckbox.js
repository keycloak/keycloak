import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { css } from '@patternfly/react-styles';
export class DropdownToggleCheckbox extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "handleChange", event => {
      this.props.onChange(event.target.checked, event);
    });

    _defineProperty(this, "calculateChecked", () => {
      const {
        isChecked,
        checked
      } = this.props;
      return isChecked !== undefined ? isChecked : checked;
    });
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _this$props = this.props,
          {
      className,
      onChange,
      isValid,
      isDisabled,
      isChecked,
      ref,
      checked,
      children
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "onChange", "isValid", "isDisabled", "isChecked", "ref", "checked", "children"]);

    const text = children && React.createElement("span", {
      className: css(styles.dropdownToggleText, className),
      "aria-hidden": "true",
      id: `${props.id}-text`
    }, children);
    return React.createElement("label", {
      className: css(styles.dropdownToggleCheck, className),
      htmlFor: props.id
    }, React.createElement("input", _extends({}, props, this.calculateChecked() !== undefined && {
      onChange: this.handleChange
    }, {
      type: "checkbox",
      ref: ref,
      "aria-invalid": !isValid,
      disabled: isDisabled,
      checked: this.calculateChecked()
    })), text);
  }

}

_defineProperty(DropdownToggleCheckbox, "propTypes", {
  className: _pt.string,
  isValid: _pt.bool,
  isDisabled: _pt.bool,
  isChecked: _pt.oneOfType([_pt.bool, _pt.oneOf([null])]),
  checked: _pt.oneOfType([_pt.bool, _pt.oneOf([null])]),
  onChange: _pt.func,
  children: _pt.node,
  id: _pt.string.isRequired,
  'aria-label': _pt.string.isRequired
});

_defineProperty(DropdownToggleCheckbox, "defaultProps", {
  className: '',
  isValid: true,
  isDisabled: false,
  onChange: () => undefined
});
//# sourceMappingURL=DropdownToggleCheckbox.js.map