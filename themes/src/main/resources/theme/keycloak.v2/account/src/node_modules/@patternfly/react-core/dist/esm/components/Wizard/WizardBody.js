import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { css } from '@patternfly/react-styles';
import { WizardDrawerWrapper } from './WizardDrawerWrapper';
import { Drawer, DrawerContent } from '../Drawer';
export const WizardBody = ({ children, hasNoBodyPadding = false, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy, mainComponent = 'div', hasDrawer, isDrawerExpanded, activeStep }) => {
    const MainComponent = mainComponent;
    return (React.createElement(MainComponent, { "aria-label": ariaLabel, "aria-labelledby": ariaLabelledBy, className: css(styles.wizardMain) },
        React.createElement(WizardDrawerWrapper, { hasDrawer: hasDrawer && activeStep.drawerPanelContent, wrapper: (children) => (React.createElement(Drawer, { isInline: true, isExpanded: isDrawerExpanded },
                React.createElement(DrawerContent, { panelContent: activeStep.drawerPanelContent }, children))) },
            React.createElement("div", { className: css(styles.wizardMainBody, hasNoBodyPadding && styles.modifiers.noPadding) }, children))));
};
WizardBody.displayName = 'WizardBody';
//# sourceMappingURL=WizardBody.js.map