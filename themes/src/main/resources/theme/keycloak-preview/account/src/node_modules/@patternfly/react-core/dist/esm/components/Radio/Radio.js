import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Radio/radio';
import { css, getModifier } from '@patternfly/react-styles';
export class Radio extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleChange", event => {
      this.props.onChange(event.currentTarget.checked, event);
    });

    if (!props.label && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('Radio:', 'Radio requires an aria-label to be specified');
    }
  }

  render() {
    const _this$props = this.props,
          {
      'aria-label': ariaLabel,
      checked,
      className,
      defaultChecked,
      isLabelWrapped,
      isLabelBeforeButton,
      isChecked,
      isDisabled,
      isValid,
      label,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onChange,
      description
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["aria-label", "checked", "className", "defaultChecked", "isLabelWrapped", "isLabelBeforeButton", "isChecked", "isDisabled", "isValid", "label", "onChange", "description"]);

    const inputRendered = React.createElement("input", _extends({}, props, {
      className: css(styles.radioInput),
      type: "radio",
      onChange: this.handleChange,
      "aria-invalid": !isValid,
      disabled: isDisabled,
      checked: checked || isChecked
    }, checked === undefined && {
      defaultChecked
    }, !label && {
      'aria-label': ariaLabel
    }));
    const labelRendered = !label ? null : isLabelWrapped ? React.createElement("span", {
      className: css(styles.radioLabel, getModifier(styles, isDisabled && 'disabled'))
    }, label) : React.createElement("label", {
      className: css(styles.radioLabel, getModifier(styles, isDisabled && 'disabled')),
      htmlFor: props.id
    }, label);
    const descRender = description ? React.createElement("div", {
      className: css(styles.radioDescription)
    }, description) : null;
    const childrenRendered = isLabelBeforeButton ? React.createElement(React.Fragment, null, labelRendered, inputRendered, descRender) : React.createElement(React.Fragment, null, inputRendered, labelRendered, descRender);
    return isLabelWrapped ? React.createElement("label", {
      className: css(styles.radio, className),
      htmlFor: props.id
    }, childrenRendered) : React.createElement("div", {
      className: css(styles.radio, className)
    }, childrenRendered);
  }

}

_defineProperty(Radio, "propTypes", {
  className: _pt.string,
  id: _pt.string.isRequired,
  isLabelWrapped: _pt.bool,
  isLabelBeforeButton: _pt.bool,
  checked: _pt.bool,
  isChecked: _pt.bool,
  isDisabled: _pt.bool,
  isValid: _pt.bool,
  label: _pt.node,
  name: _pt.string.isRequired,
  onChange: _pt.func,
  'aria-label': _pt.string,
  description: _pt.node
});

_defineProperty(Radio, "defaultProps", {
  className: '',
  isDisabled: false,
  isValid: true,
  onChange: () => {}
});
//# sourceMappingURL=Radio.js.map