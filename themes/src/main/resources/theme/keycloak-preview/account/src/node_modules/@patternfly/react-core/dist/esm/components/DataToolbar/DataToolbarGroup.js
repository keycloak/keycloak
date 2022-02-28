import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css, getModifier } from '@patternfly/react-styles';
import { formatBreakpointMods } from '../../helpers/util';
export let DataToolbarGroupVariant;

(function (DataToolbarGroupVariant) {
  DataToolbarGroupVariant["filter-group"] = "filter-group";
  DataToolbarGroupVariant["icon-button-group"] = "icon-button-group";
  DataToolbarGroupVariant["button-group"] = "button-group";
})(DataToolbarGroupVariant || (DataToolbarGroupVariant = {}));

class DataToolbarGroupWithRef extends React.Component {
  render() {
    const _this$props = this.props,
          {
      breakpointMods,
      className,
      variant,
      children,
      innerRef
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["breakpointMods", "className", "variant", "children", "innerRef"]);

    return React.createElement("div", _extends({
      className: css(styles.dataToolbarGroup, variant && getModifier(styles, variant), formatBreakpointMods(breakpointMods, styles), className)
    }, props, {
      ref: innerRef
    }), children);
  }

}

_defineProperty(DataToolbarGroupWithRef, "propTypes", {
  className: _pt.string,
  variant: _pt.oneOfType([_pt.any, _pt.oneOf(['filter-group']), _pt.oneOf(['icon-button-group']), _pt.oneOf(['button-group'])]),
  breakpointMods: _pt.arrayOf(_pt.any),
  children: _pt.node,
  innerRef: _pt.any
});

_defineProperty(DataToolbarGroupWithRef, "defaultProps", {
  breakpointMods: []
});

export const DataToolbarGroup = React.forwardRef((props, ref) => React.createElement(DataToolbarGroupWithRef, _extends({}, props, {
  innerRef: ref
})));
//# sourceMappingURL=DataToolbarGroup.js.map