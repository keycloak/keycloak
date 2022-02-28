import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { withOuiaContext } from '../withOuia';
export const NavContext = React.createContext({});

class Nav extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "state", {
      showLeftScrollButton: false,
      showRightScrollButton: false
    });

    _defineProperty(this, "updateScrollButtonState", state => {
      const {
        showLeftScrollButton,
        showRightScrollButton
      } = state;
      this.setState({
        showLeftScrollButton,
        showRightScrollButton
      });
    });
  }

  // Callback from NavItem
  onSelect(event, groupId, itemId, to, preventDefault, onClick) {
    if (preventDefault) {
      event.preventDefault();
    }

    this.props.onSelect({
      groupId,
      itemId,
      event,
      to
    });

    if (onClick) {
      onClick(event, itemId, groupId, to);
    }
  } // Callback from NavExpandable


  onToggle(event, groupId, toggleValue) {
    this.props.onToggle({
      event,
      groupId,
      isExpanded: toggleValue
    });
  }

  render() {
    const _this$props = this.props,
          {
      'aria-label': ariaLabel,
      children,
      className,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onSelect,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onToggle,
      theme,
      ouiaContext,
      ouiaId
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["aria-label", "children", "className", "onSelect", "onToggle", "theme", "ouiaContext", "ouiaId"]);

    const {
      showLeftScrollButton,
      showRightScrollButton
    } = this.state;
    const childrenProps = children.props;
    return React.createElement(NavContext.Provider, {
      value: {
        onSelect: (event, groupId, itemId, to, preventDefault, onClick) => this.onSelect(event, groupId, itemId, to, preventDefault, onClick),
        onToggle: (event, groupId, expanded) => this.onToggle(event, groupId, expanded),
        updateScrollButtonState: this.updateScrollButtonState
      }
    }, React.createElement("nav", _extends({
      className: css(styles.nav, theme === 'dark' && styles.modifiers.dark, showLeftScrollButton && styles.modifiers.start, showRightScrollButton && styles.modifiers.end, className),
      "aria-label": ariaLabel === '' ? typeof childrenProps !== 'undefined' && childrenProps.variant === 'tertiary' ? 'Local' : 'Global' : ariaLabel
    }, ouiaContext.isOuia && {
      'data-ouia-component-type': 'Nav',
      'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
    }, props), children));
  }

}

_defineProperty(Nav, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  onSelect: _pt.func,
  onToggle: _pt.func,
  'aria-label': _pt.string,
  theme: _pt.oneOf(['dark', 'light'])
});

_defineProperty(Nav, "defaultProps", {
  'aria-label': '',
  children: null,
  className: '',
  onSelect: () => undefined,
  onToggle: () => undefined,
  theme: 'light'
});

const NavWithOuiaContext = withOuiaContext(Nav);
export { NavWithOuiaContext as Nav };
//# sourceMappingURL=Nav.js.map