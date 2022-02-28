import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/FormControl/form-control';
import { css, getModifier } from '@patternfly/react-styles';
import { ValidatedOptions } from '../../helpers/constants';
export let TextAreResizeOrientation;

(function (TextAreResizeOrientation) {
  TextAreResizeOrientation["horizontal"] = "horizontal";
  TextAreResizeOrientation["vertical"] = "vertical";
  TextAreResizeOrientation["both"] = "both";
})(TextAreResizeOrientation || (TextAreResizeOrientation = {}));

export class TextArea extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleChange", event => {
      if (this.props.onChange) {
        this.props.onChange(event.currentTarget.value, event);
      }
    });

    if (!props.id && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('TextArea: TextArea requires either an id or aria-label to be specified');
    }
  }

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _this$props = this.props,
          {
      className,
      value,
      onChange,
      isValid,
      validated,
      isRequired,
      resizeOrientation
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "value", "onChange", "isValid", "validated", "isRequired", "resizeOrientation"]);

    const orientation = 'resize' + resizeOrientation.charAt(0).toUpperCase() + resizeOrientation.slice(1);
    return React.createElement("textarea", _extends({
      className: css(styles.formControl, className, resizeOrientation !== TextAreResizeOrientation.both && getModifier(styles, orientation), validated === ValidatedOptions.success && styles.modifiers.success),
      onChange: this.handleChange
    }, typeof this.props.defaultValue !== 'string' && {
      value
    }, {
      "aria-invalid": !isValid || validated === ValidatedOptions.error,
      required: isRequired
    }, props));
  }

}

_defineProperty(TextArea, "propTypes", {
  className: _pt.string,
  isRequired: _pt.bool,
  isValid: _pt.bool,
  validated: _pt.oneOf(['success', 'error', 'default']),
  value: _pt.oneOfType([_pt.string, _pt.number]),
  onChange: _pt.func,
  resizeOrientation: _pt.oneOf(['horizontal', 'vertical', 'both']),
  'aria-label': _pt.string
});

_defineProperty(TextArea, "defaultProps", {
  className: '',
  isRequired: false,
  isValid: true,
  validated: 'default',
  resizeOrientation: 'both',
  'aria-label': null
});
//# sourceMappingURL=TextArea.js.map