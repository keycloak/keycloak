"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.WizardFooterInternal = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const wizard_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));
const Button_1 = require("../Button");
const WizardFooterInternal = ({ onNext, onBack, onClose, isValid, firstStep, activeStep, nextButtonText, backButtonText, cancelButtonText }) => (React.createElement("footer", { className: react_styles_1.css(wizard_1.default.wizardFooter) },
    React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.primary, type: "submit", onClick: onNext, isDisabled: !isValid }, nextButtonText),
    !activeStep.hideBackButton && (React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.secondary, onClick: onBack, isDisabled: firstStep }, backButtonText)),
    !activeStep.hideCancelButton && (React.createElement("div", { className: wizard_1.default.wizardFooterCancel },
        React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.link, onClick: onClose }, cancelButtonText)))));
exports.WizardFooterInternal = WizardFooterInternal;
exports.WizardFooterInternal.displayName = 'WizardFooterInternal';
//# sourceMappingURL=WizardFooterInternal.js.map