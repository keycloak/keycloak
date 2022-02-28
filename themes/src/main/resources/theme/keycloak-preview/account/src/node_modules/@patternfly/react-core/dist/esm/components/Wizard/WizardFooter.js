import _pt from "prop-types";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
export const WizardFooter = ({
  children
}) => React.createElement("footer", {
  className: css(styles.wizardFooter)
}, children);
WizardFooter.propTypes = {
  children: _pt.any.isRequired
};
//# sourceMappingURL=WizardFooter.js.map