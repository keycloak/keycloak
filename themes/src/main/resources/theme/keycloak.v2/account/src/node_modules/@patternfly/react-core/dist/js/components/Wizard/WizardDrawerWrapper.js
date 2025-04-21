"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.WizardDrawerWrapper = void 0;
const WizardDrawerWrapper = ({ hasDrawer, wrapper, children }) => (hasDrawer ? wrapper(children) : children);
exports.WizardDrawerWrapper = WizardDrawerWrapper;
exports.WizardDrawerWrapper.displayName = 'WizardDrawerWrapper';
//# sourceMappingURL=WizardDrawerWrapper.js.map