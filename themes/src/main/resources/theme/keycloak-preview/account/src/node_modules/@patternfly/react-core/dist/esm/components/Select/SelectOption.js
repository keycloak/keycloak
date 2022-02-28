import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import { default as checkStyles } from '@patternfly/react-styles/css/components/Check/check';
import { css } from '@patternfly/react-styles';
import CheckIcon from '@patternfly/react-icons/dist/js/icons/check-icon';
import { SelectConsumer, SelectVariant, KeyTypes } from './selectConstants';
export class SelectOption extends React.Component {
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

        if (this.context.variant === SelectVariant.checkbox) {
          this.ref.current.focus();
        }
      }
    });
  }

  componentDidMount() {
    this.props.sendRef(this.props.isDisabled ? null : this.ref.current, this.props.index);
  }

  componentDidUpdate() {
    this.props.sendRef(this.props.isDisabled ? null : this.ref.current, this.props.index);
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
      isPlaceholder,
      isNoResultsOption,
      isSelected,
      isChecked,
      isFocused,
      sendRef,
      keyHandler,
      index,
      component
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "className", "value", "onClick", "isDisabled", "isPlaceholder", "isNoResultsOption", "isSelected", "isChecked", "isFocused", "sendRef", "keyHandler", "index", "component"]);
    /* eslint-enable @typescript-eslint/no-unused-vars */


    const Component = component;
    return React.createElement(SelectConsumer, null, ({
      onSelect,
      onClose,
      variant
    }) => React.createElement(React.Fragment, null, variant !== SelectVariant.checkbox && React.createElement("li", {
      role: "presentation"
    }, React.createElement(Component, _extends({}, props, {
      className: css(styles.selectMenuItem, isSelected && styles.modifiers.selected, isDisabled && styles.modifiers.disabled, isFocused && styles.modifiers.focus, className),
      onClick: event => {
        if (!isDisabled) {
          onClick(event);
          onSelect(event, value, isPlaceholder);
          onClose();
        }
      },
      role: "option",
      "aria-selected": isSelected || null,
      ref: this.ref,
      onKeyDown: this.onKeyDown,
      type: "button"
    }), children || value.toString(), isSelected && React.createElement(CheckIcon, {
      className: css(styles.selectMenuItemIcon),
      "aria-hidden": true
    }))), variant === SelectVariant.checkbox && !isNoResultsOption && React.createElement("label", _extends({}, props, {
      className: css(checkStyles.check, styles.selectMenuItem, isDisabled && styles.modifiers.disabled, className),
      onKeyDown: this.onKeyDown
    }), React.createElement("input", {
      id: value.toString(),
      className: css(checkStyles.checkInput),
      type: "checkbox",
      onChange: event => {
        if (!isDisabled) {
          onClick(event);
          onSelect(event, value);
        }
      },
      ref: this.ref,
      checked: isChecked || false,
      disabled: isDisabled
    }), React.createElement("span", {
      className: css(checkStyles.checkLabel, isDisabled && styles.modifiers.disabled)
    }, children || value.toString())), variant === SelectVariant.checkbox && isNoResultsOption && React.createElement("div", null, React.createElement(Component, _extends({}, props, {
      className: css(styles.selectMenuItem, isSelected && styles.modifiers.selected, isDisabled && styles.modifiers.disabled, isFocused && styles.modifiers.focus, className),
      role: "option",
      "aria-selected": isSelected || null,
      ref: this.ref,
      onKeyDown: this.onKeyDown,
      type: "button"
    }), children || value.toString()))));
  }

}

_defineProperty(SelectOption, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  index: _pt.number,
  component: _pt.node,
  value: _pt.oneOfType([_pt.string, _pt.shape({})]),
  isDisabled: _pt.bool,
  isPlaceholder: _pt.bool,
  isNoResultsOption: _pt.bool,
  isSelected: _pt.bool,
  isChecked: _pt.bool,
  isFocused: _pt.bool,
  sendRef: _pt.func,
  keyHandler: _pt.func,
  onClick: _pt.func
});

_defineProperty(SelectOption, "defaultProps", {
  className: '',
  value: '',
  index: 0,
  isDisabled: false,
  isPlaceholder: false,
  isSelected: false,
  isChecked: false,
  isFocused: false,
  isNoResultsOption: false,
  component: 'button',
  onClick: () => {},
  sendRef: () => {},
  keyHandler: () => {}
});
//# sourceMappingURL=SelectOption.js.map