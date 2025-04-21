"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.WizardContextConsumer = exports.WizardContextProvider = exports.WizardContext = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
exports.WizardContext = React.createContext({
    goToStepById: () => null,
    goToStepByName: () => null,
    onNext: () => null,
    onBack: () => null,
    onClose: () => null,
    activeStep: { name: null }
});
exports.WizardContextProvider = exports.WizardContext.Provider;
exports.WizardContextConsumer = exports.WizardContext.Consumer;
//# sourceMappingURL=WizardContext.js.map