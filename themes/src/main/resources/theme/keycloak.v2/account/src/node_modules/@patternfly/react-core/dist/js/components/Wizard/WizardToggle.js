"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.WizardToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const wizard_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const caret_down_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/caret-down-icon'));
const WizardBody_1 = require("./WizardBody");
const WizardToggle = ({ isNavOpen, onNavToggle, nav, steps, activeStep, children, hasNoBodyPadding = false, 'aria-label': ariaLabel = 'Wizard Toggle', mainAriaLabelledBy = null, mainAriaLabel = null, isInPage = true, hasDrawer, isDrawerExpanded }) => {
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
        React.createElement("button", { onClick: () => onNavToggle(!isNavOpen), className: react_styles_1.css(wizard_1.default.wizardToggle, isNavOpen && 'pf-m-expanded'), "aria-label": ariaLabel, "aria-expanded": isNavOpen },
            React.createElement("span", { className: react_styles_1.css(wizard_1.default.wizardToggleList) },
                React.createElement("span", { className: react_styles_1.css(wizard_1.default.wizardToggleListItem) },
                    React.createElement("span", { className: react_styles_1.css(wizard_1.default.wizardToggleNum) }, activeStepIndex),
                    " ",
                    activeStepName,
                    activeStepSubName && React.createElement(angle_right_icon_1.default, { className: react_styles_1.css(wizard_1.default.wizardToggleSeparator), "aria-hidden": "true" })),
                activeStepSubName && React.createElement("span", { className: react_styles_1.css(wizard_1.default.wizardToggleListItem) }, activeStepSubName)),
            React.createElement("span", { className: react_styles_1.css(wizard_1.default.wizardToggleIcon) },
                React.createElement(caret_down_icon_1.default, { "aria-hidden": "true" }))),
        React.createElement("div", { className: react_styles_1.css(wizard_1.default.wizardOuterWrap) },
            React.createElement("div", { className: react_styles_1.css(wizard_1.default.wizardInnerWrap) },
                nav(isNavOpen),
                React.createElement(WizardBody_1.WizardBody, { mainComponent: isInPage ? 'div' : 'main', "aria-label": mainAriaLabel, "aria-labelledby": mainAriaLabelledBy, hasNoBodyPadding: hasNoBodyPadding, activeStep: activeStep, isDrawerExpanded: isDrawerExpanded, hasDrawer: hasDrawer },
                    hasDrawer && !isDrawerExpanded && activeStep.drawerToggleButton,
                    activeStep.component)),
            children)));
};
exports.WizardToggle = WizardToggle;
exports.WizardToggle.displayName = 'WizardToggle';
//# sourceMappingURL=WizardToggle.js.map