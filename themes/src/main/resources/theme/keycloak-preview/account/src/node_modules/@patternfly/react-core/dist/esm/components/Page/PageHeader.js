import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Page/page';
import { css } from '@patternfly/react-styles';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';
import { Button, ButtonVariant } from '../../components/Button';
import { PageContextConsumer } from './Page';
export const PageHeader = (_ref) => {
  let {
    className = '',
    logo = null,
    logoProps = null,
    logoComponent = 'a',
    toolbar = null,
    avatar = null,
    topNav = null,
    isNavOpen = true,
    role = undefined,
    showNavToggle = false,
    onNavToggle = () => undefined,
    'aria-label': ariaLabel = 'Global navigation'
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "logo", "logoProps", "logoComponent", "toolbar", "avatar", "topNav", "isNavOpen", "role", "showNavToggle", "onNavToggle", "aria-label"]);

  const LogoComponent = logoComponent;
  return React.createElement(PageContextConsumer, null, ({
    isManagedSidebar,
    onNavToggle: managedOnNavToggle,
    isNavOpen: managedIsNavOpen
  }) => {
    const navToggle = isManagedSidebar ? managedOnNavToggle : onNavToggle;
    const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
    return React.createElement("header", _extends({
      role: role,
      className: css(styles.pageHeader, className)
    }, props), (showNavToggle || logo) && React.createElement("div", {
      className: css(styles.pageHeaderBrand)
    }, showNavToggle && React.createElement("div", {
      className: css(styles.pageHeaderBrandToggle)
    }, React.createElement(Button, {
      id: "nav-toggle",
      onClick: navToggle,
      "aria-label": ariaLabel,
      "aria-controls": "page-sidebar",
      "aria-expanded": navOpen ? 'true' : 'false',
      variant: ButtonVariant.plain
    }, React.createElement(BarsIcon, null))), logo && React.createElement(LogoComponent, _extends({
      className: css(styles.pageHeaderBrandLink)
    }, logoProps), logo)), topNav && React.createElement("div", {
      className: css(styles.pageHeaderNav)
    }, topNav), (toolbar || avatar) && React.createElement("div", {
      className: css(styles.pageHeaderTools)
    }, toolbar, avatar));
  });
};
PageHeader.propTypes = {
  className: _pt.string,
  logo: _pt.node,
  logoProps: _pt.object,
  logoComponent: _pt.node,
  toolbar: _pt.node,
  avatar: _pt.node,
  topNav: _pt.node,
  showNavToggle: _pt.bool,
  isNavOpen: _pt.bool,
  isManagedSidebar: _pt.bool,
  role: _pt.string,
  onNavToggle: _pt.func,
  'aria-label': _pt.string
};
//# sourceMappingURL=PageHeader.js.map