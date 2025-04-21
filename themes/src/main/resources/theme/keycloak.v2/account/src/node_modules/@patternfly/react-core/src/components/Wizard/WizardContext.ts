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

export const WizardContext = React.createContext<WizardContextType>({
  goToStepById: () => null,
  goToStepByName: () => null,
  onNext: () => null,
  onBack: () => null,
  onClose: () => null,
  activeStep: { name: null }
});

export const WizardContextProvider = WizardContext.Provider;
export const WizardContextConsumer = WizardContext.Consumer;
