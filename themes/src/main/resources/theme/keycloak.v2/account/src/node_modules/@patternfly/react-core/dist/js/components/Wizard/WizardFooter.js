"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.WizardFooter = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const wizard_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));
const WizardFooter = ({ children }) => (React.createElement("footer", { className: react_styles_1.css(wizard_1.default.wizardFooter) }, children));
exports.WizardFooter = WizardFooter;
exports.WizardFooter.displayName = 'WizardFooter';
//# sourceMappingURL=WizardFooter.js.map