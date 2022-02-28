import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Expandable/expandable';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
export class Expandable extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isExpanded: props.isExpanded
    };
  }

  calculateToggleText(toggleText, toggleTextExpanded, toggleTextCollapsed, propOrStateIsExpanded) {
    if (propOrStateIsExpanded && toggleTextExpanded !== '') {
      return toggleTextExpanded;
    }

    if (!propOrStateIsExpanded && toggleTextCollapsed !== '') {
      return toggleTextCollapsed;
    }

    return toggleText;
  }

  render() {
    const _this$props = this.props,
          {
      onToggle: onToggleProp,
      isFocused,
      isHovered,
      isActive,
      className,
      toggleText,
      toggleTextExpanded,
      toggleTextCollapsed,
      children,
      isExpanded
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["onToggle", "isFocused", "isHovered", "isActive", "className", "toggleText", "toggleTextExpanded", "toggleTextCollapsed", "children", "isExpanded"]);

    let onToggle = onToggleProp;
    let propOrStateIsExpanded = isExpanded; // uncontrolled

    if (isExpanded === undefined) {
      propOrStateIsExpanded = this.state.isExpanded;

      onToggle = () => {
        onToggleProp();
        this.setState(prevState => ({
          isExpanded: !prevState.isExpanded
        }));
      };
    }

    const computedToggleText = this.calculateToggleText(toggleText, toggleTextExpanded, toggleTextCollapsed, propOrStateIsExpanded);
    return React.createElement("div", _extends({}, props, {
      className: css(styles.expandable, propOrStateIsExpanded && styles.modifiers.expanded, className)
    }), React.createElement("button", {
      className: css(styles.expandableToggle, isFocused && styles.modifiers.focus, isHovered && styles.modifiers.hover, isActive && styles.modifiers.active),
      type: "button",
      "aria-expanded": propOrStateIsExpanded,
      onClick: onToggle
    }, React.createElement(AngleRightIcon, {
      className: css(styles.expandableToggleIcon),
      "aria-hidden": true
    }), React.createElement("span", null, computedToggleText)), React.createElement("div", {
      className: css(styles.expandableContent),
      hidden: !propOrStateIsExpanded
    }, children));
  }

}

_defineProperty(Expandable, "propTypes", {
  children: _pt.node.isRequired,
  className: _pt.string,
  isExpanded: _pt.bool,
  toggleText: _pt.string,
  toggleTextExpanded: _pt.string,
  toggleTextCollapsed: _pt.string,
  onToggle: _pt.func,
  isFocused: _pt.bool,
  isHovered: _pt.bool,
  isActive: _pt.bool
});

_defineProperty(Expandable, "defaultProps", {
  className: '',
  toggleText: '',
  toggleTextExpanded: '',
  toggleTextCollapsed: '',
  onToggle: () => undefined,
  isFocused: false,
  isActive: false,
  isHovered: false
});
//# sourceMappingURL=Expandable.js.map