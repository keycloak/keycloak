import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { NavContext } from './Nav';
export const NavItem = (_ref) => {
  let {
    children = null,
    className = '',
    to = '',
    isActive = false,
    groupId = null,
    itemId = null,
    preventDefault = false,
    onClick = null,
    component = 'a'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "to", "isActive", "groupId", "itemId", "preventDefault", "onClick", "component"]);

  const Component = component;

  const renderDefaultLink = () => {
    const preventLinkDefault = preventDefault || !to;
    return React.createElement(NavContext.Consumer, null, context => React.createElement(Component, _extends({
      href: to,
      onClick: e => context.onSelect(e, groupId, itemId, to, preventLinkDefault, onClick),
      className: css(styles.navLink, isActive && styles.modifiers.current, className),
      "aria-current": isActive ? 'page' : null
    }, props), children));
  };

  const renderClonedChild = child => React.createElement(NavContext.Consumer, null, context => React.cloneElement(child, {
    onClick: e => context.onSelect(e, groupId, itemId, to, preventDefault, onClick),
    className: css(styles.navLink, isActive && styles.modifiers.current, child.props && child.props.className),
    'aria-current': isActive ? 'page' : null
  }));

  return React.createElement("li", {
    className: css(styles.navItem, className)
  }, React.isValidElement(children) ? renderClonedChild(children) : renderDefaultLink());
};
NavItem.propTypes = {
  children: _pt.node,
  className: _pt.string,
  to: _pt.string,
  isActive: _pt.bool,
  groupId: _pt.oneOfType([_pt.string, _pt.number, _pt.oneOf([null])]),
  itemId: _pt.oneOfType([_pt.string, _pt.number, _pt.oneOf([null])]),
  preventDefault: _pt.bool,
  onClick: _pt.func,
  component: _pt.node
};
//# sourceMappingURL=NavItem.js.map