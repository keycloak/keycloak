import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { css } from '@patternfly/react-styles';
export const WizardNav = ({ children, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy, isOpen = false, returnList = false }) => {
    const innerList = React.createElement("ol", { className: css(styles.wizardNavList) }, children);
    if (returnList) {
        return innerList;
    }
    return (React.createElement("nav", { className: css(styles.wizardNav, isOpen && styles.modifiers.expanded), "aria-label": ariaLabel, "aria-labelledby": ariaLabelledBy },
        React.createElement("ol", { className: css(styles.wizardNavList) }, children)));
};
WizardNav.displayName = 'WizardNav';
//# sourceMappingURL=WizardNav.js.map