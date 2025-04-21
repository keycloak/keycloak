import * as React from 'react';
import { WizardStep } from './Wizard';
export interface WizardFooterInternalProps {
    onNext: any;
    onBack: any;
    onClose: any;
    isValid: boolean;
    firstStep: boolean;
    activeStep: WizardStep;
    nextButtonText: React.ReactNode;
    backButtonText: React.ReactNode;
    cancelButtonText: React.ReactNode;
}
export declare const WizardFooterInternal: React.FunctionComponent<WizardFooterInternalProps>;
//# sourceMappingURL=WizardFooterInternal.d.ts.map