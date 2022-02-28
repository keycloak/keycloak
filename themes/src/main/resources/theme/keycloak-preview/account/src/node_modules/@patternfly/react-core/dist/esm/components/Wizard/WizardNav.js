import _pt from "prop-types";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { css } from '@patternfly/react-styles';
export const WizardNav = ({
  children,
  ariaLabel,
  isOpen = false,
  returnList = false
}) => {
  const innerList = React.createElement("ol", {
    className: css(styles.wizardNavList)
  }, children);

  if (returnList) {
    return innerList;
  }

  return React.createElement("nav", {
    className: css(styles.wizardNav, isOpen && 'pf-m-expanded'),
    "aria-label": ariaLabel
  }, React.createElement("ol", {
    className: css(styles.wizardNavList)
  }, children));
};
WizardNav.propTypes = {
  children: _pt.any,
  ariaLabel: _pt.string,
  isOpen: _pt.bool,
  returnList: _pt.bool
};
//# sourceMappingURL=WizardNav.js.map