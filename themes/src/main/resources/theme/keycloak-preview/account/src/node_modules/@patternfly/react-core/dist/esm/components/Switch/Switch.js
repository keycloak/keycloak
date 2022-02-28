import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Switch/switch';
import { css } from '@patternfly/react-styles';
import CheckIcon from '@patternfly/react-icons/dist/js/icons/check-icon';
import { getUniqueId } from '../../helpers/util';
import { withOuiaContext } from '../withOuia';

class Switch extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "id", '');

    if (!props.id && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('Switch: Switch requires either an id or aria-label to be specified');
    }

    this.id = props.id || getUniqueId();
  }

  render() {
    const _this$props = this.props,
          {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      id,
      className,
      label,
      labelOff,
      isChecked,
      isDisabled,
      onChange,
      ouiaContext,
      ouiaId
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["id", "className", "label", "labelOff", "isChecked", "isDisabled", "onChange", "ouiaContext", "ouiaId"]);

    const isAriaLabelledBy = props['aria-label'] === '';
    return React.createElement("label", _extends({
      className: css(styles.switch, className),
      htmlFor: this.id
    }, ouiaContext.isOuia && {
      'data-ouia-component-type': 'Switch',
      'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
    }), React.createElement("input", _extends({
      id: this.id,
      className: css(styles.switchInput),
      type: "checkbox",
      onChange: event => onChange(event.target.checked, event),
      checked: isChecked,
      disabled: isDisabled,
      "aria-labelledby": isAriaLabelledBy ? `${this.id}-on` : null
    }, props)), label !== '' ? React.createElement(React.Fragment, null, React.createElement("span", {
      className: css(styles.switchToggle)
    }), React.createElement("span", {
      className: css(styles.switchLabel, styles.modifiers.on),
      id: isAriaLabelledBy ? `${this.id}-on` : null,
      "aria-hidden": "true"
    }, label), React.createElement("span", {
      className: css(styles.switchLabel, styles.modifiers.off),
      id: isAriaLabelledBy ? `${this.id}-off` : null,
      "aria-hidden": "true"
    }, labelOff || label)) : label !== '' && labelOff !== '' ? React.createElement(React.Fragment, null, React.createElement("span", {
      className: css(styles.switchToggle)
    }), React.createElement("span", {
      className: css(styles.switchLabel, styles.modifiers.on),
      id: isAriaLabelledBy ? `${this.id}-on` : null,
      "aria-hidden": "true"
    }, label), React.createElement("span", {
      className: css(styles.switchLabel, styles.modifiers.off),
      id: isAriaLabelledBy ? `${this.id}-off` : null,
      "aria-hidden": "true"
    }, labelOff)) : React.createElement("span", {
      className: css(styles.switchToggle)
    }, React.createElement("div", {
      className: css(styles.switchToggleIcon),
      "aria-hidden": "true"
    }, React.createElement(CheckIcon, {
      noVerticalAlign: true
    }))));
  }

}

_defineProperty(Switch, "propTypes", {
  id: _pt.string,
  className: _pt.string,
  label: _pt.string,
  labelOff: _pt.string,
  isChecked: _pt.bool,
  isDisabled: _pt.bool,
  onChange: _pt.func,
  'aria-label': _pt.string
});

_defineProperty(Switch, "defaultProps", {
  id: '',
  className: '',
  label: '',
  labelOff: '',
  isChecked: true,
  isDisabled: false,
  'aria-label': '',
  onChange: () => undefined
});

const SwitchWithOuiaContext = withOuiaContext(Switch);
export { SwitchWithOuiaContext as Switch };
//# sourceMappingURL=Switch.js.map