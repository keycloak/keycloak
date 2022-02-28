import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { css } from '@patternfly/react-styles';
export class DropdownToggleAction extends React.Component {
  render() {
    const _this$props = this.props,
          {
      id,
      className,
      onClick,
      isDisabled,
      children
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["id", "className", "onClick", "isDisabled", "children"]);

    return React.createElement("button", _extends({
      id: id,
      className: css(styles.dropdownToggleButton, className),
      onClick: onClick
    }, isDisabled && {
      disabled: true,
      'aria-disabled': true
    }, props), children);
  }

}

_defineProperty(DropdownToggleAction, "propTypes", {
  className: _pt.string,
  isDisabled: _pt.bool,
  onClick: _pt.func,
  children: _pt.node,
  id: _pt.string,
  'aria-label': _pt.string
});

_defineProperty(DropdownToggleAction, "defaultProps", {
  className: '',
  isDisabled: false,
  onClick: () => {}
});
//# sourceMappingURL=DropdownToggleAction.js.map