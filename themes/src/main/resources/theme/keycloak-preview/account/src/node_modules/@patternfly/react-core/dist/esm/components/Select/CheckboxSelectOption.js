import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import { default as checkStyles } from '@patternfly/react-styles/css/components/Check/check';
import { css } from '@patternfly/react-styles';
import { SelectConsumer, KeyTypes } from './selectConstants';
export class CheckboxSelectOption extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "ref", React.createRef());

    _defineProperty(this, "onKeyDown", event => {
      if (event.key === KeyTypes.Tab) {
        return;
      }

      event.preventDefault();

      if (event.key === KeyTypes.ArrowUp) {
        this.props.keyHandler(this.props.index, 'up');
      } else if (event.key === KeyTypes.ArrowDown) {
        this.props.keyHandler(this.props.index, 'down');
      } else if (event.key === KeyTypes.Enter) {
        this.ref.current.click();
        this.ref.current.focus();
      }
    });
  }

  componentDidMount() {
    this.props.sendRef(this.ref.current, this.props.index);
  }

  componentDidUpdate() {
    this.props.sendRef(this.ref.current, this.props.index);
  }

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const _this$props = this.props,
          {
      children,
      className,
      value,
      onClick,
      isDisabled,
      isChecked,
      sendRef,
      keyHandler,
      index
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "className", "value", "onClick", "isDisabled", "isChecked", "sendRef", "keyHandler", "index"]);
    /* eslint-enable @typescript-eslint/no-unused-vars */


    return React.createElement(SelectConsumer, null, ({
      onSelect
    }) => React.createElement("label", _extends({}, props, {
      className: css(checkStyles.check, styles.selectMenuItem, isDisabled && styles.modifiers.disabled, className),
      onKeyDown: this.onKeyDown
    }), React.createElement("input", {
      id: value,
      className: css(checkStyles.checkInput),
      type: "checkbox",
      onChange: event => {
        if (!isDisabled) {
          onClick(event);
          onSelect && onSelect(event, value);
        }
      },
      ref: this.ref,
      checked: isChecked || false,
      disabled: isDisabled
    }), React.createElement("span", {
      className: css(checkStyles.checkLabel, isDisabled && styles.modifiers.disabled)
    }, children || value)));
  }

}

_defineProperty(CheckboxSelectOption, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  index: _pt.number,
  value: _pt.string,
  isDisabled: _pt.bool,
  isChecked: _pt.bool,
  sendRef: _pt.func,
  keyHandler: _pt.func,
  onClick: _pt.func
});

_defineProperty(CheckboxSelectOption, "defaultProps", {
  className: '',
  value: '',
  index: 0,
  isDisabled: false,
  isChecked: false,
  onClick: () => {},
  sendRef: () => {},
  keyHandler: () => {}
});
//# sourceMappingURL=CheckboxSelectOption.js.map