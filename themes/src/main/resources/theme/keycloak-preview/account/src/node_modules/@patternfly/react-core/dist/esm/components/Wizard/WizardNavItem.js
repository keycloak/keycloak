import _pt from "prop-types";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
export const WizardNavItem = ({
  children = null,
  text = '',
  isCurrent = false,
  isDisabled = false,
  step,
  onNavItemClick = () => undefined,
  navItemComponent = 'a'
}) => {
  const NavItemComponent = navItemComponent;
  return React.createElement("li", {
    className: css(styles.wizardNavItem)
  }, React.createElement(NavItemComponent, {
    "aria-current": isCurrent && !children ? 'page' : false,
    onClick: () => onNavItemClick(step),
    className: css(styles.wizardNavLink, isCurrent && 'pf-m-current', isDisabled && 'pf-m-disabled'),
    "aria-disabled": isDisabled ? true : false,
    tabIndex: isDisabled ? -1 : undefined
  }, text), children);
};
WizardNavItem.propTypes = {
  children: _pt.node,
  text: _pt.string,
  isCurrent: _pt.bool,
  isDisabled: _pt.bool,
  step: _pt.number.isRequired,
  onNavItemClick: _pt.func,
  navItemComponent: _pt.node
};
//# sourceMappingURL=WizardNavItem.js.map