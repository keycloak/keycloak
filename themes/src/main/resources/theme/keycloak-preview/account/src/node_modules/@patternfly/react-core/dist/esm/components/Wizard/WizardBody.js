import _pt from "prop-types";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { css } from '@patternfly/react-styles';
export const WizardBody = ({
  children,
  hasBodyPadding = true
}) => React.createElement("main", {
  className: css(styles.wizardMain, !hasBodyPadding && styles.modifiers.noPadding)
}, React.createElement("div", {
  className: css(styles.wizardMainBody)
}, children));
WizardBody.propTypes = {
  children: _pt.any.isRequired,
  hasBodyPadding: _pt.bool.isRequired
};
//# sourceMappingURL=WizardBody.js.map