import * as React from 'react';
import { WizardStep } from './Wizard';
export interface WizardContextType {
    goToStepById: (stepId: number | string) => void;
    goToStepByName: (stepName: string) => void;
    onNext: () => void;
    onBack: () => void;
    onClose: () => void;
    activeStep: WizardStep;
}
export declare const WizardContext: React.Context<WizardContextType>;
export declare const WizardContextProvider: React.Provider<WizardContextType>;
export declare const WizardContextConsumer: React.Consumer<WizardContextType>;
//# sourceMappingURL=WizardContext.d.ts.map