import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/SkipToContent/skip-to-content';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css, getModifier } from '@patternfly/react-styles';
export class SkipToContent extends React.Component {
  render() {
    const _this$props = this.props,
          {
      component,
      children,
      className,
      href,
      show
    } = _this$props,
          rest = _objectWithoutProperties(_this$props, ["component", "children", "className", "href", "show"]);

    const Component = component;
    return React.createElement(Component, _extends({}, rest, {
      className: css(buttonStyles.button, getModifier(buttonStyles.modifiers, 'primary'), styles.skipToContent, show && getModifier(styles, 'focus'), className),
      href: href
    }), children);
  }

}

_defineProperty(SkipToContent, "propTypes", {
  component: _pt.any,
  href: _pt.string.isRequired,
  children: _pt.node,
  className: _pt.string,
  show: _pt.bool
});

_defineProperty(SkipToContent, "defaultProps", {
  component: 'a',
  className: '',
  show: false
});
//# sourceMappingURL=SkipToContent.js.map