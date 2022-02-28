import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { css, getModifier } from '@patternfly/react-styles';
import { OverflowMenuContext } from './OverflowMenuContext';
/* eslint-disable camelcase */

import global_breakpoint_md from '@patternfly/react-tokens/dist/js/global_breakpoint_md';
import global_breakpoint_lg from '@patternfly/react-tokens/dist/js/global_breakpoint_lg';
import global_breakpoint_xl from '@patternfly/react-tokens/dist/js/global_breakpoint_xl';
/* eslint-enable camelcase */

import { debounce } from '../../helpers/util';
export class OverflowMenu extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "handleResize", () => {
      const breakpoints = {
        /* eslint-disable camelcase */
        md: global_breakpoint_md,
        lg: global_breakpoint_lg,
        xl: global_breakpoint_xl
        /* eslint-enable camelcase */

      };
      const {
        breakpoint
      } = this.props;
      let breakpointWidth = breakpoints[breakpoint].value;
      breakpointWidth = Number(breakpointWidth.split('px')[0]);
      const isBelowBreakpoint = window.innerWidth < breakpointWidth;
      this.state.isBelowBreakpoint !== isBelowBreakpoint && this.setState({
        isBelowBreakpoint
      });
    });

    this.state = {
      isBelowBreakpoint: false
    };
  }

  componentDidMount() {
    this.handleResize();
    window.addEventListener('resize', debounce(this.handleResize, 250));
  }

  componentWillUnmount() {
    window.removeEventListener('resize', debounce(this.handleResize, 250));
  }

  render() {
    const _this$props = this.props,
          {
      className,
      breakpoint,
      children
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "breakpoint", "children"]);

    return React.createElement("div", _extends({}, props, {
      className: css(styles.overflowMenu, getModifier(styles.modifiers, `showOn ${breakpoint}`), className)
    }), React.createElement(OverflowMenuContext.Provider, {
      value: {
        isBelowBreakpoint: this.state.isBelowBreakpoint
      }
    }, children));
  }

}

_defineProperty(OverflowMenu, "propTypes", {
  children: _pt.any,
  className: _pt.string,
  breakpoint: _pt.oneOf(['md', 'lg', 'xl']).isRequired
});

OverflowMenu.contextType = OverflowMenuContext;
//# sourceMappingURL=OverflowMenu.js.map