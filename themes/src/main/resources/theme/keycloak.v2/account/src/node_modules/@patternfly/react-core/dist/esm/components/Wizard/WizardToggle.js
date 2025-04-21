import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import { WizardBody } from './WizardBody';
export const WizardToggle = ({ isNavOpen, onNavToggle, nav, steps, activeStep, children, hasNoBodyPadding = false, 'aria-label': ariaLabel = 'Wizard Toggle', mainAriaLabelledBy = null, mainAriaLabel = null, isInPage = true, hasDrawer, isDrawerExpanded }) => {
    let activeStepIndex;
    let activeStepName;
    let activeStepSubName;
    for (let i = 0; i < steps.length; i++) {
        if ((activeStep.id && steps[i].id === activeStep.id) || steps[i].name === activeStep.name) {
            activeStepIndex = i + 1;
            activeStepName = steps[i].name;
            break;
        }
        else if (steps[i].steps) {
            for (const step of steps[i].steps) {
                if ((activeStep.id && step.id === activeStep.id) || step.name === activeStep.name) {
                    activeStepIndex = i + 1;
                    activeStepName = steps[i].name;
                    activeStepSubName = step.name;
                    break;
                }
            }
        }
    }
    return (React.createElement(React.Fragment, null,
        React.createElement("button", { onClick: () => onNavToggle(!isNavOpen), className: css(styles.wizardToggle, isNavOpen && 'pf-m-expanded'), "aria-label": ariaLabel, "aria-expanded": isNavOpen },
            React.createElement("span", { className: css(styles.wizardToggleList) },
                React.createElement("span", { className: css(styles.wizardToggleListItem) },
                    React.createElement("span", { className: css(styles.wizardToggleNum) }, activeStepIndex),
                    " ",
                    activeStepName,
                    activeStepSubName && React.createElement(AngleRightIcon, { className: css(styles.wizardToggleSeparator), "aria-hidden": "true" })),
                activeStepSubName && React.createElement("span", { className: css(styles.wizardToggleListItem) }, activeStepSubName)),
            React.createElement("span", { className: css(styles.wizardToggleIcon) },
                React.createElement(CaretDownIcon, { "aria-hidden": "true" }))),
        React.createElement("div", { className: css(styles.wizardOuterWrap) },
            React.createElement("div", { className: css(styles.wizardInnerWrap) },
                nav(isNavOpen),
                React.createElement(WizardBody, { mainComponent: isInPage ? 'div' : 'main', "aria-label": mainAriaLabel, "aria-labelledby": mainAriaLabelledBy, hasNoBodyPadding: hasNoBodyPadding, activeStep: activeStep, isDrawerExpanded: isDrawerExpanded, hasDrawer: hasDrawer },
                    hasDrawer && !isDrawerExpanded && activeStep.drawerToggleButton,
                    activeStep.component)),
            children)));
};
WizardToggle.displayName = 'WizardToggle';
//# sourceMappingURL=WizardToggle.js.map