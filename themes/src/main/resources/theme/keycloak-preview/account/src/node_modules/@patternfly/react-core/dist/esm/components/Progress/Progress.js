import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Progress/progress';
import { css, getModifier } from '@patternfly/react-styles';
import { ProgressContainer, ProgressMeasureLocation, ProgressVariant } from './ProgressContainer';
import { getUniqueId } from '../../helpers/util';
export let ProgressSize;

(function (ProgressSize) {
  ProgressSize["sm"] = "sm";
  ProgressSize["md"] = "md";
  ProgressSize["lg"] = "lg";
})(ProgressSize || (ProgressSize = {}));

export class Progress extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "id", this.props.id || getUniqueId());
  }

  render() {
    const _this$props = this.props,
          {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      id,
      className,
      size,
      value,
      title,
      label,
      variant,
      measureLocation,
      min,
      max,
      valueText
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["id", "className", "size", "value", "title", "label", "variant", "measureLocation", "min", "max", "valueText"]);

    const additionalProps = _objectSpread({}, props, {}, valueText ? {
      'aria-valuetext': valueText
    } : {
      'aria-describedby': `${this.id}-description`
    });

    const ariaProps = {
      'aria-describedby': `${this.id}-description`,
      'aria-valuemin': min,
      'aria-valuenow': value,
      'aria-valuemax': max
    };

    if (valueText) {
      ariaProps['aria-valuetext'] = valueText;
    }

    const scaledValue = Math.min(100, Math.max(0, Math.floor((value - min) / (max - min) * 100)));
    return React.createElement("div", _extends({}, additionalProps, {
      className: css(styles.progress, getModifier(styles, variant, ''), getModifier(styles, measureLocation, ''), getModifier(styles, measureLocation === ProgressMeasureLocation.inside ? ProgressSize.lg : size, ''), !title && getModifier(styles, 'singleline', ''), className),
      id: this.id,
      role: "progressbar"
    }), React.createElement(ProgressContainer, {
      parentId: this.id,
      value: scaledValue,
      title: title,
      label: label,
      variant: variant,
      measureLocation: measureLocation,
      ariaProps: ariaProps
    }));
  }

}

_defineProperty(Progress, "propTypes", {
  className: _pt.string,
  size: _pt.oneOf(['sm', 'md', 'lg']),
  measureLocation: _pt.oneOf(['outside', 'inside', 'top', 'none']),
  variant: _pt.oneOf(['danger', 'success', 'info']),
  title: _pt.string,
  label: _pt.node,
  value: _pt.number,
  id: _pt.string,
  min: _pt.number,
  max: _pt.number,
  valueText: _pt.string
});

_defineProperty(Progress, "defaultProps", {
  className: '',
  measureLocation: ProgressMeasureLocation.top,
  variant: ProgressVariant.info,
  id: '',
  title: '',
  min: 0,
  max: 100,
  size: null,
  label: null,
  value: 0,
  valueText: null
});
//# sourceMappingURL=Progress.js.map