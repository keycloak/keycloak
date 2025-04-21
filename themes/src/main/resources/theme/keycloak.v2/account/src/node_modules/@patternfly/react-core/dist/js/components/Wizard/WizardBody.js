"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.WizardBody = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const wizard_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));
const react_styles_1 = require("@patternfly/react-styles");
const WizardDrawerWrapper_1 = require("./WizardDrawerWrapper");
const Drawer_1 = require("../Drawer");
const WizardBody = ({ children, hasNoBodyPadding = false, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy, mainComponent = 'div', hasDrawer, isDrawerExpanded, activeStep }) => {
    const MainComponent = mainComponent;
    return (React.createElement(MainComponent, { "aria-label": ariaLabel, "aria-labelledby": ariaLabelledBy, className: react_styles_1.css(wizard_1.default.wizardMain) },
        React.createElement(WizardDrawerWrapper_1.WizardDrawerWrapper, { hasDrawer: hasDrawer && activeStep.drawerPanelContent, wrapper: (children) => (React.createElement(Drawer_1.Drawer, { isInline: true, isExpanded: isDrawerExpanded },
                React.createElement(Drawer_1.DrawerContent, { panelContent: activeStep.drawerPanelContent }, children))) },
            React.createElement("div", { className: react_styles_1.css(wizard_1.default.wizardMainBody, hasNoBodyPadding && wizard_1.default.modifiers.noPadding) }, children))));
};
exports.WizardBody = WizardBody;
exports.WizardBody.displayName = 'WizardBody';
//# sourceMappingURL=WizardBody.js.map