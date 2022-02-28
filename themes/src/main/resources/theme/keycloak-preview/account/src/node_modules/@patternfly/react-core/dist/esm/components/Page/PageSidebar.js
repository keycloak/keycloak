import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import { PageContextConsumer } from './Page';
export const PageSidebar = (_ref) => {
  let {
    className = '',
    nav,
    isNavOpen = true,
    theme = 'light'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "nav", "isNavOpen", "theme"]);

  return React.createElement(PageContextConsumer, null, ({
    isManagedSidebar,
    isNavOpen: managedIsNavOpen
  }) => {
    const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
    return React.createElement("div", _extends({
      id: "page-sidebar",
      className: css(styles.pageSidebar, theme === 'dark' && styles.modifiers.dark, navOpen && styles.modifiers.expanded, !navOpen && styles.modifiers.collapsed, className)
    }, props), React.createElement("div", {
      className: css(styles.pageSidebarBody)
    }, nav));
  });
};
PageSidebar.propTypes = {
  className: _pt.string,
  nav: _pt.node,
  isManagedSidebar: _pt.bool,
  isNavOpen: _pt.bool,
  theme: _pt.oneOf(['dark', 'light'])
};
//# sourceMappingURL=PageSidebar.js.map