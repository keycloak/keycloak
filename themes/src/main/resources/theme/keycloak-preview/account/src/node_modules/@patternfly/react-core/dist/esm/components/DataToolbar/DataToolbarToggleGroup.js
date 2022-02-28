import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css, getModifier } from '@patternfly/react-styles';
import { DataToolbarContext, DataToolbarContentContext } from './DataToolbarUtils';
import { Button } from '../../components/Button';
import globalBreakpointLg from '@patternfly/react-tokens/dist/js/global_breakpoint_lg';
import { formatBreakpointMods } from '../../helpers/util';
export class DataToolbarToggleGroup extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "isContentPopup", () => {
      const viewportSize = window.innerWidth;
      const lgBreakpointValue = parseInt(globalBreakpointLg.value);
      return viewportSize < lgBreakpointValue;
    });
  }

  render() {
    const _this$props = this.props,
          {
      toggleIcon,
      breakpoint,
      variant,
      breakpointMods,
      className,
      children
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["toggleIcon", "breakpoint", "variant", "breakpointMods", "className", "children"]);

    return React.createElement(DataToolbarContext.Consumer, null, ({
      isExpanded,
      toggleIsExpanded
    }) => React.createElement(DataToolbarContentContext.Consumer, null, ({
      expandableContentRef,
      expandableContentId
    }) => {
      if (expandableContentRef.current && expandableContentRef.current.classList) {
        if (isExpanded) {
          expandableContentRef.current.classList.add(getModifier(styles, 'expanded'));
        } else {
          expandableContentRef.current.classList.remove(getModifier(styles, 'expanded'));
        }
      }

      return React.createElement("div", _extends({
        className: css(styles.dataToolbarGroup, variant && getModifier(styles, variant), formatBreakpointMods(breakpointMods, styles), getModifier(styles, 'toggle-group'), getModifier(styles, `show-on-${breakpoint}`), className)
      }, props), React.createElement("div", {
        className: css(styles.dataToolbarToggle)
      }, React.createElement(Button, _extends({
        variant: "plain",
        onClick: toggleIsExpanded,
        "aria-label": "Show Filters"
      }, isExpanded && {
        'aria-expanded': true
      }, {
        "aria-haspopup": isExpanded && this.isContentPopup(),
        "aria-controls": expandableContentId
      }), toggleIcon)), isExpanded ? ReactDOM.createPortal(children, expandableContentRef.current.firstElementChild) : children);
    }));
  }

}

_defineProperty(DataToolbarToggleGroup, "propTypes", {
  toggleIcon: _pt.node.isRequired,
  breakpoint: _pt.oneOf(['md', 'lg', 'xl']).isRequired,
  breakpointMods: _pt.arrayOf(_pt.any)
});

_defineProperty(DataToolbarToggleGroup, "defaultProps", {
  breakpointMods: []
});
//# sourceMappingURL=DataToolbarToggleGroup.js.map