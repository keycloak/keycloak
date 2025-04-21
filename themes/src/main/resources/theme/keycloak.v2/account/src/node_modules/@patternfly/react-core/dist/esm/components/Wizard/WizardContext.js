import * as React from 'react';
export const WizardContext = React.createContext({
    goToStepById: () => null,
    goToStepByName: () => null,
    onNext: () => null,
    onBack: () => null,
    onClose: () => null,
    activeStep: { name: null }
});
export const WizardContextProvider = WizardContext.Provider;
export const WizardContextConsumer = WizardContext.Consumer;
//# sourceMappingURL=WizardContext.js.map